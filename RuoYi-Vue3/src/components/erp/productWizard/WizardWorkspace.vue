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
      <main ref="mainRef" class="wizard-workspace__main">
        <slot />
      </main>
      <aside
        v-if="$slots.aside"
        ref="asideRef"
        class="wizard-workspace__aside"
        :style="asideStyle"
      >
        <slot name="aside" />
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";

const props = defineProps<{
  activeStep: number;
  title: string;
  subtitle?: string;
  showHeader?: boolean;
}>();

const showHeader = computed(() => props.showHeader !== false);

const stepNumber = computed(() => props.activeStep + 1);

const mainRef = ref<HTMLElement | null>(null);
const asideRef = ref<HTMLElement | null>(null);
const mainHeight = ref<number | null>(null);
const asideViewportHeight = ref<number | null>(null);
let resizeObserver: ResizeObserver | null = null;
let measureFrame: number | null = null;

const asideStyle = computed(() => {
  if (!mainHeight.value && !asideViewportHeight.value) {
    return undefined;
  }

  const style: Record<string, string> = {};

  if (mainHeight.value) {
    style["--wizard-main-height"] = `${mainHeight.value}px`;
  }

  if (asideViewportHeight.value) {
    style["--wizard-aside-viewport-height"] =
      `${asideViewportHeight.value}px`;
  }

  return style;
});

function updateMainHeight(): void {
  mainHeight.value = mainRef.value?.scrollHeight ?? null;
}

function updateAsideViewportHeight(): void {
  if (typeof window === "undefined") {
    return;
  }

  const top = asideRef.value?.getBoundingClientRect().top ?? 84;
  const stickyTop = Number(
    window
      .getComputedStyle(asideRef.value ?? document.documentElement)
      .getPropertyValue("--wizard-aside-sticky-top")
      .replace("px", ""),
  );
  const effectiveTop = Number.isFinite(stickyTop)
    ? Math.max(top, stickyTop)
    : top;

  asideViewportHeight.value = Math.max(
    360,
    Math.floor(window.innerHeight - effectiveTop - 32),
  );
}

function updateLayoutMeasurements(): void {
  updateMainHeight();
  updateAsideViewportHeight();
}

function scheduleLayoutMeasurements(): void {
  if (typeof window === "undefined" || measureFrame !== null) {
    return;
  }

  measureFrame = window.requestAnimationFrame(() => {
    measureFrame = null;
    updateLayoutMeasurements();
  });
}

onMounted(() => {
  nextTick(updateLayoutMeasurements);

  if (typeof ResizeObserver !== "undefined" && mainRef.value) {
    resizeObserver = new ResizeObserver(scheduleLayoutMeasurements);
    resizeObserver.observe(mainRef.value);
  }

  window.addEventListener("resize", scheduleLayoutMeasurements);
  document.addEventListener("scroll", scheduleLayoutMeasurements, true);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  window.removeEventListener("resize", scheduleLayoutMeasurements);
  document.removeEventListener("scroll", scheduleLayoutMeasurements, true);

  if (measureFrame !== null) {
    window.cancelAnimationFrame(measureFrame);
  }
});
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
  grid-template-columns: minmax(0, 1fr) minmax(320px, 360px);
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
  --wizard-aside-sticky-top: 84px;

  top: var(--wizard-aside-sticky-top);
  display: flex;
  min-width: 0;
  max-height: min(
    var(--wizard-aside-viewport-height, calc(100vh - 160px)),
    var(--wizard-main-height, 100vh)
  );
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #f6f8fb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-color: #c5d0de transparent;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

.wizard-workspace__aside::-webkit-scrollbar {
  width: 8px;
}

.wizard-workspace__aside::-webkit-scrollbar-track {
  background: transparent;
}

.wizard-workspace__aside::-webkit-scrollbar-thumb {
  border: 2px solid #f6f8fb;
  border-radius: 999px;
  background: #c5d0de;
}

.wizard-workspace__aside::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
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
