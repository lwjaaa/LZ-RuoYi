package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
/**
 * erp媒体对象 erp_media
 *
 * @author lwj
 * @date 2026-03-26
 */
@TableName("erp_media")
@Data
public class Media implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    @TableId(value = "media_id", type = IdType.ASSIGN_ID)
    private Long mediaId;

    /** 关联商品ID */
    @Excel(name = "关联商品ID")
    private Long productId;

    /** Shopify媒体ID */
    @Excel(name = "Shopify媒体ID")
    private String shopifyMediaId;

    /** Shopify媒体URL */
    @Excel(name = "Shopify媒体URL")
    private String shopifyMediaUrl;

    /** shopify的暂存上传URL */
    @Excel(name = "shopify的暂存上传URL")
    private String stagedUploadUrl;

    /** 本地nas的媒体URL */
    @Excel(name = "本地nas的媒体URL")
    private String nasMediaUrl;

    /** 文件名 */
    @Excel(name = "文件名")
    private String filename;

    /** 临时存储文件名 */
    @TableField(exist = false)
    private String tempFilename;

    /** 替代文本 */
    @Excel(name = "替代文本")
    private String alt;

    /** 排序 */
    @Excel(name = "排序")
    private Integer position;

    /** 媒体类型:VIDEO/IMAGE/MODEL_3D/EXTERNAL_VIDEO */
    @Excel(name = "媒体类型")
    private String mediaContentType;

    /** $column.columnComment */
    private Date createTime;

    /** $column.columnComment */
    private Date updateTime;

    /** $column.columnComment */
    private String delFlag;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}
