<template>
  <div class="app-container order-center">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" label-width="82px">
      <el-form-item label="店铺" prop="storeId">
        <el-select v-model="queryParams.storeId" placeholder="全部店铺" clearable filterable style="width: 190px" @change="handleQuery">
          <el-option v-for="store in storeOptions" :key="store.storeId" :label="store.storeName || store.shopName" :value="store.storeId" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单号" prop="orderName">
        <el-input v-model="queryParams.orderName" placeholder="#1001" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="付款" prop="financialStatus">
        <el-select v-model="queryParams.financialStatus" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
          <el-option label="已付款" value="PAID" />
          <el-option label="待付款" value="PENDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="部分退款" value="PARTIALLY_REFUNDED" />
        </el-select>
      </el-form-item>
      <el-form-item label="履约" prop="fulfillmentStatus">
        <el-select v-model="queryParams.fulfillmentStatus" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
          <el-option label="未履约" value="UNFULFILLED" />
          <el-option label="部分履约" value="PARTIALLY_FULFILLED" />
          <el-option label="已履约" value="FULFILLED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="searchKeyword">
        <el-input v-model="queryParams.searchKeyword" placeholder="客户/邮箱/SKU" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" :disabled="!queryParams.storeId" @click="handleSync(false)" v-hasPermi="['erp:order:sync']">增量轮询</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="RefreshRight" :disabled="!queryParams.storeId" @click="handleSync(true)" v-hasPermi="['erp:order:sync']">近30天补拉</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="Aim" :disabled="!queryParams.storeId" @click="handleCursor" v-hasPermi="['erp:order:query']">同步游标</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="orderList" border row-key="orderId" :header-cell-style="{ background: '#f7f8fb', color: '#303133' }">
      <el-table-column type="expand" width="44">
        <template #default="{ row }">
          <el-table :data="row.lineItems || []" border size="small" class="line-table">
            <el-table-column label="SKU" prop="sku" min-width="150" />
            <el-table-column label="商品" min-width="260" show-overflow-tooltip>
              <template #default="{ row: item }">{{ item.title }} <span class="muted">{{ item.variantTitle || '' }}</span></template>
            </el-table-column>
            <el-table-column label="数量" prop="quantity" width="70" align="center" />
            <el-table-column label="售价" width="110" align="right">
              <template #default="{ row: item }">{{ money(item.totalPrice, row.currencyCode) }}</template>
            </el-table-column>
            <el-table-column label="采购链接" min-width="220" show-overflow-tooltip>
              <template #default="{ row: item }">
                <el-link v-if="item.purchaseUrl" type="primary" :href="item.purchaseUrl" target="_blank">打开来源链接</el-link>
                <span v-else class="muted">未匹配</span>
              </template>
            </el-table-column>
            <el-table-column label="采购成本" width="120" align="right">
              <template #default="{ row: item }">{{ money(item.purchaseAmount, row.currencyCode) }}</template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column label="订单" min-width="190" fixed>
        <template #default="{ row }">
          <div class="strong">{{ row.orderName || '-' }}</div>
          <div class="muted">{{ row.shopName || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="客户" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ row.customerName || row.email || '-' }}</div>
          <div class="muted">{{ row.phone || row.email || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="收货地址" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">{{ fullAddress(row) }}</template>
      </el-table-column>
      <el-table-column label="付款" width="110" align="center">
        <template #default="{ row }"><el-tag :type="paymentTag(row.financialStatus)">{{ row.financialStatus || '-' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="履约" width="120" align="center">
        <template #default="{ row }"><el-tag :type="fulfillmentTag(row.fulfillmentStatus)">{{ row.fulfillmentStatus || '-' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="销售额" width="115" align="right">
        <template #default="{ row }">{{ money(row.totalPrice, row.currencyCode) }}</template>
      </el-table-column>
      <el-table-column label="采购" width="115" align="right">
        <template #default="{ row }">{{ money(row.purchaseCost, row.currencyCode) }}</template>
      </el-table-column>
      <el-table-column label="物流" width="115" align="right">
        <template #default="{ row }">{{ money(row.shippingCost, row.currencyCode) }}</template>
      </el-table-column>
      <el-table-column label="退款" width="115" align="right">
        <template #default="{ row }">{{ money(row.totalRefund, row.currencyCode) }}</template>
      </el-table-column>
      <el-table-column label="预计毛利" width="125" align="right">
        <template #default="{ row }">
          <span :class="{ danger: (row.grossProfit || 0) < 0 }">{{ money(row.grossProfit, row.currencyCode) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="155" align="center">
        <template #default="{ row }">{{ formatTime(row.placedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="openDetail(row)" v-hasPermi="['erp:order:query']">详情</el-button>
          <el-button link type="success" icon="Van" @click="openFulfillment(row)" v-hasPermi="['erp:fulfillment:add']">发货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" size="720px" title="订单详情">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderName }}</el-descriptions-item>
          <el-descriptions-item label="店铺">{{ currentOrder.shopName }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrder.customerName || currentOrder.email }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentOrder.phone || currentOrder.shippingPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ fullAddress(currentOrder) }}</el-descriptions-item>
          <el-descriptions-item label="销售额">{{ money(currentOrder.totalPrice, currentOrder.currencyCode) }}</el-descriptions-item>
          <el-descriptions-item label="预计毛利">{{ money(currentOrder.grossProfit, currentOrder.currencyCode) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="currentOrder.lineItems || []" border class="mt16">
          <el-table-column label="SKU" prop="sku" min-width="150" />
          <el-table-column label="商品" prop="title" min-width="220" show-overflow-tooltip />
          <el-table-column label="数量" prop="quantity" width="80" />
          <el-table-column label="采购链接" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <el-link v-if="row.purchaseUrl" type="primary" :href="row.purchaseUrl" target="_blank">{{ row.purchaseUrl }}</el-link>
              <span v-else class="muted">未匹配</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="fulfillmentOpen" title="发货回传 Shopify" width="560px" append-to-body>
      <el-form ref="fulfillmentRef" :model="fulfillmentForm" :rules="fulfillmentRules" label-width="110px">
        <el-form-item label="订单号">{{ fulfillmentOrder?.orderName }}</el-form-item>
        <el-form-item label="物流公司" prop="trackingCompany">
          <el-input v-model="fulfillmentForm.trackingCompany" placeholder="4PX / YunExpress / UPS" />
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNumber">
          <el-input v-model="fulfillmentForm.trackingNumber" placeholder="请输入运单号" />
        </el-form-item>
        <el-form-item label="查询链接">
          <el-input v-model="fulfillmentForm.trackingUrl" placeholder="可选" />
        </el-form-item>
        <el-form-item label="物流费用(分)">
          <el-input-number v-model="fulfillmentForm.shippingFee" :min="0" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fulfillmentOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFulfillmentForm">回传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cursorOpen" title="订单同步游标" width="520px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="状态">{{ cursorInfo?.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成功推进到">{{ formatTime(cursorInfo?.lastSuccessUpdatedAt) }}</el-descriptions-item>
        <el-descriptions-item label="最近同步">{{ formatTime(cursorInfo?.lastSuccessSyncTime) }}</el-descriptions-item>
        <el-descriptions-item label="最近任务">{{ cursorInfo?.lastTaskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近错误">{{ cursorInfo?.lastErrorSummary || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref, toRefs } from 'vue'
import { backfillOrders, getOrder, getOrderCursor, listOrder, submitFulfillment, syncOrders, type FulfillmentSubmitRequest, type ShopifyOrderQuery } from '@/api/erp/order'
import { listActiveStores } from '@/api/erp/store'
import { parseTime } from '@/utils/ruoyi'
import type { FormInstance, FormRules } from 'element-plus'
import type { ShopifyOrder, ShopifyOrderSyncCursor, ShopifyStore } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const loading = ref(false)
const showSearch = ref(true)
const orderList = ref<ShopifyOrder[]>([])
const storeOptions = ref<ShopifyStore[]>([])
const total = ref(0)
const detailOpen = ref(false)
const currentOrder = ref<ShopifyOrder | null>(null)
const cursorOpen = ref(false)
const cursorInfo = ref<ShopifyOrderSyncCursor | null>(null)
const fulfillmentOpen = ref(false)
const fulfillmentOrder = ref<ShopifyOrder | null>(null)
const submitLoading = ref(false)
const fulfillmentRef = ref<FormInstance>()

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeId: undefined,
    orderName: undefined,
    financialStatus: undefined,
    fulfillmentStatus: undefined,
    purchaseStatus: undefined,
    fulfillmentSyncStatus: undefined,
    searchKeyword: undefined
  } as ShopifyOrderQuery
})

const fulfillmentForm = reactive<FulfillmentSubmitRequest>({
  orderId: undefined,
  trackingCompany: '',
  trackingNumber: '',
  trackingUrl: '',
  shippingFee: 0
})

const fulfillmentRules: FormRules = {
  trackingCompany: [{ required: true, message: '物流公司不能为空', trigger: 'blur' }],
  trackingNumber: [{ required: true, message: '运单号不能为空', trigger: 'blur' }]
}

const { queryParams } = toRefs(data)

function getList(): void {
  loading.value = true
  listOrder(queryParams.value).then((res: any) => {
    orderList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadStores(): void {
  listActiveStores().then((res: any) => {
    storeOptions.value = res.data || []
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

function handleSync(backfill: boolean): void {
  if (!queryParams.value.storeId) {
    proxy.$modal.msgWarning('请先选择店铺')
    return
  }
  const request = backfill ? backfillOrders : syncOrders
  request(queryParams.value.storeId).then((res: any) => {
    proxy.$modal.msgSuccess(`已创建任务：${res.data}`)
  })
}

function handleCursor(): void {
  if (!queryParams.value.storeId) {
    proxy.$modal.msgWarning('请先选择店铺')
    return
  }
  getOrderCursor(queryParams.value.storeId).then((res: any) => {
    cursorInfo.value = res.data
    cursorOpen.value = true
  })
}

function openDetail(row: ShopifyOrder): void {
  if (!row.orderId) return
  getOrder(row.orderId).then((res: any) => {
    currentOrder.value = res.data
    detailOpen.value = true
  })
}

function openFulfillment(row: ShopifyOrder): void {
  fulfillmentOrder.value = row
  fulfillmentForm.orderId = row.orderId
  fulfillmentForm.trackingCompany = '4PX'
  fulfillmentForm.trackingNumber = ''
  fulfillmentForm.trackingUrl = ''
  fulfillmentForm.shippingFee = 0
  fulfillmentOpen.value = true
}

function submitFulfillmentForm(): void {
  fulfillmentRef.value?.validate((valid) => {
    if (!valid) return
    submitLoading.value = true
    submitFulfillment({ ...fulfillmentForm }).then(() => {
      proxy.$modal.msgSuccess('发货已回传')
      fulfillmentOpen.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function fullAddress(row: ShopifyOrder): string {
  return [row.shippingName, row.shippingPhone, row.shippingCountry, row.shippingProvince, row.shippingCity, row.shippingZip, row.shippingAddress1, row.shippingAddress2]
    .filter(Boolean)
    .join(' ')
}

function money(value?: number, currency?: string): string {
  const amount = ((value || 0) / 100).toFixed(2)
  return `${currency || ''} ${amount}`.trim()
}

function formatTime(value?: string | Date | null): string {
  return parseTime(value as any, '{y}-{m}-{d} {h}:{i}') || '-'
}

function paymentTag(status?: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'PAID') return 'success'
  if (status === 'REFUNDED' || status === 'PARTIALLY_REFUNDED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function fulfillmentTag(status?: string): 'success' | 'warning' | 'info' {
  if (status === 'FULFILLED') return 'success'
  if (status === 'PARTIALLY_FULFILLED') return 'warning'
  return 'info'
}

onMounted(() => {
  loadStores()
  getList()
})
</script>

<style scoped lang="scss">
.order-center {
  .line-table {
    margin: 8px 24px;
    width: calc(100% - 48px);
  }
}

.strong {
  font-weight: 600;
  color: #1f2f3d;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.danger {
  color: #f56c6c;
}

.mt16 {
  margin-top: 16px;
}
</style>
