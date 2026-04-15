import request from "@/utils/request";

// 获取指定货币对的最新汇率
export function getTodayRate(baseCurrency) {
  return request({
    url: "/erp/exchange-rate/today/" + baseCurrency,
    method: "get",
  });
}

// 获取美元汇率
export function getUsdRate() {
  return request({
    url: "/erp/exchange-rate/today/USD",
    method: "get",
  });
}
