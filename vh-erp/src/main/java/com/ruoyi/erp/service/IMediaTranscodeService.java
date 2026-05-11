package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.vo.media.PreparedMediaUpload;

/**
 * Shopify 同步前的媒体派生文件准备服务。
 */
public interface IMediaTranscodeService {

    PreparedMediaUpload prepareForShopifyUpload(Media media);
}
