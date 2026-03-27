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
      <el-form-item label="Shopify媒体ID" prop="shopifyMediaId">
        <el-input
          v-model="queryParams.shopifyMediaId"
          placeholder="请输入Shopify媒体ID"
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
          v-hasPermi="['erp:media:add']"
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
          v-hasPermi="['erp:media:edit']"
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
          v-hasPermi="['erp:media:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['erp:media:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['erp:media:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="mediaList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="${comment}" align="center" v-if="columns[0].visible" prop="mediaId" />
        <el-table-column label="关联商品ID" :show-overflow-tooltip="true" align="center" v-if="columns[1].visible" prop="productId" />
        <el-table-column label="Shopify媒体ID" :show-overflow-tooltip="true" align="center" v-if="columns[2].visible" prop="shopifyMediaId" />
        <el-table-column label="Shopify媒体URL" :show-overflow-tooltip="true" align="center" v-if="columns[3].visible" prop="shopifyMediaUrl" />
        <el-table-column label="shopify的暂存上传URL" :show-overflow-tooltip="true" align="center" v-if="columns[4].visible" prop="stagedUploadUrl" />
        <el-table-column label="本地nas的媒体URL" :show-overflow-tooltip="true" align="center" v-if="columns[5].visible" prop="nasMediaUrl" />
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
            v-hasPermi="['erp:media:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:media:remove']"
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

    <!-- 添加或修改erp媒体对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="关联商品ID" prop="productId">
          <el-input v-model="form.productId" placeholder="请输入关联商品ID" />
        </el-form-item>
        <el-form-item label="Shopify媒体ID" prop="shopifyMediaId">
          <el-input v-model="form.shopifyMediaId" placeholder="请输入Shopify媒体ID" />
        </el-form-item>
        <el-form-item label="Shopify媒体URL" prop="shopifyMediaUrl">
          <el-input v-model="form.shopifyMediaUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="shopify的暂存上传URL" prop="stagedUploadUrl">
          <el-input v-model="form.stagedUploadUrl" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="本地nas的媒体URL" prop="nasMediaUrl">
          <el-input v-model="form.nasMediaUrl" type="textarea" placeholder="请输入内容" />
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
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- erp媒体导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的erp媒体数据
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
import { listMedia, getMedia, delMedia, addMedia, updateMedia, importMedia, importTemplateMedia } from "@/api/erp/media";
import { getToken } from "@/utils/auth";

export default {
  name: "Media",
  data() {
    return {
      //表格展示列
      columns: [
        { key: 0, label: '${comment}', visible: true },
          { key: 1, label: '关联商品ID', visible: true },
          { key: 2, label: 'Shopify媒体ID', visible: true },
          { key: 3, label: 'Shopify媒体URL', visible: true },
          { key: 4, label: 'shopify的暂存上传URL', visible: true },
          { key: 5, label: '本地nas的媒体URL', visible: true },
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
      // erp媒体表格数据
      mediaList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        productId: null,
        shopifyMediaId: null,
        filename: null,
        alt: null,
      },
      // 表单参数
      form: {},
      // 导出地址
      exportUrl: 'erp/media/export',
      // erp媒体导入参数
      upload: {
        // 是否显示弹出层（erp媒体导入）
        open: false,
        // 弹出层标题（erp媒体导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的erp媒体数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/erp/media/importData",
        // 下载模板的地址
        templateUrl: 'erp/media/importTemplate'
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
    /** 查询erp媒体列表 */
    getList() {
      this.loading = true;
      listMedia(this.queryParams).then(response => {
        this.mediaList = response.rows;
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
        mediaId: null,
        productId: null,
        shopifyMediaId: null,
        shopifyMediaUrl: null,
        stagedUploadUrl: null,
        nasMediaUrl: null,
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
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.mediaId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加erp媒体";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const mediaId = row.mediaId || this.ids
      getMedia(mediaId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改erp媒体";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.mediaId != null) {
            updateMedia(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addMedia(this.form).then(response => {
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
      const mediaIds = row.mediaId || this.ids;
      this.$modal.confirm('是否确认删除erp媒体编号为"' + mediaIds + '"的数据项？').then(function() {
        return delMedia(mediaIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(this.exportUrl, {
        ...this.queryParams
      }, `media_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "erp媒体导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download(this.upload.templateUrl, {
      }, `media_template_${new Date().getTime()}.xlsx`)
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
