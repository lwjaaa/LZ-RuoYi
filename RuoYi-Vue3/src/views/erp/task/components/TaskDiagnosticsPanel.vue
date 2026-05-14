<template>
  <aside class="diagnostics-panel">
    <el-empty v-if="!task" description="选择左侧任务查看同步诊断" />

    <template v-else>
      <header class="diagnostics-header">
        <div class="diagnostics-title">
          <span class="eyebrow">任务诊断</span>
          <h3>{{ task.taskName || `任务 ${task.taskId}` }}</h3>
          <div class="meta-line">
            <span>ID：{{ task.taskId }}</span>
            <span>{{ task.shopName || '-' }}</span>
            <span>{{ formatDate(task.startTime || task.createTime) }}</span>
          </div>
        </div>
        <el-tag :type="taskStatusTag(task.taskStatus)" effect="light">{{ taskStatusLabel(task.taskStatus) }}</el-tag>
      </header>

      <section class="progress-block">
        <div class="progress-head">
          <span>执行进度</span>
          <strong>{{ safeProgress(task.progress) }}%</strong>
        </div>
        <el-progress :percentage="safeProgress(task.progress)" :color="progressColor(task.taskStatus)" />
        <div class="count-grid" aria-label="任务数量统计">
          <div>
            <span>总数</span>
            <strong>{{ task.totalCount || 0 }}</strong>
          </div>
          <div>
            <span>成功</span>
            <strong class="success">{{ task.successCount || 0 }}</strong>
          </div>
          <div>
            <span>部分</span>
            <strong class="warning">{{ task.partialCount || 0 }}</strong>
          </div>
          <div>
            <span>失败</span>
            <strong class="danger">{{ task.failedCount || 0 }}</strong>
          </div>
        </div>
      </section>

      <el-alert
        v-if="task.errorMessage"
        class="task-error-alert"
        type="error"
        :closable="false"
        show-icon
      >
        <template #title>任务错误摘要</template>
        <template #default>{{ task.errorMessage }}</template>
      </el-alert>

      <section class="diagnostic-section" v-loading="diagnosticsLoading">
        <div class="section-title">
          <span>失败分布</span>
          <el-button link type="primary" icon="Refresh" @click="refresh">刷新</el-button>
        </div>
        <div class="metric-row">
          <span
            v-for="item in itemTypeStats"
            :key="item.name"
            class="metric-chip"
          >
            {{ itemTypeLabel(item.name) }}<strong>{{ item.total || 0 }}</strong>
          </span>
          <span v-if="itemTypeStats.length === 0" class="muted">暂无明细统计</span>
        </div>
        <div class="metric-row">
          <span
            v-for="item in statusStats"
            :key="item.name"
            class="metric-chip"
            :class="{ 'danger-chip': item.name === 'FAILED', 'warning-chip': item.name === 'PART_SUCCESS' }"
          >
            {{ detailStatusLabel(item.name) }}<strong>{{ item.total || 0 }}</strong>
          </span>
          <span v-if="statusStats.length === 0" class="muted">暂无状态统计</span>
        </div>
        <div class="metric-row">
          <span
            v-for="item in failedStepStats"
            :key="item.name"
            class="metric-chip warning-chip"
          >
            {{ stepLabel(item.name) }}<strong>{{ item.total || 0 }}</strong>
          </span>
          <span v-if="failedStepStats.length === 0" class="muted">暂无失败步骤</span>
        </div>
      </section>

      <section class="diagnostic-section top-errors" v-loading="diagnosticsLoading">
        <div class="section-title">
          <span>高频错误</span>
        </div>
        <div v-if="topErrors.length" class="error-list">
          <button
            v-for="error in topErrors"
            :key="`${error.errorCode || '-'}-${error.errorField || '-'}-${error.errorMessage || '-'}`"
            type="button"
            class="error-item"
            @click="copyText(error.errorMessage || '')"
          >
            <span class="error-main">{{ error.errorMessage || '未知错误' }}</span>
            <span class="error-sub">
              {{ error.errorCode || 'NO_CODE' }}
              <template v-if="error.errorField"> · {{ error.errorField }}</template>
            </span>
            <strong>{{ error.total || 0 }}</strong>
          </button>
        </div>
        <el-empty v-else description="暂无高频错误" :image-size="56" />
      </section>

      <section class="diagnostic-section recent-failures" v-loading="diagnosticsLoading">
        <div class="section-title">
          <span>最近失败</span>
          <span class="muted">点击可带入下方明细筛选</span>
        </div>
        <div v-if="recentFailures.length" class="recent-list">
          <button
            v-for="failure in recentFailures"
            :key="failure.detailId"
            type="button"
            class="recent-item"
            @click="focusDetail(failure)"
          >
            <span class="recent-title">
              {{ itemTypeLabel(failure.itemType) }} · {{ failure.itemName || failure.itemId || '-' }}
            </span>
            <span class="recent-sub">
              商品 {{ failure.productId || '-' }} · {{ failure.step || '未知步骤' }} · {{ failure.errorMessage || '未知错误' }}
            </span>
          </button>
        </div>
        <el-empty v-else description="暂无最近失败" :image-size="56" />
      </section>

      <section class="detail-section">
        <div class="section-title">
          <span>对象明细</span>
          <span class="muted">默认查看失败项，可切换状态排查部分成功或成功记录</span>
        </div>
        <el-form :model="detailQuery" :inline="true" label-width="68px" class="detail-filter">
          <el-form-item label="商品ID">
            <el-input v-model="detailQuery.productId" placeholder="商品ID" clearable class="w-120" @keyup.enter="loadDetails" />
          </el-form-item>
          <el-form-item label="对象">
            <el-select v-model="detailQuery.itemType" placeholder="全部" clearable class="w-120" @change="handleDetailQuery">
              <el-option label="商品" value="PRODUCT" />
              <el-option label="变体" value="VARIANT" />
              <el-option label="媒体" value="MEDIA" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="detailQuery.status" placeholder="全部" clearable class="w-130" @change="handleDetailQuery">
              <el-option v-for="item in detailStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleDetailQuery">筛选</el-button>
            <el-button icon="Refresh" @click="resetDetailQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="detailLoading"
          :data="detailList"
          row-key="detailId"
          border
          size="small"
          class="detail-table"
          :row-class-name="getDetailRowClassName"
        >
          <el-table-column type="expand" width="38">
            <template #default="{ row }">
              <div class="detail-expand">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="失败原因" :span="2">{{ row.errorMessage || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="错误编码">{{ row.errorCode || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="错误字段">{{ row.errorField || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="Shopify ID">{{ row.shopifyId || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="输入下标">{{ row.inputIndex ?? '-' }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="对象" prop="itemType" width="78" align="center">
            <template #default="{ row }">
              <el-tag :type="itemTypeTag(row.itemType)" effect="light">{{ itemTypeLabel(row.itemType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="92" align="center">
            <template #default="{ row }">
              <el-tag :type="detailStatusTag(row.status)" effect="light">{{ detailStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="商品/本地ID" width="120">
            <template #default="{ row }">
              <div class="id-stack">
                <span>P：{{ row.productId || '-' }}</span>
                <span>L：{{ row.itemId || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="名称/SKU/文件" prop="itemName" min-width="150" show-overflow-tooltip />
          <el-table-column label="步骤" prop="step" min-width="120" show-overflow-tooltip />
          <el-table-column label="失败原因" prop="errorMessage" min-width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="72" align="center" fixed="right">
            <template #default="{ row }">
              <el-tooltip content="复制失败原因" placement="top">
                <el-button link type="primary" icon="CopyDocument" :disabled="!row.errorMessage" @click="copyText(row.errorMessage)" />
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="detailTotal > 0"
          :total="detailTotal"
          v-model:page="detailQuery.pageNum"
          v-model:limit="detailQuery.pageSize"
          @pagination="loadDetails"
        />
      </section>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskDiagnostics, listTaskDetails, type TaskDetailQuery } from '@/api/erp/task'
import { parseTime } from '@/utils/ruoyi'
import type { ShopifyTaskDetail, ShopifyTaskDiagnostics, ShopifyTaskDiagnosticStat, ShopifyTaskErrorStat, Task } from '@/types/erp'

const props = defineProps<{
  task: Task | null
}>()

const diagnostics = ref<ShopifyTaskDiagnostics | null>(null)
const diagnosticsLoading = ref(false)
const detailLoading = ref(false)
const detailList = ref<ShopifyTaskDetail[]>([])
const detailTotal = ref(0)

const detailQuery = reactive<TaskDetailQuery>({
  pageNum: 1,
  pageSize: 10,
  productId: undefined,
  itemType: undefined,
  status: 'FAILED'
})

const detailStatusOptions = [
  { label: '失败', value: 'FAILED' },
  { label: '部分成功', value: 'PART_SUCCESS' },
  { label: '成功', value: 'SUCCESS' },
  { label: '跳过', value: 'SKIPPED' }
]

const itemTypeStats = computed<ShopifyTaskDiagnosticStat[]>(() => diagnostics.value?.itemTypeStats || [])
const statusStats = computed<ShopifyTaskDiagnosticStat[]>(() => diagnostics.value?.statusStats || [])
const failedStepStats = computed<ShopifyTaskDiagnosticStat[]>(() => diagnostics.value?.failedStepStats || [])
const topErrors = computed<ShopifyTaskErrorStat[]>(() => diagnostics.value?.topErrors || [])
const recentFailures = computed<ShopifyTaskDetail[]>(() => diagnostics.value?.recentFailures || [])

watch(
  () => props.task?.taskId,
  (taskId) => {
    diagnostics.value = null
    detailList.value = []
    detailTotal.value = 0
    detailQuery.pageNum = 1
    detailQuery.status = 'FAILED'
    if (taskId) {
      refresh()
    }
  },
  { immediate: true }
)

function refresh(): void {
  loadDiagnostics()
  loadDetails()
}

function loadDiagnostics(): void {
  const taskId = props.task?.taskId
  if (!taskId) {
    return
  }
  diagnosticsLoading.value = true
  getTaskDiagnostics(taskId)
    .then((res: any) => {
      diagnostics.value = res.data || null
    })
    .finally(() => {
      diagnosticsLoading.value = false
    })
}

function loadDetails(): void {
  const taskId = props.task?.taskId
  if (!taskId) {
    return
  }
  detailLoading.value = true
  const params: TaskDetailQuery = { ...detailQuery }
  if (!params.productId) {
    delete params.productId
  }
  if (!params.itemType) {
    delete params.itemType
  }
  if (!params.status) {
    delete params.status
  }
  listTaskDetails(taskId, params)
    .then((res: any) => {
      detailList.value = res.rows || []
      detailTotal.value = res.total || 0
    })
    .finally(() => {
      detailLoading.value = false
    })
}

function handleDetailQuery(): void {
  detailQuery.pageNum = 1
  loadDetails()
}

function resetDetailQuery(): void {
  detailQuery.productId = undefined
  detailQuery.itemType = undefined
  detailQuery.status = 'FAILED'
  detailQuery.pageNum = 1
  loadDetails()
}

function focusDetail(detail: ShopifyTaskDetail): void {
  detailQuery.productId = detail.productId
  detailQuery.itemType = detail.itemType
  detailQuery.status = detail.status || 'FAILED'
  detailQuery.pageNum = 1
  loadDetails()
}

function copyText(text?: string): void {
  if (!text) {
    ElMessage.warning('暂无可复制内容')
    return
  }
  if (!navigator.clipboard?.writeText) {
    ElMessage.warning('当前浏览器不支持一键复制')
    return
  }
  navigator.clipboard.writeText(text)
    .then(() => ElMessage.success('复制成功'))
    .catch(() => ElMessage.error('复制失败，请手动复制'))
}

function safeProgress(value?: number | null): number {
  const progress = Number(value || 0)
  if (progress < 0) return 0
  if (progress > 100) return 100
  return progress
}

function taskStatusLabel(value?: string): string {
  const map: Record<string, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    SUCCESS: '成功',
    PART_SUCCESS: '部分成功',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return value ? map[value] || value : '-'
}

function taskStatusTag(value?: string): '' | 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'PART_SUCCESS') return 'warning'
  if (value === 'RUNNING') return 'primary'
  return 'info'
}

function detailStatusLabel(value?: string): string {
  return detailStatusOptions.find((item) => item.value === value)?.label || value || '-'
}

function detailStatusTag(value?: string): '' | 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'PART_SUCCESS') return 'warning'
  return 'info'
}

function progressColor(value?: string): string {
  const map: Record<string, string> = {
    SUCCESS: '#67C23A',
    FAILED: '#F56C6C',
    PART_SUCCESS: '#E6A23C',
    RUNNING: '#409EFF',
    PENDING: '#909399'
  }
  return map[value || ''] || '#909399'
}

function itemTypeLabel(value?: string): string {
  const map: Record<string, string> = {
    PRODUCT: '商品',
    VARIANT: '变体',
    MEDIA: '媒体'
  }
  return value ? map[value] || value : '-'
}

function itemTypeTag(value?: string): '' | 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (value === 'PRODUCT') return 'primary'
  if (value === 'VARIANT') return 'success'
  if (value === 'MEDIA') return 'warning'
  return 'info'
}

function stepLabel(value?: string): string {
  return value === 'UNKNOWN' ? '未知步骤' : value || '-'
}

function formatDate(value?: string | Date | null): string {
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}') || '-' : '-'
}

function getDetailRowClassName({ row }: { row: ShopifyTaskDetail }): string {
  if (row.status === 'FAILED') return 'is-failed'
  if (row.status === 'PART_SUCCESS') return 'is-partial'
  return ''
}

defineExpose({
  refresh
})
</script>

<style scoped lang="scss">
.diagnostics-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #fff;
  border-left: 1px solid #e6eaf0;
}

.diagnostics-panel > * {
  flex-shrink: 0;
}

.diagnostics-header {
  flex: 0 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e6eaf0;
}

.diagnostics-title {
  min-width: 0;

  h3 {
    margin: 2px 0 4px;
    overflow: hidden;
    font-size: 15px;
    line-height: 20px;
    color: #172033;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.eyebrow,
.muted,
.meta-line,
.section-title .muted {
  font-size: 12px;
  color: #667085;
}

.eyebrow {
  font-weight: 600;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}

.progress-block,
.diagnostic-section,
.detail-section {
  flex-shrink: 0;
  padding: 12px 14px;
  border-bottom: 1px solid #eef1f5;
}

.progress-head,
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.count-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;

  div {
    min-width: 0;
    padding: 8px;
    background: #f7f8fb;
    border: 1px solid #e6eaf0;
    border-radius: 6px;
  }

  span {
    display: block;
    font-size: 12px;
    color: #667085;
  }

  strong {
    display: block;
    margin-top: 2px;
    font-size: 16px;
    color: #172033;
    font-variant-numeric: tabular-nums;
  }
}

.success { color: #2f8f4e !important; }
.warning { color: #b7791f !important; }
.danger { color: #c0362c !important; }

.task-error-alert {
  flex: 0 0 auto;
  margin: 10px 14px 0;
  overflow: visible;
}

.task-error-alert :deep(.el-alert__content) {
  min-width: 0;
  width: 100%;
}

.task-error-alert :deep(.el-alert__description) {
  display: block;
  margin-top: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
  align-items: center;
}

.metric-row + .metric-row {
  margin-top: 8px;
}

.metric-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 26px;
  padding: 0 8px;
  font-size: 12px;
  color: #334155;
  background: #f7f8fb;
  border: 1px solid #d9e0ea;
  border-radius: 6px;

  strong {
    color: #1d4ed8;
    font-variant-numeric: tabular-nums;
  }
}

.warning-chip {
  background: #fffaf0;
  border-color: #f3d19e;
}

.danger-chip {
  background: #fff8f7;
  border-color: #ffd8d4;
}

.top-errors,
.recent-failures {
  flex: 0 0 auto;
}

.error-list {
  display: grid;
  gap: 6px;
  max-height: 178px;
  overflow-y: auto;
  padding-right: 2px;
}

.error-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 2px 8px;
  width: 100%;
  min-height: 42px;
  padding: 7px 8px;
  text-align: left;
  cursor: pointer;
  background: #fff8f7;
  border: 1px solid #ffd8d4;
  border-radius: 6px;
  box-sizing: border-box;

  strong {
    grid-row: 1 / span 2;
    align-self: center;
    color: #c0362c;
    font-variant-numeric: tabular-nums;
  }
}

.error-main,
.error-sub {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.error-main {
  display: -webkit-box;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.error-main {
  color: #912018;
  font-size: 13px;
  font-weight: 600;
}

.error-sub {
  color: #8a94a6;
  font-size: 12px;
  white-space: nowrap;
}

.recent-list {
  display: grid;
  gap: 6px;
  max-height: 154px;
  overflow-y: auto;
  padding-right: 2px;
}

.recent-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  min-height: 40px;
  padding: 7px 8px;
  text-align: left;
  cursor: pointer;
  background: #fbfcfe;
  border: 1px solid #e6eaf0;
  border-radius: 6px;
}

.recent-item:hover {
  background: #eef6ff;
  border-color: #9cc8ff;
}

.recent-title,
.recent-sub {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-title {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.recent-sub {
  color: #667085;
  font-size: 12px;
}

.detail-section {
  flex: 0 0 auto;
  min-height: 360px;
  overflow: visible;
  border-bottom: 0;
}

.detail-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
  margin-bottom: 8px;
}

.detail-filter :deep(.el-form-item) {
  margin-right: 6px;
  margin-bottom: 0;
}

.detail-filter :deep(.el-form-item__label),
.detail-filter :deep(.el-form-item__content) {
  line-height: 30px;
}

.detail-table {
  width: 100%;
}

.detail-table :deep(.el-table__cell) {
  padding: 4px 0;
}

.detail-table :deep(.el-table__row.is-failed) {
  --el-table-tr-bg-color: #fff8f7;
}

.detail-table :deep(.el-table__row.is-partial) {
  --el-table-tr-bg-color: #fffaf0;
}

.detail-expand {
  padding: 8px 12px;
  background: #fbfcfe;
}

.detail-expand :deep(.el-descriptions__content) {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.id-stack {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: #475467;
  font-size: 12px;
  line-height: 16px;
}

.w-120 { width: 120px; }
.w-130 { width: 130px; }

:deep(.pagination-container) {
  margin: 8px 0 0;
  padding: 0;
  background: transparent;
}

@media (max-width: 1180px) {
  .diagnostics-panel {
    border-top: 1px solid #e6eaf0;
    border-left: 0;
  }
}
</style>
