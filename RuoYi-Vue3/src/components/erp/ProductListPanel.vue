<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="商品标题" prop="productTitle">
        <el-input
          v-model="queryParams.productTitle"
          placeholder="请输入商品标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="SPU" prop="spu">
        <el-input
          v-model="queryParams.spu"
          placeholder="请输入 SPU"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品类别" prop="category">
        <el-input
          v-model="queryParams.category"
          placeholder="请输入商品类别"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品类型" prop="productType">
        <el-input
          v-model="queryParams.productType"
          placeholder="请输入商品类型"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="发布状态" prop="status">
        <el-select
          v-model="queryParams.status"
          style="width: 200px"
          placeholder="请选择发布状态"
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
      <el-form-item label="同步状态" prop="syncStatus">
        <el-select
          v-model="queryParams.syncStatus"
          style="width: 200px"
          placeholder="请选择同步状态"
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
      <el-form-item label="同步时间" style="width: 308px">
        <el-date-picker
          v-model="daterangeLastSyncTime"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery"
          >搜索</el-button
        >
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['erp:product:add']"
          >选品</el-button
        >
      </el-col>
      <el-col :span="1.5">
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
      <el-col :span="1.5">
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
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          v-hasPermi="['erp:product:import']"
          >导入</el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['erp:product:export']"
          >导出</el-button
        >
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
        :columns="columns"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="productList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column
        label="SPU"
        align="center"
        prop="spu"
        v-if="columns[0].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品标题"
        align="center"
        prop="productTitle"
        v-if="columns[1].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="主图"
        align="center"
        prop="mainMediaId"
        width="100"
        v-if="columns[2].visible"
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
        label="商品类别"
        align="center"
        prop="category"
        v-if="columns[3].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品类型"
        align="center"
        prop="productType"
        v-if="columns[4].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="来源 URL"
        align="center"
        prop="sourceUrl"
        v-if="columns[5].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="采购链接"
        align="center"
        prop="purchaseUrl"
        v-if="columns[6].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品选项"
        align="center"
        prop="optionJson"
        v-if="columns[7].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        v-if="columns[9].visible"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.lastSyncTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="发布状态"
        align="center"
        prop="status"
        v-if="columns[10].visible"
      >
        <template #default="scope">
          <dict-tag :options="erp_product_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="同步状态"
        align="center"
        prop="syncStatus"
        v-if="columns[11].visible"
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
        width="180"
        v-if="columns[12].visible"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.lastSyncTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      // 备注
      <el-table-column
        label="备注"
        align="left"
        prop="remark"
        width="250"
        v-if="columns[13].visible"
        :show-overflow-tooltip="true"
      />

      <el-table-column
        label="DESCRIPTION"
        align="center"
        prop="description"
        v-if="columns[14].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="SIZE"
        align="center"
        prop="size"
        v-if="columns[15].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="MATERIAL"
        align="center"
        prop="material"
        v-if="columns[16].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="NOTE"
        align="center"
        prop="note"
        v-if="columns[17].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="PACKAGEINCLUDE"
        align="center"
        prop="packageInclude"
        v-if="columns[18].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
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
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:product:remove']"
            >删除</el-button
          >
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

    <!-- 商品创建向导组件 -->
    <!-- <ProductCreationWizard
      v-model="wizardVisible"
      :edit-data="currentEditData"
      :selected-tag-ids="selectedTagIds"
      @submit="handleWizardSubmit"
      @refresh="getList"
    /> -->

    <!-- erp 商品导入对话框 -->
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
import { ref, reactive, toRefs, onMounted, getCurrentInstance } from "vue";
import { listProduct, getProduct, delProduct } from "@/api/erp/product";
import { getToken } from "@/utils/auth";
import { parseTime } from "@/utils/ruoyi";
import { UploadFilled } from "@element-plus/icons-vue";
import type { Product, PageQuery } from "@/types/erp";

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
  visible: boolean;
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
const daterangeCreateTime = ref<string[]>([]);

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
    { key: 0, label: "SPU", visible: true },
    { key: 1, label: "商品标题", visible: true },
    { key: 2, label: "主图", visible: true },
    { key: 3, label: "商品类别", visible: true },
    { key: 4, label: "商品类型", visible: true },
    { key: 5, label: "来源 URL", visible: true },
    { key: 6, label: "采购链接", visible: true },
    { key: 7, label: "商品选项", visible: true },
    { key: 8, label: "采购商品选项", visible: true },
    { key: 9, label: "创建时间", visible: true },
    { key: 10, label: "发布状态", visible: true },
    { key: 11, label: "同步状态", visible: true },
    { key: 12, label: "同步时间", visible: true },
    { key: 13, label: "备注", visible: true },
    { key: 14, label: "DESCRIPTION", visible: true },
    { key: 15, label: "SIZE", visible: true },
    { key: 16, label: "MATERIAL", visible: true },
    { key: 17, label: "NOTE", visible: true },
    { key: 18, label: "PACKAGEINCLUDE", visible: true },
  ] as ColumnConfig[],
});

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
  if (
    null != daterangeCreateTime.value &&
    daterangeCreateTime.value.length > 0
  ) {
    queryParams.value.params["beginCreateTime"] = daterangeCreateTime.value[0];
    queryParams.value.params["endCreateTime"] = daterangeCreateTime.value[1];
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
  daterangeCreateTime.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleSelectionChange(selection: Product[]): void {
  ids.value = selection.map((item) => item.productId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
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
});

defineExpose({
  refresh: getList,
});
</script>

<style scoped></style>
