package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Shopify 任务配置Mapper接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface ShopifyTaskMapper extends BaseMapper<ShopifyTask>
{
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
     * 删除Shopify 任务配置
     * 
     * @param taskId Shopify 任务配置主键
     * @return 结果
     */
    public int deleteShopifyTaskByTaskId(Long taskId);

    /**
     * 批量删除Shopify 任务配置
     * 
     * @param taskIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteShopifyTaskByTaskIds(Long[] taskIds);
}
