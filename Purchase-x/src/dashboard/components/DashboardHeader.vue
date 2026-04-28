<template>
  <header
    class="sticky top-0 z-50 bg-glass-heavy backdrop-blur-glass-heavy border-b border-white/10"
  >
    <div class="neon-line" />
    <div
      class="px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between gap-4"
    >
      <div class="flex items-center gap-3 flex-shrink-0">
        <div
          class="w-8 h-8 rounded-lg bg-gradient-to-br from-accent to-purple flex items-center justify-center shadow-glow"
        >
          <svg
            class="w-5 h-5 text-white"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
        </div>
        <h1
          class="text-lg font-bold bg-gradient-to-r from-accent via-purple-light to-cyan bg-clip-text text-transparent whitespace-nowrap"
        >
          PurchaseX
        </h1>
      </div>

      <div
        class="hidden md:flex items-center gap-1 bg-primary-dark/60 rounded-xl p-1 border border-white/5"
      >
        <button
          :class="[
            'px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-200',
            modelValue === 'grid'
              ? 'bg-accent/20 text-accent shadow-glow'
              : 'text-gray-400 hover:text-white hover:bg-white/5',
          ]"
          aria-label="网格视图"
          @click="$emit('update:modelValue', 'grid')"
        >
          <svg
            class="w-4 h-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="3" y="3" width="7" height="7" />
            <rect x="14" y="3" width="7" height="7" />
            <rect x="3" y="14" width="7" height="7" />
            <rect x="14" y="14" width="7" height="7" />
          </svg>
        </button>
        <button
          :class="[
            'px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-200',
            modelValue === 'list'
              ? 'bg-accent/20 text-accent shadow-glow'
              : 'text-gray-400 hover:text-white hover:bg-white/5',
          ]"
          aria-label="列表视图"
          @click="$emit('update:modelValue', 'list')"
        >
          <svg
            class="w-4 h-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line x1="8" y1="6" x2="21" y2="6" />
            <line x1="8" y1="12" x2="21" y2="12" />
            <line x1="8" y1="18" x2="21" y2="18" />
            <line x1="3" y1="6" x2="3.01" y2="6" />
            <line x1="3" y1="12" x2="3.01" y2="12" />
            <line x1="3" y1="18" x2="3.01" y2="18" />
          </svg>
        </button>
      </div>

      <div class="flex items-center gap-2 overflow-x-auto">
        <button
          class="px-3 py-2 bg-accent/10 hover:bg-accent/20 border border-accent/20 hover:border-accent/40 rounded-lg text-accent text-xs font-medium transition-all duration-200 hover:shadow-glow whitespace-nowrap cursor-pointer"
          @click="$emit('export')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出
        </button>
        <button
          :disabled="isSyncing"
          :class="[
            'px-3 py-2 border rounded-lg text-xs font-medium transition-all duration-200 whitespace-nowrap cursor-pointer',
            isSyncing
              ? 'bg-success/5 border-success/10 text-success/40'
              : 'bg-success/10 hover:bg-success/20 border-success/20 hover:border-success/40 text-success hover:shadow-glow-success',
          ]"
          @click="$emit('sync')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            :class="{ 'animate-spin-slow': isSyncing }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
          {{ isSyncing ? "同步中..." : "同步" }}
        </button>
        <button
          class="px-3 py-2 bg-purple/10 hover:bg-purple/20 border border-purple/20 hover:border-purple/40 rounded-lg text-purple-light text-xs font-medium transition-all duration-200 hover:shadow-glow-purple whitespace-nowrap cursor-pointer"
          @click="$emit('refresh-options')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M1 4v6h6" />
            <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
          </svg>
          刷新标签
        </button>
        <button
          class="px-3 py-2 bg-danger/10 hover:bg-danger/20 border border-danger/20 hover:border-danger/40 rounded-lg text-danger text-xs font-medium transition-all duration-200 hover:shadow-glow-danger whitespace-nowrap cursor-pointer"
          @click="$emit('clear')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="3 6 5 6 21 6" />
            <path
              d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
            />
          </svg>
          清空
        </button>
        <button
          class="px-3 py-2 bg-primary-light/40 hover:bg-primary-light/60 border border-white/5 hover:border-white/10 rounded-lg text-gray-300 text-xs font-medium transition-all duration-200 whitespace-nowrap cursor-pointer"
          @click="$emit('refresh')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M23 4v6h-6" />
            <path d="M1 20v-6h6" />
            <path
              d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"
            />
          </svg>
          刷新
        </button>
        <button
          class="px-3 py-2 bg-cyan/10 hover:bg-cyan/20 border border-cyan/20 hover:border-cyan/40 rounded-lg text-cyan text-xs font-medium transition-all duration-200 hover:shadow-glow-cyan whitespace-nowrap cursor-pointer"
          @click="$emit('settings')"
        >
          <svg
            class="w-3.5 h-3.5 inline-block mr-1 -mt-0.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="3" />
            <path
              d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"
            />
          </svg>
          设置
        </button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  modelValue: "grid" | "list";
  isSyncing: boolean;
}>();

defineEmits<{
  (e: "update:modelValue", value: "grid" | "list"): void;
  (e: "export"): void;
  (e: "sync"): void;
  (e: "refresh-options"): void;
  (e: "clear"): void;
  (e: "refresh"): void;
  (e: "settings"): void;
}>();
</script>
