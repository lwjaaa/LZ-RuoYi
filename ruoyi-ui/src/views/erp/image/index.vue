<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="关联商品ID" prop="productId">
        <el-input
          v-model="queryParams.productId"
          placeholder="请输入关联商品ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文件名" prop="filename">
        <el-input
          v-model="queryParams.filename"
          placeholder="请输入文件名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="替代文本" prop="alt">
        <el-input
          v-model="queryParams.alt"
          placeholder="请输入替代文本"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="排序" prop="position">
        <el-input
          v-model="queryParams.position"
          placeholder="请输入排序"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['vh-erp:image:add']"
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
          v-hasPermi="['vh-erp:image:edit']"
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
          v-hasPermi="['vh-erp:image:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['vh-erp:image:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['vh-erp:image:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="imageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="本地主键" align="center" v-if="columns[0].visible" prop="imageId" />
        <el-table-column label="关联商品ID" :show-overflow-tooltip="true" align="center" v-if="columns[1].visible" prop="productId" />
        <el-table-column label="Shopify图片ID" :show-overflow-tooltip="true" align="center" v-if="columns[2].visible" prop="shopifyImageId" />
        <el-table-column label="图片URL" :show-overflow-tooltip="true" align="center" v-if="columns[3].visible" prop="shopifyUrl" />
        <el-table-column label="shopify的暂存上传URL" :show-overflow-tooltip="true" align="center" v-if="columns[4].visible" prop="stagedUploadUrl" />
        <el-table-column label="本地图片URL" :show-overflow-tooltip="true" align="center" v-if="columns[5].visible" prop="nasUrl" />
        <el-table-column label="文件名" :show-overflow-tooltip="true" align="center" v-if="columns[6].visible" prop="filename" />
        <el-table-column label="替代文本" :show-overflow-tooltip="true" align="center" v-if="columns[7].visible" prop="alt" />
        <el-table-column label="排序" :show-overflow-tooltip="true" align="center" v-if="columns[8].visible" prop="position" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['vh-erp:image:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['vh-erp:image:remove']"
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

    <!-- 添加或修改erp图片对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="关联商品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入关联商品ID" />
        </el-form-item>
        <el-form-item label="Shopify图片ID" prop="shopifyImageId">
          <el-input v-model="form.shopifyImageId" placeholder="请输入Shopify图片ID" />
        </el-form-item>
        <el-form-item label="图片URL" prop="shopifyUrl">
          <el-input v-model="form.shopifyUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="shopify的暂存上传URL" prop="stagedUploadUrl">
          <el-input v-model="form.stagedUploadUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="本地图片URL" prop="nasUrl">
          <el-input v-model="form.nasUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="文件名" prop="filename">
          <el-input v-model="form.filename" placeholder="请输入文件名" />
        </el-form-item>
        <el-form-item label="替代文本" prop="alt">
          <el-input v-model="form.alt" placeholder="请输入替代文本" />
        </el-form-item>
        <el-form-item label="排序" prop="position">
          <el-input v-model="form.position" placeholder="请输入排序" />
        </el-form-item>
        <el-form-item label="${comment}" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入${comment}" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- erp图片导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的erp图片数据
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
import { listImage, getImage, delImage, addImage, updateImage, importImage, importTemplateImage } from "@/api/erp/image";
import { getToken } from "@/utils/auth";

export default {
  name: "Image",
  data() {
    return {
      //表格展示列
      columns: [
        { key: 0, label: '本地主键', visible: true },
          { key: 1, label: '关联商品ID', visible: true },
          { key: 2, label: 'Shopify图片ID', visible: true },
          { key: 3, label: '图片URL', visible: true },
          { key: 4, label: 'shopify的暂存上传URL', visible: true },
          { key: 5, label: '本地图片URL', visible: true },
          { key: 6, label: '文件名', visible: true },
          { key: 7, label: '替代文本', visible: true },
          { key: 8, label: '排序', visible: true },
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
      // erp图片表格数据
      imageList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // $comment时间范围
      daterangeCreateTime: [],
      // $comment时间范围
      daterangeUpdateTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        productId: null,
        filename: null,
        alt: null,
        position: null,
      },
      // 表单参数
      form: {},
      // 导出地址
      exportUrl: 'vh-erp/image/export',
      // erp图片导入参数
      upload: {
        // 是否显示弹出层（erp图片导入）
        open: false,
        // 弹出层标题（erp图片导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的erp图片数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/vh-erp/image/importData",
        // 下载模板的地址
        templateUrl: 'vh-erp/image/importTemplate'
      },
      // 表单校验
      rules: {
        productId: [
          { required: true, message: "关联商品ID不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询erp图片列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeCreateTime && '' != this.daterangeCreateTime) {
        this.queryParams.params["beginCreateTime"] = this.daterangeCreateTime[0];
        this.queryParams.params["endCreateTime"] = this.daterangeCreateTime[1];
      }
      if (null != this.daterangeUpdateTime && '' != this.daterangeUpdateTime) {
        this.queryParams.params["beginUpdateTime"] = this.daterangeUpdateTime[0];
        this.queryParams.params["endUpdateTime"] = this.daterangeUpdateTime[1];
      }
      listImage(this.queryParams).then(response => {
        this.imageList = response.rows;
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
        imageId: null,
        productId: null,
        shopifyImageId: null,
        shopifyUrl: null,
        stagedUploadUrl: null,
        nasUrl: null,
        filename: null,
        alt: null,
        position: null,
        createTime: null,
        updateTime: null,
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
      this.daterangeUpdateTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.imageId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加erp图片";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const imageId = row.imageId || this.ids
      getImage(imageId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改erp图片";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.imageId != null) {
            updateImage(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addImage(this.form).then(response => {
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
      const imageIds = row.imageId || this.ids;
      this.$modal.confirm('是否确认删除erp图片编号为"' + imageIds + '"的数据项？').then(function() {
        return delImage(imageIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(this.exportUrl, {
        ...this.queryParams
      }, `image_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "erp图片导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download(this.upload.templateUrl, {
      }, `image_template_${new Date().getTime()}.xlsx`)
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
