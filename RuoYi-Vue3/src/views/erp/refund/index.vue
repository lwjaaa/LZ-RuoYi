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
      <el-form-item label="关键词" prop="searchKeyword">
        <el-input v-model="queryParams.searchKeyword" placeholder="原因/备注/责任" clearable style="width: 220px" @keyup.enter="handleQuery" />
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

    <el-table v-loading="loading" :data="refundList" border row-key="refundId" :header-cell-style="{ background: '#f7f8fb', color: '#303133' }">
      <el-table-column label="退款ID" prop="refundId" width="110" fixed />
      <el-table-column label="订单ID" prop="orderId" width="110" />
      <el-table-column label="Shopify退款ID" prop="shopifyRefundId" min-width="220" show-overflow-tooltip />
      <el-table-column label="退款金额" width="120" align="right">
        <template #default="{ row }">{{ money(row.refundAmount, row.currencyCode) }}</template>
      </el-table-column>
      <el-table-column label="原因" prop="reason" min-width="160" show-overflow-tooltip />
      <el-table-column label="责任归类" prop="responsibility" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.responsibility || '待归类' }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="note" min-width="260" show-overflow-tooltip />
      <el-table-column label="退款时间" width="160" align="center">
        <template #default="{ row }">{{ formatTime(row.refundTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { listRefund, type RefundRecordQuery } from '@/api/erp/order'
import { listActiveStores } from '@/api/erp/store'
import { parseTime } from '@/utils/ruoyi'
import type { RefundRecord, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const refundList = ref<RefundRecord[]>([])
const storeOptions = ref<ShopifyStore[]>([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    orderId: undefined,
    searchKeyword: undefined
  } as RefundRecordQuery
})

const { queryParams } = toRefs(data)

function getList(): void {
  loading.value = true
  listRefund(queryParams.value).then((res: any) => {
    refundList.value = res.rows || []
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

function money(value?: number, currency?: string): string {
  const amount = ((value || 0) / 100).toFixed(2)
  return `${currency || ''} ${amount}`.trim()
}

function formatTime(value?: string | Date | null): string {
  return parseTime(value as any, '{y}-{m}-{d} {h}:{i}') || '-'
}

onMounted(() => {
  listActiveStores().then((res: any) => {
    storeOptions.value = res.data || []
  })
  getList()
})
</script>
