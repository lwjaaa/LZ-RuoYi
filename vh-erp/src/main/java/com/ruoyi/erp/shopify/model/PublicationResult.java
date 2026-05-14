package com.ruoyi.erp.shopify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发布结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicationResult {
    private String publicationId;
    private String channelId;
    private String channelName;
    private Boolean isPublished;
    private String publishDate;
}
