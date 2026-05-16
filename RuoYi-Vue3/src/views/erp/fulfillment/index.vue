<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="店铺" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="全部店铺" clearable filterable style="width: 190px" @change="handleQuery">
          <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName || store.shopName" :value="store.storeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单ID" prop="orderId">
        <el-input v-model.number="queryParams.orderId" placeholder="本地订单ID" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="syncStatus">
        <el-select v-model="queryParams.syncStatus" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
          <el-option label="待回传" value="PENDING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="searchKeyword">
        <el-input v-model="queryParams.searchKeyword" placeholder="运单号/物流公司" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openSubmit" v-hasPermi="['erp:fulfillment:add']">录入发货</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="recordList" border row-key="fulfillmentId" :header-cell-style="{ background: '#f7f8fb', color: '#303133' }">
      <el-table-column label="发货ID" prop="fulfillmentId" width="100" fixed />
      <el-table-column label="订单ID" prop="orderId" width="110" />
      <el-table-column label="Shopify订单" prop="shopifyOrderId" min-width="210" show-overflow-tooltip />
      <el-table-column label="物流公司" prop="trackingCompany" width="140" />
      <el-table-column label="运单号" prop="trackingNumber" min-width="180" show-overflow-tooltip />
      <el-table-column label="物流费用" width="110" align="right">
        <template #default="{ row }">{{ money(row.shippingFee) }}</template>
      </el-table-column>
      <el-table-column label="回传状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.syncStatus)">{{ row.syncStatus || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发货时间" width="160" align="center">
        <template #default="{ row }">{{ formatTime(row.fulfilledAt) }}</template>
      </el-table-column>
      <el-table-column label="错误信息" prop="errorMessage" min-width="240" show-overflow-tooltip>
        <template #default="{ row }"><span :class="{ danger: row.errorMessage }">{{ row.errorMessage || '-' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-link v-if="row.trackingUrl" type="primary" :href="row.trackingUrl" target="_blank">物流查询</el-link>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="submitOpen" title="录入发货并回传 Shopify" width="560px" append-to-body>
      <el-form ref="submitRef" :model="submitForm" :rules="rules" label-width="120px">
        <el-form-item label="本地订单ID" prop="orderId">
          <el-input-number v-model="submitForm.orderId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="物流公司" prop="trackingCompany">
          <el-input v-model="submitForm.trackingCompany" placeholder="4PX / YunExpress / UPS" />
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNumber">
          <el-input v-model="submitForm.trackingNumber" />
        </el-form-item>
        <el-form-item label="查询链接">
          <el-input v-model="submitForm.trackingUrl" />
        </el-form-item>
        <el-form-item label="物流费用(分)">
          <el-input-number v-model="submitForm.shippingFee" :min="0" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitOpen = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="submitRecord">回传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { listFulfillment, submitFulfillment, type FulfillmentRecordQuery, type FulfillmentSubmitRequest } from '@/api/erp/order'
import { listActiveStores } from '@/api/erp/store'
import { parseTime } from '@/utils/ruoyi'
import type { FormInstance, FormRules } from 'element-plus'
import type { FulfillmentRecord, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const recordList = ref<FulfillmentRecord[]>([])
const storeOptions = ref<ShopifyStore[]>([])
const submitOpen = ref(false)
const saveLoading = ref(false)
const submitRef = ref<FormInstance>()

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    orderId: undefined,
    syncStatus: undefined,
    searchKeyword: undefined
  } as FulfillmentRecordQuery
})

const submitForm = reactive<FulfillmentSubmitRequest>({
  orderId: undefined,
  trackingCompany: '4PX',
  trackingNumber: '',
  trackingUrl: '',
  shippingFee: 0
})

const rules: FormRules = {
  orderId: [{ required: true, message: '订单ID不能为空', trigger: 'blur' }],
  trackingCompany: [{ required: true, message: '物流公司不能为空', trigger: 'blur' }],
  trackingNumber: [{ required: true, message: '运单号不能为空', trigger: 'blur' }]
}

const { queryParams } = toRefs(data)

function getList(): void {
  loading.value = true
  listFulfillment(queryParams.value).then((res: any) => {
    recordList.value = res.rows || []
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

function openSubmit(): void {
  submitForm.orderId = queryParams.value.orderId
  submitForm.trackingCompany = '4PX'
  submitForm.trackingNumber = ''
  submitForm.trackingUrl = ''
  submitForm.shippingFee = 0
  submitOpen.value = true
}

function submitRecord(): void {
  submitRef.value?.validate((valid) => {
    if (!valid) return
    saveLoading.value = true
    submitFulfillment({ ...submitForm }).then(() => {
      proxy.$modal.msgSuccess('发货已回传')
      submitOpen.value = false
      getList()
    }).finally(() => {
      saveLoading.value = false
    })
  })
}

function money(value?: number): string {
  return ((value || 0) / 100).toFixed(2)
}

function formatTime(value?: string | Date | null): string {
  return parseTime(value as any, '{y}-{m}-{d} {h}:{i}') || '-'
}

function statusTag(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
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

.danger {
  color: #f56c6c;
}
</style>
