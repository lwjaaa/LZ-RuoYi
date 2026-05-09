<template>
  <section class="wizard-workspace" :data-active-step="activeStep">
    <div v-if="showHeader" class="wizard-workspace__top">
      <div class="wizard-workspace__title-group">
        <span class="wizard-workspace__eyebrow">Step {{ stepNumber }} / 2</span>
        <h2 class="wizard-workspace__title">{{ title }}</h2>
        <p v-if="subtitle" class="wizard-workspace__subtitle">
          {{ subtitle }}
        </p>
      </div>
      <div v-if="$slots.actions" class="wizard-workspace__actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="wizard-workspace__body">
      <main class="wizard-workspace__main">
        <slot />
      </main>
      <aside v-if="$slots.aside" class="wizard-workspace__aside">
        <slot name="aside" />
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  activeStep: number;
  title: string;
  subtitle?: string;
  showHeader?: boolean;
}>();

const showHeader = computed(() => props.showHeader !== false);

const stepNumber = computed(() => props.activeStep + 1);
</script>

<style scoped>
.wizard-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wizard-workspace__top {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.wizard-workspace__title-group {
  min-width: 0;
}

.wizard-workspace__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border: 1px solid #d7e1ee;
  border-radius: 999px;
  color: #2563eb;
  background: #f8fbff;
  font-size: 12px;
  font-weight: 600;
}

.wizard-workspace__title {
  margin: 10px 0 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.3;
  font-weight: 700;
}

.wizard-workspace__subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.wizard-workspace__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.wizard-workspace__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 336px);
  align-items: start;
  gap: 18px;
}

.wizard-workspace__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 14px;
}

.wizard-workspace__aside {
  position: sticky;
  top: 84px;
  display: flex;
  min-width: 0;
  max-height: calc(100vh - 108px);
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  scrollbar-width: thin;
}

@media (max-width: 1180px) {
  .wizard-workspace__body {
    grid-template-columns: 1fr;
  }

  .wizard-workspace__aside {
    display: none;
  }
}

@media (max-width: 768px) {
  .wizard-workspace__top {
    align-items: stretch;
    flex-direction: column;
  }

  .wizard-workspace__actions {
    justify-content: flex-start;
  }

  .wizard-workspace__title {
    font-size: 20px;
  }
}
</style>
