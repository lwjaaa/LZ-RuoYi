package com.ruoyi.erp.service.impl;

import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.ruoyi.common.utils.StringUtils;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.utils.DateUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.mapper.ShopifyTaskMapper;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.service.IShopifyTaskService;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskQuery;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskVo;

/**
 * Shopify 任务配置Service业务层处理
 *
 * @author lwj
 * @date 2026-03-26
 */
@Service
public class ShopifyTaskServiceImpl extends ServiceImpl<ShopifyTaskMapper, ShopifyTask> implements IShopifyTaskService
{

    @Resource
    private ShopifyTaskMapper shopifyTaskMapper;

    //region mybatis代码
    /**
     * 查询Shopify 任务配置
     *
     * @param taskId Shopify 任务配置主键
     * @return Shopify 任务配置
     */
    @Override
    public ShopifyTask selectShopifyTaskByTaskId(Long taskId)
    {
        return shopifyTaskMapper.selectShopifyTaskByTaskId(taskId);
    }

    /**
     * 查询Shopify 任务配置列表
     *
     * @param shopifyTask Shopify 任务配置
     * @return Shopify 任务配置
     */
    @Override
    public List<ShopifyTask> selectShopifyTaskList(ShopifyTask shopifyTask)
    {
        return shopifyTaskMapper.selectShopifyTaskList(shopifyTask);
    }

    /**
     * 新增Shopify 任务配置
     *
     * @param shopifyTask Shopify 任务配置
     * @return 结果
     */
    @Override
    public int insertShopifyTask(ShopifyTask shopifyTask)
    {
        shopifyTask.setCreateTime(DateUtils.getNowDate());
        return shopifyTaskMapper.insertShopifyTask(shopifyTask);
    }

    /**
     * 修改Shopify 任务配置
     *
     * @param shopifyTask Shopify 任务配置
     * @return 结果
     */
    @Override
    public int updateShopifyTask(ShopifyTask shopifyTask)
    {
        shopifyTask.setUpdateTime(DateUtils.getNowDate());
        return shopifyTaskMapper.updateShopifyTask(shopifyTask);
    }

    /**
     * 批量删除Shopify 任务配置
     *
     * @param taskIds 需要删除的Shopify 任务配置主键
     * @return 结果
     */
    @Override
    public int deleteShopifyTaskByTaskIds(Long[] taskIds)
    {
        return shopifyTaskMapper.deleteShopifyTaskByTaskIds(taskIds);
    }

    /**
     * 删除Shopify 任务配置信息
     *
     * @param taskId Shopify 任务配置主键
     * @return 结果
     */
    @Override
    public int deleteShopifyTaskByTaskId(Long taskId)
    {
        return shopifyTaskMapper.deleteShopifyTaskByTaskId(taskId);
    }
    //endregion
    @Override
    public QueryWrapper<ShopifyTask> getQueryWrapper(ShopifyTaskQuery shopifyTaskQuery){
        QueryWrapper<ShopifyTask> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = shopifyTaskQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        String taskName = shopifyTaskQuery.getTaskName();
        queryWrapper.like(StringUtils.isNotEmpty(taskName) ,"task_name",taskName);

        String taskGroup = shopifyTaskQuery.getTaskGroup();
        queryWrapper.eq(StringUtils.isNotEmpty(taskGroup) ,"task_group",taskGroup);

        String taskType = shopifyTaskQuery.getTaskType();
        queryWrapper.eq(StringUtils.isNotEmpty(taskType) ,"task_type",taskType);

        String businessType = shopifyTaskQuery.getBusinessType();
        queryWrapper.eq(StringUtils.isNotEmpty(businessType) ,"business_type",businessType);

        String businessIds = shopifyTaskQuery.getBusinessIds();
        queryWrapper.eq(StringUtils.isNotEmpty(businessIds) ,"business_ids",businessIds);

        String taskStatus = shopifyTaskQuery.getTaskStatus();
        queryWrapper.eq(StringUtils.isNotEmpty(taskStatus) ,"task_status",taskStatus);

        Date startTime = shopifyTaskQuery.getStartTime();
        queryWrapper.between(StringUtils.isNotNull(params.get("beginStartTime"))&&StringUtils.isNotNull(params.get("endStartTime")),"start_time",params.get("beginStartTime"),params.get("endStartTime"));

        Long parentTaskId = shopifyTaskQuery.getParentTaskId();
        queryWrapper.eq( StringUtils.isNotNull(parentTaskId),"parent_task_id",parentTaskId);

        Long rootTaskId = shopifyTaskQuery.getRootTaskId();
        queryWrapper.eq( StringUtils.isNotNull(rootTaskId),"root_task_id",rootTaskId);

        return queryWrapper;
    }

    @Override
    public List<ShopifyTaskVo> convertVoList(List<ShopifyTask> shopifyTaskList) {
        if (StringUtils.isEmpty(shopifyTaskList)) {
            return Collections.emptyList();
        }
        return shopifyTaskList.stream().map(ShopifyTaskVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入Shopify 任务配置数据
     *
     * @param shopifyTaskList Shopify 任务配置数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importShopifyTaskData(List<ShopifyTask> shopifyTaskList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(shopifyTaskList))
        {
            throw new ServiceException("导入Shopify 任务配置数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (ShopifyTask shopifyTask : shopifyTaskList)
        {
            try
            {
                // 验证是否存在这个Shopify 任务配置
                Long taskId = shopifyTask.getTaskId();
                ShopifyTask shopifyTaskExist = null;
                if (StringUtils.isNotNull(taskId))
                {
                    shopifyTaskExist = shopifyTaskMapper.selectShopifyTaskByTaskId(taskId);
                }
                if (StringUtils.isNull(shopifyTaskExist))
                {
                    shopifyTask.setCreateTime(DateUtils.getNowDate());
                    shopifyTaskMapper.insertShopifyTask(shopifyTask);
                    successNum++;
                    String taskIdStr = StringUtils.isNotNull(taskId) ? taskId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、Shopify 任务配置 " + taskIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    shopifyTask.setUpdateTime(DateUtils.getNowDate());
                    shopifyTaskMapper.updateShopifyTask(shopifyTask);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、Shopify 任务配置 " + taskId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String taskIdStr = StringUtils.isNotNull(taskId) ? taskId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、Shopify 任务配置 " + taskIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long taskId = shopifyTask.getTaskId();
                String taskIdStr = StringUtils.isNotNull(taskId) ? taskId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、Shopify 任务配置 " + taskIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
