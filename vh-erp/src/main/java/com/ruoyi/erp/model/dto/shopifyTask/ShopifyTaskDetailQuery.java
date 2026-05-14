package com.ruoyi.erp.model.dto.shopifyTask;

import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * Shopify 同步任务明细查询对象
 */
@Data
public class ShopifyTaskDetailQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long productId;

    private String itemType;

    private String status;

    public static ShopifyTaskDetail queryToObj(ShopifyTaskDetailQuery query) {
        if (query == null) {
            return null;
        }
        ShopifyTaskDetail detail = new ShopifyTaskDetail();
        BeanUtils.copyProperties(query, detail);
        return detail;
    }
}
