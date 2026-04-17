import request from '@/utils/request'
import type { ApiResponse, Task, PageQuery } from '@/types/erp'

export interface TaskQuery extends PageQuery {
  taskName?: string
  taskGroup?: string
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

export function delTask(taskId: number): Promise<ApiResponse<void>> {
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
