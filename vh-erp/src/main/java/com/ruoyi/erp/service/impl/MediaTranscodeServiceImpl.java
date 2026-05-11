package com.ruoyi.erp.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.MediaConstants;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.vo.media.PreparedMediaUpload;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IMediaTranscodeService;
import com.ruoyi.erp.utils.MediaFileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;

/**
 * Shopify 同步阶段按需生成图片派生文件，避免保存商品时承担转码成本。
 */
@Slf4j
@Service
public class MediaTranscodeServiceImpl implements IMediaTranscodeService {

    private static final String SHOPIFY_SYNC_DIR = "_shopify_sync";
    private static final String WEBP_EXTENSION = ".webp";
    private static final String MIME_IMAGE_WEBP = "image/webp";
    private static final String DEFAULT_TRANSCODE_PROFILE = "webp-q82-max2400-v1";

    @Resource
    private IMediaService mediaService;

    @Value("${erp.media.shopify-transcode.webp-quality:82}")
    private int webpQuality = 82;

    @Value("${erp.media.shopify-transcode.max-side:2400}")
    private int maxSide = 2400;

    @Value("${erp.media.shopify-transcode.profile:webp-q82-max2400-v1}")
    private String transcodeProfile = DEFAULT_TRANSCODE_PROFILE;

    @Override
    public PreparedMediaUpload prepareForShopifyUpload(Media media) {
        if (media == null) {
            throw new ServiceException("媒体信息为空，无法准备 Shopify 上传文件");
        }

        Path sourcePath = resolveSourcePath(media);
        String filename = resolveFilename(media, sourcePath);
        String mediaType = resolveMediaContentType(media, filename);

        if (MediaConstants.TYPE_VIDEO.equals(mediaType)) {
            return buildOriginalUpload(sourcePath, filename, mediaType);
        }
        if (!MediaConstants.TYPE_IMAGE.equals(mediaType)) {
            throw new ServiceException("不支持同步到 Shopify 的媒体类型: " + mediaType + "，文件: " + filename);
        }
        if (isGif(filename)) {
            log.info("GIF 文件保持原格式上传，跳过 WebP 转码: mediaId={}, filename={}", media.getMediaId(), filename);
            return buildOriginalUpload(sourcePath, filename, MediaConstants.TYPE_IMAGE);
        }
        if (!isTranscodableImage(filename)) {
            return buildOriginalUpload(sourcePath, filename, MediaConstants.TYPE_IMAGE);
        }

        return prepareWebpUpload(media, sourcePath, filename);
    }

    private PreparedMediaUpload prepareWebpUpload(Media media, Path sourcePath, String filename) {
        String sourceHash = sha256(sourcePath);
        Path targetPath = buildDerivedWebpPath(sourcePath);
        String targetFilename = buildWebpFilename(filename);

        if (isCacheValid(media, sourceHash, targetPath)) {
            return PreparedMediaUpload.builder()
                    .file(targetPath.toAbsolutePath().normalize().toFile())
                    .filename(targetFilename)
                    .mimeType(MIME_IMAGE_WEBP)
                    .mediaContentType(MediaConstants.TYPE_IMAGE)
                    .transcoded(true)
                    .build();
        }

        transcodeToWebp(sourcePath, targetPath);
        updateTranscodeMetadata(media, sourceHash, targetPath);

        return PreparedMediaUpload.builder()
                .file(targetPath.toAbsolutePath().normalize().toFile())
                .filename(targetFilename)
                .mimeType(MIME_IMAGE_WEBP)
                .mediaContentType(MediaConstants.TYPE_IMAGE)
                .transcoded(true)
                .build();
    }

    private Path resolveSourcePath(Media media) {
        if (StringUtils.isEmpty(media.getNasMediaUrl())) {
            throw new ServiceException("媒体缺少 NAS 文件路径，mediaId=" + media.getMediaId());
        }

        String filePath = MediaFileUtil.convertUrlToFilePath(media.getNasMediaUrl());
        if (StringUtils.isEmpty(filePath)) {
            throw new ServiceException("媒体 NAS 文件路径无效，mediaId=" + media.getMediaId());
        }

        Path sourcePath = Paths.get(filePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(sourcePath)) {
            throw new ServiceException("媒体源文件不存在: " + sourcePath);
        }
        return sourcePath;
    }

    private String resolveFilename(Media media, Path sourcePath) {
        if (StringUtils.isNotEmpty(media.getFilename())) {
            return media.getFilename();
        }
        return sourcePath.getFileName().toString();
    }

    private String resolveMediaContentType(Media media, String filename) {
        String type = media.getMediaContentType();
        if (StringUtils.isEmpty(type) || MediaConstants.TYPE_UNKNOWN.equals(type)) {
            type = MediaFileUtil.getMediaType(filename);
        }
        if (StringUtils.isEmpty(type) || MediaConstants.TYPE_UNKNOWN.equals(type)) {
            throw new ServiceException("无法识别媒体类型，文件: " + filename);
        }
        return type;
    }

    private boolean isCacheValid(Media media, String sourceHash, Path expectedTargetPath) {
        if (StringUtils.isEmpty(media.getTranscodedMediaUrl())) {
            return false;
        }
        if (!Objects.equals(sourceHash, media.getTranscodeSourceHash())) {
            return false;
        }
        if (!Objects.equals(currentProfile(), media.getTranscodeProfile())) {
            return false;
        }

        String cachedFilePath = MediaFileUtil.convertUrlToFilePath(media.getTranscodedMediaUrl());
        if (StringUtils.isEmpty(cachedFilePath)) {
            return false;
        }
        Path cachedPath = Paths.get(cachedFilePath).toAbsolutePath().normalize();
        Path expectedPath = expectedTargetPath.toAbsolutePath().normalize();
        return expectedPath.equals(cachedPath) && Files.isRegularFile(cachedPath);
    }

    private void transcodeToWebp(Path sourcePath, Path targetPath) {
        try {
            Files.createDirectories(targetPath.getParent());
            BufferedImage sourceImage = ImageIO.read(sourcePath.toFile());
            if (sourceImage == null) {
                throw new ServiceException("无法读取图片文件: " + sourcePath);
            }

            BufferedImage outputImage = resizeAndNormalize(sourceImage);
            writeWebp(outputImage, targetPath);
        } catch (IOException e) {
            throw new ServiceException("图片转码 WebP 失败: " + sourcePath + "，原因: " + e.getMessage());
        }
    }

    private BufferedImage resizeAndNormalize(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int longestSide = Math.max(sourceWidth, sourceHeight);
        double scale = maxSide > 0 && longestSide > maxSide ? (double) maxSide / longestSide : 1.0D;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        boolean hasAlpha = sourceImage.getColorModel().hasAlpha();
        int imageType = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = outputImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!hasAlpha) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
            }
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return outputImage;
    }

    private void writeWebp(BufferedImage image, Path targetPath) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            throw new ServiceException("当前运行环境缺少 WebP 图片编码器，请确认已加载 webp-imageio 依赖");
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(targetPath.toFile())) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] compressionTypes = writeParam.getCompressionTypes();
                if (compressionTypes != null && compressionTypes.length > 0) {
                    writeParam.setCompressionType(compressionTypes[0]);
                }
                writeParam.setCompressionQuality(Math.min(1.0F, Math.max(0.0F, webpQuality / 100.0F)));
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private void updateTranscodeMetadata(Media media, String sourceHash, Path targetPath) {
        Date now = new Date();
        String targetUrl = MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(targetPath.toString()));

        media.setTranscodedMediaUrl(targetUrl);
        media.setTranscodeSourceHash(sourceHash);
        media.setTranscodeProfile(currentProfile());
        media.setTranscodeTime(now);

        if (media.getMediaId() == null) {
            return;
        }

        Media updateMedia = new Media();
        updateMedia.setMediaId(media.getMediaId());
        updateMedia.setTranscodedMediaUrl(targetUrl);
        updateMedia.setTranscodeSourceHash(sourceHash);
        updateMedia.setTranscodeProfile(currentProfile());
        updateMedia.setTranscodeTime(now);
        mediaService.updateMedia(updateMedia);
    }

    private PreparedMediaUpload buildOriginalUpload(Path sourcePath, String filename, String mediaType) {
        return PreparedMediaUpload.builder()
                .file(sourcePath.toAbsolutePath().normalize().toFile())
                .filename(filename)
                .mimeType(resolveMimeType(filename, sourcePath))
                .mediaContentType(mediaType)
                .transcoded(false)
                .build();
    }

    private Path buildDerivedWebpPath(Path sourcePath) {
        return sourcePath.getParent()
                .resolve(SHOPIFY_SYNC_DIR)
                .resolve(buildWebpFilename(sourcePath.getFileName().toString()))
                .toAbsolutePath()
                .normalize();
    }

    private String buildWebpFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        String basename = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return basename + WEBP_EXTENSION;
    }

    private boolean isTranscodableImage(String filename) {
        String extension = getExtension(filename);
        return "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "png".equals(extension)
                || "bmp".equals(extension)
                || "webp".equals(extension);
    }

    private boolean isGif(String filename) {
        return "gif".equals(getExtension(filename));
    }

    private String resolveMimeType(String filename, Path sourcePath) {
        return switch (getExtension(filename)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> MIME_IMAGE_WEBP;
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "mkv" -> "video/x-matroska";
            default -> probeMimeType(sourcePath);
        };
    }

    private String probeMimeType(Path sourcePath) {
        try {
            String mimeType = Files.probeContentType(sourcePath);
            return StringUtils.isNotEmpty(mimeType) ? mimeType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    private String getExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sha256(Path sourcePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(sourcePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new ServiceException("计算媒体源文件 Hash 失败: " + sourcePath + "，原因: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("当前 JDK 不支持 SHA-256: " + e.getMessage());
        }
    }

    private String currentProfile() {
        return StringUtils.isNotEmpty(transcodeProfile) ? transcodeProfile : DEFAULT_TRANSCODE_PROFILE;
    }
}
