<template>
  <div
    :class="[
      'glass-card-hover group animate-fade-in-up overflow-hidden',
      viewMode === 'list' ? 'flex flex-col' : '',
    ]"
    :style="{ animationDelay: `${index * 60}ms` }"
  >
    <div v-if="viewMode === 'list'" class="flex">
      <div class="relative overflow-hidden w-[300px] h-[300px] flex-shrink-0">
        <img
          v-if="currentImageUrl"
          :src="currentImageUrl"
          :alt="product.productName"
          loading="lazy"
          class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110 cursor-pointer"
          @click="$emit('preview', currentImageUrl)"
        />
        <div
          v-else
          class="w-full h-full bg-primary-dark flex items-center justify-center"
        >
          <svg
            class="w-10 h-10 text-gray-700"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
        </div>
        <div
          class="absolute inset-0 bg-gradient-to-t from-primary-deeper/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"
        />
        <span
          v-if="product.spu"
          class="absolute top-2 right-2 px-2 py-0.5 bg-accent/20 backdrop-blur-sm border border-accent/30 text-accent text-[10px] rounded-md font-mono"
        >
          {{ product.spu }}
        </span>
      </div>

      <div class="flex-1 p-4 min-w-0 h-[300px] flex flex-col justify-between">
        <div class="space-y-3">
          <div>
            <h3
              class="text-sm font-semibold text-white truncate group-hover:text-accent transition-colors duration-200"
            >
              {{ product.productName }}
            </h3>
            <a
              :href="product.sourceUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="text-[11px] text-gray-500 hover:text-accent transition-colors break-all block mt-0.5"
            >
              {{ product.sourceUrl }}
            </a>
          </div>

          <div>
            <div class="flex items-center gap-1 mb-1.5">
              <div class="flex items-center gap-1.5">
                <svg
                  class="w-3 h-3 text-gray-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
                  />
                  <line x1="7" y1="7" x2="7.01" y2="7" />
                </svg>
                <span class="text-[11px] text-gray-500">标签</span>
              </div>
              <button
                class="text-[11px] text-accent/70 hover:text-accent transition-colors cursor-pointer"
                @click="openTagSelector"
              >
                + 选择标签
              </button>
            </div>
            <div v-if="selectedTags.length > 0" class="flex flex-wrap gap-1">
              <span
                v-for="tag in selectedTags"
                :key="tag.tagId"
                :class="[
                  'px-2 py-0.5 text-[10px] rounded-md flex items-center gap-1 border transition-colors duration-200',
                  tag.tagType === 'MENU'
                    ? 'bg-purple/15 text-purple-light border-purple/20'
                    : 'bg-accent/15 text-accent-light border-accent/20',
                ]"
              >
                {{ tag.tagName }}
                <button
                  class="hover:text-danger transition-colors cursor-pointer"
                  :aria-label="`移除标签${tag.tagName}`"
                  @click="removeTag(tag.tagId)"
                >
                  ×
                </button>
              </span>
            </div>
            <div v-else class="text-[11px] text-gray-600">未选择标签</div>
          </div>
        </div>

        <div>
          <div class="flex items-center gap-1.5 mb-1.5">
            <svg
              class="w-3 h-3 text-gray-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <path d="M21 15l-5-5L5 21" />
            </svg>
            <span class="text-[11px] text-gray-500"
              >图片 ({{ product.mediaUrlList.length }})</span
            >
          </div>
          <div
            class="flex flex-wrap gap-1.5 pb-1 scrollbar-thin max-h-[120px] overflow-y-auto"
          >
            <div
              v-for="(url, imgIdx) in product.mediaUrlList"
              :key="imgIdx"
              class="relative group/img"
            >
              <img
                :src="url"
                :alt="`${product.productName} - 图片${imgIdx + 1}`"
                loading="lazy"
                :class="[
                  'w-16 h-16 object-cover rounded-lg border cursor-pointer hover:border-accent/40 transition-all duration-200 border-white/5',
                  currentImageIndex === imgIdx ? 'ring-2 ring-accent' : '',
                ]"
                @click="selectImage(imgIdx)"
              />
              <button
                class="absolute -top-1 -right-1 w-4 h-4 bg-danger/80 rounded-full text-white text-[9px] opacity-0 group-hover/img:opacity-100 transition-opacity duration-200 flex items-center justify-center cursor-pointer"
                :aria-label="`删除图片${imgIdx + 1}`"
                @click="$emit('delete-image', product.id, imgIdx)"
              >
                ×
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="relative overflow-hidden h-48">
      <img
        v-if="product.mediaUrlList.length > 0"
        :src="product.mediaUrlList[0]"
        :alt="product.productName"
        loading="lazy"
        class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110 cursor-pointer"
        @click="$emit('preview', product.mediaUrlList[0])"
      />
      <div
        v-else
        class="w-full h-full bg-primary-dark flex items-center justify-center"
      >
        <svg
          class="w-10 h-10 text-gray-700"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
        >
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <circle cx="8.5" cy="8.5" r="1.5" />
          <path d="M21 15l-5-5L5 21" />
        </svg>
      </div>
      <div
        class="absolute inset-0 bg-gradient-to-t from-primary-deeper/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"
      />
      <span
        v-if="product.spu"
        class="absolute top-2 right-2 px-2 py-0.5 bg-accent/20 backdrop-blur-sm border border-accent/30 text-accent text-[10px] rounded-md font-mono"
      >
        {{ product.spu }}
      </span>
      <div class="absolute bottom-2 left-2 flex gap-1">
        <span
          class="px-1.5 py-0.5 bg-black/50 backdrop-blur-sm text-white text-[10px] rounded"
        >
          {{ product.mediaUrlList.length }} 图
        </span>
        <span
          class="px-1.5 py-0.5 bg-black/50 backdrop-blur-sm text-white text-[10px] rounded"
        >
          {{ product.productVariantList.length }} 变体
        </span>
      </div>
    </div>

    <div v-if="viewMode === 'list'" class="w-full px-4 pb-4 space-y-3">
      <div class="neon-line" />

      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <svg
            class="w-3 h-3 text-gray-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <span class="text-[11px] text-gray-500">规格选项</span>
        </div>
        <div class="space-y-1">
          <div
            v-for="(option, optIdx) in product.optionList"
            :key="optIdx"
            class="flex items-center gap-1.5 group/opt"
          >
            <span class="text-[11px] text-gray-400 w-14 flex-shrink-0"
              >{{ option.chineseName }}:</span
            >
            <div class="flex flex-wrap gap-1 flex-1 min-w-0">
              <span
                v-for="(val, vIdx) in option.values"
                :key="vIdx"
                class="px-1.5 py-0.5 bg-primary-dark/60 text-gray-300 text-[10px] rounded border border-white/5"
              >
                {{ val.chineseValue }}
              </span>
            </div>
            <button
              class="w-4 h-4 flex items-center justify-center text-danger/0 group-hover/opt:text-danger/80 hover:!text-danger text-[10px] transition-all duration-200 flex-shrink-0 cursor-pointer"
              :aria-label="`删除规格${option.chineseName}`"
              @click="$emit('delete-option', product.id, optIdx)"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <div>
        <div class="flex items-center justify-between mb-1.5">
          <div class="flex items-center gap-1.5">
            <svg
              class="w-3 h-3 text-gray-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"
              />
              <rect x="8" y="2" width="8" height="4" rx="1" />
            </svg>
            <span class="text-[11px] text-gray-500"
              >变体 ({{ product.productVariantList.length }})</span
            >
          </div>
          <div class="flex items-center gap-1">
            <button
              v-for="count in [2, 3, 4, 5]"
              :key="count"
              :class="[
                'px-2 py-0.5 text-[10px] rounded border transition-all cursor-pointer',
                variantsPerRow === count
                  ? 'bg-accent/20 text-accent border-accent/40'
                  : 'bg-primary-dark/40 text-gray-400 border-white/10 hover:border-white/20',
              ]"
              @click="changeVariantsPerRow(count)"
            >
              {{ count }}列
            </button>
          </div>
        </div>
        <div class="max-h-48 overflow-y-auto space-y-1 scrollbar-thin">
          <div
            class="grid gap-1"
            :style="{ gridTemplateColumns: `repeat(${variantsPerRow}, 1fr)` }"
          >
            <div
              v-for="(variant, vIdx) in product.productVariantList"
              :key="vIdx"
              class="flex items-center gap-2 p-1.5 bg-primary-dark/40 rounded-lg text-[11px] border border-white/[0.03] hover:border-white/10 transition-colors duration-200"
            >
              <img
                v-if="variant.mediaUrl"
                :src="variant.mediaUrl"
                :alt="`变体${vIdx + 1}`"
                loading="lazy"
                class="w-7 h-7 object-cover rounded cursor-pointer hover:ring-1 hover:ring-accent/50 transition-all flex-shrink-0"
                @click="selectVariantImage(variant.mediaUrl)"
              />
              <div
                v-else
                class="w-7 h-7 bg-primary-dark rounded flex items-center justify-center flex-shrink-0"
              >
                <svg
                  class="w-3 h-3 text-gray-600"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                </svg>
              </div>
              <div class="flex-1 min-w-0 flex flex-col gap-0.5">
                <span
                  v-for="ov in variant.optionValueList"
                  :key="ov.englishValue"
                  :title="ov.chineseValue"
                  class="text-gray-400 truncate text-[10px]"
                >
                  {{ ov.chineseValue }}
                </span>
              </div>
              <div class="flex flex-col items-end gap-0.5 flex-shrink-0">
                <span class="text-accent font-medium font-mono text-[11px]">
                  ¥{{ variant.purchasePrice.toFixed(2) }}
                </span>
                <span
                  :class="[
                    'px-1 py-0.5 rounded text-[9px] font-medium',
                    variant.isActiveAvailable
                      ? 'bg-success/15 text-success'
                      : 'bg-danger/15 text-danger',
                  ]"
                >
                  {{ variant.isActiveAvailable ? "可购" : "缺货" }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="flex-1 p-4 space-y-3 min-w-0">
      <div>
        <h3
          class="text-sm font-semibold text-white truncate group-hover:text-accent transition-colors duration-200"
        >
          {{ product.productName }}
        </h3>
        <a
          :href="product.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="text-[11px] text-gray-500 hover:text-accent transition-colors break-all block mt-0.5"
        >
          {{ product.sourceUrl }}
        </a>
      </div>

      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <svg
            class="w-3 h-3 text-gray-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
          <span class="text-[11px] text-gray-500"
            >图片 ({{ product.mediaUrlList.length }})</span
          >
        </div>
        <div class="flex gap-1.5 overflow-x-auto pb-1 scrollbar-thin">
          <div
            v-for="(url, imgIdx) in product.mediaUrlList.slice(0, 5)"
            :key="imgIdx"
            class="relative flex-shrink-0 group/img"
          >
            <img
              :src="url"
              :alt="`${product.productName} - 图片${imgIdx + 1}`"
              loading="lazy"
              class="w-12 h-12 object-cover rounded-lg border border-white/5 cursor-pointer hover:border-accent/40 transition-all duration-200"
              @click="$emit('preview', url)"
            />
            <button
              class="absolute -top-1 -right-1 w-4 h-4 bg-danger/80 rounded-full text-white text-[9px] opacity-0 group-hover/img:opacity-100 transition-opacity duration-200 flex items-center justify-center cursor-pointer"
              :aria-label="`删除图片${imgIdx + 1}`"
              @click="$emit('delete-image', product.id, imgIdx)"
            >
              ×
            </button>
          </div>
          <div
            v-if="product.mediaUrlList.length > 5"
            class="w-12 h-12 flex-shrink-0 bg-primary-dark/60 rounded-lg border border-white/5 flex items-center justify-center text-[10px] text-gray-500"
          >
            +{{ product.mediaUrlList.length - 5 }}
          </div>
        </div>
      </div>

      <div>
        <div class="flex items-center justify-between mb-1.5">
          <div class="flex items-center gap-1.5">
            <svg
              class="w-3 h-3 text-gray-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
              />
              <line x1="7" y1="7" x2="7.01" y2="7" />
            </svg>
            <span class="text-[11px] text-gray-500">标签</span>
          </div>
          <button
            class="text-[11px] text-accent/70 hover:text-accent transition-colors cursor-pointer"
            @click="openTagSelector"
          >
            + 选择标签
          </button>
        </div>
        <div v-if="selectedTags.length > 0" class="flex flex-wrap gap-1">
          <span
            v-for="tag in selectedTags"
            :key="tag.tagId"
            :class="[
              'px-2 py-0.5 text-[10px] rounded-md flex items-center gap-1 border transition-colors duration-200',
              tag.tagType === 'MENU'
                ? 'bg-purple/15 text-purple-light border-purple/20'
                : 'bg-accent/15 text-accent-light border-accent/20',
            ]"
          >
            {{ tag.tagName }}
            <button
              class="hover:text-danger transition-colors cursor-pointer"
              :aria-label="`移除标签${tag.tagName}`"
              @click="removeTag(tag.tagId)"
            >
              ×
            </button>
          </span>
        </div>
        <div v-else class="text-[11px] text-gray-600">未选择标签</div>
      </div>

      <div class="neon-line" />

      <div>
        <div class="flex items-center gap-1.5 mb-1.5">
          <svg
            class="w-3 h-3 text-gray-500"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 2L2 7l10 5 10-5-10-5z" />
            <path d="M2 17l10 5 10-5" />
            <path d="M2 12l10 5 10-5" />
          </svg>
          <span class="text-[11px] text-gray-500">规格选项</span>
        </div>
        <div class="space-y-1">
          <div
            v-for="(option, optIdx) in product.optionList"
            :key="optIdx"
            class="flex items-center gap-1.5 group/opt"
          >
            <span class="text-[11px] text-gray-400 w-14 flex-shrink-0"
              >{{ option.chineseName }}:</span
            >
            <div class="flex flex-wrap gap-1 flex-1 min-w-0">
              <span
                v-for="(val, vIdx) in option.values"
                :key="vIdx"
                class="px-1.5 py-0.5 bg-primary-dark/60 text-gray-300 text-[10px] rounded border border-white/5"
              >
                {{ val.chineseValue }}
              </span>
            </div>
            <button
              class="w-4 h-4 flex items-center justify-center text-danger/0 group-hover/opt:text-danger/80 hover:!text-danger text-[10px] transition-all duration-200 flex-shrink-0 cursor-pointer"
              :aria-label="`删除规格${option.chineseName}`"
              @click="$emit('delete-option', product.id, optIdx)"
            >
              ×
            </button>
          </div>
        </div>
      </div>

      <div>
        <div class="flex items-center justify-between mb-1.5">
          <div class="flex items-center gap-1.5">
            <svg
              class="w-3 h-3 text-gray-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"
              />
              <rect x="8" y="2" width="8" height="4" rx="1" />
            </svg>
            <span class="text-[11px] text-gray-500"
              >变体 ({{ product.productVariantList.length }})</span
            >
          </div>
        </div>
        <div class="max-h-32 overflow-y-auto space-y-1 scrollbar-thin">
          <div
            v-for="(variant, vIdx) in product.productVariantList"
            :key="vIdx"
            class="flex items-center gap-2 p-1.5 bg-primary-dark/40 rounded-lg text-[11px] border border-white/[0.03] hover:border-white/10 transition-colors duration-200"
          >
            <img
              v-if="variant.mediaUrl"
              :src="variant.mediaUrl"
              :alt="`变体${vIdx + 1}`"
              loading="lazy"
              class="w-7 h-7 object-cover rounded cursor-pointer hover:ring-1 hover:ring-accent/50 transition-all"
              @click="$emit('preview', variant.mediaUrl)"
            />
            <div
              v-else
              class="w-7 h-7 bg-primary-dark rounded flex items-center justify-center"
            >
              <svg
                class="w-3 h-3 text-gray-600"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <rect x="3" y="3" width="18" height="18" rx="2" />
              </svg>
            </div>
            <div class="flex-1 min-w-0 flex items-center gap-1">
              <span
                v-for="ov in variant.optionValueList"
                :key="ov.englishValue"
                :title="ov.chineseValue"
                class="text-gray-400 truncate"
              >
                {{ ov.chineseValue }}
              </span>
            </div>
            <span class="text-accent font-medium font-mono text-[11px]">
              ¥{{ variant.purchasePrice.toFixed(2) }}
            </span>
            <span
              :class="[
                'px-1 py-0.5 rounded text-[9px] font-medium',
                variant.isActiveAvailable
                  ? 'bg-success/15 text-success'
                  : 'bg-danger/15 text-danger',
              ]"
            >
              {{ variant.isActiveAvailable ? "可购" : "缺货" }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <TagSelector
      :visible="showTagSelector"
      :tag-tree="tagTree"
      :selected-ids="product.tagIds"
      :is-loading="isLoading"
      @close="showTagSelector = false"
      @toggle="toggleTag"
      @confirm="confirmSelection"
      @refresh-tags="handleRefreshTags"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import type { Product, TagNode } from "@/types";
import { fetchTagTree, generateSPU } from "@/api";
import TagSelector from "./TagSelector.vue";

const props = defineProps<{
  product: Product;
  viewMode: "grid" | "list";
  index: number;
}>();

const emit = defineEmits<{
  (e: "delete-image", productId: string, imageIndex: number): void;
  (e: "delete-option", productId: string, optionIndex: number): void;
  (e: "update-tags", productId: string, tagIds: number[], spu: string): void;
  (e: "preview", url: string): void;
}>();

const showTagSelector = ref(false);
const tagTree = ref<TagNode[]>([]);
const isLoading = ref(false);
const currentImageIndex = ref(0);
const variantsPerRow = ref(3);

const selectedTags = computed(() => {
  const tagIds = props.product.tagIds;
  const ids = new Set(Array.isArray(tagIds) ? tagIds : []);
  const result: TagNode[] = [];

  function collectTags(tags: TagNode[]) {
    for (const tag of tags) {
      if (ids.has(tag.tagId)) result.push(tag);
      if (tag.children.length > 0) collectTags(tag.children);
    }
  }

  collectTags(tagTree.value);
  return result;
});

const currentImageUrl = computed(() => {
  if (
    props.product.mediaUrlList.length > 0 &&
    currentImageIndex.value < props.product.mediaUrlList.length
  ) {
    return props.product.mediaUrlList[currentImageIndex.value];
  }
  return null;
});

async function loadTags(forceRefresh = false): Promise<void> {
  isLoading.value = true;
  try {
    tagTree.value = await fetchTagTree(forceRefresh);
  } catch (error) {
    console.error("[ProductCard] Failed to load tags:", error);
  } finally {
    isLoading.value = false;
  }
}

async function handleRefreshTags(): Promise<void> {
  await loadTags(true);
}

function openTagSelector(): void {
  showTagSelector.value = true;
  if (tagTree.value.length === 0) loadTags();
}

function selectImage(index: number): void {
  currentImageIndex.value = index;
}

function selectVariantImage(url: string): void {
  const index = props.product.mediaUrlList.indexOf(url);
  if (index > -1) {
    currentImageIndex.value = index;
  }
}

function changeVariantsPerRow(count: number): void {
  variantsPerRow.value = count;
}

function toggleTag(tagId: number): void {
  const currentIds = [...props.product.tagIds];
  const idx = currentIds.indexOf(tagId);
  if (idx > -1) {
    currentIds.splice(idx, 1);
  } else {
    currentIds.push(tagId);
  }
  emit("update-tags", props.product.id, currentIds, props.product.spu);
}

function removeTag(tagId: number): void {
  const currentIds = props.product.tagIds.filter((id) => id !== tagId);
  emit("update-tags", props.product.id, currentIds, props.product.spu);
}

async function confirmSelection(): Promise<void> {
  showTagSelector.value = false;
  let newSpu = props.product.spu;
  if (!newSpu) {
    for (const tagId of props.product.tagIds) {
      const tag = findTagById(tagTree.value, tagId);
      if (tag && tag.tagType === "MENU" && tag.children.length === 0) {
        newSpu = await generateSPU(tag);
        if (newSpu) break;
      }
    }
  }
  emit("update-tags", props.product.id, props.product.tagIds, newSpu);
}

function findTagById(tags: TagNode[], id: number): TagNode | null {
  for (const tag of tags) {
    if (tag.tagId === id) return tag;
    if (tag.children.length > 0) {
      const found = findTagById(tag.children, id);
      if (found) return found;
    }
  }
  return null;
}

onMounted(() => {
  loadTags();
});
</script>
