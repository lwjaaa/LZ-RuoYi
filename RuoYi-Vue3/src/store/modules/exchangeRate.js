import { defineStore } from "pinia";
import { getUsdRate } from "@/api/erp/exchange-rate";

// 定义汇率状态管理
export const useExchangeRateStore = defineStore("exchangeRate", {
  state: () => ({
    rate: null,
    rateDate: null,
    isLoading: false,
  }),

  actions: {
    // 获取汇率数据
    async fetchUsdRate() {
      this.isLoading = true;
      try {
        const response = await getUsdRate();
        this.rate = response.data.rate;
        this.rateDate = response.data.rateDate;
        // 缓存到本地存储
        this.cacheRate();
        return response.data;
      } catch (error) {
        console.error("获取汇率失败:", error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    // 从本地存储加载汇率
    loadCachedRate() {
      try {
        const cached = localStorage.getItem("exchangeRate");
        if (cached) {
          const data = JSON.parse(cached);
          this.rate = data.rate;
          this.rateDate = data.rateDate;
        }
      } catch (error) {
        console.error("加载缓存汇率失败:", error);
      }
    },

    // 缓存汇率到本地存储
    cacheRate() {
      try {
        localStorage.setItem(
          "exchangeRate",
          JSON.stringify({
            rate: this.rate,
            rateDate: this.rateDate,
          }),
        );
      } catch (error) {
        console.error("缓存汇率失败:", error);
      }
    },

    // 检查并更新汇率
    async checkAndUpdateRate() {
      console.log("检查汇率");
      const today = new Date().toISOString().split("T")[0];
      if (this.rateDate !== today) {
        console.log("汇率已过期，开始更新汇率");
        await this.fetchUsdRate();
      }
      return this.rate;
    },
  },
});
