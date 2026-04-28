<template>
  <div class="min-h-screen bg-primary-deeper text-white">
    <DashboardHeader
      v-model="viewMode"
      :is-syncing="isSyncing"
      @export="handleExport"
      @sync="handleSync"
      @refresh-options="handleRefreshOptions"
      @clear="handleClearProducts"
      @refresh="loadProducts"
      @settings="showSettings = true"
    />

    <main class="px-4 sm:px-6 lg:px-8 py-6 max-w-[1920px] mx-auto space-y-5">
      <TaskProgressPanel :task-queue="taskQueue" />

      <FilterBar
        :total-count="filteredProducts.length"
        @filter-change="handleFilterChange"
      />

      <div
        v-if="isLoadingData"
        class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4"
      >
        <div
          v-for="i in 4"
          :key="i"
          class="glass-card h-72 shimmer-loading rounded-2xl"
        />
      </div>

      <div
        v-else-if="filteredProducts.length === 0"
        class="glass-card p-12 text-center animate-fade-in"
      >
        <svg
          class="w-16 h-16 mx-auto text-gray-700 mb-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1"
        >
          <path
            d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"
          />
          <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
          <line x1="12" y1="22.08" x2="12" y2="12" />
        </svg>
        <p class="text-gray-400 text-sm">暂无商品数据</p>
        <p class="text-xs text-gray-600 mt-1">请通过 Popup 获取商品信息</p>
      </div>

      <div
        v-else
        :class="[
          viewMode === 'grid'
            ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'
            : 'flex flex-col gap-3',
        ]"
      >
        <ProductCard
          v-for="(product, pIdx) in paginatedProducts"
          :key="product.id"
          :product="product"
          :view-mode="viewMode"
          :index="pIdx"
          @delete-image="handleDeleteImage"
          @delete-option="handleDeleteOption"
          @update-tags="handleUpdateTags"
          @preview="handlePreviewImage"
          @delete-product="handleDeleteProduct"
        />
      </div>

      <div
        v-if="totalPages > 1"
        class="flex items-center justify-center gap-2 pt-4"
      >
        <button
          :disabled="currentPage === 1"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs transition-all duration-200 cursor-pointer',
            currentPage === 1
              ? 'bg-primary-dark/40 text-gray-600 cursor-not-allowed'
              : 'bg-primary-dark/60 text-gray-300 hover:bg-accent/20 hover:text-accent border border-white/5',
          ]"
          @click="currentPage--"
        >
          上一页
        </button>
        <div class="flex items-center gap-1">
          <button
            v-for="page in displayedPages"
            :key="page"
            :class="[
              'w-8 h-8 rounded-lg text-xs font-medium transition-all duration-200 cursor-pointer',
              page === currentPage
                ? 'bg-accent/20 text-accent shadow-glow'
                : 'bg-primary-dark/60 text-gray-400 hover:text-white hover:bg-white/5',
            ]"
            @click="currentPage = page"
          >
            {{ page }}
          </button>
        </div>
        <button
          :disabled="currentPage === totalPages"
          :class="[
            'px-3 py-1.5 rounded-lg text-xs transition-all duration-200 cursor-pointer',
            currentPage === totalPages
              ? 'bg-primary-dark/40 text-gray-600 cursor-not-allowed'
              : 'bg-primary-dark/60 text-gray-300 hover:bg-accent/20 hover:text-accent border border-white/5',
          ]"
          @click="currentPage++"
        >
          下一页
        </button>
      </div>
    </main>

    <ImagePreviewModal
      v-if="previewImage"
      :url="previewImage"
      @close="previewImage = ''"
    />

    <ConfirmModal
      v-if="confirmModal.show"
      :title="confirmModal.title"
      :message="confirmModal.message"
      @confirm="confirmModal.onConfirm"
      @cancel="confirmModal.onCancel"
    />

    <Toast ref="toastRef" />

    <SettingsModal
      v-if="showSettings"
      @close="showSettings = false"
      @save="handleSettingsSave"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, toRaw } from "vue";
import type { Product, FetchTask } from "@/types";
import {
  syncProductToErp,
  fetchTagTree,
  setApiBaseUrl,
  hasValidDomain,
} from "@/api";
import DashboardHeader from "./components/DashboardHeader.vue";
import TaskProgressPanel from "./components/TaskProgressPanel.vue";
import FilterBar from "./components/FilterBar.vue";
import type { FilterState } from "./components/FilterBar.vue";
import ProductCard from "./components/ProductCard.vue";
import ImagePreviewModal from "./components/ImagePreviewModal.vue";
import ConfirmModal from "./components/ConfirmModal.vue";
import Toast from "./components/Toast.vue";
import SettingsModal from "./components/SettingsModal.vue";

const products = ref<Product[]>([]);
const taskQueue = ref<FetchTask[]>([]);
const previewImage = ref("");
const toastRef = ref<InstanceType<typeof Toast> | null>(null);
const isSyncing = ref(false);
const isLoadingData = ref(true);
const viewMode = ref<"grid" | "list">("grid");
const currentPage = ref(1);
const pageSize = 12;
const showSettings = ref(false);
const filterState = ref<FilterState>({
  searchQuery: "",
  sortBy: "default",
  statusFilter: "all",
});

let refreshInterval: number | null = null;
let previousTaskQueue: FetchTask[] = [];

const confirmModal = ref<{
  show: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}>({
  show: false,
  title: "",
  message: "",
  onConfirm: () => {},
  onCancel: () => {},
});

const filteredProducts = computed(() => {
  let result = [...products.value];
  const { searchQuery, sortBy, statusFilter } = filterState.value;

  if (searchQuery) {
    const query = searchQuery.toLowerCase();
    result = result.filter((p) => p.productName.toLowerCase().includes(query));
  }

  if (statusFilter === "tagged") {
    result = result.filter((p) => p.tagIds.length > 0);
  } else if (statusFilter === "untagged") {
    result = result.filter((p) => p.tagIds.length === 0);
  } else if (statusFilter === "available") {
    result = result.filter((p) =>
      p.productVariantList.some((v) => v.isActiveAvailable === "1"),
    );
  } else if (statusFilter === "unavailable") {
    result = result.filter((p) =>
      p.productVariantList.every((v) => v.isActiveAvailable === "0"),
    );
  }

  switch (sortBy) {
    case "price-asc":
      result.sort((a, b) => {
        const minA = Math.min(
          ...a.productVariantList.map((v) => v.purchasePrice),
          Infinity,
        );
        const minB = Math.min(
          ...b.productVariantList.map((v) => v.purchasePrice),
          Infinity,
        );
        return minA - minB;
      });
      break;
    case "price-desc":
      result.sort((a, b) => {
        const maxA = Math.max(
          ...a.productVariantList.map((v) => v.purchasePrice),
          0,
        );
        const maxB = Math.max(
          ...b.productVariantList.map((v) => v.purchasePrice),
          0,
        );
        return maxB - maxA;
      });
      break;
    case "name-asc":
      result.sort((a, b) => a.productName.localeCompare(b.productName, "zh"));
      break;
    case "name-desc":
      result.sort((a, b) => b.productName.localeCompare(a.productName, "zh"));
      break;
    case "variants-asc":
      result.sort(
        (a, b) => a.productVariantList.length - b.productVariantList.length,
      );
      break;
    case "variants-desc":
      result.sort(
        (a, b) => b.productVariantList.length - a.productVariantList.length,
      );
      break;
  }

  return result;
});

const totalPages = computed(() =>
  Math.ceil(filteredProducts.value.length / pageSize),
);

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredProducts.value.slice(start, start + pageSize);
});

const displayedPages = computed(() => {
  const total = totalPages.value;
  const current = currentPage.value;
  const pages: number[] = [];
  const delta = 2;

  for (
    let i = Math.max(1, current - delta);
    i <= Math.min(total, current + delta);
    i++
  ) {
    pages.push(i);
  }

  if (pages[0] > 1) {
    pages.unshift(1);
    if (pages[1] > 2) pages.splice(1, 0, -1);
  }
  if (pages[pages.length - 1] < total) {
    if (pages[pages.length - 1] < total - 1) pages.push(-1);
    pages.push(total);
  }

  return pages;
});

watch(
  filterState,
  () => {
    currentPage.value = 1;
  },
  { deep: true },
);

function handleFilterChange(filter: FilterState): void {
  filterState.value = filter;
}

async function loadProducts(): Promise<void> {
  isLoadingData.value = true;
  try {
    const result = await chrome.storage.local.get("productList");

    console.log("[Dashboard] products after load:", result);
    console.log(
      "[Dashboard] products after load1111:",
      JSON.parse(JSON.stringify(result.productList)),
    );
    products.value = Array.isArray(result.productList)
      ? result.productList
      : [];
    console.log("[Dashboard] products after load:", products.value);
  } catch (error) {
    console.error("[Dashboard] Failed to load products:", error);
  } finally {
    isLoadingData.value = false;
  }
}

async function loadTaskQueue(): Promise<void> {
  try {
    const result = await chrome.storage.local.get("taskQueue");
    const newQueue: FetchTask[] = result.taskQueue || [];

    const hasChanges =
      JSON.stringify(newQueue) !== JSON.stringify(previousTaskQueue);
    if (hasChanges) {
      const previousIds = new Set(previousTaskQueue.map((t) => t.id));
      const newCompleted = newQueue.filter(
        (t) => t.status === "completed" && !previousIds.has(t.id),
      );

      if (newCompleted.length > 0) {
        await loadProducts();
      }

      previousTaskQueue = [...newQueue];
    }

    taskQueue.value = newQueue;
  } catch (error) {
    console.error("[Dashboard] Failed to load task queue:", error);
  }
}

function showConfirm(title: string, message: string): Promise<boolean> {
  return new Promise((resolve) => {
    confirmModal.value = {
      show: true,
      title,
      message,
      onConfirm: () => {
        confirmModal.value.show = false;
        resolve(true);
      },
      onCancel: () => {
        confirmModal.value.show = false;
        resolve(false);
      },
    };
  });
}

async function handleExport(): Promise<void> {
  if (products.value.length === 0) {
    toastRef.value?.warning("没有可导出的商品");
    return;
  }

  try {
    const dataStr = JSON.stringify(products.value, null, 2);
    const blob = new Blob([dataStr], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `purchasex-products-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);

    toastRef.value?.success(`成功导出 ${products.value.length} 个商品`);

    const shouldClear = await showConfirm("导出成功", "是否清空商品列表？");
    if (shouldClear) {
      await handleClearProducts();
    }
  } catch {
    toastRef.value?.error("导出失败，请重试");
  }
}

async function handleSync(): Promise<void> {
  if (products.value.length === 0) {
    toastRef.value?.warning("没有可同步的商品");
    return;
  }

  if (!hasValidDomain()) {
    toastRef.value?.warning("请先设置服务器域名");
    showSettings.value = true;
    return;
  }

  const untaggedProducts = products.value.filter((p) => p.tagIds.length === 0);
  if (untaggedProducts.length > 0) {
    const confirmed = await showConfirm(
      "确认提交",
      `有 ${untaggedProducts.length} 个商品未选择标签，是否继续提交？`,
    );
    if (!confirmed) {
      return;
    }
  }

  if (isSyncing.value) {
    toastRef.value?.info("正在同步中，请稍候");
    return;
  }

  isSyncing.value = true;
  let successCount = 0;
  let failCount = 0;

  try {
    for (const product of products.value) {
      try {
        await syncProductToErp({
          productName: product.productName,
          tagIds: product.tagIds,
          sourceUrl: product.sourceUrl,
          mediaUrlList: product.mediaUrlList,
          optionList: product.optionList,
          productVariantList: product.productVariantList,
        });
        successCount++;
      } catch {
        failCount++;
        console.error(
          `[Dashboard] Failed to sync product: ${product.productName}`,
        );
      }
    }

    if (successCount > 0) {
      toastRef.value?.success(`成功同步 ${successCount} 个商品`);
    }
    if (failCount > 0) {
      toastRef.value?.error(`${failCount} 个商品同步失败`);
    }

    if (successCount > 0 && failCount === 0) {
      const shouldClear = await showConfirm("同步成功", "是否清空商品列表？");
      if (shouldClear) {
        await handleClearProducts();
      }
    }
  } catch (error) {
    toastRef.value?.error("同步过程发生错误");
    console.error("[Dashboard] Sync error:", error);
  } finally {
    isSyncing.value = false;
  }
}

async function handleRefreshOptions(): Promise<void> {
  console.log("[Dashboard] Refresh options triggered");

  if (!hasValidDomain()) {
    toastRef.value?.warning("请先设置服务器域名");
    showSettings.value = true;
    return;
  }

  try {
    // 强制刷新标签树，从服务器获取最新数据
    await fetchTagTree(true);
    toastRef.value?.success("标签缓存已刷新");
  } catch (error) {
    console.error("[Dashboard] Failed to refresh tags:", error);
    toastRef.value?.error("刷新标签失败");
  }
}

function handleSettingsSave(domain: string): void {
  setApiBaseUrl(domain);
  showSettings.value = false;

  if (domain) {
    toastRef.value?.success("服务器域名已保存");
  } else {
    toastRef.value?.info("服务器域名已清除");
  }
}

async function handleClearProducts(): Promise<void> {
  const confirmed = await showConfirm(
    "确认清空",
    "确定要清空所有商品数据吗？此操作不可撤销。",
  );
  if (confirmed) {
    await chrome.storage.local.set({ productList: [] });
    products.value = [];
  }
}

async function handleDeleteImage(
  productId: string,
  imageIndex: number,
): Promise<void> {
  const confirmed = await showConfirm("确认删除", "确定要删除这张图片吗？");
  if (confirmed) {
    const product = products.value.find((p) => p.id === productId);
    if (product) {
      product.mediaUrlList.splice(imageIndex, 1);
      console.log("[Dashboard] Updated product:", products.value);
      await chrome.storage.local.set({ productList: toRaw(products.value) });
    }
  }
}

function handlePreviewImage(url: string): void {
  previewImage.value = url;
}

async function handleDeleteOption(
  productId: string,
  optionIndex: number,
): Promise<void> {
  const confirmed = await showConfirm(
    "确认删除",
    "删除规格选项将同步删除相关变体，确定要继续吗？",
  );
  if (confirmed) {
    const product = products.value.find((p) => p.id === productId);
    if (product) {
      const deletedOption = product.optionList[optionIndex];
      product.optionList.splice(optionIndex, 1);

      product.productVariantList = product.productVariantList.filter(
        (variant) => {
          return !variant.optionValueList.some(
            (ov) => ov.chineseName === deletedOption.chineseName,
          );
        },
      );

      console.log("[Dashboard] Updated product:", products.value);
      await chrome.storage.local.set({ productList: toRaw(products.value) });
    }
  }
}

async function handleUpdateTags(
  productId: string,
  tagIds: number[],
): Promise<void> {
  const product = products.value.find((p) => p.id === productId);
  if (product) {
    product.tagIds = tagIds;
    console.log("[Dashboard] Updated product:", products.value);
    await chrome.storage.local.set({ productList: toRaw(products.value) });
  }
}

async function handleDeleteProduct(productId: string): Promise<void> {
  const confirmed = await showConfirm(
    "确认删除",
    "确定要删除此商品吗？此操作不可撤销。",
  );
  if (confirmed) {
    const index = products.value.findIndex((p) => p.id === productId);
    if (index > -1) {
      products.value.splice(index, 1);
      await chrome.storage.local.set({ productList: toRaw(products.value) });
      toastRef.value?.success("商品已删除");
    }
  }
}

onMounted(() => {
  loadProducts();
  loadTaskQueue();

  refreshInterval = window.setInterval(() => {
    loadTaskQueue();
  }, 3000);

  chrome.storage.onChanged.addListener((changes) => {
    if (changes.productList) {
      products.value = Array.isArray(changes.productList.newValue)
        ? changes.productList.newValue
        : [];
    }
  });
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});
</script>
