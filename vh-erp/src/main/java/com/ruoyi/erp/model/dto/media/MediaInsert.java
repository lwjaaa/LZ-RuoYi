package com.ruoyi.erp.model.dto.media;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.Media;
/**
 * erp媒体Vo对象 erp_media
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class MediaInsert implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long mediaId;

    /** 关联商品ID */
    private Long productId;

    /** Shopify媒体ID */
    private String shopifyMediaId;

    /** Shopify媒体URL */
    private String shopifyMediaUrl;

    /** shopify的暂存上传URL */
    private String stagedUploadUrl;

    /** 本地nas的媒体URL */
    private String nasMediaUrl;

    /** 文件名 */
    private String filename;

    /** 替代文本 */
    private String alt;

    /** 排序 */
    private Long position;

    /** $column.columnComment */
    private Date createTime;

    /** $column.columnComment */
    private Date updateTime;

    /**
     * 对象转封装类
     *
     * @param mediaInsert 插入对象
     * @return MediaInsert
     */
    public static Media insertToObj(MediaInsert mediaInsert) {
        if (mediaInsert == null) {
            return null;
        }
        Media media = new Media();
        BeanUtils.copyProperties(mediaInsert, media);
        return media;
    }
}
