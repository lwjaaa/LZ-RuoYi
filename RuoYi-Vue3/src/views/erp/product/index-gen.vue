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
          placeholder="请输入SPU"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品类别ID (Category)" prop="category">
        <el-input
          v-model="queryParams.category"
          placeholder="请输入商品类别ID (Category)"
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
      <el-form-item label="发布状态 (0:草稿, 1:已发布, 2:归档)" prop="status">
        <el-select
          v-model="queryParams.status"
          style="width: 200px"
          placeholder="请选择发布状态 (0:草稿, 1:已发布, 2:归档)"
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
      <el-form-item
        label="同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)"
        prop="syncStatus"
      >
        <el-select
          v-model="queryParams.syncStatus"
          style="width: 200px"
          placeholder="请选择同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)"
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
      <el-form-item label="最后同步时间" style="width: 308px">
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
          >新增</el-button
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
        label="本地主键"
        align="center"
        prop="productId"
        v-if="columns[0].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="Shopify平台商品ID"
        align="center"
        prop="shopifyProductId"
        v-if="columns[1].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品标题"
        align="center"
        prop="productTitle"
        v-if="columns[2].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="SPU"
        align="center"
        prop="spu"
        v-if="columns[3].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品类别ID (Category)"
        align="center"
        prop="category"
        v-if="columns[4].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品类型"
        align="center"
        prop="productType"
        v-if="columns[5].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="来源URL"
        align="center"
        prop="sourceUrl"
        v-if="columns[6].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="采购链接"
        align="center"
        prop="purchaseUrl"
        v-if="columns[7].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="商品选项"
        align="center"
        prop="optionJson"
        v-if="columns[8].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="采购商品选项"
        align="center"
        prop="purchaseOptionJson"
        v-if="columns[9].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="发布状态 (0:草稿, 1:已发布, 2:归档)"
        align="center"
        prop="status"
        v-if="columns[10].visible"
      >
        <template #default="scope">
          <dict-tag :options="erp_product_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="商品详情描述 (HTML)"
        align="center"
        prop="bodyHtml"
        v-if="columns[11].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="主图ID，仅用户erp后台展示"
        align="center"
        prop="mainMediaId"
        width="100"
        v-if="columns[12].visible"
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
        label="同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)"
        align="center"
        prop="syncStatus"
        v-if="columns[13].visible"
      >
        <template #default="scope">
          <dict-tag
            :options="erp_product_sync_status"
            :value="scope.row.syncStatus"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="最后一次同步错误信息或结果"
        align="center"
        prop="syncMessage"
        v-if="columns[14].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="最后同步时间"
        align="center"
        prop="lastSyncTime"
        width="180"
        v-if="columns[15].visible"
        :show-overflow-tooltip="true"
      >
        <template #default="scope">
          <span>{{
            parseTime(scope.row.lastSyncTime, "{y}-{m}-{d} {h}:{i}:{s}")
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="乐观锁版本号"
        align="center"
        prop="version"
        v-if="columns[16].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="描述"
        align="center"
        prop="description"
        v-if="columns[17].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="大小"
        align="center"
        prop="size"
        v-if="columns[18].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="材质"
        align="center"
        prop="material"
        v-if="columns[19].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="备注"
        align="center"
        prop="note"
        v-if="columns[20].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="包含的包材"
        align="center"
        prop="packageInclude"
        v-if="columns[21].visible"
        :show-overflow-tooltip="true"
      />
      <el-table-column
        label="备注"
        align="center"
        prop="remark"
        v-if="columns[26].visible"
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
            >修改</el-button
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

    <!-- 添加或修改erp商品对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="productRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="Shopify平台商品ID" prop="shopifyProductId">
          <el-input
            v-model="form.shopifyProductId"
            placeholder="请输入Shopify平台商品ID"
          />
        </el-form-item>
        <el-form-item label="商品标题" prop="productTitle">
          <el-input v-model="form.productTitle" placeholder="请输入商品标题" />
        </el-form-item>
        <el-form-item label="SPU" prop="spu">
          <el-input v-model="form.spu" placeholder="请输入SPU" />
        </el-form-item>
        <el-form-item label="商品类别ID (Category)" prop="category">
          <el-input
            v-model="form.category"
            placeholder="请输入商品类别ID (Category)"
          />
        </el-form-item>
        <el-form-item label="商品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="请输入商品类型" />
        </el-form-item>
        <el-form-item label="来源URL" prop="sourceUrl">
          <el-input
            v-model="form.sourceUrl"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="采购链接" prop="purchaseUrl">
          <el-input
            v-model="form.purchaseUrl"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="发布状态 (0:草稿, 1:已发布, 2:归档)" prop="status">
          <el-select
            v-model="form.status"
            placeholder="请选择发布状态 (0:草稿, 1:已发布, 2:归档)"
          >
            <el-option
              v-for="dict in erp_product_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="商品详情描述 (HTML)" prop="bodyHtml">
          <el-input
            v-model="form.bodyHtml"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="主图ID，仅用户erp后台展示" prop="mainMediaId">
          <image-upload v-model="form.mainMediaId" />
        </el-form-item>
        <el-form-item
          label="同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)"
          prop="syncStatus"
        >
          <el-radio-group v-model="form.syncStatus">
            <el-radio
              v-for="dict in erp_product_sync_status"
              :key="dict.value"
              :value="dict.value"
              >{{ dict.label }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item label="最后一次同步错误信息或结果" prop="syncMessage">
          <el-input
            v-model="form.syncMessage"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="最后同步时间" prop="lastSyncTime">
          <el-date-picker
            clearable
            v-model="form.lastSyncTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择最后同步时间"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="乐观锁版本号" prop="version">
          <el-input v-model="form.version" placeholder="请输入乐观锁版本号" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="大小" prop="size">
          <el-input
            v-model="form.size"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="材质" prop="material">
          <el-input
            v-model="form.material"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input
            v-model="form.note"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="包含的包材" prop="packageInclude">
          <el-input
            v-model="form.packageInclude"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="删除标志 (0代表存在 2代表删除)" prop="delFlag">
          <el-input
            v-model="form.delFlag"
            placeholder="请输入删除标志 (0代表存在 2代表删除)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- erp商品导入对话框 -->
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
            <el-checkbox
              v-model="upload.updateSupport"
            />是否更新已经存在的erp商品数据
            <div>仅允许导入xls、xlsx格式文件。</div>
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

<script setup name="Product">
import { listProduct, getProduct, delProduct, addProduct, updateProduct, importProduct, importTemplateProduct } from "@/api/erp/product";
import { getToken } from "@/utils/auth";
import { UploadFilled } from '@element-plus/icons-vue';

const { proxy } = getCurrentInstance();
    const { erp_product_sync_status, erp_product_status } = proxy.useDict('erp_product_sync_status', 'erp_product_status');

const productList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
        const daterangeLastSyncTime = ref([]);
        const daterangeCreateTime = ref([]);

const data = reactive({
  form: {},
  // 导出地址
  exportUrl: 'erp/product/export',
  // erp商品导入参数
  upload: {
    // 是否显示弹出层（erp商品导入）
    open: false,
    // 弹出层标题（erp商品导入）
    title: "",
    // 是否禁用上传
    isUploading: false,
    // 是否更新已经存在的erp商品数据
    updateSupport: 0,
    // 设置上传的请求头部
    headers: { Authorization: "Bearer " + getToken() },
    // 上传的地址
    url: import.meta.env.VITE_APP_BASE_API + "/erp/product/importData",
    // 下载模板的地址
    templateUrl: 'erp/product/importTemplate'
  },
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
  },
  rules: {
                  spu: [
              { required: true, message: "SPU不能为空", trigger: "blur" }
            ],
  },
  //表格展示列
  columns: [
            { key: 0, label: '本地主键', visible: true },
              { key: 1, label: 'Shopify平台商品ID', visible: true },
              { key: 2, label: '商品标题', visible: true },
              { key: 3, label: 'SPU', visible: true },
              { key: 4, label: '商品类别ID (Category)', visible: true },
              { key: 5, label: '商品类型', visible: true },
              { key: 6, label: '来源URL', visible: true },
              { key: 7, label: '采购链接', visible: true },
              { key: 8, label: '商品选项', visible: true },
              { key: 9, label: '采购商品选项', visible: true },
              { key: 10, label: '发布状态 (0:草稿, 1:已发布, 2:归档)', visible: true },
              { key: 11, label: '商品详情描述 (HTML)', visible: true },
              { key: 12, label: '主图ID，仅用户erp后台展示', visible: true },
              { key: 13, label: '同步状态 (0:未同步, 1:同步成功, 2:同步失败, 3:同步中)', visible: true },
              { key: 14, label: '最后一次同步错误信息或结果', visible: true },
              { key: 15, label: '最后同步时间', visible: true },
              { key: 16, label: '乐观锁版本号', visible: true },
              { key: 17, label: '描述', visible: true },
              { key: 18, label: '大小', visible: true },
              { key: 19, label: '材质', visible: true },
              { key: 20, label: '备注', visible: true },
              { key: 21, label: '包含的包材', visible: true },
              { key: 22, label: '备注', visible: true },
    ],
});

const { queryParams, form, rules, columns, exportUrl, upload } = toRefs(data);
const uploadRef = ref();

/** 查询erp商品列表 */
function getList() {
  loading.value = true;
          queryParams.value.params = {};
          if (null != daterangeLastSyncTime && '' != daterangeLastSyncTime) {
            queryParams.value.params["beginLastSyncTime"] = daterangeLastSyncTime.value[0];
            queryParams.value.params["endLastSyncTime"] = daterangeLastSyncTime.value[1];
          }
          if (null != daterangeCreateTime && '' != daterangeCreateTime) {
            queryParams.value.params["beginCreateTime"] = daterangeCreateTime.value[0];
            queryParams.value.params["endCreateTime"] = daterangeCreateTime.value[1];
          }
  listProduct(queryParams.value).then(response => {
          productList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
                  productId: null,
                  shopifyProductId: null,
                  productTitle: null,
                  spu: null,
                  category: null,
                  productType: null,
                  sourceUrl: null,
                  purchaseUrl: null,
                  optionJson: null,
                  purchaseOptionJson: null,
                  status: null,
                  bodyHtml: null,
                  mainMediaId: null,
                  syncStatus: null,
                  syncMessage: null,
                  lastSyncTime: null,
                  version: null,
                  description: null,
                  size: null,
                  material: null,
                  note: null,
                  packageInclude: null,
                  createBy: null,
                  createTime: null,
                  updateBy: null,
                  updateTime: null,
                  remark: null,
                  delFlag: null
  };
  proxy.resetForm("productRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
          daterangeLastSyncTime.value = [];
          daterangeCreateTime.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.productId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加erp商品";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _productId = row.productId || ids.value
  getProduct(_productId).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改erp商品";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      if (form.value.productId != null) {
        updateProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _productIds = row.productId || ids.value;
  proxy.$modal.confirm('是否确认删除erp商品编号为"' + _productIds + '"的数据项？').then(function() {
    return delProduct(_productIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(exportUrl.value, {
    ...queryParams.value
  }, `product_${new Date().getTime()}.xlsx`)
}
/** 导入按钮操作 */
function handleImport() {
  upload.value.title = "erp商品导入";
  upload.value.open = true;
}
/** 下载模板操作 */
function importTemplate() {
  proxy.download(upload.value.templateUrl, {
  }, `product_template_${new Date().getTime()]]}.xlsx`)
}
// 文件上传中处理
function handleFileUploadProgress(event, file, fileList) {
  upload.value.isUploading = true;
}
// 文件上传成功处理
function handleFileSuccess(response, file, fileList) {
  upload.value.open = false;
  upload.value.isUploading = false;
  uploadRef.value.clearFiles();
  proxy.#[[$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
}
// 提交上传文件
function submitFileForm() {
  uploadRef.value.submit();
}

getList();
</script>
