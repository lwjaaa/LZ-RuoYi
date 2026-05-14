package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticsVo;

import java.util.List;

/**
 * Shopify 同步任务明细 Service 接口
 */
public interface IShopifyTaskDetailService extends IService<ShopifyTaskDetail> {

    ShopifyTaskDetail selectShopifyTaskDetailByDetailId(Long detailId);

    List<ShopifyTaskDetail> selectShopifyTaskDetailList(ShopifyTaskDetail detail);

    int insertShopifyTaskDetail(ShopifyTaskDetail detail);

    int updateShopifyTaskDetail(ShopifyTaskDetail detail);

    ShopifyTaskDetail selectLatestProductDetail(Long productId, Long storeId);

    ShopifyTaskDiagnosticsVo selectTaskDiagnostics(Long taskId);
}
