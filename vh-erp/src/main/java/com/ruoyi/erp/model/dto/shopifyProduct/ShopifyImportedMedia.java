package com.ruoyi.erp.model.dto.shopifyProduct;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShopifyImportedMedia implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String originalUrl;
    private String previewUrl;
    private String alt;
    private String mediaContentType;
    private Integer position;
}
