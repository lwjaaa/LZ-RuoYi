export type BulkVariantField =
  | "sku"
  | "media"
  | "purchasePrice"
  | "pkLength"
  | "pkWidth"
  | "pkHeight"
  | "pkWeight"
  | "isActualShipment"
  | "freight"
  | "unitCostPrice"
  | "exchangeRate"
  | "price"
  | "compareAtPrice";

export interface BulkEditMedia {
  mediaId?: number | string | null;
  filename?: string | null;
  nasMediaUrl?: string | null;
  shopifyMediaUrl?: string | null;
}

export interface BulkEditableVariant {
  variantId?: number | string | null;
  sku?: string | null;
  mediaId?: number | string | null;
  media?: BulkEditMedia | null;
  purchasePrice?: number | null;
  pkLength?: number | null;
  pkWidth?: number | null;
  pkHeight?: number | null;
  pkWeight?: number | null;
  isActualShipment?: string | null;
  freight?: number | null;
  unitCostPrice?: number | null;
  exchangeRate?: number | null;
  price?: number | null;
  compareAtPrice?: number | null;
}

export interface BulkVariantPatch {
  field: BulkVariantField;
  value?: string | number | null;
  media?: BulkEditMedia | null;
}

export const BULK_EDITABLE_FIELDS: BulkVariantField[] = [
  "sku",
  "media",
  "purchasePrice",
  "pkLength",
  "pkWidth",
  "pkHeight",
  "pkWeight",
  "isActualShipment",
  "freight",
  "unitCostPrice",
  "exchangeRate",
  "price",
  "compareAtPrice",
];

export const FILL_DOWN_FIELDS: BulkVariantField[] = [
  "sku",
  "media",
  "purchasePrice",
  "pkLength",
  "pkWidth",
  "pkHeight",
  "pkWeight",
  "isActualShipment",
  "freight",
  "unitCostPrice",
  "exchangeRate",
  "price",
  "compareAtPrice",
];

export function createFieldPatch(
  field: Exclude<BulkVariantField, "media">,
  value: string | number | null,
): BulkVariantPatch {
  return {
    field,
    value,
  };
}

export function createMediaPatch(media: BulkEditMedia | null): BulkVariantPatch {
  return {
    field: "media",
    media,
  };
}

function isSelected(
  variant: BulkEditableVariant,
  selectedVariantIds: Array<number | string>,
): boolean {
  return variant.variantId != null && selectedVariantIds.includes(variant.variantId);
}

function cloneVariant<T extends BulkEditableVariant>(variant: T): T {
  return {
    ...variant,
  };
}

function applyPatchToVariant<T extends BulkEditableVariant>(
  variant: T,
  patch: BulkVariantPatch,
): T {
  const nextVariant = cloneVariant(variant);

  if (patch.field === "media") {
    nextVariant.media = patch.media || undefined;
    nextVariant.mediaId = patch.media?.mediaId ?? undefined;
    return nextVariant;
  }

  nextVariant[patch.field] = patch.value as never;
  return nextVariant;
}

export function applyBulkVariantPatch<T extends BulkEditableVariant>(
  variants: T[],
  selectedVariantIds: Array<number | string>,
  patch: BulkVariantPatch,
): T[] {
  if (selectedVariantIds.length === 0) {
    return variants;
  }

  return variants.map((variant) =>
    isSelected(variant, selectedVariantIds)
      ? applyPatchToVariant(variant, patch)
      : variant,
  );
}

export function buildFillDownValues(
  sourceVariant: BulkEditableVariant,
): Partial<BulkEditableVariant> {
  return {
    sku: sourceVariant.sku,
    mediaId: sourceVariant.mediaId,
    media: sourceVariant.media,
    purchasePrice: sourceVariant.purchasePrice,
    pkLength: sourceVariant.pkLength,
    pkWidth: sourceVariant.pkWidth,
    pkHeight: sourceVariant.pkHeight,
    pkWeight: sourceVariant.pkWeight,
    isActualShipment: sourceVariant.isActualShipment,
    freight: sourceVariant.freight,
    unitCostPrice: sourceVariant.unitCostPrice,
    exchangeRate: sourceVariant.exchangeRate,
    price: sourceVariant.price,
    compareAtPrice: sourceVariant.compareAtPrice,
  };
}

export function applyFirstSelectedFillDown<T extends BulkEditableVariant>(
  variants: T[],
  selectedVariantIds: Array<number | string>,
): T[] {
  if (selectedVariantIds.length < 2) {
    return variants;
  }

  const sourceId = selectedVariantIds[0];
  const sourceVariant = variants.find((variant) => variant.variantId === sourceId);
  if (!sourceVariant) {
    return variants;
  }

  const fillValues = buildFillDownValues(sourceVariant);
  return variants.map((variant) => {
    if (variant.variantId === sourceId || !isSelected(variant, selectedVariantIds)) {
      return variant;
    }
    return {
      ...variant,
      ...fillValues,
    };
  });
}
