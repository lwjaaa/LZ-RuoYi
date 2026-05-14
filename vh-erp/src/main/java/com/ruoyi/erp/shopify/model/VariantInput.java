package com.ruoyi.erp.shopify.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariantInput {
    private BigDecimal price;
    private String mediaId;
    private BigDecimal compareAtPrice;
    private String inventoryPolicy;
    private List<OptionValueInput> optionValues;
    private Boolean taxable;
    private InventoryItemInput inventoryItem;
    private List<InventoryQuantity> inventoryQuantities;
}
