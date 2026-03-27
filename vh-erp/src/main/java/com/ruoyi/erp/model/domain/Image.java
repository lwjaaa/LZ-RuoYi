package com.ruoyi.erp.model.domain;

import java.io.Serializable;
import java.util.Map;
import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.ruoyi.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * erp图片对象 erp_image
 *
 * @author lwj
 * @date 2026-03-24
 */
@TableName("erp_image")
@Data
public class Image implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 本地主键 */
    @TableId(value = "image_id", type = IdType.ASSIGN_ID)
    private Long imageId;

    /** 关联商品ID */
    @Excel(name = "关联商品ID")
    private Long productId;

    /** Shopify图片ID */
    @Excel(name = "Shopify图片ID")
    private String shopifyImageId;

    /** 图片URL */
    @Excel(name = "图片URL")
    private String shopifyUrl;

    /** shopify的暂存上传URL */
    @Excel(name = "shopify的暂存上传URL")
    private String stagedUploadUrl;

    /** 本地图片URL */
    @Excel(name = "本地图片URL")
    private String nasUrl;

    /** 文件名 */
    @Excel(name = "文件名")
    private String filename;

    /** 替代文本 */
    @Excel(name = "替代文本")
    private String alt;

    /** 排序 */
    @Excel(name = "排序")
    private Long position;

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
