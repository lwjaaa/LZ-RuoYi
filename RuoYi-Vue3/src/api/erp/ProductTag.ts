import request from '@/utils/request'
import type { ApiResponse, ProductTag, PageQuery } from '@/types/erp'

export interface ProductTagQuery extends PageQuery {
  productId?: number
  tagId?: number
}

export function listProductTag(query: ProductTagQuery): Promise<ApiResponse<ProductTag[]>> {
  return request({
    url: '/vh-erp/ProductTag/list',
    method: 'get',
    params: query
  })
}

export function getProductTag(relId: number): Promise<ApiResponse<ProductTag>> {
  return request({
    url: '/vh-erp/ProductTag/' + relId,
    method: 'get'
  })
}

export function addProductTag(data: ProductTag): Promise<ApiResponse<number>> {
  return request({
    url: '/vh-erp/ProductTag',
    method: 'post',
    data: data
  })
}

export function updateProductTag(data: ProductTag): Promise<ApiResponse<void>> {
  return request({
    url: '/vh-erp/ProductTag',
    method: 'put',
    data: data
  })
}

export function delProductTag(relId: number): Promise<ApiResponse<void>> {
  return request({
    url: '/vh-erp/ProductTag/' + relId,
    method: 'delete'
  })
}

export function importProductTag(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: '/vh-erp/ProductTag/importData',
    method: 'post',
    data: data
  })
}

export function importTemplateProductTag(): Promise<Blob> {
  return request({
    url: '/vh-erp/ProductTag/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
