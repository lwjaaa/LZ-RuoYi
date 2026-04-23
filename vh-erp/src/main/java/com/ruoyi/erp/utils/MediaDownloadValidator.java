package com.ruoyi.erp.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 媒体文件下载验证器
 * 用于验证文件保存的完整性和正确性
 * 
 * @author lwj
 * @date 2026-04-19
 */
@Component
public class MediaDownloadValidator {
    
    private static final Logger log = LoggerFactory.getLogger(MediaDownloadValidator.class);
    
    /**
     * 验证媒体文件下载功能的完整性和正确性
     * 
     * @param spu 商品SPU
     * @param expectedFileCount 预期文件数量
     * @return 验证结果
     */
    public ValidationResult validateMediaDownload(String spu, int expectedFileCount) {
        ValidationResult result = new ValidationResult();
        result.setSpu(spu);
        result.setExpectedFileCount(expectedFileCount);
        
        try {
            // 验证文件夹结构
            boolean folderValid = validateFolderStructure(spu);
            result.setFolderStructureValid(folderValid);
            
            // 验证文件完整性
            boolean filesValid = validateFilesIntegrity(spu);
            result.setFilesIntegrityValid(filesValid);
            
            // 验证文件数量
            int actualFileCount = getActualFileCount(spu);
            result.setActualFileCount(actualFileCount);
            result.setFileCountValid(actualFileCount == expectedFileCount);
            
            // 计算总体验证结果
            boolean overallValid = folderValid && filesValid && 
                                 (actualFileCount == expectedFileCount);
            result.setOverallValid(overallValid);
            
            log.info("媒体文件下载验证完成，SPU: {}, 结果: {}", spu, overallValid ? "通过" : "失败");
            
        } catch (Exception e) {
            log.error("媒体文件下载验证异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            result.setOverallValid(false);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 验证文件完整性
     */
    private boolean validateFilesIntegrity(String spu) {
        try {
            // 清理SPU中的非法字符
            String safeSpu = spu.replaceAll("[^a-zA-Z0-9.-_]", "_");
            String folderPath = MediaDownloadUtil.MEDIA_BASE_PATH + "products/" + safeSpu + "/";
            
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                return false;
            }
            
            File[] files = folder.listFiles(File::isFile);
            if (files == null || files.length == 0) {
                return false;
            }
            
            // 检查每个文件是否可读且大小大于0
            for (File file : files) {
                if (!file.canRead() || file.length() == 0) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("验证文件完整性异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证文件夹结构
     */
    private boolean validateFolderStructure(String spu) {
        try {
            // 清理SPU中的非法字符
            String safeSpu = spu.replaceAll("[^a-zA-Z0-9.-_]", "_");
            String folderPath = MediaDownloadUtil.MEDIA_BASE_PATH + "products/" + safeSpu + "/";
            
            File folder = new File(folderPath);
            
            // 检查文件夹是否存在
            if (!folder.exists()) {
                log.warn("SPU文件夹不存在: {}", folderPath);
                return false;
            }
            
            // 检查是否是目录
            if (!folder.isDirectory()) {
                log.warn("SPU路径不是目录: {}", folderPath);
                return false;
            }
            
            // 检查文件夹是否可读
            if (!folder.canRead()) {
                log.warn("SPU文件夹不可读: {}", folderPath);
                return false;
            }
            
            // 检查文件夹是否可写
            if (!folder.canWrite()) {
                log.warn("SPU文件夹不可写: {}", folderPath);
                return false;
            }
            
            log.debug("文件夹结构验证通过: {}", folderPath);
            return true;
            
        } catch (Exception e) {
            log.error("验证文件夹结构异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取实际文件数量
     */
    private int getActualFileCount(String spu) {
        try {
            // 清理SPU中的非法字符
            String safeSpu = spu.replaceAll("[^a-zA-Z0-9.-_]", "_");
            String folderPath = MediaDownloadUtil.MEDIA_BASE_PATH + "products/" + safeSpu + "/";
            
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }
            
            File[] files = folder.listFiles(File::isFile);
            return files != null ? files.length : 0;
            
        } catch (Exception e) {
            log.error("获取实际文件数量异常，SPU: {}, 错误信息: {}", spu, e.getMessage());
            return 0;
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private String spu;
        private int expectedFileCount;
        private int actualFileCount;
        private boolean folderStructureValid;
        private boolean filesIntegrityValid;
        private boolean fileCountValid;
        private boolean overallValid;
        private String errorMessage;
        
        // Getters and Setters
        public String getSpu() { return spu; }
        public void setSpu(String spu) { this.spu = spu; }
        
        public int getExpectedFileCount() { return expectedFileCount; }
        public void setExpectedFileCount(int expectedFileCount) { this.expectedFileCount = expectedFileCount; }
        
        public int getActualFileCount() { return actualFileCount; }
        public void setActualFileCount(int actualFileCount) { this.actualFileCount = actualFileCount; }
        
        public boolean isFolderStructureValid() { return folderStructureValid; }
        public void setFolderStructureValid(boolean folderStructureValid) { this.folderStructureValid = folderStructureValid; }
        
        public boolean isFilesIntegrityValid() { return filesIntegrityValid; }
        public void setFilesIntegrityValid(boolean filesIntegrityValid) { this.filesIntegrityValid = filesIntegrityValid; }
        
        public boolean isFileCountValid() { return fileCountValid; }
        public void setFileCountValid(boolean fileCountValid) { this.fileCountValid = fileCountValid; }
        
        public boolean isOverallValid() { return overallValid; }
        public void setOverallValid(boolean overallValid) { this.overallValid = overallValid; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        @Override
        public String toString() {
            return String.format(
                "ValidationResult{spu='%s', expected=%d, actual=%d, folderValid=%s, " +
                "filesValid=%s, countValid=%s, overallValid=%s, error='%s'}",
                spu, expectedFileCount, actualFileCount, folderStructureValid,
                filesIntegrityValid, fileCountValid, overallValid, errorMessage
            );
        }
    }
}