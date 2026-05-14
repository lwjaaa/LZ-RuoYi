<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="店铺名称" prop="storeName">
        <el-input v-model="queryParams.storeName" placeholder="请输入店铺名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Shop Name" prop="shopName">
        <el-input v-model="queryParams.shopName" placeholder="请输入 Shopify 标识" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="isActive">
        <el-select v-model="queryParams.isActive" placeholder="请选择状态" clearable style="width: 120px">
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="默认店铺" prop="isDefault">
        <el-select v-model="queryParams.isDefault" placeholder="请选择" clearable style="width: 120px">
          <el-option label="是" value="1" />
          <el-option label="否" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:store:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['erp:store:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['erp:store:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['erp:store:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="storeList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="店铺名称" prop="storeName" min-width="150" show-overflow-tooltip />
      <el-table-column label="Shop Name" prop="shopName" min-width="170" show-overflow-tooltip />
      <el-table-column label="API版本" prop="apiVersion" width="100" align="center" />
      <el-table-column label="启用" prop="isActive" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive === '1' ? 'success' : 'info'">{{ row.isActive === '1' ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" prop="isDefault" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === '1'" type="warning">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="连接状态" prop="status" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存仓库" prop="inventoryLocationName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.inventoryLocationName || row.inventoryLocationId || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="库存" width="120" align="center">
        <template #default="{ row }">
          <span>{{ row.inventoryTracked === '1' ? row.defaultInventoryQuantity ?? 0 : '不跟踪' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="自动发布渠道" prop="publishPublicationNames" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.publishPublicationNames || row.publishPublicationIds || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="最后同步" prop="lastSyncTime" width="170" align="center">
        <template #default="{ row }">
          {{ parseTime(row.lastSyncTime, '{y}-{m}-{d} {h}:{i}') || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="{ row }">
          <div class="operation-actions">
            <el-button link type="warning" icon="Operation" @click="handleApiCall(row)" v-hasPermi="['erp:store:edit']">接口</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['erp:store:edit']">修改</el-button>
            <el-dropdown trigger="click" class="operation-more">
              <el-button link type="primary" class="operation-more-button">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item icon="Connection" @click="handleTest(row)" v-hasPermi="['erp:store:query']">测试</el-dropdown-item>
                  <el-dropdown-item icon="Promotion" @click="handlePublishUpdate(row)" v-hasPermi="['erp:store:edit']">自动发布渠道</el-dropdown-item>
                  <el-dropdown-item icon="Refresh" @click="handleProductImport(row, 'incremental')" v-hasPermi="['erp:store:edit']">增量同步商品</el-dropdown-item>
                  <el-dropdown-item icon="UploadFilled" @click="handleProductImport(row, 'full')" v-hasPermi="['erp:store:edit']">全量同步商品</el-dropdown-item>
                  <el-dropdown-item icon="View" @click="handleImportCursor(row)" v-hasPermi="['erp:store:query']">同步游标</el-dropdown-item>
                  <el-dropdown-item icon="Delete" @click="handleDelete(row)" v-hasPermi="['erp:store:remove']">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="880px" append-to-body :close-on-click-modal="!saveLoading">
      <el-form ref="storeRef" v-loading="saveLoading" :model="form" :rules="rules" label-width="130px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="店铺名称" prop="storeName">
              <el-input v-model="form.storeName" placeholder="例如 Velar Home" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Shop Name" prop="shopName">
              <el-input v-model="form.shopName" placeholder="myshopify.com 前缀" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API版本" prop="apiVersion">
              <el-input v-model="form.apiVersion" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认证模式" prop="authMode">
              <el-select v-model="form.authMode" style="width: 100%">
                <el-option label="Private App" value="PRIVATE_APP" />
                <el-option label="OAuth" value="OAUTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用">
              <el-switch v-model="form.isActive" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认店铺">
              <el-switch v-model="form.isDefault" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="连接状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="已连接" value="CONNECTED" />
                <el-option label="未连接" value="DISCONNECTED" />
                <el-option label="已过期" value="EXPIRED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">API 凭据</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="API Key">
              <el-input v-model="form.apiKey" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Secret">
              <el-input v-model="form.apiSecret" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Access Token">
              <el-input v-model="form.accessToken" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Token过期时间">
              <el-date-picker v-model="form.tokenExpiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">库存设置</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="跟踪库存" prop="inventoryTracked">
              <el-switch v-model="form.inventoryTracked" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认库存" prop="defaultInventoryQuantity">
              <el-input-number v-model="form.defaultInventoryQuantity" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="缺货策略" prop="inventoryPolicy">
              <el-select v-model="form.inventoryPolicy" style="width: 100%">
                <el-option label="拒绝继续销售" value="DENY" />
                <el-option label="允许继续销售" value="CONTINUE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="库存仓库" prop="inventoryLocationId">
              <el-input v-model="form.inventoryLocationId" placeholder="gid://shopify/Location/..." class="input-with-action">
                <template #append>
                  <el-button :loading="resourceLoading" @click="loadLocations">拉取</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="locationOptions.length > 0">
            <el-form-item label="选择仓库">
              <el-select v-model="form.inventoryLocationId" filterable style="width: 100%" @change="handleLocationChange">
                <el-option v-for="item in locationOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">商品资料规则</el-divider>
        <el-form-item label="必填字段">
          <el-select
            v-model="selectedRequiredFields"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择商品资料必填项"
            style="width: 100%"
          >
            <el-option v-for="item in productMissingFieldOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="saveLoading" @click="submitForm">确定</el-button>
          <el-button :disabled="saveLoading" @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="自动发布渠道" v-model="publishOpen" width="640px" append-to-body :close-on-click-modal="!saveLoading">
      <el-form v-loading="saveLoading" :model="form" label-width="120px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.storeName" disabled />
        </el-form-item>
        <el-form-item label="默认商品状态" prop="defaultProductStatus">
          <el-select v-model="form.defaultProductStatus" style="width: 100%">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="在售" value="ACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="自动发布渠道">
          <div class="inline-resource">
            <el-select
              v-model="selectedPublicationIds"
              multiple
              filterable
              allow-create
              default-first-option
              style="width: 100%"
              @change="handlePublicationChange"
            >
              <el-option v-for="item in publicationOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <el-button :loading="resourceLoading" @click="loadPublications">拉取</el-button>
          </div>
        </el-form-item>
        <el-form-item label="Publication ID">
          <el-input v-model="form.publishPublicationIds" type="textarea" :rows="2" placeholder="多个 ID 使用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="渠道名称">
          <el-input v-model="form.publishPublicationNames" placeholder="多个名称使用英文逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="saveLoading" @click="submitPublishForm">确定</el-button>
          <el-button :disabled="saveLoading" @click="cancelPublish">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="Shopify 接口调用" v-model="apiCallOpen" width="920px" append-to-body :close-on-click-modal="!apiCallLoading">
      <el-form :model="apiCallForm" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="店铺">
              <el-input :model-value="apiCallStore?.storeName || ''" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Shop Name">
              <el-input :model-value="apiCallStore?.shopName || ''" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Method">
              <el-radio-group v-model="apiCallForm.method">
                <el-radio-button label="GET" />
                <el-radio-button label="POST" />
                <el-radio-button label="PUT" />
                <el-radio-button label="DELETE" />
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Mode">
              <el-radio-group v-model="apiCallForm.mode">
                <el-radio-button label="GRAPHQL">GraphQL</el-radio-button>
                <el-radio-button label="JSON">JSON</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="URL">
              <el-input v-model="apiCallForm.url" />
            </el-form-item>
          </el-col>
          <template v-if="apiCallForm.mode === 'GRAPHQL'">
            <el-col :span="24">
              <el-form-item label="Query">
                <el-input v-model="apiCallForm.query" type="textarea" :rows="8" class="api-call-textarea" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="Variables">
                <div class="api-call-editor">
                  <el-input v-model="apiCallForm.variables" type="textarea" :rows="5" class="api-call-textarea" />
                  <el-button icon="MagicStick" @click="formatApiCallJson('variables')">格式化</el-button>
                </div>
              </el-form-item>
            </el-col>
          </template>
          <template v-else>
            <el-col :span="24">
              <el-form-item label="Body">
                <div class="api-call-editor">
                  <el-input v-model="apiCallForm.body" type="textarea" :rows="10" class="api-call-textarea" />
                  <el-button icon="MagicStick" @click="formatApiCallJson('body')">格式化</el-button>
                </div>
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
      <div v-if="apiCallResult" class="api-response">
        <div class="api-response-meta">
          <el-tag :type="apiCallStatusType">{{ apiCallResult.statusCode }}</el-tag>
          <span>{{ apiCallResult.durationMs ?? 0 }}ms</span>
          <span>{{ apiCallResult.contentType || '-' }}</span>
        </div>
        <pre class="api-response-body">{{ formattedApiCallResponse }}</pre>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="apiCallLoading" @click="submitApiCall">调用</el-button>
          <el-button :disabled="apiCallLoading" @click="cancelApiCall">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, reactive, ref, toRefs } from 'vue'
import { addStore, callStoreApi, delStore, fetchStoreLocations, fetchStorePublications, getShopifyProductImportCursor, getStore, importShopifyProductsFull, importShopifyProductsIncremental, listStore, testStoreConnection, updateStore } from '@/api/erp/store'
import { parseTime } from '@/utils/ruoyi'
import { defaultRequiredMissingFields, productMissingFieldOptions, splitFieldCodes } from '@/utils/erp/productWorkbench'
import type { ShopifyApiCallRequest, ShopifyApiCallResponse, ShopifyProductImportCursor, ShopifyResourceOption, ShopifyStore, ShopifyStoreQuery } from '@/types/erp'

const { proxy } = getCurrentInstance() as any

const storeList = ref<ShopifyStore[]>([])
const loading = ref(false)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const publishOpen = ref(false)
const apiCallOpen = ref(false)
const title = ref('')
const saveLoading = ref(false)
const resourceLoading = ref(false)
const apiCallLoading = ref(false)
const locationOptions = ref<ShopifyResourceOption[]>([])
const publicationOptions = ref<ShopifyResourceOption[]>([])
const selectedPublicationIds = ref<string[]>([])
const selectedRequiredFields = ref<string[]>([...defaultRequiredMissingFields])
const apiCallStore = ref<ShopifyStore | null>(null)
const apiCallResult = ref<ShopifyApiCallResponse | null>(null)
const apiCallForm = reactive<ShopifyApiCallRequest>({
  method: 'POST',
  url: '',
  mode: 'GRAPHQL',
  query: '{ shop { name } }',
  variables: '{}',
  body: '{}'
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    storeName: undefined,
    shopName: undefined,
    isActive: undefined,
    isDefault: undefined
  } as ShopifyStoreQuery,
  form: createDefaultForm(),
  rules: {
    storeName: [{ required: true, message: '店铺名称不能为空', trigger: 'blur' }],
    shopName: [{ required: true, message: 'Shop Name不能为空', trigger: 'blur' }],
    apiVersion: [{ required: true, message: 'API版本不能为空', trigger: 'blur' }],
    inventoryLocationId: [{ validator: validateLocation, trigger: 'blur' }],
    defaultInventoryQuantity: [{ validator: validateQuantity, trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const currentStoreId = computed(() => form.value.storeId)
const formattedApiCallResponse = computed(() => {
  if (!apiCallResult.value) {
    return ''
  }
  const rawBody = apiCallResult.value.rawBody
  if (rawBody) {
    return formatJsonForDisplay(rawBody)
  }
  if (apiCallResult.value.body === undefined || apiCallResult.value.body === null) {
    return ''
  }
  if (typeof apiCallResult.value.body === 'string') {
    return formatJsonForDisplay(apiCallResult.value.body)
  }
  return JSON.stringify(apiCallResult.value.body, null, 2)
})
const apiCallStatusType = computed(() => {
  const statusCode = apiCallResult.value?.statusCode || 0
  if (statusCode >= 400) {
    return 'danger'
  }
  if (statusCode >= 300) {
    return 'warning'
  }
  return 'success'
})

function createDefaultForm(): ShopifyStore {
  return {
    storeName: '',
    shopName: '',
    apiVersion: '2026-04',
    apiKey: '',
    apiSecret: '',
    accessToken: '',
    refreshToken: '',
    baseUrl: '',
    inventoryLocationId: '',
    inventoryLocationName: '',
    inventoryTracked: '0',
    defaultInventoryQuantity: 100,
    inventoryPolicy: 'DENY',
    publishPublicationIds: '',
    publishPublicationNames: '',
    defaultProductStatus: 'DRAFT',
    requiredProductFields: defaultRequiredMissingFields.join(','),
    availablePublicationIds: '',
    isActive: '1',
    isDefault: '0',
    authMode: 'PRIVATE_APP',
    status: 'DISCONNECTED',
    remark: ''
  }
}

function getList(): void {
  loading.value = true
  listStore(queryParams.value)
    .then((res: any) => {
      storeList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
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

function reset(): void {
  form.value = createDefaultForm()
  selectedPublicationIds.value = []
  selectedRequiredFields.value = [...defaultRequiredMissingFields]
  locationOptions.value = []
  publicationOptions.value = []
  proxy.resetForm('storeRef')
}

function handleSelectionChange(selection: ShopifyStore[]): void {
  ids.value = selection.map((item) => item.storeId as number)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

function handleAdd(): void {
  reset()
  open.value = true
  title.value = '新增 Shopify 店铺'
}

function handleUpdate(row?: ShopifyStore): void {
  reset()
  const storeId = row?.storeId || ids.value[0]
  if (!storeId) {
    return
  }
  getStore(storeId).then((res: any) => {
    form.value = { ...createDefaultForm(), ...res.data, refreshToken: '' }
    selectedPublicationIds.value = splitCsv(form.value.publishPublicationIds)
    selectedRequiredFields.value = splitFieldCodes(form.value.requiredProductFields)
    if (!selectedRequiredFields.value.length) {
      selectedRequiredFields.value = [...defaultRequiredMissingFields]
    }
    seedPublicationOptions()
    open.value = true
    title.value = '修改 Shopify 店铺'
  })
}

function handlePublishUpdate(row: ShopifyStore): void {
  reset()
  if (!row.storeId) {
    return
  }
  getStore(row.storeId).then((res: any) => {
    form.value = { ...createDefaultForm(), ...res.data, refreshToken: '' }
    selectedPublicationIds.value = splitCsv(form.value.publishPublicationIds)
    seedPublicationOptions()
    publishOpen.value = true
  })
}

function handleApiCall(row: ShopifyStore): void {
  if (!row.storeId) {
    return
  }
  apiCallStore.value = row
  apiCallResult.value = null
  apiCallForm.method = 'POST'
  apiCallForm.mode = 'GRAPHQL'
  apiCallForm.url = getDefaultApiCallUrl(row)
  apiCallForm.query = '{ shop { name } }'
  apiCallForm.variables = '{}'
  apiCallForm.body = '{}'
  apiCallOpen.value = true
}

function submitForm(): void {
  if (saveLoading.value) {
    return
  }
  proxy.$refs.storeRef.validate((valid: boolean) => {
    if (!valid) {
      return
    }
    syncPublicationFields()
    syncRequiredFields()
    saveLoading.value = true
    const request = form.value.storeId ? updateStore(form.value) : addStore(form.value)
    request
      .then(() => {
        proxy.$modal.msgSuccess(form.value.storeId ? '修改成功' : '新增成功')
        open.value = false
        getList()
      })
      .finally(() => {
        saveLoading.value = false
      })
  })
}

function submitPublishForm(): void {
  if (saveLoading.value) {
    return
  }
  syncPublicationFields()
  saveLoading.value = true
  updateStore(form.value)
    .then(() => {
      proxy.$modal.msgSuccess('自动发布渠道修改成功')
      publishOpen.value = false
      getList()
    })
    .finally(() => {
      saveLoading.value = false
  })
}

function submitApiCall(): void {
  if (apiCallLoading.value || !apiCallStore.value?.storeId) {
    return
  }
  if (!validateApiCallForm()) {
    return
  }
  apiCallLoading.value = true
  apiCallResult.value = null
  callStoreApi(apiCallStore.value.storeId, { ...apiCallForm })
    .then((res: any) => {
      apiCallResult.value = res.data
    })
    .finally(() => {
      apiCallLoading.value = false
    })
}

function cancel(): void {
  if (saveLoading.value) {
    return
  }
  open.value = false
  reset()
}

function cancelApiCall(): void {
  if (apiCallLoading.value) {
    return
  }
  apiCallOpen.value = false
  apiCallResult.value = null
}

function cancelPublish(): void {
  if (saveLoading.value) {
    return
  }
  publishOpen.value = false
  reset()
}

function handleDelete(row?: ShopifyStore): void {
  const storeIds = row?.storeId || ids.value.join(',')
  proxy.$modal.confirm(`是否确认删除店铺编号为 ${storeIds} 的数据项？`)
    .then(() => delStore(storeIds as number | string))
    .then(() => {
      getList()
      proxy.$modal.msgSuccess('删除成功')
    })
    .catch(() => {})
}

function handleExport(): void {
  proxy.download('erp/store/export', queryParams.value, `store_${new Date().getTime()}.xlsx`)
}

function handleTest(row: ShopifyStore): void {
  if (!row.storeId) {
    return
  }
  testStoreConnection(row.storeId).then(() => {
    proxy.$modal.msgSuccess('连接成功')
    getList()
  })
}

function handleProductImport(row: ShopifyStore, mode: 'full' | 'incremental'): void {
  if (!row.storeId) {
    return
  }
  const label = mode === 'full' ? '全量同步' : '增量同步'
  const request = mode === 'full' ? importShopifyProductsFull : importShopifyProductsIncremental
  proxy.$modal.confirm(`是否确认对店铺「${row.storeName || row.shopName}」发起 Shopify 商品${label}？`)
    .then(() => request(row.storeId as number))
    .then((res: any) => {
      proxy.$modal.msgSuccess(`任务已创建：${res.data}`)
      getList()
    })
    .catch(() => {})
}

function handleImportCursor(row: ShopifyStore): void {
  if (!row.storeId) {
    return
  }
  getShopifyProductImportCursor(row.storeId).then((res: any) => {
    const cursor = res.data as ShopifyProductImportCursor
    proxy.$modal.alert(formatCursor(cursor), 'Shopify 商品同步游标')
  })
}

function formatCursor(cursor?: ShopifyProductImportCursor | null): string {
  if (!cursor) {
    return '暂无同步游标'
  }
  return [
    `状态：${cursor.status || '-'}`,
    `同步模式：${cursor.syncMode || '-'}`,
    `上次成功 Shopify 更新时间：${parseTime(cursor.lastSuccessUpdatedAt, '{y}-{m}-{d} {h}:{i}:{s}') || '-'}`,
    `上次成功完成时间：${parseTime(cursor.lastSuccessSyncTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-'}`,
    `最近任务ID：${cursor.lastTaskId || '-'}`,
    `最近 Bulk Operation：${cursor.lastBulkOperationId || '-'}`,
    `错误摘要：${cursor.lastErrorSummary || '-'}`
  ].join('\n')
}

function loadLocations(): void {
  if (!ensureSavedStore()) {
    return
  }
  resourceLoading.value = true
  fetchStoreLocations(currentStoreId.value as number)
    .then((res: any) => {
      locationOptions.value = res.data || []
      proxy.$modal.msgSuccess('仓库已拉取')
    })
    .finally(() => {
      resourceLoading.value = false
    })
}

function loadPublications(): void {
  if (!ensureSavedStore()) {
    return
  }
  resourceLoading.value = true
  fetchStorePublications(currentStoreId.value as number)
    .then((res: any) => {
      publicationOptions.value = res.data || []
      form.value.availablePublicationIds = publicationOptions.value.map((item) => item.id).join(',')
      selectedPublicationIds.value = publicationOptions.value.map((item) => item.id)
      handlePublicationChange()
      proxy.$modal.msgSuccess('自动发布渠道已拉取')
    })
    .finally(() => {
      resourceLoading.value = false
    })
}

function ensureSavedStore(): boolean {
  if (!currentStoreId.value) {
    proxy.$modal.msgWarning('请先保存店铺后再拉取 Shopify 配置')
    return false
  }
  return true
}

function handleLocationChange(value: string): void {
  const selected = locationOptions.value.find((item) => item.id === value)
  form.value.inventoryLocationName = selected?.name || ''
}

function handlePublicationChange(): void {
  form.value.publishPublicationIds = selectedPublicationIds.value.join(',')
  form.value.publishPublicationNames = selectedPublicationIds.value
    .map((id) => publicationOptions.value.find((item) => item.id === id)?.name || id)
    .join(',')
}

function syncPublicationFields(): void {
  form.value.availablePublicationIds = publicationOptions.value.map((item) => item.id).join(',')
  const ids = splitCsv(form.value.publishPublicationIds)
  selectedPublicationIds.value = ids
  form.value.publishPublicationIds = ids.join(',')
  form.value.publishPublicationNames = ids
    .map((id) => publicationOptions.value.find((item) => item.id === id)?.name || id)
    .join(',')
}

function syncRequiredFields(): void {
  form.value.requiredProductFields = selectedRequiredFields.value.join(',')
}

function seedPublicationOptions(): void {
  const ids = splitCsv(form.value.publishPublicationIds)
  const names = splitCsv(form.value.publishPublicationNames)
  publicationOptions.value = ids.map((id, index) => ({ id, name: names[index] || id }))
}

function splitCsv(value?: string): string[] {
  if (!value) {
    return []
  }
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

function getDefaultApiCallUrl(row: ShopifyStore): string {
  if (row.baseUrl) {
    return row.baseUrl
  }
  const shopName = (row.shopName || '').replace(/\.myshopify\.com$/i, '')
  const apiVersion = row.apiVersion || '2026-04'
  return `https://${shopName}.myshopify.com/admin/api/${apiVersion}/graphql.json`
}

function validateApiCallForm(): boolean {
  if (!apiCallForm.url?.trim()) {
    proxy.$modal.msgError('接口地址不能为空')
    return false
  }
  if (apiCallForm.mode === 'GRAPHQL') {
    if (!apiCallForm.query?.trim()) {
      proxy.$modal.msgError('GraphQL 查询不能为空')
      return false
    }
    return validateJsonObjectText(apiCallForm.variables, 'Variables')
  }
  return validateJsonText(apiCallForm.body, 'Body')
}

function validateJsonObjectText(value: string | undefined, label: string): boolean {
  if (!validateJsonText(value, label)) {
    return false
  }
  if (!value?.trim()) {
    return true
  }
  const parsed = JSON.parse(value)
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    proxy.$modal.msgError(`${label} 必须是 JSON 对象`)
    return false
  }
  return true
}

function validateJsonText(value: string | undefined, label: string): boolean {
  if (!value?.trim()) {
    return true
  }
  try {
    JSON.parse(value)
    return true
  } catch (_error) {
    proxy.$modal.msgError(`${label} JSON格式不正确`)
    return false
  }
}

function formatApiCallJson(field: 'variables' | 'body'): void {
  const value = apiCallForm[field]
  if (!value?.trim()) {
    return
  }
  try {
    apiCallForm[field] = JSON.stringify(JSON.parse(value), null, 2)
  } catch (_error) {
    proxy.$modal.msgError('JSON格式不正确')
  }
}

function formatJsonForDisplay(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (_error) {
    return value
  }
}

function validateLocation(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (form.value.inventoryTracked === '1' && !value) {
    callback(new Error('启用库存跟踪时必须配置库存仓库'))
    return
  }
  callback()
}

function validateQuantity(_rule: unknown, value: number, callback: (error?: Error) => void): void {
  if (form.value.inventoryTracked === '1' && (value === undefined || value === null || value < 0)) {
    callback(new Error('默认库存数量不能小于0'))
    return
  }
  callback()
}

function statusText(status?: string): string {
  const map: Record<string, string> = {
    CONNECTED: '已连接',
    DISCONNECTED: '未连接',
    EXPIRED: '已过期',
    TOKEN_EXPIRING_SOON: '即将过期'
  }
  return status ? map[status] || status : '-'
}

function statusTagType(status?: string): string {
  if (status === 'CONNECTED') {
    return 'success'
  }
  if (status === 'EXPIRED') {
    return 'danger'
  }
  return 'info'
}

getList()
</script>

<style scoped>
.input-with-action {
  width: 100%;
}

.inline-resource {
  display: flex;
  gap: 8px;
  width: 100%;
}

.operation-actions {
  align-items: center;
  display: inline-flex;
  gap: 12px;
  justify-content: center;
  line-height: 1;
}

.operation-actions :deep(.el-button) {
  margin-left: 0;
}

.operation-more {
  align-items: center;
  display: inline-flex;
}

.operation-more-button {
  align-items: center;
  display: inline-flex;
}

.api-call-editor {
  display: flex;
  gap: 8px;
  width: 100%;
}

.api-call-editor .el-button {
  align-self: flex-start;
}

.api-response {
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
  padding-top: 12px;
}

.api-response-meta {
  align-items: center;
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
}

.api-response-body {
  background: #f6f8fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  color: #1f2937;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
  line-height: 1.5;
  margin: 0;
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

:deep(.api-call-textarea textarea) {
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>
