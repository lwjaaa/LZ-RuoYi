package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ShopifyTaskMapper;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskQuery;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskVo;
import com.ruoyi.erp.service.IShopifyTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shopify 同步任务 Service 业务层处理
 */
@Slf4j
@Service
public class ShopifyTaskServiceImpl extends ServiceImpl<ShopifyTaskMapper, ShopifyTask> implements IShopifyTaskService {

    @Resource
    private ShopifyTaskMapper shopifyTaskMapper;

    @Override
    public ShopifyTask selectShopifyTaskByTaskId(Long taskId) {
        return shopifyTaskMapper.selectShopifyTaskByTaskId(taskId);
    }

    @Override
    public List<ShopifyTask> selectShopifyTaskList(ShopifyTask shopifyTask) {
        return shopifyTaskMapper.selectShopifyTaskList(shopifyTask);
    }

    @Override
    public int insertShopifyTask(ShopifyTask shopifyTask) {
        shopifyTask.setCreateTime(DateUtils.getNowDate());
        return shopifyTaskMapper.insert(shopifyTask);
    }

    @Override
    public int updateShopifyTask(ShopifyTask shopifyTask) {
        shopifyTask.setUpdateTime(DateUtils.getNowDate());
        return shopifyTaskMapper.updateById(shopifyTask);
    }

    @Override
    public int deleteShopifyTaskByTaskIds(Long[] taskIds) {
        return shopifyTaskMapper.deleteShopifyTaskByTaskIds(taskIds);
    }

    @Override
    public int deleteShopifyTaskByTaskId(Long taskId) {
        return shopifyTaskMapper.deleteShopifyTaskByTaskId(taskId);
    }

    @Override
    public QueryWrapper<ShopifyTask> getQueryWrapper(ShopifyTaskQuery query) {
        QueryWrapper<ShopifyTask> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        Map<String, Object> params = query.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        wrapper.eq(StringUtils.isNotNull(query.getStoreId()), "store_id", query.getStoreId());
        wrapper.like(StringUtils.isNotEmpty(query.getShopName()), "shop_name", query.getShopName());
        wrapper.like(StringUtils.isNotEmpty(query.getTaskName()), "task_name", query.getTaskName());
        wrapper.eq(StringUtils.isNotEmpty(query.getTaskType()), "task_type", query.getTaskType());
        wrapper.eq(StringUtils.isNotEmpty(query.getTaskStatus()), "task_status", query.getTaskStatus());
        wrapper.between(StringUtils.isNotNull(params.get("beginStartTime")) && StringUtils.isNotNull(params.get("endStartTime")),
                "start_time", params.get("beginStartTime"), params.get("endStartTime"));
        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    @Override
    public List<ShopifyTaskVo> convertVoList(List<ShopifyTask> shopifyTaskList) {
        if (StringUtils.isEmpty(shopifyTaskList)) {
            return Collections.emptyList();
        }
        return shopifyTaskList.stream().map(ShopifyTaskVo::objToVo).collect(Collectors.toList());
    }

    @Override
    public String importShopifyTaskData(List<ShopifyTask> shopifyTaskList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isEmpty(shopifyTaskList)) {
            throw new ServiceException("导入 Shopify 同步任务数据不能为空");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (ShopifyTask task : shopifyTaskList) {
            try {
                Long taskId = task.getTaskId();
                ShopifyTask exists = taskId == null ? null : shopifyTaskMapper.selectShopifyTaskByTaskId(taskId);
                if (exists == null) {
                    task.setCreateTime(DateUtils.getNowDate());
                    shopifyTaskMapper.insert(task);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、Shopify 同步任务 ")
                            .append(taskId == null ? "新记录" : taskId).append(" 导入成功");
                } else if (isUpdateSupport) {
                    task.setUpdateTime(DateUtils.getNowDate());
                    shopifyTaskMapper.updateById(task);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、Shopify 同步任务 ")
                            .append(taskId).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、Shopify 同步任务 ")
                            .append(taskId == null ? "未知" : taskId).append(" 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>").append(failureNum).append("、Shopify 同步任务导入失败：")
                        .append(e.getMessage());
                log.error("导入 Shopify 同步任务失败", e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        return successMsg.toString();
    }
}
