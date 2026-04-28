package com.ruoyi.erp.utils;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.MediaConstants;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ProductVariantOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/24 15:32
 **/
@Slf4j
public class MediaFileUtil {


    /**
     * 将文件路径转换为可访问的 URL
     */
    public static String fixFileSeparator(String filePath) {
        return StringUtils.isBlank(filePath) ? "" : filePath.replace("\\", "/");
    }

    /**
     * 将文件路径转换为可访问的 URL
     */
    public static String generateNasUrl(File file, String dirPath) {
        String profile = RuoYiConfig.getProfile();
        String absolutePath = file.getAbsolutePath();

        // 将本地路径转换为资源访问路径
        if (absolutePath.startsWith(profile)) {
            return Constants.RESOURCE_PREFIX + absolutePath.substring(profile.length());
        }
        return Constants.RESOURCE_PREFIX + "/media/" + dirPath + "/" + file.getName();
    }

    /**
     * 将文件路径转换为 URL
     *
     * @param filePath 文件绝对路径
     * @return NAS 媒体 URL
     */
    public static String generateNasUrl(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return null;
        }

        String profile = RuoYiConfig.getProfile();
        if (filePath.startsWith(profile)) {
            String relativePath = filePath.substring(profile.length());
            return Constants.RESOURCE_PREFIX + relativePath;
        }

        return filePath;
    }

    /**
     * 根据文件名判断媒体类型
     */
    public static String getMediaType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp") || lowerName.endsWith(".bmp")) {
            return MediaConstants.TYPE_IMAGE;
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
                lowerName.endsWith(".mov") || lowerName.endsWith(".wmv") ||
                lowerName.endsWith(".flv") || lowerName.endsWith(".mkv")) {
            return MediaConstants.TYPE_VIDEO;
        }
        return MediaConstants.TYPE_UNKNOWN;
    }

    public static String concatFilename(String filename, String fileExtension) {
        // 如果后缀有小数点
        if (fileExtension.contains(".")) {
            return filename + fileExtension;
        }
        return filename + "." + fileExtension;
    }

    /**
     * 获取主媒体文件名
     *
     * @param filenamePrefix 文件名前缀，优先为SPU，其次为商品ID
     * @param fileExtension  文件拓展名
     * @return 新文件名
     */
    public static String getMainMediaFilename(String filenamePrefix, String fileExtension) {
        return concatFilename(filenamePrefix, fileExtension);
    }

    /**
     * 获取其他媒体文件名
     *
     * @param filenamePrefix 文件名前缀，优先为SPU，其次为商品ID
     * @param sequence       序列号
     * @param fileExtension  文件拓展名
     * @return 新文件名
     */
    public static String getOtherMediaFilename(String filenamePrefix, int sequence, String fileExtension) {
        // 如果后缀有小数点
        return concatFilename(String.format("%s-%d", filenamePrefix, sequence), fileExtension);
    }


    /**
     * 生成变体媒体文件名
     * <p>
     * 有SKU优先使用格式1：SKU.扩展名
     * 规格为空使用格式2：SPU-DEFAULT.扩展名
     * 默认格式3：SPU_规格值1_规格值2...扩展名
     * 例如：ABC0001_Red_L.jpg
     * </p>
     *
     * @param filenamePrefix 文件名前缀，优先为SPU，其次为商品ID
     * @param variant        变体对象
     * @param fileExtension  文件拓展名
     * @return 新文件名
     */
    public static String getVariantMediaFilename(String filenamePrefix, ProductVariant variant, String fileExtension) {
        String sku = variant.getSku();
        if (StringUtils.isNotEmpty(sku)) {
            return concatFilename(sku, fileExtension);
        }

        // 解析选项值
        String optionValuesJson = variant.getOptionValues();
        if (StringUtils.isEmpty(optionValuesJson)) {
            return concatFilename(filenamePrefix + "-DEFAULT", fileExtension);
        }

        List<ProductVariantOption> optionValueList = JSON.parseArray(
                optionValuesJson, ProductVariantOption.class);

        if (CollectionUtils.isEmpty(optionValueList)) {
            return concatFilename(filenamePrefix + "-DEFAULT", fileExtension);
        }

        // 构建文件名：SPU_规格值1_规格值2...
        StringBuilder filenameBuilder = new StringBuilder(filenamePrefix);
        for (ProductVariantOption option : optionValueList) {
            // 优先使用英文值，如果没有则使用中文值
            String value = StringUtils.isNotEmpty(option.getEnglishValue())
                    ? option.getEnglishValue()
                    : option.getChineseValue();
            if (StringUtils.isNotEmpty(value)) {
                // 清理非法字符
                String cleanValue = value.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5.-_]", "_");
                filenameBuilder.append("_").append(cleanValue);
            }
        }

        // 添加扩展名
        return concatFilename(filenameBuilder.toString(), fileExtension);

    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（不含点）
     */
    public static String getFileExtensionByFilename(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 获取文件扩展名
     *
     * @param contentType Content-Type
     * @param mediaUrl    媒体文件URL
     * @return 文件扩展名
     */
    public static String getFileExtensionByUrl(String contentType, String mediaUrl) {
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
}
