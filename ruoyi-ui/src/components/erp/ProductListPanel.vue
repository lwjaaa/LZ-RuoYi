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
          >选品</el-button
        >
        <el-button
          size="small"
          type="success"
          icon="el-icon-upload"
          @click="batchPush"
          :disabled="selectedProducts.length === 0"
          >批量推送 Shopify</el-button
        >
        <el-button
          size="small"
          type="danger"
          icon="el-icon-delete"
          @click="batchDelete"
          :disabled="selectedProducts.length === 0"
          >批量删除</el-button
        >
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
          <template slot-scope="scope">
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
          <template slot-scope="scope">
            <div class="tags-container">
              <el-tag
                v-for="tag in scope.row.tags"
                :key="tag.tagId"
                size="mini"
                :type="getTagType(tag)"
              >
                {{ tag.tagName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template slot-scope="scope">
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
          <template slot-scope="scope">
            <div class="action-buttons">
              <el-button
                size="mini"
                icon="el-icon-edit"
                @click="editProduct(scope.row)"
                type="primary"
                >编辑</el-button
              >
              <el-button
                size="mini"
                icon="el-icon-upload"
                @click="pushProduct(scope.row)"
                type="success"
                >推送</el-button
              >
              <el-button
                size="mini"
                icon="el-icon-view"
                @click="viewDetails(scope.row)"
                type="info"
                >详情</el-button
              >
              <el-button
                size="mini"
                icon="el-icon-picture"
                @click="saveMedia(scope.row)"
                type="warning"
                >媒体</el-button
              >
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 选品向导 -->
    <ProductCreationWizard
      :visible.sync="wizardVisible"
      :selected-tags="selectedTags"
      @saved="handleWizardSaved"
    />
  </div>
</template>

<script>
import { listProduct, delProduct, pushBatch } from "@/api/erp/product";
import ProductCreationWizard from "@/components/erp/ProductCreationWizard";

export default {
  name: "ProductListPanel",
  components: {
    ProductCreationWizard,
  },
  props: {
    selectedTags: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      loading: true,
      productList: [],
      selectedProducts: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tagIds: [],
      },
      wizardVisible: false,
    };
  },
  computed: {
    canCreate() {
      return (
        this.selectedTags.length > 0 &&
        this.selectedTags.some((tag) => tag.tagType === "MENU")
      );
    },
  },
  watch: {
    selectedTags: {
      handler(newTags) {
        this.queryParams.tagIds = newTags.map((tag) => tag.tagId);
        this.getList();
      },
      deep: true,
    },
  },
  mounted() {
    this.getList();
  },
  methods: {
    getList() {
      this.loading = true;
      listProduct(this.queryParams)
        .then((response) => {
          this.productList = response.rows;
          this.total = response.total;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    handleSelectionChange(selection) {
      this.selectedProducts = selection;
    },
    openWizard() {
      this.wizardVisible = true;
    },
    handleWizardSaved() {
      this.getList();
    },
    batchPush() {
      const productIds = this.selectedProducts.map((p) => p.productId);
      this.$confirm("确定批量推送选中的商品到 Shopify 吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        pushBatch(productIds).then((response) => {
          this.$message.success("推送任务已启动，任务ID: " + response.taskId);
          // 可选：轮询结果
        });
      });
    },
    batchDelete() {
      const productIds = this.selectedProducts.map((p) => p.productId);
      this.$confirm("确定删除选中的商品吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        delProduct(productIds.join(",")).then(() => {
          this.$message.success("删除成功");
          this.getList();
        });
      });
    },
    editProduct(row) {
      // 打开编辑模式，复用向导
      this.wizardVisible = true;
      // 传递数据到向导
      this.$nextTick(() => {
        this.$refs.wizard.loadProductData(row);
      });
    },
    pushProduct(row) {
      pushBatch([row.productId]).then((response) => {
        this.$message.success("推送任务已��动，任务ID: " + response.taskId);
      });
    },
    viewDetails(row) {
      // 打开详情弹窗
    },
    saveMedia(row) {
      // 保存媒体资源逻辑
    },
    getTagType(tag) {
      // 根据标签类型返回不同的颜色
      if (tag.tagType === "MENU") {
        return "primary";
      } else if (tag.tagType === "CATEGORY") {
        return "success";
      } else {
        return "";
      }
    },
  },
};
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
