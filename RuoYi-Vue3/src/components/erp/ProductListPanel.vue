<template>
  <div class="app-container product-workbench">
    <div class="workbench-header">
      <div class="toolbar-primary">
        <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="90px" class="filter-form toolbar-filter-form">
          <el-form-item label="关键词" prop="searchKeyword" class="keyword-filter-item">
            <el-input
              v-model="queryParams.searchKeyword"
              placeholder="标题 / SPU / SKU / Shopify ID"
              clearable
              class="w-218"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="同步时间">
            <el-date-picker
              v-model="daterangeLastSyncTime"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              class="w-260"
            />
          </el-form-item>
          <el-form-item class="filter-actions">
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            <el-button link type="primary" @click="advancedVisible = !advancedVisible">
              {{ advancedVisible ? "收起筛选" : "高级筛选" }}
              <el-icon class="el-icon--right"><ArrowUp v-if="advancedVisible" /><ArrowDown v-else /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-collapse-transition>
        <el-form v-show="advancedVisible" :model="queryParams" :inline="true" label-width="90px" class="filter-form advanced-form">
          <el-form-item label="店铺" prop="storeId">
            <el-select v-model="queryParams.storeId" placeholder="请选择店铺" filterable class="w-218" :loading="storeAction.loading">
              <el-option
                v-for="store in activeStores"
                :key="store.storeId"
                :label="store.isDefault === '1' ? `${store.storeName}（默认）` : store.storeName"
                :value="store.storeId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="更新人" prop="updatedBy">
            <el-input v-model="queryParams.updatedBy" placeholder="更新人" clearable class="w-218" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-input v-model="queryParams.category" placeholder="分类" clearable class="w-218" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="商品类型" prop="productType">
            <el-input v-model="queryParams.productType" placeholder="商品类型" clearable class="w-218" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="来源链接" prop="sourceUrl">
            <el-input v-model="queryParams.sourceUrl" placeholder="来源链接关键词" clearable class="w-218" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="采购链接" prop="purchaseUrl">
            <el-input v-model="queryParams.purchaseUrl" placeholder="采购链接关键词" clearable class="w-218" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="资料完整度" prop="qualityState">
            <el-select v-model="queryParams.qualityState" placeholder="全部" clearable class="w-218">
              <el-option label="资料完整" value="complete" />
              <el-option label="资料不完整" value="incomplete" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-collapse-transition>

      <div class="toolbar-status-row">
        <div class="toolbar-row">
          <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['erp:product:add']">新增选品</el-button>
          <el-button icon="Upload" @click="handleImport" v-hasPermi="['erp:product:import']">导入</el-button>
          <el-popover placement="bottom-end" width="220" trigger="click">
            <template #reference>
              <el-button icon="Setting">列设置</el-button>
            </template>
            <div class="column-settings">
              <el-checkbox
                v-for="column in columns"
                :key="column.key"
                v-model="column.visible"
              >
                {{ column.label }}
              </el-checkbox>
            </div>
          </el-popover>
          <el-dropdown trigger="click" @command="handleToolbarCommand" style="margin-left: 12px;">
            <el-button icon="MoreFilled">
              更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="hasPermi('erp:product:export')" command="export">导出当前筛选</el-dropdown-item>
                <el-dropdown-item command="refresh">刷新列表</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="summary-grid" aria-label="同步状态筛选">
          <button
            v-for="item in summaryCards"
            :key="item.key"
            type="button"
            class="summary-card"
            :class="{ active: item.active }"
            :aria-pressed="item.active"
            @click="applySummaryFilter(item)"
          >
            <span class="summary-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </button>
        </div>
      </div>
    </div>

    <div class="selected-tags" v-show="selectedTags && selectedTags.length > 0">
      <span class="tag-label">已选标签</span>
      <el-tag v-for="tag in selectedTags" :key="tag.tagId" size="small" closable @close="handleRemoveTag(tag)">
        {{ tag.tagName }}
      </el-tag>
    </div>

    <div class="batch-bar" v-show="ids.length > 0">
      <span>已选 {{ ids.length }} 个商品</span>
      <el-button type="primary" plain icon="Promotion" @click="handlePushToShopify">推送到 Shopify</el-button>
      <el-button plain icon="CircleCheck" @click="handleMarkChecked">标记已检查</el-button>
      <el-dropdown trigger="click" @command="handleBatchCommand">
        <el-button plain icon="MoreFilled">更多</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="hasPermi('erp:product:remove')" command="delete" divided>批量删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div class="table-zone">
      <el-table
        v-loading="loading"
        :data="productList"
        row-key="productId"
        class="workbench-table"
        height="100%"
        :row-class-name="getRowClassName"
        :header-cell-style="{ background: '#f7f8fb', color: '#303133' }"
        @selection-change="handleSelectionChange"
      >
      <el-table-column type="selection" width="48" align="center" fixed />
      <el-table-column label="商品信息" min-width="360" fixed>
        <template #default="{ row }">
          <div class="product-cell">
            <el-image :src="getMainImage(row)" fit="cover" class="product-thumb" lazy>
              <template #error>
                <div class="image-empty">无图</div>
              </template>
            </el-image>
            <div class="product-meta">
              <div class="title-row">
                <el-tooltip :content="getProductTitle(row)" placement="top" :disabled="getProductTitle(row).length < 24">
                  <button type="button" class="title-link" @click.stop="openQuickView(row)">{{ getProductTitle(row) }}</button>
                </el-tooltip>
                <el-tooltip content="复制标题" placement="top">
                  <el-button
                    class="copy-icon-btn"
                    link
                    type="primary"
                    icon="CopyDocument"
                    aria-label="复制商品标题"
                    @click.stop="copyText(getProductTitle(row))"
                  />
                </el-tooltip>
                <el-tag v-if="isChecked(row)" size="small" type="success" effect="plain">已检查</el-tag>
              </div>
              <div class="subline">
                <span class="spu-inline">
                  SPU：{{ row.spu || "-" }}
                </span>
                <span>{{ row.category || "未分类" }}</span>
                <span>{{ row.productType || "未设类型" }}</span>
                <span class="muted">{{ row.mediaCount || 0 }} 张图</span>
              </div>
              <div v-if="row.tagCodeList?.length" class="tag-row">
                <el-tag v-for="tag in (row.tagCodeList || []).slice(0, 3)" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                <span v-if="(row.tagCodeList || []).length > 3" class="muted">+{{ (row.tagCodeList || []).length - 3 }} 标签</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column v-if="isColumnVisible('price')" label="价格/变体" width="250">
        <template #default="{ row }">
          <div class="metric-stack">
            <strong>{{ formatCentsRange(row.priceMin, row.priceMax) }}</strong>
            <span>{{ row.variantCount || 0 }} 个变体 · 采购 {{ formatCentsRange(row.purchasePriceMin, row.purchasePriceMax, "¥") }}</span>
            <span v-if="row.skuPreview">SKU：{{ row.skuPreview }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column v-if="isColumnVisible('quality')" label="资料完整度" width="180">
        <template #default="{ row }">
          <div class="quality-stack">
            <el-tag :type="getQualityTagType(row)" effect="light">{{ getQualityTitle(row) }}</el-tag>
            <div v-if="formatMissingFields(row.missingFields).length" class="missing-tags">
              <el-tag v-for="label in formatMissingFields(row.missingFields).slice(0, 3)" :key="label" size="small" type="warning" effect="plain">
                {{ label }}
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column v-if="isColumnVisible('sync')" label="Shopify 同步" min-width="200">
        <template #default="{ row }">
          <div class="sync-cell">
            <div class="sync-title">
              <el-tag :type="getSyncState(row).tagType" effect="light">{{ getSyncState(row).label }}</el-tag>
            </div>
            <div class="sync-detail">
              <span>最后同步：{{ formatDate(row.lastSyncTime) }}</span>
              <span v-if="row.shopifyProductId">Shopify：{{ shortId(row.shopifyProductId) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column v-if="isColumnVisible('editor')" label="最后编辑时间" width="140">
        <template #default="{ row }">
          <div class="metric-stack">
<!--            <span>发布：<dict-tag :options="erp_product_status" :value="row.status" /></span>-->
            <span>{{ formatDate(row.updateTime || row.createTime) }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" @click.stop="handleUpdate(row)">编辑</el-button>
            <template v-if="getSyncState(row).key === 'failed'">
              <el-button link type="danger" @click.stop="handleRetry(row)">重试</el-button>
            </template>
            <template v-else-if="getSyncState(row).key === 'need_resync'">
              <el-button link type="warning" @click.stop="handleRetry(row)">同步</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" @click.stop="handleRetry(row)">同步</el-button>
            </template>

            <el-dropdown trigger="click" @command="handleRowCommand($event, row)">
              <el-button link type="primary" @click.stop="noop">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="purchase" :disabled="!row.purchaseUrl">打开采购链接</el-dropdown-item>
                  <el-dropdown-item command="source" :disabled="!row.sourceUrl">打开来源链接</el-dropdown-item>
                  <el-dropdown-item command="task" :disabled="!row.latestTaskId">查看同步任务</el-dropdown-item>
                  <el-dropdown-item command="copyShopify" :disabled="!row.shopifyProductId">复制 Shopify ID</el-dropdown-item>
                  <el-dropdown-item v-if="hasPermi('erp:product:remove')" command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper">
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <el-drawer
      v-model="quickViewOpen"
      size="540px"
      :title="selectedProduct ? getProductTitle(selectedProduct) : '商品速览'"
      append-to-body
      class="quick-view-drawer"
    >
      <template v-if="selectedProduct">
        <div class="drawer-content">
          <div class="drawer-section image-grid">
            <el-image
              v-for="(url, index) in getMediaUrls(selectedProduct).slice(0, 8)"
              :key="`${url}-${index}`"
              :src="url"
              fit="cover"
              lazy
            />
            <div v-if="getMediaUrls(selectedProduct).length === 0" class="drawer-empty">暂无图片</div>
          </div>

          <el-descriptions :column="2" border class="drawer-section">
            <el-descriptions-item label="SPU">{{ selectedProduct.spu || "-" }}</el-descriptions-item>
            <el-descriptions-item label="变体数">{{ selectedProduct.variantCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="售价">{{ formatCentsRange(selectedProduct.priceMin, selectedProduct.priceMax) }}</el-descriptions-item>
            <el-descriptions-item label="采购价">{{ formatCentsRange(selectedProduct.purchasePriceMin, selectedProduct.purchasePriceMax, "¥") }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ selectedProduct.category || "-" }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ selectedProduct.productType || "-" }}</el-descriptions-item>
          </el-descriptions>

          <div class="drawer-section">
            <h4>标签</h4>
            <div class="tag-row">
              <el-tag v-for="tag in selectedProduct.tagCodeList || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              <span v-if="!selectedProduct.tagCodeList?.length" class="muted">暂无标签</span>
            </div>
          </div>

          <div class="drawer-section diagnosis">
            <h4>同步诊断</h4>
            <el-alert :type="getSyncAlertType(selectedProduct)" :closable="false" show-icon>
              <template #title>{{ getSyncState(selectedProduct).label }}</template>
              <template #default>
                <div>{{ getSyncState(selectedProduct).description }}</div>
                <div v-if="selectedProduct.latestTaskError || selectedProduct.syncMessage" class="error-detail">
                  {{ selectedProduct.latestTaskError || selectedProduct.syncMessage }}
                </div>
              </template>
            </el-alert>
            <div class="missing-tags drawer-missing">
              <el-tag v-for="label in formatMissingFields(selectedProduct.missingFields)" :key="label" size="small" type="warning" effect="plain">
                {{ label }}
              </el-tag>
              <el-tag v-if="!formatMissingFields(selectedProduct.missingFields).length" size="small" type="success" effect="plain">资料完整</el-tag>
            </div>
          </div>

          <div class="drawer-section">
            <h4>变体预览</h4>
            <el-table :data="selectedProduct.productVariantList || []" size="small" max-height="220">
              <el-table-column label="SKU" prop="sku" min-width="130" show-overflow-tooltip />
              <el-table-column label="售价" width="90">
                <template #default="{ row }">{{ formatCents(row.price) }}</template>
              </el-table-column>
              <el-table-column label="采购" width="90">
                <template #default="{ row }">{{ formatCents(row.purchasePrice, "¥") }}</template>
              </el-table-column>
              <el-table-column label="运费" width="80">
                <template #default="{ row }">{{ formatCents(row.freight, "¥") }}</template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="drawer-actions">
          <el-button type="primary" @click="handleUpdate(selectedProduct)">编辑</el-button>
          <el-button type="warning" @click="handleRetry(selectedProduct)">同步/重试</el-button>
          <el-button :disabled="!selectedProduct.latestTaskId" @click="openTask(selectedProduct)">查看任务</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog :title="storeAction.mode === 'publish' ? '批量发布渠道' : '推送到 Shopify'" v-model="storeAction.open" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="店铺">
          <el-select
            v-model="storeAction.storeId"
            filterable
            placeholder="请选择店铺"
            style="width: 100%"
            :loading="storeAction.loading"
          >
            <el-option
              v-for="store in activeStores"
              :key="store.storeId"
              :label="store.isDefault === '1' ? `${store.storeName}（默认）` : store.storeName"
              :value="store.storeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <span>{{ storeAction.desc }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="storeAction.submitting" @click="submitStoreAction">确定</el-button>
          <el-button @click="storeAction.open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <el-checkbox v-model="upload.updateSupport" /> 是否更新已经存在的商品数据
            <div>仅允许导入 xls、xlsx 格式文件。</div>
            <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">
              下载模板
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确定</el-button>
          <el-button @click="upload.open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref, toRefs, watch } from "vue";
import { ElMessageBox } from "element-plus";
import { ArrowDown, ArrowUp, UploadFilled } from "@element-plus/icons-vue";
import { delProduct, getProductWorkbenchSummary, listProduct, publishChannels, pushBatch } from "@/api/erp/product";
import type { ProductQuery } from "@/api/erp/product";
import { listActiveStores } from "@/api/erp/store";
import { getToken } from "@/utils/auth";
import { parseTime } from "@/utils/ruoyi";
import {
  formatCents,
  formatCentsRange,
  formatMissingFields,
  getMissingFieldSeverity,
  getProductSyncState,
  defaultRequiredMissingFields,
  splitFieldCodes,
} from "@/utils/erp/productWorkbench";
import type { Product, ProductWorkbenchSummary, ShopifyStore } from "@/types/erp";

interface UploadConfig {
  open: boolean;
  title: string;
  isUploading: boolean;
  updateSupport: boolean;
  headers: { Authorization: string };
  url: string;
  templateUrl: string;
}

interface SelectedTag {
  tagId: number;
  tagName: string;
  tagCode?: string;
  tagType?: string;
}

interface ColumnConfig {
  key: string;
  label: string;
  visible: boolean;
}

interface SummaryCard {
  key: string;
  label: string;
  value: number;
  syncStatus?: string | null;
  qualityState?: string | null;
  active: boolean;
}

const emit = defineEmits<{
  (e: "remove-tag", tag: SelectedTag): void;
}>();

const props = defineProps<{
  selectedTags?: SelectedTag[];
}>();

const { proxy } = getCurrentInstance() as any;
const { erp_product_sync_status, erp_product_status } = proxy.useDict("erp_product_sync_status", "erp_product_status");

const productList = ref<Product[]>([]);
const loading = ref(true);
const advancedVisible = ref(false);
const ids = ref<number[]>([]);
const selectedRows = ref<Product[]>([]);
const total = ref(0);
const daterangeLastSyncTime = ref<string[]>([]);
const activeStores = ref<ShopifyStore[]>([]);
const quickViewOpen = ref(false);
const selectedProduct = ref<Product | null>(null);
const checkedProductIds = ref<Set<number>>(new Set());
const uploadRef = ref<any>();

const baseUrl = import.meta.env.VITE_APP_BASE_API;

const storeAction = reactive({
  open: false,
  desc: "",
  mode: "sync" as "sync" | "publish",
  storeId: undefined as number | undefined,
  targetIds: [] as number[],
  loading: false,
  submitting: false,
});

const summary = reactive<ProductWorkbenchSummary>({
  totalCount: 0,
  pendingPushCount: 0,
  syncFailedCount: 0,
  syncingCount: 0,
  syncedCount: 0,
  needResyncCount: 0,
  incompleteCount: 0,
});

function createDefaultQuery(): ProductQuery {
  return {
    pageNum: 1,
    pageSize: 10,
    searchKeyword: null,
    productTitle: null,
    spu: null,
    status: null,
    syncStatus: null,
    qualityState: null,
    storeId: null,
    tagIds: [],
    category: null,
    productType: null,
    sourceUrl: null,
    purchaseUrl: null,
    updatedBy: null,
    lastSyncTime: null,
    params: {},
  };
}

const data = reactive({
  exportUrl: "erp/product/export",
  upload: {
    open: false,
    title: "",
    isUploading: false,
    updateSupport: false,
    headers: { Authorization: "Bearer " + getToken() },
    url: import.meta.env.VITE_APP_BASE_API + "/erp/product/importData",
    templateUrl: "erp/product/importTemplate",
  } as UploadConfig,
  queryParams: createDefaultQuery(),
  columns: [
    { key: "price", label: "价格/变体", visible: true },
    { key: "quality", label: "资料完整度", visible: true },
    { key: "sync", label: "Shopify 同步", visible: true },
    { key: "editor", label: "最后编辑时间", visible: true },
  ] as ColumnConfig[],
});

const { queryParams, columns, exportUrl, upload } = toRefs(data);

const summaryCards = computed<SummaryCard[]>(() => [
  {
    key: "sync-0",
    label: "未同步",
    value: summary.pendingPushCount || 0,
    syncStatus: "0",
    qualityState: null,
    active: queryParams.value.syncStatus === "0",
  },
  {
    key: "sync-1",
    label: "同步成功",
    value: summary.syncedCount || 0,
    syncStatus: "1",
    qualityState: null,
    active: queryParams.value.syncStatus === "1",
  },
  {
    key: "sync-2",
    label: "同步失败",
    value: summary.syncFailedCount || 0,
    syncStatus: "2",
    qualityState: null,
    active: queryParams.value.syncStatus === "2",
  },
  {
    key: "sync-3",
    label: "同步中",
    value: summary.syncingCount || 0,
    syncStatus: "3",
    qualityState: null,
    active: queryParams.value.syncStatus === "3",
  },
  {
    key: "incomplete",
    label: "资料不完整",
    value: summary.incompleteCount || 0,
    syncStatus: null,
    qualityState: "incomplete",
    active: queryParams.value.qualityState === "incomplete",
  },
]);

const selectedStore = computed(() => activeStores.value.find((store) => store.storeId === queryParams.value.storeId));

const requiredMissingFields = computed(() => {
  const fields = splitFieldCodes(selectedStore.value?.requiredProductFields);
  return fields.length ? fields : defaultRequiredMissingFields;
});

function buildQuery(includePage = true): ProductQuery {
  const selectedTagIds = props.selectedTags?.map((tag) => tag.tagId) || [];
  const query: ProductQuery = {
    ...queryParams.value,
    tagIds: selectedTagIds,
    params: {},
  };

  if (!includePage) {
    delete query.pageNum;
    delete query.pageSize;
  }

  if (daterangeLastSyncTime.value?.length === 2) {
    query.params = {
      beginLastSyncTime: daterangeLastSyncTime.value[0],
      endLastSyncTime: daterangeLastSyncTime.value[1],
    };
  }
  return query;
}

function getList(): void {
  if (activeStores.value.length && !queryParams.value.storeId) {
    proxy.$modal.msgWarning("请选择店铺");
    return;
  }
  loading.value = true;
  listProduct(buildQuery())
    .then((response: any) => {
      productList.value = response.rows || response.data || [];
      total.value = normalizeTotal(response.total, productList.value.length);
    })
    .finally(() => {
      loading.value = false;
    });
  loadSummary();
}

function loadSummary(): void {
  const summaryQuery = buildQuery(false);
  summaryQuery.syncStatus = null;
  summaryQuery.qualityState = null;
  getProductWorkbenchSummary(summaryQuery).then((response: any) => {
    Object.assign(summary, response.data || {});
  });
}

function handleQuery(): void {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery(): void {
  daterangeLastSyncTime.value = [];
  Object.assign(queryParams.value, createDefaultQuery());
  const defaultStore = activeStores.value.find((store) => store.isDefault === "1") || activeStores.value[0];
  queryParams.value.storeId = defaultStore?.storeId || null;
  getList();
}

function applySummaryFilter(item: SummaryCard): void {
  const syncStatus = item.syncStatus || null;
  const qualityState = item.qualityState || null;
  const isSameFilter =
    queryParams.value.syncStatus === syncStatus &&
    queryParams.value.qualityState === qualityState;
  queryParams.value.pageNum = 1;
  queryParams.value.syncStatus = isSameFilter ? null : syncStatus;
  queryParams.value.qualityState = isSameFilter ? null : qualityState;
  getList();
}

function handleSelectionChange(selection: Product[]): void {
  selectedRows.value = selection;
  ids.value = selection.map((item) => item.productId);
}

function noop(): void {}

function handleRemoveTag(tagToRemove: SelectedTag): void {
  emit("remove-tag", tagToRemove);
}

function handleToolbarCommand(command: string): void {
  if (command === "export") {
    handleExport();
  }
  if (command === "refresh") {
    getList();
  }
}

function hasPermi(permission: string): boolean {
  return proxy.$auth.hasPermi(permission);
}

function handleBatchCommand(command: string): void {
  if (command === "delete") {
    handleDelete();
  }
}

function isColumnVisible(key: string): boolean {
  return columns.value.find((column) => column.key === key)?.visible !== false;
}

function getProductTitle(row: Product): string {
  return row.productTitle || row.productName || "未命名商品";
}

/**
 * 将媒体地址转换为可访问地址，完整外链直接使用。
 */
function resolveMediaUrl(url?: string | null): string {
  if (!url) {
    return "";
  }
  if (/^(https?:)?\/\//i.test(url) || /^(data|blob):/i.test(url)) {
    return url;
  }
  return baseUrl + url;
}

/**
 * 规范化分页总数，保留后端返回的 0，避免空列表时误显示当前页条数。
 */
function normalizeTotal(value: unknown, fallback = 0): number {
  const totalValue = value ?? fallback;
  const parsedTotal = Number(totalValue);
  return Number.isFinite(parsedTotal) ? parsedTotal : fallback;
}

/**
 * 读取媒体优先展示地址，NAS 为空时回退到 Shopify。
 */
function getMediaSource(media?: NonNullable<Product["mediaList"]>[number] | null): string {
  return media?.nasMediaUrl || media?.shopifyMediaUrl || media?.transcodedMediaUrl || "";
}

/**
 * 获取商品列表主图地址。
 */
function getMainImage(row: Product): string {
  const mainMedia = row.mediaList?.find((media) => media.mediaId === row.mainMediaId);
  const source =
    row.mainMediaUrl ||
    getMediaSource(mainMedia) ||
    row.mediaUrlList?.find(Boolean) ||
    getMediaSource(row.mediaList?.[0]);
  return resolveMediaUrl(source);
}

/**
 * 获取商品速览图集地址。
 */
function getMediaUrls(row: Product): string[] {
  const sources = row.mediaList?.length
    ? row.mediaList.map(getMediaSource)
    : row.mediaUrlList || [];
  return sources.map(resolveMediaUrl).filter(Boolean);
}

function getSyncState(row: Product) {
  return getProductSyncState(row);
}

function getRowClassName({ row }: { row: Product }): string {
  return getSyncState(row).className;
}

function getQualityTitle(row: Product): string {
  const labels = formatMissingFields(row.missingFields);
  return labels.length ? `缺 ${labels.length} 项` : "资料完整";
}

function getQualityTagType(row: Product): "success" | "warning" | "danger" {
  return getMissingFieldSeverity(row.missingFields, requiredMissingFields.value);
}

function getSyncAlertType(row: Product): "success" | "warning" | "error" | "info" {
  const key = getSyncState(row).key;
  if (key === "failed") return "error";
  if (key === "synced") return "success";
  if (key === "need_resync" || key === "running" || key === "partial") return "warning";
  return "info";
}

function formatDate(value?: string | Date | null): string {
  return value ? parseTime(value, "{y}-{m}-{d} {h}:{i}") || "-" : "-";
}

function shortId(value?: string): string {
  if (!value) {
    return "-";
  }
  const parts = value.split("/");
  return parts[parts.length - 1] || value;
}

function isChecked(row: Product): boolean {
  return checkedProductIds.value.has(row.productId);
}

function openQuickView(row: Product): void {
  selectedProduct.value = row;
  quickViewOpen.value = true;
}

function openExternal(url?: string): void {
  if (!url) {
    proxy.$modal.msgWarning("暂无链接");
    return;
  }
  window.open(url, "_blank", "noopener,noreferrer");
}

function copyText(text?: string): void {
  if (!text) {
    proxy.$modal.msgWarning("暂无可复制内容");
    return;
  }
  if (!navigator.clipboard?.writeText) {
    proxy.$modal.msgWarning("当前浏览器不支持一键复制");
    return;
  }
  navigator.clipboard.writeText(text).then(() => {
    proxy.$modal.msgSuccess("复制成功");
  }).catch(() => {
    proxy.$modal.msgError("复制失败，请手动复制");
  });
}

function handleRowCommand(command: string, row: Product): void {
  if (command === "purchase") openExternal(row.purchaseUrl);
  if (command === "source") openExternal(row.sourceUrl);
  if (command === "task") openTask(row);
  if (command === "copyShopify") copyText(row.shopifyProductId);
  if (command === "delete") handleDelete(row);
}

function handleAdd(): void {
  const selectedTagIds = props.selectedTags?.map((tag) => tag.tagId) || [];
  proxy.$tab.openPage("新增选品", "/erp/product/creation-wizard/create", {
    tagIds: JSON.stringify(selectedTagIds),
  });
}

function handleUpdate(row?: Product): void {
  const productId = row?.productId || ids.value[0];
  if (!productId) {
    proxy.$modal.msgWarning("请选择商品");
    return;
  }
  proxy.$tab.openPage("编辑商品", "/erp/product/creation-wizard/edit/" + productId, { step: 1 });
}

function handleDelete(row?: Product): void {
  const productIds = row?.productId || ids.value;
  if (!productIds || (Array.isArray(productIds) && productIds.length === 0)) {
    proxy.$modal.msgWarning("请选择商品");
    return;
  }
  proxy.$modal
    .confirm(`是否确认删除商品编号为 "${productIds}" 的数据项？`)
    .then(() => delProduct(productIds as any))
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function handleExport(): void {
  proxy.download(exportUrl.value, buildQuery(false), `product_${new Date().getTime()}.xlsx`);
}

function handleImport(): void {
  upload.value.title = "商品导入";
  upload.value.open = true;
}

function importTemplate(): void {
  proxy.download(upload.value.templateUrl, {}, `product_template_${new Date().getTime()}.xlsx`);
}

function loadActiveStores(): Promise<void> {
  storeAction.loading = true;
  return listActiveStores()
    .then((res: any) => {
      activeStores.value = res.data || [];
      const defaultStore = activeStores.value.find((store) => store.isDefault === "1") || activeStores.value[0];
      if (!queryParams.value.storeId || !activeStores.value.some((store) => store.storeId === queryParams.value.storeId)) {
        queryParams.value.storeId = defaultStore?.storeId || null;
      }
      if (!storeAction.storeId || !activeStores.value.some((store) => store.storeId === storeAction.storeId)) {
        storeAction.storeId = defaultStore?.storeId;
      }
    })
    .finally(() => {
      storeAction.loading = false;
    });
}

function openStoreAction(mode: "sync" | "publish", desc: string, targetIds: number[] = []): void {
  storeAction.mode = mode;
  storeAction.desc = desc;
  storeAction.targetIds = [...targetIds];
  loadActiveStores().then(() => {
    if (!activeStores.value.length) {
      proxy.$modal.msgWarning("请先配置并启用 Shopify 店铺");
      return;
    }
    storeAction.open = true;
  });
}

function submitStoreAction(): void {
  if (!storeAction.storeId) {
    proxy.$modal.msgWarning("请选择店铺");
    return;
  }

  storeAction.submitting = true;
  const targetIds = storeAction.targetIds.length ? storeAction.targetIds : ids.value;
  const request =
    storeAction.mode === "sync" && targetIds.length === 0
      ? { productQuery: buildQuery(false), storeId: storeAction.storeId }
      : { productIds: targetIds, storeId: storeAction.storeId };

  const action = storeAction.mode === "publish" ? publishChannels(request) : pushBatch(request);
  action
    .then((res: any) => {
      storeAction.open = false;
      if (storeAction.mode === "publish") {
        const result = res.data || {};
        proxy.$modal.msgSuccess(`发布完成：成功 ${result.successCount || 0}，失败 ${result.failedCount || 0}`);
        return;
      }
      const taskId = res.data?.taskId || res.taskId;
      if (taskId) {
        proxy.$modal.msgSuccess("已创建推送任务，任务 ID: " + taskId);
        openTaskProgress(taskId);
      } else {
        proxy.$modal.msgWarning("没有可推送的商品");
      }
    })
    .finally(() => {
      storeAction.submitting = false;
    });
}

function handlePushToShopify(): void {
  const isSelected = ids.value.length > 0;
  const count = isSelected ? ids.value.length : total.value;
  if (count === 0) {
    proxy.$modal.msgWarning("没有可推送的商品");
    return;
  }
  openStoreAction("sync", isSelected ? `已勾选的 ${count} 个商品` : `当前筛选的 ${count} 个商品`, isSelected ? ids.value : []);
}


function handleRetry(row: Product): void {
  openStoreAction("sync", `商品 ${row.spu || row.productId}`, [row.productId]);
}

function handleMarkChecked(): void {
  checkedProductIds.value = new Set([...checkedProductIds.value, ...ids.value]);
  proxy.$modal.msgSuccess(`已标记 ${ids.value.length} 个商品为已检查`);
}

function openTask(product: Product): void {
  if (!product.latestTaskId) {
    proxy.$modal.msgWarning("暂无同步任务");
    return;
  }
  proxy.$tab.openPage("Shopify 同步任务", "/erp/task", { taskId: product.latestTaskId });
}

function openTaskProgress(taskId: number): void {
  ElMessageBox.confirm(`推送任务已创建，任务 ID：${taskId}。可前往任务页面查看进度。`, "推送任务已创建", {
    confirmButtonText: "打开任务页面",
    cancelButtonText: "稍后查看",
    type: "info",
  })
    .then(() => {
      proxy.$tab.openPage("Shopify 同步任务", "/erp/task", { taskId });
    })
    .catch(() => {});
}

function handleFileUploadProgress(): void {
  upload.value.isUploading = true;
}

function handleFileSuccess(response: any): void {
  upload.value.open = false;
  upload.value.isUploading = false;
  uploadRef.value?.clearFiles();
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>",
    "导入结果",
    { dangerouslyUseHTMLString: true },
  );
  getList();
}

function submitFileForm(): void {
  uploadRef.value?.submit();
}

watch(
  () => props.selectedTags,
  () => {
    queryParams.value.pageNum = 1;
    getList();
  },
  { deep: true },
);

onMounted(() => {
  loadActiveStores().then(() => {
    getList();
  });
});

defineExpose({
  refresh: getList,
});
</script>

<style scoped lang="scss">
.product-workbench {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  padding: 0;
  overflow: hidden;
  box-sizing: border-box;
  background: #fff;
}

.workbench-header,
.batch-bar,
.selected-tags {
  background: #fff;
  border: 0;
  border-radius: 0;
}

.workbench-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
  padding: 10px 12px 8px;
  margin-bottom: 0;
  border-bottom: 1px solid #e6eaf0;
}

.toolbar-primary {
  display: block;
  min-width: 0;
}

.toolbar-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding-top: 7px;
  border-top: 1px solid #eef1f5;
}

.summary-grid {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.summary-card {
  display: inline-flex;
  min-height: 30px;
  padding: 0 10px;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d9e0ea;
  border-radius: 6px;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.summary-card:hover,
.summary-card.active {
  background: #eef6ff;
  border-color: #409eff;
}

.summary-card.active {
  box-shadow: inset 0 0 0 1px rgb(64 158 255 / 18%);
}

.summary-card strong {
  display: inline-block;
  min-width: 18px;
  font-size: 14px;
  line-height: 1;
  color: #1d4ed8;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.summary-label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.muted,
.tag-label {
  font-size: 12px;
  color: #667085;
}

.toolbar-row {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-start;
  flex: 0 0 auto;
}

.workbench-header :deep(.el-button) {
  min-height: 30px;
  padding: 0 10px;
}

.workbench-header :deep(.el-input__wrapper),
.workbench-header :deep(.el-select__wrapper) {
  min-height: 30px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
  align-items: center;
}

.toolbar-filter-form {
  min-width: 0;
}

.keyword-filter-item :deep(.el-form-item__content) {
  width: 218px;
  flex: 0 0 218px;
}

.keyword-filter-item :deep(.el-input) {
  width: 218px !important;
}

.filter-form :deep(.el-form-item) {
  margin-right: 4px;
  margin-bottom: 0;
}

.filter-form :deep(.el-form-item__label),
.filter-form :deep(.el-form-item__content) {
  line-height: 30px;
}

.advanced-form {
  padding: 8px 0 2px;
  margin-top: 0;
  border-top: 1px dashed #d9e0ea;
}

.advanced-form :deep(.el-form-item__content) {
  width: 218px;
  flex: 0 0 218px;
}

.advanced-form :deep(.el-input),
.advanced-form :deep(.el-select) {
  width: 218px !important;
}

.filter-actions {
  margin-left: 30px;
}

.w-120 { width: 120px; }
.w-130 { width: 130px; }
.w-140 { width: 140px; }
.w-150 { width: 150px; }
.w-160 { width: 160px; }
.w-170 { width: 170px; }
.w-180 { width: 180px; }
.w-218 { width: 218px; }
.w-220 { width: 220px; }
.w-240 { width: 240px; }
.w-260 { width: 260px; }

.selected-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 10px;
  margin-bottom: 0;
  border-bottom: 1px solid #e6eaf0;
}

.batch-bar {
  position: sticky;
  top: 0;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  margin-bottom: 0;
  border-bottom: 1px solid #e6eaf0;
  box-shadow: none;
}

.table-zone {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.workbench-table {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  border: 0;
  border-radius: 0;
  overflow: hidden;
}

.workbench-table :deep(.el-table__cell) {
  padding: 4px 0;
}

.workbench-table :deep(.el-table__header .el-table__cell) {
  padding: 6px 0;
}

.workbench-table :deep(.cell) {
  line-height: 18px;
}

.product-cell {
  display: flex;
  gap: 8px;
  align-items: center;
  min-height: 58px;
}

.product-thumb,
.image-empty {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #eef1f5;
  flex: 0 0 48px;
}

.image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8a94a6;
  font-size: 12px;
}

.product-meta {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.title-link {
  max-width: 250px;
  padding: 0;
  overflow: hidden;
  font-weight: 600;
  line-height: 18px;
  color: #172033;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.title-link:hover {
  color: #2f6fed;
}

.subline,
.tag-row,
.sync-detail,
.link-actions,
.missing-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  align-items: center;
}

.spu-inline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.copy-icon-btn {
  width: 20px;
  height: 20px;
  padding: 0;
}

.subline,
.sync-detail {
  margin-top: 3px;
  font-size: 12px;
  color: #667085;
}

.tag-row {
  margin-top: 3px;
}

.metric-stack,
.quality-stack,
.sync-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  line-height: 1.25;
}

.metric-stack strong {
  line-height: 18px;
}

.metric-stack span {
  font-size: 12px;
  line-height: 16px;
  color: #667085;
}

.sync-title {
  display: flex;
  gap: 6px;
  align-items: center;
}

.error-line {
  max-width: 220px;
  overflow: hidden;
  font-size: 12px;
  color: #c0362c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  gap: 2px;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  height: 24px;
  padding: 0 2px;
  margin-left: 0;
}

.pagination-wrapper {
  position: sticky;
  bottom: 0;
  z-index: 4;
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  padding: 6px 12px 0;
  margin-top: 0;
  background: #fff;
  border-top: 1px solid #e6eaf0;
}

.pagination-wrapper :deep(.pagination-container) {
  padding: 0;
  margin: 0;
  background: transparent;
}

:deep(.el-tag.el-tag--small) {
  height: 20px;
  padding: 0 6px;
  line-height: 18px;
}

.column-settings {
  display: grid;
  gap: 6px;
}

:global(.quick-view-drawer.el-drawer) {
  display: flex;
  flex-direction: column;
}

:global(.quick-view-drawer .el-drawer__header) {
  flex: 0 0 auto;
  margin-bottom: 0;
  padding: 16px 20px;
  border-bottom: 1px solid #edf0f5;
}

:global(.quick-view-drawer .el-drawer__body) {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

.drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  padding: 16px 20px 84px;
  overflow-y: auto;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.drawer-section {
  margin-bottom: 18px;
}

.drawer-section h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: #1f2937;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.image-grid :deep(.el-image) {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  background: #eef1f5;
}

.drawer-empty {
  grid-column: 1 / -1;
  padding: 28px;
  text-align: center;
  color: #8a94a6;
  background: #f5f7fa;
  border-radius: 6px;
}

.diagnosis :deep(.el-alert__content) {
  width: 100%;
}

.error-detail {
  margin-top: 6px;
  color: #c0362c;
}

.drawer-missing {
  margin-top: 10px;
}

.drawer-actions {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2;
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  padding: 12px 20px;
  background: #fff;
  border-top: 1px solid #edf0f5;
  box-shadow: 0 -6px 16px rgb(15 23 42 / 6%);
}

:deep(.el-table__row.is-failed) {
  --el-table-tr-bg-color: #fff8f7;
}

:deep(.el-table__row.is-partial) {
  --el-table-tr-bg-color: #fffaf0;
}

:deep(.el-table__row.is-resync) {
  --el-table-tr-bg-color: #fffaf0;
}

@media (max-width: 1180px) {
  .toolbar-status-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-row {
    justify-content: flex-start;
    min-width: 0;
  }

  .summary-grid {
    justify-content: flex-start;
  }

  .toolbar-filter-form {
    flex-basis: 100%;
    min-width: 0;
  }
}
</style>
