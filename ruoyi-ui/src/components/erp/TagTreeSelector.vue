<template>
  <div class="tag-tree-selector">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button size="small" @click="clearSelection">清除选中</el-button>
      <el-button size="small" type="primary" @click="openAddDialog"
        >新增标签</el-button
      >
    </div>

    <!-- 标签树 -->
    <!-- check-strictly不知道有什么作用 -->
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
              size="mini"
              icon="Edit"
              @click.stop="openEditDialog(data)"
              >编辑</el-button
            >
            <el-button
              size="mini"
              type="danger"
              icon="Delete"
              @click.stop="deleteTag(data)"
              >删除</el-button
            >
            <el-button size="mini" icon="Top" @click.stop="pinToTop(data)"
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

<script>
import draggable from "vuedraggable";
import { treeList, addTag, updateTag, delTag } from "@/api/erp/tag";
// 引入新生成的标签对话框组件
import TagDialog from "@/components/erp/TagDialog.vue";

export default {
  name: "TagTreeSelector",
  components: {
    draggable,
    TagDialog,
  },
  props: {
    selectedTags: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      tagList: [],
      treeProps: {
        children: "children",
        label: "tagName",
      },
      hoveredNode: null,
    };
  },
  mounted() {
    this.loadTags();
  },
  methods: {
    loadTags() {
      treeList("ALL").then((response) => {
        this.tagList = response.data;
      });
    },
    handleCheck(data, checked) {
      this.$emit(
        "selection-change",
        checked
          ? [...this.selectedTags, data]
          : this.selectedTags.filter((tag) => tag.tagId !== data.tagId)
      );
    },
    handleNodeClick(data) {
      this.$refs.tree.setChecked(
        data.tagId,
        !this.$refs.tree
          .getCheckedNodes()
          .some((node) => node.tagId === data.tagId)
      );
    },
    clearSelection() {
      this.$refs.tree.setCheckedKeys([]);
      this.$emit("selection-change", []);
    },
    // 新增编辑标签对话框
    openAddDialog() {
      this.$refs.tagModal.open(null);
    },
    openEditDialog(data) {
      this.$refs.tagModal.open(data);
    },
    // 新增：处理子组件保存成功的回调
    handleDialogSuccess() {
      this.loadTags();
    },
    deleteTag(data) {
      this.$confirm("确定删除该标签吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        delTag(data.tagId).then(() => {
          this.$message.success("删除成功");
          this.loadTags();
        });
      });
    },
    pinToTop(data) {
      // 置顶逻辑：设置sortOrder为同级最小值-1
      const siblings = this.getSiblings(data.parentId);
      const minSort = Math.min(...siblings.map((s) => s.sortOrder));
      updateTag({ ...data, sortOrder: minSort - 1 }).then(() => {
        this.loadTags();
      });
    },
    getSiblings(parentId) {
      return this.tagList.filter((tag) => tag.parentId === parentId);
    },
    allowDrop(draggingNode, dropNode, type) {
      // 拖拽时判定目标节点能否被放置。
      // type 参数有三种情况：'prev'、'inner' 和 'next'，
      // 分别表示放置在目标节点前、插入至目标节点和放置在目标节点后
      console.log(
        "拖动节点:",
        draggingNode,
        "放置目标:",
        dropNode,
        "放置类型:",
        type
      );
      return true;
    },
    handleNodeDrop(draggingNode, dropNode, dropType) {
      // 拖拽成功完成时触发的事件
      // 共四个参数，依次为：被拖拽节点对应的 Node、结束拖拽时最后进入的节点、被拖拽节点的放置位置
      // （before、after、inner）、event
      // 更新父级ID
      const dropParentId =
        dropNode.parentId === 0 ? dropNode.tagId : dropNode.parentId;
      const updatedNode = { ...draggingNode, parentId: dropParentId };
      updateTag(updatedNode)
        .then(() => {
          this.$message.success("移动成功");
          this.loadTags();
        })
        .catch(() => {
          this.$message.error("移动失败");
        });
    },
  },
};
</script>

<style scoped>
.tag-tree-selector {
  height: 100%;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s ease;
  cursor: pointer;
}

.custom-tree-node:hover {
  background-color: #f5f5f5;
}

.node-icon {
  margin-right: 8px;
  color: #666;
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
}

.hover-buttons {
  display: none;
  gap: 4px;
}

.custom-tree-node:hover .hover-buttons {
  display: flex;
}

/* Element UI tree styles override */
:deep(.el-tree-node__content) {
  height: auto;
  padding: 0;
}

:deep(.el-tree-node__expand-icon) {
  margin-right: 4px;
}

:deep(.el-tree-node__expand-icon.is-leaf) {
  display: none;
}

:deep(.el-tree-node.is-checked > .el-tree-node__content) {
  background-color: #ecf5ff;
}

:deep(.el-tree-node.is-checked > .el-tree-node__content .custom-tree-node) {
  background-color: #ecf5ff;
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
