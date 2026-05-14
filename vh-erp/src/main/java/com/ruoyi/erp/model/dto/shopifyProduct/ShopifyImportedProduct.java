package com.ruoyi.erp.model.dto.shopifyProduct;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class ShopifyImportedProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String handle;
    private String descriptionHtml;
    private String productType;
    private String vendor;
    private List<String> tags = new ArrayList<>();
    private String status;
    private Date updatedAt;
    private String spu;
    private Map<String, String> seo;
    private List<ShopifyImportedVariant> variants = new ArrayList<>();
    private List<ShopifyImportedMedia> media = new ArrayList<>();
}
