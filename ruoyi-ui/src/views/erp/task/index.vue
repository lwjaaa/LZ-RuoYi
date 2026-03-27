<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="任务名称" prop="taskName">
        <el-input
          v-model="queryParams.taskName"
          placeholder="请输入任务名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务分组" prop="taskGroup">
        <el-input
          v-model="queryParams.taskGroup"
          placeholder="请输入任务分组"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="请选择任务类型" clearable>
          <el-option
            v-for="dict in dict.type.erp_task_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关联业务类型" prop="businessType">
        <el-select v-model="queryParams.businessType" placeholder="请选择关联业务类型" clearable>
          <el-option
            v-for="dict in dict.type.erp_task_business_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="taskStatus">
        <el-select v-model="queryParams.taskStatus" placeholder="请选择任务状态" clearable>
          <el-option
            v-for="dict in dict.type.erp_task_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="开始执行时间">
        <el-date-picker
          v-model="daterangeStartTime"
          style="width: 240px"
          value-format="yyyy-MM-dd"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item label="父任务 ID" prop="parentTaskId">
        <el-input
          v-model="queryParams.parentTaskId"
          placeholder="请输入父任务 ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="根任务 ID" prop="rootTaskId">
        <el-input
          v-model="queryParams.rootTaskId"
          placeholder="请输入根任务 ID"
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
          v-hasPermi="['erp:task:add']"
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
          v-hasPermi="['erp:task:edit']"
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
          v-hasPermi="['erp:task:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['erp:task:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['erp:task:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务主键" align="center" v-if="columns[0].visible" prop="taskId" />
        <el-table-column label="任务名称" :show-overflow-tooltip="true" align="center" v-if="columns[1].visible" prop="taskName" />
        <el-table-column label="任务分组" align="center" v-if="columns[2].visible" prop="taskGroup">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.erp_task_group" :value="scope.row.taskGroup"/>
        </template>
      </el-table-column>
        <el-table-column label="任务类型" align="center" v-if="columns[3].visible" prop="taskType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.erp_task_type" :value="scope.row.taskType"/>
        </template>
      </el-table-column>
        <el-table-column label="关联业务类型" align="center" v-if="columns[4].visible" prop="businessType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.erp_task_business_type" :value="scope.row.businessType"/>
        </template>
      </el-table-column>
        <el-table-column label="关联业务 ID 集合" :show-overflow-tooltip="true" align="center" v-if="columns[5].visible" prop="businessIds" />
        <el-table-column label="请求地址" :show-overflow-tooltip="true" align="center" v-if="columns[6].visible" prop="requestPath" />
        <el-table-column label="请求参数" :show-overflow-tooltip="true" align="center" v-if="columns[7].visible" prop="requestParams" />
        <el-table-column label="任务状态" align="center" v-if="columns[8].visible" prop="taskStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.erp_task_status" :value="scope.row.taskStatus"/>
        </template>
      </el-table-column>
        <el-table-column label="执行进度" :show-overflow-tooltip="true" align="center" v-if="columns[9].visible" prop="progress" />
        <el-table-column label="错误信息" :show-overflow-tooltip="true" align="center" v-if="columns[10].visible" prop="errorMessage" />
        <el-table-column label="执行结果数据" :show-overflow-tooltip="true" align="center" v-if="columns[11].visible" prop="resultData" />
        <el-table-column label="执行耗时" :show-overflow-tooltip="true" align="center" v-if="columns[12].visible" prop="executionTime" />
        <el-table-column label="开始执行时间" align="center" v-if="columns[13].visible" prop="startTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
        <el-table-column label="结束时间" align="center" v-if="columns[14].visible" prop="endTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
        <el-table-column label="父任务 ID" :show-overflow-tooltip="true" align="center" v-if="columns[15].visible" prop="parentTaskId" />
        <el-table-column label="根任务 ID" :show-overflow-tooltip="true" align="center" v-if="columns[16].visible" prop="rootTaskId" />
        <el-table-column label="备注" :show-overflow-tooltip="true" align="center" v-if="columns[17].visible" prop="remark" />
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['erp:task:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['erp:task:remove']"
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

    <!-- 添加或修改Shopify 任务配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务分组" prop="taskGroup">
          <el-input v-model="form.taskGroup" placeholder="请输入任务分组" />
        </el-form-item>
        <el-form-item label="任务类型" prop="taskType">
          <el-select v-model="form.taskType" placeholder="请选择任务类型">
            <el-option
              v-for="dict in dict.type.erp_task_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关联业务类型" prop="businessType">
          <el-select v-model="form.businessType" placeholder="请选择关联业务类型">
            <el-option
              v-for="dict in dict.type.erp_task_business_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关联业务 ID 集合" prop="businessIds">
          <el-input v-model="form.businessIds" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="请求地址" prop="requestPath">
          <el-input v-model="form.requestPath" placeholder="请输入请求地址" />
        </el-form-item>
        <el-form-item label="任务状态" prop="taskStatus">
          <el-radio-group v-model="form.taskStatus">
            <el-radio
              v-for="dict in dict.type.erp_task_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行进度" prop="progress">
          <el-input v-model="form.progress" placeholder="请输入执行进度" />
        </el-form-item>
        <el-form-item label="错误信息" prop="errorMessage">
          <el-input v-model="form.errorMessage" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="执行耗时" prop="executionTime">
          <el-input v-model="form.executionTime" placeholder="请输入执行耗时" />
        </el-form-item>
        <el-form-item label="开始执行时间" prop="startTime">
          <el-date-picker clearable
            v-model="form.startTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择开始执行时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker clearable
            v-model="form.endTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择结束时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="父任务 ID" prop="parentTaskId">
          <el-input v-model="form.parentTaskId" placeholder="请输入父任务 ID" />
        </el-form-item>
        <el-form-item label="根任务 ID" prop="rootTaskId">
          <el-input v-model="form.rootTaskId" placeholder="请输入根任务 ID" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
        <el-form-item label="删除标志" prop="delFlag">
          <el-input v-model="form.delFlag" placeholder="请输入删除标志" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- Shopify 任务配置导入对话框 -->
    <el-dialog :title="upload.title" :visible.sync="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xlsx, .xls" :headers="upload.headers" :action="upload.url + '?updateSupport=' + upload.updateSupport" :disabled="upload.isUploading" :on-progress="handleFileUploadProgress" :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip text-center" slot="tip">
          <div class="el-upload__tip" slot="tip">
            <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的Shopify 任务配置数据
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
import { listTask, getTask, delTask, addTask, updateTask, importTask, importTemplateTask } from "@/api/erp/task";
import { getToken } from "@/utils/auth";

export default {
  name: "Task",
  dicts: ['erp_task_business_type', 'erp_task_type', 'erp_task_status'],
  data() {
    return {
      //表格展示列
      columns: [
        { key: 0, label: '任务主键', visible: true },
          { key: 1, label: '任务名称', visible: true },
          { key: 2, label: '任务分组', visible: true },
          { key: 3, label: '任务类型', visible: true },
          { key: 4, label: '关联业务类型', visible: true },
          { key: 5, label: '关联业务 ID 集合', visible: true },
          { key: 6, label: '请求地址', visible: true },
          { key: 7, label: '请求参数', visible: true },
          { key: 8, label: '任务状态', visible: true },
          { key: 9, label: '执行进度', visible: true },
          { key: 10, label: '错误信息', visible: true },
          { key: 11, label: '执行结果数据', visible: true },
          { key: 12, label: '执行耗时', visible: true },
          { key: 13, label: '开始执行时间', visible: true },
          { key: 14, label: '结束时间', visible: true },
          { key: 15, label: '父任务 ID', visible: true },
          { key: 16, label: '根任务 ID', visible: true },
          { key: 17, label: '备注', visible: true },
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
      // Shopify 任务配置表格数据
      taskList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 删除标志时间范围
      daterangeStartTime: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        taskName: null,
        taskGroup: null,
        taskType: null,
        businessType: null,
        businessIds: null,
        taskStatus: null,
        startTime: null,
        parentTaskId: null,
        rootTaskId: null,
      },
      // 表单参数
      form: {},
      // 导出地址
      exportUrl: 'erp/task/export',
      // Shopify 任务配置导入参数
      upload: {
        // 是否显示弹出层（Shopify 任务配置导入）
        open: false,
        // 弹出层标题（Shopify 任务配置导入）
        title: "",
        // 是否禁用上传
        isUploading: false,
        // 是否更新已经存在的Shopify 任务配置数据
        updateSupport: 0,
        // 设置上传的请求头部
        headers: { Authorization: "Bearer " + getToken() },
        // 上传的地址
        url: process.env.VUE_APP_BASE_API + "/erp/task/importData",
        // 下载模板的地址
        templateUrl: 'erp/task/importTemplate'
      },
      // 表单校验
      rules: {
        taskName: [
          { required: true, message: "任务名称不能为空", trigger: "blur" }
        ],
        taskType: [
          { required: true, message: "任务类型不能为空", trigger: "change" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询Shopify 任务配置列表 */
    getList() {
      this.loading = true;
      this.queryParams.params = {};
      if (null != this.daterangeStartTime && '' != this.daterangeStartTime) {
        this.queryParams.params["beginStartTime"] = this.daterangeStartTime[0];
        this.queryParams.params["endStartTime"] = this.daterangeStartTime[1];
      }
      listTask(this.queryParams).then(response => {
        this.taskList = response.rows;
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
        taskId: null,
        taskName: null,
        taskGroup: null,
        taskType: null,
        businessType: null,
        businessIds: null,
        requestPath: null,
        requestParams: null,
        taskStatus: null,
        progress: null,
        errorMessage: null,
        resultData: null,
        executionTime: null,
        startTime: null,
        endTime: null,
        parentTaskId: null,
        rootTaskId: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
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
      this.daterangeStartTime = [];
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.taskId)
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加Shopify 任务配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const taskId = row.taskId || this.ids
      getTask(taskId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改Shopify 任务配置";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.taskId != null) {
            updateTask(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addTask(this.form).then(response => {
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
      const taskIds = row.taskId || this.ids;
      this.$modal.confirm('是否确认删除Shopify 任务配置编号为"' + taskIds + '"的数据项？').then(function() {
        return delTask(taskIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download(this.exportUrl, {
        ...this.queryParams
      }, `task_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleImport() {
      this.upload.title = "Shopify 任务配置导入";
      this.upload.open = true;
    },
    /** 下载模板操作 */
    importTemplate() {
      this.download(this.upload.templateUrl, {
      }, `task_template_${new Date().getTime()}.xlsx`)
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
