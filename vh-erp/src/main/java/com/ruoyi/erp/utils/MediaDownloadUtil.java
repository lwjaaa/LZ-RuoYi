package com.ruoyi.erp.utils;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 媒体文件下载工具类
 * 
 * @author lwj
 * @date 2026-04-18
 */
@Component
public class MediaDownloadUtil {
    
    private static final Logger log = LoggerFactory.getLogger(MediaDownloadUtil.class);
    
    /**
     * 媒体文件存储根路径
     */
    public static final String MEDIA_BASE_PATH = RuoYiConfig.getProfile() + "/media/";
    
    /**
     * 下载媒体文件列表并保存到SPU专属文件夹
     * 
     * @param spu 商品SPU
     * @param mediaUrlList 媒体文件URL列表
     * @return 下载成功的文件路径列表
     */
    public List<String> downloadMediaFiles(String spu, List<String> mediaUrlList) {
        long startTime = System.currentTimeMillis();
        List<String> downloadedFiles = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        if (StringUtils.isEmpty(spu) || mediaUrlList == null || mediaUrlList.isEmpty()) {
            log.warn("SPU或媒体文件列表为空，跳过下载");
            return downloadedFiles;
        }
        
        log.info("开始下载媒体文件，SPU: {}, 文件数量: {}", spu, mediaUrlList.size());
        
        try {
            // 创建SPU专属文件夹
            String spuFolderPath = createSpuFolder(spu);
            if (spuFolderPath == null) {
                log.error("创建SPU文件夹失败: {}", spu);
                return downloadedFiles;
            }
            
            for (int i = 0; i < mediaUrlList.size(); i++) {
                String mediaUrl = mediaUrlList.get(i);
                if (StringUtils.isEmpty(mediaUrl)) {
                    log.warn("第{}个媒体文件URL为空，跳过", i + 1);
                    failCount++;
                    continue;
                }
                
                try {
                    String savedFilePath = downloadAndSaveMediaFile(mediaUrl, spuFolderPath, i + 1);
                    if (savedFilePath != null) {
                        downloadedFiles.add(savedFilePath);
                        successCount++;
                        log.info("媒体文件下载成功: {} -> {}", mediaUrl, savedFilePath);
                    } else {
                        failCount++;
                        log.warn("媒体文件下载返回null: {}", mediaUrl);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("下载媒体文件失败: {}, 错误信息: {}", mediaUrl, e.getMessage());
                    log.debug("详细错误信息:", e);
                }
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("媒体文件下载完成，SPU: {}, 成功: {}个, 失败: {}个, 总耗时: {}ms", 
                    spu, successCount, failCount, duration);
            
            // 记录详细的下载统计信息
            if (failCount > 0) {
                log.warn("部分文件下载失败，成功率: {}/{} ({}%)", 
                        successCount, mediaUrlList.size(), 
                        (successCount * 100) / mediaUrlList.size());
            }
            
        } catch (Exception e) {
            log.error("下载媒体文件整体流程异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            log.debug("详细错误信息:", e);
        }
        
        return downloadedFiles;
    }
    
    /**
     * 创建SPU专属文件夹
     * 
     * @param spu 商品SPU
     * @return 文件夹路径，创建失败返回null
     */
    private String createSpuFolder(String spu) {
        try {
            // 清理SPU中的非法字符
            String safeSpu = spu.replaceAll("[^a-zA-Z0-9.-_]", "_");
            String folderPath = MEDIA_BASE_PATH + safeSpu + "/";
            
            File folder = new File(folderPath);
            if (!folder.exists()) {
                boolean created = folder.mkdirs();
                if (!created) {
                    log.error("创建SPU文件夹失败: {}", folderPath);
                    return null;
                }
                log.info("创建SPU文件夹成功: {}", folderPath);
            }
            
            return folderPath;
        } catch (Exception e) {
            log.error("创建SPU文件夹异常: {}, 错误信息: {}", spu, e.getMessage());
            return null;
        }
    }
    
    /**
     * 下载并保存单个媒体文件
     * 
     * @param mediaUrl 媒体文件URL
     * @param folderPath 保存文件夹路径
     * @param index 文件序号
     * @return 保存的文件路径，失败返回null
     */
    private String downloadAndSaveMediaFile(String mediaUrl, String folderPath, int index) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        
        try {
            // 创建URL连接
            URL url = new URL(mediaUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("媒体文件下载失败，HTTP状态码: {}, URL: {}", responseCode, mediaUrl);
                return null;
            }
            
            // 获取文件类型和扩展名
            String contentType = connection.getContentType();
            String fileExtension = getFileExtension(contentType, mediaUrl);
            
            // 生成文件名
            String fileName = generateFileName(index, fileExtension);
            String filePath = folderPath + fileName;
            
            // 下载文件
            inputStream = connection.getInputStream();
            Path targetPath = Paths.get(filePath);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 验证文件完整性
            if (isFileValid(targetPath)) {
                return filePath;
            } else {
                log.warn("文件完整性验证失败: {}", filePath);
                Files.deleteIfExists(targetPath);
                return null;
            }
            
        } catch (Exception e) {
            log.error("下载媒体文件异常: {}, 错误信息: {}", mediaUrl, e.getMessage());
            return null;
        } finally {
            // 关闭连接和流
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输入流异常: {}", e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param contentType Content-Type
     * @param mediaUrl 媒体文件URL
     * @return 文件扩展名
     */
    private String getFileExtension(String contentType, String mediaUrl) {
        // 根据Content-Type判断文件类型
        if (contentType != null) {
            if (contentType.startsWith("image/jpeg") || contentType.startsWith("image/jpg")) {
                return ".jpg";
            } else if (contentType.startsWith("image/png")) {
                return ".png";
            } else if (contentType.startsWith("image/gif")) {
                return ".gif";
            } else if (contentType.startsWith("image/webp")) {
                return ".webp";
            } else if (contentType.startsWith("video/mp4")) {
                return ".mp4";
            } else if (contentType.startsWith("video/avi")) {
                return ".avi";
            } else if (contentType.startsWith("video/mov")) {
                return ".mov";
            }
        }
        
        // 从URL中提取扩展名
        if (mediaUrl != null) {
            int lastDotIndex = mediaUrl.lastIndexOf('.');
            int lastSlashIndex = mediaUrl.lastIndexOf('/');
            if (lastDotIndex > lastSlashIndex && lastDotIndex < mediaUrl.length() - 1) {
                String ext = mediaUrl.substring(lastDotIndex);
                if (ext.length() <= 5) { // 扩展名通常不超过5个字符
                    return ext;
                }
            }
        }
        
        // 默认扩展名
        return ".dat";
    }
    
    /**
     * 生成文件名
     * 
     * @param index 文件序号
     * @param extension 文件扩展名
     * @return 文件名
     */
    private String generateFileName(int index, String extension) {
        return String.format("media_%02d_%s%s", index, 
                UUID.randomUUID().toString().substring(0, 8), extension);
    }
    
    /**
     * 验证文件完整性
     * 
     * @param filePath 文件路径
     * @return 是否有效
     */
    private boolean isFileValid(Path filePath) {
        try {
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filePath);
                return false;
            }
            
            // 检查是否是文件（非目录）
            if (!Files.isRegularFile(filePath)) {
                log.warn("路径不是文件: {}", filePath);
                return false;
            }
            
            // 检查文件大小
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                log.warn("文件大小为0: {}", filePath);
                return false;
            }
            
            // 检查文件大小是否合理（最小1KB，最大100MB）
            if (fileSize < 1024) {
                log.warn("文件大小过小: {}字节，可能下载不完整", fileSize);
                return false;
            }
            
            if (fileSize > 100 * 1024 * 1024) {
                log.warn("文件大小过大: {}字节，可能异常", fileSize);
                return false;
            }
            
            // 检查文件是否可读
            if (!Files.isReadable(filePath)) {
                log.warn("文件不可读: {}", filePath);
                return false;
            }
            
            // 检查文件最后修改时间（确保是新创建的文件）
            long lastModified = Files.getLastModifiedTime(filePath).toMillis();
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastModified > 300000) { // 5分钟
                log.warn("文件最后修改时间异常，可能不是新下载的文件: {}", filePath);
                return false;
            }
            
            // 验证常见文件类型的魔数（Magic Number）
            if (!validateFileMagicNumber(filePath)) {
                log.warn("文件类型验证失败: {}", filePath);
                return false;
            }
            
            log.debug("文件完整性验证通过: {}, 大小: {}字节", filePath, fileSize);
            return true;
        } catch (Exception e) {
            log.error("验证文件完整性异常: {}, 错误信息: {}", filePath, e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证文件魔数（Magic Number）
     * 
     * @param filePath 文件路径
     * @return 是否通过验证
     */
    private boolean validateFileMagicNumber(Path filePath) {
        try {
            byte[] buffer = new byte[8];
            try (InputStream is = Files.newInputStream(filePath)) {
                int bytesRead = is.read(buffer);
                if (bytesRead < 8) {
                    log.warn("文件过小，无法读取魔数: {}", filePath);
                    return false;
                }
                
                // 检查常见图片文件类型
                if (isJpegFile(buffer)) return true;
                if (isPngFile(buffer)) return true;
                if (isGifFile(buffer)) return true;
                if (isWebPFile(buffer)) return true;
                
                // 检查常见视频文件类型
                if (isMp4File(buffer)) return true;
                
                // 如果无法识别文件类型，但文件大小合理，也认为是有效的
                log.debug("无法识别文件类型，但文件大小合理: {}", filePath);
                return true;
            }
        } catch (Exception e) {
            log.warn("验证文件魔数异常: {}, 错误信息: {}", filePath, e.getMessage());
            return false; // 验证失败时保守处理
        }
    }
    
    /**
     * 检查JPEG文件
     */
    private boolean isJpegFile(byte[] buffer) {
        return (buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xD8 && 
                buffer[2] == (byte) 0xFF && (buffer[3] == (byte) 0xE0 || buffer[3] == (byte) 0xE1));
    }
    
    /**
     * 检查PNG文件
     */
    private boolean isPngFile(byte[] buffer) {
        return (buffer[0] == (byte) 0x89 && buffer[1] == (byte) 0x50 && 
                buffer[2] == (byte) 0x4E && buffer[3] == (byte) 0x47);
    }
    
    /**
     * 检查GIF文件
     */
    private boolean isGifFile(byte[] buffer) {
        return (buffer[0] == (byte) 0x47 && buffer[1] == (byte) 0x49 && 
                buffer[2] == (byte) 0x46 && buffer[3] == (byte) 0x38);
    }
    
    /**
     * 检查WebP文件
     */
    private boolean isWebPFile(byte[] buffer) {
        return (buffer[0] == (byte) 0x52 && buffer[1] == (byte) 0x49 && 
                buffer[2] == (byte) 0x46 && buffer[3] == (byte) 0x46 &&
                buffer[8] == (byte) 0x57 && buffer[9] == (byte) 0x45 && 
                buffer[10] == (byte) 0x42 && buffer[11] == (byte) 0x50);
    }
    
    /**
     * 检查MP4文件
     */
    private boolean isMp4File(byte[] buffer) {
        return (buffer[4] == (byte) 0x66 && buffer[5] == (byte) 0x74 && 
                buffer[6] == (byte) 0x79 && buffer[7] == (byte) 0x70) ||
               (buffer[0] == (byte) 0x00 && buffer[1] == (byte) 0x00 && 
                buffer[2] == (byte) 0x00 && (buffer[3] == (byte) 0x18 || buffer[3] == (byte) 0x20) &&
                buffer[4] == (byte) 0x66 && buffer[5] == (byte) 0x74 && 
                buffer[6] == (byte) 0x79 && buffer[7] == (byte) 0x70);
    }
    
    /**
     * 验证已下载文件的完整性和正确性
     * 
     * @param spu 商品SPU
     * @return 验证结果
     */
    public boolean validateDownloadedFiles(String spu) {
        try {
            if (StringUtils.isEmpty(spu)) {
                log.warn("SPU为空，无法验证文件");
                return false;
            }
            
            // 清理SPU中的非法字符
            String safeSpu = spu.replaceAll("[^a-zA-Z0-9.-_]", "_");
            String folderPath = MEDIA_BASE_PATH + safeSpu + "/";
            
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                log.warn("SPU文件夹不存在: {}", folderPath);
                return false;
            }
            
            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                log.warn("SPU文件夹为空: {}", folderPath);
                return false;
            }
            
            int validCount = 0;
            int invalidCount = 0;
            
            log.info("开始验证SPU文件夹中的文件，路径: {}", folderPath);
            
            for (File file : files) {
                if (file.isFile()) {
                    Path filePath = file.toPath();
                    if (isFileValid(filePath)) {
                        validCount++;
                        log.debug("文件验证通过: {}", file.getName());
                    } else {
                        invalidCount++;
                        log.warn("文件验证失败: {}", file.getName());
                    }
                }
            }
            
            log.info("文件验证完成，SPU: {}, 有效文件: {}个, 无效文件: {}个, 总数: {}个", 
                    spu, validCount, invalidCount, files.length);
            
            // 如果所有文件都有效，返回true；否则返回false
            boolean isValid = invalidCount == 0 && validCount > 0;
            if (!isValid) {
                log.warn("文件验证失败，存在无效文件或文件夹为空");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("验证下载文件异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            log.debug("详细错误信息:", e);
            return false;
        }
    }
}