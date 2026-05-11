package com.ruoyi.erp.model.vo.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * Shopify 媒体上传前的实际文件信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedMediaUpload {

    private File file;

    private String filename;

    private String mimeType;

    private String mediaContentType;

    private boolean transcoded;
}
