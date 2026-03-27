<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-tree
          :data="tagTree"
          :props="{ label: 'tagName', children: 'children' }"
          @node-click="handleNodeClick"
          highlight-current
          default-expand-all
        ></el-tree>
      </el-col>
      <el-col :span="18">
        <el-form
          :model="queryParams"
          ref="queryForm"
          size="small"
          :inline="true"
          v-show="showSearch"
          label-width="68px"
        >
          <el-form-item label="标签名称" prop="tagName">
            <el-input
              v-model="queryParams.tagName"
              placeholder="请输入标签名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="标签编码" prop="tagCode">
            <el-input
              v-model="queryParams.tagCode"
              placeholder="请输入标签编码"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="标签类型" prop="tagType">
            <el-select
              v-model="queryParams.tagType"
              placeholder="请选择标签类型"
              clearable
            >
              <el-option
                v-for="dict in dict.type.erp_tag_type"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="SPU 前缀" prop="spuPrefix">
            <el-input
              v-model="queryParams.spuPrefix"
              placeholder="请输入SPU 前缀"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="创建时间" prop="createTime">
            <el-date-picker
              clearable
              v-model="queryParams.createTime"
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="选择创建时间"
            >
            </el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              size="mini"
              @click="handleQuery"
              >搜索</el-button
            >
            <el-button icon="el-icon-refresh" size="mini" @click="resetQuery"
              >重置</el-button
            >
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
              v-hasPermi="['vh-erp:tag:add']"
              >新增</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="info"
              plain
              icon="el-icon-sort"
              size="mini"
              @click="toggleExpandAll"
              >展开/折叠</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="info"
              plain
              icon="el-icon-upload2"
              size="mini"
              @click="handleImport"
              v-hasPermi="['vh-erp:tag:import']"
              >导入</el-button
            >
          </el-col>
          <el-col :span="1.5">
            <el-button
              type="warning"
              plain
              icon="el-icon-download"
              size="mini"
              @click="handleExport"
              v-hasPermi="['vh-erp:tag:export']"
              >导出</el-button
            >
          </el-col>
          <right-toolbar
            :showSearch.sync="showSearch"
            @queryTable="getList"
            :columns="columns"
          ></right-toolbar>
        </el-row>

        <el-table
          v-if="refreshTable"
          v-loading="loading"
          :data="tagList"
          row-key="tagCode"
          :default-expand-all="isExpandAll"
          :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        >
          <el-table-column
            label="标签名称"
            :show-overflow-tooltip="true"
            v-if="columns[0].visible"
            prop="tagName"
          />
          <el-table-column
            label="标签编码"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[1].visible"
            prop="tagCode"
          />
          <el-table-column
            label="标签类型"
            align="center"
            v-if="columns[2].visible"
            prop="tagType"
          >
            <template slot-scope="scope">
              <dict-tag
                :options="dict.type.erp_tag_type"
                :value="scope.row.tagType"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="排序"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[3].visible"
            prop="sortOrder"
          />
          <el-table-column
            label="父级ID (0表示顶级菜单)"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[4].visible"
            prop="parentId"
          />
          <el-table-column
            label="所有父级ID路径 (如: 0,10,25)，方便快速查询子树"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[5].visible"
            prop="ancestors"
          />
          <el-table-column
            label="菜单层级"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[6].visible"
            prop="menuLevel"
          />
          <el-table-column
            label="SPU 前缀"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[7].visible"
            prop="spuPrefix"
          />
          <el-table-column
            label="当前最大流水号"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[8].visible"
            prop="currentMaxSeq"
          />
          <el-table-column
            label="创建时间"
            align="center"
            v-if="columns[9].visible"
            prop="createTime"
            width="180"
          >
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime, "{y}-{m}-{d}") }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="备注"
            align="center"
            :show-overflow-tooltip="true"
            v-if="columns[10].visible"
            prop="remark"
          />
          <el-table-column
            label="操作"
            align="center"
            class-name="small-padding fixed-width"
          >
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                icon="el-icon-edit"
                @click="handleUpdate(scope.row)"
                v-hasPermi="['vh-erp:tag:edit']"
                >修改</el-button
              >
              <el-button
                size="mini"
                type="text"
                icon="el-icon-plus"
                @click="handleAdd(scope.row)"
                v-hasPermi="['vh-erp:tag:add']"
                >新增</el-button
              >
              <el-button
                size="mini"
                type="text"
                icon="el-icon-delete"
                @click="handleDelete(scope.row)"
                v-hasPermi="['vh-erp:tag:remove']"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>

        <!-- 添加或修改erp标签对话框 -->

        <TagDialog ref="tagmodal" @success="handleSuccess" />

        <!-- erp标签导入对话框 -->
        <el-dialog
          :title="upload.title"
          :visible.sync="upload.open"
          width="400px"
          append-to-body
        >
          <el-upload
            ref="upload"
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
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <div class="el-upload__tip text-center" slot="tip">
              <div class="el-upload__tip" slot="tip">
                <el-checkbox
                  v-model="upload.updateSupport"
                />是否更新已经存在的erp标签数据
              </div>
              <span>仅允许导入xls、xlsx格式文件。</span>
              <el-link
                type="primary"
                :underline="false"
                style="font-size: 12px; vertical-align: baseline"
                @click="importTemplate"
                >下载模板</el-link
              >
            </div>
          </el-upload>
          <div slot="footer" class="dialog-footer">
            <el-button type="primary" @click="submitFileForm">确 定</el-button>
            <el-button @click="upload.open = false">取 消</el-button>
          </div>
        </el-dialog>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import {
  listTag,
  getTag,
  delTag,
  addTag,
  updateTag,
  importTag,
  importTemplateTag,
} from "@/api/erp/tag";
import { getToken } from "@/utils/auth";
import TagDialog from "@/components/erp/TagDialog";
export default {
  name: "Tag",
  dicts: ["erp_tag_type"],
  components: {
    TagDialog,
  },
  data() {
    return {
      //表格展示列
      columns: [
        { key: 0, label: "标签名称", visible: true },
        { key: 1, label: "标签编码", visible: true },
        { key: 2, label: "标签类型", visible: true },
        { key: 3, label: "排序", visible: true },
        { key: 4, label: "父级ID (0表示顶级菜单)", visible: true },
        {
          key: 5,
          label: "所有父级ID路径 (如: 0,10,25)，方便快速查询子树",
          visible: true,
        },
        { key: 6, label: "菜单层级", visible: true },
        { key: 7, label: "SPU 前缀", visible: true },
        { key: 8, label: "当前最大流水号", visible: true },
        { key: 9, label: "创建时间", visible: true },
        { key: 10, label: "备注", visible: true },
      ],
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // erp标签表格数据
      tagList: [],
      // erp标签树数据
      tagTree: [],
      // erp标签树选项
      tagOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        tagName: null,
        tagCode: null,
        tagType: null,
        spuPrefix: null,
        createTime: null,
      },
      // 导出地址
      exportUrl: "vh-erp/tag/export",
      // erp标签导入参数
      upload: {
        // 是否显示弹出层（erp标签导入）
        open: false,
        // 弹出层标题（erp标签导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的erp标签数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/vh-erp/tag/importData",
        // 下载模板的地址
        templateUrl: "vh-erp/tag/importTemplate",
      },
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询erp标签列表 */
    getList() {
      this.loading = true;
      listTag(this.queryParams).then((response) => {
        this.tagList = this.handleTree(response.data, "tagCode", "parentId");
        this.tagTree = this.handleTree(response.data, "tagCode", "parentId");
        this.loading = false;
      });
    },
    /** 树节点点击事件 */
    handleNodeClick(node) {
      // 可以根据需要过滤右侧表格，例如设置parentId
      // this.queryParams.parentId = node.tagCode;
      // this.getList();
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false;
      this.isExpandAll = !this.isExpandAll;
      this.$nextTick(() => {
        this.refreshTable = true;
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal
        .confirm('是否确认删除erp标签编号为"' + row.tagId + '"的数据项？')
        .then(function () {
          return delTag(row.tagId);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess("删除成功");
        })
        .catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(
        this.exportUrl,
        {
          ...this.queryParams,
        },
        `tag_${new Date().getTime()}.xlsx`
      );
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "erp标签导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download(
        this.upload.templateUrl,
        {},
        `tag_template_${new Date().getTime()}.xlsx`
      );
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
      this.$alert(
        "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
          response.msg +
          "</div>",
        "导入结果",
        { dangerouslyUseHTMLString: true }
      );
      this.getList();
    },
    // 提交上传文件
    submitFileForm() {
      this.$refs.upload.submit();
    },
    /** 提交按钮 */
    handleSuccess() {
      this.$modal.msgSuccess("操作成功");
      this.open = false; // 关闭对话框
      this.getList(); // 刷新列表
    },
    handleAdd(row) {
      let currentRow = null; // null 表示新增模式
      if (row != null && row.tagCode) {
        currentRow = { parentId: row.id }; // 可选：设置父级 ID
      }
      this.$tagmodal.open(currentRow); // 直接调用子组件的方法，并传递行数据
    },
    handleUpdate(row) {
      this.$tagmodal.open(row); // 直接调用子组件的方法，并传递行数据
    },
  },
};
</script>
