<template>
  <div class="w-80 min-h-96 bg-primary-dark text-white">
    <header class="p-4 border-b border-accent/20">
      <h1
        class="text-xl font-bold bg-gradient-to-r from-accent to-accent-light bg-clip-text text-transparent cursor-pointer hover:opacity-80 transition-opacity"
        @click="handleOpenDashboard"
      >
        PurchaseX
      </h1>
      <p class="text-xs text-gray-400 mt-1">跨境电商选品助手</p>
    </header>

    <div class="p-4 space-y-3">
      <div class="flex items-center gap-2 text-xs">
        <span
          :class="[
            'w-2 h-2 rounded-full',
            isSupportedPage ? 'bg-success' : 'bg-gray-500',
          ]"
        />
        <span :class="isSupportedPage ? 'text-gray-300' : 'text-gray-500'">
          {{ pageStatusText }}
        </span>
      </div>

      <button
        :disabled="!isSupportedPage"
        :class="[
          'w-full py-2.5 px-4 rounded-lg font-medium transition-all',
          isSupportedPage
            ? 'bg-accent hover:bg-accent-dark text-white'
            : 'bg-primary-light text-gray-500 cursor-not-allowed',
        ]"
        @click="handleFetchSingle"
      >
        获取当前商品
      </button>

      <button
        :disabled="!isSupportedPage"
        :class="[
          'w-full py-2.5 px-4 rounded-lg font-medium transition-all border',
          isSupportedPage
            ? 'bg-primary-light hover:bg-primary border-accent/30 text-white'
            : 'bg-primary-light text-gray-500 cursor-not-allowed border-gray-600',
        ]"
        @click="handleFetchBatch"
      >
        批量获取 ({{ batchCount }})
      </button>

      <button
        :disabled="taskQueue.length === 0"
        :class="[
          'w-full py-2.5 px-4 rounded-lg font-medium transition-all border',
          taskQueue.length > 0
            ? 'bg-primary-light hover:bg-danger/20 border-danger/30 text-danger'
            : 'bg-primary-light text-gray-500 cursor-not-allowed border-gray-600',
        ]"
        @click="handleCancelTasks"
      >
        取消任务
      </button>
    </div>

    <div class="px-4 pb-4">
      <div class="p-3 bg-primary rounded-lg border border-accent/20">
        <div class="flex items-center justify-between mb-2">
          <p class="text-sm text-gray-300">任务进度</p>
          <span v-if="taskQueue.length > 0" class="text-xs text-accent">
            {{ taskQueue.length }} 个任务
          </span>
        </div>

        <div v-if="taskQueue.length === 0" class="text-xs text-gray-400">
          暂无进行中的任务
        </div>

        <div v-else class="space-y-2">
          <div class="h-2 bg-primary-dark rounded-full overflow-hidden">
            <div
              class="h-full bg-gradient-to-r from-accent to-accent-light transition-all duration-300"
              :style="{ width: `${totalProgress}%` }"
            />
          </div>

          <div class="grid grid-cols-2 gap-2 text-xs">
            <div class="flex justify-between">
              <span class="text-gray-400">商品:</span>
              <span class="text-white"
                >{{ completedTasks }}/{{ taskQueue.length }}</span
              >
            </div>
            <div class="flex justify-between">
              <span class="text-gray-400">变体:</span>
              <span class="text-white"
                >{{ totalCurrentVariants }}/{{ totalVariants }}</span
              >
            </div>
          </div>

          <div class="text-xs text-center text-accent font-medium">
            {{ totalProgress.toFixed(1) }}%
          </div>
        </div>
      </div>
    </div>

    <div v-if="errorMessage" class="px-4 pb-4">
      <div class="p-3 bg-danger/10 border border-danger/30 rounded-lg">
        <p class="text-xs text-danger">{{ errorMessage }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import type { FetchTask, Platform } from "@/types";

const SUPPORTED_PLATFORMS: Record<Platform, RegExp> = {
  taobao: /taobao\.com\/item\.htm/i,
  tmall: /tmall\.com\/item\.htm|detail\.tmall\.com\/item\.htm/i,
  "1688": /1688\.com\/offer/i,
};

function isSupportedUrl(url: string): boolean {
  return Object.values(SUPPORTED_PLATFORMS).some((regex) => regex.test(url));
}

const taskQueue = ref<FetchTask[]>([]);
const currentTab = ref<chrome.tabs.Tab | null>(null);
const errorMessage = ref("");
const batchCount = ref(0);
const batchFetching = ref(false);
let refreshInterval: number | null = null;

const isSupportedPage = computed(() => {
  if (!currentTab.value?.url) return false;
  const url = currentTab.value.url;
  return Object.values(SUPPORTED_PLATFORMS).some((regex) => regex.test(url));
});

const pageStatusText = computed(() => {
  if (!currentTab.value?.url) return "无法获取当前页面";
  if (isSupportedPage.value) {
    const url = currentTab.value.url;
    if (SUPPORTED_PLATFORMS.taobao.test(url)) return "淘宝商品详情页";
    if (SUPPORTED_PLATFORMS.tmall.test(url)) return "天猫商品详情页";
    if (SUPPORTED_PLATFORMS["1688"].test(url)) return "1688商品详情页";
  }
  return "不支持的平台";
});

const totalVariants = computed(() =>
  taskQueue.value.reduce((sum, t) => sum + t.totalVariants, 0),
);

const totalCurrentVariants = computed(() =>
  taskQueue.value.reduce((sum, t) => sum + t.currentVariants, 0),
);

const completedTasks = computed(
  () => taskQueue.value.filter((t) => t.status === "completed").length,
);

const totalProgress = computed(() => {
  if (totalVariants.value === 0) return 0;
  return (totalCurrentVariants.value / totalVariants.value) * 100;
});

async function loadTaskQueue(): Promise<void> {
  try {
    const result = await chrome.storage.local.get("taskQueue");
    taskQueue.value = result.taskQueue || [];
  } catch (error) {
    console.error("[Popup] Failed to load task queue:", error);
  }
}

async function getCurrentTab(): Promise<void> {
  try {
    const [tab] = await chrome.tabs.query({
      active: true,
      currentWindow: true,
    });
    currentTab.value = tab || null;
  } catch (error) {
    console.error("[Popup] Failed to get current tab:", error);
  }
}

function showError(message: string): void {
  errorMessage.value = message;
  setTimeout(() => {
    errorMessage.value = "";
  }, 3000);
}

async function checkUniqueness(title: string, url: string): Promise<boolean> {
  try {
    const result = await chrome.storage.local.get(["productList", "taskQueue"]);
    const products = result.productList || [];
    const tasks = result.taskQueue || [];

    const productExists = products.some(
      (p: { productName: string; sourceUrl: string }) =>
        p.productName === title || p.sourceUrl === url,
    );
    const taskExists = tasks.some(
      (t: FetchTask) => t.title === title || t.url === url,
    );

    return !productExists && !taskExists;
  } catch (error) {
    console.error("[Popup] Failed to check uniqueness:", error);
    return false;
  }
}

async function handleFetchSingle(): Promise<void> {
  if (!currentTab.value?.id || !currentTab.value?.url) return;

  const title = currentTab.value.title || "";
  const url = currentTab.value.url;
  const tabId = currentTab.value.id;

  if (!tabId) return;

  const isUnique = await checkUniqueness(title, url);
  if (!isUnique) {
    showError("该商品已存在或正在获取中");
    return;
  }

  try {
    const response = await chrome.runtime.sendMessage({
      type: "START_FETCH",
      payload: { title, url, tabId },
    });

    if (response.status === "error") {
      showError("response:" + response.message || "获取失败");
    }
  } catch (error) {
    showError("发送请求失败");
  }
}

async function loadBatchCount(): Promise<void> {
  try {
    const tabs = await chrome.tabs.query({ currentWindow: true });
    const supportedTabs = tabs.filter(
      (tab) => tab.url && isSupportedUrl(tab.url),
    );
    batchCount.value = supportedTabs.length;
  } catch (error) {
    console.error("[Popup] Failed to load batch count:", error);
    batchCount.value = 0;
  }
}

async function handleFetchBatch(): Promise<void> {
  if (batchFetching.value) return;
  batchFetching.value = true;

  try {
    const tabs = await chrome.tabs.query({ currentWindow: true });
    const supportedTabs = tabs.filter(
      (tab) => tab.url && isSupportedUrl(tab.url) && tab.id,
    );

    if (supportedTabs.length === 0) {
      showError("当前窗口没有支持的商品页面");
      return;
    }

    const result = await chrome.storage.local.get([
      "productList",
      "taskQueue",
    ]);
    const products = result.productList || [];
    const existingTasks = result.taskQueue || [];

    const existingUrls = new Set<string>();
    const existingTitles = new Set<string>();
    for (const p of products) {
      existingUrls.add(p.sourceUrl);
      existingTitles.add(p.productName);
    }
    for (const t of existingTasks) {
      existingUrls.add(t.url);
      existingTitles.add(t.title);
    }

    const tasksToCreate = supportedTabs.filter(
      (tab) =>
        tab.url &&
        tab.id &&
        !existingUrls.has(tab.url) &&
        !existingTitles.has(tab.title || ""),
    );

    if (tasksToCreate.length === 0) {
      showError("所有商品已存在或正在获取中");
      return;
    }

    let successCount = 0;
    let failCount = 0;
    let skipCount = supportedTabs.length - tasksToCreate.length;

    for (const tab of tasksToCreate) {
      const title = tab.title || "";
      const url = tab.url!;
      const tabId = tab.id!;

      try {
        const response = await chrome.runtime.sendMessage({
          type: "START_FETCH",
          payload: { title, url, tabId },
        });
        if (response.status === "ok") {
          successCount++;
        } else {
          failCount++;
        }
      } catch (error) {
        failCount++;
        console.error(`[Popup] Failed to start fetch for tab ${tabId}:`, error);
      }
    }

    const parts: string[] = [];
    if (successCount > 0) parts.push(`成功: ${successCount}`);
    if (failCount > 0) parts.push(`失败: ${failCount}`);
    if (skipCount > 0) parts.push(`已存在: ${skipCount}`);
    showError(`批量获取完成 - ${parts.join(", ")}`);
  } catch (error) {
    showError("批量获取失败");
    console.error("[Popup] Batch fetch error:", error);
  } finally {
    batchFetching.value = false;
  }
}

async function handleCancelTasks(): Promise<void> {
  try {
    await chrome.runtime.sendMessage({ type: "CANCEL_ALL_TASKS" });
    taskQueue.value = [];
  } catch (error) {
    showError("取消任务失败");
  }
}

function handleOpenDashboard(): void {
  chrome.tabs.create({
    url: chrome.runtime.getURL("src/dashboard/index.html"),
  });
}

onMounted(() => {
  getCurrentTab();
  loadTaskQueue();
  loadBatchCount();

  refreshInterval = window.setInterval(() => {
    loadTaskQueue();
  }, 1000);

  chrome.storage.onChanged.addListener((changes) => {
    if (changes.taskQueue) {
      taskQueue.value = changes.taskQueue.newValue || [];
    }
  });

  chrome.tabs.onUpdated.addListener(() => {
    loadBatchCount();
  });
  chrome.tabs.onCreated.addListener(() => {
    loadBatchCount();
  });
  chrome.tabs.onRemoved.addListener(() => {
    loadBatchCount();
  });
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});
</script>
