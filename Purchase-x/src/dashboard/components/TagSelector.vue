<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        aria-label="选择标签"
      >
        <div
          class="absolute inset-0 bg-black/60 backdrop-blur-sm"
          @click="$emit('close')"
        />
        <div
          class="relative glass-card w-full max-w-lg max-h-[85vh] flex flex-col animate-scale-in border-accent/20"
        >
          <div
            class="p-4 border-b border-white/5 flex items-center justify-between flex-shrink-0"
          >
            <div class="flex items-center gap-2">
              <div
                class="w-1 h-5 rounded-full bg-gradient-to-b from-accent to-purple"
              />
              <h3 class="text-sm font-semibold text-white">选择标签</h3>
            </div>
            <div class="flex items-center gap-3">
              <button
                class="text-xs text-accent/70 hover:text-accent transition-colors cursor-pointer"
                @click="$emit('refresh-tags')"
              >
                刷新标签
              </button>
              <button
                class="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-white/5 text-gray-400 hover:text-white transition-all cursor-pointer"
                aria-label="关闭"
                @click="$emit('close')"
              >
                <svg
                  class="w-4 h-4"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          </div>

          <div class="p-4 flex-1 overflow-y-auto">
            <div v-if="isLoading" class="flex items-center justify-center py-8">
              <div
                class="w-6 h-6 border-2 border-accent/30 border-t-accent rounded-full animate-spin"
              />
            </div>
            <div v-else-if="tagTree.length === 0" class="text-center py-8">
              <p class="text-xs text-gray-500">暂无标签数据</p>
            </div>
            <div v-else class="space-y-0.5">
              <TagTreeItem
                v-for="tag in tagTree"
                :key="tag.tagId"
                :tag="tag"
                :selected-ids="selectedIds"
                :level="0"
                @toggle="$emit('toggle', $event)"
              />
            </div>
          </div>

          <div
            class="p-4 border-t border-white/5 flex items-center justify-between flex-shrink-0"
          >
            <span class="text-xs text-gray-500">
              已选
              <span class="text-accent font-mono">{{
                selectedIds.length
              }}</span>
              个标签
            </span>
            <button
              class="px-4 py-2 bg-gradient-to-r from-accent to-purple hover:from-accent-dark hover:to-purple-dark text-white text-xs font-medium rounded-lg transition-all duration-200 hover:shadow-glow cursor-pointer"
              @click="$emit('confirm')"
            >
              确认选择
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { TagNode } from "@/types";
import TagTreeItem from "./TagTreeItem.vue";

defineProps<{
  visible: boolean;
  tagTree: TagNode[];
  selectedIds: number[];
  isLoading: boolean;
}>();

defineEmits<{
  (e: "close"): void;
  (e: "toggle", tagId: number): void;
  (e: "confirm"): void;
  (e: "refresh-tags"): void;
}>();
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
