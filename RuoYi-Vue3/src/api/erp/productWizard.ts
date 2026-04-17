import request from '@/utils/request'
import type { ApiResponse, SelectionInfoData, BaseInfoData, ShippingCalcData, ShippingResult, ExchangeRate } from '@/types/erp'

export function addSelectionInfo(data: SelectionInfoData): Promise<ApiResponse<number>> {
  return request({
    url: '/erp/product/wizard/selectionInfo',
    method: 'post',
    data: data
  })
}

export function updateBaseInfo(data: BaseInfoData): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/product/wizard/baseInfo',
    method: 'post',
    data: data
  })
}

export function calculateShipping(data: ShippingCalcData): Promise<ApiResponse<ShippingResult>> {
  return request({
    url: '/erp/product/wizard/calculateShipping',
    method: 'post',
    data: data
  })
}

export function getUsdRate(): Promise<ApiResponse<ExchangeRate>> {
  return request({
    url: '/erp/product/wizard/getUsdRate',
    method: 'get'
  })
}
