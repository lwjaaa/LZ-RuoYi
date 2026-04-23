package com.ruoyi.erp.controller;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.ProductSaveByExtentionEdit;
import com.ruoyi.erp.service.IProductWizardService;
import com.ruoyi.erp.utils.MediaDownloadUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * erp商品Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/api/erp/product")
public class ApiProductController extends BaseController {
    
    private static final Logger log = LoggerFactory.getLogger(ApiProductController.class);
    
    @Resource
    private IProductWizardService productWizardService;
    
    @Resource
    private MediaDownloadUtil mediaDownloadUtil;

    /**
     * 通过浏览器插件保存商品信息
     * 保存媒体文件列表信息，并下载文件保存到本地
     */
    @PostMapping("/selectionInfo")
    public AjaxResult saveSelectionInfo(@RequestBody ProductSaveByExtentionEdit productSaveByExtentionEdit) {
        log.info("处理浏览器插件商品保存请求:{}", productSaveByExtentionEdit);
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始处理浏览器插件商品保存请求，SPU: {}", productSaveByExtentionEdit.getSpu());
            
            // 下载媒体文件
            List<String> downloadedFiles = new ArrayList<>();
            if (productSaveByExtentionEdit.getMediaUrlList() != null && 
                !productSaveByExtentionEdit.getMediaUrlList().isEmpty()) {
                
                log.info("开始下载媒体文件，SPU: {}, 文件数量: {}", 
                        productSaveByExtentionEdit.getSpu(), 
                        productSaveByExtentionEdit.getMediaUrlList().size());
                
                downloadedFiles = mediaDownloadUtil.downloadMediaFiles(
                    productSaveByExtentionEdit.getSpu(), 
                    productSaveByExtentionEdit.getMediaUrlList()
                );
                
                log.info("媒体文件下载完成，成功下载: {}个文件", downloadedFiles.size());
            } else {
                log.info("无媒体文件需要下载，SPU: {}", productSaveByExtentionEdit.getSpu());
            }
            
            // 转换数据对象
            Product product = ProductSaveByExtentionEdit.editToObj(productSaveByExtentionEdit);
            
            // 将下载的文件路径设置到产品对象中
            if (!downloadedFiles.isEmpty()) {
                // 这里可以根据需要将文件路径保存到产品的相应字段中
                // 例如：product.setMediaPaths(downloadedFiles);
                log.info("已下载媒体文件路径: {}", downloadedFiles);
            }
            
            // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
            AjaxResult result = success(productWizardService.saveProductWithWizard(product, 1));
            
            long endTime = System.currentTimeMillis();
            log.info("浏览器插件商品保存完成，SPU: {}, 总耗时: {}ms", 
                    productSaveByExtentionEdit.getSpu(), endTime - startTime);
            
            return result;
            
        } catch (Exception e) {
            log.error("处理浏览器插件商品保存请求失败，SPU: {}, 错误信息: {}", 
                    productSaveByExtentionEdit.getSpu(), e.getMessage());
            log.debug("详细错误信息:", e);
            
            return error("商品保存失败: " + e.getMessage());
        }
    }

}
