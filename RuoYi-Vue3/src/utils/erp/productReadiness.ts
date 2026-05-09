export type ReadinessSeverity = "error" | "warning";

export type ReadinessTarget =
  | "productTitle"
  | "media"
  | "bodyHtml"
  | "productType"
  | "variantTable";

export interface ReadinessIssue {
  id: string;
  severity: ReadinessSeverity;
  title: string;
  description: string;
  target: ReadinessTarget;
  variantId?: number | string | null;
  field?: string;
}

export interface ReadinessResult {
  issues: ReadinessIssue[];
  errorIssues: ReadinessIssue[];
  warningIssues: ReadinessIssue[];
  errorCount: number;
  warningCount: number;
  completion: number;
}

interface ReadinessMedia {
  mediaId?: number | string | null;
  filename?: string | null;
  mediaContentType?: string | null;
}

interface ReadinessFormData {
  productTitle?: string | null;
  productType?: string | null;
  bodyHtml?: string | null;
  description?: string | null;
  mediaList?: ReadinessMedia[] | null;
}

interface ReadinessVariant {
  variantId?: number | string | null;
  sku?: string | null;
  mediaId?: number | string | null;
  isActiveAvailable?: string | null;
  purchasePrice?: number | null;
  pkLength?: number | null;
  pkWidth?: number | null;
  pkHeight?: number | null;
  pkWeight?: number | null;
  freight?: number | null;
  profitRate?: number | null;
  price?: number | null;
}

export interface EvaluateProductReadinessParams {
  formData: ReadinessFormData;
  variants: ReadinessVariant[];
  mediaList?: ReadinessMedia[] | null;
}

function hasText(value: unknown): boolean {
  return typeof value === "string" ? value.trim().length > 0 : value != null;
}

function hasPositiveNumber(value: unknown): boolean {
  return typeof value === "number" && Number.isFinite(value) && value > 0;
}

function hasNumber(value: unknown): boolean {
  return typeof value === "number" && Number.isFinite(value);
}

function isActiveVariant(variant: ReadinessVariant): boolean {
  return variant.isActiveAvailable !== "0";
}

function isImageMedia(media: ReadinessMedia): boolean {
  const contentType = media.mediaContentType || "";
  const filename = media.filename || "";
  return (
    contentType === "IMAGE" ||
    /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(filename)
  );
}

function getVariantLabel(variant: ReadinessVariant, index: number): string {
  return variant.sku?.trim() || `第 ${index + 1} 个变体`;
}

function getVariantIssueId(
  variant: ReadinessVariant,
  index: number,
  suffix: string,
): string {
  return `variant-${variant.variantId ?? index + 1}-${suffix}`;
}

function createIssue(issue: ReadinessIssue): ReadinessIssue {
  return issue;
}

export function evaluateProductReadiness({
  formData,
  variants,
  mediaList,
}: EvaluateProductReadinessParams): ReadinessResult {
  const issues: ReadinessIssue[] = [];
  const currentMediaList = mediaList || formData.mediaList || [];
  const currentMediaIds = new Set(
    currentMediaList
      .map((media) => media.mediaId)
      .filter((mediaId) => mediaId !== null && mediaId !== undefined),
  );
  const activeVariants = variants.filter(isActiveVariant);

  if (!hasText(formData.productTitle)) {
    issues.push(
      createIssue({
        id: "product-title-required",
        severity: "error",
        title: "商品标题不能为空",
        description: "Shopify 商品发布需要清晰的商品标题。",
        target: "productTitle",
        field: "productTitle",
      }),
    );
  }

  if (!currentMediaList.some(isImageMedia)) {
    issues.push(
      createIssue({
        id: "media-image-required",
        severity: "error",
        title: "至少需要一张商品图片",
        description: "Shopify 商品发布前应至少包含一张图片。",
        target: "media",
        field: "mediaList",
      }),
    );
  }

  if (!hasText(formData.bodyHtml) && !hasText(formData.description)) {
    issues.push(
      createIssue({
        id: "body-html-recommended",
        severity: "warning",
        title: "建议补充商品详情",
        description: "完整详情能减少发布后返工，也更利于买家理解商品。",
        target: "bodyHtml",
        field: "bodyHtml",
      }),
    );
  }

  if (!hasText(formData.productType)) {
    issues.push(
      createIssue({
        id: "product-type-recommended",
        severity: "warning",
        title: "建议填写商品类型",
        description: "商品类型有助于 Shopify 后续归类、筛选和运营。",
        target: "productType",
        field: "productType",
      }),
    );
  }

  activeVariants.forEach((variant, index) => {
    const label = getVariantLabel(variant, index);
    const variantId = variant.variantId ?? index + 1;

    if (!hasText(variant.sku)) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "sku-required"),
          severity: "error",
          title: `${label} 缺少 SKU`,
          description: "启用的变体必须有 SKU，避免 Shopify 同步后难以识别。",
          target: "variantTable",
          variantId,
          field: "sku",
        }),
      );
    }

    if (!hasPositiveNumber(variant.price)) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "price-required"),
          severity: "error",
          title: `${label} 缺少售价`,
          description: "启用的变体售价需要大于 0。",
          target: "variantTable",
          variantId,
          field: "price",
        }),
      );
    }

    if (variant.mediaId != null && !currentMediaIds.has(variant.mediaId)) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "media-missing"),
          severity: "error",
          title: `${label} 绑定的规格图不存在`,
          description: "该变体绑定的图片不在当前媒体列表中，保存时后端会拒绝。",
          target: "variantTable",
          variantId,
          field: "mediaId",
        }),
      );
    }

    if (!hasNumber(variant.purchasePrice) || variant.purchasePrice == null) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "purchase-price-recommended"),
          severity: "warning",
          title: `${label} 建议填写采购价`,
          description: "采购价缺失会影响成本和利润判断。",
          target: "variantTable",
          variantId,
          field: "purchasePrice",
        }),
      );
    }

    if (
      !hasPositiveNumber(variant.pkLength) ||
      !hasPositiveNumber(variant.pkWidth) ||
      !hasPositiveNumber(variant.pkHeight)
    ) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "package-size-recommended"),
          severity: "warning",
          title: `${label} 建议补全包装尺寸`,
          description: "包装长宽高缺失会影响材积重和运费计算。",
          target: "variantTable",
          variantId,
          field: "packageSize",
        }),
      );
    }

    if (!hasPositiveNumber(variant.pkWeight)) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "weight-recommended"),
          severity: "warning",
          title: `${label} 建议填写实重`,
          description: "实重缺失会降低运费计算可信度。",
          target: "variantTable",
          variantId,
          field: "pkWeight",
        }),
      );
    }

    if (!hasNumber(variant.freight) || variant.freight == null) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "freight-recommended"),
          severity: "warning",
          title: `${label} 建议填写运费`,
          description: "运费缺失会影响成本价和利润率。",
          target: "variantTable",
          variantId,
          field: "freight",
        }),
      );
    }

    if (!hasNumber(variant.profitRate) || variant.profitRate == null) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "profit-rate-recommended"),
          severity: "warning",
          title: `${label} 建议确认利润率`,
          description: "利润率缺失时，用户难以判断该售价是否可发布。",
          target: "variantTable",
          variantId,
          field: "profitRate",
        }),
      );
    }

    if (variant.mediaId == null) {
      issues.push(
        createIssue({
          id: getVariantIssueId(variant, index, "media-recommended"),
          severity: "warning",
          title: `${label} 建议绑定规格图`,
          description: "有规格图的变体更便于买家选择，也减少同步后返工。",
          target: "variantTable",
          variantId,
          field: "mediaId",
        }),
      );
    }
  });

  const errorIssues = issues.filter((issue) => issue.severity === "error");
  const warningIssues = issues.filter((issue) => issue.severity === "warning");
  const totalChecks = 5 + Math.max(activeVariants.length, 1) * 8;
  const failedChecks = issues.length;
  const completion =
    failedChecks === 0
      ? 100
      : Math.max(0, Math.round(((totalChecks - failedChecks) / totalChecks) * 100));

  return {
    issues,
    errorIssues,
    warningIssues,
    errorCount: errorIssues.length,
    warningCount: warningIssues.length,
    completion,
  };
}
