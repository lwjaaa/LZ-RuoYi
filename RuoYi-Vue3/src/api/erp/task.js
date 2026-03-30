import request from '@/utils/request'

// 查询Shopify 任务配置列表
export function listTask(query) {
  return request({
    url: '/erp/task/list',
    method: 'get',
    params: query
  })
}

// 查询Shopify 任务配置详细
export function getTask(taskId) {
  return request({
    url: '/erp/task/' + taskId,
    method: 'get'
  })
}

// 新增Shopify 任务配置
export function addTask(data) {
  return request({
    url: '/erp/task',
    method: 'post',
    data: data
  })
}

// 修改Shopify 任务配置
export function updateTask(data) {
  return request({
    url: '/erp/task',
    method: 'put',
    data: data
  })
}

// 删除Shopify 任务配置
export function delTask(taskId) {
  return request({
    url: '/erp/task/' + taskId,
    method: 'delete'
  })
}

// 导入Shopify 任务配置
export function importTask(data) {
  return request({
    url: '/erp/task/importData',
    method: 'post',
    data: data
  })
}

// 下载Shopify 任务配置导入模板
export function importTemplateTask() {
  return request({
    url: '/erp/task/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
