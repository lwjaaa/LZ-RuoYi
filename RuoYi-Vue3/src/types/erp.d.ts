import type { AxiosPromise } from 'axios'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  rows?: T[]
  total?: number
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
}

export interface Product {
  productId: number
  shopifyProductId?: string
  productTitle: string
  productName?: string
  spu: string
  category?: string
  productType?: string
  sourceUrl?: string
  purchaseUrl?: string
  optionJson?: string
  optionList?: ProductOption[]
  status?: string
  bodyHtml?: string
  mainMediaId?: number
  syncStatus?: string
  syncMessage?: string
  lastSyncTime?: Date
  version?: number
  description?: string
  descriptionCn?: string
  size?: string
  material?: string
  note?: string
  noteCn?: string
  packageInclude?: string
  createBy?: string
  createTime?: Date
  updateBy?: string
  updateTime?: Date
  remark?: string
  delFlag?: string
  productVariantList?: ProductVariant[]
  tagIds?: number[]
  params?: Record<string, unknown>
  mediaList?: Media[]
  tagCodeList?: string[]
  mainMediaUrl?: string
  mediaUrlList?: string[]
  mediaCount?: number
  variantCount?: number
  skuPreview?: string
  priceMin?: number | null
  priceMax?: number | null
  purchasePriceMin?: number | null
  purchasePriceMax?: number | null
  profitRateMin?: number | null
  profitRateMax?: number | null
  missingFields?: string[]
  needResync?: boolean
  latestTaskId?: number
  latestTaskStatus?: string
  latestTaskError?: string
}

export interface ProductWorkbenchSummary {
  totalCount?: number
  pendingPushCount?: number
  syncFailedCount?: number
  syncingCount?: number
  syncedCount?: number
  needResyncCount?: number
  incompleteCount?: number
}

export interface ProductOption {
  optionId?: string
  englishName: string
  chineseName: string
  collapsed: boolean
  values: ProductOptionValue[]
}

export interface ProductOptionValue {
  valueId?: string
  englishValue: string
  chineseValue: string
}

export interface ProductVariant {
  variantId: number
  productId: number
  shopifyVariantId?: string
  sku: string
  price: number | null
  compareAtPrice?: number | null
  purchasePrice?: number | null
  purchaseUrl?: string
  optionValues: string
  optionValueList: ProductVariantOption[]
  mediaId?: number
  media?: Media
  position: number
  pkWidth?: number
  pkHeight?: number
  pkLength?: number
  materialWeight?: number
  pkWeight?: number
  freight?: number | null
  isActualShipment?: string
  isActiveAvailable?: string
  unitCostPrice?: number | null
  exchangeRate?: number | null
  suggestedPrice?: number | null
  profit?: number | null
  profitRate?: number | null
  createBy?: string
  createTime?: Date
  updateBy?: string
  updateTime?: Date
  remark?: string
  delFlag?: string
  params?: Record<string, unknown>
}

export interface ProductVariantOption {
  optionId: string
  valueId?: string
  englishName: string
  chineseName: string
  englishValue?: string
  chineseValue?: string
}

export interface TagDictMenu {
  tagId: number
  tagName: string
  tagCode?: string
  tagType?: string
  sortOrder?: number
  parentId?: number
  ancestors?: string
  menuLevel?: number
  spuPrefix?: string
  currentMaxSeq?: number
  children?: TagDictMenu[]
}

export interface Media {
  mediaId: number
  productId?: number
  shopifyMediaId?: string
  shopifyMediaUrl?: string
  stagedUploadUrl?: string
  nasMediaUrl?: string
  transcodedMediaUrl?: string
  filename?: string
  alt?: string
  position?: number
  mediaContentType?: string
  createTime?: Date
  updateTime?: Date
}

export interface Task {
  taskId: number
  storeId?: number
  shopName?: string
  taskName?: string
  taskGroup?: string
  taskType?: string
  businessType?: string
  businessIds?: string
  taskStatus?: string
  progress?: number
  errorMessage?: string
  resultData?: string
  executionTime?: number
  startTime?: Date
  endTime?: Date
  totalCount?: number
  successCount?: number
  failedCount?: number
  createBy?: string
  createTime?: Date
  updateBy?: string
  updateTime?: Date
  remark?: string
  params?: Record<string, unknown>
}

export interface ShopifyStore {
  storeId?: number
  storeName: string
  shopName: string
  apiVersion: string
  apiKey?: string
  apiSecret?: string
  accessToken?: string
  refreshToken?: string
  hasApiSecret?: boolean
  hasAccessToken?: boolean
  hasRefreshToken?: boolean
  tokenExpiresAt?: string | Date | null
  baseUrl?: string
  inventoryLocationId?: string
  inventoryLocationName?: string
  inventoryTracked?: string
  defaultInventoryQuantity?: number
  inventoryPolicy?: string
  publishPublicationIds?: string
  publishPublicationNames?: string
  defaultProductStatus?: string
  requiredProductFields?: string
  availablePublicationIds?: string
  isActive?: string
  isDefault?: string
  authMode?: string
  status?: string
  lastSyncTime?: string | Date | null
  syncCount?: number
  createBy?: string
  createTime?: string | Date
  updateBy?: string
  updateTime?: string | Date
  remark?: string
}

export interface ShopifyStoreQuery extends PageQuery {
  storeName?: string
  shopName?: string
  isActive?: string
  isDefault?: string
  authMode?: string
  status?: string
}

export interface ShopifyResourceOption {
  id: string
  name: string
}

export interface ProductTag {
  relId: number
  productId: number
  tagId: number
  createBy?: string
  createTime?: Date
  updateBy?: string
  updateTime?: Date
  remark?: string
}

export interface ExchangeRate {
  rate: number
  rateDate: string
  baseCurrency?: string
  targetCurrency?: string
}

export interface SelectionInfoData {
  tagIds?: number[]
  spu?: string
  sourceUrl?: string
  purchaseUrl?: string
  optionList?: ProductOption[]
}

export interface BaseInfoData {
  productId: number | undefined;
  spu: string;
  productTitle: string;
  category: string;
  productType: string;
  description: string;
  descriptionCn: string;
  size: string;
  material: string;
  note: string;
  noteCn: string;
  packageInclude: string;
  bodyHtml: string;
  mediaList: Media[];
  remark: string;
  productVariantList?: ProductVariant[]
}

export interface ShippingCalcData {
  pkWidth?: number
  pkHeight?: number
  pkLength?: number
  pkWeight?: number
}

export interface ShippingResult {
  lumpSumFee: number
  estimatedTime?: string
  logisticsProductCode: string
  chargeWeight?: number
  isShowTrack?: string
  isVolumeCargo?: string
  remarks?: string
  trackingNo?: string
  refNo?: string
}

export interface DragNodeData {
  tagId: number
  targetParentId: number
  targetSortOrder?: number
}

export interface ScanMediaParams {
  dirPath: string
  productId?: number
}

export type RequestResult<T = unknown> = AxiosPromise<ApiResponse<T>>
