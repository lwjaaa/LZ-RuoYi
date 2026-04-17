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
            <!-- 置顶 -->
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

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, getCurrentInstance } from "vue";
import { ElIcon, ElMessage, ElMessageBox } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import {
  FolderOpened,
  Ticket,
  Plus,
  Edit,
  Delete,
  ArrowUp,
  More,
} from "@element-plus/icons-vue";
import {
  treeList,
  addTag,
  updateTag,
  dragNode,
  delTag,
  top,
} from "@/api/erp/tag";
import TagDialog from "@/components/erp/TagDialog.vue";
import type { TagDictMenu, DragNodeData } from "@/types/erp";

const { proxy } = getCurrentInstance() as any;

interface TagSelection {
  tagId: number;
  tagName: string;
  tagCode?: string;
  tagType?: string;
  parentId?: number;
  sortOrder?: number;
}

const props = defineProps<{
  selectedTags?: TagSelection[];
}>();

const emit = defineEmits<{
  (e: "selection-change", tags: TagSelection[]): void;
}>();

const tree = ref<any>(null);
const tagModal = ref<any>(null);

const tagList = ref<TagDictMenu[]>([]);
const hoveredNode = ref<number | null>(null);
const selectedTagIds = ref<number[]>([]);

const treeProps = {
  children: "children",
  label: "tagName",
};

const loadTags = async (): Promise<void> => {
  const response = await treeList("ALL");
  tagList.value = response.data;
};

const normalizeTag = (node: any): TagSelection => {
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

const updateSelection = (): void => {
  const checkedNodes = tree.value.getCheckedNodes();
  const normalized = checkedNodes.map((node: any) => normalizeTag(node));
  selectedTagIds.value = normalized.map((node: TagSelection) => node.tagId);
  emit("selection-change", normalized);
};

const handleCheck = (): void => {
  updateSelection();
};

const clearSelection = (): void => {
  tree.value.setCheckedKeys([]);
  selectedTagIds.value = [];
  emit("selection-change", []);
};

const handleMoreCommand = (data: TagDictMenu, command: string): void => {
  if (command === "edit") {
    openEditDialog(data);
  } else if (command === "delete") {
    deleteTag(data);
  } else if (command === "add") {
    openAddDialogFor(data);
  }
};

const openAddDialog = (options: { parentId?: number } | null = null): void => {
  tagModal.value.open(options);
};

const openAddDialogFor = (data: TagDictMenu): void => {
  tagModal.value.open({ parentId: data.tagId });
};

const openEditDialog = (data: TagDictMenu): void => {
  tagModal.value.open(data);
};

const handleDialogSuccess = (): void => {
  loadTags();
};

const deleteTag = (data: TagDictMenu): void => {
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

const pinToTop = (data: TagDictMenu): void => {
  top(data.tagId)
    .then(() => {
      proxy.$modal.msgSuccess("置顶成功");
      loadTags();
    })
    .catch(() => {
      proxy.$modal.msgError("置顶失败");
    });
};

const allowDrop = (draggingNode: any, dropNode: any, type: string): boolean => {
  if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
    return false;
  }
  const draggingData = draggingNode.data;
  const dropData = dropNode.data;

  if (draggingData.tagType === "MENU") {
    return dropData.tagType === "MENU";
  } else {
    return dropData.tagType !== "MENU" && type !== "inner";
  }
};

const handleNodeDrop = (
  draggingNode: any,
  dropNode: any,
  dropType: string,
): void => {
  if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
    return;
  }

  const draggingData = draggingNode.data;
  const dropData = dropNode.data;
  const TreeDragDTO: DragNodeData = {
    tagId: draggingData.tagId,
    targetParentId: dropData.tagId,
  };

  dragNode(TreeDragDTO)
    .then(() => {
      proxy.$modal.msgSuccess("移动成功");
      loadTags();
    })
    .catch(() => {
      proxy.$modal.msgError("移动失败");
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
  gap: 8px; /* 添加间距 */
}

.node-icon {
  flex-shrink: 0; /* 防止图标被压缩 */
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

  min-width: 0; /* 允许文本截断 */
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  overflow: hidden; /* 隐藏溢出 */
  text-overflow: ellipsis; /* 显示省略号 */
  white-space: nowrap; /* 单行显示 */
}

.hover-buttons {
  flex-grow: 0; /* 不增长 */
  flex-shrink: 0; /* 防止按钮被压缩 */
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: opacity 0.2s ease, visibility 0.2s ease;
}

.hover-buttons.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

:deep(.el-tree-node__content:hover),
:deep(.el-tree-node.is-checked > .el-tree-node__content) {
  background-color: #e6f7ff; /* 一致的高亮背景 */
  color: #1890ff; /* 一致的高亮文字 */
  position: relative;
}
/* :deep(.el-tree-node__content:hover)::before, */
:deep(.el-tree-node.is-checked > .el-tree-node__content)::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: #1890ff;
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
