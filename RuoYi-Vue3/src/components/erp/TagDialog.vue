<template>
  <el-dialog
    :title="title"
    v-model="visible"
    width="500px"
    append-to-body
    @close="handleCancel"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="标签名称" prop="tagName">
        <el-input v-model="form.tagName" placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="标签编码" prop="tagCode">
        <el-input v-model="form.tagCode" placeholder="请输入标签编码" />
      </el-form-item>
      <el-form-item label="标签类型" prop="tagType">
        <el-select v-model="form.tagType" placeholder="请选择标签类型">
          <el-option
            v-for="dict in erpTagTypeOptions"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <!-- <el-form-item v-if="form.tagType === 'MENU'" label="父级" prop="parentId">
        <treeselect
          v-model="form.parentId"
          :options="tagOptions"
          :normalizer="normalizer"
          placeholder="请选择父级"
        />
      </el-form-item> -->
      <el-form-item
        v-if="form.tagType === 'MENU'"
        label="父级标签"
        prop="parentId"
      >
        <el-cascader
          v-model="form.parentId"
          :options="tagOptions"
          :props="{
            value: 'tagId',
            label: 'tagName',
            children: 'children',
            checkStrictly: true,
            emitPath: false,
            expandTrigger: 'hover',
          }"
          placeholder="请选择父级标签 (不选表示顶级)"
          clearable
          style="width: 100%"
          :loading="menuLoading"
        />
      </el-form-item>
      <el-form-item
        v-if="form.tagType == 'MENU'"
        label="SPU 前缀"
        prop="spuPrefix"
      >
        <el-input v-model="form.spuPrefix" placeholder="请输入SPU 前缀" />
      </el-form-item>
      <el-form-item
        v-if="form.tagType === 'MENU'"
        label="当前最大流水号"
        prop="currentMaxSeq"
      >
        <el-input
          v-model="form.currentMaxSeq"
          placeholder="请输入当前最大流水号"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          placeholder="请输入内容"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
        <el-button @click="handleCancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from "vue";

import { treeList, addTag, updateTag } from "@/api/erp/tag";
import { ElMessage } from "element-plus";
import { useDict } from "@/utils/dict";

const emit = defineEmits(["success"]);

// 使用字典
const { erp_tag_type: erpTagTypeOptions } = useDict("erp_tag_type");

// 响应式数据
const title = ref("");
const form = reactive({});
const rules = reactive({
  tagName: [{ required: true, message: "标签名称不能为空", trigger: "blur" }],
  tagCode: [{ required: true, message: "标签编码不能为空", trigger: "blur" }],
  tagType: [{ required: true, message: "标签类型不能为空", trigger: "change" }],
});
const tagOptions = ref([]);
const menuLoading = ref(false);
const visible = ref(false);

// 表单引用
const formRef = ref(null);

// 监听器
watch(
  () => form.tagType,
  (newType) => {
    if (newType === "MENU") {
      fetchTags();
    }
  },
);

// 方法
const open = (data) => {
  fetchTags();
  visible.value = true;
  if (data && data.tagId) {
    title.value = "修改标签";
    Object.assign(form, data);
  } else {
    title.value = "新增标签";
    resetForm();
    if (data && data.parentId != null) {
      form.parentId = data.parentId;
    }
  }
};

const fetchTags = async () => {
  try {
    menuLoading.value = true;
    const response = await treeList("MENU");
    const rows = response.data || [];
    tagOptions.value = rows;
  } finally {
    menuLoading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, {
    tagId: null,
    tagName: null,
    tagCode: null,
    tagType: "MENU",
    sortOrder: null,
    parentId: null,
    menuLevel: null,
    spuPrefix: null,
    currentMaxSeq: 0,
    remark: null,
  });
};

const handleSubmit = async () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      const apiCall = form.tagId ? updateTag(form) : addTag(form);
      try {
        await apiCall;
        emit("success");
        visible.value = false;
      } catch (error) {
        ElMessage.error(form.tagId ? "修改失败" : "新增失败");
      }
    }
  });
};

const handleCancel = () => {
  visible.value = false;
  resetForm();
};

// 暴露方法给父组件
defineExpose({
  open,
});
</script>
