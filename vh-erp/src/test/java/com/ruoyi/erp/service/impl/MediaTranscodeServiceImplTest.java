package com.ruoyi.erp.service.impl;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.erp.constant.MediaConstants;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.vo.media.PreparedMediaUpload;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.utils.MediaFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MediaTranscodeServiceImplTest {

    @TempDir
    Path tempDir;

    private IMediaService mediaService;
    private MediaTranscodeServiceImpl transcodeService;

    @BeforeEach
    void setUp() {
        new RuoYiConfig().setProfile(tempDir.toString().replace("\\", "/"));
        mediaService = mock(IMediaService.class);
        transcodeService = new MediaTranscodeServiceImpl();
        ReflectionTestUtils.setField(transcodeService, "mediaService", mediaService);
    }

    @Test
    void prepareForShopifyUploadTranscodesImageToCachedWebpAndUpdatesMediaMetadata() throws Exception {
        Path source = createImage("ABC001", "ABC001-1.jpg", "jpg", Color.RED);
        Media media = buildMedia(11L, "ABC001-1.jpg", source, MediaConstants.TYPE_IMAGE);
        String originalNasMediaUrl = media.getNasMediaUrl();
        String originalFilename = media.getFilename();

        PreparedMediaUpload upload = transcodeService.prepareForShopifyUpload(media);

        Path expectedWebp = source.getParent().resolve("_shopify_sync").resolve("ABC001-1.webp");
        assertEquals(expectedWebp.toAbsolutePath().normalize().toFile(), upload.getFile().getAbsoluteFile());
        assertEquals("ABC001-1.webp", upload.getFilename());
        assertEquals("image/webp", upload.getMimeType());
        assertEquals(MediaConstants.TYPE_IMAGE, upload.getMediaContentType());
        assertTrue(upload.isTranscoded());
        assertTrue(Files.exists(expectedWebp));
        assertTrue(Files.exists(source));
        assertEquals(originalNasMediaUrl, media.getNasMediaUrl());
        assertEquals(originalFilename, media.getFilename());

        ArgumentCaptor<Media> updateCaptor = forClass(Media.class);
        verify(mediaService).updateMedia(updateCaptor.capture());
        assertNull(updateCaptor.getValue().getNasMediaUrl());
        assertNull(updateCaptor.getValue().getFilename());
        assertNotNull(media.getTranscodedMediaUrl());
        assertEquals(MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(expectedWebp.toString())), media.getTranscodedMediaUrl());
        assertEquals(sha256(source), media.getTranscodeSourceHash());
        assertEquals("webp-q82-max2400-v1", media.getTranscodeProfile());
        assertNotNull(media.getTranscodeTime());
    }

    @Test
    void prepareForShopifyUploadReusesCachedWebpWhenSourceHashAndProfileMatch() throws Exception {
        Path source = createImage("ABC001", "ABC001-2.png", "png", Color.BLUE);
        Path cached = source.getParent().resolve("_shopify_sync").resolve("ABC001-2.webp");
        Files.createDirectories(cached.getParent());
        Files.writeString(cached, "cached-webp");

        Media media = buildMedia(12L, "ABC001-2.png", source, MediaConstants.TYPE_IMAGE);
        media.setTranscodedMediaUrl(MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(cached.toString())));
        media.setTranscodeSourceHash(sha256(source));
        media.setTranscodeProfile("webp-q82-max2400-v1");
        long lastModified = Files.getLastModifiedTime(cached).toMillis();

        PreparedMediaUpload upload = transcodeService.prepareForShopifyUpload(media);

        assertEquals(cached.toAbsolutePath().normalize().toFile(), upload.getFile().getAbsoluteFile());
        assertEquals(lastModified, Files.getLastModifiedTime(cached).toMillis());
        assertTrue(Files.exists(source));
        assertEquals(MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(source.toString())), media.getNasMediaUrl());
        verify(mediaService, never()).updateMedia(any(Media.class));
    }

    @Test
    void prepareForShopifyUploadRebuildsCachedWebpWhenSourceHashChanged() throws Exception {
        Path source = createImage("ABC001", "ABC001-3.png", "png", Color.GREEN);
        Path cached = source.getParent().resolve("_shopify_sync").resolve("ABC001-3.webp");
        Files.createDirectories(cached.getParent());
        Files.writeString(cached, "old-cache");

        Media media = buildMedia(13L, "ABC001-3.png", source, MediaConstants.TYPE_IMAGE);
        media.setTranscodedMediaUrl(MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(cached.toString())));
        media.setTranscodeSourceHash("stale-hash");
        media.setTranscodeProfile("webp-q82-max2400-v1");

        PreparedMediaUpload upload = transcodeService.prepareForShopifyUpload(media);

        assertEquals(cached.toAbsolutePath().normalize().toFile(), upload.getFile().getAbsoluteFile());
        assertEquals(sha256(source), media.getTranscodeSourceHash());
        verify(mediaService).updateMedia(any(Media.class));
    }

    @Test
    void prepareForShopifyUploadAlsoNormalizesExistingWebpSourceIntoDerivedCache() throws Exception {
        Path png = createImage("ABC001", "webp-source.png", "png", Color.YELLOW);
        Media pngMedia = buildMedia(14L, "webp-source.png", png, MediaConstants.TYPE_IMAGE);
        PreparedMediaUpload firstUpload = transcodeService.prepareForShopifyUpload(pngMedia);

        Path sourceWebp = png.getParent().resolve("already.webp");
        Files.copy(firstUpload.getFile().toPath(), sourceWebp);
        Media webpMedia = buildMedia(15L, "already.webp", sourceWebp, MediaConstants.TYPE_IMAGE);

        PreparedMediaUpload upload = transcodeService.prepareForShopifyUpload(webpMedia);

        Path expectedWebp = sourceWebp.getParent().resolve("_shopify_sync").resolve("already.webp");
        assertEquals(expectedWebp.toAbsolutePath().normalize().toFile(), upload.getFile().getAbsoluteFile());
        assertEquals("image/webp", upload.getMimeType());
        assertTrue(upload.isTranscoded());
        verify(mediaService, times(2)).updateMedia(any(Media.class));
    }

    @Test
    void prepareForShopifyUploadLeavesVideoAndGifOnOriginalFile() throws Exception {
        Path video = createFile("ABC001", "clip.mp4", "video");
        PreparedMediaUpload videoUpload = transcodeService.prepareForShopifyUpload(
                buildMedia(21L, "clip.mp4", video, MediaConstants.TYPE_VIDEO));

        assertEquals(video.toAbsolutePath().normalize().toFile(), videoUpload.getFile().getAbsoluteFile());
        assertEquals("video/mp4", videoUpload.getMimeType());
        assertEquals(MediaConstants.TYPE_VIDEO, videoUpload.getMediaContentType());
        assertFalse(videoUpload.isTranscoded());

        Path gif = createFile("ABC001", "animated.gif", "gif");
        PreparedMediaUpload gifUpload = transcodeService.prepareForShopifyUpload(
                buildMedia(22L, "animated.gif", gif, MediaConstants.TYPE_IMAGE));

        assertEquals(gif.toAbsolutePath().normalize().toFile(), gifUpload.getFile().getAbsoluteFile());
        assertEquals("image/gif", gifUpload.getMimeType());
        assertEquals(MediaConstants.TYPE_IMAGE, gifUpload.getMediaContentType());
        assertFalse(gifUpload.isTranscoded());
        verify(mediaService, never()).updateMedia(any(Media.class));
    }

    private Path createImage(String folder, String filename, String format, Color color) throws Exception {
        Path file = tempDir.resolve("media").resolve(folder).resolve(filename);
        Files.createDirectories(file.getParent());
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 16, 16);
        graphics.dispose();
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    private Path createFile(String folder, String filename, String content) throws Exception {
        Path file = tempDir.resolve("media").resolve(folder).resolve(filename);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private Media buildMedia(Long mediaId, String filename, Path path, String mediaType) {
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setFilename(filename);
        media.setNasMediaUrl(MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(path.toString())));
        media.setMediaContentType(mediaType);
        return media;
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
    }
}
