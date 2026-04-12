import request from "@/utils/request";

// 查询erp商品列表
export function listProduct(query) {
  return request({
    url: "/erp/product/list",
    method: "get",
    params: query,
  });
}

// 查询erp商品详细
export function getProduct(productId) {
  return request({
    url: "/erp/product/" + productId,
    method: "get",
  });
}

// 修改erp商品
export function updateProduct(data) {
  return request({
    url: "/erp/product",
    method: "put",
    data: data,
  });
}

// 删除erp商品
export function delProduct(productId) {
  return request({
    url: "/erp/product/" + productId,
    method: "delete",
  });
}

// 导入erp商品
export function importProduct(data) {
  return request({
    url: "/erp/product/importData",
    method: "post",
    data: data,
  });
}

// 下载erp商品导入模板
export function importTemplateProduct() {
  return request({
    url: "/erp/product/importTemplate",
    method: "post",
    responseType: "blob",
  });
}

// 批量推送商品
export function pushBatch(productIds) {
  return request({
    url: "/erp/product/pushBatch",
    method: "post",
    data: productIds,
  });
}
