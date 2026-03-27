<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="500px"
    append-to-body
    @close="handleCancel"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="标签名称" prop="tagName">
        <el-input v-model="form.tagName" placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="标签编码" prop="tagCode">
        <el-input v-model="form.tagCode" placeholder="请输入标签编码" />
      </el-form-item>
      <el-form-item label="标签类型" prop="tagType">
        <el-select v-model="form.tagType" placeholder="请选择标签类型">
          <el-option
            v-for="dict in dict.type.erp_tag_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          ></el-option>
        </el-select>
      </el-form-item>
      <!-- <el-form-item v-if="form.tagType === 'MENU'" label="父级" prop="parentId">
        <treeselect
          v-model="form.parentId"
          :options="tagOptions"
          :normalizer="normalizer"
          placeholder="请选择父级"
        />
      </el-form-item> -->
      <el-form-item
        v-if="form.tagType === 'MENU'"
        label="父级标签"
        prop="parentId"
      >
        <el-cascader
          v-model="form.parentId"
          :options="tagOptions"
          :props="{
            value: 'tagId',
            label: 'tagName',
            children: 'children',
            checkStrictly: true,
            emitPath: false,
            expandTrigger: 'hover',
          }"
          placeholder="请选择父级标签 (不选表示顶级)"
          clearable
          style="width: 100%"
          :loading="menuLoading"
        />
      </el-form-item>
      <el-form-item
        v-if="form.tagType == 'MENU'"
        label="SPU 前缀"
        prop="spuPrefix"
      >
        <el-input v-model="form.spuPrefix" placeholder="请输入SPU 前缀" />
      </el-form-item>
      <el-form-item
        v-if="form.tagType === 'MENU'"
        label="当前最大流水号"
        prop="currentMaxSeq"
      >
        <el-input
          v-model="form.currentMaxSeq"
          placeholder="请输入当前最大流水号"
        />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          placeholder="请输入内容"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="handleSubmit">确 定</el-button>
      <el-button @click="handleCancel">取 消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import { treeList, addTag, updateTag } from "@/api/erp/tag";
import { watch } from "vue";

export default {
  name: "TagDialog",
  dicts: ["erp_tag_type"],
  components: {
    Treeselect,
  },
  props: {},
  data() {
    return {
      title: "",
      form: {},
      rules: {
        tagName: [
          { required: true, message: "标签名称不能为空", trigger: "blur" },
        ],
        tagCode: [
          { required: true, message: "标签编码不能为空", trigger: "blur" },
        ],
        tagType: [
          { required: true, message: "标签类型不能为空", trigger: "change" },
        ],
      },
      tagOptions: [],
      menuLoading: false,
      rowData: null, // 用于接收父组件传递的行数据
      visible: false, // 控制对话框显示
    };
  },
  created() {
    this.fetchTags();
    if (this.rowData && this.rowData.tagId) {
      this.title = "修改标签";
      this.form = { ...this.rowData };
    } else {
      this.title = "新增标签";
      this.resetForm();
    }
  },
  watch: {
    tagType(newType) {
      if (newType === "MENU") {
        this.fetchTags();
      }
    },
  },
  methods: {
    open(data) {
      this.visible = true;
      this.rowData = data;
    },
    fetchTags() {
      this.menuLoading = true;
      treeList("MENU")
        .then((response) => {
          const rows = response.rows || [];
          this.tagOptions = rows;
          // this.tagOptions = this.buildTreeData(rows);
        })
        .finally(() => {
          this.menuLoading = false;
        });
    },
    buildTreeData(list) {
      const map = {};
      const result = [];
      list.forEach((item) => {
        map[item.tagId] = { ...item, children: [] };
      });
      list.forEach((item) => {
        if (
          item.parentId === 0 ||
          item.parentId === "0" ||
          item.parentId === null
        ) {
          result.push(map[item.tagId]);
        } else {
          const parent = map[item.parentId];
          if (parent) {
            parent.children.push(map[item.tagId]);
          }
        }
      });
      return result;
    },
    resetForm() {
      this.form = {
        tagId: null,
        tagName: null,
        tagCode: null,
        tagType: "MENU",
        sortOrder: null,
        parentId: null,
        menuLevel: null,
        spuPrefix: null,
        currentMaxSeq: 0,
        remark: null,
      };
    },
    handleSubmit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          const apiCall = this.form.tagId
            ? updateTag(this.form)
            : addTag(this.form);
          apiCall
            .then(() => {
              this.$emit("success");
              this.visible = false;
            })
            .catch(() => {
              this.$message.error(this.form.tagId ? "修改失败" : "新增失败");
            });
        }
      });
    },
    handleCancel() {
      this.visible = false;
      this.resetForm();
    },
  },
};
</script>
