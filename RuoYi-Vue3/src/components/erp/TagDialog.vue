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

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from "vue";
import type { FormInstance, FormRules } from "element-plus";

import { treeList, addTag, updateTag } from "@/api/erp/tag";
import { ElMessage } from "element-plus";
import { useDict } from "@/utils/dict";
import type { TagDictMenu } from "@/types/erp";

interface TagFormData {
  tagId: number | null;
  tagName: string | null;
  tagCode: string | null;
  tagType: string;
  sortOrder: number | null;
  parentId: number | null;
  menuLevel: number | null;
  spuPrefix: string | null;
  currentMaxSeq: number;
  remark: string | null;
}

const emit = defineEmits<{
  (e: "success"): void;
}>();

const { erp_tag_type: erpTagTypeOptions } = useDict("erp_tag_type") as any;

const title = ref<string>("");
const form = reactive<TagFormData>({
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
const rules = reactive<FormRules<TagFormData>>({
  tagName: [{ required: true, message: "标签名称不能为空", trigger: "blur" }],
  tagCode: [{ required: true, message: "标签编码不能为空", trigger: "blur" }],
  tagType: [{ required: true, message: "标签类型不能为空", trigger: "change" }],
});
const tagOptions = ref<TagDictMenu[]>([]);
const menuLoading = ref<boolean>(false);
const visible = ref<boolean>(false);

const formRef = ref<FormInstance | null>(null);

watch(
  () => form.tagType,
  (newType: string) => {
    if (newType === "MENU") {
      fetchTags();
    }
  },
);

const open = (data?: Partial<TagDictMenu> & { parentId?: number }): void => {
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

const fetchTags = async (): Promise<void> => {
  try {
    menuLoading.value = true;
    const response = await treeList("MENU");
    const rows = response.data || [];
    tagOptions.value = rows;
  } finally {
    menuLoading.value = false;
  }
};

const resetForm = (): void => {
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

const handleSubmit = async (): Promise<void> => {
  const valid = await formRef.value?.validate();
  if (valid) {
    const apiCall = form.tagId ? updateTag(form as unknown as TagDictMenu) : addTag(form as unknown as TagDictMenu);
    try {
      await apiCall;
      emit("success");
      visible.value = false;
    } catch (error) {
      ElMessage.error(form.tagId ? "修改失败" : "新增失败");
    }
  }
};

const handleCancel = (): void => {
  visible.value = false;
  resetForm();
};

defineExpose({
  open,
});
</script>
