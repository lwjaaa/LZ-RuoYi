package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.model.dto.shipping.ShippingFeeQuery;
import com.ruoyi.erp.model.vo.shipping.ShippingFeeVo;
import com.ruoyi.erp.service.IProductWizardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 4px物流运费查询Controller
 *
 * @author lwj
 * @date 2026-04-26
 */
@Slf4j
@RestController
@RequestMapping("/erp/shipping")
public class ShippingFeeController extends BaseController {

    @Resource
    private IProductWizardService productWizardService;

    /**
     * 查询运费
     * <p>
     * 根据包裹尺寸、重量和目的地查询4px物流运费
     * </p>
     *
     * @param shippingFeeQuery 运费查询参数
     * @return 运费信息
     */
    @PreAuthorize("@ss.hasPermi('erp:shipping:query')")
    @Log(title = "运费查询", businessType = BusinessType.OTHER)
    @PostMapping("/calculate")
    public AjaxResult calculateShipping(@RequestBody ShippingFeeQuery shippingFeeQuery) {
        log.info("收到运费查询请求，参数: {}", shippingFeeQuery);
        
        try {
            List<ShippingFeeVo> resultList = productWizardService.calculateShipping(shippingFeeQuery);
            return success(resultList);
        } catch (Exception e) {
            log.error("运费查询失败: {}", e.getMessage(), e);
            return error("运费查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询运费
     * <p>
     * 支持一次查询多个包裹的运费
     * </p>
     *
     * @param shippingFeeQueries 运费查询参数列表
     * @return 运费信息列表
     */
    @PreAuthorize("@ss.hasPermi('erp:shipping:query')")
    @Log(title = "批量运费查询", businessType = BusinessType.OTHER)
    @PostMapping("/batch-calculate")
    public AjaxResult batchCalculateShipping(@RequestBody ShippingFeeQuery[] shippingFeeQueries) {
        log.info("收到批量运费查询请求，数量: {}", shippingFeeQueries.length);
        
        try {
            List<List<ShippingFeeVo>> results = new java.util.ArrayList<>();
            for (int i = 0; i < shippingFeeQueries.length; i++) {
                List<ShippingFeeVo> singleResult = productWizardService.calculateShipping(shippingFeeQueries[i]);
                results.add(singleResult);
            }
            return success(results);
        } catch (Exception e) {
            log.error("批量运费查询失败: {}", e.getMessage(), e);
            return error("批量运费查询失败: " + e.getMessage());
        }
    }
}
