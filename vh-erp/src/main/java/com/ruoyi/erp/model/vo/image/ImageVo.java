package com.ruoyi.erp.model.vo.image;

import java.io.Serializable;
import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.annotation.Excel;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.Image;
/**
 * erp图片Vo对象 erp_image
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ImageVo implements Serializable
{
    private static final long serialVersionUID = 1L;

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


     /**
     * 对象转封装类
     *
     * @param image Image实体对象
     * @return ImageVo
     */
    public static ImageVo objToVo(Image image) {
        if (image == null) {
            return null;
        }
        ImageVo imageVo = new ImageVo();
        BeanUtils.copyProperties(image, imageVo);
        return imageVo;
    }
}
