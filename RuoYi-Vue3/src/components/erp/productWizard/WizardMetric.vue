<template>
  <div class="wizard-metric" :class="toneClass">
    <span class="wizard-metric__label">{{ label }}</span>
    <strong class="wizard-metric__value">{{ value }}</strong>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    label: string;
    value: string | number;
    tone?: "default" | "success" | "warning" | "danger" | "info";
  }>(),
  {
    tone: "default",
  },
);

const toneClass = computed(() => `is-${props.tone}`);
</script>

<style scoped>
.wizard-metric {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.wizard-metric__label {
  color: #64748b;
  font-size: 12px;
  line-height: 1.3;
}

.wizard-metric__value {
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 700;
}

.wizard-metric.is-success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.wizard-metric.is-warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.wizard-metric.is-danger {
  border-color: #fecaca;
  background: #fef2f2;
}

.wizard-metric.is-info {
  border-color: #bfdbfe;
  background: #eff6ff;
}
</style>
