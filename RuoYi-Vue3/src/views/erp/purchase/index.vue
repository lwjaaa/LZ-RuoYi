<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="店铺" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="全部店铺" clearable filterable style="width: 190px" @change="handleQuery">
          <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName || store.shopName" :value="store.storeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="采购状态" prop="purchaseStatus">
        <el-select v-model="queryParams.purchaseStatus" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
          <el-option label="待采购" value="PENDING" />
          <el-option label="已采购" value="PURCHASED" />
          <el-option label="异常" value="EXCEPTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="searchKeyword">
        <el-input v-model="queryParams.searchKeyword" placeholder="订单/SKU/商品" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" border row-key="purchaseTaskId" :header-cell-style="{ background: '#f7f8fb', color: '#303133' }">
      <el-table-column label="订单" prop="orderName" width="130" fixed />
      <el-table-column label="SKU" prop="sku" min-width="150" show-overflow-tooltip />
      <el-table-column label="商品" prop="itemTitle" min-width="260" show-overflow-tooltip />
      <el-table-column label="数量" prop="quantity" width="80" align="center" />
      <el-table-column label="采购链接" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.purchaseUrl" type="primary" :href="row.purchaseUrl" target="_blank">打开来源链接</el-link>
          <span v-else class="muted">未匹配采购链接</span>
        </template>
      </el-table-column>
      <el-table-column label="预计采购" width="120" align="right">
        <template #default="{ row }">{{ money(row.expectedPurchaseAmount) }}</template>
      </el-table-column>
      <el-table-column label="实际采购" width="120" align="right">
        <template #default="{ row }">{{ money(row.actualPurchaseAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.purchaseStatus)">{{ statusLabel(row.purchaseStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="异常/备注" prop="remark" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.exceptionReason || row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="CopyDocument" @click="copyAddress(row)">收货信息</el-button>
          <el-button link type="success" icon="Check" @click="markPurchased(row)" v-hasPermi="['erp:purchase-task:edit']">已采购</el-button>
          <el-button link type="warning" icon="Edit" @click="openEdit(row)" v-hasPermi="['erp:purchase-task:edit']">记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="editOpen" title="记录采购结果" width="560px" append-to-body>
      <el-form ref="editRef" :model="editForm" label-width="120px">
        <el-form-item label="订单号">{{ currentTask?.orderName }}</el-form-item>
        <el-form-item label="SKU">{{ currentTask?.sku }}</el-form-item>
        <el-form-item label="采购状态">
          <el-radio-group v-model="editForm.purchaseStatus">
            <el-radio-button label="PENDING">待采购</el-radio-button>
            <el-radio-button label="PURCHASED">已采购</el-radio-button>
            <el-radio-button label="EXCEPTION">异常</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实际金额(分)">
          <el-input-number v-model="editForm.actualPurchaseAmount" :min="0" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="异常原因">
          <el-input v-model="editForm.exceptionReason" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { getOrder, listPurchaseTask, updatePurchaseTask, type PurchaseTaskEdit, type PurchaseTaskQuery } from '@/api/erp/order'
import { listActiveStores } from '@/api/erp/store'
import type { PurchaseTask, ShopifyOrder, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref<PurchaseTask[]>([])
const storeOptions = ref<ShopifyStore[]>([])
const editOpen = ref(false)
const saveLoading = ref(false)
const currentTask = ref<PurchaseTask | null>(null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    purchaseStatus: undefined,
    searchKeyword: undefined
  } as PurchaseTaskQuery
})

const editForm = reactive<PurchaseTaskEdit>({
  purchaseTaskId: undefined,
  purchaseStatus: 'PENDING',
  actualPurchaseAmount: undefined,
  exceptionReason: '',
  remark: ''
})

const { queryParams } = toRefs(data)

function getList(): void {
  loading.value = true
  listPurchaseTask(queryParams.value).then((res: any) => {
    taskList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery(): void {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openEdit(row: PurchaseTask): void {
  currentTask.value = row
  editForm.purchaseTaskId = row.purchaseTaskId
  editForm.purchaseStatus = row.purchaseStatus || 'PENDING'
  editForm.actualPurchaseAmount = row.actualPurchaseAmount ?? row.expectedPurchaseAmount
  editForm.exceptionReason = row.exceptionReason || ''
  editForm.remark = row.remark || ''
  editOpen.value = true
}

function markPurchased(row: PurchaseTask): void {
  updatePurchaseTask({
    purchaseTaskId: row.purchaseTaskId,
    purchaseStatus: 'PURCHASED',
    actualPurchaseAmount: row.actualPurchaseAmount ?? row.expectedPurchaseAmount,
    remark: row.remark
  }).then(() => {
    proxy.$modal.msgSuccess('已标记采购完成')
    getList()
  })
}

function submitEdit(): void {
  saveLoading.value = true
  updatePurchaseTask({ ...editForm }).then(() => {
    proxy.$modal.msgSuccess('采购记录已保存')
    editOpen.value = false
    getList()
  }).finally(() => {
    saveLoading.value = false
  })
}

function copyAddress(row: PurchaseTask): void {
  if (!row.orderId) return
  getOrder(row.orderId).then((res: any) => {
    const order = res.data as ShopifyOrder
    const text = [
      `收货人：${order.shippingName || ''}`,
      `电话：${order.shippingPhone || order.phone || ''}`,
      `地址：${[order.shippingCountry, order.shippingProvince, order.shippingCity, order.shippingZip, order.shippingAddress1, order.shippingAddress2].filter(Boolean).join(' ')}`,
      `订单号：${order.orderName || ''}`
    ].join('\n')
    navigator.clipboard?.writeText(text)
    proxy.$modal.msgSuccess('收货信息已复制')
  })
}

function money(value?: number): string {
  return ((value || 0) / 100).toFixed(2)
}

function statusLabel(status?: string): string {
  return ({ PENDING: '待采购', PURCHASED: '已采购', EXCEPTION: '异常' } as Record<string, string>)[status || ''] || status || '-'
}

function statusTag(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'PURCHASED') return 'success'
  if (status === 'EXCEPTION') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

onMounted(() => {
  listActiveStores().then((res: any) => {
    storeOptions.value = res.data || []
  })
  getList()
})
</script>

<style scoped lang="scss">
.muted {
  color: #909399;
}
</style>
