package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.TagDict;
import com.ruoyi.erp.model.dto.tagDict.TagDictQuery;
import com.ruoyi.erp.model.dto.tagDict.TreeDragDTO;
import com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo;
import com.ruoyi.erp.model.vo.tagDict.TagDictVo;
import jakarta.validation.Valid;

import java.util.List;
/**
 * erp标签Service接口
 * 
 * @author lwj
 * @date 2026-03-25
 */
public interface ITagDictService extends IService<TagDict>
{
    //region mybatis代码
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
     * 批量删除erp标签
     * 
     * @param tagIds 需要删除的erp标签主键集合
     * @return 结果
     */
    public int deleteTagDictByTagIds(Long[] tagIds);

    /**
     * 删除erp标签信息
     * 
     * @param tagId erp标签主键
     * @return 结果
     */
    public int deleteTagDictByTagId(Long tagId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param tagDictQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<TagDict> getQueryWrapper(TagDictQuery tagDictQuery);

    /**
     * 转换vo
     *
     * @param tagDictList TagDict集合
     * @return TagDictVO集合
     */
    List<TagDictVo> convertVoList(List<TagDict> tagDictList);

    /**
     * 导入erp标签数据
     *
     * @param tagDictList erp标签数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importTagDictData(List<TagDict> tagDictList, Boolean isUpdateSupport, String operName);

    /**
     * 查询标签菜单列表（树形结构，带缓存）
     *
     * @return java.util.List<com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo>
     * @author lwj
     **/
    List<TagDictMenuVo> getTagMenuList(String type);

    /**
     * 手动刷新菜单缓存
     */
    void refreshMenuCache();
    /**
     * 拖拽修改节点位置
     *
     * @param dto
     * @return void
     * @author lwj
     **/
    void dragNode(@Valid TreeDragDTO dto);
}
