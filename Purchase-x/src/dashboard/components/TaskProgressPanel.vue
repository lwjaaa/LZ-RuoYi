<template>
  <div class="glass-card p-4 animate-fade-in-up">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <div
          class="w-2 h-2 rounded-full"
          :class="
            taskQueue.length > 0
              ? 'bg-accent animate-pulse-glow'
              : 'bg-gray-600'
          "
        />
        <h2 class="text-sm font-semibold text-white">任务进度</h2>
      </div>
      <span
        v-if="taskQueue.length > 0"
        class="text-xs text-accent/80 font-mono"
      >
        {{ taskQueue.length }} 个任务进行中
      </span>
    </div>

    <div v-if="taskQueue.length === 0" class="flex items-center gap-3 py-2">
      <svg
        class="w-5 h-5 text-gray-600"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <circle cx="12" cy="12" r="10" />
        <path d="M8 14s1.5 2 4 2 4-2 4-2" />
        <line x1="9" y1="9" x2="9.01" y2="9" />
        <line x1="15" y1="9" x2="15.01" y2="9" />
      </svg>
      <p class="text-xs text-gray-500">暂无进行中的任务</p>
    </div>

    <div v-else class="space-y-4">
      <div
        class="relative h-2.5 bg-primary-deeper rounded-full overflow-hidden border border-white/5"
      >
        <div
          class="absolute inset-y-0 left-0 rounded-full bg-gradient-to-r from-accent via-purple to-cyan transition-all duration-500 ease-out"
          :style="{ width: `${totalProgress}%` }"
        />
        <div
          class="absolute inset-y-0 left-0 rounded-full bg-gradient-to-r from-accent via-purple to-cyan opacity-30 blur-sm transition-all duration-500 ease-out"
          :style="{ width: `${totalProgress}%` }"
        />
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div
          class="bg-primary-dark/60 rounded-xl p-3 border border-white/5 text-center"
        >
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-1">
            商品总数
          </p>
          <p class="text-lg font-bold text-white font-mono">
            {{ taskQueue.length }}
          </p>
        </div>
        <div
          class="bg-primary-dark/60 rounded-xl p-3 border border-white/5 text-center"
        >
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-1">
            已完成
          </p>
          <p class="text-lg font-bold text-success font-mono">
            {{ completedTasks }}
          </p>
        </div>
        <div
          class="bg-primary-dark/60 rounded-xl p-3 border border-white/5 text-center"
        >
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-1">
            变体进度
          </p>
          <p class="text-lg font-bold text-cyan font-mono">
            {{ totalCurrentVariants
            }}<span class="text-gray-600 text-sm">/{{ totalVariants }}</span>
          </p>
        </div>
        <div
          class="bg-primary-dark/60 rounded-xl p-3 border border-white/5 text-center"
        >
          <p class="text-[10px] text-gray-500 uppercase tracking-wider mb-1">
            预计剩余
          </p>
          <p class="text-lg font-bold text-warning font-mono">
            {{ estimatedTime }}
          </p>
        </div>
      </div>

      <div class="text-center">
        <span
          class="text-2xl font-bold bg-gradient-to-r from-accent to-cyan bg-clip-text text-transparent font-mono"
        >
          {{ totalProgress.toFixed(1) }}%
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type { FetchTask } from "@/types";

const props = defineProps<{
  taskQueue: FetchTask[];
}>();

const totalVariants = computed(() =>
  props.taskQueue.reduce((sum, t) => sum + t.totalVariants, 0),
);

const totalCurrentVariants = computed(() =>
  props.taskQueue.reduce((sum, t) => sum + t.currentVariants, 0),
);

const completedTasks = computed(
  () => props.taskQueue.filter((t) => t.status === "completed").length,
);

const totalProgress = computed(() => {
  if (totalVariants.value === 0) return 0;
  return (totalCurrentVariants.value / totalVariants.value) * 100;
});

const estimatedTime = computed(() => {
  if (totalVariants.value === 0 || totalCurrentVariants.value === 0)
    return "--";
  const remaining = totalVariants.value - totalCurrentVariants.value;
  const avgTimePerVariant = 0.5;
  const minutes = Math.ceil((remaining * avgTimePerVariant) / 60);
  if (minutes < 1) return "<1min";
  if (minutes < 60) return `${minutes}m`;
  return `${Math.floor(minutes / 60)}h${minutes % 60}m`;
});
</script>
