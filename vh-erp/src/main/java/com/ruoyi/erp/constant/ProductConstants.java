package com.ruoyi.erp.constant;

/**
 * @author lwj
 * @version 1.0.0
 * @description 商品常量
 * @date 2026/4/24 14:21
 **/
public interface ProductConstants {


    /**
     * 是否可用(0:否, 1:是) 是
     */
    String IS_ACTIVE_AVAILABLE_YES = "1";

    String SUGGESTIONS_KEY_SIZE = "size";
    String SUGGESTIONS_KEY_MATERIAL = "material";
    String SUGGESTIONS_KEY_NOTE = "note";
    String SUGGESTIONS_KEY_NOTECN = "noteCn";
    String SUGGESTIONS_KEY_OPTION_ENGLISH_NAME = "optionEnglishName";
    String SUGGESTIONS_KEY_OPTION_ENGLISH_VALUE = "optionEnglishValue";

    /**
     * 默认供应商
     */
    String DEFAULT_PRODUCT_VENDER = "VELART HOME";
    String SEO_TITTLE_SUFFIX = " | VELART HOME";

    /**
     * 商品同步状态
     */
    String SYNC_STATUS_WAITING = "0";
    /**
     * 商品同步状态-成功
     */
    String SYNC_STATUS_SUCCESS = "1";
    /**
     * 商品同步状态-失败
     */
    String SYNC_STATUS_FAILED = "2";
    /**
     * 商品同步状态-同步中
     */
    String SYNC_STATUS_RUNNING = "3";
}
