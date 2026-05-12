package com.ruoyi.erp.model.vo.product;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductWorkbenchSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount;

    private Integer pendingPushCount;

    private Integer syncFailedCount;

    private Integer syncingCount;

    private Integer syncedCount;

    private Integer needResyncCount;

    private Integer incompleteCount;
}
