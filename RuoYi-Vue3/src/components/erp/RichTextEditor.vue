<template>
  <div class="rte" :class="{ 'rte--fullscreen': isFullscreen }">
    <div class="rte__toolbar">
      <div class="rte__toolbar-left">
        <div class="rte__mode-switch">
          <button
            v-for="m in modes"
            :key="m.value"
            type="button"
            :class="[
              'rte__mode-btn',
              { 'rte__mode-btn--active': mode === m.value },
            ]"
            :title="m.label"
            @click="switchMode(m.value)"
          >
            <svg
              v-if="m.value === 'code'"
              class="rte__icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
            <svg
              v-else
              class="rte__icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <span class="rte__mode-label">{{ m.label }}</span>
          </button>
        </div>
      </div>

      <div v-if="mode === 'code'" class="rte__toolbar-center">
        <button
          type="button"
          class="rte__tool-btn"
          title="格式化代码"
          @click="formatCode"
        >
          <svg
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
          <span>格式化</span>
        </button>
        <button
          type="button"
          class="rte__tool-btn"
          title="压缩代码"
          @click="minifyCode"
        >
          <svg
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="4 17 10 11 4 5" />
            <line x1="12" y1="19" x2="20" y2="19" />
          </svg>
          <span>压缩</span>
        </button>
        <button
          type="button"
          class="rte__tool-btn"
          title="复制代码"
          @click="copyCode"
        >
          <svg
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
          </svg>
          <span>复制</span>
        </button>
        <button
          type="button"
          class="rte__tool-btn"
          title="清空内容"
          @click="clearCode"
        >
          <svg
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="3 6 5 6 21 6" />
            <path
              d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
            />
          </svg>
          <span>清空</span>
        </button>
      </div>

      <div class="rte__toolbar-right">
        <slot name="toolbar-actions"></slot>
        <div
          v-if="mode === 'code' && htmlErrors.length > 0"
          class="rte__error-badge"
          :title="htmlErrors.join('\n')"
        >
          <svg
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          <span>{{ htmlErrors.length }}</span>
        </div>
        <div
          v-if="mode === 'code'"
          class="rte__char-count"
          :class="{ 'rte__char-count--warn': charCount > 50000 }"
        >
          {{ charCount }} 字符
        </div>
        <button
          type="button"
          class="rte__tool-btn"
          :title="isFullscreen ? '退出全屏' : '全屏编辑'"
          @click="toggleFullscreen"
        >
          <svg
            v-if="!isFullscreen"
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="15 3 21 3 21 9" />
            <polyline points="9 21 3 21 3 15" />
            <line x1="21" y1="3" x2="14" y2="10" />
            <line x1="3" y1="21" x2="10" y2="14" />
          </svg>
          <svg
            v-else
            class="rte__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="4 14 10 14 10 20" />
            <polyline points="20 10 14 10 14 4" />
            <line x1="14" y1="10" x2="21" y2="3" />
            <line x1="3" y1="21" x2="10" y2="14" />
          </svg>
        </button>
      </div>
    </div>

    <div class="rte__body">
      <Transition name="rte-fade" mode="out-in">
        <div v-if="mode === 'code'" key="code" class="rte__code">
          <div class="rte__code-editor" ref="codeEditorRef">
            <div class="rte__line-numbers">
              <div v-for="n in lineCount" :key="n" class="rte__line-number">
                {{ n }}
              </div>
            </div>
            <textarea
              ref="codeTextareaRef"
              v-model="codeContent"
              class="rte__code-input"
              spellcheck="false"
              @scroll="syncScroll"
              @input="onCodeInput"
            ></textarea>
            <pre
              class="rte__code-highlight"
              aria-hidden="true"
            ><code ref="highlightRef" class="hljs language-html" v-html="highlightedCode"></code></pre>
          </div>
        </div>

        <div v-else key="preview" class="rte__preview">
          <div
            v-if="modelValue"
            class="rte__preview-content"
            v-html="modelValue"
          ></div>
          <div v-else class="rte__preview-empty" @click="emit('sync-html')">
            <svg
              class="rte__preview-empty-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <span>暂无内容，请点击同步HTML</span>
          </div>
        </div>
      </Transition>
    </div>

    <div v-if="mode === 'code' && htmlErrors.length > 0" class="rte__statusbar">
      <div
        v-for="(err, i) in htmlErrors.slice(0, 3)"
        :key="i"
        class="rte__statusbar-error"
      >
        <svg
          class="rte__icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"
          />
          <line x1="12" y1="9" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
        {{ err }}
      </div>
      <div v-if="htmlErrors.length > 3" class="rte__statusbar-more">
        还有 {{ htmlErrors.length - 3 }} 个问题...
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from "vue";
import hljs from "highlight.js";
import { html_beautify } from "js-beautify";

type EditorMode = "code" | "preview";

const modes: { value: EditorMode; label: string }[] = [
  { value: "code", label: "代码" },
  { value: "preview", label: "预览" },
];

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    minHeight?: number;
    readOnly?: boolean;
  }>(),
  {
    modelValue: "",
    placeholder: "请输入商品详情描述",
    minHeight: 280,
    readOnly: false,
  },
);

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
  (e: "sync-html"): void;
}>();

const mode = ref<EditorMode>("preview");
const isFullscreen = ref(false);
const codeContent = ref("");
const htmlErrors = ref<string[]>([]);
const charCount = ref(0);

const codeTextareaRef = ref<HTMLTextAreaElement | null>(null);
const codeEditorRef = ref<HTMLElement | null>(null);
const highlightRef = ref<HTMLElement | null>(null);

let isInternalUpdate = false;

const lineCount = computed(() => {
  return (codeContent.value || "").split("\n").length;
});

const highlightedCode = computed(() => {
  try {
    if (!codeContent.value) return "";
    return hljs.highlight(codeContent.value, { language: "html" }).value;
  } catch {
    return codeContent.value;
  }
});

function switchMode(newMode: EditorMode) {
  if (newMode === mode.value) return;

  if (mode.value === "code") {
    // codeContent is already up to date via v-model
  }

  mode.value = newMode;

  if (newMode === "code") {
    codeContent.value = props.modelValue || "";
    charCount.value = codeContent.value.length;
    validateHtml();
  }
}

function onCodeInput() {
  charCount.value = codeContent.value.length;
  isInternalUpdate = true;
  emit("update:modelValue", codeContent.value);
  nextTick(() => {
    isInternalUpdate = false;
  });
  validateHtml();
}

function validateHtml() {
  const errors: string[] = [];
  const html = codeContent.value;

  if (!html) {
    htmlErrors.value = [];
    return;
  }

  const openTags: string[] = [];
  const tagRegex = /<\/?([a-zA-Z][a-zA-Z0-9]*)[^>]*>/g;
  const selfClosing = new Set([
    "br",
    "hr",
    "img",
    "input",
    "meta",
    "link",
    "area",
    "base",
    "col",
    "embed",
    "source",
    "track",
    "wbr",
  ]);
  let match: RegExpExecArray | null;

  while ((match = tagRegex.exec(html)) !== null) {
    const tagName = match[1].toLowerCase();
    if (selfClosing.has(tagName)) continue;
    if (match[0].startsWith("</")) {
      if (openTags.length === 0 || openTags[openTags.length - 1] !== tagName) {
        errors.push(`未匹配的闭合标签: </${tagName}>`);
      } else {
        openTags.pop();
      }
    } else {
      openTags.push(tagName);
    }
  }

  openTags.forEach((tag) => {
    errors.push(`未闭合的标签: <${tag}>`);
  });

  const attrRegex = /\s([a-zA-Z-]+)\s*=\s*["']([^"']*)$/gm;
  if (attrRegex.test(html)) {
    errors.push("存在未闭合的属性引号");
  }

  htmlErrors.value = errors;
}

function formatCode() {
  try {
    codeContent.value = html_beautify(codeContent.value, {
      indent_size: 2,
      wrap_line_length: 120,
      indent_inner_html: true,
      preserve_newlines: true,
      max_preserve_newlines: 2,
    });
    isInternalUpdate = true;
    emit("update:modelValue", codeContent.value);
    nextTick(() => {
      isInternalUpdate = false;
    });
  } catch {
    // ignore formatting errors
  }
}

function minifyCode() {
  try {
    const minified = codeContent.value
      .replace(/\n\s*\n/g, "\n")
      .replace(/>\s+</g, "><")
      .replace(/\s{2,}/g, " ")
      .trim();
    codeContent.value = minified;
    isInternalUpdate = true;
    emit("update:modelValue", codeContent.value);
    nextTick(() => {
      isInternalUpdate = false;
    });
  } catch {
    // ignore minification errors
  }
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(codeContent.value);
  } catch {
    // fallback
    const textarea = document.createElement("textarea");
    textarea.value = codeContent.value;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
  }
}

function clearCode() {
  codeContent.value = "";
  isInternalUpdate = true;
  emit("update:modelValue", "");
  nextTick(() => {
    isInternalUpdate = false;
  });
  htmlErrors.value = [];
}

function syncScroll() {
  if (!codeTextareaRef.value || !highlightRef.value) return;
  highlightRef.value.scrollTop = codeTextareaRef.value.scrollTop;
  highlightRef.value.scrollLeft = codeTextareaRef.value.scrollLeft;
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value;
}

watch(
  () => props.modelValue,
  (val) => {
    if (isInternalUpdate) return;
    if (mode.value === "code") {
      if (val !== codeContent.value) {
        codeContent.value = val || "";
        charCount.value = codeContent.value.length;
      }
    }
  },
);

onMounted(() => {
  codeContent.value = props.modelValue || "";
  charCount.value = codeContent.value.length;
});
</script>

<style>
@import "highlight.js/styles/atom-one-dark.css";

.rte__tool-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}

.rte__tool-btn:hover {
  background: #f1f5f9;
  border-color: rgba(37, 99, 235, 0.2);
  color: #2563eb;
}

.rte__tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  background: #f8fafc;
  color: #94a3b8;
  border-color: rgba(148, 163, 184, 0.12);
}

.rte__tool-btn--apply {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: rgba(37, 99, 235, 0.25);
  color: #2563eb;
}

.rte__tool-btn--apply:hover:not(:disabled) {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-color: rgba(37, 99, 235, 0.4);
}

.rte__tool-btn--extract {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: rgba(22, 163, 74, 0.25);
  color: #16a34a;
}

.rte__tool-btn--extract:hover:not(:disabled) {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  border-color: rgba(22, 163, 74, 0.4);
}
</style>

<style scoped>
.rte {
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  min-height: v-bind(minHeight + 48 + "px");
}

.rte:hover {
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.08);
}

.rte--fullscreen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  border-radius: 0;
  border: none;
  min-height: 0;
}

.rte__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.96),
    rgba(241, 245, 249, 0.96)
  );
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  gap: 12px;
  flex-shrink: 0;
}

.rte__toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rte__toolbar-center {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rte__toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.rte__mode-switch {
  display: flex;
  align-items: center;
  background: rgba(15, 23, 42, 0.04);
  border-radius: 10px;
  padding: 3px;
  gap: 2px;
}

.rte__mode-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.rte__mode-btn:hover {
  color: #334155;
  background: rgba(255, 255, 255, 0.6);
}

.rte__mode-btn--active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
}

.rte__icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.rte__mode-label {
  line-height: 1;
}

.rte__error-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  color: #ef4444;
  font-size: 11px;
  font-weight: 600;
}

.rte__char-count {
  font-size: 11px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

.rte__char-count--warn {
  color: #f59e0b;
}

.rte__body {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.rte--fullscreen .rte__body {
  min-height: 0;
  flex: 1;
}

.rte__code {
  height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: v-bind(minHeight + "px");
}

.rte__code-editor {
  position: relative;
  flex: 1;
  display: flex;
  overflow: hidden;
  background: #1e1e2e;
}

.rte__line-numbers {
  width: 48px;
  padding: 16px 8px 16px 0;
  text-align: right;
  background: #181825;
  color: #585b70;
  font-family: "Fira Code", "Cascadia Code", "JetBrains Mono", Consolas,
    monospace;
  font-size: 13px;
  line-height: 1.6;
  user-select: none;
  overflow: hidden;
  flex-shrink: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.rte__line-number {
  padding-right: 8px;
  font-variant-numeric: tabular-nums;
}

.rte__code-input {
  position: absolute;
  top: 0;
  left: 48px;
  right: 0;
  bottom: 0;
  width: calc(100% - 48px);
  height: 100%;
  padding: 16px;
  background: transparent;
  color: transparent;
  caret-color: #89b4fa;
  border: none;
  outline: none;
  resize: none;
  font-family: "Fira Code", "Cascadia Code", "JetBrains Mono", Consolas,
    monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  white-space: pre;
  overflow: auto;
  z-index: 2;
}

.rte__code-highlight {
  position: absolute;
  top: 0;
  left: 48px;
  right: 0;
  bottom: 0;
  margin: 0;
  padding: 16px;
  background: transparent;
  color: #cdd6f4;
  font-family: "Fira Code", "Cascadia Code", "JetBrains Mono", Consolas,
    monospace;
  font-size: 13px;
  line-height: 1.6;
  tab-size: 2;
  white-space: pre;
  overflow: auto;
  pointer-events: none;
  z-index: 1;
}

.rte__code-highlight code {
  font-family: inherit;
  font-size: inherit;
  background: transparent !important;
  padding: 0 !important;
}

.rte__preview {
  min-height: v-bind(minHeight + "px");
  padding: 24px;
  background: #ffffff;
  overflow-y: auto;
}

.rte__preview-content {
  color: #334155;
  line-height: 1.8;
  font-size: 14px;
}

.rte__preview-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 8px 0;
}

.rte__preview-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.rte__preview-content :deep(th),
.rte__preview-content :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}

.rte__preview-content :deep(th) {
  background: #f8fafc;
  font-weight: 600;
}

.rte__preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 20px;
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
}

.rte__preview-empty-icon {
  width: 40px;
  height: 40px;
  opacity: 0.4;
}

.rte__statusbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  padding: 8px 16px;
  background: rgba(239, 68, 68, 0.04);
  border-top: 1px solid rgba(239, 68, 68, 0.12);
  font-size: 11px;
}

.rte__statusbar-error {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ef4444;
}

.rte__statusbar-error .rte__icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.rte__statusbar-more {
  color: #94a3b8;
}

.rte-fade-enter-active,
.rte-fade-leave-active {
  transition: opacity 0.15s ease;
}

.rte-fade-enter-from,
.rte-fade-leave-to {
  opacity: 0;
}
</style>
