package com.ruoyi.erp.model.vo.tagDict;

import com.ruoyi.erp.model.domain.TagDict;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.List;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/3/25 16:17
 **/
@Data
public class TagDictMenuVo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标签主键 */
    private Long tagId;

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

    /** 子节点列表 */
    private List<TagDictMenuVo> children;


    /**
     * 将 TagDict 转换为 TagDictMenuVo
     *
     * @param tagDict 标签对象
     * @return TagDictMenuVo
     */
    public static TagDictMenuVo convertToMenuVo(TagDict tagDict) {
        if (tagDict == null) {
            return null;
        }
        TagDictMenuVo tagDictVo = new TagDictMenuVo();
        BeanUtils.copyProperties(tagDict, tagDictVo);
        return tagDictVo;
    }

}
