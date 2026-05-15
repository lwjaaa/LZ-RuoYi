package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.service.IShopifyProductImportService;
import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测试接口
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "测试接口, 无业务逻辑，仅开发调试使用")
public class ApiTestController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ApiTestController.class);

    @Resource
    private ShopifyGraphQLClient shopifyGraphQLClient;

    @Resource
    private IShopifyProductImportService shopifyProductImportService;

    @PostMapping("/downloadBulkJsonlProducts")
    @Operation(summary = "下载并解析 Bulk JSONL", description = "下载并解析 Bulk JSONL")
    public AjaxResult downloadBulkJsonlProducts(String url) {


        List<ShopifyImportedProduct> products = shopifyGraphQLClient.downloadBulkJsonlProductsWithAgent(url);
        return AjaxResult.success(products.size());
    }
    @PostMapping("/parseLocalJsonlProducts")
    @Operation(summary = "读取文件并解析 Bulk JSONL", description = "读取文件并解析 Bulk JSONL")
    public AjaxResult parseLocalJsonlProducts(String filePath) {
        shopifyProductImportService.executeLocalFile(filePath);
        return AjaxResult.success();
    }
}
