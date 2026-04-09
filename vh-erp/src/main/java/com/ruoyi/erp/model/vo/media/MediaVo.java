package com.ruoyi.erp.model.vo.media;

import com.ruoyi.erp.model.domain.Media;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
/**
 * erp媒体Vo对象 erp_media
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class MediaVo implements Serializable
{
    private static final long serialVersionUID = 1L;

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

    /** 媒体类型:VIDEO/IMAGE/MODEL_3D/EXTERNAL_VIDEO */
    private String mediaContentType;


     /**
     * 对象转封装类
     *
     * @param media Media实体对象
     * @return MediaVo
     */
    public static MediaVo objToVo(Media media) {
        if (media == null) {
            return null;
        }
        MediaVo mediaVo = new MediaVo();
        BeanUtils.copyProperties(media, mediaVo);
        return mediaVo;
    }
}
