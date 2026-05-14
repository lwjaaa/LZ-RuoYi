package com.ruoyi.erp.service.impl;

import com.ruoyi.erp.mapper.ShopifyTaskDetailMapper;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDetailVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticStatVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskDiagnosticsVo;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskErrorStatVo;
import com.ruoyi.erp.shopify.enums.ShopifyTaskDetailItemType;
import com.ruoyi.erp.shopify.enums.ShopifyTaskDetailStatus;
import com.ruoyi.erp.shopify.enums.ShopifyTaskStep;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifyTaskDetailServiceImplTest {

    @Test
    void selectTaskDiagnosticsAggregatesStatsTopErrorsAndRecentFailures() {
        ShopifyTaskDetailMapper mapper = mock(ShopifyTaskDetailMapper.class);
        ShopifyTaskDetailServiceImpl service = new ShopifyTaskDetailServiceImpl();
        ReflectionTestUtils.setField(service, "shopifyTaskDetailMapper", mapper);

        when(mapper.selectItemTypeStats(900L)).thenReturn(List.of(stat(ShopifyTaskDetailItemType.VARIANT.getCode(), 2L), stat(ShopifyTaskDetailItemType.MEDIA.getCode(), 1L)));
        when(mapper.selectStatusStats(900L)).thenReturn(List.of(stat(ShopifyTaskDetailStatus.FAILED.getCode(), 2L), stat(ShopifyTaskDetailStatus.PART_SUCCESS.getCode(), 1L)));
        when(mapper.selectFailedStepStats(900L)).thenReturn(List.of(stat(ShopifyTaskStep.VARIANT_CREATE.getCode(), 2L)));
        when(mapper.selectTopErrors(900L, 5)).thenReturn(List.of(error("INVALID", "variants.1.sku", "SKU 已存在", 2L)));
        when(mapper.selectRecentFailures(900L, 10)).thenReturn(List.of(detail(501L, ShopifyTaskDetailItemType.VARIANT.getCode(), ShopifyTaskDetailStatus.FAILED.getCode())));

        ShopifyTaskDiagnosticsVo diagnostics = service.selectTaskDiagnostics(900L);

        assertEquals(900L, diagnostics.getTaskId());
        assertEquals(ShopifyTaskDetailItemType.VARIANT.getCode(), diagnostics.getItemTypeStats().get(0).getName());
        assertEquals(2L, diagnostics.getItemTypeStats().get(0).getTotal());
        assertEquals(ShopifyTaskDetailStatus.FAILED.getCode(), diagnostics.getStatusStats().get(0).getName());
        assertEquals(ShopifyTaskStep.VARIANT_CREATE.getCode(), diagnostics.getFailedStepStats().get(0).getName());
        assertEquals("SKU 已存在", diagnostics.getTopErrors().get(0).getErrorMessage());

        List<ShopifyTaskDetailVo> recentFailures = diagnostics.getRecentFailures();
        assertEquals(1, recentFailures.size());
        assertEquals(501L, recentFailures.get(0).getDetailId());
        assertEquals(ShopifyTaskDetailItemType.VARIANT.getCode(), recentFailures.get(0).getItemType());

        verify(mapper).selectTopErrors(900L, 5);
        verify(mapper).selectRecentFailures(900L, 10);
    }

    @Test
    void selectTaskDiagnosticsUsesEmptyCollectionsWhenThereAreNoDetails() {
        ShopifyTaskDetailMapper mapper = mock(ShopifyTaskDetailMapper.class);
        ShopifyTaskDetailServiceImpl service = new ShopifyTaskDetailServiceImpl();
        ReflectionTestUtils.setField(service, "shopifyTaskDetailMapper", mapper);

        ShopifyTaskDiagnosticsVo diagnostics = service.selectTaskDiagnostics(901L);

        assertEquals(901L, diagnostics.getTaskId());
        assertNotNull(diagnostics.getItemTypeStats());
        assertNotNull(diagnostics.getStatusStats());
        assertNotNull(diagnostics.getFailedStepStats());
        assertNotNull(diagnostics.getTopErrors());
        assertNotNull(diagnostics.getRecentFailures());
        assertTrue(diagnostics.getItemTypeStats().isEmpty());
        assertTrue(diagnostics.getRecentFailures().isEmpty());
    }

    private ShopifyTaskDiagnosticStatVo stat(String name, Long total) {
        ShopifyTaskDiagnosticStatVo stat = new ShopifyTaskDiagnosticStatVo();
        stat.setName(name);
        stat.setTotal(total);
        return stat;
    }

    private ShopifyTaskErrorStatVo error(String code, String field, String message, Long total) {
        ShopifyTaskErrorStatVo error = new ShopifyTaskErrorStatVo();
        error.setErrorCode(code);
        error.setErrorField(field);
        error.setErrorMessage(message);
        error.setTotal(total);
        return error;
    }

    private ShopifyTaskDetail detail(Long detailId, String itemType, String status) {
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        detail.setDetailId(detailId);
        detail.setTaskId(900L);
        detail.setItemType(itemType);
        detail.setStatus(status);
        detail.setItemName("SKU-RED");
        detail.setErrorMessage("SKU 已存在");
        return detail;
    }
}
