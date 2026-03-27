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
 * erp商品与标签关联对象 erp_product_tag_rel
 *
 * @author lwj
 * @date 2026-03-24
 */
@TableName("erp_product_tag_rel")
@Data
public class ProductTagRel implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联主键 */
    @TableId(value = "rel_id", type = IdType.ASSIGN_ID)
    private Long relId;

    /** 商品主表ID */
    @Excel(name = "商品主表ID")
    private Long productId;

    /** 标签字典表ID */
    @Excel(name = "标签字典表ID")
    private Long tagId;

    /** 创建时间 */
    private Date createTime;

    /** 创建者 */
    private String createBy;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}
