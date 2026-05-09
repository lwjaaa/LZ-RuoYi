<template>
  <section class="wizard-aside-panel" :class="toneClass">
    <div class="wizard-aside-panel__header">
      <h3 class="wizard-aside-panel__title">{{ title }}</h3>
      <slot name="actions" />
    </div>
    <div class="wizard-aside-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    title: string;
    tone?: "default" | "success" | "warning" | "danger" | "info";
  }>(),
  {
    tone: "default",
  },
);

const toneClass = computed(() => `is-${props.tone}`);
</script>

<style scoped>
.wizard-aside-panel {
  border-radius: 12px;
  background: #ffffff;
  overflow: hidden;
}

.wizard-aside-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 44px;
  padding: 12px 14px;
  background: #ffffff;
}

.wizard-aside-panel__title {
  margin: 0;
  color: #111827;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 700;
}

.wizard-aside-panel__body {
  padding: 14px;
}

.wizard-aside-panel.is-success .wizard-aside-panel__title {
  color: #047857;
}

.wizard-aside-panel.is-warning .wizard-aside-panel__title {
  color: #b45309;
}

.wizard-aside-panel.is-danger .wizard-aside-panel__title {
  color: #b91c1c;
}

.wizard-aside-panel.is-info .wizard-aside-panel__title {
  color: #1d4ed8;
}
</style>
