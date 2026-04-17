import request from '@/utils/request'
import type { ApiResponse, ProductVariant, PageQuery } from '@/types/erp'

export interface VariantQuery extends PageQuery {
  productId?: number
  sku?: string
}

export function listVariant(query: VariantQuery): Promise<ApiResponse<ProductVariant[]>> {
  return request({
    url: '/erp/variant/list',
    method: 'get',
    params: query
  })
}

export function getVariant(variantId: number): Promise<ApiResponse<ProductVariant>> {
  return request({
    url: '/erp/variant/' + variantId,
    method: 'get'
  })
}

export function addVariant(data: ProductVariant): Promise<ApiResponse<number>> {
  return request({
    url: '/erp/variant',
    method: 'post',
    data: data
  })
}

export function updateVariant(data: ProductVariant): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/variant',
    method: 'put',
    data: data
  })
}

export function delVariant(variantId: number): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/variant/' + variantId,
    method: 'delete'
  })
}

export function importVariant(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/variant/importData',
    method: 'post',
    data: data
  })
}

export function importTemplateVariant(): Promise<Blob> {
  return request({
    url: '/erp/variant/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
