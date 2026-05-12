import request from '@/utils/request'
import type { ApiResponse, Product, PageQuery, ProductWorkbenchSummary } from '@/types/erp'

export interface ProductQuery extends PageQuery {
  searchKeyword?: string | null
  productTitle?: string | null
  spu?: string | null
  status?: string | null
  syncStatus?: string | null
  qualityState?: string | null
  storeId?: number | null
  tagId?: number | null
  tagIds?: number[] | null
  category?: string | null
  productType?: string | null
  sourceUrl?: string | null
  purchaseUrl?: string | null
  mainMediaId?: number | null
  updatedBy?: string | null
  lastSyncTime?: string | null
  params?: Record<string, unknown>
}

export interface ProductPushRequest {
  productQuery?: ProductQuery
  productIds?: number[]
  storeId?: number
}

export function listProduct(query: ProductQuery): Promise<ApiResponse<Product[]>> {
  return request({
    url: '/erp/product/list',
    method: 'get',
    params: query
  })
}

export function getProductWorkbenchSummary(query: ProductQuery): Promise<ApiResponse<ProductWorkbenchSummary>> {
  return request({
    url: '/erp/product/workbench-summary',
    method: 'get',
    params: query
  })
}

export function getProduct(productId: number): Promise<ApiResponse<Product>> {
  return request({
    url: '/erp/product/' + productId,
    method: 'get'
  })
}

export function addProduct(data: Product): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/product',
    method: 'post',
    data: data
  })
}

export function updateProduct(data: Product): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/product',
    method: 'put',
    data: data
  })
}

export function delProduct(productId: number): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/product/' + productId,
    method: 'delete'
  })
}

export function importProduct(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/product/importData',
    method: 'post',
    data: data
  })
}

export function importTemplateProduct(): Promise<Blob> {
  return request({
    url: '/erp/product/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}

export function pushBatch(data: ProductPushRequest): Promise<ApiResponse<{ taskId: number }>> {
  return request({
    url: '/erp/product/push-batch',
    method: 'post',
    data: data
  })
}

export function publishChannels(data: ProductPushRequest): Promise<ApiResponse<any>> {
  return request({
    url: '/erp/product/publish-channels',
    method: 'post',
    data
  })
}

export function getPushResult(taskId: number): Promise<ApiResponse<any>> {
  return request({
    url: '/erp/product/push-result/' + taskId,
    method: 'get'
  })
}
