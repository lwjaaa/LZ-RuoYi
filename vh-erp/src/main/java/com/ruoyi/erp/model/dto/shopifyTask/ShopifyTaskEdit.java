package com.ruoyi.erp.model.dto.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务编辑对象
 */
@Data
public class ShopifyTaskEdit implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String taskName;

    private String taskStatus;

    private Integer progress;

    private Integer totalCount;

    private Integer successCount;

    private Integer partialCount;

    private Integer failedCount;

    private String errorMessage;

    private Long executionTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public static ShopifyTask editToObj(ShopifyTaskEdit edit) {
        if (edit == null) {
            return null;
        }
        ShopifyTask task = new ShopifyTask();
        BeanUtils.copyProperties(edit, task);
        return task;
    }
}
