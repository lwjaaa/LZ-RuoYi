package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.vo.shopifyTask.ShopifyTaskVo;
import com.ruoyi.erp.model.dto.shopifyTask.ShopifyTaskQuery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
/**
 * Shopify 任务配置Service接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface IShopifyTaskService extends IService<ShopifyTask>
{
    //region mybatis代码
    /**
     * 查询Shopify 任务配置
     * 
     * @param taskId Shopify 任务配置主键
     * @return Shopify 任务配置
     */
    public ShopifyTask selectShopifyTaskByTaskId(Long taskId);

    /**
     * 查询Shopify 任务配置列表
     * 
     * @param shopifyTask Shopify 任务配置
     * @return Shopify 任务配置集合
     */
    public List<ShopifyTask> selectShopifyTaskList(ShopifyTask shopifyTask);

    /**
     * 新增Shopify 任务配置
     * 
     * @param shopifyTask Shopify 任务配置
     * @return 结果
     */
    public int insertShopifyTask(ShopifyTask shopifyTask);

    /**
     * 修改Shopify 任务配置
     * 
     * @param shopifyTask Shopify 任务配置
     * @return 结果
     */
    public int updateShopifyTask(ShopifyTask shopifyTask);

    /**
     * 批量删除Shopify 任务配置
     * 
     * @param taskIds 需要删除的Shopify 任务配置主键集合
     * @return 结果
     */
    public int deleteShopifyTaskByTaskIds(Long[] taskIds);

    /**
     * 删除Shopify 任务配置信息
     * 
     * @param taskId Shopify 任务配置主键
     * @return 结果
     */
    public int deleteShopifyTaskByTaskId(Long taskId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param shopifyTaskQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<ShopifyTask> getQueryWrapper(ShopifyTaskQuery shopifyTaskQuery);

    /**
     * 转换vo
     *
     * @param shopifyTaskList ShopifyTask集合
     * @return ShopifyTaskVO集合
     */
    List<ShopifyTaskVo> convertVoList(List<ShopifyTask> shopifyTaskList);

    /**
     * 导入Shopify 任务配置数据
     *
     * @param shopifyTaskList Shopify 任务配置数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importShopifyTaskData(List<ShopifyTask> shopifyTaskList, Boolean isUpdateSupport, String operName);
}
