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
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #ffffff;
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
  border-color: #dbe3ef;
  background: #fbfefd;
}

.wizard-metric.is-success .wizard-metric__value {
  color: #047857;
}

.wizard-metric.is-warning {
  border-color: #dbe3ef;
  background: #fffdf7;
}

.wizard-metric.is-warning .wizard-metric__value {
  color: #b45309;
}

.wizard-metric.is-danger {
  border-color: #dbe3ef;
  background: #fffafa;
}

.wizard-metric.is-danger .wizard-metric__value {
  color: #b91c1c;
}

.wizard-metric.is-info {
  border-color: #dbe3ef;
  background: #fbfdff;
}

.wizard-metric.is-info .wizard-metric__value {
  color: #1d4ed8;
}
</style>
