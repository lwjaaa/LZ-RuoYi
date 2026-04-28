package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.dto.product.ProductSaveByExtentionEdit;
import com.ruoyi.erp.service.IProductWizardService;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * erp商品Controller - 浏览器插件接口
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

    /**
     * 通过浏览器插件保存商品信息
     */
    @PostMapping("/selectionInfo")
    public AjaxResult saveSelectionInfo(@RequestBody ProductSaveByExtentionEdit productSaveByExtentionEdit) {
        log.info("接收到浏览器插件商品保存请求: {}", productSaveByExtentionEdit);
        
        if (productSaveByExtentionEdit == null) {
            return error("请求数据不能为空");
        }
        
        try {
            Long productId = productWizardService.saveProductFromExtension(productSaveByExtentionEdit);
            return AjaxResult.success("商品保存成功", productId);
        } catch (Exception e) {
            log.error("商品保存失败: {}", e.getMessage(), e);
            return error("商品保存失败: " + e.getMessage());
        }
    }

    /**
     * 通过JSON文件批量保存商品信息
     */
    @PostMapping("/importJson")
    public AjaxResult importProductsFromJson(MultipartFile file) {
        log.info("接收到JSON文件批量导入请求");
        
        if (file == null || file.isEmpty()) {
            return error("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".json")) {
            return error("文件格式错误，仅支持JSON文件");
        }
        
        try {
            Integer successCount = productWizardService.saveProductsFromJsonFile(file);
            return AjaxResult.success("批量导入完成，成功保存" + successCount + "个商品", successCount);
        } catch (Exception e) {
            log.error("JSON文件批量导入失败: {}", e.getMessage(), e);
            return error("批量导入失败: " + e.getMessage());
        }
    }

}
