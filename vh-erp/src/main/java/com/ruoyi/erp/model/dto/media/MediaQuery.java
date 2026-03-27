package com.ruoyi.erp.model.dto.media;

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
import com.ruoyi.erp.model.domain.Media;
/**
 * erp媒体Query对象 erp_media
 *
 * @author lwj
 * @date 2026-03-26
 */
@Data
public class MediaQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联商品ID */
    private Long productId;

    /** Shopify媒体ID */
    private String shopifyMediaId;

    /** 文件名 */
    private String filename;

    /** 替代文本 */
    private String alt;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param mediaQuery 查询对象
     * @return Media
     */
    public static Media queryToObj(MediaQuery mediaQuery) {
        if (mediaQuery == null) {
            return null;
        }
        Media media = new Media();
        BeanUtils.copyProperties(mediaQuery, media);
        return media;
    }
}
