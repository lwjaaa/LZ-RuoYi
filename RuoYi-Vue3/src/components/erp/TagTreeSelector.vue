<template>
  <div class="tag-tree-selector">
    <!-- 标签树 -->
    <el-tree
      ref="tree"
      :data="tagList"
      :props="treeProps"
      show-checkbox
      node-key="tagId"
      :check-strictly="true"
      :check-on-click-node="true"
      :expand-on-click-node="false"
      default-expand-all
      draggable
      :allow-drop="allowDrop"
      @check="handleCheck"
      @node-drop="handleNodeDrop"
    >
      <template v-slot="{ data }">
        <span
          class="custom-tree-node"
          :class="{ 'selected-node': selectedTagIds.includes(data.tagId) }"
          @mouseenter="hoveredNode = data.tagId"
          @mouseleave="hoveredNode = null"
        >
          <el-icon class="node-icon" v-if="data.tagType === 'MENU'">
            <FolderOpened />
          </el-icon>
          <el-icon class="node-icon" v-else>
            <Ticket />
          </el-icon>
          <span class="node-label"
            >{{ data.tagName }} ({{ data.tagCode }})</span
          >
          <span
            class="hover-buttons"
            :class="{ visible: hoveredNode === data.tagId }"
          >
            <el-dropdown
              trigger="click"
              @command="(command) => handleMoreCommand(data, command)"
            >
              <span class="el-dropdown-link" @click.stop>
                <el-button size="small">
                  <el-icon><More /></el-icon>
                </el-button>
              </span>
              <template #dropdown>
                <el-dropdown-item command="edit">
                  <el-icon><Edit /></el-icon> 编辑
                </el-dropdown-item>
                <el-dropdown-item command="delete">
                  <el-icon><Delete /></el-icon> 删除
                </el-dropdown-item>
                <el-dropdown-item v-if="data.tagType === 'MENU'" command="add">
                  <el-icon><Plus /></el-icon> 新增
                </el-dropdown-item>
              </template>
            </el-dropdown>
            <el-button size="small" @click.stop="pinToTop(data)">
              <el-icon><ArrowUp /></el-icon>
            </el-button>
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 引用独立的标签编辑组件 -->
    <TagDialog ref="tagModal" @success="handleDialogSuccess" />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from "vue";
import { ElIcon, ElMessage, ElMessageBox } from "element-plus";
import {
  FolderOpened,
  Ticket,
  Plus,
  Edit,
  Delete,
  ArrowUp,
  More,
} from "@element-plus/icons-vue";
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
const expandedButtons = ref(null);
const selectedTagIds = ref([]);

const treeProps = {
  children: "children",
  label: "tagName",
};

// Methods
const loadTags = async () => {
  const response = await treeList("ALL");
  tagList.value = response.data;
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
  selectedTagIds.value = normalized.map((node) => node.tagId);
  emit("selection-change", normalized);
};

const handleCheck = (checkedNodes, options) => {
  updateSelection();
  console.log("handleCheck-选中的节点：", tree.value.getCheckedNodes());
};

const clearSelection = () => {
  tree.value.setCheckedKeys([]);
  selectedTagIds.value = [];
  emit("selection-change", []);
};

const toggleMore = (tagId) => {
  expandedButtons.value = expandedButtons.value === tagId ? null : tagId;
};

const handleMoreCommand = (data, command) => {
  if (command === "edit") {
    openEditDialog(data);
  } else if (command === "delete") {
    deleteTag(data);
  } else if (command === "add") {
    openAddDialogFor(data);
  }
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
onMounted(async () => {
  await loadTags();
  if (props.selectedTags && props.selectedTags.length > 0) {
    const ids = props.selectedTags.map((tag) => tag.tagId);
    tree.value.setCheckedKeys(ids);
    selectedTagIds.value = ids;
  }
});

// Watch for changes in selectedTags prop
watch(
  () => props.selectedTags,
  (newSelectedTags) => {
    if (newSelectedTags && newSelectedTags.length > 0) {
      const ids = newSelectedTags.map((tag) => tag.tagId);
      tree.value.setCheckedKeys(ids);
      selectedTagIds.value = ids;
    } else {
      tree.value.setCheckedKeys([]);
      selectedTagIds.value = [];
    }
  },
  { deep: true },
);
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
  padding: 6px 8px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
  cursor: pointer;
  min-height: 32px;
}

.custom-tree-node:hover,
.custom-tree-node.selected-node,
.el-tree-node.is-checked > .el-tree-node__content .custom-tree-node {
  background-color: #e6f7ff; /* 一致的高亮背景 */
  color: #1890ff; /* 一致的高亮文字 */
  border-left: 3px solid #1890ff;
}

.node-icon {
  margin-right: 8px;
  color: #666;
  font-size: 14px;
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
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition:
    opacity 0.2s ease,
    visibility 0.2s ease;
}

.hover-buttons.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.custom-tree-node.selected-node,
.el-tree-node.is-checked > .el-tree-node__content .custom-tree-node {
  background-color: #e6f7ff;
  color: #1890ff;
  border-left: 3px solid #1890ff;
}

.expanded-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 4px;
}

/* Element Plus tree styles override */
:deep(.el-tree-node__content) {
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
  margin-bottom: 2px;
}

/* 隐藏复选框 */
:deep(.el-tree-node__content .el-checkbox),
:deep(.el-tree-node__content .el-checkbox__inner),
:deep(.el-checkbox),
:deep(.el-checkbox__inner) {
  display: none !important;
}

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
