<template>
  <div class="app-container profit-report">
    <el-form ref="queryRef" :model="queryParams" :inline="true" label-width="82px">
      <el-form-item label="店铺" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="全部店铺" clearable filterable style="width: 190px">
          <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName || store.shopName" :value="store.storeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="下单时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">统计</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <section class="metric-grid">
      <div class="metric">
        <span>订单数</span>
        <strong>{{ summary.orderCount || 0 }}</strong>
      </div>
      <div class="metric">
        <span>销售额</span>
        <strong>{{ money(summary.salesAmount) }}</strong>
      </div>
      <div class="metric">
        <span>采购成本</span>
        <strong>{{ money(summary.purchaseCost) }}</strong>
      </div>
      <div class="metric">
        <span>物流费</span>
        <strong>{{ money(summary.shippingCost) }}</strong>
      </div>
      <div class="metric">
        <span>退款额</span>
        <strong>{{ money(summary.refundAmount) }}</strong>
      </div>
      <div class="metric">
        <span>毛利</span>
        <strong :class="{ danger: (summary.grossProfit || 0) < 0 }">{{ money(summary.grossProfit) }}</strong>
      </div>
      <div class="metric">
        <span>毛利率</span>
        <strong>{{ percent(summary.grossProfitRate) }}</strong>
      </div>
    </section>

    <el-table v-loading="loading" :data="orderList" border row-key="orderId" :header-cell-style="{ background: '#f7f8fb', color: '#303133' }">
      <el-table-column label="订单" prop="orderName" min-width="140" fixed />
      <el-table-column label="店铺" prop="shopName" min-width="150" show-overflow-tooltip />
      <el-table-column label="客户" prop="customerName" min-width="150" show-overflow-tooltip />
      <el-table-column label="销售额" width="110" align="right">
        <template #default="{ row }">{{ money(row.totalPrice) }}</template>
      </el-table-column>
      <el-table-column label="采购成本" width="110" align="right">
        <template #default="{ row }">{{ money(row.purchaseCost) }}</template>
      </el-table-column>
      <el-table-column label="物流费" width="110" align="right">
        <template #default="{ row }">{{ money(row.shippingCost) }}</template>
      </el-table-column>
      <el-table-column label="退款额" width="110" align="right">
        <template #default="{ row }">{{ money(row.totalRefund) }}</template>
      </el-table-column>
      <el-table-column label="毛利" width="110" align="right">
        <template #default="{ row }"><span :class="{ danger: (row.grossProfit || 0) < 0 }">{{ money(row.grossProfit) }}</span></template>
      </el-table-column>
      <el-table-column label="毛利率" width="100" align="right">
        <template #default="{ row }">{{ percent(row.grossProfitRate) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="160" align="center">
        <template #default="{ row }">{{ formatTime(row.placedAt) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { getProfitSummary, listOrder, type ProfitReportQuery } from '@/api/erp/order'
import { listActiveStores } from '@/api/erp/store'
import { parseTime } from '@/utils/ruoyi'
import type { OrderProfitSummary, ShopifyOrder, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const total = ref(0)
const orderList = ref<ShopifyOrder[]>([])
const storeOptions = ref<ShopifyStore[]>([])
const dateRange = ref<string[]>([])
const summary = ref<OrderProfitSummary>({})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    beginDate: undefined,
    endDate: undefined
  } as ProfitReportQuery
})

const { queryParams } = toRefs(data)

function applyDateRange(): void {
  queryParams.value.beginDate = dateRange.value?.[0]
  queryParams.value.endDate = dateRange.value?.[1]
}

function getList(): void {
  applyDateRange()
  loading.value = true
  Promise.all([
    getProfitSummary(queryParams.value),
    listOrder({
      ...queryParams.value,
      pageNum: queryParams.value.pageNum,
      pageSize: queryParams.value.pageSize
    })
  ]).then(([summaryRes, orderRes]: any[]) => {
    summary.value = summaryRes.data || {}
    orderList.value = orderRes.rows || []
    total.value = orderRes.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery(): void {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery(): void {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function money(value?: number): string {
  return ((value || 0) / 100).toFixed(2)
}

function percent(value?: number): string {
  return `${(((value || 0) * 100)).toFixed(2)}%`
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

<style scoped lang="scss">
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 10px 0 16px;
}

.metric {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px 14px;
  background: #fff;

  span {
    display: block;
    color: #909399;
    font-size: 12px;
    margin-bottom: 6px;
  }

  strong {
    font-size: 20px;
    color: #1f2f3d;
  }
}

.danger {
  color: #f56c6c !important;
}
</style>
