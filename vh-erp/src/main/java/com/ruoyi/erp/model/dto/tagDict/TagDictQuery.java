package com.ruoyi.erp.model.dto.tagDict;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.erp.model.domain.TagDict;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
/**
 * erp标签Query对象 erp_tag_dict
 *
 * @author lwj
 * @date 2026-03-25
 */
@Data
public class TagDictQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标签名称 */
    private String tagName;

    /** 标签编码 */
    private String tagCode;

    /** 标签类型 */
    private String tagType;

    /** SPU 前缀 */
    private String spuPrefix;

    /** 父级ID (0表示顶级菜单) */
    private Long parentId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params;

    /**
     * 对象转封装类
     *
     * @param tagDictQuery 查询对象
     * @return TagDict
     */
    public static TagDict queryToObj(TagDictQuery tagDictQuery) {
        if (tagDictQuery == null) {
            return null;
        }
        TagDict tagDict = new TagDict();
        BeanUtils.copyProperties(tagDictQuery, tagDict);
        return tagDict;
    }
}
