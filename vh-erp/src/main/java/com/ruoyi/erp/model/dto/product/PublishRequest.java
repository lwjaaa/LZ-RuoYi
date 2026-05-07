package com.ruoyi.erp.model.dto.product;

import lombok.Data;

import java.io.Serializable;

/**
 * 商品发布到渠道请求对象
 */
@Data
public class PublishRequest implements Serializable {

    /** 要发布的商品ID列表 */
    private Long[] productIds;

    /** 店铺ID（可选，默认使用默认店铺） */
    private Long storeId;
}