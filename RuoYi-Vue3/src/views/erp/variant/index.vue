<template>
  <div class="app-container sku-console">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="78px" class="sku-filter">
      <el-form-item label="店铺" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="全部店铺" clearable filterable class="w-180" @change="handleQuery">
          <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName || store.shopName" :value="store.storeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品" prop="productKeyword">
        <el-input v-model="queryParams.productKeyword" placeholder="标题/SPU/ID" clearable class="w-180" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="SKU" prop="sku">
        <el-input v-model="queryParams.sku" placeholder="SKU" clearable class="w-170" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Shopify" prop="shopifyVariantId">
        <el-input v-model="queryParams.shopifyVariantId" placeholder="Variant ID" clearable class="w-180" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="同步" prop="syncStatus">
        <el-select v-model="queryParams.syncStatus" placeholder="全部" clearable class="w-130" @change="handleQuery">
          <el-option label="待同步" value="0" />
          <el-option label="成功" value="1" />
          <el-option label="失败" value="2" />
          <el-option label="同步中" value="3" />
          <el-option label="部分成功" value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="可售" prop="isActiveAvailable">
        <el-select v-model="queryParams.isActiveAvailable" placeholder="全部" clearable class="w-110" @change="handleQuery">
          <el-option label="可售" value="1" />
          <el-option label="停用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="w-240"
        />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="queryParams.purchaseUrlMissing" @change="handleQuery">缺采购链接</el-checkbox>
        <el-checkbox v-model="queryParams.lowProfitOnly" @change="handleQuery">低毛利</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="metric-grid">
      <button type="button" class="metric" @click="clearQuickFilters">
        <span>SKU 总数</span>
        <strong>{{ summary.totalCount || 0 }}</strong>
      </button>
      <button type="button" class="metric warning" @click="applySyncFilter">
        <span>待同步</span>
        <strong>{{ summary.needSyncCount || 0 }}</strong>
      </button>
      <button type="button" class="metric danger" @click="applyLowProfitFilter">
        <span>低毛利</span>
        <strong>{{ summary.lowProfitCount || 0 }}</strong>
      </button>
      <button type="button" class="metric danger" @click="applyMissingPurchaseFilter">
        <span>缺采购链接</span>
        <strong>{{ summary.missingPurchaseUrlCount || 0 }}</strong>
      </button>
      <button type="button" class="metric success">
        <span>近30天有单</span>
        <strong>{{ summary.orderedSkuCount30d || 0 }}</strong>
      </button>
    </section>

    <el-row :gutter="10" class="mb8 toolbar-row">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Edit" :disabled="multiple" @click="openBatchEdit" v-hasPermi="['erp:variant:edit']">批量编辑</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Upload" @click="handleImport" v-hasPermi="['erp:variant:import']">导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:variant:export']">导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="variantList"
      border
      row-key="variantId"
      height="640"
      :header-cell-style="{ background: '#f7f8fb', color: '#303133' }"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" align="center" fixed />
      <el-table-column label="SKU" min-width="270" fixed>
        <template #default="{ row }">
          <div class="sku-cell">
            <el-image class="sku-thumb" :src="row.mediaUrl || row.mainMediaUrl" fit="cover">
              <template #error>
                <div class="sku-thumb-empty">IMG</div>
              </template>
            </el-image>
            <div class="sku-main">
              <button type="button" class="sku-code" @click="openDetail(row)">{{ row.sku || '未填写 SKU' }}</button>
              <div class="sku-subline">{{ row.productTitle || '-' }}</div>
              <div class="sku-tags">
                <el-tag v-if="row.spu" size="small" effect="plain">SPU {{ row.spu }}</el-tag>
                <el-tag size="small" :type="activeTag(row.isActiveAvailable)" effect="light">{{ activeLabel(row.isActiveAvailable) }}</el-tag>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="店铺/Shopify" min-width="210" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ row.shopName || '-' }}</div>
          <div class="muted">{{ shortId(row.shopifyVariantId) || '未绑定 Variant ID' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="选项" prop="optionValues" min-width="180" show-overflow-tooltip />
      <el-table-column label="价格" width="150" align="right">
        <template #default="{ row }">
          <div>{{ money(row.price) }}</div>
          <div class="muted">对比 {{ money(row.compareAtPrice) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="成本" width="155" align="right">
        <template #default="{ row }">
          <div>采购 {{ money(row.purchasePrice) }}</div>
          <div class="muted">运费 {{ money(row.freight) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="毛利" width="145" align="right">
        <template #default="{ row }">
          <div :class="{ danger: Number(row.profit || 0) < 0 }">{{ money(row.profit) }}</div>
          <div :class="{ danger: Number(row.profitRate || 0) < 0.15 }">{{ percent(row.profitRate) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="包裹" width="160">
        <template #default="{ row }">
          <div>{{ packageSize(row) }}</div>
          <div class="muted">重 {{ row.pkWeight ?? '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="近30天表现" width="180" align="right">
        <template #default="{ row }">
          <div>订单 {{ row.orderCount30d || 0 }} / 销售 {{ money(row.salesAmount30d) }}</div>
          <div class="muted">退款 {{ money(row.refundAmount30d) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="采购/履约" width="145" align="center">
        <template #default="{ row }">
          <el-tag :type="Number(row.pendingPurchaseCount || 0) > 0 ? 'warning' : 'success'" effect="light">待采购 {{ row.pendingPurchaseCount || 0 }}</el-tag>
          <el-tag class="mt4" :type="Number(row.fulfillmentExceptionCount || 0) > 0 ? 'danger' : 'info'" effect="plain">履约异常 {{ row.fulfillmentExceptionCount || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="健康检查" min-width="190">
        <template #default="{ row }">
          <div class="flag-list">
            <el-tag v-for="flag in missingFlags(row)" :key="flag" size="small" :type="flagTag(flag)" effect="light">{{ flagLabel(flag) }}</el-tag>
            <el-tag v-if="!missingFlags(row).length" size="small" type="success" effect="light">资料完整</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="同步" width="150" align="center">
        <template #default="{ row }">
          <el-tag :type="syncTag(row.productSyncStatus)" effect="light">{{ syncLabel(row.productSyncStatus) }}</el-tag>
          <div class="muted">{{ formatTime(row.lastShopifyImportTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="采购链接" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.purchaseUrl" type="primary" :href="row.purchaseUrl" target="_blank">打开来源</el-link>
          <span v-else class="danger">缺失</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="openQuickEdit(row)" v-hasPermi="['erp:variant:edit']">快编</el-button>
          <el-button link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
          <el-dropdown trigger="click">
            <el-button link type="primary">跳转<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProduct(row)">父商品</el-dropdown-item>
                <el-dropdown-item @click="goOrders(row)">订单中心</el-dropdown-item>
                <el-dropdown-item @click="goPurchase(row)">采购任务</el-dropdown-item>
                <el-dropdown-item @click="goTasks(row)">任务诊断</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="quickEditOpen" title="SKU 快速编辑" width="680px" append-to-body :close-on-click-modal="!saveLoading">
      <el-form ref="quickEditRef" :model="quickForm" :rules="quickRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="SKU" prop="sku">
              <el-input v-model="quickForm.sku" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否可售">
              <el-switch v-model="quickForm.isActiveAvailable" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="售价(分)" prop="price">
              <el-input-number v-model="quickForm.price" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="对比价(分)">
              <el-input-number v-model="quickForm.compareAtPrice" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购价(分)">
              <el-input-number v-model="quickForm.purchasePrice" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="采购链接">
              <el-input v-model="quickForm.purchaseUrl" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="宽">
              <el-input-number v-model="quickForm.pkWidth" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高">
              <el-input-number v-model="quickForm.pkHeight" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="长">
              <el-input-number v-model="quickForm.pkLength" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重量">
              <el-input-number v-model="quickForm.pkWeight" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运费(分)">
              <el-input-number v-model="quickForm.freight" :min="0" :precision="0" controls-position="right" class="full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="实际运费">
              <el-switch v-model="quickForm.isActualShipment" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="quickEditOpen = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="submitQuickEdit">保存并标记待同步</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchOpen" title="批量编辑 SKU" width="640px" append-to-body :close-on-click-modal="!saveLoading">
      <el-alert type="info" show-icon :closable="false" class="mb16" :title="`已选择 ${selectedIds.length} 个 SKU；留空的字段不会更新。`" />
      <el-form :model="batchForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="售价(分)"><el-input-number v-model="batchForm.price" :min="0" :precision="0" controls-position="right" class="full" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="对比价(分)"><el-input-number v-model="batchForm.compareAtPrice" :min="0" :precision="0" controls-position="right" class="full" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="采购价(分)"><el-input-number v-model="batchForm.purchasePrice" :min="0" :precision="0" controls-position="right" class="full" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="运费(分)"><el-input-number v-model="batchForm.freight" :min="0" :precision="0" controls-position="right" class="full" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="是否可售"><el-select v-model="batchForm.isActiveAvailable" clearable class="full"><el-option label="可售" value="1" /><el-option label="停用" value="0" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="重量"><el-input-number v-model="batchForm.pkWeight" :min="0" :precision="0" controls-position="right" class="full" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="采购链接"><el-input v-model="batchForm.purchaseUrl" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="batchForm.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="batchOpen = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="submitBatchEdit">批量保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="SKU 详情" size="720px">
      <template v-if="currentSku">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="SKU">{{ currentSku.sku || '-' }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ currentSku.shopName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="商品">{{ currentSku.productTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="SPU">{{ currentSku.spu || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Shopify Variant" :span="2">{{ currentSku.shopifyVariantId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="采购链接" :span="2">
            <el-link v-if="currentSku.purchaseUrl" type="primary" :href="currentSku.purchaseUrl" target="_blank">{{ currentSku.purchaseUrl }}</el-link>
            <span v-else class="danger">未填写</span>
          </el-descriptions-item>
          <el-descriptions-item label="售价">{{ money(currentSku.price) }}</el-descriptions-item>
          <el-descriptions-item label="毛利率">{{ percent(currentSku.profitRate) }}</el-descriptions-item>
          <el-descriptions-item label="近30天订单">{{ currentSku.orderCount30d || 0 }}</el-descriptions-item>
          <el-descriptions-item label="待采购">{{ currentSku.pendingPurchaseCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="健康检查" :span="2">
            <div class="flag-list">
              <el-tag v-for="flag in missingFlags(currentSku)" :key="flag" :type="flagTag(flag)" effect="light">{{ flagLabel(flag) }}</el-tag>
              <el-tag v-if="!missingFlags(currentSku).length" type="success" effect="light">资料完整</el-tag>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog :title="upload.title" v-model="upload.open" width="420px" append-to-body>
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
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <el-checkbox v-model="upload.updateSupport" /> 更新已存在 SKU
            <el-link type="primary" :underline="false" class="ml8" @click="importTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button type="primary" @click="submitFileForm">确定</el-button>
        <el-button @click="upload.open = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, UploadFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadInstance } from 'element-plus'
import { batchUpdateVariant, getVariantSummary, importTemplateVariant, listVariant, updateVariant, type VariantBatchEdit, type VariantQuery, type VariantSummary } from '@/api/erp/variant'
import { listActiveStores } from '@/api/erp/store'
import { getToken } from '@/utils/auth'
import { parseTime } from '@/utils/ruoyi'
import type { ProductVariant, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any
const router = useRouter()

const loading = ref(false)
const saveLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const variantList = ref<ProductVariant[]>([])
const storeOptions = ref<ShopifyStore[]>([])
const selectedIds = ref<number[]>([])
const summary = ref<VariantSummary>({})
const dateRange = ref<string[]>([])
const quickEditOpen = ref(false)
const batchOpen = ref(false)
const detailOpen = ref(false)
const currentSku = ref<ProductVariant | null>(null)
const quickEditRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    productKeyword: undefined,
    sku: undefined,
    shopifyVariantId: undefined,
    syncStatus: undefined,
    isActiveAvailable: undefined,
    purchaseUrlMissing: false,
    lowProfitOnly: false,
    params: {}
  } as VariantQuery
})

const { queryParams } = toRefs(data)
const multiple = computed(() => selectedIds.value.length === 0)

const quickForm = reactive<Partial<ProductVariant>>({})
const batchForm = reactive<Partial<VariantBatchEdit>>({})

const quickRules: FormRules = {
  sku: [{ required: true, message: 'SKU 不能为空', trigger: 'blur' }],
  price: [{ required: true, message: '售价不能为空', trigger: 'blur' }]
}

const upload = reactive({
  open: false,
  title: 'SKU 导入',
  isUploading: false,
  updateSupport: false,
  headers: { Authorization: `Bearer ${getToken()}` },
  url: `${import.meta.env.VITE_APP_BASE_API}/erp/variant/importData`,
  templateUrl: 'erp/variant/importTemplate'
})

function applyDateRange(): VariantQuery {
  const params: Record<string, unknown> = {}
  if (dateRange.value?.length === 2) {
    params.beginCreateTime = dateRange.value[0]
    params.endCreateTime = dateRange.value[1]
  }
  return { ...queryParams.value, params }
}

function getList(): void {
  loading.value = true
  const params = applyDateRange()
  Promise.all([listVariant(params), getVariantSummary(params)]).then(([listRes, summaryRes]: any[]) => {
    variantList.value = listRes.rows || []
    total.value = listRes.total || 0
    summary.value = summaryRes.data || {}
  }).finally(() => {
    loading.value = false
  })
}

function refreshAll(): void {
  getList()
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery(): void {
  dateRange.value = []
  queryParams.value.purchaseUrlMissing = false
  queryParams.value.lowProfitOnly = false
  proxy.resetForm('queryRef')
  handleQuery()
}

function clearQuickFilters(): void {
  queryParams.value.syncStatus = undefined
  queryParams.value.purchaseUrlMissing = false
  queryParams.value.lowProfitOnly = false
  handleQuery()
}

function applySyncFilter(): void {
  queryParams.value.syncStatus = '0'
  handleQuery()
}

function applyLowProfitFilter(): void {
  queryParams.value.lowProfitOnly = true
  handleQuery()
}

function applyMissingPurchaseFilter(): void {
  queryParams.value.purchaseUrlMissing = true
  handleQuery()
}

function handleSelectionChange(selection: ProductVariant[]): void {
  selectedIds.value = selection.map((item) => item.variantId)
}

function openQuickEdit(row: ProductVariant): void {
  Object.assign(quickForm, row)
  currentSku.value = row
  quickEditOpen.value = true
}

function submitQuickEdit(): void {
  quickEditRef.value?.validate((valid) => {
    if (!valid) return
    saveLoading.value = true
    updateVariant(quickForm as ProductVariant).then(() => {
      proxy.$modal.msgSuccess('SKU 已保存，父商品已标记待同步')
      quickEditOpen.value = false
      getList()
    }).finally(() => {
      saveLoading.value = false
    })
  })
}

function openBatchEdit(): void {
  Object.keys(batchForm).forEach((key) => delete (batchForm as Record<string, unknown>)[key])
  batchOpen.value = true
}

function submitBatchEdit(): void {
  saveLoading.value = true
  batchUpdateVariant({ ...batchForm, variantIds: selectedIds.value } as VariantBatchEdit).then(() => {
    proxy.$modal.msgSuccess('批量更新完成，相关父商品已标记待同步')
    batchOpen.value = false
    getList()
  }).finally(() => {
    saveLoading.value = false
  })
}

function openDetail(row: ProductVariant): void {
  currentSku.value = row
  detailOpen.value = true
}

function handleExport(): void {
  proxy.download('erp/variant/export', applyDateRange(), `sku_${new Date().getTime()}.xlsx`)
}

function handleImport(): void {
  upload.title = 'SKU 导入'
  upload.open = true
}

function importTemplate(): void {
  proxy.download(upload.templateUrl, {}, `sku_template_${new Date().getTime()}.xlsx`)
}

function submitFileForm(): void {
  uploadRef.value?.submit()
}

function handleFileUploadProgress(): void {
  upload.isUploading = true
}

function handleFileSuccess(response: any): void {
  upload.open = false
  upload.isUploading = false
  uploadRef.value?.clearFiles()
  proxy.$alert(`<div style='overflow:auto;overflow-x:hidden;max-height:70vh;padding:10px 20px 0;'>${response.msg}</div>`, '导入结果', { dangerouslyUseHTMLString: true })
  getList()
}

function goProduct(row: ProductVariant): void {
  router.push({ path: '/erp/product', query: { productId: row.productId } })
}

function goOrders(row: ProductVariant): void {
  router.push({ path: '/erp/order', query: { sku: row.sku } })
}

function goPurchase(row: ProductVariant): void {
  router.push({ path: '/erp/purchase', query: { sku: row.sku } })
}

function goTasks(row: ProductVariant): void {
  router.push({ path: '/erp/task', query: { productId: row.productId } })
}

function money(value?: number | null): string {
  return ((Number(value || 0)) / 100).toFixed(2)
}

function percent(value?: number | null): string {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

function shortId(value?: string): string {
  if (!value) return ''
  const parts = value.split('/')
  return parts[parts.length - 1] || value
}

function packageSize(row: ProductVariant): string {
  return `${row.pkLength ?? '-'} x ${row.pkWidth ?? '-'} x ${row.pkHeight ?? '-'}`
}

function formatTime(value?: string | Date | null): string {
  return parseTime(value as any, '{y}-{m}-{d} {h}:{i}') || '-'
}

function missingFlags(row: ProductVariant): string[] {
  return (row.missingFlags || '').split(',').filter(Boolean)
}

function flagLabel(flag: string): string {
  const labels: Record<string, string> = {
    SKU_MISSING: '缺 SKU',
    PURCHASE_URL_MISSING: '缺采购链接',
    SHOPIFY_ID_MISSING: '未绑定 Shopify',
    PACKAGE_MISSING: '缺包裹',
    LOW_PROFIT: '低毛利'
  }
  return labels[flag] || flag
}

function flagTag(flag: string): 'success' | 'warning' | 'danger' | 'info' {
  if (flag === 'LOW_PROFIT' || flag === 'PURCHASE_URL_MISSING') return 'danger'
  if (flag === 'SHOPIFY_ID_MISSING' || flag === 'PACKAGE_MISSING') return 'warning'
  return 'info'
}

function syncLabel(status?: string): string {
  return ({ '0': '待同步', '1': '成功', '2': '失败', '3': '同步中', '4': '部分成功' } as Record<string, string>)[status || ''] || '待同步'
}

function syncTag(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (status === '1') return 'success'
  if (status === '2') return 'danger'
  if (status === '3') return 'primary'
  if (status === '4') return 'warning'
  return 'info'
}

function activeLabel(value?: string): string {
  return value === '0' ? '停用' : '可售'
}

function activeTag(value?: string): 'success' | 'info' {
  return value === '0' ? 'info' : 'success'
}

onMounted(() => {
  listActiveStores().then((res: any) => {
    storeOptions.value = res.data || []
  })
  getList()
})
</script>

<style scoped lang="scss">
.sku-console {
  .w-110 { width: 110px; }
  .w-130 { width: 130px; }
  .w-170 { width: 170px; }
  .w-180 { width: 180px; }
  .w-240 { width: 240px; }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 10px 0 14px;
}

.metric {
  text-align: left;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  padding: 12px 14px;
  cursor: pointer;

  span {
    display: block;
    color: #909399;
    font-size: 12px;
    margin-bottom: 5px;
  }

  strong {
    font-size: 22px;
    color: #1f2f3d;
  }

  &.warning strong { color: #e6a23c; }
  &.danger strong { color: #f56c6c; }
  &.success strong { color: #67c23a; }
}

.toolbar-row {
  align-items: center;
}

.sku-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.sku-thumb,
.sku-thumb-empty {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  flex: 0 0 52px;
}

.sku-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 12px;
}

.sku-main {
  min-width: 0;
}

.sku-code {
  border: 0;
  background: transparent;
  padding: 0;
  color: #1f2f3d;
  font-weight: 600;
  cursor: pointer;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-subline {
  color: #606266;
  font-size: 12px;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-tags,
.flag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.danger {
  color: #f56c6c;
}

.mt4 {
  margin-top: 4px;
}

.mb16 {
  margin-bottom: 16px;
}

.ml8 {
  margin-left: 8px;
}

.full {
  width: 100%;
}
</style>
