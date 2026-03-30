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
              size="mini"
              icon="Plus"
              @click.stop="openAddDialogFor(data)"
              >新增</el-button
            >
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
import { treeList, addTag, updateTag, dragNode, delTag } from "@/api/erp/tag";
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
    normalizeTag(node) {
      const data = (node && node.data) || node || {};
      return {
        tagId: data.tagId,
        tagName: data.tagName,
        tagCode: data.tagCode,
        tagType: data.tagType,
        parentId: data.parentId,
        sortOrder: data.sortOrder,
      };
    },
    updateSelection() {
      const checkedNodes = this.$refs.tree.getCheckedNodes();
      const normalized = checkedNodes.map((node) => this.normalizeTag(node));
      this.$emit("selection-change", normalized);
    },
    handleCheck() {
      this.updateSelection();
    },
    handleNodeClick(data) {
      this.$refs.tree.setChecked(
        data.tagId,
        !this.$refs.tree
          .getCheckedNodes()
          .some((node) => node.tagId === data.tagId)
      );
      this.$nextTick(() => {
        this.updateSelection();
      });
    },
    clearSelection() {
      this.$refs.tree.setCheckedKeys([]);
      this.$emit("selection-change", []);
    },
    // 新增编辑标签对话框
    openAddDialog() {
      this.$refs.tagModal.open(null);
    },
    openAddDialogFor(data) {
      // 新增子标签，传递 parentId
      this.$refs.tagModal.open(null, { parentId: data.tagId });
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
      // 置顶逻辑：根据标签类型设置不同的排序
      const siblings = this.getSiblings(data.parentId);
      const menuTags = siblings.filter((tag) => tag.tagType === "MENU");
      const otherTags = siblings.filter((tag) => tag.tagType !== "MENU");

      let newSortOrder;
      if (data.tagType === "MENU") {
        // MENU类型置顶到最前面
        newSortOrder = Math.min(...siblings.map((s) => s.sortOrder)) - 1;
      } else {
        // 其他类型置顶到其他类型的最前面（在MENU类型之后）
        if (menuTags.length > 0) {
          const maxMenuSort = Math.max(...menuTags.map((s) => s.sortOrder));
          newSortOrder = maxMenuSort + 1;
        } else {
          newSortOrder = Math.min(...siblings.map((s) => s.sortOrder)) - 1;
        }
      }

      updateTag({ ...data, sortOrder: newSortOrder }).then(() => {
        this.loadTags();
      });
    },
    getSiblings(parentId) {
      return this.tagList.filter((tag) => tag.parentId === parentId);
    },
    calculateSortOrder(draggingData, dropData, dropType) {
      const siblings = this.getSiblings(dropData.parentId || 0);

      if (dropType === "inner") {
        // 插入到子级，排在最后
        const children = siblings.filter(
          (tag) => tag.parentId === dropData.tagId
        );
        return children.length > 0
          ? Math.max(...children.map((tag) => tag.sortOrder)) + 1
          : 1;
      } else {
        // before 或 after
        const dropIndex = siblings.findIndex(
          (tag) => tag.tagId === dropData.tagId
        );
        if (dropType === "before") {
          return dropData.sortOrder - 0.5;
        } else {
          return dropData.sortOrder + 0.5;
        }
      }
    },
    allowDrop(draggingNode, dropNode, type) {
      // 拖拽时判定目标节点能否被放置。
      // type 参数有三种情况：'prev'、'inner' 和 'next'，
      // 分别表示放置在目标节点前、插入至目标节点和放置在目标节点后

      if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
        return false;
      }
      const draggingData = draggingNode.data;
      const dropData = dropNode.data;

      if (draggingData.tagType === "MENU") {
        // MENU 节点只能拖到 MENU 节点的前后或内部，不能拖到其他类型节点附近
        return dropData.tagType === "MENU";
      } else {
        // 非 MENU 节点只能拖到 MENU 节点的后面（next），不能有子级
        return dropData.tagType === "MENU" && type === "next";
      }
    },
    handleNodeDrop(draggingNode, dropNode, dropType) {
      // 拖拽成功完成时触发的事件
      // 共四个参数，依次为：被拖拽节点对应的 Node、结束拖拽时最后进入的节点、被拖拽节点的放置位置
      // （before、after、inner）、event

      if (!draggingNode || !draggingNode.data || !dropNode || !dropNode.data) {
        return;
      }

      const draggingData = draggingNode.data;
      const dropData = dropNode.data;

      // 正常拖拽逻辑
      const dropParentId =
        dropType === "inner"
          ? dropData.tagId
          : dropData.parentId === 0
          ? 0
          : dropData.parentId;

      const updatedNode = {
        tagId: draggingData.tagId,
        parentId: dropParentId,
        sortOrder: this.calculateSortOrder(draggingData, dropData, dropType),
      };

      console.log("更新节点:", updatedNode);
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
.el-tree-node__content >>> .el-tree-node__content,
.el-tree-node__content /deep/ .el-tree-node__content {
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

.el-tree-node__content >>> .el-checkbox,
.el-tree-node__content /deep/ .el-checkbox,
.el-tree-node__content >>> .el-checkbox__inner,
.el-tree-node__content /deep/ .el-checkbox__inner {
  display: none !important; /* 隐藏复选框 */
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
