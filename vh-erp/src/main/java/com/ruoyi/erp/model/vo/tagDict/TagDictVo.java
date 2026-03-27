package com.ruoyi.erp.model.vo.tagDict;

import java.io.Serializable;
import java.util.Date;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.ruoyi.common.annotation.Excel;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.erp.model.domain.TagDict;
/**
 * erp标签Vo对象 erp_tag_dict
 *
 * @author lwj
 * @date 2026-03-25
 */
@Data
public class TagDictVo implements Serializable
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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    /** 备注 */
    private String remark;


     /**
     * 对象转封装类
     *
     * @param tagDict TagDict实体对象
     * @return TagDictVo
     */
    public static TagDictVo objToVo(TagDict tagDict) {
        if (tagDict == null) {
            return null;
        }
        TagDictVo tagDictVo = new TagDictVo();
        BeanUtils.copyProperties(tagDict, tagDictVo);
        return tagDictVo;
    }
}
