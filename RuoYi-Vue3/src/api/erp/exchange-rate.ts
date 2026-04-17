import request from '@/utils/request'
import type { ApiResponse, ExchangeRate } from '@/types/erp'

export function getTodayRate(baseCurrency: string): Promise<ApiResponse<ExchangeRate>> {
  return request({
    url: '/erp/exchange-rate/today/' + baseCurrency,
    method: 'get'
  })
}

export function getUsdRate(): Promise<ApiResponse<ExchangeRate>> {
  return request({
    url: '/erp/exchange-rate/today/USD',
    method: 'get'
  })
}
