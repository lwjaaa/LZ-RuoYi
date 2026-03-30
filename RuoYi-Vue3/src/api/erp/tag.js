import request from "@/utils/request";

// 查询erp标签列表
export function listTag(query) {
  return request({
    url: "/vh-erp/tag/list",
    method: "get",
    params: query,
  });
}

// 查询erp标签分级列表
export function treeList(type) {
  return request({
    url: "/vh-erp/tag/treelist/" + type,
    method: "get",
  });
}

// 查询erp标签详细
export function getTag(tagId) {
  return request({
    url: "/vh-erp/tag/" + tagId,
    method: "get",
  });
}

// 新增erp标签
export function addTag(data) {
  return request({
    url: "/vh-erp/tag",
    method: "post",
    data: data,
  });
}

// 修改erp标签
export function updateTag(data) {
  return request({
    url: "/vh-erp/tag",
    method: "put",
    data: data,
  });
}

// 修改erp标签
export function dragNode(data) {
  return request({
    url: "/vh-erp/tag/dragNode",
    method: "post",
    data: data,
  });
}

// 删除erp标签
export function delTag(tagId) {
  return request({
    url: "/vh-erp/tag/" + tagId,
    method: "delete",
  });
}

// 导入erp标签
export function importTag(data) {
  return request({
    url: "/vh-erp/tag/importData",
    method: "post",
    data: data,
  });
}

// 下载erp标签导入模板
export function importTemplateTag() {
  return request({
    url: "/vh-erp/tag/importTemplate",
    method: "post",
    responseType: "blob",
  });
}
