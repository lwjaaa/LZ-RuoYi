package com.ruoyi.erp.model.dto.tagDict;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import com.ruoyi.erp.model.domain.TagDict;
/**
 * erp标签Vo对象 erp_tag_dict
 *
 * @author lwj
 * @date 2026-03-25
 */
@Data
public class TagDictEdit implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标签名称 */
    private String tagName;

    /** 标签编码 */
    private String tagCode;

    /** 标签类型 */
    private String tagType;

    /** 排序 */
    private Long sortOrder;

    /** 父级ID (0表示顶级菜单) */
    private Long parentId;

    /** 所有父级ID路径 (如: 0,10,25)，方便快速查询子树 */
    private String ancestors;

    /** 菜单层级 */
    private Long menuLevel;

    /** SPU 前缀 */
    private String spuPrefix;

    /** 当前最大流水号 */
    private Long currentMaxSeq;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    /**
     * 对象转封装类
     *
     * @param tagDictEdit 编辑对象
     * @return TagDict
     */
    public static TagDict editToObj(TagDictEdit tagDictEdit) {
        if (tagDictEdit == null) {
            return null;
        }
        TagDict tagDict = new TagDict();
        BeanUtils.copyProperties(tagDictEdit, tagDict);
        return tagDict;
    }
}
