package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.ProductBaseInfoEdit;
import com.ruoyi.erp.model.dto.product.ProductSelectionEdit;
import com.ruoyi.erp.model.dto.productVariant.ShippingFeeQurey;
import com.ruoyi.erp.service.IProductService;
import com.ruoyi.erp.service.IProductWizardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/12 20:54
 **/
@Slf4j
@RestController
@RequestMapping("/erp/product/wizard")
public class ProductWizardController extends BaseController {
    @Resource
    private IProductService productService;
    @Resource
    private IProductWizardService productWizardService;


    /**
     * 编辑/新增 选品信息
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:add')")
    @Log(title = "编辑/新增 选品信息", businessType = BusinessType.INSERT)
    @PostMapping("/selectionInfo")
    public AjaxResult saveSelectionInfo(@RequestBody ProductSelectionEdit productSelectionEdit) {
        // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
        Product product = ProductSelectionEdit.editToObj(productSelectionEdit);
        return success(productWizardService.saveProductWithWizard(product,1));
    }

    /**
     * 编辑 商品其他信息16:03:02.718 16:03:02.914
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:product:edit')")
    @Log(title = "编辑 商品其他信息", businessType = BusinessType.UPDATE)
    @PostMapping("/baseInfo")
    public AjaxResult saveProductBaseInfo(@RequestBody ProductBaseInfoEdit productSelectionEdit) {
        // 事务控制：保存 SPU -> 更新 Tag 流水号 -> 保存 Variants -> 保存 Media
        Product product = ProductBaseInfoEdit.editToObj(productSelectionEdit);
        return success(productWizardService.saveProductWithWizard(product,2));
    }

    /**
     * 计算运费
     */
    @PostMapping("/calculateShipping")
    public AjaxResult calculateShipping(@RequestBody ShippingFeeQurey shippingFeeQurey) {
        return success(productWizardService.calculateShipping(shippingFeeQurey));
    }

    /**
     * 获取今日美国汇率
     */
    @GetMapping("/getUsdRate")
    public AjaxResult getUsdRate() {
        return success(productWizardService.getUsdRate());
    }
}
