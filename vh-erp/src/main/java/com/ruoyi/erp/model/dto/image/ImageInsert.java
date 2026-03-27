package com.ruoyi.erp.model.dto.image;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.Image;
/**
 * erp图片Vo对象 erp_image
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ImageInsert implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    private Long imageId;

    /** 关联商品ID */
    private Long productId;

    /** Shopify图片ID */
    private String shopifyImageId;

    /** 图片URL */
    private String shopifyUrl;

    /** shopify的暂存上传URL */
    private String stagedUploadUrl;

    /** 本地图片URL */
    private String nasUrl;

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

    /** $column.columnComment */
    private String delFlag;

    /**
     * 对象转封装类
     *
     * @param imageInsert 插入对象
     * @return ImageInsert
     */
    public static Image insertToObj(ImageInsert imageInsert) {
        if (imageInsert == null) {
            return null;
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageInsert, image);
        return image;
    }
}
