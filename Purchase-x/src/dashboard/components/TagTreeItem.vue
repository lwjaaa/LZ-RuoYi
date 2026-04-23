<template>
  <div class="py-0.5">
    <div
      :class="[
        'flex items-center gap-2 px-2 py-1.5 rounded-lg cursor-pointer transition-all duration-200',
        isSelected
          ? 'bg-accent/15 text-accent border border-accent/20'
          : 'hover:bg-white/5 text-gray-400 border border-transparent'
      ]"
      :style="{ paddingLeft: `${level * 16 + 8}px` }"
      role="treeitem"
      :aria-selected="isSelected"
      tabindex="0"
      @click="$emit('toggle', tag.tagId)"
      @keydown.enter="$emit('toggle', tag.tagId)"
      @keydown.space.prevent="$emit('toggle', tag.tagId)"
    >
      <svg
        v-if="tag.children.length > 0"
        class="w-3 h-3 flex-shrink-0 transition-transform duration-200"
        :class="{ 'rotate-90': isExpanded }"
        viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
      >
        <polyline points="9 18 15 12 9 6" />
      </svg>
      <span v-else class="w-3 flex-shrink-0" />
      <span class="text-xs flex-1">{{ tag.tagName }}</span>
      <span v-if="tag.tagType === 'MENU'" class="text-[10px] text-purple/60 font-mono">
        {{ tag.spuPrefix }}
      </span>
      <svg v-if="isSelected" class="w-3.5 h-3.5 text-accent flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    </div>
    <div v-if="isExpanded && tag.children.length > 0" role="group">
      <TagTreeItem
        v-for="child in tag.children"
        :key="child.tagId"
        :tag="child"
        :selected-ids="selectedIds"
        :level="level + 1"
        @toggle="$emit('toggle', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { TagNode } from '@/types'

const props = withDefaults(defineProps<{
  tag: TagNode
  selectedIds: number[]
  level?: number
}>(), {
  level: 0
})

defineEmits<{
  (e: 'toggle', tagId: number): void
}>()

const isExpanded = ref(true)

const isSelected = computed(() => props.selectedIds.includes(props.tag.tagId))
</script>
