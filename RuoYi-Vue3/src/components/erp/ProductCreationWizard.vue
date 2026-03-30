<template>
  <el-dialog
    title="商品选品向导"
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    width="80%"
    :before-close="handleClose"
    :close-on-click-modal="false"
  >
    <el-steps :active="activeStep" finish-status="success" align-center>
      <el-step title="选品基础信息" description="设置 SPU 和选项"></el-step>
      <el-step title="信息录入" description="完善商品详情"></el-step>
    </el-steps>

    <el-form
      v-if="activeStep === 0"
      :model="step1Form"
      :rules="step1Rules"
      ref="step1Form"
      label-width="120px"
    >
      <el-form-item label="SPU" prop="spu">
        <el-input
          v-model="step1Form.spu"
          placeholder="自动生成或手动输入"
        ></el-input>
      </el-form-item>
      <el-form-item label="来源URL" prop="sourceUrl">
        <el-input v-model="step1Form.sourceUrl"></el-input>
      </el-form-item>
      <el-form-item label="采购链接" prop="purchaseLink">
        <el-input v-model="step1Form.purchaseLink"></el-input>
      </el-form-item>
      <el-form-item label="采购商品选项">
        <div
          v-for="(option, index) in step1Form.purchaseOptions"
          :key="index"
          class="option-item"
        >
          <el-input
            v-model="option.name"
            placeholder="选项名称"
            style="width: 30%; margin-right: 10px"
          ></el-input>
          <el-input
            v-model="option.values"
            placeholder="选项值，用逗号分隔"
            style="width: 50%; margin-right: 10px"
          ></el-input>
          <el-button @click="removeOption(index)" type="danger" size="small"
            >删除</el-button
          >
        </div>
        <el-button @click="addOption" type="primary" size="small"
          >添加选项</el-button
        >
      </el-form-item>

      <!-- 变体预览 -->
      <el-form-item label="变体列表">
        <draggable v-model="variants" group="variants" handle=".drag-handle">
          <div
            v-for="(variant, index) in variants"
            :key="index"
            class="variant-item"
          >
            <i class="el-icon-rank drag-handle"></i>
            <span>{{ variant.title }}</span>
            <el-input
              v-model="variant.purchaseLink"
              placeholder="采购链接"
              style="width: 200px; margin-left: 10px"
            ></el-input>
            <el-input-number
              v-model="variant.purchasePrice"
              :precision="2"
              :min="0"
              placeholder="采购价"
              style="width: 100px; margin-left: 10px"
            ></el-input-number>
            <el-button
              @click="removeVariant(index)"
              type="danger"
              size="small"
              style="margin-left: 10px"
              >删除</el-button
            >
          </div>
        </draggable>
      </el-form-item>
    </el-form>

    <el-form
      v-if="activeStep === 1"
      :model="step2Form"
      :rules="step2Rules"
      ref="step2Form"
      label-width="120px"
    >
      <el-form-item label="商品标题" prop="productTitle">
        <el-input v-model="step2Form.productTitle"></el-input>
      </el-form-item>
      <el-form-item label="商品类别ID" prop="category">
        <el-input v-model="step2Form.category"></el-input>
      </el-form-item>
      <el-form-item label="商品类型" prop="productType">
        <el-input v-model="step2Form.productType"></el-input>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="step2Form.description"></el-input>
      </el-form-item>
      <el-form-item label="大小" prop="size">
        <el-input v-model="step2Form.size"></el-input>
      </el-form-item>
      <el-form-item label="材质" prop="material">
        <el-input v-model="step2Form.material"></el-input>
      </el-form-item>
      <el-form-item label="备注" prop="note">
        <el-input v-model="step2Form.note"></el-input>
      </el-form-item>
      <el-form-item label="包含的包材" prop="packageInclude">
        <el-input v-model="step2Form.packageInclude"></el-input>
      </el-form-item>
      <el-form-item label="商品详情描述" prop="bodyHtml">
        <QuillEditor
          v-model:content="step2Form.bodyHtml"
          :options="editorOption"
          content-type="html"
        />
      </el-form-item>

      <!-- 商品图片列表 -->
      <el-form-item label="商品图片列表">
        <el-button @click="scanMediaLocal" type="primary" size="small"
          >导入媒体</el-button
        >
        <div class="media-grid">
          <div
            v-for="(media, index) in selectedMedia"
            :key="index"
            class="media-item"
            draggable="true"
            @dragstart="onDragStart($event, media)"
          >
            <img :src="media.url" alt="media" />
          </div>
        </div>
      </el-form-item>

      <!-- 变体详细设置 -->
      <el-form-item label="变体设置">
        <el-table :data="variants" style="width: 100%">
          <el-table-column
            prop="title"
            label="选项"
            width="150"
          ></el-table-column>
          <el-table-column label="SKU" width="150">
            <template #default="scope">
              <el-input v-model="scope.row.sku"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="图片" width="200">
            <template #default="scope">
              <div
                class="drop-zone"
                @drop="onDrop($event, scope.row)"
                @dragover.prevent
                @dragenter.prevent
              >
                <img
                  v-if="scope.row.media"
                  :src="scope.row.media.url"
                  alt="variant image"
                />
                <span v-else>拖拽图片到此处</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="包装宽度" width="100">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.pkWidth"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="包装高度" width="100">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.pkHeight"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="包装长度" width="100">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.pkLength"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="材积重" width="100">
            <template #default="scope">
              {{ scope.row.materialWeight }}
            </template>
          </el-table-column>
          <el-table-column label="常规包装重量" width="120">
            <template #default="scope">
              <el-input-number
                v-model="scope.row.pkWeight"
                :min="0"
                @change="calculateCostPrice(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="运费" width="100">
            <template #default="scope">
              {{ scope.row.freight }}
            </template>
          </el-table-column>
          <el-table-column label="商品成本价" width="120">
            <template #default="scope">
              {{ scope.row.unitCostPrice }}
            </template>
          </el-table-column>
          <el-table-column label="销售价格" width="120">
            <template slot-scope="scope">
              {{ scope.row.price }}
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="activeStep === 0" @click="nextStep" type="primary"
          >下一步</el-button
        >
        <el-button v-if="activeStep === 1" @click="prevStep">上一步</el-button>
        <el-button v-if="activeStep === 0" @click="saveAndNew" type="success"
          >保存并新增</el-button
        >
        <el-button v-if="activeStep === 0" @click="saveAndClose" type="primary"
          >保存并关闭</el-button
        >
        <el-button v-if="activeStep === 1" @click="saveProduct" type="primary"
          >保存商品</el-button
        >
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from "vue";
import draggable from "vuedraggable";
import "quill/dist/quill.snow.css";
import { QuillEditor } from "@vueup/vue-quill";
import { scanMedia } from "@/api/erp/media";
import { addProduct, updateProduct } from "@/api/erp/product";
import { updateTag } from "@/api/erp/tag";
import { ElMessage } from "element-plus";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  selectedTags: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(["update:visible"]);

// 响应式数据
const activeStep = ref(0);
const step1Form = reactive({
  spu: "",
  sourceUrl: "",
  purchaseLink: "",
  purchaseOptions: [],
});
const step1Rules = reactive({
  spu: [{ required: true, message: "请输入 SPU", trigger: "blur" }],
  sourceUrl: [{ required: true, message: "请输入来源URL", trigger: "blur" }],
  purchaseLink: [
    { required: true, message: "请输入采购链接", trigger: "blur" },
  ],
});
const step2Form = reactive({
  productTitle: "",
  category: "",
  productType: "",
  description: "",
  size: "",
  material: "",
  note: "",
  packageInclude: "",
  bodyHtml: "",
});
const step2Rules = reactive({
  productTitle: [
    { required: true, message: "请输入商品标题", trigger: "blur" },
  ],
  category: [{ required: true, message: "请输入商品类别ID", trigger: "blur" }],
  productType: [{ required: true, message: "请输入商品类型", trigger: "blur" }],
});
const variants = ref([]);
const selectedMedia = ref([]);
const draggedMedia = ref(null);
const editorOption = reactive({
  theme: "snow",
});
const isEdit = ref(false);

// 表单引用
const step1FormRef = ref(null);
const step2FormRef = ref(null);

// 监听器
watch(
  () => props.visible,
  (val) => {
    if (val) {
      initWizard();
    }
  },
);

watch(
  () => step1Form.purchaseOptions,
  () => {
    generateVariants();
  },
  { deep: true },
);

// 方法
const initWizard = () => {
  activeStep.value = 0;
  generateSPU();
  step1Form.purchaseOptions = [];
  variants.value = [
    {
      title: "Default Title",
      purchaseLink: "",
      purchasePrice: 0,
      sku: "",
      media: null,
      pkWidth: 0,
      pkHeight: 0,
      pkLength: 0,
      materialWeight: 0,
      pkWeight: 0,
      freight: 0,
      unitCostPrice: 0,
      price: 0,
    },
  ];
  Object.assign(step2Form, {
    productTitle: "",
    category: "",
    productType: "",
    description: "",
    size: "",
    material: "",
    note: "",
    packageInclude: "",
    bodyHtml: "",
  });
  selectedMedia.value = [];
  isEdit.value = false;
};
const generateSPU = () => {
  const menuTag = props.selectedTags.find((tag) => tag.tagType === "MENU");
  if (menuTag) {
    const prefix = menuTag.spuPrefix;
    const seq = menuTag.currentMaxSeq + 1;
    step1Form.spu = `${prefix}-${String(seq).padStart(3, "0")}`;
  }
};

const addOption = () => {
  step1Form.purchaseOptions.push({ name: "", values: "" });
};

const removeOption = (index) => {
  step1Form.purchaseOptions.splice(index, 1);
};
const generateVariants = () => {
  const options = step1Form.purchaseOptions.filter(
    (opt) => opt.name && opt.values,
  );
  if (options.length === 0) {
    variants.value = [
      {
        title: "Default Title",
        purchaseLink: step1Form.purchaseLink,
        purchasePrice: 0,
        sku: "",
        media: null,
        pkWidth: 0,
        pkHeight: 0,
        pkLength: 0,
        materialWeight: 0,
        pkWeight: 0,
        freight: 0,
        unitCostPrice: 0,
        price: 0,
      },
    ];
    return;
  }
  const combinations = cartesianProduct(
    options.map((opt) => opt.values.split(",").map((v) => v.trim())),
  );
  variants.value = combinations.map((combo) => {
    const title = combo.join(" / ");
    return {
      title,
      purchaseLink: step1Form.purchaseLink,
      purchasePrice: 0,
      sku: "",
      media: null,
      pkWidth: 0,
      pkHeight: 0,
      pkLength: 0,
      materialWeight: 0,
      pkWeight: 0,
      freight: 0,
      unitCostPrice: 0,
      price: 0,
    };
  });
};

const cartesianProduct = (arrays) => {
  return arrays.reduce(
    (acc, curr) => acc.flatMap((a) => curr.map((b) => [...a, b])),
    [[]],
  );
};

const removeVariant = (index) => {
  variants.value.splice(index, 1);
};

const nextStep = () => {
  step1FormRef.value.validate((valid) => {
    if (valid) {
      activeStep.value = 1;
    }
  });
};

const prevStep = () => {
  activeStep.value = 0;
};

const saveAndNew = () => {
  saveProduct(true);
};

const saveAndClose = () => {
  saveProduct(false);
};

const saveProduct = async (isNew = false) => {
  const form = activeStep.value === 0 ? step1FormRef.value : step2FormRef.value;
  form.validate(async (valid) => {
    if (valid) {
      const productData = {
        ...step1Form,
        ...step2Form,
        selectedTagIds: props.selectedTags.map((tag) => tag.tagId),
        variants: variants.value,
        media: selectedMedia.value,
      };
      const api = isEdit.value ? updateProduct : addProduct;
      try {
        await api(productData);
        ElMessage.success("保存成功");
        // 更新标签的 currentMaxSeq 只在新增时
        if (!isEdit.value) {
          const menuTag = props.selectedTags.find(
            (tag) => tag.tagType === "MENU",
          );
          if (menuTag) {
            await updateTag({
              ...menuTag,
              currentMaxSeq: menuTag.currentMaxSeq + 1,
            });
            // 可选：重新加载标签
          }
        }
        if (isNew) {
          initWizard();
        } else {
          emit("update:visible", false);
        }
      } catch (error) {
        ElMessage.error("保存失败");
      }
    }
  });
};

const handleClose = () => {
  emit("update:visible", false);
};

const loadProductData = (product) => {
  // 加载产品数据用于编辑
  Object.assign(step1Form, {
    spu: product.spu,
    sourceUrl: product.sourceUrl,
    purchaseLink: product.purchaseLink,
    purchaseOptions: product.purchaseOptions || [],
  });
  Object.assign(step2Form, {
    productTitle: product.productTitle,
    category: product.category,
    productType: product.productType,
    description: product.description,
    size: product.size,
    material: product.material,
    note: product.note,
    packageInclude: product.packageInclude,
    bodyHtml: product.bodyHtml,
  });
  variants.value = product.variants || [];
  selectedMedia.value = product.media || [];
  // 设置为编辑模式
  isEdit.value = true;
};

// 拖拽相关方法
const onDragStart = (event, media) => {
  draggedMedia.value = media;
};

const onDrop = (event, row) => {
  event.preventDefault();
  if (draggedMedia.value) {
    row.media = draggedMedia.value;
    draggedMedia.value = null;
  }
};

// 计算相关方法
const calculateVolumeWeight = (row) => {
  // 材积重计算逻辑
  row.materialWeight = (row.pkWidth * row.pkHeight * row.pkLength) / 5000;
};

const calculateCostPrice = (row) => {
  // 成本价计算逻辑
  row.unitCostPrice = row.pkWeight + row.materialWeight + row.freight;
};

// 媒体相关方法
const scanMediaLocal = async () => {
  try {
    const response = await scanMedia();
    selectedMedia.value = response.data || [];
  } catch (error) {
    ElMessage.error("导入媒体失败");
  }
};

// 暴露方法给父组件
defineExpose({
  loadProductData,
});
</script>

<style scoped>
.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.variant-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.drag-handle {
  cursor: move;
  margin-right: 10px;
}

.media-grid {
  display: flex;
  flex-wrap: wrap;
}

.media-item {
  position: relative;
  width: 100px;
  height: 100px;
  margin-right: 10px;
  margin-bottom: 10px;
  cursor: pointer;
}

.drop-zone {
  width: 100px;
  height: 100px;
  border: 2px dashed #d9d9d9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

.el-dialog {
  max-width: 1200px;
}

.el-form-item {
  margin-bottom: 20px;
}
</style>
