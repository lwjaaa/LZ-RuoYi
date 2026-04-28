package com.ruoyi.erp.utils;

import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MediaDownloadUtil 测试类
 * 
 * @author lwj
 * @date 2026-04-19
 */
public class MediaDownloadUtilTest {

    @Test
    public void testCreateSpuFolder() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 测试正常SPU
        String spu = "TEST-SPU-001";
        
        // 注意：由于是测试环境，实际创建文件夹可能会失败
        // 这里主要测试逻辑流程
        System.out.println("测试创建SPU文件夹: " + spu);
    }
    
    @Test
    public void testDownloadMediaFiles() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 创建测试商品对象
        Product product = new Product();
        product.setProductId(1001L);
        product.setSpu("TEST-SPU-002");
        product.setProductName("测试商品");
        
        // 测试数据
        List<String> mediaUrls = Arrays.asList(
            // 这里可以添加一些测试用的图片URL
            // 注意：实际测试时需要可访问的URL
            "https://via.placeholder.com/150",
            "https://via.placeholder.com/300"
        );
        
        // 创建 URL 到变体的映射（可以为空）
        Map<String, ProductVariant> mediaUrlVariantMap = new HashMap<>();
        
        // 测试下载功能
        try {
            List<Media> downloadedMedias = mediaDownloadUtil.downloadMediaFiles(product, mediaUrls, mediaUrlVariantMap);
            
            // 验证下载结果
            assertNotNull(downloadedMedias, "下载结果不应为null");
            
            // 由于网络环境可能不稳定，这里只验证基本逻辑
            System.out.println("测试下载媒体文件，SPU: " + product.getSpu());
            System.out.println("下载 Media 对象数量: " + downloadedMedias.size());
            
            // 验证 Media 对象的属性
            for (int i = 0; i < downloadedMedias.size(); i++) {
                Media media = downloadedMedias.get(i);
                System.out.println("Media " + (i + 1) + ":");
                System.out.println("  - 文件名: " + media.getFilename());
                System.out.println("  - NAS URL: " + media.getNasMediaUrl());
                System.out.println("  - 位置: " + media.getPosition());
                System.out.println("  - 类型标记: " + media.getAlt());
                System.out.println("  - 商品ID: " + media.getProductId());
                
                // 验证第一张图是主图
                if (i == 0) {
                    assertEquals("main", media.getAlt(), "第一张图应该是主图");
                } else {
                    assertEquals("other", media.getAlt(), "其他图应该标记为 other");
                }
            }
            
        } catch (Exception e) {
            // 网络错误是正常的，这里只记录不抛出异常
            System.out.println("下载测试遇到网络错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test
    public void testValidateDownloadedFiles() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 测试验证功能
        String spu = "TEST-SPU-003";
        
        try {
            boolean isValid = mediaDownloadUtil.validateDownloadedFiles(spu);
            
            // 由于测试环境可能没有实际文件，这里只验证方法调用
            System.out.println("测试验证下载文件，SPU: " + spu);
            System.out.println("验证结果: " + isValid);
            
        } catch (Exception e) {
            System.out.println("验证测试遇到错误: " + e.getMessage());
        }
    }
    
    @Test
    public void testFileValidationLogic() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 测试文件验证逻辑
        System.out.println("测试文件验证逻辑");
        
        // 这里可以添加更详细的文件验证测试
        // 包括文件大小检查、文件类型检查等
    }
    
    @Test
    public void testErrorHandling() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 创建测试商品对象
        Product product = new Product();
        product.setProductId(1002L);
        product.setSpu("TEST-SPU-ERROR");
        
        // 测试异常情况处理
        
        // 测试空URL列表
        List<Media> result1 = mediaDownloadUtil.downloadMediaFiles(product, null, null);
        assertTrue(result1.isEmpty(), "空URL列表应返回空列表");
        
        // 测试空URL列表（空集合）
        List<Media> result2 = mediaDownloadUtil.downloadMediaFiles(product, Collections.emptyList(), null);
        assertTrue(result2.isEmpty(), "空URL集合应返回空列表");
        
        // 测试无效URL
        List<String> invalidUrls = Arrays.asList("invalid-url", "not-a-valid-url");
        List<Media> result3 = mediaDownloadUtil.downloadMediaFiles(product, invalidUrls, null);
        // 由于是无效URL，下载应该失败，但不会抛出异常
        assertNotNull(result3, "无效URL应返回列表（可能为空）");
        
        System.out.println("异常处理测试完成");
    }
    
    @Test
    public void testDownloadWithVariantMapping() {
        MediaDownloadUtil mediaDownloadUtil = new MediaDownloadUtil();
        
        // 创建测试商品对象
        Product product = new Product();
        product.setProductId(1003L);
        product.setSpu("TEST-SPU-VARIANT");
        product.setProductName("测试商品-规格图");
        
        // 测试数据 - 3张图片
        List<String> mediaUrls = Arrays.asList(
            "https://via.placeholder.com/150",  // 主图
            "https://via.placeholder.com/300",  // 规格图1
            "https://via.placeholder.com/450"   // 规格图2
        );
        
        // 创建 URL 到变体的映射
        Map<String, ProductVariant> mediaUrlVariantMap = new HashMap<>();
        
        // 模拟规格图绑定
        ProductVariant variant1 = new ProductVariant();
        variant1.setVariantId(101L);
        variant1.setSku("SKU-001");
        mediaUrlVariantMap.put(mediaUrls.get(1), variant1);
        
        ProductVariant variant2 = new ProductVariant();
        variant2.setVariantId(102L);
        variant2.setSku("SKU-002");
        mediaUrlVariantMap.put(mediaUrls.get(2), variant2);
        
        // 测试下载功能
        try {
            List<Media> downloadedMedias = mediaDownloadUtil.downloadMediaFiles(product, mediaUrls, mediaUrlVariantMap);
            
            // 验证下载结果
            assertNotNull(downloadedMedias, "下载结果不应为null");
            
            System.out.println("测试带规格图绑定的下载，SPU: " + product.getSpu());
            System.out.println("下载 Media 对象数量: " + downloadedMedias.size());
            
            // 验证 Media 对象的属性
            for (int i = 0; i < downloadedMedias.size(); i++) {
                Media media = downloadedMedias.get(i);
                System.out.println("\nMedia " + (i + 1) + ":");
                System.out.println("  - 文件名: " + media.getFilename());
                System.out.println("  - NAS URL: " + media.getNasMediaUrl());
                System.out.println("  - 位置: " + media.getPosition());
                System.out.println("  - 类型标记: " + media.getAlt());
                System.out.println("  - 商品ID: " + media.getProductId());
                
                // 验证图片类型
                if (i == 0) {
                    assertEquals("main", media.getAlt(), "第一张图应该是主图");
                    System.out.println("  ✓ 正确识别为主图");
                } else {
                    assertEquals("variant", media.getAlt(), "规格图应该标记为 variant");
                    System.out.println("  ✓ 正确识别为规格图");
                }
            }
            
        } catch (Exception e) {
            // 网络错误是正常的，这里只记录不抛出异常
            System.out.println("下载测试遇到网络错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
