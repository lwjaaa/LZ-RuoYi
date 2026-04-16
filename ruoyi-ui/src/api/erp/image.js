import request from '@/utils/request'

// 查询erp图片列表
export function listImage(query) {
  return request({
    url: '/erp/image/list',
    method: 'get',
    params: query
  })
}

// 查询erp图片详细
export function getImage(imageId) {
  return request({
    url: '/erp/image/' + imageId,
    method: 'get'
  })
}

// 新增erp图片
export function addImage(data) {
  return request({
    url: '/erp/image',
    method: 'post',
    data: data
  })
}

// 修改erp图片
export function updateImage(data) {
  return request({
    url: '/erp/image',
    method: 'put',
    data: data
  })
}

// 删除erp图片
export function delImage(imageId) {
  return request({
    url: '/erp/image/' + imageId,
    method: 'delete'
  })
}

// 导入erp图片
export function importImage(data) {
  return request({
    url: '/erp/image/importData',
    method: 'post',
    data: data
  })
}

// 下载erp图片导入模板
export function importTemplateImage() {
  return request({
    url: '/erp/image/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
