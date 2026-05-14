package com.ruoyi.erp.model.dto.shopifyTask;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.ShopifyTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Shopify 同步任务查询对象
 */
@Data
public class ShopifyTaskQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long storeId;

    private String shopName;

    private String taskName;

    private String taskType;

    private String taskStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    public static ShopifyTask queryToObj(ShopifyTaskQuery query) {
        if (query == null) {
            return null;
        }
        ShopifyTask task = new ShopifyTask();
        BeanUtils.copyProperties(query, task);
        return task;
    }
}
