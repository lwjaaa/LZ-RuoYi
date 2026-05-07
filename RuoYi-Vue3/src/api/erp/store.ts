import request from '@/utils/request'
import type { ApiResponse, PageQuery, ShopifyResourceOption, ShopifyStore, ShopifyStoreQuery } from '@/types/erp'

export type { ShopifyStore, ShopifyStoreQuery, ShopifyResourceOption }

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
