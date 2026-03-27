package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.TagDict;

import java.util.List;

/**
 * erp标签Mapper接口
 * 
 * @author lwj
 * @date 2026-03-25
 */
public interface TagDictMapper extends BaseMapper<TagDict>
{
    /**
     * 查询erp标签
     * 
     * @param tagId erp标签主键
     * @return erp标签
     */
    public TagDict selectTagDictByTagId(Long tagId);

    /**
     * 查询erp标签列表
     * 
     * @param tagDict erp标签
     * @return erp标签集合
     */
    public List<TagDict> selectTagDictList(TagDict tagDict);

    /**
     * 新增erp标签
     * 
     * @param tagDict erp标签
     * @return 结果
     */
    public int insertTagDict(TagDict tagDict);

    /**
     * 修改erp标签
     * 
     * @param tagDict erp标签
     * @return 结果
     */
    public int updateTagDict(TagDict tagDict);

    /**
     * 删除erp标签
     * 
     * @param tagId erp标签主键
     * @return 结果
     */
    public int deleteTagDictByTagId(Long tagId);

    /**
     * 批量删除erp标签
     * 
     * @param tagIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTagDictByTagIds(Long[] tagIds);
}
