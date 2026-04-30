package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.ShopifyStore;

import java.util.List;

/**
 * Shopify 店铺配置Mapper接口
 */
public interface ShopifyStoreMapper extends BaseMapper<ShopifyStore> {

    /**
     * 查询默认店铺
     */
    ShopifyStore selectDefaultStore();

    /**
     * 查询启用的店铺列表
     */
    List<ShopifyStore> selectActiveStores();

    /**
     * 根据 shopName 查询店铺
     */
    ShopifyStore selectByShopName(String shopName);
}