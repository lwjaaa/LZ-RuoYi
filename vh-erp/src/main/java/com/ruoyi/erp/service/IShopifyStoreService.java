package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.ShopifyStore;

import java.util.List;

/**
 * Shopify 店铺配置Service接口
 */
public interface IShopifyStoreService extends IService<ShopifyStore> {

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

    /**
     * 根据 storeId 查询店铺
     */
    ShopifyStore selectByStoreId(Long storeId);

    /**
     * 刷新店铺 Token
     * @return true=刷新成功或不需要刷新, false=刷新失败
     */
    boolean refreshToken(Long storeId);

    /**
     * 更新 Token 信息
     */
    void updateTokenInfo(Long storeId, String accessToken, Long expiresInSeconds);

    /**
     * 更新连接状态
     */
    void updateStatus(Long storeId, String status);

    /**
     * 检查 Token 是否需要刷新
     */
    boolean isTokenExpiringSoon(Long storeId);

    /**
     * 检查店铺是否已连接
     */
    boolean isConnected(Long storeId);
}