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
        tagDict.setSortOrder(this.getMaxOrderNum(tagDict.getParentId(), tagDict.getTagType()) + 1);

        // 5. 自动设置层级
        this.setLevelInfo(tagDict);

        tagDict.setCreateTime(DateUtils.getNowDate());
        return tagDictMapper.insertTagDict(tagDict);
    }

    /**
     * 设置标签层级信息:ancestors+menuLevel
     *
     * @param tagDict 标签信息
     */
    private void setLevelInfo(TagDict tagDict) {
        Long parentId = tagDict.getParentId();
        TagDict parentNode = null;
        if (parentId != null && parentId > 0) {
            parentNode = this.getById(parentId);
        }
        if (parentNode != null) {
            tagDict.setMenuLevel(parentNode.getMenuLevel() + 1);
            tagDict.setAncestors(parentNode.getAncestors() + "," + parentId);
        } else {
            tagDict.setMenuLevel(1);
            tagDict.setAncestors("0");
        }
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
            queryWrapper.select(TagDict::getTagId).eq(TagDict::getTagCode, tagDict.getTagCode())
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
            this.setLevelInfo(tagDict);
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

        this.updateBatchById(updateList);

        // 递归更新下一层子节点
        for (TagDict child : children) {
            updateChildrenInfo(child);
        }
    }

    /**
     * 校验标签名称唯一性
     */
    private boolean checkTagNameUnique(TagDict tagDict) {
        Long tagId = StringUtils.isNull(tagDict.getTagId()) ? -1L : tagDict.getTagId();

        LambdaQueryWrapper<TagDict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(TagDict::getTagId)
                .eq(TagDict::getTagName, tagDict.getTagName())
                .ne(TagDict::getTagId, tagId)
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
                    tagDict.setSortOrder(this.getMaxOrderNum(tagDict.getParentId(), tagDict.getTagType()) + 1);
                }

                // 3. 自动设置层级（如果未提供）
                this.setLevelInfo(tagDict);

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
        TagDict targetNode = getById(targetId);
        if (targetNode == null) {
            throw new ServiceException("目标节点不存在");
        }

        // 4. 类型隔离检查：非 MENU 类型不能拖拽到 MENU 类型的前面
        if (!TagConstants.TYPE_MENU.equals(dragNode.getTagType())
                && TagConstants.TYPE_MENU.equals(targetNode.getTagType())) {
            throw new ServiceException("其他类型的标签不能排序到菜单类型的前面");
        }
        int originSortOrder = dragNode.getSortOrder();
        String tagType = dragNode.getTagType();

        // 新父节点 ID
        Long newParentId = null;
        // 新排序值
        int newOrderNum;
        if ("inner".equals(dropType)) {
            // ======================
            // 拖入作为子节点（放到最后）
            // ======================
            newParentId = targetId;
            newOrderNum = this.getMaxOrderNum(newParentId, tagType) + 1;

            // 将原节点之后的排序值减1
            lambdaUpdate().eq(TagDict::getParentId, dragNode.getParentId())
                    .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                    .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                    .ge(TagDict::getSortOrder, originSortOrder)
                    .setSql("sort_order = sort_order - 1")
                    .update();
        } else {
            // ======================
            // 同级或跨层级拖拽（before / after）
            // ======================
            int targetOrder = targetNode.getSortOrder();
            newOrderNum = "before".equals(dropType) ? targetOrder : targetOrder + 1;
            // 跨层级的话，
            if (!Objects.equals(dragNode.getParentId(), targetNode.getParentId())) {
                newParentId = targetNode.getParentId();
                // 防止循环依赖：不能将父节点拖拽到其子节点中
                if (newParentId > 0 && isDescendant(newParentId, dragId)) {
                    throw new ServiceException("不能将节点拖拽到其子节点下");
                }
                // 原节点之后的排序值减1，新节点之后的排序值加1
                lambdaUpdate().eq(TagDict::getParentId, dragNode.getParentId())
                        .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                        .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                        .gt(TagDict::getSortOrder, originSortOrder)
                        .setSql("sort_order = sort_order - 1")
                        .update();
                lambdaUpdate().eq(TagDict::getParentId, targetNode.getParentId())
                        .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                        .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                        .ge(TagDict::getSortOrder, targetOrder)
                        .setSql("sort_order = sort_order + 1")
                        .update();
            } else {
                // 同级拖拽的话，
                // 如果是往前拖拽，则新节点到原节点之间的节点排序加1
                if ("before".equals(dropType)) {
                    lambdaUpdate().eq(TagDict::getParentId, dragNode.getParentId())
                            .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                            .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                            .lt(TagDict::getSortOrder, originSortOrder)
                            .ge(TagDict::getSortOrder, targetOrder)
                            .setSql("sort_order = sort_order + 1")
                            .update();
                } else {
                    // 如果是往后拖拽，则原节点到新节点之间的节点排序减1
                    lambdaUpdate().eq(TagDict::getParentId, dragNode.getParentId())
                            .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                            .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                            .gt(TagDict::getSortOrder, originSortOrder)
                            .le(TagDict::getSortOrder, targetOrder)
                            .setSql("sort_order = sort_order - 1")
                            .update();
                }

            }
        }

        // 5. 设置被拖拽节点新排序和层级信息
        dragNode.setParentId(newParentId);
        dragNode.setSortOrder(newOrderNum);
        this.setLevelInfo(dragNode);

        // 6. 批量更新（1 次 SQL！）
        boolean success = this.updateById(dragNode);

        if (!success) {
            throw new ServiceException("节点排序更新失败");
        }

        // 7. 清理缓存
        clearMenuCache();
    }

    /**
     * 获取某节点下的当前最大排序值
     *
     * @param parentId 父级ID
     * @param tagType  标签类型
     * @return
     */
    private int getMaxOrderNum(Long parentId, String tagType) {
        return lambdaQuery().select(TagDict::getSortOrder).eq(TagDict::getParentId, parentId)
                // MENU
                .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                // 非MENU
                .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                .orderByDesc(TagDict::getSortOrder)
                .last("limit 1")
                .oneOpt()
                .map(TagDict::getSortOrder)
                .orElse(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int top(Long tagId) {
        // 于展示的时候，默认MENU类型的tag优先级最高、所以调整排序的时候需要区分是MENU还是其他类型的tag。

        // 1. 获取要置顶的节点
        TagDict tagDict = getById(tagId);
        if (tagDict == null) {
            throw new ServiceException("要置顶的节点不存在");
        }

        Long parentId = tagDict.getParentId();
        String tagType = tagDict.getTagType();
        int currentOrder = tagDict.getSortOrder();

        // 查询第一的位置
        int newOrderNum = lambdaQuery().select(TagDict::getSortOrder).eq(TagDict::getParentId, parentId)
                .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                .orderByAsc(TagDict::getSortOrder)
                .last("limit 1")
                .oneOpt()
                .map(TagDict::getSortOrder)
                .orElse(0);

        // 2. 如果当前已经在第一位，无需更新
        if (currentOrder == 1) {
            return 0;
        }

        // 3. 将 [1, currentOrder-1] 范围内的节点排序 +1（仅影响前面的节点）
        lambdaUpdate()
                .eq(TagDict::getParentId, parentId)
                .eq(TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, tagType)
                .ne(!TagConstants.TYPE_MENU.equals(tagType), TagDict::getTagType, TagConstants.TYPE_MENU)
                .between(TagDict::getSortOrder, newOrderNum, currentOrder)
                .setSql("sort_order = sort_order + 1")
                .update();

        // 7. 更新被置顶节点的排序（仅更新这一条记录）
        lambdaUpdate()
                .eq(TagDict::getTagId, tagId)
                .set(TagDict::getSortOrder, newOrderNum)
                .update();

        // 8. 清理缓存
        clearMenuCache();

        return 1;
    }

    @Override
    public int updateMaxSeqBySpuPrefix(String spuPrefix, Integer seqNum) {
        if (StringUtils.isEmpty(spuPrefix) || seqNum == null) {
            return 0;
        }

        // 1. 根据 SPU 前缀查询标签字典
        TagDict tagDict = lambdaQuery()
                .eq(TagDict::getTagType, TagConstants.TYPE_MENU)
                .eq(TagDict::getSpuPrefix, spuPrefix)
                .one();

        if (tagDict == null) {
            throw new ServiceException("标签不存在");
        }

        // 2. 只有当新序列号大于当前最大流水号时才更新
        Integer currentMaxSeq = tagDict.getCurrentMaxSeq();
        if (seqNum > (currentMaxSeq == null ? 0 : currentMaxSeq)) {
            tagDict.setCurrentMaxSeq(seqNum);
            tagDict.setUpdateTime(DateUtils.getNowDate());
            return updateById(tagDict) ? 1 : 0;
        }

        return 0;
    }

    /**
     * 判断某个节点是否是另一个节点的后代
     */
    private boolean isDescendant(Long potentialDescendantId, Long ancestorId) {
        if (potentialDescendantId == null || ancestorId == null) {
            return false;
        }
        TagDict node = getById(potentialDescendantId);
        if(node == null){
            return false;
        }
        String ancestors = node.getAncestors();
        if (StringUtils.isNotEmpty(ancestors)) {
            // 快速检查：如果祖先路径中不包含新父节点 ID，直接跳过
            if (ancestors.contains(String.valueOf(ancestorId))) {
                // 详细检查：遍历祖先 ID，确认是否直接匹配
                for (String id : ancestors.split(",")) {
                    if (String.valueOf(ancestorId).equals(id)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
