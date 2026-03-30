<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关联商品主表ID" prop="productId">
        <el-input
          v-model="queryParams.productId"
          placeholder="请输入关联商品主表ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="Shopify平台变体ID" prop="shopifyVariantId">
        <el-input
          v-model="queryParams.shopifyVariantId"
          placeholder="请输入Shopify平台变体ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="SKU" prop="sku">
        <el-input
          v-model="queryParams.sku"
          placeholder="请输入SKU"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="销售价格(美元*100)" prop="price">
        <el-input
          v-model="queryParams.price"
          placeholder="请输入销售价格(美元*100)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="采购价" prop="purchasePrice">
        <el-input
          v-model="queryParams.purchasePrice"
          placeholder="请输入采购价"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="运费是否来自实际发货数据(0:否, 1:是)" prop="isActualShipment">
        <el-select v-model="queryParams.isActualShipment" placeholder="请选择运费是否来自实际发货数据(0:否, 1:是)" clearable>
          <el-option
            v-for="dict in dict.type.erp_product_variant_is_actual_shipment"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="daterangeCreateTime"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['erp:variant:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['erp:variant:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['erp:variant:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['erp:variant:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['erp:variant:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="variantList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="本地主键" align="center" v-if="columns[0].visible" prop="variantId" />
        <el-table-column label="关联商品主表ID" :show-overflow-tooltip="true" align="center" v-if="columns[1].visible" prop="productId" />
        <el-table-column label="Shopify平台变体ID" :show-overflow-tooltip="true" align="center" v-if="columns[2].visible" prop="shopifyVariantId" />
        <el-table-column label="SKU" :show-overflow-tooltip="true" align="center" v-if="columns[3].visible" prop="sku" />
        <el-table-column label="销售价格(美元*100)" :show-overflow-tooltip="true" align="center" v-if="columns[4].visible" prop="price" />
        <el-table-column label="原价/对比价(美元*100)" :show-overflow-tooltip="true" align="center" v-if="columns[5].visible" prop="compareAtPrice" />
        <el-table-column label="采购价" :show-overflow-tooltip="true" align="center" v-if="columns[6].visible" prop="purchasePrice" />
        <el-table-column label="采购链接" :show-overflow-tooltip="true" align="center" v-if="columns[7].visible" prop="purchaseUrl" />
        <el-table-column label="采购产品名称" :show-overflow-tooltip="true" align="center" v-if="columns[8].visible" prop="purchaseProductName" />
        <el-table-column label="变体对应的选项" :show-overflow-tooltip="true" align="center" v-if="columns[9].visible" prop="optionValues" />
        <el-table-column label="关联的图片ID (若有)" align="center" v-if="columns[10].visible" prop="mediaId" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.mediaId" :width="50" :height="50"/>
        </template>
      </el-table-column>
        <el-table-column label="排序位置 列表中的第一个位置是 1" :show-overflow-tooltip="true" align="center" v-if="columns[11].visible" prop="position" />
        <el-table-column label="包装宽度" :show-overflow-tooltip="true" align="center" v-if="columns[12].visible" prop="pkWidth" />
        <el-table-column label="包装高度" :show-overflow-tooltip="true" align="center" v-if="columns[13].visible" prop="pkHeight" />
        <el-table-column label="包装长度" :show-overflow-tooltip="true" align="center" v-if="columns[14].visible" prop="pkLength" />
        <el-table-column label="材积重" :show-overflow-tooltip="true" align="center" v-if="columns[15].visible" prop="materialWeight" />
        <el-table-column label="常规包装重量" :show-overflow-tooltip="true" align="center" v-if="columns[16].visible" prop="pkWeight" />
        <el-table-column label="运费" :show-overflow-tooltip="true" align="center" v-if="columns[17].visible" prop="freight" />
        <el-table-column label="运费是否来自实际发货数据(0:否, 1:是)" align="center" v-if="columns[18].visible" prop="isActualShipment">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.erp_product_variant_is_actual_shipment" :value="scope.row.isActualShipment"/>
        </template>
      </el-table-column>
        <el-table-column label="商品成本价" :show-overflow-tooltip="true" align="center" v-if="columns[19].visible" prop="unitCostPrice" />
        <el-table-column label="备注" :show-overflow-tooltip="true" align="center" v-if="columns[20].visible" prop="remark" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['erp:variant:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:variant:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改erp商品变体对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="关联商品主表ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入关联商品主表ID" />
        </el-form-item>
        <el-form-item label="Shopify平台变体ID" prop="shopifyVariantId">
          <el-input v-model="form.shopifyVariantId" placeholder="请输入Shopify平台变体ID" />
        </el-form-item>
        <el-form-item label="SKU" prop="sku">
          <el-input v-model="form.sku" placeholder="请输入SKU" />
        </el-form-item>
        <el-form-item label="销售价格(美元*100)" prop="price">
          <el-input v-model="form.price" placeholder="请输入销售价格(美元*100)" />
        </el-form-item>
        <el-form-item label="原价/对比价(美元*100)" prop="compareAtPrice">
          <el-input v-model="form.compareAtPrice" placeholder="请输入原价/对比价(美元*100)" />
        </el-form-item>
        <el-form-item label="采购价" prop="purchasePrice">
          <el-input v-model="form.purchasePrice" placeholder="请输入采购价" />
        </el-form-item>
        <el-form-item label="采购链接" prop="purchaseUrl">
          <el-input v-model="form.purchaseUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="采购产品名称" prop="purchaseProductName">
          <el-input v-model="form.purchaseProductName" placeholder="请输入采购产品名称" />
        </el-form-item>
        <el-form-item label="变体对应的选项" prop="optionValues">
          <el-input v-model="form.optionValues" placeholder="请输入变体对应的选项" />
        </el-form-item>
        <el-form-item label="关联的图片ID (若有)" prop="mediaId">
          <image-upload v-model="form.mediaId"/>
        </el-form-item>
        <el-form-item label="排序位置 列表中的第一个位置是 1" prop="position">
          <el-input v-model="form.position" placeholder="请输入排序位置 列表中的第一个位置是 1" />
        </el-form-item>
        <el-form-item label="包装宽度" prop="pkWidth">
          <el-input v-model="form.pkWidth" placeholder="请输入包装宽度" />
        </el-form-item>
        <el-form-item label="包装高度" prop="pkHeight">
          <el-input v-model="form.pkHeight" placeholder="请输入包装高度" />
        </el-form-item>
        <el-form-item label="包装长度" prop="pkLength">
          <el-input v-model="form.pkLength" placeholder="请输入包装长度" />
        </el-form-item>
        <el-form-item label="材积重" prop="materialWeight">
          <el-input v-model="form.materialWeight" placeholder="请输入材积重" />
        </el-form-item>
        <el-form-item label="常规包装重量" prop="pkWeight">
          <el-input v-model="form.pkWeight" placeholder="请输入常规包装重量" />
        </el-form-item>
        <el-form-item label="运费" prop="freight">
          <el-input v-model="form.freight" placeholder="请输入运费" />
        </el-form-item>
        <el-form-item label="运费是否来自实际发货数据(0:否, 1:是)" prop="isActualShipment">
          <el-radio-group v-model="form.isActualShipment">
            <el-radio
              v-for="dict in dict.type.erp_product_variant_is_actual_shipment"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="商品成本价" prop="unitCostPrice">
          <el-input v-model="form.unitCostPrice" placeholder="请输入商品成本价" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="删除标志 (0代表存在 2代表删除)" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志 (0代表存在 2代表删除)" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- erp商品变体导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的erp商品变体数据
          </div>
          <span>仅允许导入xls、xlsx格式文件。</span>
          <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="importTemplate">下载模板</el-link>
        </div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitFileForm">确 定</el-button>
        <el-button @click="upload.open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listVariant, getVariant, delVariant, addVariant, updateVariant, importVariant, importTemplateVariant } from "@/api/erp/variant";
import { getToken } from "@/utils/auth";

export default {
  name: "Variant",
  dicts: ['erp_product_variant_is_actual_shipment'],
  data() {
    return {
      //表格展示列
      columns: [
        { key: 0, label: '本地主键', visible: true },
          { key: 1, label: '关联商品主表ID', visible: true },
          { key: 2, label: 'Shopify平台变体ID', visible: true },
          { key: 3, label: 'SKU', visible: true },
          { key: 4, label: '销售价格(美元*100)', visible: true },
          { key: 5, label: '原价/对比价(美元*100)', visible: true },
          { key: 6, label: '采购价', visible: true },
          { key: 7, label: '采购链接', visible: true },
          { key: 8, label: '采购产品名称', visible: true },
          { key: 9, label: '变体对应的选项', visible: true },
          { key: 10, label: '关联的图片ID (若有)', visible: true },
          { key: 11, label: '排序位置 列表中的第一个位置是 1', visible: true },
          { key: 12, label: '包装宽度', visible: true },
          { key: 13, label: '包装高度', visible: true },
          { key: 14, label: '包装长度', visible: true },
          { key: 15, label: '材积重', visible: true },
          { key: 16, label: '常规包装重量', visible: true },
          { key: 17, label: '运费', visible: true },
          { key: 18, label: '运费是否来自实际发货数据(0:否, 1:是)', visible: true },
          { key: 19, label: '商品成本价', visible: true },
          { key: 20, label: '备注', visible: true },
        ],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // erp商品变体表格数据
      variantList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 删除标志 (0代表存在 2代表删除)时间范围
      daterangeCreateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        productId: null,
        shopifyVariantId: null,
        sku: null,
        price: null,
        purchasePrice: null,
        isActualShipment: null,
        createTime: null,
      },
      // 表单参数
      form: {},
      // 导出地址
      exportUrl: 'erp/variant/export',
      // erp商品变体导入参数
      upload: {
        // 是否显示弹出层（erp商品变体导入）
        open: false,
        // 弹出层标题（erp商品变体导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的erp商品变体数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/erp/variant/importData",
        // 下载模板的地址
        templateUrl: 'erp/variant/importTemplate'
      },
      // 表单校验
      rules: {
        productId: [
          { required: true, message: "关联商品主表ID不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询erp商品变体列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      listVariant(this.queryParams).then(response => {
        this.variantList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        variantId: null,
        productId: null,
        shopifyVariantId: null,
        sku: null,
        price: null,
        compareAtPrice: null,
        purchasePrice: null,
        purchaseUrl: null,
        purchaseProductName: null,
        optionValues: null,
        mediaId: null,
        position: null,
        pkWidth: null,
        pkHeight: null,
        pkLength: null,
        materialWeight: null,
        pkWeight: null,
        freight: null,
        isActualShipment: null,
        unitCostPrice: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null,
        delFlag: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.daterangeCreateTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.variantId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加erp商品变体";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const variantId = row.variantId || this.ids
      getVariant(variantId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改erp商品变体";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.variantId != null) {
            updateVariant(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addVariant(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const variantIds = row.variantId || this.ids;
      this.$modal.confirm('是否确认删除erp商品变体编号为"' + variantIds + '"的数据项？').then(function() {
        return delVariant(variantIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(this.exportUrl, {
        ...this.queryParams
      }, `variant_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "erp商品变体导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download(this.upload.templateUrl, {
      }, `variant_template_${new Date().getTime()}.xlsx`)
    },
    // 文件上传中处理
    handleFileUploadProgress(event, file, fileList) {
      this.upload.isUploading = true;
    },
    // 文件上传成功处理
    handleFileSuccess(response, file, fileList) {
      this.upload.open = false;
      this.upload.isUploading = false;
      this.$refs.upload.clearFiles();
      this.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    }
  }
};
</script>
