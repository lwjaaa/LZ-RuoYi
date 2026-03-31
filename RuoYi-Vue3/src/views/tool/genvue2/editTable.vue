<template>
  <el-card>
    <el-tabs v-model="activeName">
      <el-tab-pane label="基本信息" name="basic">
        <basic-info-form ref="basicInfoRef" :info="info" />
      </el-tab-pane>
      <el-tab-pane label="字段信息" name="columnInfo">
        <el-table
          ref="dragTableRef"
          :data="columns"
          row-key="columnId"
          :max-height="tableHeight"
        >
          <el-table-column
            label="序号"
            type="index"
            min-width="5%"
            class-name="allowDrag"
          />
          <el-table-column
            label="字段列名"
            prop="columnName"
            min-width="10%"
            :show-overflow-tooltip="true"
          />
          <el-table-column label="字段描述" min-width="10%">
            <template #default="scope">
              <el-input v-model="scope.row.columnComment"></el-input>
            </template>
          </el-table-column>
          <el-table-column
            label="物理类型"
            prop="columnType"
            min-width="10%"
            :show-overflow-tooltip="true"
          />
          <el-table-column label="Java 类型" min-width="11%">
            <template #default="scope">
              <el-select v-model="scope.row.javaType">
                <el-option label="Long" value="Long" />
                <el-option label="String" value="String" />
                <el-option label="Integer" value="Integer" />
                <el-option label="Double" value="Double" />
                <el-option label="BigDecimal" value="BigDecimal" />
                <el-option label="Date" value="Date" />
                <el-option label="Boolean" value="Boolean" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="java 属性" min-width="10%">
            <template #default="scope">
              <el-input v-model="scope.row.javaField"></el-input>
            </template>
          </el-table-column>

          <el-table-column label="插入" min-width="5%">
            <template #default="scope">
              <el-checkbox
                true-label="1"
                false-label="0"
                v-model="scope.row.isInsert"
              ></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="编辑" min-width="5%">
            <template #default="scope">
              <el-checkbox
                true-label="1"
                false-label="0"
                v-model="scope.row.isEdit"
              ></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="列表" min-width="5%">
            <template #default="scope">
              <el-checkbox
                true-label="1"
                false-label="0"
                v-model="scope.row.isList"
              ></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="查询" min-width="5%">
            <template #default="scope">
              <el-checkbox
                true-label="1"
                false-label="0"
                v-model="scope.row.isQuery"
              ></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="查询方式" min-width="10%">
            <template #default="scope">
              <el-select v-model="scope.row.queryType">
                <el-option label="=" value="EQ" />
                <el-option label="!=" value="NE" />
                <el-option label=">" value="GT" />
                <el-option label=">=" value="GTE" />
                <el-option label="<" value="LT" />
                <el-option label="<=" value="LTE" />
                <el-option label="LIKE" value="LIKE" />
                <el-option label="BETWEEN" value="BETWEEN" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" min-width="5%">
            <template #default="scope">
              <el-checkbox
                true-label="1"
                false-label="0"
                v-model="scope.row.isRequired"
              ></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column label="显示类型" min-width="12%">
            <template #default="scope">
              <el-select v-model="scope.row.htmlType">
                <el-option label="文本框" value="input" />
                <el-option label="文本域" value="textarea" />
                <el-option label="下拉框" value="select" />
                <el-option label="单选框" value="radio" />
                <el-option label="复选框" value="checkbox" />
                <el-option label="日期控件" value="datetime" />
                <el-option label="图片上传" value="imageUpload" />
                <el-option label="文件上传" value="fileUpload" />
                <el-option label="富文本控件" value="editor" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="字典类型" min-width="12%">
            <template #default="scope">
              <el-select
                v-model="scope.row.dictType"
                clearable
                filterable
                placeholder="请选择"
              >
                <el-option
                  v-for="dict in dictOptions"
                  :key="dict.dictType"
                  :label="dict.dictName"
                  :value="dict.dictType"
                >
                  <span style="float: left">{{ dict.dictName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{
                    dict.dictType
                  }}</span>
                </el-option>
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="生成信息" name="genInfo">
        <gen-info-form
          ref="genInfoRef"
          :info="info"
          :tables="tables"
          :menus="menus"
        />
      </el-tab-pane>
    </el-tabs>
    <el-form label-width="100px">
      <el-form-item
        style="text-align: center; margin-left: -100px; margin-top: 10px"
      >
        <el-button type="primary" @click="submitForm()">提交</el-button>
        <el-button @click="close()">返回</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getGenTable, updateGenTable } from "@/api/tool/genvue2";
import { optionselect as getDictOptionselect } from "@/api/system/dict/type";
import { listMenu as getMenuTreeselect } from "@/api/system/menu";
import { handleTree } from "@/utils/ruoyi";
import BasicInfoForm from "./basicInfoForm";
import GenInfoForm from "./genInfoForm";
import Sortable from "sortablejs";

const { proxy } = getCurrentInstance();
const route = useRoute();
const router = useRouter();

// 选中选项卡的 name
const activeName = ref("columnInfo");
// 表格的高度
const tableHeight = ref(document.documentElement.scrollHeight - 245 + "px");
// 表信息
const tables = ref([]);
// 表列信息
const columns = ref([]);
// 字典信息
const dictOptions = ref([]);
// 菜单信息
const menus = ref([]);
// 表详细信息
const info = ref({});

// ref 引用
const basicInfoRef = ref(null);
const genInfoRef = ref(null);
const dragTableRef = ref(null);

// 初始化数据
const initData = () => {
  const tableId = route.params && route.params.tableId;
  if (tableId) {
    // 获取表详细信息
    getGenTable(tableId).then((res) => {
      columns.value = res.data.rows;
      info.value = res.data.info;
      tables.value = res.data.tables;
    });
    /** 查询字典下拉列表 */
    getDictOptionselect().then((response) => {
      dictOptions.value = response.data;
    });
    /** 查询菜单下拉列表 */
    getMenuTreeselect().then((response) => {
      menus.value = handleTree(response.data, "menuId");
    });
  }
};

// 获取表单验证 Promise
const getFormPromise = (form) => {
  return new Promise((resolve) => {
    form.validate((res) => {
      resolve(res);
    });
  });
};

// 提交按钮
const submitForm = () => {
  const basicForm = basicInfoRef.value.$refs.basicInfoForm;
  const genForm = genInfoRef.value.$refs.genInfoForm;
  Promise.all([basicForm, genForm].map(getFormPromise)).then((res) => {
    const validateResult = res.every((item) => !!item);
    if (validateResult) {
      const genTable = Object.assign({}, basicForm.model, genForm.model);
      genTable.columns = columns.value;
      genTable.params = {
        treeCode: genTable.treeCode,
        treeName: genTable.treeName,
        treeParentCode: genTable.treeParentCode,
        parentMenuId: genTable.parentMenuId,
      };
      updateGenTable(genTable).then((res) => {
        proxy.$modal.msgSuccess(res.msg);
        if (res.code === 200) {
          close();
        }
      });
    } else {
      proxy.$modal.msgError("表单校验未通过，请重新检查提交内容");
    }
  });
};

// 关闭按钮
const close = () => {
  const obj = {
    path: "/tool/gen",
    query: { t: Date.now(), pageNum: route.query.pageNum },
  };
  proxy.$tab.closeOpenPage(obj);
};

onMounted(() => {
  initData();

  // 初始化拖拽排序
  const el = dragTableRef.value.$el.querySelectorAll(
    ".el-table__body-wrapper > table > tbody",
  )[0];
  Sortable.create(el, {
    handle: ".allowDrag",
    onEnd: (evt) => {
      const targetRow = columns.value.splice(evt.oldIndex, 1)[0];
      columns.value.splice(evt.newIndex, 0, targetRow);
      for (let index in columns.value) {
        columns.value[index].sort = parseInt(index) + 1;
      }
    },
  });
});
</script>
