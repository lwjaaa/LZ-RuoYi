<template>
  <el-dialog
    :title="title"
    v-model="visible"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-steps
      :active="activeStep"
      finish-status="success"
      align-center
      class="mb-4"
    >
      <el-step title="选品基础信息" />
      <el-step title="信息录入" />
    </el-steps>

    <!-- 第一步：选品基础信息 -->
    <div v-show="activeStep === 0" class="step-content">
      <el-form
        ref="step1FormRef"
        :model="formData"
        :rules="step1Rules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标签" prop="tagIds">
              <el-cascader
                v-model="formData.tagIds"
                :options="tagList"
                :props="{
                  value: 'tagId',
                  label: 'tagName',
                  children: 'children',
                  multiple: true,
                  expandTrigger: 'hover',
                }"
                placeholder="请选择标签"
                clearable
                style="width: 100%"
                :loading="menuLoading"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SPU" prop="spu">
              <el-input v-model="formData.spu" placeholder="自动生成或手动输入">
                <template #append>
                  <el-button
                    @click="generateSpu(false)"
                    :loading="spuGenerating"
                    :disabled="spuGenerating"
                    >生成</el-button
                  >
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="来源 URL" prop="sourceUrl">
              <el-input
                ref="sourceUrlInputRef"
                v-model="formData.sourceUrl"
                type="textarea"
                :rows="2"
                placeholder="请输入来源 URL"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购链接" prop="purchaseUrl">
              <el-input
                v-model="formData.purchaseUrl"
                type="textarea"
                :rows="2"
                placeholder="请输入采购链接"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 采购商品选项 -->
        <el-form-item label="采购商品选项">
          <div class="options-container">
            <div
              v-for="(option, optIndex) in optionList"
              :key="optIndex"
              class="option-row"
              :class="{ 'option-row-collapsed': option.collapsed }"
            >
              <!-- 选项值列表（可收起） -->
              <div class="option-values-container" v-show="!option.collapsed">
                <div class="option-value-row">
                  <div class="input-label">选项名称</div>
                  <div class="input-combined">
                    <el-input
                      v-model="option.purchaseName"
                      placeholder="采购选项名称"
                      class="input-left"
                      size="small"
                    />
                    <el-input
                      v-model="option.productName"
                      placeholder="英文选项名称"
                      class="input-right"
                      size="small"
                    />
                    <div class="delete-button-wrapper">
                      <!-- <el-button
                      type="danger"
                      icon="Delete"
                      circle
                      size="small"
                      @click="removeOption(optIndex)"
                      class="ml-2"
                    /> -->
                    </div>
                  </div>
                </div>
                <div
                  v-for="(value, valIndex) in option.values"
                  :key="valIndex"
                  class="option-value-row"
                >
                  <div class="input-label">选项值</div>
                  <div class="input-combined">
                    <el-input
                      v-model="value.purchaseValue"
                      placeholder="采购选项值"
                      class="input-left"
                      size="small"
                      @keyup.enter.prevent="
                        handleEnterKey($event, optIndex, valIndex, 'purchase')
                      "
                      @input="handleValueInput(optIndex, valIndex)"
                    />
                    <el-input
                      v-model="value.productValue"
                      placeholder="英文选项值"
                      class="input-right"
                      size="small"
                      @keyup.enter.prevent="
                        handleEnterKey($event, optIndex, valIndex, 'product')
                      "
                    />
                    <div class="delete-button-wrapper">
                      <el-button
                        v-if="valIndex < option.values.length - 1"
                        type="danger"
                        icon="Delete"
                        circle
                        size="small"
                        @click="removeOptionValue(optIndex, valIndex)"
                        class="ml-2"
                      />
                    </div>
                  </div>
                </div>
                <div class="option-footer" v-show="!option.collapsed">
                  <el-button
                    type="danger"
                    size="small"
                    @click="removeOption(optIndex)"
                  >
                    删除
                  </el-button>
                  <el-button
                    type="primary"
                    size="small"
                    @click="toggleCollapse(optIndex)"
                  >
                    {{ option.collapsed ? "展开" : "完成" }}
                  </el-button>
                </div>
              </div>

              <!-- 底部操作按钮 -->

              <!-- 收起状态提示 -->
              <div class="option-collapsed-hint" v-show="option.collapsed">
                <div class="collapsed-content">
                  <span class="option-name-display">
                    <strong>{{ option.purchaseName || "采购选项" }}</strong>
                    <span class="en-name"
                      >[{{ option.productName || "商品选项" }}]</span
                    >
                  </span>
                  <div class="option-values-display">
                    <template v-for="(value, idx) in option.values" :key="idx">
                      <el-tag
                        v-if="value.purchaseValue || value.productValue"
                        size="small"
                        class="value-tag"
                      >
                        {{ value.purchaseValue || "-" }}
                        <span class="en-value"
                          >[{{ value.productValue || "-" }}]</span
                        >
                      </el-tag>
                    </template>
                  </div>
                </div>
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="toggleCollapse(optIndex)"
                  class="expand-btn"
                >
                  展开
                </el-button>
              </div>
            </div>
            <el-button
              type="primary"
              icon="Plus"
              @click="addOption"
              size="small"
            >
              添加选项
            </el-button>
          </div>
        </el-form-item>

        <!-- 变体分录表格 -->
        <el-form-item label="变体分录">
          <el-table
            :data="variants"
            border
            style="width: 100%"
            row-key="variantId"
            @row-drop="handleRowDrop"
          >
            <!-- 动态生成选项列 -->
            <template v-if="getActiveOptions().length > 0">
              <el-table-column
                v-for="(opt, idx) in getActiveOptions()"
                :key="opt.purchaseName || idx"
                :label="opt.purchaseName || '选项'"
                width="120"
                align="center"
              >
                <template #default="{ row }">
                  <span>{{
                    `${row.optionValueList[idx]?.purchaseName}[${row.optionValueList[idx]?.optionValue}]` ||
                    "-"
                  }}</span>
                </template>
              </el-table-column>
            </template>

            <!-- 当没有选项时，显示默认规格列 -->
            <el-table-column v-else label="默认规格" width="120" align="center">
              <template #default>
                <span>-</span>
              </template>
            </el-table-column>

            <el-table-column
              label="采购链接"
              prop="purchaseUrl"
              min-width="200"
            >
              <template #default="{ row }">
                <el-input
                  v-model="row.purchaseUrl"
                  placeholder="继承主商品"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="采购价" prop="purchasePrice" width="120">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.purchasePrice"
                  :min="0"
                  :precision="2"
                  size="small"
                  style="width: 100%"
                  @change="calculateVariantCost(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row, $index }">
                <el-button
                  v-if="variants.length > 1"
                  type="danger"
                  icon="Delete"
                  circle
                  size="small"
                  @click="removeVariant($index)"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
    </div>

    <!-- 第二步：信息录入 -->
    <div v-show="activeStep === 1" class="step-content">
      <el-form
        ref="step2FormRef"
        :model="formData"
        :rules="step2Rules"
        label-width="120px"
      >
        <!-- 主商品信息 -->
        <el-divider content-position="left">主商品信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="SPU" prop="spu">
              <el-input v-model="formData.spu" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品标题" prop="productTitle">
              <el-input
                v-model="formData.productTitle"
                placeholder="请输入商品标题"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品类别 ID" prop="category">
              <el-input
                v-model="formData.category"
                placeholder="请输入商品类别 ID"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="商品类型" prop="productType">
              <el-input
                v-model="formData.productType"
                placeholder="请输入商品类型"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="大小" prop="size">
              <el-input v-model="formData.size" placeholder="请输入大小" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="材质" prop="material">
              <el-input v-model="formData.material" placeholder="请输入材质" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="备注" prop="note">
              <el-input
                v-model="formData.note"
                type="textarea"
                :rows="2"
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="包含的包材" prop="packageInclude">
              <el-input
                v-model="formData.packageInclude"
                type="textarea"
                :rows="2"
                placeholder="请输入包含的包材"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="商品详情描述">
          <el-input
            v-model="formData.bodyHtml"
            type="textarea"
            :rows="6"
            placeholder="请输入商品详情描述 (HTML)"
          />
        </el-form-item>

        <!-- 商品图片列表 -->
        <el-divider content-position="left">商品图片管理</el-divider>
        <el-form-item label="图片列表">
          <div class="image-manager">
            <div class="image-toolbar mb-2">
              <el-button
                type="primary"
                icon="Upload"
                @click="loadServerImages"
                :loading="imageLoading"
              >
                从服务器导入
              </el-button>
              <el-input
                v-model="imageSearchKeyword"
                placeholder="输入 SPU 或目录路径搜索"
                style="width: 300px"
                clearable
                @keyup.enter="loadServerImages"
              />
            </div>
            <div class="image-grid" @dragover.prevent @drop="handleImageDrop">
              <div
                v-for="(img, index) in formData.mediaIdList"
                :key="img.id || index"
                class="image-item"
                draggable
                @dragstart="handleImageDragStart($event, img)"
              >
                <img :src="img.url" :alt="img.name" class="image-thumb" />
                <div class="image-overlay">
                  <el-button
                    type="danger"
                    icon="Delete"
                    circle
                    size="small"
                    @click="removeImage(index)"
                  />
                </div>
              </div>
              <div
                class="image-placeholder"
                v-if="formData.mediaIdList?.length === 0"
              >
                <el-icon><Picture /></el-icon>
                <span>点击"从服务器导入"或拖拽图片到下方变体行</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 变体详细设置 -->
        <el-divider content-position="left">变体详细设置</el-divider>
        <el-table :data="variants" border style="width: 100%">
          <el-table-column
            label="选项组合"
            prop="optionCombination"
            width="150"
          >
            <template #default="{ row }">
              <span>{{ row.optionCombination || "Default Title" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="SKU" width="150">
            <template #default="{ row }">
              <el-input v-model="row.sku" placeholder="自动生成" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="图片" width="100">
            <template #default="{ row }">
              <div
                class="variant-image-drop"
                @dragover.prevent
                @drop="handleVariantImageDrop($event, row)"
              >
                <img
                  v-if="row.imagePath"
                  :src="row.imagePath"
                  class="variant-thumb"
                />
                <span v-else class="drop-hint">拖拽图片</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="采购价" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.purchasePrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
                @change="calculateVariantCost(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="尺寸 (L/W/H)" width="180">
            <template #default="{ row }">
              <el-input
                v-model="row.dimension"
                placeholder="L/W/H"
                size="small"
                @change="calculateVolumetricWeight(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="材积重" width="80">
            <template #default="{ row }">
              <span>{{ row.volumetricWeight || 0 }} kg</span>
            </template>
          </el-table-column>
          <el-table-column label="重量" width="80">
            <template #default="{ row }">
              <el-input-number
                v-model="row.weight"
                :min="0"
                :precision="2"
                size="small"
                style="width: 60px"
                @change="calculateShippingFee(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="运费" width="80">
            <template #default="{ row }">
              <span>{{ row.shippingFee || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本价" width="80">
            <template #default="{ row }">
              <span>{{ row.costPrice || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="销售价格" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.salePrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
              />
            </template>
          </el-table-column>
          <el-table-column label="对比价" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.comparePrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
              />
            </template>
          </el-table-column>
          <el-table-column label="已发货" width="70">
            <template #default="{ row }">
              <el-switch
                v-model="row.isActualShipment"
                @change="calculateVariantCost(row)"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
    </div>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="activeStep === 1" @click="activeStep--"
          >上一步</el-button
        >
        <el-button v-if="activeStep === 0" @click="handleClose">取消</el-button>
        <el-button
          v-if="activeStep === 0"
          type="primary"
          @click="handleNext"
          :loading="loading"
        >
          下一步
        </el-button>
        <el-button
          v-if="activeStep === 1"
          @click="handleSubmit('continue')"
          :loading="loading"
        >
          保存并新增
        </el-button>
        <el-button
          v-if="activeStep === 1"
          type="primary"
          @click="handleSubmit('close')"
          :loading="loading"
        >
          保存并关闭
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import {
  ref,
  reactive,
  computed,
  watch,
  onMounted,
  getCurrentInstance,
  nextTick,
} from "vue";
import { Picture } from "@element-plus/icons-vue";
import {
  getProduct,
  addSelectionInfo,
  updateBaseInfo,
} from "@/api/erp/product";
import { treeList } from "@/api/erp/tag";
import { scanMedia, calculateShipping } from "@/api/erp/media";
const { proxy } = getCurrentInstance();

// Emit
const emit = defineEmits(["submit"]);

// 状态
const visible = ref(false);

const activeStep = ref(0);
const title = ref("新增商品");
const loading = ref(false);
const step1FormRef = ref();
const step2FormRef = ref();
const sourceUrlInputRef = ref(); // 来源 URL 输入框引用

// 表单数据
const formData = reactive({
  productId: null,
  spu: "",
  productTitle: "",
  category: "",
  productType: "",
  sourceUrl: "",
  purchaseUrl: "",
  optionJson: null,
  purchaseOptionJson: null,
  note: "",
  packageInclude: "",
  bodyHtml: "",
  size: "",
  material: "",
  tagIds: [],
  mediaIdList: [],
  mainMediaId: null,
  remark: "",
  description: "",
});

// 采购商品选项
const optionList = ref([]);

// 变体列表
const variants = ref([
  {
    variantId: null,
    sku: "",
    price: null,
    compareAtPrice: null,
    purchasePrice: null,
    purchaseUrl: "",
    purchaseProductName: "",
    optionValues: "",
    optionValueList: [],
    mediaId: null,
    position: 0,
    pkWidth: null,
    pkHeight: null,
    pkLength: null,
    materialWeight: null,
    pkWeight: null,
    freight: null,
    isActualShipment: 0,
    unitCostPrice: null,
    remark: "",
  },
]);

// 标签列表
const tagList = ref([]);

// SPU 生成相关
// 获取 MENU 类型的标签 ID
// 获取 MENU 类型的标签 ID（支持级联多选）
const getCurrentMenuTag = () => {
  if (!formData.tagIds || formData.tagIds.length === 0) {
    return null;
  }
  if (!menuTagList.value) {
    consoloe.error("menuTagList is null");
    return null;
  }
  // 获取每条路径的叶子节点（最后一个元素）
  const leafTagIds = formData.tagIds.map((path) => {
    return Array.isArray(path) ? path[path.length - 1] : path;
  });
  // 去重
  const uniqueLeafIds = [...new Set(leafTagIds)];

  // 查找并返回完整的 tag 对象
  const tag = menuTagList.value.find((t) => {
    return uniqueLeafIds.includes(t.tagId) && t.tagType === "MENU";
  });

  return tag || null;
};

// 第一步校验规则
const step1Rules = {
  spu: [{ required: true, message: "SPU 不能为空", trigger: "blur" }],

  tagIds: [{ required: true, message: "标签不能为空", trigger: "change" }],
};

// 第二步校验规则
const step2Rules = {};

// 图片加载状态
const imageLoading = ref(false);
const imageSearchKeyword = ref("");
const draggedImage = ref(null);

// 监听来源 URL 变化，同步到采购链接
watch(
  () => formData.sourceUrl,
  (newVal) => {
    // 如果采购链接为空，且来源 URL 有值，则同步
    if (newVal && !formData.purchaseUrl) {
      formData.purchaseUrl = newVal;
    }
  },
);

// 监听 SPU 变化，自动更新搜索关键词
watch(
  () => formData.spu,
  (newVal) => {
    if (newVal && !imageSearchKeyword.value) {
      imageSearchKeyword.value = newVal;
    }
  },
  { immediate: true },
);

// 监听 activeStep 和 visible，动态管理快捷键监听器
watch([() => activeStep.value, () => visible.value], ([step, isVisible]) => {
  // 只有当弹窗可见 且 处于第一步时，才挂载监听器
  if (isVisible && step === 0) {
    nextTick(() => {
      const formEl = step1FormRef.value?.$el;
      if (formEl) {
        // 移除旧的（防止重复绑定）
        formEl.removeEventListener("keydown", handleGlobalKeydown);
        // 绑定新的
        formEl.addEventListener("keydown", handleGlobalKeydown);
      }
    });
  } else {
    // 如果切换到第二步或关闭弹窗，则移除监听器
    const formEl = step1FormRef.value?.$el;
    if (formEl) {
      formEl.removeEventListener("keydown", handleGlobalKeydown);
    }
  }
});

// 处理键盘事件
function handleGlobalKeydown(event) {
  // 检查是否按下了 Ctrl (或 Mac 上的 Cmd) + '+' 或 '='
  if (
    (event.ctrlKey || event.metaKey) &&
    (event.key === "+" || event.key === "=")
  ) {
    event.preventDefault(); // 防止浏览器缩放
    addOption();
  }
}

const menuLoading = ref(false);
// 扁平化的标签列表（缓存）
const menuTagList = ref([]);
// 加载标签列表
async function fetchTags() {
  try {
    menuLoading.value = true;
    const response = await treeList("ALL");
    tagList.value = response.data;
    const menut = flattenTagList(tagList.value);
    menuTagList.value = menut;
  } catch (error) {
    console.error("加载标签列表失败", error);
    proxy.$modal.msgError("加载标签列表失败");
  } finally {
    menuLoading.value = false;
  }
}

function flattenTagList(tagList) {
  const result = [];
  function traverse(list) {
    for (const tag of list) {
      // 判断是否是叶子节点（没有 children 或 children 为空）
      const isLeaf = !tag.children || tag.children.length === 0;
      // 只收集叶子节点且 tagType='MENU' 的标签
      if (isLeaf && tag.tagType === "MENU") {
        result.push(tag);
      }
      // 如果有子节点，继续递归遍历
      if (tag.children && tag.children.length > 0) {
        traverse(tag.children);
      }
    }
  }
  traverse(tagList);
  return result;
}

// 打开编辑页面
const open = async (selectedTagIds, productId) => {
  console.log(
    "打开 ProductCreationWizard，selectedTagIds:",
    selectedTagIds,
    "productId:",
    productId,
  );
  await fetchTags();
  visible.value = true;

  if (productId == null) {
    title.value = "新增选品";
    resetForm(selectedTagIds);

    generateSpu(true);

    // 优化1：打开弹窗时，来源 URL 自动获得焦点
    nextTick(() => {
      setTimeout(() => {
        console.log("尝试设置焦点到来源 URL 输入框");
        sourceUrlInputRef.value.focus();
      }, 50); // 增加延迟以等待 Dialog 动画完成
    });
    return;
  }

  resetForm();
  title.value = "编辑商品";
  activeStep.value = 0;

  // 加载商品详情
  const response = await getProduct(productId);
  const productData = response.data;

  // 填充表单
  Object.assign(formData, {
    productId: productData.productId,
    spu: productData.spu || null,
    productTitle: productData.productTitle || null,
    category: productData.category || null,
    productType: productData.productType || null,
    sourceUrl: productData.sourceUrl || null,
    purchaseUrl: productData.purchaseUrl || null,
    optionJson: productData.optionJson || null,
    purchaseOptionJson: productData.purchaseOptionJson || null,
    note: productData.note || null,
    packageInclude: productData.packageInclude || null,
    bodyHtml: productData.bodyHtml || null,
    size: productData.size || null,
    material: productData.material || null,
    tagIds: productData.tagIds || [],
    mediaIdList: productData.mediaIdList || [],
    mainMediaId: productData.mainMediaId,
    remark: productData.remark || null,
    description: productData.description || null,
  });

  // 解析采购商品选项
  // 解析采购商品选项
  if (productData.purchaseOptionJson) {
    try {
      const parsed = JSON.parse(productData.purchaseOptionJson);
      // 确保 values 是数组对象格式
      optionList.value = parsed.map((opt) => ({
        name: opt.name,
        values: Array.isArray(opt.values)
          ? opt.values.map((v) => (typeof v === "string" ? { name: v } : v))
          : [],
      }));
    } catch (e) {
      optionList.value = [];
    }
  }

  if (productData.optionJson) {
    try {
      optionList.value = JSON.parse(productData.optionJson);
    } catch (e) {
      optionList.value = [];
    }
  }

  // 加载变体数据
  if (productData.variants && productData.variants.length > 0) {
    variants.value = productData.variants.map((v, index) => ({
      variantId: v.variantId,
      sku:
        v.sku ||
        productData.spu + "-" + (index + 1).toString().padStart(3, "0"),
      price: v.price,
      compareAtPrice: v.compareAtPrice,
      purchasePrice: v.purchasePrice,
      purchaseUrl: v.purchaseUrl || formData.purchaseUrl,
      purchaseProductName: v.purchaseProductName,
      optionValues: v.optionValues || "",
      optionValueList: v.optionValueList || [],
      mediaId: v.mediaId,
      position: v.position || 0,
      pkWidth: v.pkWidth,
      pkHeight: v.pkHeight,
      pkLength: v.pkLength,
      materialWeight: v.materialWeight,
      pkWeight: v.pkWeight,
      freight: v.freight,
      isActualShipment: v.isActualShipment,
      unitCostPrice: v.unitCostPrice,
      remark: v.remark,
    }));
  } else {
    variants.value = [
      {
        variantId: null,
        sku:
          v.sku ||
          productData.spu + "-" + (index + 1).toString().padStart(3, "0"),
        price: null,
        compareAtPrice: null,
        purchasePrice: null,
        purchaseUrl: formData.purchaseUrl,
        purchaseProductName: "",
        optionValues: "",
        optionValueList: [],
        mediaId: null,
        position: 0,
        pkWidth: null,
        pkHeight: null,
        pkLength: null,
        materialWeight: null,
        pkWeight: null,
        freight: null,
        isActualShipment: 0,
        unitCostPrice: null,
        remark: "",
      },
    ];
  }

  // 自动带入选中标签
  if (selectedTagIds?.length > 0 && !formData.tagIds?.length) {
    formData.tagIds = selectedTagIds;
  }
};

// 重置表单
function resetForm(selectedTagIds) {
  title.value = "新增商品";
  activeStep.value = 0;

  Object.assign(formData, {
    productId: null,
    spu: "",
    productTitle: "",
    category: "",
    productType: "",
    sourceUrl: "",
    purchaseUrl: "",
    optionJson: null,
    purchaseOptionJson: null,
    note: "",
    packageInclude: "",
    bodyHtml: "",
    size: "",
    material: "",
    tagIds: selectedTagIds || [],
    mediaIdList: [],
    mainMediaId: null,
    remark: "",
    description: "",
  });

  optionList.value = [];
  variants.value = [
    {
      variantId: null,
      sku: "",
      price: null,
      compareAtPrice: null,
      purchasePrice: null,
      purchaseUrl: "",
      purchaseProductName: "",
      optionValues: "",
      optionValueList: [],
      mediaId: null,
      position: 0,
      pkWidth: null,
      pkHeight: null,
      pkLength: null,
      materialWeight: null,
      pkWeight: null,
      freight: null,
      isActualShipment: 0,
      unitCostPrice: null,
      remark: "",
    },
  ];

  imageSearchKeyword.value = ""; // 重置搜索关键词

  step1FormRef.value?.resetFields();
  step2FormRef.value?.resetFields();
  loading.value = false;
}

// 生成 SPU
const spuGenerating = ref(false); // ✅ 新增：SPU 生成中状态

const generateSpu = async (auto) => {
  // 防止重复点击
  if (spuGenerating.value) return;

  spuGenerating.value = true;

  try {
    const menuTag = getCurrentMenuTag();

    if (!menuTag) {
      if (!auto) proxy.$modal.msgError("未选择标签");
      return;
    }

    if (menuTag.spuPrefix == null) {
      if (!auto)
        proxy.$modal.msgError("当前选中的标签没有配置 SPU 前缀，无法生成 SPU");
      return;
    }

    // ✅ 模拟异步操作（如果需要调用后端接口更新序列号，可在此处 await）
    await new Promise((resolve) => setTimeout(resolve, 300));

    const seq = menuTag.currentMaxSeq || 0;
    const nextSeq = seq + 1;
    const paddedSeq = String(nextSeq).padStart(3, "0");
    formData.spu = `${menuTag.spuPrefix}${paddedSeq}`;

    if (!auto) proxy.$modal.msgSuccess("SPU 生成成功");
  } catch (error) {
    console.error("生成 SPU 失败", error);
    if (!auto) proxy.$modal.msgError("生成 SPU 失败");
  } finally {
    // ✅ 生成完成，恢复状态
    spuGenerating.value = false;
  }
};
// 添加选项值
function addOptionValue(optIndex) {
  if (!optionList.value[optIndex].values) {
    optionList.value[optIndex].values = [];
  }
  optionList.value[optIndex].values.push({
    purchaseValue: "",
    productValue: "",
  });
}
function toggleCollapse(optIndex) {
  optionList.value[optIndex].collapsed = !optionList.value[optIndex].collapsed;
  if (optionList.value[optIndex].collapsed) {
    generateVariants();
  }
}

// 删除选项值
function removeOptionValue(optIndex, valIndex) {
  optionList.value[optIndex].values.splice(valIndex, 1);
}

// 处理 Enter 键导航
function handleEnterKey(event, optIndex, valIndex, type) {
  const values = optionList.value[optIndex].values;
  console.log(values);
  if (type === "purchase") {
    // 从采购选项值跳到下一个采购选项值
    if (valIndex < values.length - 1) {
      focusNextInput(optIndex, valIndex + 2, "purchase");
    } else {
      // 如果是最后一个，添加新行并聚焦
      addOptionValue(optIndex);
      // setTimeout(() => {
      focusNextInput(optIndex, values.length, "purchase");
      // }, 100);
    }
  } else if (type === "product") {
    // 从英文选项值跳到下一个英文选项值
    if (valIndex < values.length - 1) {
      focusNextInput(optIndex, valIndex + 2, "product");
    } else {
      // 如果是最后一个，添加新行并聚焦到采购选项值
      addOptionValue(optIndex);
      // setTimeout(() => {
      focusNextInput(optIndex, values.length, "purchase");
      // }, 100);
    }
  }
}

// 聚焦到指定输入框
function focusNextInput(optIndex, valIndex, type) {
  setTimeout(() => {
    const inputs = document.querySelectorAll(
      `.option-row:nth-child(${optIndex + 1}) .option-value-row`,
    );
    if (inputs[valIndex]) {
      const inputClass =
        type === "purchase" ? ".input-left input" : ".input-right input";
      const targetInput = inputs[valIndex].querySelector(inputClass);
      if (targetInput) {
        targetInput.focus();
      }
    }
  }, 50);
}

// 用于跟踪每个选项是否正在自动添加新行
const autoAddingFlags = ref({});
// 处理选项值输入，自动添加新行
// 处理选项值输入，自动添加新行
function handleValueInput(optIndex, valIndex) {
  const values = optionList.value[optIndex].values;
  const currentValue = values[valIndex];

  // 生成唯一标识
  const key = `${optIndex}-${valIndex}`;

  // 检查是否是最后一个选项值且有输入内容
  if (
    valIndex === values.length - 1 &&
    (currentValue.purchaseValue || currentValue.productValue)
  ) {
    // 如果已经在添加中，直接返回
    if (autoAddingFlags.value[key]) {
      return;
    }

    // 设置标志位
    autoAddingFlags.value[key] = true;

    // 延迟执行，避免在输入过程中频繁触发
    setTimeout(() => {
      // 再次检查，确保仍然是最后一个且没有被其他操作改变
      const currentValues = optionList.value[optIndex].values;
      if (valIndex === currentValues.length - 1) {
        addOptionValue(optIndex);
        delete autoAddingFlags.value[key];
      } else {
        // 如果不是最后一个了，清除标志位
        delete autoAddingFlags.value[key];
      }
    }, 300);
  }
}

// 添加选项
const addOption = () => {
  optionList.value.push({
    purchaseName: "",
    productName: "",
    values: [
      {
        purchaseValue: "",
        productValue: "",
      },
    ],
    collapsed: false,
  });

  // 优化3：点击添加选项时，新添加的选项名称自动获得光标
  nextTick(() => {
    // 使用 setTimeout 确保 DOM 完全渲染
    setTimeout(() => {
      const optionRows = document.querySelectorAll(".option-row");
      if (optionRows.length > 0) {
        const allInputs = optionRows[optionRows.length - 1].querySelectorAll(
          ".option-value-row .input-left input",
        );
        if (allInputs.length > 0) {
          const lastInput = allInputs[0];
          lastInput.focus();
        }
      }
    }, 50);
  });
};

// 移除选项
function removeOption(index) {
  optionList.value.splice(index, 1);
  generateVariants();
}

// 获取当前有效的选项列表（用于生成动态表头）
function getActiveOptions() {
  return optionList.value.filter(
    (opt) =>
      (opt.purchaseName || opt.productName) &&
      opt.values?.some((v) => v.purchaseValue || v.productValue),
  );
}

// 生成变体（笛卡尔积）
function generateVariants() {
  // 过滤有实际值的选项
  const activeOptions = getActiveOptions();

  if (activeOptions.length === 0) {
    variants.value = [
      {
        variantId: null,
        sku: formData.spu + "-",
        price: null,
        compareAtPrice: null,
        purchasePrice: null,
        purchaseUrl: formData.purchaseUrl,
        purchaseProductName: "",
        optionValues: "",
        optionValueList: [],
        mediaId: null,
        position: 0,
        pkWidth: null,
        pkHeight: null,
        pkLength: null,
        materialWeight: null,
        pkWeight: null,
        freight: null,
        isActualShipment: 0,
        unitCostPrice: null,
        remark: "",
      },
    ];
    return;
  }
  // 计算笛卡尔积（使用 values 数组，并保留 purchaseValue 和 productValue）
  const valueArrays = activeOptions.map((opt) =>
    opt.values
      .filter((v) => v.purchaseValue || v.productValue)
      .map((v) => ({
        purchaseValue: v.purchaseValue,
        productValue: v.productValue,
      })),
  );
  console.log("valueArrays", valueArrays);
  const combinations = cartesianProduct(valueArrays);
  console.log("combinations", combinations);

  // 2. 映射生成变体行
  variants.value = combinations.map((combo, index) => {
    const existingVariant = variants.value[index];
    console.log("existingVariant", existingVariant);
    const optionValueList = combo.map((val, idx) => {
      const opt = activeOptions[idx];
      return {
        optionName: opt.productName,
        optionValue: val.productValue,
        purchaseOptionName: opt.purchaseName,
        purchaseName: val.purchaseValue,
      };
    });
    return {
      variantId: existingVariant?.variantId,
      sku:
        existingVariant?.sku ||
        formData.spu + "-" + (index + 1).toString().padStart(3, "0"),
      purchaseUrl: existingVariant?.purchaseUrl || formData.purchaseUrl,
      purchasePrice: existingVariant?.purchasePrice || 0,
      optionValues: JSON.stringify(optionValueList),
      optionValueList: optionValueList,
      price: existingVariant?.price || null,
      compareAtPrice: existingVariant?.compareAtPrice || null,
      purchaseProductName: existingVariant?.purchaseProductName || "",
      mediaId: existingVariant?.mediaId || null,
      position: existingVariant?.position || index,
      pkWidth: existingVariant?.pkWidth || null,
      pkHeight: existingVariant?.pkHeight || null,
      pkLength: existingVariant?.pkLength || null,
      materialWeight: existingVariant?.materialWeight || null,
      pkWeight: existingVariant?.pkWeight || null,
      freight: existingVariant?.freight || null,
      isActualShipment: existingVariant?.isActualShipment || 0,
      unitCostPrice: existingVariant?.unitCostPrice || null,
      remark: existingVariant?.remark || "",
    };
  });
  console.log("variants.value", variants.value);
}

// 笛卡尔积工具函数
// 笛卡尔积工具函数
function cartesianProduct(arrays) {
  // 1. 过滤掉非数组或空数组的项，确保输入纯净
  const validArrays = arrays.filter(
    (arr) => Array.isArray(arr) && arr.length > 0,
  );

  // 2. 如果没有有效数组，返回包含一个空数组的数组（表示一种“空”组合）
  if (validArrays.length === 0) return [[]];

  // 3. 执行笛卡尔积计算
  return validArrays.reduce(
    (acc, curr) => {
      return acc.flatMap((item) => curr.map((value) => [...item, value]));
    },
    [[]],
  ); // 初始值设为 [[]]，确保 reduce 能正确启动
}

// 移除变体
function removeVariant(index) {
  if (variants.value.length > 1) {
    variants.value.splice(index, 1);
  }
}

// 变体行拖拽排序
function handleRowDrop({ row, $index }, targetIndex) {
  const sourceIndex = variants.value.findIndex(
    (v) => v.position === row.position,
  );
  if (sourceIndex !== -1 && sourceIndex !== targetIndex) {
    const item = variants.value.splice(sourceIndex, 1)[0];
    variants.value.splice(targetIndex, 0, item);
  }
}

// 计算变体成本
function calculateVariantCost(row) {
  row.costPrice = (row.purchasePrice || 0) + (row.shippingFee || 0);
  if (!row.isActualShipment) {
    row.costPrice = row.purchasePrice || 0;
  }
  // 自动计算销售价格
  if (row.costPrice > 0) {
    // 假设美元汇率 7.2，成本价基础上加30%作为销售价 向上取整
    row.salePrice = Math.ceil((row.costPrice * 1.3) / 7.2); // 假设美元汇率 7.2
  }
}

// 计算材积重
function calculateVolumetricWeight(row) {
  if (row.dimension) {
    const dims = row.dimension
      .split(/[\/\*x]/)
      .map((d) => parseFloat(d.trim()));
    if (dims.length >= 3 && dims.every((d) => !isNaN(d))) {
      row.volumetricWeight = (dims[0] * dims[1] * dims[2]) / 8000;
    }
  }
}

// 计算运费
async function calculateShippingFee(row) {
  if (row.weight > 0) {
    try {
      const response = await calculateShipping({ weight: row.weight });
      row.shippingFee = response.data?.fee || 0;
      calculateVariantCost(row);
    } catch (error) {
      console.error("计算运费失败", error);
    }
  }
}

// 加载服务器图片（直接添加到列表）
async function loadServerImages() {
  imageLoading.value = true;
  try {
    const response = await scanMedia({
      dirPath: imageSearchKeyword.value || formData.spu,
      productId: formData.productId,
    });

    const serverImages = response.data || [];

    if (serverImages.length === 0) {
      proxy.$modal.msgWarning("未找到图片");
      return;
    }

    // 直接添加到图片列表，避免重复
    let addedCount = 0;
    serverImages.forEach((img) => {
      const exists = formData.mediaIdList.some((item) => item.id === img.id);
      if (!exists) {
        formData.mediaIdList.push({
          id: img.id,
          url: img.url,
          name: img.name,
          type: img.type || "image",
        });
        addedCount++;
      }
    });

    proxy.$modal.msgSuccess(`已导入 ${addedCount} 张图片/视频`);
  } catch (error) {
    console.error("加载服务器图片失败", error);
    proxy.$modal.msgError("加载服务器图片失败");
  } finally {
    imageLoading.value = false;
  }
}

// 移除图片
function removeImage(index) {
  formData.mediaImages.splice(index, 1);
}

// 图片拖拽开始
function handleImageDragStart(event, img) {
  draggedImage.value = img;
  event.dataTransfer.setData("image-url", img.url);
  event.dataTransfer.setData("image-id", img.id);
}

// 图片拖拽到变体
function handleVariantImageDrop(event, row) {
  if (draggedImage.value) {
    row.imagePath = draggedImage.value.url;
    row.mediaId = draggedImage.value.id;
    draggedImage.value = null;
  }
}

// 下一步
async function handleNext() {
  try {
    await step1FormRef.value?.validate();
    // 如果没有父级标签ID，就填充
    const menuTag = getCurrentMenuTag();
    if (menuTag) {
      menuTag.ancestors.split(",").forEach((ancestorId) => {
        if (
          ancestorId != "0" &&
          !formData.tagIds.some((path) => path.includes(ancestorId))
        ) {
          formData.tagIds.push(ancestorId);
        }
      });
    } else {
      proxy.$modal.msgError("请至少选择一个菜单类型的标签");
      return;
    }

    // 保存采购商品选项
    formData.purchaseOptionJson = JSON.stringify(optionList.value);

    // 同步变体的采购链接
    variants.value.forEach((v) => {
      if (!v.purchaseUrl) {
        v.purchaseUrl = formData.purchaseUrl;
      }
    });
    const submitData = {
      ...formData,
      optionJson: JSON.stringify(optionList.value),
      purchaseOptionJson: JSON.stringify(optionList.value),
      variants: variants.value,
    };
    loading.value = true;
    await addSelectionInfo(submitData);
    emit("submit", { action, data: submitData });

    activeStep.value = 1;
  } catch (error) {
    console.error("第一步验证失败", error);
  } finally {
    loading.value = false;
  }
}

// 提交表单
async function handleSubmit(action) {
  try {
    await step2FormRef.value?.validate();
    loading.value = true;

    // 准备提交数据
    const submitData = {
      ...formData,
      optionJson: JSON.stringify(optionList.value),
      purchaseOptionJson: JSON.stringify(optionList.value),
      variants: variants.value,
      mediaIdList: formData.mediaIdList, // 使用 mediaIdList
    };

    if (formData.productId) {
      await updateBaseInfo(submitData);
      emit("submit", { action, data: submitData });
    } else {
      await addSelectionInfo(submitData);
      emit("submit", { action, data: submitData });
    }

    if (action === "close") {
      visible.value = false;
    } else {
      resetForm(formData.tagIds);
      activeStep.value = 0;
    }
  } catch (error) {
    console.error("提交失败", error);
  } finally {
    loading.value = false;
  }
}

// 关闭对话框
function handleClose() {
  visible.value = false;
  emit("submit", { action: "cancel" });
}

// 暴露方法给父组件
defineExpose({
  open,
});
</script>

<style scoped>
.step-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 10px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mb-2 {
  margin-bottom: 8px;
}

.mt-3 {
  margin-top: 12px;
}

.options-container {
  width: 100%;
}

.option-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  background: #fafafa;
  transition: all 0.3s;
}

.option-row-collapsed {
  background: #f5f7fa;
  padding: 10px 15px;
}

.option-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 16px;
}

.input-label {
  width: 80px;
  flex-shrink: 0;
  text-align: right;
  font-size: 14px;
  color: #606266;
  line-height: 32px;
}

.delete-button-wrapper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8px;
}

.option-values-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  /* gap: 8px; */
  padding: 12px;
  background: #fff;
  /* border-radius: 4px; */
  /* border-bottom: 1px solid #e4e7ed; */
}

.option-value-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.option-value-row .input-label {
  width: 80px;
  flex-shrink: 0;
  text-align: right;
  font-size: 14px;
  color: #606266;
  line-height: 32px;
}

.option-value-row .input-combined {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}

.option-value-row .input-combined .input-left,
.option-value-row .input-combined .input-right {
  flex: 1;
}

.option-value-row .input-combined .input-left :deep(.el-input__wrapper) {
  border-radius: 4px 0 0 4px;
  border-right: none;
}

.option-value-row .input-combined .input-right :deep(.el-input__wrapper) {
  border-radius: 0 4px 4px 0;
}

.option-value-row .delete-button-wrapper {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8px;
}

.option-value-row .input-combined .ml-2 {
  margin-left: 8px !important;
  flex-shrink: 0;
}

.option-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  /* border-top: 1px solid #e4e7ed; */
}

.option-collapsed-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f5f7fa;
  border-radius: 4px;
  gap: 12px;
}

.collapsed-content {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  flex-wrap: wrap;
}

.option-name-display {
  font-size: 14px;
  color: #303133;
  white-space: nowrap;
}

.option-name-display strong {
  font-weight: 600;
  color: #303133;
}

.option-name-display .en-name {
  color: #909399;
  font-size: 13px;
  margin-left: 4px;
}

.option-values-display {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.value-tag {
  background-color: #f4f4f5 !important;
  border-color: #e9e9eb !important;
  color: #606266 !important;
  font-size: 12px;
  padding: 2px 8px;
}

.value-tag .en-value {
  color: #909399;
  font-size: 11px;
  margin-left: 2px;
}

.expand-btn {
  flex-shrink: 0;
}

.mt-2 {
  margin-top: 8px;
}

.ml-2 {
  margin-left: 8px !important;
}

.image-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
}

.image-manager {
  width: 100%;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 100px;
  padding: 10px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
  cursor: move;
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-placeholder {
  width: 100%;
  height: 100px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.variant-image-drop {
  width: 80px;
  height: 60px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.variant-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.drop-hint {
  font-size: 12px;
  color: #909399;
}

.server-image-item {
  border: 2px solid transparent;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.3s;
}

.server-image-item:hover,
.server-image-item.selected {
  border-color: #409eff;
}

.server-image-item img {
  width: 100%;
  height: 80px;
  object-fit: cover;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
