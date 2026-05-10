<template>
  <div class="app-container product-list-panel">
    <!-- 搜索区域 -->
    <div class="search-section">
      <el-form
        :model="queryParams"
        ref="queryRef"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <!-- 主要查询条件 - 默认展示 -->
        <el-form-item label="商品标题" prop="productTitle">
          <el-input
            v-model="queryParams.productTitle"
            placeholder="请输入商品标题"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="SPU" prop="spu">
          <el-input
            v-model="queryParams.spu"
            placeholder="请输入 SPU"
            clearable
            style="width: 120px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="发布状态" prop="status">
          <el-select
            v-model="queryParams.status"
            style="width: 140px"
            placeholder="请选择"
            clearable
          >
            <el-option
              v-for="dict in erp_product_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery"
            >搜索</el-button
          >
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button
            type="primary"
            @click="expandSearchVisible = !expandSearchVisible"
          >
            {{ expandSearchVisible ? "收起" : "展开" }}
            <el-icon class="el-icon--right">
              <ArrowUp v-if="expandSearchVisible" />
              <ArrowDown v-else />
            </el-icon>
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 次要查询条件 - 可折叠 -->
      <el-collapse-transition>
        <el-form
          v-show="expandSearchVisible"
          :model="queryParams"
          :inline="true"
          label-width="68px"
          class="secondary-search"
        >
          <el-form-item label="商品类别" prop="category">
            <el-input
              v-model="queryParams.category"
              placeholder="请输入商品类别"
              clearable
              style="width: 120px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="商品类型" prop="productType">
            <el-select
              v-model="queryParams.productType"
              style="width: 140px"
              placeholder="请选择"
              clearable
            >
              <el-option label="普通商品" value="0" />
              <el-option label="变体商品" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="同步状态" prop="syncStatus">
            <el-select
              v-model="queryParams.syncStatus"
              style="width: 140px"
              placeholder="请选择"
              clearable
            >
              <el-option
                v-for="dict in erp_product_sync_status"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="同步时间" style="width: 280px">
            <el-date-picker
              v-model="daterangeLastSyncTime"
              value-format="YYYY-MM-DD"
              type="daterange"
              range-separator="-"
              start-placeholder="开始"
              end-placeholder="结束"
              style="width: 240px"
            />
          </el-form-item>
        </el-form>
      </el-collapse-transition>
    </div>

    <!-- 操作按钮区域 - 固定左侧 -->
    <el-row :gutter="10" class="mb8 action-bar">
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['erp:product:add']"
          >选品</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['erp:product:edit']"
          >修改</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['erp:product:remove']"
          >删除</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['erp:product:import']"
          >导入</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['erp:product:export']"
          >导出</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="success"
          plain
          icon="Promotion"
          @click="handlePushToShopify"
          v-hasPermi="['erp:product:push']"
          >推送 Shopify</el-button
        >
      </el-col>
      <el-col :span="1.5" class="fixed-actions">
        <el-button
          type="primary"
          plain
          icon="Upload"
          @click="handlePublish"
          v-hasPermi="['erp:product:push']"
          >发布</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
        :columns="columns"
      />
    </el-row>

    <!-- 商品表格 -->
    <el-table
      v-loading="loading"
      :data="productList"
      @selection-change="handleSelectionChange"
      @expand-change="handleExpandChange"
      :row-key="(row) => row.productId"
      border
      style="width: 100%"
      :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
    >
      <el-table-column type="selection" width="55" align="center" fixed />
      <el-table-column type="expand" width="50" fixed>
        <template #default="props">
          <div class="variant-expand">
            <h4>变体信息</h4>
            <el-table
              v-if="props.row.variants && props.row.variants.length > 0"
              :data="props.row.variants"
              size="small"
              border
              style="width: 80%"
            >
              <el-table-column
                label="SKU"
                align="center"
                prop="sku"
                width="150"
                show-overflow-tooltip
              />
              <el-table-column
                label="规格"
                align="center"
                prop="optionValues"
                width="200"
                show-overflow-tooltip
              >
                <template #default="scope">
                  {{ formatVariantOptions(scope.row) }}
                </template>
              </el-table-column>
              <el-table-column
                label="采购价(¥)"
                align="center"
                prop="purchasePrice"
                width="100"
              />
              <el-table-column
                label="销售价($)"
                align="center"
                prop="salePrice"
                width="100"
              />
              <el-table-column
                label="库存"
                align="center"
                prop="stock"
                width="80"
              />
              <el-table-column
                label="重量(kg)"
                align="center"
                prop="weight"
                width="90"
              />
              <el-table-column
                label="状态"
                align="center"
                prop="status"
                width="80"
              >
                <template #default="scope">
                  <el-tag
                    :type="scope.row.status === '0' ? 'success' : 'info'"
                    size="small"
                  >
                    {{ scope.row.status === "0" ? "启用" : "禁用" }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无变体数据" />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="SPU"
        align="center"
        prop="spu"
        width="120"
        v-if="colSpu.visible"
        show-overflow-tooltip
      />
      <el-table-column
        label="商品标题"
        align="left"
        prop="productTitle"
        min-width="200"
        v-if="colProductTitle.visible"
        show-overflow-tooltip
      />
      <el-table-column
        label="主图"
        align="center"
        prop="mainMediaId"
        width="80"
        v-if="colMainMedia.visible"
      >
        <template #default="scope">
          <image-preview
            :src="scope.row.mainMediaId"
            :width="50"
            :height="50"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="类别"
        align="center"
        prop="category"
        width="100"
        v-if="colCategory.visible"
        show-overflow-tooltip
      />

      <el-table-column
        label="发布状态"
        align="center"
        prop="status"
        width="90"
        v-if="colStatus.visible"
      >
        <template #default="scope">
          <dict-tag :options="erp_product_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="同步状态"
        align="center"
        prop="syncStatus"
        width="90"
        v-if="colSyncStatus.visible"
      >
        <template #default="scope">
          <dict-tag
            :options="erp_product_sync_status"
            :value="scope.row.syncStatus"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="同步时间"
        align="center"
        prop="lastSyncTime"
        width="150"
        v-if="colLastSyncTime.visible"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.lastSyncTime, "{y}-{m}-{d} {h}:{i}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="采购链接"
        align="center"
        prop="purchaseUrl"
        width="120"
        v-if="colPurchaseUrl.visible"
        show-overflow-tooltip
      />
      <el-table-column
        label="备注"
        align="left"
        prop="remark"
        min-width="150"
        v-if="colRemark.visible"
        show-overflow-tooltip
      />

      <!-- 动态显示的其他列 -->
      <el-table-column
        v-for="col in dynamicColumns"
        :key="col.key"
        :label="col.label"
        :align="col.label === 'DESCRIPTION' ? 'left' : 'center'"
        :prop="col.prop"
        :width="col.width || 120"
        show-overflow-tooltip
      />

      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['erp:product:edit']"
            >编辑</el-button
          >
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:product:remove']"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <div
        class="selected-tags"
        v-show="selectedTags && selectedTags.length > 0"
      >
        <span class="tag-label">已选标签：</span>
        <el-tag
          v-for="tag in selectedTags"
          :key="tag.tagId"
          size="small"
          closable
          @close="handleRemoveTag(tag)"
        >
          {{ tag.tagName }}
        </el-tag>
      </div>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- Shopify 店铺选择对话框 -->
    <el-dialog
      :title="storeAction.type === 'push' ? '推送 Shopify' : '发布商品'"
      v-model="storeAction.open"
      width="460px"
      append-to-body
    >
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

    <el-dialog
      :title="upload.title"
      v-model="upload.open"
      width="400px"
      append-to-body
    >
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
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的 erp
            商品数据
            <div>仅允许导入 xls、xlsx 格式文件。</div>
            <el-link
              type="primary"
              :underline="false"
              style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
              >下载模板</el-link
            >
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  reactive,
  toRefs,
  onMounted,
  getCurrentInstance,
  computed,
} from "vue";
import { ElMessageBox } from "element-plus";
import { listProduct, getProduct, delProduct, pushBatch, getPushResult, publishProducts } from "@/api/erp/product";
import { listActiveStores } from "@/api/erp/store";
import { getToken } from "@/utils/auth";
import { parseTime } from "@/utils/ruoyi";
import { UploadFilled, ArrowDown, ArrowUp, Promotion } from "@element-plus/icons-vue";
import type { Product, PageQuery, ShopifyStore } from "@/types/erp";

interface ProductQuery extends PageQuery {
  productTitle?: string | null;
  spu?: string | null;
  category?: string | null;
  productType?: string | null;
  status?: string | null;
  mainMediaId?: number | null;
  syncStatus?: string | null;
  lastSyncTime?: string | null;
  params?: Record<string, unknown>;
}

interface ColumnConfig {
  key: number;
  label: string;
  prop?: string;
  visible: boolean;
  width?: number;
}

interface UploadConfig {
  open: boolean;
  title: string;
  isUploading: boolean;
  updateSupport: number;
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

const emit = defineEmits<{
  (e: "remove-tag", tag: SelectedTag): void;
}>();

const props = defineProps<{
  selectedTags?: SelectedTag[];
}>();

const { proxy } = getCurrentInstance() as any;
const { erp_product_sync_status, erp_product_status } = proxy.useDict(
  "erp_product_sync_status",
  "erp_product_status",
);

const productList = ref<Product[]>([]);
const loading = ref<boolean>(true);
const showSearch = ref<boolean>(true);
const ids = ref<number[]>([]);
const single = ref<boolean>(true);
const multiple = ref<boolean>(true);
const total = ref<number>(0);
const daterangeLastSyncTime = ref<string[]>([]);
const expandSearchVisible = ref<boolean>(false);
const activeStores = ref<ShopifyStore[]>([]);
const storeAction = reactive({
  open: false,
  type: "push" as "push" | "publish",
  desc: "",
  storeId: undefined as number | undefined,
  loading: false,
  submitting: false,
});

const creationWizardModal = ref<any>(null);

const data = reactive({
  exportUrl: "erp/product/export",
  upload: {
    open: false,
    title: "",
    isUploading: false,
    updateSupport: 0,
    headers: { Authorization: "Bearer " + getToken() },
    url: import.meta.env.VITE_APP_BASE_API + "/erp/product/importData",
    templateUrl: "erp/product/importTemplate",
  } as UploadConfig,
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    productTitle: null,
    spu: null,
    category: null,
    productType: null,
    status: null,
    mainMediaId: null,
    syncStatus: null,
    lastSyncTime: null,
  } as ProductQuery,
  columns: [
    { key: 0, label: "SPU", prop: "spu", visible: true },
    { key: 1, label: "商品标题", prop: "productTitle", visible: true },
    { key: 2, label: "主图", prop: "mainMediaId", visible: true },
    { key: 3, label: "商品类别", prop: "category", visible: true },
    { key: 4, label: "商品类型", prop: "productType", visible: true },
    { key: 5, label: "来源 URL", prop: "sourceUrl", visible: false },
    { key: 6, label: "采购链接", prop: "purchaseUrl", visible: true },
    { key: 7, label: "商品选项", prop: "optionJson", visible: false },
    {
      key: 8,
      label: "采购商品选项",
      prop: "purchaseOptionJson",
      visible: false,
    },
    { key: 9, label: "创建时间", prop: "createTime", visible: false },
    { key: 10, label: "发布状态", prop: "status", visible: true },
    { key: 11, label: "同步状态", prop: "syncStatus", visible: true },
    { key: 12, label: "同步时间", prop: "lastSyncTime", visible: true },
    { key: 13, label: "备注", prop: "remark", visible: true },
    {
      key: 14,
      label: "DESCRIPTION",
      prop: "description",
      visible: false,
      width: 150,
    },
    { key: 15, label: "SIZE", prop: "size", visible: false },
    { key: 16, label: "MATERIAL", prop: "material", visible: false },
    { key: 17, label: "NOTE", prop: "note", visible: false },
    {
      key: 18,
      label: "PACKAGEINCLUDE",
      prop: "packageInclude",
      visible: false,
    },
  ] as ColumnConfig[],
});

// 动态列 - 只显示配置为可见的且不在主要列中的列
const dynamicColumns = computed(() => {
  const mainKeys = [0, 1, 2, 3, 4, 6, 10, 11, 12, 13];
  return data.columns.filter(
    (col) => !mainKeys.includes(col.key) && col.visible,
  );
});

// 主列可见性 - 使用 computed 安全获取
const colSpu = computed(
  () => data.columns.find((c) => c.key === 0) || { visible: false },
);
const colProductTitle = computed(
  () => data.columns.find((c) => c.key === 1) || { visible: false },
);
const colMainMedia = computed(
  () => data.columns.find((c) => c.key === 2) || { visible: false },
);
const colCategory = computed(
  () => data.columns.find((c) => c.key === 3) || { visible: false },
);
const colProductType = computed(
  () => data.columns.find((c) => c.key === 4) || { visible: false },
);
const colPurchaseUrl = computed(
  () => data.columns.find((c) => c.key === 6) || { visible: false },
);
const colStatus = computed(
  () => data.columns.find((c) => c.key === 10) || { visible: false },
);
const colSyncStatus = computed(
  () => data.columns.find((c) => c.key === 11) || { visible: false },
);
const colLastSyncTime = computed(
  () => data.columns.find((c) => c.key === 12) || { visible: false },
);
const colRemark = computed(
  () => data.columns.find((c) => c.key === 13) || { visible: false },
);

const { queryParams, columns, exportUrl, upload } = toRefs(data);
const uploadRef = ref<any>();

function getList(): void {
  loading.value = true;
  queryParams.value.params = {};
  if (
    null != daterangeLastSyncTime.value &&
    daterangeLastSyncTime.value.length > 0
  ) {
    queryParams.value.params["beginLastSyncTime"] =
      daterangeLastSyncTime.value[0];
    queryParams.value.params["endLastSyncTime"] =
      daterangeLastSyncTime.value[1];
  }
  listProduct(queryParams.value as ProductQuery).then((response: any) => {
    productList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery(): void {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery(): void {
  daterangeLastSyncTime.value = [];
  proxy.resetForm("queryRef");
  queryParams.value.category = null;
  queryParams.value.productType = null;
  queryParams.value.syncStatus = null;
  handleQuery();
}

function handleSelectionChange(selection: Product[]): void {
  ids.value = selection.map((item) => item.productId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleExpandChange(row: Product, expanded: boolean): void {
  console.log("展开行变化:", row.productId, expanded);
}

function formatVariantOptions(variant: any): string {
  if (!variant.optionValues) return "-";
  try {
    const options = JSON.parse(variant.optionValues);
    return Object.entries(options)
      .map(([key, value]) => `${key}: ${value}`)
      .join(", ");
  } catch {
    return variant.optionValues || "-";
  }
}

function handleRemoveTag(tagToRemove: SelectedTag): void {
  emit("remove-tag", tagToRemove);
}

function handleAdd(): void {
  const selectedTagIds = props.selectedTags?.map((tag) => tag.tagId) || [];
  proxy.$tab.openPage("新增选品", "/erp/product/creation-wizard/create", {
    tagIds: JSON.stringify(selectedTagIds),
  });
}

function handleUpdate(row: Product): void {
  const _productId = row.productId || ids.value[0];
  console.log("编辑 erp 商品，productId:", _productId, "row", row);
  proxy.$tab.openPage(
    "编辑商品",
    "/erp/product/creation-wizard/edit/" + _productId,
    { step: 1 },
  );
}

function handleWizardSubmit({
  action,
  hasChanged,
}: {
  action: string;
  hasChanged: boolean;
}): void {
  console.log("向导提交处理", action, hasChanged);
  if (action === "cancel") {
    console.log("用户取消操作");
  } else {
    if (hasChanged) {
      getList();
      proxy.$modal.msgSuccess("保存成功");
    }
  }
}

function handleDelete(row: Product): void {
  const _productIds = row.productId || ids.value;
  proxy.$modal
    .confirm('是否确认删除 erp 商品编号为"' + _productIds + '"的数据项？')
    .then(function () {
      return delProduct(_productIds as number);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

function handleExport(): void {
  proxy.download(
    exportUrl.value,
    {
      ...queryParams.value,
    },
    `product_${new Date().getTime()}.xlsx`,
  );
}

function handleImport(): void {
  upload.value.title = "erp 商品导入";
  upload.value.open = true;
}

function importTemplate(): void {
  proxy.download(
    upload.value.templateUrl,
    {},
    `product_template_${new Date().getTime()}.xlsx`,
  );
}

function loadActiveStores(): Promise<void> {
  storeAction.loading = true;
  return listActiveStores()
    .then((res: any) => {
      activeStores.value = res.data || [];
      if (!storeAction.storeId || !activeStores.value.some((store) => store.storeId === storeAction.storeId)) {
        storeAction.storeId = activeStores.value.find((store) => store.isDefault === "1")?.storeId || activeStores.value[0]?.storeId;
      }
    })
    .finally(() => {
      storeAction.loading = false;
    });
}

function openStoreAction(type: "push" | "publish", desc: string): void {
  storeAction.type = type;
  storeAction.desc = desc;
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
  if (storeAction.type === "push") {
    const isSelected = ids.value.length > 0;
    pushBatch({
      productQuery: isSelected ? undefined : queryParams.value,
      productIds: isSelected ? ids.value : undefined,
      storeId: storeAction.storeId,
    })
      .then((res: any) => {
        storeAction.open = false;
        const taskId = res.data?.taskId || res.taskId;
        if (taskId) {
          proxy.$modal.msgSuccess("已创建推送任务，任务ID: " + taskId);
          openTaskProgress(taskId);
        } else {
          proxy.$modal.msgWarning("没有可推送的商品");
        }
      })
      .finally(() => {
        storeAction.submitting = false;
      });
    return;
  }

  publishProducts({
    productIds: ids.value,
    storeId: storeAction.storeId,
  })
    .then((res: any) => {
      storeAction.open = false;
      if (res.data) {
        const { successCount, failedCount } = res.data;
        proxy.$modal.msgSuccess(`发布完成：成功 ${successCount}，失败 ${failedCount}`);
        getList();
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
  openStoreAction("push", isSelected ? `已勾选的 ${count} 个商品` : `当前筛选的 ${count} 个商品`);
}

function openTaskProgress(taskId: number): void {
  ElMessageBox.confirm(`推送任务已创建，任务ID：${taskId}。可前往推送任务页面查看进度。`, "推送任务已创建", {
    confirmButtonText: "打开任务页面",
    cancelButtonText: "稍后查看",
    type: "info",
  }).then(() => {
    proxy.$tab.openPage("Shopify 推送任务", "/erp/task");
  }).catch(() => {});
}

function handlePublish(): void {
  if (ids.value.length === 0) {
    proxy.$modal.msgWarning("请先勾选要发布的商品");
    return;
  }
  const count = ids.value.length;
  openStoreAction("publish", `已勾选的 ${count} 个商品`);
}

function handleFileUploadProgress(event: any, file: any, fileList: any): void {
  upload.value.isUploading = true;
}

function handleFileSuccess(response: any, file: any, fileList: any): void {
  upload.value.open = false;
  upload.value.isUploading = false;
  uploadRef.value.clearFiles();
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
      response.msg +
      "</div>",
    "导入结果",
    { dangerouslyUseHTMLString: true },
  );
  getList();
}

function submitFileForm(): void {
  uploadRef.value.submit();
}

onMounted(() => {
  getList();
  loadActiveStores();
});

defineExpose({
  refresh: getList,
});
</script>

<style scoped lang="scss">
.product-list-panel {
  .search-section {
    background: #fff;
    padding: 16px;
    border-radius: 4px;
    margin-bottom: 12px;
  }

  .secondary-search {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #ebeef5;
  }

  .action-bar {
    margin-bottom: 12px;
    position: sticky;
    left: 0;
    background: #fff;
    z-index: 10;
    padding: 8px 0;
  }

  .fixed-actions {
    flex-shrink: 0;
  }

  .variant-expand {
    padding: 12px 20px;
    background: #fafafa;

    h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #303133;
    }
  }

  :deep(.el-table) {
    .el-table__header th {
      font-weight: 600;
    }
    .el-table__row:hover {
      background-color: #f5f7fa;
    }
  }

  .pagination-wrapper {
    display: flex;
    align-items: end;
    justify-content: space-between;
    width: 100%;
  }

  .pagination-wrapper :deep(.pagination-container) {
    margin-left: auto;
  }

  .selected-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .tag-label {
    font-size: 12px;
    color: #6c757d;
    white-space: nowrap;
  }
}
</style>
