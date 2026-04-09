import request from "@/utils/request";

// 查询erp媒体列表
export function listMedia(query) {
  return request({
    url: "/erp/media/list",
    method: "get",
    params: query,
  });
}

// 查询erp媒体详细
export function getMedia(mediaId) {
  return request({
    url: "/erp/media/" + mediaId,
    method: "get",
  });
}

// 新增erp媒体
export function addMedia(data) {
  return request({
    url: "/erp/media",
    method: "post",
    data: data,
  });
}

// 修改erp媒体
export function updateMedia(data) {
  return request({
    url: "/erp/media",
    method: "put",
    data: data,
  });
}

// 删除erp媒体
export function delMedia(mediaId) {
  return request({
    url: "/erp/media/" + mediaId,
    method: "delete",
  });
}

// 导入erp媒体
export function importMedia(data) {
  return request({
    url: "/erp/media/importData",
    method: "post",
    data: data,
  });
}

// 下载erp媒体导入模板
export function importTemplateMedia() {
  return request({
    url: "/erp/media/importTemplate",
    method: "post",
    responseType: "blob",
  });
}

// 扫描媒体
export function scanMedia({ dirPath, productId }) {
  return request({
    url: "/erp/media/scan",
    method: "get",
    params: { dirPath, productId },
  });
}

// calculateShipping
export function calculateShipping(data) {
  return request({
    url: "/vh-erp/media/calculateShipping",
    method: "post",
    data: data,
  });
}
