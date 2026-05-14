package com.ruoyi.erp.shopify.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductMetafield {
    /**
     * The unique ID of the metafield. Using namespace and key is preferred for creating and updating.
     */
    private String id;
    private String key;
    private String namespace;
    private String type;
    private String value;
}
