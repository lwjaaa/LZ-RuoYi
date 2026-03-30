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
import com.ruoyi.erp.model.dto.tagDict.TreeDragDTO;
import com.ruoyi.erp.model.vo.tagDict.TagDictMenuVo;
import com.ruoyi.erp.model.vo.tagDict.TagDictVo;
import com.ruoyi.erp.service.ITagDictService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 1. 校验标签名称唯一性
        if (!checkTagNameUnique(tagDict)) {
            throw new ServiceException("标签名称已存在");
        }

        // 2. 校验标签编码唯一性（如果提供了编码）
        if (StringUtils.isNotEmpty(tagDict.getTagCode())) {
            LambdaQueryWrapper<TagDict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TagDict::getTagCode, tagDict.getTagCode())
                    .last("LIMIT 1");
            TagDict existTag = this.getOne(queryWrapper);
            if (existTag != null) {
                throw new ServiceException("标签编码已存在");
            }
        }

        // 3. 统一处理 parentId 为 null 的情况
        if (tagDict.getParentId() == null) {
            tagDict.setParentId(0L);
        }

        // 4. 自动设置排序号（如果未提供）
        if (tagDict.getSortOrder() == null || tagDict.getSortOrder() <= 0) {
            tagDict.setSortOrder(this.getMaxOrderNum(tagDict.getParentId()) + 1);
        }

        // 5. 自动设置层级
        if (tagDict.getMenuLevel() == null) {
            if (tagDict.getParentId() <= 0) {
                tagDict.setMenuLevel(1); // 顶级节点层级为 1
            } else {
                // 子节点层级 = 父节点层级 + 1
                TagDict parent = getById(tagDict.getParentId());
                if (parent != null) {
                    tagDict.setMenuLevel(parent.getMenuLevel() + 1);
                } else {
                    tagDict.setMenuLevel(1);
                }
            }
        }

        // 6. 自动设置祖先路径
        if (StringUtils.isEmpty(tagDict.getAncestors())) {
            if (tagDict.getParentId() <= 0) {
                tagDict.setAncestors("0");
            } else {
                TagDict parent = getById(tagDict.getParentId());
                if (parent != null) {
                    tagDict.setAncestors(parent.getAncestors() + "," + tagDict.getParentId());
                } else {
                    tagDict.setAncestors("0," + tagDict.getParentId());
                }
            }
        }

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
    @Transactional(rollbackFor = Exception.class)
    public int updateTagDict(TagDict tagDict) {
        // 1. 获取原节点信息
        TagDict oldNode = getById(tagDict.getTagId());
        if (oldNode == null) {
            throw new ServiceException("要修改的节点不存在");
        }

        // 2. 校验标签名称唯一性（排除自己）
        if (!checkTagNameUnique(tagDict)) {
            throw new ServiceException("标签名称已存在");
        }

        // 3. 校验标签编码唯一性（如果修改了编码）
        if (StringUtils.isNotEmpty(tagDict.getTagCode()) &&
                !Objects.equals(oldNode.getTagCode(), tagDict.getTagCode())) {
            LambdaQueryWrapper<TagDict> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TagDict::getTagCode, tagDict.getTagCode())
                    .ne(TagDict::getTagId, tagDict.getTagId())
                    .last("LIMIT 1");
            TagDict existTag = this.getOne(queryWrapper);
            if (existTag != null) {
                throw new ServiceException("标签编码已存在");
            }
        }

        // 4. 统一处理 parentId 为 null 的情况
        if (tagDict.getParentId() == null) {
            tagDict.setParentId(0L);
        }

        // 5. 检查是否修改了父级
        Long oldParentId = oldNode.getParentId();
        Long newParentId = tagDict.getParentId();
        boolean parentChanged = !Objects.equals(oldParentId, newParentId);

        // 6. 防止循环依赖：不能将父节点设置为它的子节点
        if (parentChanged && newParentId > 0) {
            if (isDescendant(newParentId, tagDict.getTagId())) {
                throw new ServiceException("不能将节点的父级设置为其子节点");
            }
        }

        // 7. 如果父级改变，需要更新层级和祖先路径
        if (parentChanged) {
            // 7.1 更新当前节点的层级
            if (newParentId <= 0) {
                tagDict.setMenuLevel(1);
                tagDict.setAncestors("0");
            } else {
                TagDict newParent = getById(newParentId);
                if (newParent != null) {
                    tagDict.setMenuLevel(newParent.getMenuLevel() + 1);
                    tagDict.setAncestors(newParent.getAncestors() + "," + newParentId);
                }
            }

            // 7.2 更新旧父级下后续节点的排序（前移）
            if (oldParentId > 0) {
                List<TagDict> oldParentList = lambdaQuery()
                        .eq(TagDict::getParentId, oldParentId)
                        .gt(TagDict::getSortOrder, oldNode.getSortOrder())
                        .list();

                for (TagDict node : oldParentList) {
                    node.setSortOrder(node.getSortOrder() - 1);
                }
                if (!oldParentList.isEmpty()) {
                    this.updateBatchById(oldParentList);
                }
            }

            // 7.3 将当前节点排序设为新父级下的最大值（放到最后）
            tagDict.setSortOrder(this.getMaxOrderNum(newParentId) + 1);
        }

        // 8. 递归更新所有子节点的层级和祖先路径
        if (parentChanged) {
            updateChildrenInfo(tagDict);
        }

        tagDict.setUpdateTime(DateUtils.getNowDate());
        return tagDictMapper.updateTagDict(tagDict);
    }

    /**
     * 递归更新子节点的层级和祖先路径
     */
    private void updateChildrenInfo(TagDict parentNode) {
        List<TagDict> children = lambdaQuery()
                .eq(TagDict::getParentId, parentNode.getTagId())
                .list();

        if (children.isEmpty()) {
            return;
        }

        List<TagDict> updateList = new ArrayList<>();
        for (TagDict child : children) {
            // 更新层级
            child.setMenuLevel(parentNode.getMenuLevel() + 1);
            // 更新祖先路径
            child.setAncestors(parentNode.getAncestors() + "," + parentNode.getTagId());
            child.setUpdateTime(DateUtils.getNowDate());
            updateList.add(child);
        }

        if (!updateList.isEmpty()) {
            this.updateBatchById(updateList);

            // 递归更新下一层子节点
            for (TagDict child : children) {
                updateChildrenInfo(child);
            }
        }
    }

    private boolean checkTagNameUnique(TagDict tagDict) {
        Long tagId = StringUtils.isNull(tagDict.getTagId()) ? -1L : tagDict.getTagId();

        LambdaQueryWrapper<TagDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TagDict::getTagName, tagDict.getTagName())
                .ne(StringUtils.isNotNull(tagId), TagDict::getTagId, tagId)
                .last("LIMIT 1");

        TagDict existTag = this.getOne(queryWrapper);

        return StringUtils.isNull(existTag);
    }

    /**
     * 批量删除erp标签
     *
     * @param tagIds 需要删除的erp标签主键
     * @return 结果
     */
    @Override
    public int deleteTagDictByTagIds(Long[] tagIds) {
        // 1. 检查每个节点是否有子节点
        for (Long tagId : tagIds) {
            Long childrenCount = lambdaQuery()
                    .eq(TagDict::getParentId, tagId)
                    .count();

            if (childrenCount > 0) {
                throw new ServiceException(
                        String.format("ID 为 %d 的节点下存在子节点，请先删除或转移子节点", tagId)
                );
            }
        }

        // 2. 批量删除
        int result = tagDictMapper.deleteTagDictByTagIds(tagIds);

        // 3. 清理缓存
        clearMenuCache();

        return result;
    }

    /**
     * 删除erp标签信息
     *
     * @param tagId erp标签主键
     * @return 结果
     */
    @Override
    public int deleteTagDictByTagId(Long tagId) {
        // 1. 检查是否存在子节点
        Long childrenCount = lambdaQuery()
                .eq(TagDict::getParentId, tagId)
                .count();

        if (childrenCount > 0) {
            throw new ServiceException("该节点下存在子节点，请先删除或转移子节点");
        }

        // 2. 执行删除
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
            throw new ServiceException("导入 erp 标签数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (TagDict tagDict : tagDictList) {
            try {
                // 1. 统一处理 parentId 为 null 的情况
                if (tagDict.getParentId() == null) {
                    tagDict.setParentId(0L);
                }

                // 2. 自动设置排序号（如果未提供）
                if (tagDict.getSortOrder() == null || tagDict.getSortOrder() <= 0) {
                    tagDict.setSortOrder(this.getMaxOrderNum(tagDict.getParentId()) + 1);
                }

                // 3. 自动设置层级（如果未提供）
                if (tagDict.getMenuLevel() == null) {
                    if (tagDict.getParentId() <= 0) {
                        tagDict.setMenuLevel(1);
                    } else {
                        TagDict parent = getById(tagDict.getParentId());
                        if (parent != null) {
                            tagDict.setMenuLevel(parent.getMenuLevel() + 1);
                        } else {
                            tagDict.setMenuLevel(1);
                        }
                    }
                }

                // 4. 自动设置祖先路径（如果未提供）
                if (StringUtils.isEmpty(tagDict.getAncestors())) {
                    if (tagDict.getParentId() <= 0) {
                        tagDict.setAncestors("0");
                    } else {
                        TagDict parent = getById(tagDict.getParentId());
                        if (parent != null) {
                            tagDict.setAncestors(parent.getAncestors() + "," + tagDict.getParentId());
                        } else {
                            tagDict.setAncestors("0," + tagDict.getParentId());
                        }
                    }
                }

                // 5. 验证是否存在这个 erp 标签
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
                    successMsg.append("<br/>" + successNum + "、erp 标签 " + tagIdStr + " 导入成功");
                } else if (isUpdateSupport) {
                    tagDict.setUpdateTime(DateUtils.getNowDate());
                    tagDictMapper.updateTagDict(tagDict);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp 标签 " + tagId.toString() + " 更新成功");
                } else {
                    failureNum++;
                    String tagIdStr = StringUtils.isNotNull(tagId) ? tagId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp 标签 " + tagIdStr + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                Long tagId = tagDict.getTagId();
                String tagIdStr = StringUtils.isNotNull(tagId) ? tagId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp 标签 " + tagIdStr + " 导入失败：";
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dragNode(TreeDragDTO dto) {
        Long dragId = dto.getDragId();
        Long targetId = dto.getTargetId();
        String dropType = dto.getDropType();
        Long newParentId = dto.getNewParentId();

        // 1. 获取被拖拽节点
        TagDict dragNode = getById(dragId);
        if (dragNode == null) {
            throw new ServiceException("拖拽节点不存在");
        }

        // 2. 防止自己拖给自己
        if (dragId.equals(targetId)) {
            throw new ServiceException("不能将节点拖拽到自身");
        }

        // 3. 获取目标节点（inner/before/after 都需要）
        TagDict targetNode = null;
        if (targetId != null && !"inner".equals(dropType)) {
            targetNode = getById(targetId);
            if (targetNode == null) {
                throw new ServiceException("目标节点不存在");
            }
        }

        // 4. 防止循环依赖：不能将父节点拖拽到其子节点中
        if (newParentId != null && newParentId > 0 && isDescendant(newParentId, dragId)) {
            throw new ServiceException("不能将节点拖拽到其子节点下");
        }

        // 5. 类型隔离检查：非 MENU 类型不能拖拽到 MENU 类型的前面
        if (targetNode != null && !TagConstants.TYPE_MENU.equals(dragNode.getTagType())
                && TagConstants.TYPE_MENU.equals(targetNode.getTagType())) {
            throw new ServiceException("其他类型的标签不能排序到菜单类型的前面");
        }

        int newOrderNum;
        List<TagDict> needUpdateNodes = new ArrayList<>();

        if ("inner".equals(dropType)) {
            // ======================
            // 拖入作为子节点（放到最后）
            // ======================
            newOrderNum = this.getMaxOrderNum(newParentId) + 1;
        } else {
            // ======================
            // 同级或跨层级拖拽（before / after）
            // ======================
            if (targetNode == null) {
                throw new ServiceException("目标节点不能为空");
            }

            int targetOrder = targetNode.getSortOrder();
            newOrderNum = "before".equals(dropType) ? targetOrder : targetOrder + 1;

            // 如果新旧父级不同，需要更新两个父级下的节点排序
            Long oldParentId = dragNode.getParentId();
            if (!Objects.equals(oldParentId, newParentId)) {
                // 旧父级下的节点：排序前移（填补拖拽节点的空缺）
                List<TagDict> oldParentList = lambdaQuery()
                        .eq(TagDict::getParentId, oldParentId)
                        .gt(TagDict::getSortOrder, dragNode.getSortOrder())
                        .list();

                for (TagDict node : oldParentList) {
                    node.setSortOrder(node.getSortOrder() - 1);
                }
                needUpdateNodes.addAll(oldParentList);
            }

            // 新父级下的节点：排序后移（为拖拽节点腾出位置，排除被拖拽节点本身）
            List<TagDict> newParentList = lambdaQuery()
                    .eq(TagDict::getParentId, newParentId)
                    .ge(TagDict::getSortOrder, newOrderNum)
                    .ne(TagDict::getTagId, dragId)
                    .list();

            for (TagDict node : newParentList) {
                node.setSortOrder(node.getSortOrder() + 1);
            }
            needUpdateNodes.addAll(newParentList);
        }

        // 5. 设置被拖拽节点新排序和父级
        dragNode.setParentId(newParentId);
        dragNode.setSortOrder(newOrderNum);
        needUpdateNodes.add(dragNode);

        // 6. 批量更新（1 次 SQL！）
        boolean success = this.updateBatchById(needUpdateNodes);

        if (!success) {
            throw new ServiceException("节点排序更新失败");
        }

        // 7. 清理缓存
        clearMenuCache();
    }

    /**
     * 判断某个节点是否是另一个节点的后代
     */
    private boolean isDescendant(Long potentialDescendantId, Long ancestorId) {
        if (potentialDescendantId == null || ancestorId == null) {
            return false;
        }

        TagDict node = getById(potentialDescendantId);
        while (node != null && node.getParentId() != null && node.getParentId() > 0) {
            if (node.getParentId().equals(ancestorId)) {
                return true;
            }
            node = getById(node.getParentId());
        }
        return false;
    }
// ... existing code ...

    /**
     * 获取某个父节点下最大排序号
     */
    private int getMaxOrderNum(Long parentId) {
        // 统一处理 parentId 为 null 的情况
        if (parentId == null) {
            parentId = 0L;
        }

        TagDict maxNode = lambdaQuery()
                .eq(TagDict::getParentId, parentId)
                .orderByDesc(TagDict::getSortOrder)
                .last("LIMIT 1")
                .one();
        return maxNode == null ? 0 : maxNode.getSortOrder();
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
