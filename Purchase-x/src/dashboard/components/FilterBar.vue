<template>
  <div class="glass-card p-3 animate-fade-in">
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative flex-1 min-w-[200px] max-w-md">
        <svg
          class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索商品名称..."
          class="w-full pl-9 pr-4 py-2 bg-primary-dark/60 border border-white/5 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:border-accent/40 focus:shadow-glow transition-all duration-200"
          aria-label="搜索商品"
          @input="emitFilterChange"
        />
      </div>

      <div class="flex items-center gap-2">
        <label for="sort-select" class="text-xs text-gray-500">排序</label>
        <select
          id="sort-select"
          v-model="sortBy"
          class="bg-primary-dark/60 border border-white/5 rounded-lg px-3 py-2 text-xs text-gray-300 focus:outline-none focus:border-accent/40 transition-all duration-200 cursor-pointer"
          @change="emitFilterChange"
        >
          <option value="default">默认</option>
          <option value="price-asc">价格升序</option>
          <option value="price-desc">价格降序</option>
          <option value="name-asc">名称 A-Z</option>
          <option value="name-desc">名称 Z-A</option>
          <option value="variants-asc">变体数升序</option>
          <option value="variants-desc">变体数降序</option>
        </select>
      </div>

      <div class="flex items-center gap-2">
        <label for="status-filter" class="text-xs text-gray-500">状态</label>
        <select
          id="status-filter"
          v-model="statusFilter"
          class="bg-primary-dark/60 border border-white/5 rounded-lg px-3 py-2 text-xs text-gray-300 focus:outline-none focus:border-accent/40 transition-all duration-200 cursor-pointer"
          @change="emitFilterChange"
        >
          <option value="all">全部</option>
          <option value="tagged">已选标签</option>
          <option value="untagged">未选标签</option>
          <option value="available">有可购变体</option>
          <option value="unavailable">全部缺货</option>
        </select>
      </div>

      <div class="flex items-center gap-2 text-xs text-gray-500">
        <span
          >共
          <span class="text-accent font-mono">{{ totalCount }}</span>
          件商品</span
        >
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const props = defineProps<{
  totalCount: number;
}>();

const emit = defineEmits<{
  (e: "filter-change", filter: FilterState): void;
}>();

export interface FilterState {
  searchQuery: string;
  sortBy: string;
  statusFilter: string;
}

const searchQuery = ref("");
const sortBy = ref("default");
const statusFilter = ref("all");

function emitFilterChange(): void {
  emit("filter-change", {
    searchQuery: searchQuery.value,
    sortBy: sortBy.value,
    statusFilter: statusFilter.value,
  });
}
</script>
