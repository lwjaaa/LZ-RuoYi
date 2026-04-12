import request from "@/utils/request";

// 编辑/新增 选品信息
export function addSelectionInfo(data) {
  return request({
    url: "/erp/product/wizard/selectionInfo",
    method: "post",
    data: data,
  });
}
// 编辑 商品其他信息
export function updateBaseInfo(data) {
  return request({
    url: "/erp/product/wizard/baseInfo",
    method: "post",
    data: data,
  });
}

//获取商品运费
export function calculateShipping(data) {
  return request({
    url: "/erp/product/wizard/calculateShipping",
    method: "post",
    data: data,
  });
}
// 获取美元汇率
export function getUsdRate() {
  return request({
    url: "/erp/product/wizard/getUsdRate",
    method: "get",
  });
}
