import request from '@/utils/request'

// 查询erp商品变体列表
export function listVariant(query) {
  return request({
    url: '/erp/variant/list',
    method: 'get',
    params: query
  })
}

// 查询erp商品变体详细
export function getVariant(variantId) {
  return request({
    url: '/erp/variant/' + variantId,
    method: 'get'
  })
}

// 新增erp商品变体
export function addVariant(data) {
  return request({
    url: '/erp/variant',
    method: 'post',
    data: data
  })
}

// 修改erp商品变体
export function updateVariant(data) {
  return request({
    url: '/erp/variant',
    method: 'put',
    data: data
  })
}

// 删除erp商品变体
export function delVariant(variantId) {
  return request({
    url: '/erp/variant/' + variantId,
    method: 'delete'
  })
}

// 导入erp商品变体
export function importVariant(data) {
  return request({
    url: '/erp/variant/importData',
    method: 'post',
    data: data
  })
}

// 下载erp商品变体导入模板
export function importTemplateVariant() {
  return request({
    url: '/erp/variant/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
