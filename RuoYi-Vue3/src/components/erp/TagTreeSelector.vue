<template>
  <div class="tag-tree-selector">
    <!-- 标签树 -->
    <el-tree
      ref="tree"
      :data="tagList"
      :props="treeProps"
      show-checkbox
      node-key="tagId"
      :check-strictly="false"
      :expand-on-click-node="false"
      draggable
      :allow-drop="allowDrop"
      @check="handleCheck"
      @node-click="handleNodeClick"
      @node-drop="handleNodeDrop"
      highlight-current
    >
      <template v-slot="{ data }">
        <span
          class="custom-tree-node"
          @mouseenter="hoveredNode = data.tagId"
          @mouseleave="hoveredNode = null"
        >
          <i
            class="el-icon-folder-opened node-icon folder-icon"
            v-if="data.children && data.children.length > 0"
          ></i>
          <i class="el-icon-price-tag node-icon tag-icon" v-else></i>
          <span class="node-label"
            >{{ data.tagName }} ({{ data.tagCode }})</span
          >
          <span class="hover-buttons" v-if="hoveredNode === data.tagId">
            <el-button
              size="small"
              icon="Plus"
              @click.stop="openAddDialogFor(data)"
              >新增</el-button
            >
            <el-button
              size="small"
              icon="Edit"
              @click.stop="openEditDialog(data)"
              >编辑</el-button
            >
            <el-button
              size="small"
              type="danger"
              icon="Delete"
              @click.stop="deleteTag(data)"
              >删除</el-button
            >
            <el-button size="small" icon="Top" @click.stop="pinToTop(data)"
              >置顶</el-button
            >
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 引用独立的标签编辑组件 -->
    <TagDialog ref="tagModal" @success="handleDialogSuccess" />
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { treeList, addTag, updateTag, dragNode, delTag } from "@/api/erp/tag";
import TagDialog from "@/components/erp/TagDialog.vue";

// Props
const props = defineProps({
  selectedTags: {
    type: Array,
    default: () => [],
  },
});

// Emits
const emit = defineEmits(["selection-change"]);

// Refs
const tree = ref(null);
const tagModal = ref(null);

// Reactive data
const tagList = ref([]);
const hoveredNode = ref(null);

const treeProps = {
  children: "children",
  label: "tagName",
};

// Methods
const loadTags = () => {
  treeList("ALL").then((response) => {
    tagList.value = response.data;
  });
};

const normalizeTag = (node) => {
  const data = (node && node.data) || node || {};
  return {
    tagId: data.tagId,
    tagName: data.tagName,
    tagCode: data.tagCode,
    tagType: data.tagType,
    parentId: data.parentId,
    sortOrder: data.sortOrder,
  };
};

const updateSelection = () => {
  const checkedNodes = tree.value.getCheckedNodes();
  const normalized = checkedNodes.map((node) => normalizeTag(node));
  emit("selection-change", normalized);
};

const handleCheck = () => {
  updateSelection();
};

const handleNodeClick = (data) => {
  tree.value.setChecked(
    data.tagId,
    !tree.value.getCheckedNodes().some((node) => node.tagId === data.tagId),
  );
  // Use nextTick equivalent
  setTimeout(() => {
    updateSelection();
  }, 0);
};

const clearSelection = () => {
  tree.value.setCheckedKeys([]);
  emit("selection-change", []);
};

const openAddDialog = () => {
  tagModal.value.open(null);
};

const openAddDialogFor = (data) => {
  tagModal.value.open(null, { parentId: data.tagId });
};

const openEditDialog = (data) => {
  tagModal.value.open(data);
};

const handleDialogSuccess = () => {
  loadTags();
};

const deleteTag = (data) => {
  ElMessageBox.confirm("确定删除该标签吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    delTag(data.tagId).then(() => {
      ElMessage.success("删除成功");
      loadTags();
    });
  });
};

const pinToTop = (data) => {
  const siblings = getSiblings(data.parentId);
  const menuTags = siblings.filter((tag) => tag.tagType === "MENU");
  const otherTags = siblings.filter((tag) => tag.tagType !== "MENU");

  let newSortOrder;
  if (data.tagType === "MENU") {
    newSortOrder = Math.min(...siblings.map((s) => s.sortOrder)) - 1;
  } else {
    if (menuTags.length > 0) {
      const maxMenuSort = Math.max(...menuTags.map((s) => s.sortOrder));
      newSortOrder = maxMenuSort + 1;
    } else {
      newSortOrder = Math.min(...siblings.map((s) => s.sortOrder)) - 1;
    }
  }

  updateTag({ ...data, sortOrder: newSortOrder }).then(() => {
    loadTags();
  });
};

const getSiblings = (parentId) => {
  return tagList.value.filter((tag) => tag.parentId === parentId);
};

const calculateSortOrder = (draggingData, dropData, dropType) => {
  const siblings = getSiblings(dropData.parentId || 0);

  if (dropType === "inner") {
    const children = siblings.filter((tag) => tag.parentId === dropData.tagId);
    return children.length > 0
      ? Math.max(...children.map((tag) => tag.sortOrder)) + 1
      : 1;
  } else {
    if (dropType === "before") {
      return dropData.sortOrder - 0.5;
    } else {
      return dropData.sortOrder + 0.5;
    }
  }
};

const allowDrop = (draggingNode, dropNode, type) => {
  if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
    return false;
  }
  const draggingData = draggingNode.data;
  const dropData = dropNode.data;

  if (draggingData.tagType === "MENU") {
    return dropData.tagType === "MENU";
  } else {
    return dropData.tagType === "MENU" && type === "next";
  }
};

const handleNodeDrop = (draggingNode, dropNode, dropType) => {
  if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
    return;
  }

  const draggingData = draggingNode.data;
  const dropData = dropNode.data;

  const dropParentId =
    dropType === "inner"
      ? dropData.tagId
      : dropData.parentId === 0
        ? 0
        : dropData.parentId;

  const updatedNode = {
    tagId: draggingData.tagId,
    parentId: dropParentId,
    sortOrder: calculateSortOrder(draggingData, dropData, dropType),
  };

  console.log("更新节点:", updatedNode);
  updateTag(updatedNode)
    .then(() => {
      ElMessage.success("移动成功");
      loadTags();
    })
    .catch(() => {
      ElMessage.error("移动失败");
    });
};

// Expose methods for parent component
defineExpose({
  loadTags,
  clearSelection,
  openAddDialog,
});

// Lifecycle
onMounted(() => {
  loadTags();
});
</script>

<style scoped>
.tag-tree-selector {
  height: 100%;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 16px; /* 增加内边距 */
  border-radius: 6px;
  transition: background-color 0.2s ease;
  cursor: pointer;
  min-height: 48px; /* 增加高度 */
}

.custom-tree-node:hover {
  background-color: #f0f2f5; /* 更柔和的高亮 */
}

.node-icon {
  margin-right: 12px; /* 增加间距 */
  color: #666;
  font-size: 16px;
}

.folder-icon {
  color: #409eff;
}

.tag-icon {
  color: #67c23a;
}

.node-label {
  flex: 1;
  font-size: 14px;
  color: #333;
  line-height: 1.4;
}

.hover-buttons {
  display: none;
  gap: 6px; /* 增加按钮间距 */
  opacity: 0;
  transition: opacity 0.2s ease;
}

.custom-tree-node:hover .hover-buttons {
  display: flex;
  opacity: 1;
}

/* Element UI tree styles override */
.el-tree-node__content :deep(.el-tree-node__content) {
  height: auto;
  padding: 0;
}

.el-tree-node__expand-icon {
  margin-right: 8px;
  color: #999;
}

.el-tree-node__expand-icon.is-leaf {
  display: none;
}

.el-tree-node {
  margin-bottom: 4px; /* 增加节点间距 */
}

/* 隐藏复选框 */
/* :deep(.el-checkbox) {
  display: none !important; 
} */

.el-tree-node.is-checked > .el-tree-node__content {
  background-color: #e6f7ff; /* 自定义选中背景色 */
  border-left: 3px solid #1890ff; /* 添加左侧边框表示选中 */
}

.el-tree-node.is-checked > .el-tree-node__content .custom-tree-node {
  background-color: #e6f7ff;
  color: #1890ff; /* 选中时文字颜色 */
}

.sortable-item {
  display: flex;
  align-items: center;
  padding: 5px;
  border: 1px solid #ddd;
  margin-bottom: 5px;
  background: #f9f9f9;
}

.drag-handle {
  margin-right: 10px;
  cursor: move;
}
</style>
