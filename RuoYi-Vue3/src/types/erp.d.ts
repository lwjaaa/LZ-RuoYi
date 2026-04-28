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
  imageSearchKeyword?: string
}

export interface ProductOption {
  optionId?: string
  englishName: string
  chineseName: string
  collapsed: boolean
  values: ProductOptionValue[]
}

export interface ProductOptionValue {
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
  filename?: string
  alt?: string
  position?: number
  mediaContentType?: string
  createTime?: Date
  updateTime?: Date
}

export interface Task {
  taskId: number
  taskName?: string
  taskGroup?: string
  invokeTarget?: string
  cronExpression?: string
  misfirePolicy?: string
  concurrent?: string
  status?: string
  createBy?: string
  createTime?: Date
  updateBy?: string
  updateTime?: Date
  remark?: string
  params?: Record<string, unknown>
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
  imageSearchKeyword: string;
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
