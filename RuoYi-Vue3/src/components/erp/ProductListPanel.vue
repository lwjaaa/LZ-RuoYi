<template>
  <div class="product-list-panel">
    <!-- 顶部操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button
          size="small"
          type="primary"
          icon="el-icon-plus"
          @click="openWizard"
          :disabled="!canCreate"
        >
          选品
        </el-button>
        <el-button
          size="small"
          type="success"
          icon="el-icon-upload"
          @click="batchPush"
          :disabled="selectedProducts.length === 0"
        >
          批量推送 Shopify
        </el-button>
        <el-button
          size="small"
          type="danger"
          icon="el-icon-delete"
          @click="batchDelete"
          :disabled="selectedProducts.length === 0"
        >
          批量删除
        </el-button>
      </div>
      <div class="toolbar-right" v-if="selectedProducts.length > 0">
        <span class="selection-info">
          已选择 {{ selectedProducts.length }} 个商品
        </span>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="productList"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        :header-cell-style="{ background: '#fafafa', color: '#666' }"
        :row-style="{ height: '50px' }"
      >
        <el-table-column
          type="selection"
          width="55"
          align="center"
        ></el-table-column>
        <el-table-column prop="spu" label="SPU" width="120" align="center">
          <template #default="scope">
            <el-tag size="small" type="info">{{ scope.row.spu }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="productTitle"
          label="标题"
          min-width="200"
          show-overflow-tooltip
        ></el-table-column>
        <el-table-column label="关联标签" min-width="150">
          <template #default="scope">
            <div class="tags-container">
              <el-tag
                v-for="tag in scope.row.tags"
                :key="tag.tagId"
                size="small"
                :type="getTagType(tag)"
              >
                {{ tag.tagName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag
              :type="scope.row.status === '1' ? 'success' : 'warning'"
              size="small"
            >
              <i
                :class="
                  scope.row.status === '1' ? 'el-icon-check' : 'el-icon-edit'
                "
              ></i>
              {{ scope.row.status === "1" ? "已发布" : "草稿" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
          align="center"
        ></el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                size="small"
                icon="el-icon-edit"
                @click="editProduct(scope.row)"
                type="primary"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                icon="el-icon-upload"
                @click="pushProduct(scope.row)"
                type="success"
              >
                推送
              </el-button>
              <el-button
                size="small"
                icon="el-icon-view"
                @click="viewDetails(scope.row)"
                type="info"
              >
                详情
              </el-button>
              <el-button
                size="small"
                icon="el-icon-picture"
                @click="saveMedia(scope.row)"
                type="warning"
              >
                媒体
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page="queryParams.pageNum"
      :limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 选品向导 -->
    <ProductCreationWizard
      v-model="wizardVisible"
      :selected-tags="selectedTags"
      @saved="handleWizardSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listProduct, delProduct, pushBatch } from "@/api/erp/product";
import ProductCreationWizard from "@/components/erp/ProductCreationWizard";

// Props
const props = defineProps({
  selectedTags: {
    type: Array,
    default: () => [],
  },
});

// Emits
const emit = defineEmits(["product-updated"]);

// Reactive data
const loading = ref(true);
const productList = ref([]);
const selectedProducts = ref([]);
const total = ref(0);
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  tagIds: [],
});
const wizardVisible = ref(false);

// Computed
const canCreate = computed(() => {
  return (
    props.selectedTags.length > 0 &&
    props.selectedTags.some((tag) => tag.tagType === "MENU")
  );
});

// Methods
const getList = () => {
  loading.value = true;
  listProduct(queryParams.value)
    .then((response) => {
      productList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
};

const handleSelectionChange = (selection) => {
  selectedProducts.value = selection;
};

const openWizard = () => {
  wizardVisible.value = true;
};

const handleWizardSaved = () => {
  getList();
  emit("product-updated");
};

const batchPush = () => {
  const productIds = selectedProducts.value.map((p) => p.productId);
  ElMessageBox.confirm("确定批量推送选中的商品到 Shopify 吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    pushBatch(productIds).then((response) => {
      ElMessage.success("推送任务已启动，任务ID: " + response.taskId);
      // 可选：轮询结果
    });
  });
};

const batchDelete = () => {
  const productIds = selectedProducts.value.map((p) => p.productId);
  ElMessageBox.confirm("确定删除选中的商品吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    delProduct(productIds.join(",")).then(() => {
      ElMessage.success("删除成功");
      getList();
    });
  });
};

const editProduct = (row) => {
  // 打开编辑模式，复用向导
  wizardVisible.value = true;
  // 传递数据到向导
  // TODO: 需要在 ProductCreationWizard 中添加 loadProductData 方法
};

const pushProduct = (row) => {
  pushBatch([row.productId]).then((response) => {
    ElMessage.success("推送任务已启动，任务ID: " + response.taskId);
  });
};

const viewDetails = (row) => {
  // 打开详情弹窗
  ElMessage.info(`查看商品详情: ${row.spu}`);
};

const saveMedia = (row) => {
  // 保存媒体资源逻辑
  ElMessage.info(`保存媒体资源: ${row.spu}`);
};

const getTagType = (tag) => {
  // 根据标签类型返回不同的颜色
  if (tag.tagType === "MENU") {
    return "primary";
  } else if (tag.tagType === "CATEGORY") {
    return "success";
  } else {
    return "";
  }
};

// Watchers
watch(
  () => props.selectedTags,
  (newTags) => {
    queryParams.value.tagIds = newTags.map((tag) => tag.tagId);
    queryParams.value.pageNum = 1; // 重置到第一页
    getList();
  },
  { deep: true },
);

// Lifecycle
onMounted(() => {
  getList();
});

// Expose methods for parent component
defineExpose({
  refresh: getList,
});
</script>

<style scoped>
.product-list-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.toolbar-right {
  color: #6c757d;
  font-size: 12px;
}

.selection-info {
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
  font-size: 12px;
}

.table-container {
  flex: 1;
  background: #fff;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-width: 200px;
}

.tags-container .el-tag {
  margin: 0;
  font-size: 11px;
}

.action-buttons {
  display: flex;
  gap: 4px;
  justify-content: center;
}

/* 表格样式优化 */
:deep(.el-table) {
  border-radius: 4px;
}

:deep(.el-table th) {
  background-color: #f8f9fa !important;
  color: #495057 !important;
  font-weight: 500;
  border-bottom: 1px solid #dee2e6;
  font-size: 13px;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f1f3f4;
  font-size: 13px;
}

:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background-color: #f8f9fa;
}

:deep(.el-table__row--striped td) {
  background-color: #fafbfc;
}

/* 分页样式 */
:deep(.el-pagination) {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .action-buttons {
    flex-direction: column;
    gap: 2px;
  }

  .tags-container {
    max-width: 150px;
  }
}
</style>
