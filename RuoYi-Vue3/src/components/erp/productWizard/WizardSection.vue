<template>
  <section class="wizard-section" :class="{ 'is-compact': compact }">
    <div class="wizard-section__header">
      <div class="wizard-section__title-group">
        <h3 class="wizard-section__title">{{ title }}</h3>
        <p v-if="description" class="wizard-section__description">
          {{ description }}
        </p>
      </div>
      <div v-if="$slots.actions" class="wizard-section__actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="wizard-section__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
  compact?: boolean;
}>();
</script>

<style scoped>
.wizard-section {
  padding: 18px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #ffffff;
}

.wizard-section.is-compact {
  padding: 14px;
}

.wizard-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.wizard-section__title-group {
  min-width: 0;
}

.wizard-section__title {
  margin: 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.4;
  font-weight: 700;
}

.wizard-section__description {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.wizard-section__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.wizard-section__body {
  min-width: 0;
}

@media (max-width: 768px) {
  .wizard-section {
    padding: 14px;
  }

  .wizard-section__header {
    flex-direction: column;
    gap: 10px;
  }

  .wizard-section__actions {
    justify-content: flex-start;
  }
}
</style>
