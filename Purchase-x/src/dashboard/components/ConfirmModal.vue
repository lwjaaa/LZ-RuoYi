<template>
  <Teleport to="body">
    <Transition name="modal">
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" :aria-label="title">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="$emit('cancel')" />
        <div class="relative glass-card w-80 overflow-hidden animate-scale-in border-danger/20">
          <div class="p-4 border-b border-white/5">
            <div class="flex items-center gap-2">
              <div class="w-1 h-5 rounded-full bg-gradient-to-b from-danger to-warning" />
              <h3 class="text-sm font-semibold text-white">{{ title }}</h3>
            </div>
          </div>
          <div class="p-4">
            <p class="text-sm text-gray-300">{{ message }}</p>
          </div>
          <div class="p-4 flex gap-2 justify-end border-t border-white/5">
            <button
              class="px-4 py-2 bg-primary-dark/60 hover:bg-primary-light/40 border border-white/5 text-gray-300 text-sm rounded-lg transition-all duration-200 cursor-pointer"
              @click="$emit('cancel')"
            >
              取消
            </button>
            <button
              class="px-4 py-2 bg-gradient-to-r from-danger to-danger/80 hover:from-danger hover:to-danger text-white text-sm rounded-lg transition-all duration-200 hover:shadow-glow-danger cursor-pointer"
              @click="$emit('confirm')"
            >
              确认
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  message: string
}>()

defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
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
