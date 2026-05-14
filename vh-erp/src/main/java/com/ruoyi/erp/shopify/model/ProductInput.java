package com.ruoyi.erp.shopify.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInput {
    private String id;
    private String title;
    private String bodyHtml;
    private String descriptionHtml;
    private List<ProductOptionInput> productOptions;
    private String productType;
    private String vendor;
    private String category;
    private SeoInput seo;
    private String status;
    private List<String> tags;
    private List<ProductMetafield> metafields;

    public static ProductInputBuilder builderFrom(ProductInput other) {
        return builder()
                .id(other.id)
                .title(other.title)
                .bodyHtml(other.bodyHtml)
                .descriptionHtml(other.descriptionHtml)
                .productOptions(other.productOptions)
                .productType(other.productType)
                .vendor(other.vendor)
                .category(other.category)
                .seo(other.seo)
                .metafields(other.metafields)
                .tags(other.tags);
    }
}
