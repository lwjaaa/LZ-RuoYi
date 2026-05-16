import request from '@/utils/request'
import type {
  ApiResponse,
  FulfillmentRecord,
  OrderProfitSummary,
  PageQuery,
  PurchaseTask,
  RefundRecord,
  ShopifyOrder,
  ShopifyOrderSyncCursor
} from '@/types/erp'

export interface ShopifyOrderQuery extends PageQuery {
  storeId?: number
  orderName?: string
  financialStatus?: string
  fulfillmentStatus?: string
  purchaseStatus?: string
  fulfillmentSyncStatus?: string
  searchKeyword?: string
  beginDate?: string
  endDate?: string
}

export interface PurchaseTaskQuery extends PageQuery {
  storeId?: number
  purchaseStatus?: string
  searchKeyword?: string
}

export interface PurchaseTaskEdit {
  purchaseTaskId?: number
  purchaseStatus?: string
  actualPurchaseAmount?: number
  exceptionReason?: string
  remark?: string
}

export interface FulfillmentRecordQuery extends PageQuery {
  storeId?: number
  orderId?: number
  syncStatus?: string
  searchKeyword?: string
}

export interface FulfillmentSubmitRequest {
  orderId?: number
  trackingCompany?: string
  trackingNumber?: string
  trackingUrl?: string
  shippingFee?: number
}

export interface RefundRecordQuery extends PageQuery {
  storeId?: number
  orderId?: number
  searchKeyword?: string
}

export interface ProfitReportQuery extends PageQuery {
  storeId?: number
  beginDate?: string
  endDate?: string
}

export function listOrder(query: ShopifyOrderQuery): Promise<ApiResponse<ShopifyOrder[]>> {
  return request({
    url: '/erp/order/list',
    method: 'get',
    params: query
  })
}

export function getOrder(orderId: number): Promise<ApiResponse<ShopifyOrder>> {
  return request({
    url: `/erp/order/${orderId}`,
    method: 'get'
  })
}

export function syncOrders(storeId: number): Promise<ApiResponse<number>> {
  return request({
    url: `/erp/order/sync/${storeId}`,
    method: 'post'
  })
}

export function backfillOrders(storeId: number): Promise<ApiResponse<number>> {
  return request({
    url: `/erp/order/backfill/${storeId}`,
    method: 'post'
  })
}

export function getOrderCursor(storeId: number): Promise<ApiResponse<ShopifyOrderSyncCursor>> {
  return request({
    url: `/erp/order/cursor/${storeId}`,
    method: 'get'
  })
}

export function listPurchaseTask(query: PurchaseTaskQuery): Promise<ApiResponse<PurchaseTask[]>> {
  return request({
    url: '/erp/purchase-task/list',
    method: 'get',
    params: query
  })
}

export function updatePurchaseTask(data: PurchaseTaskEdit): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/purchase-task',
    method: 'put',
    data
  })
}

export function listFulfillment(query: FulfillmentRecordQuery): Promise<ApiResponse<FulfillmentRecord[]>> {
  return request({
    url: '/erp/fulfillment/list',
    method: 'get',
    params: query
  })
}

export function submitFulfillment(data: FulfillmentSubmitRequest): Promise<ApiResponse<FulfillmentRecord>> {
  return request({
    url: '/erp/fulfillment/submit',
    method: 'post',
    data
  })
}

export function listRefund(query: RefundRecordQuery): Promise<ApiResponse<RefundRecord[]>> {
  return request({
    url: '/erp/refund/list',
    method: 'get',
    params: query
  })
}

export function getProfitSummary(query: ProfitReportQuery): Promise<ApiResponse<OrderProfitSummary>> {
  return request({
    url: '/erp/profit/summary',
    method: 'get',
    params: query
  })
}
