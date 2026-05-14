package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticStatVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskErrorStatVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Shopify 同步任务明细 Mapper
 */
public interface ShopifyTaskDetailMapper extends BaseMapper<ShopifyTaskDetail> {

    ShopifyTaskDetail selectShopifyTaskDetailByDetailId(Long detailId);

    List<ShopifyTaskDetail> selectShopifyTaskDetailList(ShopifyTaskDetail detail);

    int insertShopifyTaskDetail(ShopifyTaskDetail detail);

    int updateShopifyTaskDetail(ShopifyTaskDetail detail);

    ShopifyTaskDetail selectLatestProductDetail(@Param("productId") Long productId, @Param("storeId") Long storeId);

    List<ShopifyTaskDiagnosticStatVo> selectItemTypeStats(@Param("taskId") Long taskId);

    List<ShopifyTaskDiagnosticStatVo> selectStatusStats(@Param("taskId") Long taskId);

    List<ShopifyTaskDiagnosticStatVo> selectFailedStepStats(@Param("taskId") Long taskId);

    List<ShopifyTaskErrorStatVo> selectTopErrors(@Param("taskId") Long taskId, @Param("limit") Integer limit);

    List<ShopifyTaskDetail> selectRecentFailures(@Param("taskId") Long taskId, @Param("limit") Integer limit);
}
