package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.constant.TagConstants;
import com.ruoyi.erp.mapper.TagDictMapper;
import com.ruoyi.erp.model.domain.TagDict;
import com.ruoyi.erp.model.dto.tagDict.TagDictQuery;
import com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo;
import com.ruoyi.erp.model.vo.tagDict.TagDictVo;
import com.ruoyi.erp.service.ITagDictService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * erp标签Service业务层处理
 *
 * @author lwj
 * @date 2026-03-25
 */
@Service
public class TagDictServiceImpl extends ServiceImpl<TagDictMapper, TagDict> implements ITagDictService {


    @Resource
    private RedisCache redisCache;


    @Resource
    private TagDictMapper tagDictMapper;

    //region mybatis代码

    /**
     * 查询erp标签
     *
     * @param tagId erp标签主键
     * @return erp标签
     */
    @Override
    public TagDict selectTagDictByTagId(Long tagId) {
        return tagDictMapper.selectTagDictByTagId(tagId);
    }

    /**
     * 查询erp标签列表
     *
     * @param tagDict erp标签
     * @return erp标签
     */
    @Override
    public List<TagDict> selectTagDictList(TagDict tagDict) {
        return tagDictMapper.selectTagDictList(tagDict);
    }

    /**
     * 新增erp标签
     *
     * @param tagDict erp标签
     * @return 结果
     */
    @Override
    public int insertTagDict(TagDict tagDict) {
        tagDict.setCreateTime(DateUtils.getNowDate());
        return tagDictMapper.insertTagDict(tagDict);
    }

    /**
     * 修改erp标签
     *
     * @param tagDict erp标签
     * @return 结果
     */
    @Override
    public int updateTagDict(TagDict tagDict) {
        tagDict.setUpdateTime(DateUtils.getNowDate());
        return tagDictMapper.updateTagDict(tagDict);
    }

    /**
     * 批量删除erp标签
     *
     * @param tagIds 需要删除的erp标签主键
     * @return 结果
     */
    @Override
    public int deleteTagDictByTagIds(Long[] tagIds) {
        return tagDictMapper.deleteTagDictByTagIds(tagIds);
    }

    /**
     * 删除erp标签信息
     *
     * @param tagId erp标签主键
     * @return 结果
     */
    @Override
    public int deleteTagDictByTagId(Long tagId) {
        return tagDictMapper.deleteTagDictByTagId(tagId);
    }

    //endregion
    @Override
    public QueryWrapper<TagDict> getQueryWrapper(TagDictQuery tagDictQuery) {
        QueryWrapper<TagDict> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = tagDictQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        String tagName = tagDictQuery.getTagName();
        queryWrapper.like(StringUtils.isNotEmpty(tagName), "tag_name", tagName);

        String tagCode = tagDictQuery.getTagCode();
        queryWrapper.eq(StringUtils.isNotEmpty(tagCode), "tag_code", tagCode);

        String tagType = tagDictQuery.getTagType();
        queryWrapper.eq(StringUtils.isNotEmpty(tagType), "tag_type", tagType);

        String spuPrefix = tagDictQuery.getSpuPrefix();
        queryWrapper.like(StringUtils.isNotEmpty(spuPrefix), "spu_prefix", spuPrefix);

        Date createTime = tagDictQuery.getCreateTime();
        queryWrapper.eq(StringUtils.isNotNull(createTime), "create_time", createTime);

        return queryWrapper;
    }

    @Override
    public List<TagDictVo> convertVoList(List<TagDict> tagDictList) {
        if (StringUtils.isEmpty(tagDictList)) {
            return Collections.emptyList();
        }
        return tagDictList.stream().map(TagDictVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp标签数据
     *
     * @param tagDictList     erp标签数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importTagDictData(List<TagDict> tagDictList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isEmpty(tagDictList)) {
            throw new ServiceException("导入erp标签数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (TagDict tagDict : tagDictList) {
            try {
                // 验证是否存在这个erp标签
                Long tagId = tagDict.getTagId();
                TagDict tagDictExist = null;
                if (StringUtils.isNotNull(tagId)) {
                    tagDictExist = tagDictMapper.selectTagDictByTagId(tagId);
                }
                if (StringUtils.isNull(tagDictExist)) {
                    tagDict.setCreateTime(DateUtils.getNowDate());
                    tagDictMapper.insertTagDict(tagDict);
                    successNum++;
                    String tagIdStr = StringUtils.isNotNull(tagId) ? tagId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp标签 " + tagIdStr + " 导入成功");
                } else if (isUpdateSupport) {
                    tagDict.setUpdateTime(DateUtils.getNowDate());
                    tagDictMapper.updateTagDict(tagDict);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp标签 " + tagId.toString() + " 更新成功");
                } else {
                    failureNum++;
                    String tagIdStr = StringUtils.isNotNull(tagId) ? tagId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp标签 " + tagIdStr + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                Long tagId = tagDict.getTagId();
                String tagIdStr = StringUtils.isNotNull(tagId) ? tagId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp标签 " + tagIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }


    @Override
    public List<TagDictMenuVo> getTagMenuList(String type) {
        if (StringUtils.isNotEmpty(type)) {
            type = TagConstants.TYPE_ALL;
        }
        String cacheKey = CacheConstants.ERP_MENU_KEY + type;

        List<TagDictMenuVo> cacheResult = redisCache.getCacheObject(cacheKey);
        if (cacheResult != null && !cacheResult.isEmpty()) {
            return cacheResult;
        }
        // 从数据库查询标签菜单列表（树形结构）
        List<TagDictMenuVo> tree = selectTagMenuList(type);

//        redisCache.setCacheObject(cacheKey, tree, 30, TimeUnit.MINUTES);

        return tree;
    }

    /**
     * 清除菜单缓存
     */
    public void clearMenuCache() {
        redisCache.deleteObject(CacheConstants.ERP_MENU_KEY + TagConstants.TYPE_ALL);
        redisCache.deleteObject(CacheConstants.ERP_MENU_KEY + TagConstants.TYPE_MENU);
    }

    /**
     * 手动刷新菜单缓存
     */
    public void refreshMenuCache() {
        clearMenuCache();
        getTagMenuList(TagConstants.TYPE_ALL);
        getTagMenuList(TagConstants.TYPE_MENU);
    }

    /**
     * 从数据库查询标签菜单列表（树形结构）
     *
     * @param
     * @return java.util.List<com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo>
     * @author lwj
     **/
    private List<TagDictMenuVo> selectTagMenuList(String type) {
        LambdaQueryWrapper<TagDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!TagConstants.TYPE_ALL.equals(type), TagDict::getTagType, type)
                .eq(TagDict::getDelFlag, "0")
                .orderByAsc(TagDict::getTagType)
                .orderByAsc(TagDict::getSortOrder);

        List<TagDict> tagDictList = tagDictMapper.selectList(queryWrapper);

        if (tagDictList == null || tagDictList.isEmpty()) {
            return Collections.emptyList();
        }

        List<TagDictMenuVo> menuVoList = tagDictList.stream()
                .map(TagDictMenuVo::convertToMenuVo)
                .collect(Collectors.toList());

        return buildMenuTree(menuVoList);
    }

    /**
     * 构建菜单树形结构
     *
     * @param allMenus 所有菜单列表
     * @return 树形结构菜单列表
     */
    private List<TagDictMenuVo> buildMenuTree(List<TagDictMenuVo> allMenus) {
        List<TagDictMenuVo> tree = new ArrayList<>();

        for (TagDictMenuVo menu : allMenus) {
            if (menu.getParentId() == null || menu.getParentId() <= 0) {
                menu.setChildren(findChildren(allMenus, menu.getTagId()));
                tree.add(menu);
            }
        }

        return tree;
    }

    /**
     * 递归查找子节点
     *
     * @param allMenus 所有菜单列表
     * @param parentId 父级 ID
     * @return 子节点列表
     */
    private List<TagDictMenuVo> findChildren(List<TagDictMenuVo> allMenus, Long parentId) {
        List<TagDictMenuVo> children = new ArrayList<>();

        for (TagDictMenuVo menu : allMenus) {
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                menu.setChildren(findChildren(allMenus, menu.getTagId()));
                children.add(menu);
            }
        }

        children.sort(Comparator.comparing(TagDictMenuVo::getSortOrder));

        return children;
    }
}
