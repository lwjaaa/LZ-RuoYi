package com.ruoyi.erp.model.vo.product;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商品发布结果
 */
@Data
public class PublishResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功的商品数量 */
    private int successCount;

    /** 失败的数量 */
    private int failedCount;

    /** 失败详情 */
    private List<ChannelPublishFailed> failedChannels;

    @Data
    public static class ChannelPublishFailed implements Serializable {
        private Long productId;
        private String channelId;
        private String channelName;
        private String error;
    }
}