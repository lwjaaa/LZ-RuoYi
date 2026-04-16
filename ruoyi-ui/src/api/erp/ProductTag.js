import request from '@/utils/request'

// 查询erp商品与标签关联列表
export function listProductTag(query) {
  return request({
    url: '/erp/ProductTag/list',
    method: 'get',
    params: query
  })
}

// 查询erp商品与标签关联详细
export function getProductTag(relId) {
  return request({
    url: '/erp/ProductTag/' + relId,
    method: 'get'
  })
}

// 新增erp商品与标签关联
export function addProductTag(data) {
  return request({
    url: '/erp/ProductTag',
    method: 'post',
    data: data
  })
}

// 修改erp商品与标签关联
export function updateProductTag(data) {
  return request({
    url: '/erp/ProductTag',
    method: 'put',
    data: data
  })
}

// 删除erp商品与标签关联
export function delProductTag(relId) {
  return request({
    url: '/erp/ProductTag/' + relId,
    method: 'delete'
  })
}

// 导入erp商品与标签关联
export function importProductTag(data) {
  return request({
    url: '/erp/ProductTag/importData',
    method: 'post',
    data: data
  })
}

// 下载erp商品与标签关联导入模板
export function importTemplateProductTag() {
  return request({
    url: '/erp/ProductTag/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
