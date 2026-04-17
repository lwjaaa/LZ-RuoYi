import { defineStore } from "pinia";
import { getUsdRate } from "@/api/erp/exchange-rate";
import type { ExchangeRate } from "@/types/erp";

interface ExchangeRateState {
  rate: number | null;
  rateDate: string | null;
  isLoading: boolean;
}

interface CachedRateData {
  rate: number | null;
  rateDate: string | null;
}

export const useExchangeRateStore = defineStore("exchangeRate", {
  state: (): ExchangeRateState => ({
    rate: null,
    rateDate: null,
    isLoading: false,
  }),

  getters: {
    isRateUpToDate: (state): boolean => {
      if (!state.rateDate) return false;
      const today = new Date().toISOString().split("T")[0];
      return state.rateDate === today;
    },
  },

  actions: {
    async fetchUsdRate(): Promise<ExchangeRate> {
      this.isLoading = true;
      try {
        const response = await getUsdRate();
        this.rate = response.data.rate;
        this.rateDate = response.data.rateDate;
        this.cacheRate();
        return response.data;
      } catch (error) {
        console.error("获取汇率失败:", error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    loadCachedRate(): void {
      try {
        const cached = localStorage.getItem("exchangeRate");
        if (cached) {
          const data: CachedRateData = JSON.parse(cached);
          this.rate = data.rate;
          this.rateDate = data.rateDate;
        }
      } catch (error) {
        console.error("加载缓存汇率失败:", error);
      }
    },

    cacheRate(): void {
      try {
        const data: CachedRateData = {
          rate: this.rate,
          rateDate: this.rateDate,
        };
        localStorage.setItem("exchangeRate", JSON.stringify(data));
      } catch (error) {
        console.error("缓存汇率失败:", error);
      }
    },

    async checkAndUpdateRate(): Promise<number | null> {
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
