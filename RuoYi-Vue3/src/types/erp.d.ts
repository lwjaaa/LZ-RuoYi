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
  size?: string
  material?: string
  note?: string
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
  price: number
  compareAtPrice?: number
  purchasePrice?: number
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
  freight?: number
  isActualShipment?: string
  unitCostPrice?: number
  exchangeRate?: number
  suggestedPrice?: number
  profitRate?: number
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
  productId: number
  productTitle?: string
  bodyHtml?: string
  mediaList?: Media[]
  productVariantList?: ProductVariant[]
}

export interface ShippingCalcData {
  pkWidth?: number
  pkHeight?: number
  pkLength?: number
  pkWeight?: number
}

export interface ShippingResult {
  freight: number
  currency?: string
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
