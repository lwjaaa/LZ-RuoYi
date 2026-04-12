import { defineStore } from "pinia";
import { getUsdRate } from "@/api/erp/productWizard";

// 定义汇率状态管理
export const useExchangeRateStore = defineStore("exchangeRate", {
  state: () => ({
    usdRate: null,
    usdRateTime: null,
    isLoading: false,
  }),

  getters: {
    // 检查汇率是否为当天
    isRateUpToDate: (state) => {
      if (!state.usdRateTime) return false;
      const today = new Date().toISOString().split("T")[0];
      return state.usdRateTime === today;
    },
  },

  actions: {
    // 获取汇率数据
    async fetchUsdRate() {
      this.isLoading = true;
      try {
        const response = await getUsdRate();
        this.usdRate = response.data.usdRate;
        this.usdRateTime = response.data.usdRateTime;
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
          this.usdRate = data.usdRate;
          this.usdRateTime = data.usdRateTime;
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
            usdRate: this.usdRate,
            usdRateTime: this.usdRateTime,
          }),
        );
      } catch (error) {
        console.error("缓存汇率失败:", error);
      }
    },

    // 检查并更新汇率
    async checkAndUpdateRate() {
      if (!this.isRateUpToDate) {
        await this.fetchUsdRate();
      }
      return this.usdRate;
    },
  },
});
