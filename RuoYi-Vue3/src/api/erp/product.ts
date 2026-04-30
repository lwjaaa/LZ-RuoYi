import request from '@/utils/request'
import type { ApiResponse, Product, PageQuery } from '@/types/erp'

export interface ProductQuery extends PageQuery {
  productTitle?: string | null
  spu?: string | null
  status?: string | null
  syncStatus?: string | null
  tagId?: number | null
  category?: string | null
  productType?: string | null
  mainMediaId?: number | null
  lastSyncTime?: string | null
}

export interface ProductPushRequest {
  category?: string
  tagIds?: string
  syncStatus?: string
  selectAll?: boolean
  productQuery?: ProductQuery
}

export function listProduct(query: ProductQuery): Promise<ApiResponse<Product[]>> {
  return request({
    url: '/erp/product/list',
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

export function pushBatch(request: ProductPushRequest): Promise<ApiResponse<{ taskId: number }>> {
  return request({
    url: '/erp/product/push-batch',
    method: 'post',
    data: request
  })
}

export function getPushResult(taskId: number): Promise<ApiResponse<any>> {
  return request({
    url: '/erp/product/push-result/' + taskId,
    method: 'get'
  })
}
