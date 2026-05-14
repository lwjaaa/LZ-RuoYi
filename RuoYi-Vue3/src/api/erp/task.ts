import request from '@/utils/request'
import type { ApiResponse, Task, ShopifyTaskDetail, ShopifyTaskDiagnostics, PageQuery } from '@/types/erp'

export interface TaskQuery extends PageQuery {
  taskName?: string
  shopName?: string
  taskType?: string
  taskStatus?: string
}

export interface TaskDetailQuery extends PageQuery {
  productId?: number | string
  itemType?: string
  status?: string
}

export function listTask(query: TaskQuery): Promise<ApiResponse<Task[]>> {
  return request({
    url: '/erp/task/list',
    method: 'get',
    params: query
  })
}

export function getTask(taskId: number): Promise<ApiResponse<Task>> {
  return request({
    url: '/erp/task/' + taskId,
    method: 'get'
  })
}

export function listTaskDetails(taskId: number, query?: TaskDetailQuery): Promise<ApiResponse<ShopifyTaskDetail[]>> {
  return request({
    url: `/erp/task/${taskId}/details`,
    method: 'get',
    params: query
  })
}

export function getTaskDiagnostics(taskId: number): Promise<ApiResponse<ShopifyTaskDiagnostics>> {
  return request({
    url: `/erp/task/${taskId}/diagnostics`,
    method: 'get'
  })
}

export function addTask(data: Task): Promise<ApiResponse<number>> {
  return request({
    url: '/erp/task',
    method: 'post',
    data: data
  })
}

export function updateTask(data: Task): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/task',
    method: 'put',
    data: data
  })
}

export function delTask(taskId: number | string): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/task/' + taskId,
    method: 'delete'
  })
}

export function importTask(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: '/erp/task/importData',
    method: 'post',
    data: data
  })
}

export function importTemplateTask(): Promise<Blob> {
  return request({
    url: '/erp/task/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
