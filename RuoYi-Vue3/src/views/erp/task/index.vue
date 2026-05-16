<template>
  <div class="app-container task-console">
    <header class="task-console-header">
      <el-form
        ref="queryRef"
        :model="queryParams"
        :inline="true"
        v-show="showSearch"
        label-width="76px"
        class="console-filter"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="queryParams.taskName" placeholder="任务名称" clearable class="w-180" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="店铺" prop="shopName">
          <el-input v-model="queryParams.shopName" placeholder="Shop Name" clearable class="w-160" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="类型" prop="taskType">
          <el-select v-model="queryParams.taskType" placeholder="全部类型" clearable class="w-170" @change="handleQuery">
            <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="taskStatus">
          <el-select v-model="queryParams.taskStatus" placeholder="全部状态" clearable class="w-140" @change="handleQuery">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="daterangeStartTime"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="w-240"
          />
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar-status-row">
        <div class="toolbar-row">
          <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:task:export']">导出</el-button>
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['erp:task:remove']">删除</el-button>
          <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>

        <div class="status-segments" aria-label="任务状态快捷筛选">
          <button
            v-for="item in statusSegments"
            :key="item.key"
            type="button"
            class="status-segment"
            :class="{ active: item.active, danger: item.value === 'FAILED', warning: item.value === 'PART_SUCCESS' }"
            :aria-pressed="item.active"
            @click="applyStatusFilter(item.value)"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </button>
        </div>
      </div>
    </header>

    <main class="console-body">
      <section class="task-list-shell">
        <div class="task-list-head">
          <div>
            <h3>同步任务</h3>
            <span>当前筛选 {{ total }} 条，当前页 {{ taskList.length }} 条</span>
          </div>
          <el-tag v-if="selectedTask" :type="taskStatusTag(selectedTask.taskStatus)" effect="light">
            已选：{{ selectedTask.taskId }}
          </el-tag>
        </div>

        <div class="table-zone">
          <el-table
            v-loading="loading"
            :data="taskList"
            row-key="taskId"
            border
            highlight-current-row
            height="100%"
            class="task-table"
            :current-row-key="selectedTask?.taskId"
            :row-class-name="getTaskRowClassName"
            :header-cell-style="{ background: '#f7f8fb', color: '#303133' }"
            @row-click="selectTask"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="44" align="center" fixed />
            <el-table-column label="任务" min-width="260" fixed>
              <template #default="{ row }">
                <div class="task-cell">
                  <div class="task-title-row">
                    <button type="button" class="task-title" @click.stop="selectTask(row)">
                      {{ row.taskName || `任务 ${row.taskId}` }}
                    </button>
                    <el-tag :type="taskStatusTag(row.taskStatus)" effect="light" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
                  </div>
                  <div class="task-subline">
                    <span>ID：{{ row.taskId }}</span>
                    <span>{{ row.shopName || '-' }}</span>
                    <span>{{ taskTypeLabel(row.taskType) }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="170">
              <template #default="{ row }">
                <div class="progress-cell">
                  <el-progress :percentage="safeProgress(row.progress)" :color="progressColor(row.taskStatus)" />
                  <span>{{ formatDuration(row.executionTime) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="180">
              <template #default="{ row }">
                <div class="result-grid">
                  <span>总 {{ row.totalCount || 0 }}</span>
                  <span class="success">成 {{ row.successCount || 0 }}</span>
                  <span class="warning">部 {{ row.partialCount || 0 }}</span>
                  <span class="danger">败 {{ row.failedCount || 0 }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="错误摘要" prop="errorMessage" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <span :class="{ 'error-text': row.errorMessage }">{{ row.errorMessage || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="开始时间" prop="startTime" width="150" align="center">
              <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" prop="endTime" width="150" align="center">
              <template #default="{ row }">{{ formatDate(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="128" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" icon="Tickets" @click.stop="selectTask(row)" v-hasPermi="['erp:task:query']">诊断</el-button>
                <el-button link type="danger" icon="Delete" @click.stop="handleDelete(row)" v-hasPermi="['erp:task:remove']">删除</el-button>
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
      </section>

      <TaskDiagnosticsPanel ref="diagnosticsRef" :task="selectedTask" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onMounted, reactive, ref, toRefs, watch } from 'vue'
import { useRoute } from 'vue-router'
import { delTask, getTask, listTask, type TaskQuery } from '@/api/erp/task'
import { parseTime } from '@/utils/ruoyi'
import type { Task } from '@/types/erp'
import TaskDiagnosticsPanel from './components/TaskDiagnosticsPanel.vue'

const { proxy } = getCurrentInstance() as any
const route = useRoute()

const loading = ref(false)
const showSearch = ref(true)
const taskList = ref<Task[]>([])
const total = ref(0)
const ids = ref<number[]>([])
const multiple = ref(true)
const selectedTask = ref<Task | null>(null)
const daterangeStartTime = ref<string[]>([])
const diagnosticsRef = ref<{ refresh: () => void } | null>(null)
const pendingRouteTaskId = ref<number | null>(Number(route.query.taskId) || null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskName: undefined,
    shopName: undefined,
    taskType: undefined,
    taskStatus: undefined
  } as TaskQuery
})

const { queryParams } = toRefs(data)

const taskTypeOptions = [
  { label: '商品批量同步', value: 'PRODUCT_SYNC_BATCH' },
  { label: '商品同步', value: 'PRODUCT_SYNC' },
  { label: '媒体批量同步', value: 'MEDIA_SYNC_BATCH' },
  { label: '媒体同步', value: 'MEDIA_SYNC' },
  { label: '订单轮询', value: 'ORDER_SYNC' },
  { label: '发货回传', value: 'FULFILLMENT_SYNC' },
  { label: '退款同步', value: 'REFUND_SYNC' }
]

const statusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '执行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '部分成功', value: 'PART_SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' }
]

const quickStatusOptions = [
  { key: 'all', label: '全部', value: undefined },
  { key: 'running', label: '执行中', value: 'RUNNING' },
  { key: 'partial', label: '部分成功', value: 'PART_SUCCESS' },
  { key: 'failed', label: '失败', value: 'FAILED' },
  { key: 'success', label: '成功', value: 'SUCCESS' }
]

const statusSegments = computed(() => quickStatusOptions.map((item) => ({
  ...item,
  count: item.value ? taskList.value.filter((task) => task.taskStatus === item.value).length : taskList.value.length,
  active: item.value ? queryParams.value.taskStatus === item.value : !queryParams.value.taskStatus
})))

function getList(): void {
  loading.value = true
  const params: any = { ...queryParams.value, params: {} }
  if (daterangeStartTime.value?.length === 2) {
    params.params.beginStartTime = daterangeStartTime.value[0]
    params.params.endStartTime = daterangeStartTime.value[1]
  }
  listTask(params)
    .then((res: any) => {
      taskList.value = res.rows || []
      total.value = res.total || 0
      syncSelectionAfterList()
    })
    .finally(() => {
      loading.value = false
    })
}

function syncSelectionAfterList(): void {
  const routeTaskId = pendingRouteTaskId.value
  if (routeTaskId) {
    const routeTask = taskList.value.find((task) => task.taskId === routeTaskId)
    if (routeTask) {
      selectedTask.value = routeTask
    } else if (!selectedTask.value || selectedTask.value.taskId !== routeTaskId) {
      openTaskById(routeTaskId)
    }
    pendingRouteTaskId.value = null
    return
  }

  if (!taskList.value.length) {
    selectedTask.value = null
    return
  }

  const current = selectedTask.value
  const refreshed = current ? taskList.value.find((task) => task.taskId === current.taskId) : null
  selectedTask.value = refreshed || taskList.value[0]
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery(): void {
  daterangeStartTime.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function refreshAll(): void {
  getList()
  diagnosticsRef.value?.refresh()
}

function applyStatusFilter(value?: string): void {
  queryParams.value.taskStatus = queryParams.value.taskStatus === value ? undefined : value
  handleQuery()
}

function selectTask(row: Task): void {
  pendingRouteTaskId.value = null
  const sameTask = selectedTask.value?.taskId === row.taskId
  selectedTask.value = row
  if (sameTask) {
    nextTick(() => diagnosticsRef.value?.refresh())
  }
}

function openTaskById(taskId: number): void {
  getTask(taskId).then((res: any) => {
    selectedTask.value = res.data || { taskId }
  })
}

function handleSelectionChange(selection: Task[]): void {
  ids.value = selection.map((item) => item.taskId)
  multiple.value = selection.length === 0
}

function handleDelete(row?: Task): void {
  const taskIds = row?.taskId || ids.value.join(',')
  proxy.$modal.confirm(`是否确认删除 Shopify 同步任务 ${taskIds}？`)
    .then(() => delTask(taskIds))
    .then(() => {
      const deleteIds = String(taskIds).split(',').map((item) => Number(item))
      if (selectedTask.value && deleteIds.includes(selectedTask.value.taskId)) {
        selectedTask.value = null
      }
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

function handleExport(): void {
  const params: any = { ...queryParams.value, params: {} }
  if (daterangeStartTime.value?.length === 2) {
    params.params.beginStartTime = daterangeStartTime.value[0]
    params.params.endStartTime = daterangeStartTime.value[1]
  }
  proxy.download('erp/task/export', params, `task_${new Date().getTime()}.xlsx`)
}

function taskTypeLabel(value?: string): string {
  return taskTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

function taskStatusLabel(value?: string): string {
  return statusOptions.find((item) => item.value === value)?.label || value || '-'
}

function taskStatusTag(value?: string): '' | 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED') return 'danger'
  if (value === 'PART_SUCCESS') return 'warning'
  if (value === 'RUNNING') return 'primary'
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

function safeProgress(value?: number | null): number {
  const progress = Number(value || 0)
  if (progress < 0) return 0
  if (progress > 100) return 100
  return progress
}

function formatDate(value?: string | Date | null): string {
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}') || '-' : '-'
}

function formatDuration(value?: number | null): string {
  if (value === undefined || value === null) {
    return '-'
  }
  if (value < 1000) {
    return `${value}ms`
  }
  const seconds = value / 1000
  if (seconds < 60) {
    return `${seconds.toFixed(1)}s`
  }
  return `${Math.floor(seconds / 60)}m ${Math.round(seconds % 60)}s`
}

function getTaskRowClassName({ row }: { row: Task }): string {
  if (row.taskStatus === 'FAILED') return 'is-failed'
  if (row.taskStatus === 'PART_SUCCESS') return 'is-partial'
  if (row.taskStatus === 'RUNNING') return 'is-running'
  return ''
}

watch(
  () => route.query.taskId,
  (taskId) => {
    const nextTaskId = Number(taskId)
    if (nextTaskId) {
      pendingRouteTaskId.value = nextTaskId
      const task = taskList.value.find((item) => item.taskId === nextTaskId)
      if (task) {
        selectedTask.value = task
        pendingRouteTaskId.value = null
      } else {
        openTaskById(nextTaskId)
        pendingRouteTaskId.value = null
      }
    } else {
      pendingRouteTaskId.value = null
    }
  }
)

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.task-console {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  min-height: 560px;
  padding: 0;
  overflow: hidden;
  background: #fff;
}

.task-console-header {
  flex: 0 0 auto;
  padding: 10px 12px 8px;
  border-bottom: 1px solid #e6eaf0;
}

.console-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 0;
  align-items: center;
}

.console-filter :deep(.el-form-item) {
  margin-right: 6px;
  margin-bottom: 0;
}

.console-filter :deep(.el-form-item__label),
.console-filter :deep(.el-form-item__content) {
  line-height: 30px;
}

.console-filter :deep(.el-input__wrapper),
.console-filter :deep(.el-select__wrapper) {
  min-height: 30px;
}

.toolbar-status-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  margin-top: 8px;
  border-top: 1px solid #eef1f5;
}

.toolbar-row,
.status-segments {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.status-segments {
  justify-content: flex-end;
}

.status-segment {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 10px;
  color: #334155;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d9e0ea;
  border-radius: 6px;
  transition: background 0.18s ease, border-color 0.18s ease;

  strong {
    min-width: 16px;
    color: #1d4ed8;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
}

.status-segment:hover,
.status-segment.active {
  background: #eef6ff;
  border-color: #409eff;
}

.status-segment.danger.active {
  background: #fff8f7;
  border-color: #f56c6c;
}

.status-segment.warning.active {
  background: #fffaf0;
  border-color: #e6a23c;
}

.console-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 44%);
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.task-list-shell {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.task-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #eef1f5;

  h3 {
    margin: 0;
    font-size: 15px;
    color: #172033;
  }

  span {
    font-size: 12px;
    color: #667085;
  }
}

.table-zone {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.task-table {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  border: 0;
}

.task-table :deep(.el-table__cell) {
  padding: 5px 0;
}

.task-table :deep(.el-table__row.is-failed) {
  --el-table-tr-bg-color: #fff8f7;
}

.task-table :deep(.el-table__row.is-partial) {
  --el-table-tr-bg-color: #fffaf0;
}

.task-table :deep(.el-table__row.is-running) {
  --el-table-tr-bg-color: #f5f9ff;
}

.task-cell {
  min-width: 0;
}

.task-title-row,
.task-subline {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.task-title {
  max-width: 180px;
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

.task-title:hover {
  color: #2f6fed;
}

.task-subline {
  flex-wrap: wrap;
  margin-top: 4px;
  font-size: 12px;
  color: #667085;
}

.progress-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;

  span {
    font-size: 12px;
    color: #667085;
  }
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 8px;
  font-size: 12px;
  line-height: 18px;
}

.success { color: #2f8f4e; }
.warning { color: #b7791f; }
.danger,
.error-text { color: #c0362c; }

.pagination-wrapper {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  padding: 6px 12px 0;
  border-top: 1px solid #e6eaf0;
}

.pagination-wrapper :deep(.pagination-container) {
  padding: 0;
  margin: 0;
  background: transparent;
}

.w-140 { width: 140px; }
.w-160 { width: 160px; }
.w-170 { width: 170px; }
.w-180 { width: 180px; }
.w-240 { width: 240px; }

@media (max-width: 1180px) {
  .task-console {
    height: auto;
    min-height: calc(100vh - 84px);
    overflow: visible;
  }

  .toolbar-status-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-segments {
    justify-content: flex-start;
  }

  .console-body {
    display: grid;
    grid-template-columns: 1fr;
    grid-template-rows: minmax(420px, 58vh) minmax(560px, auto);
    overflow: visible;
  }
}

@media (max-width: 640px) {
  .task-console-header,
  .task-list-head {
    padding-right: 8px;
    padding-left: 8px;
  }

  .w-140,
  .w-160,
  .w-170,
  .w-180,
  .w-240 {
    width: min(100%, 280px);
  }

  .status-segment {
    flex: 1 1 120px;
    justify-content: space-between;
  }
}
</style>
