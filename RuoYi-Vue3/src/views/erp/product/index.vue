<template>
  <div class="app-container">
    <!-- 左侧标签面板 -->
    <div
      class="left-panel"
      :class="{ collapsed: isCollapsed }"
      :style="{ width: panelWidth + 'px' }"
    >
      <div class="panel-header">
        <span class="panel-title">标签选择</span>
        <div class="panel-actions">
          <el-button size="small" @click="refreshTags" title="刷新">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button size="small" @click="openAddDialog" title="新增标签">
            <el-icon><Plus /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="panel-content" v-show="!isCollapsed">
        <TagTreeSelector ref="tagTree" @selection-change="handleTagSelection" />
      </div>
    </div>

    <!-- 宽度调节器 -->
    <div
      class="resize-handle"
      @mousedown="startResize"
      :style="{ left: panelWidth + 'px' }"
    ></div>

    <!-- 右侧内容区域 -->
    <div class="right-panel" :style="{ left: panelWidth + 8 + 'px' }">
      <div class="content-header">
        <h3 class="content-title">商品管理</h3>
        <div class="selected-tags" v-if="selectedTags.length > 0">
          <span class="tag-label">已选标签：</span>
          <el-tag
            v-for="tag in selectedTags"
            :key="tag.tagId"
            size="small"
            closable
            @close="removeTag(tag)"
          >
            {{ tag.tagName }}
          </el-tag>
        </div>
      </div>
      <div class="content-body">
        <ProductListPanel ref="productList" :selected-tags="selectedTags" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { ElIcon } from "element-plus";
import { Refresh, Plus } from "@element-plus/icons-vue";
import TagTreeSelector from "@/components/erp/TagTreeSelector";
import ProductListPanel from "@/components/erp/ProductListPanel";

const tagTree = ref(null);
const productList = ref(null);
const selectedTags = ref([]);
const isCollapsed = ref(false);
const panelWidth = ref(320);
const isResizing = ref(false);
const startX = ref(0);
const startWidth = ref(0);

const handleTagSelection = (tags) => {
  selectedTags.value = tags;
};

const removeTag = (tagToRemove) => {
  selectedTags.value = selectedTags.value.filter(
    (tag) => tag.tagId !== tagToRemove.tagId,
  );
};

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};

const refreshTags = () => {
  // 清除标签选中状态
  tagTree.value?.clearSelection();
  selectedTags.value = [];
  // 重新加载标签
  tagTree.value?.loadTags();
  // 刷新商品列表
  productList.value?.refresh();
};

const openAddDialog = () => {
  tagTree.value?.openAddDialog();
};

const startResize = (e) => {
  isResizing.value = true;
  startX.value = e.clientX;
  startWidth.value = panelWidth.value;

  document.addEventListener("mousemove", handleResize);
  document.addEventListener("mouseup", stopResize);
  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";
};

const handleResize = (e) => {
  if (!isResizing.value) return;

  const deltaX = e.clientX - startX.value;
  let newWidth = startWidth.value + deltaX;

  newWidth = Math.max(200, Math.min(600, newWidth));
  panelWidth.value = newWidth;
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
};

const handleWindowResize = () => {
  if (window.innerWidth < 768 && !isCollapsed.value) {
    isCollapsed.value = true;
  }
};

onMounted(() => {
  window.addEventListener("resize", handleWindowResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleWindowResize);
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
});
</script>

<style scoped>
.app-container {
  height: calc(100vh - 84px);
  background: #f8f9fa;
  position: relative;
  overflow: hidden;
}

.left-panel {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e9ecef;
  transition: width 0.3s ease;
  z-index: 10;
}

.left-panel.collapsed {
  width: 48px !important;
}

.panel-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e9ecef;
  background: #f8f9fa;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
}

.panel-actions {
  display: flex;
  gap: 4px;
}

.panel-content {
  height: calc(100% - 48px);
  overflow: auto;
  padding: 8px 0;
}

.resize-handle {
  position: absolute;
  top: 0;
  width: 8px;
  height: 100%;
  background: transparent;
  cursor: col-resize;
  z-index: 20;
  transition: background-color 0.2s;
}

.resize-handle:hover {
  background: rgba(64, 158, 255, 0.1);
}

.right-panel {
  position: absolute;
  top: 0;
  height: 100%;
  background: #fff;
  transition: left 0.3s ease;
  overflow: hidden;
}

.content-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e9ecef;
  background: #fff;
}

.content-title {
  font-size: 16px;
  font-weight: 500;
  color: #212529;
  margin: 0;
}

.selected-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-label {
  font-size: 12px;
  color: #6c757d;
  white-space: nowrap;
}

.content-body {
  height: calc(100% - 48px);
  padding: 20px;
  overflow: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .left-panel {
    position: fixed;
    z-index: 1000;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  }

  .left-panel.collapsed {
    width: 0 !important;
  }

  .resize-handle {
    display: none;
  }

  .right-panel {
    left: 0 !important;
  }

  .content-header {
    padding: 0 16px;
  }

  .content-body {
    padding: 16px;
  }
}

/* 滚动条样式 */
.panel-content::-webkit-scrollbar,
.content-body::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-track,
.content-body::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.panel-content::-webkit-scrollbar-thumb,
.content-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.panel-content::-webkit-scrollbar-thumb:hover,
.content-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
