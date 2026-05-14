package com.ruoyi.erp.model.vo.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务明细响应对象
 */
@Data
public class ShopifyTaskDetailVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long detailId;

    private Long taskId;

    private Long storeId;

    private String shopName;

    private Long productId;

    private String itemType;

    private Long itemId;

    private String itemName;

    private String shopifyId;

    private String step;

    private String status;

    private String errorCode;

    private String errorField;

    private Integer inputIndex;

    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public static ShopifyTaskDetailVo objToVo(ShopifyTaskDetail detail) {
        if (detail == null) {
            return null;
        }
        ShopifyTaskDetailVo vo = new ShopifyTaskDetailVo();
        BeanUtils.copyProperties(detail, vo);
        return vo;
    }
}
