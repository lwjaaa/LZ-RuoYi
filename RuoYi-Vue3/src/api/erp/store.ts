import request from '@/utils/request'
import type { ApiResponse, PageQuery, ShopifyApiCallRequest, ShopifyApiCallResponse, ShopifyProductImportCursor, ShopifyResourceOption, ShopifyStore, ShopifyStoreQuery } from '@/types/erp'

export type { ShopifyStore, ShopifyStoreQuery, ShopifyResourceOption, ShopifyApiCallRequest, ShopifyApiCallResponse, ShopifyProductImportCursor }

export function listStore(query: ShopifyStoreQuery & PageQuery): Promise<ApiResponse<ShopifyStore[]>> {
  return request({
    url: '/erp/store/list',
    method: 'get',
    params: query
  })
}

export function listActiveStores(): Promise<ApiResponse<ShopifyStore[]>> {
  return request({
    url: '/erp/store/active',
    method: 'get'
  })
}

export function getStore(storeId: number): Promise<ApiResponse<ShopifyStore>> {
  return request({
    url: '/erp/store/' + storeId,
    method: 'get'
  })
}

export function addStore(data: ShopifyStore): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/store',
    method: 'post',
    data
  })
}

export function updateStore(data: ShopifyStore): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/store',
    method: 'put',
    data
  })
}

export function delStore(storeId: number | string): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/store/' + storeId,
    method: 'delete'
  })
}

export function testStoreConnection(storeId: number): Promise<ApiResponse<string>> {
  return request({
    url: '/erp/store/test/' + storeId,
    method: 'post'
  })
}

export function fetchStoreLocations(storeId: number): Promise<ApiResponse<ShopifyResourceOption[]>> {
  return request({
    url: `/erp/store/${storeId}/locations`,
    method: 'get'
  })
}

export function fetchStorePublications(storeId: number): Promise<ApiResponse<ShopifyResourceOption[]>> {
  return request({
    url: `/erp/store/${storeId}/publications`,
    method: 'get'
  })
}

export function callStoreApi(storeId: number, data: ShopifyApiCallRequest): Promise<ApiResponse<ShopifyApiCallResponse>> {
  return request({
    url: `/erp/store/${storeId}/api-call`,
    method: 'post',
    data
  })
}

export function importShopifyProductsFull(storeId: number): Promise<ApiResponse<number>> {
  return request({
    url: `/erp/store/${storeId}/shopify-products/import-full`,
    method: 'post'
  })
}

export function importShopifyProductsIncremental(storeId: number): Promise<ApiResponse<number>> {
  return request({
    url: `/erp/store/${storeId}/shopify-products/import-incremental`,
    method: 'post'
  })
}

export function getShopifyProductImportCursor(storeId: number): Promise<ApiResponse<ShopifyProductImportCursor>> {
  return request({
    url: `/erp/store/${storeId}/shopify-products/import-cursor`,
    method: 'get'
  })
}
