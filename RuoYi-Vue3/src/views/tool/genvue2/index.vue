<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryFormRef"
      size="small"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="表名称" prop="tableName">
        <el-input
          v-model="queryParams.tableName"
          placeholder="请输入表名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="表描述" prop="tableComment">
        <el-input
          v-model="queryParams.tableComment"
          placeholder="请输入表描述"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker
          v-model="dateRange"
          style="width: 240px"
          value-format="YYYY-MM-DD"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        ></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          :icon="Search"
          size="small"
          @click="handleQuery"
          >搜索</el-button
        >
        <el-button :icon="Refresh" size="small" @click="resetQuery"
          >重置</el-button
        >
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          :icon="Download"
          size="small"
          :disabled="multiple"
          @click="handleGenTable"
          v-hasPermi="['tool:gen:code']"
          >生成
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          :icon="Plus"
          size="small"
          @click="openCreateTable"
          v-hasRole="['admin']"
          >创建
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          :icon="Upload"
          size="small"
          @click="openImportTable"
          v-hasPermi="['tool:gen:import']"
          >导入
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          :icon="Edit"
          size="small"
          :disabled="single"
          @click="handleEditTable"
          v-hasPermi="['tool:gen:edit']"
          >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          :icon="Delete"
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['tool:gen:remove']"
          >删除
        </el-button>
      </el-col>
      <right-toolbar
        v-model:showSearch="showSearch"
        @queryTable="getList"
      ></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="tableList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column
        type="selection"
        align="center"
        width="55"
      ></el-table-column>
      <el-table-column label="序号" type="index" width="50" align="center">
        <template #default="scope">
          <span>{{
            (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1
          }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="表名称"
        align="center"
        prop="tableName"
        :show-overflow-tooltip="true"
        width="120"
      />
      <el-table-column
        label="表描述"
        align="center"
        prop="tableComment"
        :show-overflow-tooltip="true"
        width="120"
      />
      <el-table-column
        label="实体"
        align="center"
        prop="className"
        :show-overflow-tooltip="true"
        width="120"
      />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="160"
      />
      <el-table-column
        label="更新时间"
        align="center"
        prop="updateTime"
        width="160"
      />
      <el-table-column
        label="操作"
        align="center"
        class-name="small-padding fixed-width"
      >
        <template #default="scope">
          <el-button
            type="text"
            size="small"
            :icon="View"
            @click="handlePreview(scope.row)"
            v-hasPermi="['tool:gen:preview']"
            >预览
          </el-button>
          <el-button
            type="text"
            size="small"
            :icon="Edit"
            @click="handleGenValue(scope.row)"
            v-has-role="['admin']"
            >生成数据
          </el-button>
          <el-button
            type="text"
            size="small"
            :icon="Edit"
            @click="handleEditTable(scope.row)"
            v-hasPermi="['tool:gen:edit']"
            >编辑
          </el-button>
          <el-button
            type="text"
            size="small"
            :icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['tool:gen:remove']"
            >删除
          </el-button>
          <el-button
            type="text"
            size="small"
            :icon="Refresh"
            @click="handleSynchDb(scope.row)"
            v-hasPermi="['tool:gen:edit']"
            >同步
          </el-button>
          <el-button
            type="text"
            size="small"
            :icon="Download"
            @click="handleGenTable(scope.row)"
            v-hasPermi="['tool:gen:code']"
            >生成代码
          </el-button>
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
    <!-- 预览界面 -->
    <el-dialog
      :title="preview.title"
      v-model:visible="preview.open"
      width="80%"
      top="5vh"
      append-to-body
      class="scrollbar"
    >
      <el-tabs v-model="preview.activeName">
        <el-tab-pane
          v-for="(value, key) in preview.data"
          :label="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.vm'))"
          :name="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.vm'))"
          :key="key"
        >
          <el-link
            :underline="false"
            :icon="DocumentCopy"
            v-clipboard:copy="value"
            v-clipboard:success="clipboardSuccess"
            style="float: right"
            >复制
          </el-link>
          <pre><code class="hljs" v-html="highlightedCode(value, key)"></code></pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
    <import-table ref="importRef" @ok="handleQuery" />
    <create-table ref="createRef" @ok="handleQuery" />
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted, onActivated } from "vue";
import { useRoute } from "vue-router";
import {
  listTable,
  previewTable,
  delTable,
  genCode,
  synchDb,
  getGenTable,
} from "@/api/tool/genvue2";
import ImportTable from "./importTable";
import CreateTable from "./createTable";
import hljs from "highlight.js/lib/core";
import "highlight.js/styles/github-gist.css";
import {
  Search,
  Refresh,
  Download,
  Plus,
  Upload,
  Edit,
  Delete,
  View,
  DocumentCopy,
} from "@element-plus/icons-vue";

// 注册语言
// 按需注册语言
import java from "highlight.js/lib/languages/java";
import xml from "highlight.js/lib/languages/xml";
import javascript from "highlight.js/lib/languages/javascript";
import sql from "highlight.js/lib/languages/sql";

hljs.registerLanguage("java", java);
hljs.registerLanguage("xml", xml);
hljs.registerLanguage("html", xml);
hljs.registerLanguage("vue", xml);
hljs.registerLanguage("javascript", javascript);
hljs.registerLanguage("sql", sql);

const { proxy } = getCurrentInstance();
const route = useRoute();

// 遮罩层
const loading = ref(true);
// 唯一标识符
const uniqueId = ref("");
// 选中数组
const ids = ref([]);
// 选中表数组
const tableNames = ref([]);
// 非单个禁用
const single = ref(true);
// 非多个禁用
const multiple = ref(true);
// 显示搜索条件
const showSearch = ref(true);
// 总条数
const total = ref(0);
// 表数据
const tableList = ref([]);
// 日期范围
const dateRange = ref([]);
// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  tableComment: undefined,
});
// 预览参数
const preview = reactive({
  open: false,
  title: "代码预览",
  data: {},
  activeName: "domain.java",
});

// ref 引用
const queryFormRef = ref(null);
const importRef = ref(null);
const createRef = ref(null);

// 查询表集合
const getList = () => {
  loading.value = true;
  listTable(proxy.addDateRange(queryParams, dateRange.value)).then(
    (response) => {
      tableList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    },
  );
};

// 搜索按钮操作
const handleQuery = () => {
  queryParams.pageNum = 1;
  getList();
};

// 重置按钮操作
const resetQuery = () => {
  dateRange.value = [];
  queryFormRef.value?.resetFields();
  handleQuery();
};

// 多选框选中数据
const handleSelectionChange = (selection) => {
  ids.value = selection.map((item) => item.tableId);
  tableNames.value = selection.map((item) => item.tableName);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
};

// 生成数据
const handleGenValue = (row) => {
  const tableId = row.tableId;
  const tableName = row.tableName || tableNames.value[0];
  const params = { pageNum: queryParams.pageNum };
  proxy.$tab.openPage(
    "生成[" + tableName + "] 数据",
    "/tool/gen-add-value/index/" + tableId,
    params,
  );
};

// 生成代码操作
const handleGenTable = (row) => {
  const tableNamesVal = row.tableName || tableNames.value;
  if (tableNamesVal === "") {
    proxy.$modal.msgError("请选择要生成的数据");
    return;
  }
  if (row.genType === "1") {
    genCode(row.tableName).then((response) => {
      proxy.$modal.msgSuccess("成功生成到自定义路径：" + row.genPath);
    });
  } else {
    proxy.$download.zip(
      "/tool/gen/batchGenCode?tables=" + tableNamesVal,
      "ruoyi.zip",
    );
  }
};

// 同步数据库操作
const handleSynchDb = (row) => {
  const tableName = row.tableName;
  proxy.$modal
    .confirm('确认要强制同步"' + tableName + '"表结构吗？')
    .then(() => {
      return synchDb(tableName);
    })
    .then(() => {
      proxy.$modal.msgSuccess("同步成功");
    })
    .catch(() => {});
};

// 打开导入表弹窗
const openImportTable = () => {
  importRef.value?.show();
};

// 打开创建表弹窗
const openCreateTable = () => {
  createRef.value?.show();
};

// 预览按钮
const handlePreview = (row) => {
  previewTable(row.tableId).then((response) => {
    preview.data = response.data;
    preview.open = true;
    preview.activeName = "domain.java";
  });
};

// 高亮显示
const highlightedCode = (code, key) => {
  const vmName = key.substring(key.lastIndexOf("/") + 1, key.indexOf(".vm"));
  const language = vmName.substring(vmName.indexOf(".") + 1, vmName.length);
  const result = hljs.highlight(language, code || "", true);
  return result.value || "&nbsp;";
};

// 复制代码成功
const clipboardSuccess = () => {
  proxy.$modal.msgSuccess("复制成功");
};

// 修改按钮操作
const handleEditTable = (row) => {
  const tableId = row.tableId || ids.value[0];
  const tableName = row.tableName || tableNames.value[0];
  const params = { pageNum: queryParams.pageNum };
  proxy.$tab.openPage(
    "修改[" + tableName + "] 生成配置",
    "/tool/gen-edit/index/" + tableId,
    params,
  );
};

// 删除按钮操作
const handleDelete = (row) => {
  const tableIds = row.tableId || ids.value;
  proxy.$modal
    .confirm('是否确认删除表编号为"' + tableIds + '"的数据项？')
    .then(() => {
      return delTable(tableIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
};

// 初始化
onMounted(() => {
  getList();
});

// 激活时执行
onActivated(() => {
  const time = route.query.t;
  if (time != null && time !== uniqueId.value) {
    uniqueId.value = time;
    queryParams.pageNum = Number(route.query.pageNum);
    getList();
  }
});
</script>
