<template>
  <div class="app-container">
    <!-- 左侧标签面板 -->
    <div class="left-panel" :class="{ collapsed: isCollapsed }">
      <div class="panel-header">
        <span class="panel-title">TAG 标签</span>
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

    <!-- 宽度调节器 - 语雀风格 -->
    <div
      class="resize-handle"
      :class="{ 'is-hovering': isHandleHovering, 'is-resizing': isResizing }"
      @mousedown="startResize"
      @mouseenter="isHandleHovering = true"
      @mouseleave="isHandleHovering = false"
    >
      <!-- 1px 可见拖拽线 -->
      <div class="drag-line"></div>

      <!-- 悬浮时显示的控制按钮 -->
      <div class="control-buttons" v-show="isHandleHovering && !isResizing">
        <el-button
          size="small"
          circle
          @click.stop="toggleCollapse"
          @mousedown.stop
          :title="isCollapsed ? '展开' : '收起'"
        >
          <el-icon>
            <ArrowRight v-if="isCollapsed" />
            <ArrowLeft v-else />
          </el-icon>
        </el-button>
      </div>
    </div>

    <!-- 右侧内容区域 -->
    <div class="right-panel" :class="{ 'is-collapsed': isCollapsed }">
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
import { ref, onMounted, onBeforeUnmount, watch } from "vue";
import { ElIcon } from "element-plus";
import { Refresh, Plus, Fold, Expand } from "@element-plus/icons-vue";
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
const isHandleHovering = ref(false);

// 最小宽度阈值，小于此值自动收起
const MIN_WIDTH_THRESHOLD = 280;
// 默认展开宽度
const DEFAULT_EXPAND_WIDTH = 280;

const handleTagSelection = (tags) => {
  selectedTags.value = tags;
};

const removeTag = (tagToRemove) => {
  selectedTags.value = selectedTags.value.filter(
    (tag) => tag.tagId !== tagToRemove.tagId,
  );
};

const toggleCollapse = () => {
  console.log("原 panelWidth:", panelWidth.value);
  console.log("原 isCollapsed:", isCollapsed.value);
  isCollapsed.value = !isCollapsed.value;
  // 收起时宽度设为 0，展开时恢复 280px
  panelWidth.value = isCollapsed.value ? 0 : DEFAULT_EXPAND_WIDTH;
  console.log("panelWidth:", panelWidth);
  console.log("后 panelWidth:", panelWidth.value);
  console.log("后 isCollapsed:", isCollapsed.value);
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
  // 过滤，只保留菜单类型的标签
  const selectedMenuTags = selectedTags.value.filter(
    (tag) => tag.tagType === "MENU",
  );
  const parentTag = selectedMenuTags.length === 1 ? selectedMenuTags[0] : null;
  tagTree.value?.openAddDialog(
    parentTag ? { parentId: parentTag.tagId } : null,
  );
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
  console.log("handleResize-panelWidth:", panelWidth.value);

  const deltaX = e.clientX - startX.value;
  let newWidth = startWidth.value + deltaX;

  // 原本是展开状态时，宽度小于阈值时，自动触发收起
  if (!isCollapsed.value) {
    newWidth = Math.max(200, Math.min(600, newWidth));
    panelWidth.value = newWidth;
    if (newWidth < MIN_WIDTH_THRESHOLD) {
      isCollapsed.value = true;
      panelWidth.value = newWidth;
      console.log("当宽度小于阈值时，panelWidth:", panelWidth);
    }
  } else {
    // 原本是收起状态时，宽度大于等于阈值时，自动触发展开
    if (newWidth >= MIN_WIDTH_THRESHOLD) {
      isCollapsed.value = false;
      panelWidth.value = newWidth;
      console.log("当宽度大于等于阈值时，panelWidth:", panelWidth);
    }
  }
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
  if (panelWidth.value < MIN_WIDTH_THRESHOLD && isCollapsed.value) {
    panelWidth.value = 0;
  } else if (panelWidth.value >= MIN_WIDTH_THRESHOLD && !isCollapsed.value) {
    panelWidth.value = panelWidth.value;
  }
  console.log("stopResize-panelWidth:", panelWidth.value, isCollapsed.value);
  console.log("stopResize-isCollapsed:", isCollapsed.value);
};

const handleWindowResize = () => {
  if (window.innerWidth < 768 && !isCollapsed.value) {
    isCollapsed.value = true;
    panelWidth.value = 0;
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
  min-width: 800px;
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
  overflow: hidden;
  /* 使用 v-bind 动态设置宽度 */
  width: v-bind(panelWidth + "px");
}

.left-panel.collapsed {
  width: 0 !important;
  transition: width 0.3s ease;
}

.left-panel.collapsed .panel-header,
.left-panel.collapsed .panel-content {
  display: none;
}

.panel-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e9ecef;
  background: #f8f9fa;
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.panel-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.panel-content {
  height: calc(100% - 48px);
  overflow: auto;
  padding: 8px 0;
}

/* 语雀风格调节器 */
.resize-handle {
  position: absolute;
  top: 0;
  width: 8px;
  height: 100%;
  background: transparent;
  cursor: col-resize;
  z-index: 20;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 使用 v-bind 动态设置位置 */
  left: v-bind(panelWidth + "px");
}

.resize-handle:hover,
.resize-handle.is-hovering {
  background: rgba(64, 158, 255, 0.08);
}

.resize-handle.is-resizing {
  background: rgba(64, 158, 255, 0.15);
}

/* 1px 可见拖拽线 */
.drag-line {
  width: 1px;
  height: 100%;
  background: #e9ecef;
  transition: background-color 0.2s;
}

.resize-handle:hover .drag-line,
.resize-handle.is-hovering .drag-line {
  background: #409eff;
}

.resize-handle.is-resizing .drag-line {
  background: #409eff;
}

/* 悬浮控制按钮 */
.control-buttons {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 21;
}

.control-buttons .el-button {
  width: 28px;
  height: 28px;
  padding: 0;
  background: #fff;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.2s ease;
}

.resize-handle:hover .control-buttons .el-button,
.resize-handle.is-hovering .control-buttons .el-button {
  opacity: 1;
  transform: scale(1);
}

.control-buttons .el-button:hover {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.right-panel {
  position: absolute;
  top: 0;
  /* 使用 v-bind 动态设置左侧位置 */
  left: v-bind(panelWidth + 8 + "px");
  right: 0;
  height: 100%;
  background: #fff;
  transition: left 0.3s ease;
  overflow: hidden;
}

/* 收起状态时，右侧面板贴左边 */
.right-panel.is-collapsed {
  left: 0 !important;
}

.content-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e9ecef;
  background: #fff;
  flex-shrink: 0;
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
  box-sizing: border-box;
  width: 100%;
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
