<template>
  <Teleport to="body">
    <Transition name="modal">
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true" aria-label="设置">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="$emit('close')" />
        <div class="relative glass-card w-[420px] overflow-hidden animate-scale-in border-accent/20">
          <div class="p-4 border-b border-white/5">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <div class="w-1 h-5 rounded-full bg-gradient-to-b from-accent to-purple" />
                <h3 class="text-sm font-semibold text-white">设置</h3>
              </div>
              <button
                class="w-6 h-6 flex items-center justify-center rounded-lg hover:bg-white/10 text-gray-400 hover:text-white transition-all duration-200 cursor-pointer"
                @click="$emit('close')"
              >
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          </div>

          <div class="p-4 space-y-4">
            <div class="space-y-2">
              <label class="text-xs font-medium text-gray-300 flex items-center gap-1">
                <svg class="w-3.5 h-3.5 text-accent" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2" />
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2" />
                  <line x1="6" y1="6" x2="6.01" y2="6" />
                  <line x1="6" y1="18" x2="6.01" y2="18" />
                </svg>
                服务器域名
              </label>
              <div class="relative">
                <input
                  v-model="serverDomain"
                  type="text"
                  placeholder="例如: http://192.168.1.100:8080"
                  class="w-full px-3 py-2.5 bg-primary-dark/60 border border-white/10 rounded-lg text-sm text-white placeholder-gray-500 focus:outline-none focus:border-accent/50 focus:ring-1 focus:ring-accent/20 transition-all duration-200"
                />
                <div v-if="serverDomain" class="absolute right-2 top-1/2 -translate-y-1/2">
                  <div class="w-2 h-2 rounded-full bg-success animate-pulse" title="已配置" />
                </div>
              </div>
              <p class="text-xs text-gray-500">
                设置后端服务器地址，用于API请求
              </p>
            </div>

            <div v-if="showWarning" class="p-3 bg-warning/10 border border-warning/20 rounded-lg">
              <div class="flex items-start gap-2">
                <svg class="w-4 h-4 text-warning flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                  <line x1="12" y1="9" x2="12" y2="13" />
                  <line x1="12" y1="17" x2="12.01" y2="17" />
                </svg>
                <p class="text-xs text-warning">
                  未设置服务器域名，接口请求将无法正常工作
                </p>
              </div>
            </div>
          </div>

          <div class="p-4 flex gap-2 justify-end border-t border-white/5">
            <button
              class="px-4 py-2 bg-primary-dark/60 hover:bg-primary-light/40 border border-white/5 text-gray-300 text-sm rounded-lg transition-all duration-200 cursor-pointer"
              @click="$emit('close')"
            >
              取消
            </button>
            <button
              class="px-4 py-2 bg-gradient-to-r from-accent to-purple hover:from-accent hover:to-purple text-white text-sm rounded-lg transition-all duration-200 hover:shadow-glow cursor-pointer"
              @click="handleSave"
            >
              保存
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'save', domain: string): void
}>()

const serverDomain = ref('')
const showWarning = ref(false)

const STORAGE_KEY = 'purchasex_server_domain'

onMounted(() => {
  const savedDomain = localStorage.getItem(STORAGE_KEY)
  if (savedDomain) {
    serverDomain.value = savedDomain
  }
  showWarning.value = !serverDomain.value
})

function handleSave() {
  const domain = serverDomain.value.trim()
  
  if (domain) {
    localStorage.setItem(STORAGE_KEY, domain)
    showWarning.value = false
    emit('save', domain)
  } else {
    localStorage.removeItem(STORAGE_KEY)
    showWarning.value = true
    emit('save', '')
  }
}
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
