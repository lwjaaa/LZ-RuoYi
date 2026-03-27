package com.ruoyi.erp.model.dto.image;

import java.util.Map;
import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.BeanUtils;
import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.erp.model.domain.Image;
/**
 * erp图片Query对象 erp_image
 *
 * @author lwj
 * @date 2026-03-24
 */
@Data
public class ImageQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联商品ID */
    private Long productId;

    /** 文件名 */
    private String filename;

    /** 替代文本 */
    private String alt;

    /** 排序 */
    private Long position;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param imageQuery 查询对象
     * @return Image
     */
    public static Image queryToObj(ImageQuery imageQuery) {
        if (imageQuery == null) {
            return null;
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageQuery, image);
        return image;
    }
}
