package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.erp.mapper.ShopifyTaskDetailMapper;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDetailVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticStatVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticsVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskErrorStatVo;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shopify 同步任务明细 Service 实现
 */
@Service
public class ShopifyTaskDetailServiceImpl extends ServiceImpl<ShopifyTaskDetailMapper, ShopifyTaskDetail>
        implements IShopifyTaskDetailService {

    private static final int TOP_ERROR_LIMIT = 5;

    private static final int RECENT_FAILURE_LIMIT = 10;

    @Resource
    private ShopifyTaskDetailMapper shopifyTaskDetailMapper;

    @Override
    public ShopifyTaskDetail selectShopifyTaskDetailByDetailId(Long detailId) {
        return shopifyTaskDetailMapper.selectShopifyTaskDetailByDetailId(detailId);
    }

    @Override
    public List<ShopifyTaskDetail> selectShopifyTaskDetailList(ShopifyTaskDetail detail) {
        return shopifyTaskDetailMapper.selectShopifyTaskDetailList(detail);
    }

    @Override
    public int insertShopifyTaskDetail(ShopifyTaskDetail detail) {
        detail.setCreateTime(DateUtils.getNowDate());
        return shopifyTaskDetailMapper.insertShopifyTaskDetail(detail);
    }

    @Override
    public int updateShopifyTaskDetail(ShopifyTaskDetail detail) {
        detail.setUpdateTime(DateUtils.getNowDate());
        return shopifyTaskDetailMapper.updateShopifyTaskDetail(detail);
    }

    @Override
    public ShopifyTaskDetail selectLatestProductDetail(Long productId, Long storeId) {
        return shopifyTaskDetailMapper.selectLatestProductDetail(productId, storeId);
    }

    @Override
    public ShopifyTaskDiagnosticsVo selectTaskDiagnostics(Long taskId) {
        ShopifyTaskDiagnosticsVo diagnostics = new ShopifyTaskDiagnosticsVo();
        diagnostics.setTaskId(taskId);
        diagnostics.setItemTypeStats(emptyIfNull(shopifyTaskDetailMapper.selectItemTypeStats(taskId)));
        diagnostics.setStatusStats(emptyIfNull(shopifyTaskDetailMapper.selectStatusStats(taskId)));
        diagnostics.setFailedStepStats(emptyIfNull(shopifyTaskDetailMapper.selectFailedStepStats(taskId)));
        diagnostics.setTopErrors(emptyIfNull(shopifyTaskDetailMapper.selectTopErrors(taskId, TOP_ERROR_LIMIT)));
        diagnostics.setRecentFailures(emptyIfNull(shopifyTaskDetailMapper.selectRecentFailures(taskId, RECENT_FAILURE_LIMIT))
                .stream()
                .map(ShopifyTaskDetailVo::objToVo)
                .collect(Collectors.toList()));
        return diagnostics;
    }

    private <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
