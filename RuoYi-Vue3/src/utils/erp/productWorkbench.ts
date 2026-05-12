export type ProductSyncStateKey = "pending" | "running" | "synced" | "need_resync" | "failed";

export interface ProductSyncInput {
  syncStatus?: string | null;
  shopifyProductId?: string | null;
  syncMessage?: string | null;
  needResync?: boolean | null;
  latestTaskStatus?: string | null;
  latestTaskError?: string | null;
}

export interface ProductSyncState {
  key: ProductSyncStateKey;
  label: string;
  tagType: "success" | "warning" | "danger" | "info" | "primary";
  className: string;
  primaryAction: string;
  description: string;
}

const missingFieldLabelMap: Record<string, string> = {
  TITLE: "缺标题",
  SPU: "缺SPU",
  MAIN_MEDIA: "缺主图",
  DESCRIPTION: "缺描述",
  PRODUCT_TYPE: "缺商品类型",
  PRODUCT_TITLE: "缺商品标题",
  CATEGORY: "缺商品类别",
  VARIANT: "缺变体",
  SKU: "缺 SKU",
  PRICE: "缺售价",
  FREIGHT: "缺运费",
  PURCHASE_PRICE: "缺采购价",
  BODY_HTML: "缺商品详情",
  VARIANT_MEDIA: "缺变体图",
};

export const productMissingFieldOptions = Object.entries(missingFieldLabelMap).map(([value, label]) => ({
  value,
  label,
}));

export const defaultRequiredMissingFields = [
  "TITLE",
  "SPU",
  "MAIN_MEDIA",
  "DESCRIPTION",
  "BODY_HTML",
  "VARIANT",
  "PRICE",
  "FREIGHT",
  "VARIANT_MEDIA",
  "SKU",
];

export function getProductSyncState(product: ProductSyncInput): ProductSyncState {
  const syncStatus = product.syncStatus || "";
  const latestTaskStatus = (product.latestTaskStatus || "").toUpperCase();
  const hasShopifyId = Boolean(product.shopifyProductId);

  if (syncStatus === "2" || latestTaskStatus === "FAILED" || latestTaskStatus === "PART_SUCCESS") {
    return {
      key: "failed",
      label: "同步失败",
      tagType: "danger",
      className: "is-failed",
      primaryAction: "修复",
      description: product.latestTaskError || product.syncMessage || "最近一次同步失败，需要查看错误并重试",
    };
  }

  if (syncStatus === "3" || latestTaskStatus === "RUNNING" || latestTaskStatus === "PENDING") {
    return {
      key: "running",
      label: latestTaskStatus === "PENDING" ? "待执行" : "同步中",
      tagType: "warning",
      className: "is-running",
      primaryAction: "查看任务",
      description: "Shopify 同步任务正在排队或执行中",
    };
  }

  if (hasShopifyId && product.needResync) {
    return {
      key: "need_resync",
      label: "需重新同步",
      tagType: "warning",
      className: "is-resync",
      primaryAction: "重同步",
      description: "本地商品资料晚于最后同步时间",
    };
  }

  if (syncStatus === "1" && hasShopifyId) {
    return {
      key: "synced",
      label: "已同步",
      tagType: "success",
      className: "is-synced",
      primaryAction: "查看",
      description: "商品已与 Shopify 建立同步关系",
    };
  }

  return {
    key: "pending",
    label: "未同步",
    tagType: "info",
    className: "is-pending",
    primaryAction: "推送",
    description: "商品尚未生成 Shopify 商品 ID",
  };
}

export function formatMissingFields(fields?: string[] | null): string[] {
  if (!fields || fields.length === 0) {
    return [];
  }
  return fields.map((field) => missingFieldLabelMap[field] || field);
}

export function getMissingFieldSeverity(
  fields?: string[] | null,
  requiredFields: string[] = defaultRequiredMissingFields,
): "success" | "warning" | "danger" {
  if (!fields || fields.length === 0) {
    return "success";
  }
  const requiredFieldSet = new Set(requiredFields);
  if (fields.some((field) => requiredFieldSet.has(field))) {
    return "danger";
  }
  return "warning";
}

export function splitFieldCodes(value?: string | null): string[] {
  if (!value) {
    return [];
  }
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

export function formatCents(value?: number | null, currency = "$"): string {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${currency}${(value / 100).toFixed(2)}`;
}

export function formatCentsRange(min?: number | null, max?: number | null, currency = "$"): string {
  if (min === null || min === undefined) {
    return "-";
  }
  if (max === null || max === undefined || max === min) {
    return formatCents(min, currency);
  }
  return `${formatCents(min, currency)} - ${formatCents(max, currency)}`;
}
