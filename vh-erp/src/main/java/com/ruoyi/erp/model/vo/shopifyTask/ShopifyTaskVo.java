package com.ruoyi.erp.model.vo.shopifyTask;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.ShopifyTask;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 同步任务响应对象
 */
@Data
public class ShopifyTaskVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long storeId;

    private String shopName;

    private String taskName;

    private String taskType;

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

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private String updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public static ShopifyTaskVo objToVo(ShopifyTask task) {
        if (task == null) {
            return null;
        }
        ShopifyTaskVo vo = new ShopifyTaskVo();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }
}
