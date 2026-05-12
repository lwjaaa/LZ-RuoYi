package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Shopify 店铺配置对象 erp_shopify_store
 */
@TableName("erp_shopify_store")
@Data
public class ShopifyStore implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 店铺主键 */
    @TableId(value = "store_id", type = IdType.ASSIGN_ID)
    private Long storeId;

    /** 店铺名称 (用于展示) */
    @Excel(name = "店铺名称")
    private String storeName;

    /** Shop 名称 (myshopify.com 前的名称) */
    @Excel(name = "店铺标识")
    private String shopName;

    /** API 版本 (如: 2026-04) */
    @Excel(name = "API版本")
    private String apiVersion;

    /** API Key */
    @Excel(name = "API Key")
    private String apiKey;

    /** API Secret (不返回给前端) */
    private String apiSecret;

    /** Access Token (不返回给前端) */
    private String accessToken;

    /** Refresh Token (不返回给前端) */
    private String refreshToken;

    /** Token 过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date tokenExpiresAt;

    /** 自定义 API 端点 */
    private String baseUrl;

    /** Shopify 库存仓库 Location ID */
    @Excel(name = "库存仓库ID")
    private String inventoryLocationId;

    /** Shopify 库存仓库名称 */
    @Excel(name = "库存仓库名称")
    private String inventoryLocationName;

    /** 是否跟踪库存 (0:否, 1:是) */
    @Excel(name = "是否跟踪库存", readConverterExp = "0=否,1=是")
    private String inventoryTracked;

    /** 默认库存数量 */
    @Excel(name = "默认库存数量")
    private Integer defaultInventoryQuantity;

    /** 缺货销售策略: DENY, CONTINUE */
    @Excel(name = "缺货销售策略")
    private String inventoryPolicy;

    /** 自动发布 Publication ID，英文逗号分隔 */
    @Excel(name = "自动发布渠道ID")
    private String publishPublicationIds;

    /** 自动发布渠道名称，英文逗号分隔 */
    @Excel(name = "自动发布渠道名称")
    private String publishPublicationNames;

    /** 推送到 Shopify 时的默认商品状态：DRAFT, ACTIVE */
    @Excel(name = "默认商品状态")
    private String defaultProductStatus;

    /** 商品资料必填字段编码，英文逗号分隔 */
    @Excel(name = "商品资料必填字段")
    private String requiredProductFields;

    /** 本次从 Shopify 拉取到的 Publication ID，英文逗号分隔，不落库 */
    @TableField(exist = false)
    private String availablePublicationIds;

    /** 是否启用 (0:禁用, 1:启用) */
    @Excel(name = "是否启用", readConverterExp = "0=禁用,1=启用")
    private String isActive;

    /** 是否默认店铺 (0:否, 1:是) */
    @Excel(name = "是否默认", readConverterExp = "0=否,1=是")
    private String isDefault;

    /** 认证模式: PRIVATE_APP, OAUTH */
    @Excel(name = "认证模式", readConverterExp = "PRIVATE_APP=Private App,OAUTH=OAuth")
    private String authMode;

    /** 连接状态: CONNECTED, DISCONNECTED, EXPIRED */
    @Excel(name = "连接状态")
    private String status;

    /** 最后同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date lastSyncTime;

    /** 同步次数 */
    private Integer syncCount;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 创建者 */
    private String createBy;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    private Date updateTime;

    /** 删除标志 (0=存在，2=删除) */
    private String delFlag;

    /**
     * 生成完整的 GraphQL API URL
     */
    public String getFullGraphQLUrl() {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }
        return String.format("https://%s.myshopify.com/admin/api/%s/graphql.json", shopName, apiVersion);
    }

    /**
     * 检查 token 是否即将过期 (提前 5 分钟)
     */
    public boolean isTokenExpiringSoon() {
        if (tokenExpiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() > tokenExpiresAt.getTime() - 5 * 60 * 1000;
    }

    /**
     * 检查 token 是否已过期
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() > tokenExpiresAt.getTime();
    }
}
