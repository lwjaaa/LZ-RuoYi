<template>
  <el-form
    ref="genInfoFormRef"
    :model="info"
    :rules="rules"
    label-width="150px"
  >
    <!-- 表单内容... -->
  </el-form>
</template>

<script setup>
import { reactive, ref } from "vue";

defineProps({
  info: {
    type: Object,
    default: () => ({}),
  },
  tables: {
    type: Array,
    default: () => [],
  },
  menus: {
    type: Array,
    default: () => [],
  },
});

const genInfoFormRef = ref(null);

const rules = reactive({
  // 验证规则...
});

defineExpose({
  genInfoFormRef,
});
</script>
