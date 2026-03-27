package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
/**
 * erp标签对象 erp_tag_dict
 *
 * @author lwj
 * @date 2026-03-25
 */
@TableName("erp_tag_dict")
@Data
public class TagDict implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标签主键 */
    @TableId(value = "tag_id", type = IdType.ASSIGN_ID)
    private Long tagId;

    /** 标签名称 */
    @Excel(name = "标签名称")
    private String tagName;

    /** 标签编码 */
    @Excel(name = "标签编码")
    private String tagCode;

    /** 标签类型 */
    @Excel(name = "标签类型", dictType = "erp_tag_type")
    private String tagType;

    /** 排序 */
    @Excel(name = "排序")
    private Long sortOrder;

    /** 父级ID (0表示顶级菜单) */
    @Excel(name = "父级ID (0表示顶级菜单)")
    private Long parentId;

    /** 所有父级ID路径 (如: 0,10,25)，方便快速查询子树 */
    @Excel(name = "所有父级ID路径 (如: 0,10,25)，方便快速查询子树")
    private String ancestors;

    /** 菜单层级 */
    @Excel(name = "菜单层级")
    private Long menuLevel;

    /** SPU 前缀 */
    @Excel(name = "SPU 前缀")
    private String spuPrefix;

    /** 当前最大流水号 */
    @Excel(name = "当前最大流水号")
    private Long currentMaxSeq;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 删除标志 */
    private String delFlag;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;
}
