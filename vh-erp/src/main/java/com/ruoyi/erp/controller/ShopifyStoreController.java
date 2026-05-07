package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreEdit;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreInsert;
import com.ruoyi.erp.model.dto.shopifyStore.ShopifyStoreQuery;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyStoreVo;
import com.ruoyi.erp.service.IShopifyStoreService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shopify 店铺管理 Controller
 */
@RestController
@RequestMapping("/erp/store")
public class ShopifyStoreController extends BaseController {

    @Resource
    private IShopifyStoreService shopifyStoreService;

    /**
     * 查询 Shopify 店铺列表
     */
    @PreAuthorize("@ss.hasPermi('erp:store:list')")
    @GetMapping("/list")
    public TableDataInfo list(ShopifyStoreQuery query) {
        startPage();
        List<ShopifyStore> list = shopifyStoreService.selectShopifyStoreList(query);
        TableDataInfo table = getDataTable(list);
        table.setRows(shopifyStoreService.convertVoList(list));
        return table;
    }

    /**
     * 查询启用店铺下拉选项
     */
    @PreAuthorize("@ss.hasAnyPermi('erp:store:list,erp:product:push')")
    @GetMapping("/active")
    public AjaxResult active() {
        return success(shopifyStoreService.convertVoList(shopifyStoreService.selectActiveStores()));
    }

    /**
     * 导出 Shopify 店铺列表
     */
    @PreAuthorize("@ss.hasPermi('erp:store:export')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ShopifyStoreQuery query) {
        List<ShopifyStore> list = shopifyStoreService.selectShopifyStoreList(query);
        ExcelUtil<ShopifyStore> util = new ExcelUtil<>(ShopifyStore.class);
        util.exportExcel(response, list, "Shopify 店铺数据");
    }

    /**
     * 获取 Shopify 店铺详情
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}")
    public AjaxResult getInfo(@PathVariable("storeId") Long storeId) {
        return success(ShopifyStoreVo.objToVo(shopifyStoreService.selectByStoreId(storeId)));
    }

    /**
     * 新增 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:add')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ShopifyStoreInsert insert) {
        return toAjax(shopifyStoreService.insertShopifyStore(ShopifyStoreInsert.insertToObj(insert)));
    }

    /**
     * 修改 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:edit')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ShopifyStoreEdit edit) {
        return toAjax(shopifyStoreService.updateShopifyStore(ShopifyStoreEdit.editToObj(edit)));
    }

    /**
     * 删除 Shopify 店铺
     */
    @PreAuthorize("@ss.hasPermi('erp:store:remove')")
    @Log(title = "Shopify 店铺", businessType = BusinessType.DELETE)
    @DeleteMapping("/{storeIds}")
    public AjaxResult remove(@PathVariable Long[] storeIds) {
        return toAjax(shopifyStoreService.deleteShopifyStoreByStoreIds(storeIds));
    }

    /**
     * 测试 Shopify 店铺连接
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @PostMapping("/test/{storeId}")
    public AjaxResult testConnection(@PathVariable("storeId") Long storeId) {
        shopifyStoreService.testConnection(storeId);
        return success("连接成功");
    }

    /**
     * 拉取 Shopify 库存仓库
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}/locations")
    public AjaxResult locations(@PathVariable("storeId") Long storeId) {
        return success(shopifyStoreService.fetchLocations(storeId));
    }

    /**
     * 拉取 Shopify 发布渠道
     */
    @PreAuthorize("@ss.hasPermi('erp:store:query')")
    @GetMapping("/{storeId}/publications")
    public AjaxResult publications(@PathVariable("storeId") Long storeId) {
        return success(shopifyStoreService.fetchPublications(storeId));
    }
}
