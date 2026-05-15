package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.ShopifySyncCursor;

public interface IShopifyProductImportService {

    Long startFullImport(Long storeId);

    Long startIncrementalImport(Long storeId);

    void syncIncrementalAllActiveStores();

    ShopifySyncCursor getCursor(Long storeId);

    void executeLocalFile(String filePath);
}
