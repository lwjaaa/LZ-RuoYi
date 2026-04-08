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
                @blur="handleSourceUrlBlur"
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
              tabindex="0"
              @keyup.enter="toggleCollapse(optIndex)"
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
                      @keydown.tab.prevent="
                        handleTabKey($event, optIndex, null, 'purchase')
                      "
                    />
                    <el-input
                      v-model="option.productName"
                      placeholder="英文选项名称"
                      class="input-right"
                      size="small"
                      @keydown.tab.prevent="
                        handleTabKey($event, optIndex, null, 'product')
                      "
                    />
                    <div class="delete-button-wrapper"></div>
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
                      @keydown.tab.prevent="
                        handleTabKey($event, optIndex, valIndex, 'purchase')
                      "
                      @input="handleValueInput(optIndex, valIndex)"
                    />
                    <el-input
                      v-model="value.productValue"
                      placeholder="英文选项值"
                      class="input-right"
                      size="small"
                      @keydown.tab.prevent="
                        handleTabKey($event, optIndex, valIndex, 'product')
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
                  <el-tooltip content="快捷键: Enter" placement="top">
                    <el-button
                      type="primary"
                      size="small"
                      @click="toggleCollapse(optIndex)"
                      tabindex="-1"
                    >
                      {{ option.collapsed ? "展开" : "完成" }}
                    </el-button>
                  </el-tooltip>
                </div>
              </div>

              <!-- 底部操作按钮 -->

              <!-- 收起状态提示 -->
              <div class="option-collapsed-hint" v-show="option.collapsed">
                <div class="collapsed-content">
                  <span class="option-name-display">
                    <strong>{{ option.purchaseName || "采购选项" }}</strong>
                    <span v-if="option.productName" class="en-name"
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
                        <span v-if="value.productValue" class="en-value"
                          >[{{ value.productValue || "-" }}]</span
                        >
                      </el-tag>
                    </template>
                  </div>
                </div>
                <el-tooltip content="快捷键: Enter" placement="top">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="toggleCollapse(optIndex)"
                    class="expand-btn"
                    tabindex="-1"
                  >
                    展开
                  </el-button>
                </el-tooltip>
              </div>
            </div>
            <el-tooltip content="快捷键: Ctrl+=" placement="top">
              <el-button
                type="primary"
                icon="Plus"
                @click="addOption"
                size="small"
                tabindex="-1"
              >
                添加选项
              </el-button>
            </el-tooltip>
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
                  <!-- <span>{{
                    `${row.optionValueList[idx]?.purchaseName}[${row.optionValueList[idx]?.optionValue}]` ||
                    "-"
                  }}</span> -->

                  <span>
                    {{ row.optionValueList[idx]?.purchaseName || "-" }}
                    <span v-if="row.optionValueList[idx]?.optionValue">
                      [{{ row.optionValueList[idx]?.optionValue }}]
                    </span>
                  </span>
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
                  tabindex="-1"
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
                  tabindex="-1"
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
        class="step2-form"
        :label-position="'left'"
      >
        <!-- 主商品信息 -->
        <el-divider content-position="left">主商品信息</el-divider>

        <!-- SPU 与 商品标题 同行 -->
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="SPU" prop="spu">
              <el-input
                v-model="formData.spu"
                disabled
                placeholder="系统自动生成"
              />
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="商品标题" prop="productTitle">
              <el-input
                v-model="formData.productTitle"
                placeholder="请输入商品标题"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 商品类别 与 商品类型 同行 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品类别" prop="category">
              <el-input
                v-model="formData.category"
                placeholder="请输入商品类别"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品类型" prop="productType">
              <el-input
                v-model="formData.productType"
                placeholder="请输入商品类型"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- SIZE 文本域 -->
        <el-form-item label="SIZE" prop="size">
          <div class="expandable-textarea">
            <el-input
              v-if="textareaExpand.size || formData.size"
              v-model="formData.size"
              type="textarea"
              :rows="2"
              placeholder="请输入SIZE"
              @blur="handleTextareaBlur('size')"
              @focus="textareaExpand.size = true"
            />
            <div
              v-else
              class="textarea-collapse"
              @click="textareaExpand.size = true"
            >
              <span class="placeholder-text">点击展开输入 SIZE</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
          </div>
        </el-form-item>

        <!-- MATERIAL 文本域 -->
        <el-form-item label="MATERIAL" prop="material">
          <div class="expandable-textarea">
            <el-input
              v-if="textareaExpand.material || formData.material"
              v-model="formData.material"
              type="textarea"
              :rows="2"
              placeholder="请输入MATERIAL"
              @blur="handleTextareaBlur('material')"
              @focus="textareaExpand.material = true"
            />
            <div
              v-else
              class="textarea-collapse"
              @click="textareaExpand.material = true"
            >
              <span class="placeholder-text">点击展开输入 MATERIAL</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
          </div>
        </el-form-item>

        <!-- NOTE 文本域（默认展开） -->
        <el-form-item label="NOTE" prop="note">
          <el-input
            v-model="formData.note"
            type="textarea"
            :rows="2"
            placeholder="请输入NOTE"
          />
        </el-form-item>

        <!-- PACKAGE_INCLUDE 文本域 -->
        <el-form-item label="PACKAGE_INCLUDE" prop="packageInclude">
          <div class="expandable-textarea">
            <el-input
              v-if="textareaExpand.packageInclude || formData.packageInclude"
              v-model="formData.packageInclude"
              type="textarea"
              :rows="2"
              placeholder="请输入PACKAGE_INCLUDE"
              @blur="handleTextareaBlur('packageInclude')"
              @focus="textareaExpand.packageInclude = true"
            />
            <div
              v-else
              class="textarea-collapse"
              @click="textareaExpand.packageInclude = true"
            >
              <span class="placeholder-text">点击展开输入 PACKAGE_INCLUDE</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
          </div>
        </el-form-item>

        <!-- 富文本编辑器 -->
        <el-form-item label="商品详情描述" prop="bodyHtml">
          <div class="rich-text-editor-container">
            <div
              v-if="!richTextEditor.expanded && !formData.bodyHtml"
              class="rich-text-editor-collapse"
              @click="richTextEditor.expanded = true"
            >
              <span class="placeholder-text">点击编辑商品详情描述</span>
              <el-icon><Edit /></el-icon>
            </div>
            <div v-else class="rich-text-editor-expanded">
              <div class="rich-text-editor-toolbar">
                <el-radio-group v-model="richTextEditor.mode" size="small">
                  <el-radio-button value="edit">HTML 输入</el-radio-button>
                  <el-radio-button value="preview">预览</el-radio-button>
                </el-radio-group>
                <el-button
                  type="danger"
                  size="small"
                  link
                  @click="richTextEditor.expanded = false"
                  v-if="!formData.bodyHtml"
                >
                  收起
                </el-button>
              </div>
              <el-input
                v-if="richTextEditor.mode === 'edit'"
                v-model="formData.bodyHtml"
                type="textarea"
                :rows="6"
                placeholder="请输入商品详情描述 (HTML)"
              />
              <div
                v-else
                class="rich-text-preview"
                v-html="
                  formData.bodyHtml || '<span class=no-content>暂无内容</span>'
                "
              ></div>
            </div>
          </div>
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
                size="default"
              >
                从服务器导入
              </el-button>
              <el-input
                v-model="imageSearchKeyword"
                placeholder="输入 SPU 或目录路径搜索"
                style="width: 300px; margin-left: 10px"
                clearable
                @keyup.enter="loadServerImages"
              />
            </div>
            <!-- 添加拖拽排序事件 -->
            <div class="image-grid" @dragover.prevent @drop="handleImageDrop">
              <div
                v-for="(img, index) in formData.mediaIdList"
                :key="img.id || index"
                class="image-item"
                draggable
                @dragstart="handleImageDragStart($event, index)"
                @dragover.prevent="handleImageDragOver($event)"
                :title="img.name"
              >
                <img :src="img.url" :alt="img.name" class="image-thumb" />
                <div class="image-overlay">
                  <el-button
                    type="danger"
                    icon="Delete"
                    circle
                    size="small"
                    @click="removeImage(index)"
                    title="删除图片"
                  />
                </div>
              </div>
              <div
                class="image-placeholder"
                v-if="formData.mediaIdList?.length === 0"
              >
                <el-icon class="placeholder-icon"><Picture /></el-icon>
                <span class="placeholder-text"
                  >点击"从服务器导入"或拖拽图片到下方变体行</span
                >
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 变体详细设置 -->
        <el-divider content-position="left">变体详细设置</el-divider>
        <el-table
          :data="variants"
          border
          style="width: 100%"
          stripe
          :loading="loading"
        >
          <!-- 修正选项组合列，支持编辑且与第一步逻辑一致 -->
          <el-table-column
            label="选项组合"
            prop="optionCombination"
            min-width="200"
          >
            <template #default="{ row }">
              <div
                v-if="row.optionValueList && row.optionValueList.length > 0"
                class="option-value-list"
              >
                <div
                  v-for="(opt, idx) in row.optionValueList"
                  :key="idx"
                  class="option-value-item"
                >
                  <!-- 使用 purchaseName 和 purchaseValue 以匹配第一步逻辑，并允许编辑 -->
                  <el-input
                    v-model="opt.purchaseName"
                    size="small"
                    placeholder="选项名"
                    style="width: 45%; margin-right: 5px"
                  />
                  <el-input
                    v-model="opt.purchaseValue"
                    size="small"
                    placeholder="值"
                    style="width: 45%"
                  />
                </div>
              </div>
              <span v-else>{{ row.optionCombination || "Default Title" }}</span>
            </template>
          </el-table-column>
          <el-table-column label="SKU" width="150">
            <template #default="{ row }">
              <el-input
                v-model="row.sku"
                placeholder="自动生成"
                size="small"
                :disabled="!row.sku"
              />
            </template>
          </el-table-column>
          <el-table-column label="图片" width="100">
            <template #default="{ row }">
              <div
                class="variant-image-drop"
                @dragover.prevent
                @drop="handleVariantImageDrop($event, row)"
                title="拖拽图片到此处"
              >
                <img
                  v-if="row.imagePath"
                  :src="row.imagePath"
                  class="variant-thumb"
                  :alt="'Variant Image'"
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
                placeholder="0.00"
              />
            </template>
          </el-table-column>
          <el-table-column label="尺寸 (L/W/H)" width="180">
            <template #default="{ row }">
              <el-input
                v-model="row.dimension"
                placeholder="如: 10/10/10"
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
                placeholder="0.00"
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
                placeholder="0.00"
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
                placeholder="0.00"
              />
            </template>
          </el-table-column>
          <el-table-column label="是否实际发货" width="100">
            <template #default="{ row }">
              <el-switch
                v-model="row.isActualShipment"
                :active-value="'1'"
                :inactive-value="'0'"
                @change="calculateVariantCost(row)"
                active-text="是"
                inactive-text="否"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
    </div>

    <!-- 底部操作按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <div class="footer-right-buttons">
          <el-tooltip
            v-if="activeStep === 1"
            content="快捷键: Ctrl+←"
            placement="top"
          >
            <el-button @click="activeStep--" type="primary">上一步</el-button>
          </el-tooltip>
          <el-tooltip
            v-if="activeStep === 0"
            content="快捷键: Ctrl+Enter"
            placement="top"
          >
            <el-button
              type="primary"
              @click="handleSubmit('continue')"
              :loading="loading"
            >
              继续选品
            </el-button>
          </el-tooltip>
          <el-tooltip
            v-if="activeStep === 0"
            content="快捷键: Ctrl+→"
            placement="top"
          >
            <el-button
              type="primary"
              @click="handleSubmit('next')"
              :loading="loading"
            >
              下一步
            </el-button>
          </el-tooltip>
          <el-tooltip content="快捷键: Ctrl+S" placement="top">
            <el-button
              type="primary"
              @click="handleSubmit('close')"
              :loading="loading"
            >
              保存
            </el-button>
          </el-tooltip>
        </div>
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
  onUnmounted,
  getCurrentInstance,
  nextTick,
} from "vue";
import { Picture, ArrowDown, Edit } from "@element-plus/icons-vue";
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

/** @type {Product} */
const formData = reactive({
  productId: null,
  spu: "",
  productTitle: "",
  category: "",
  productType: "",
  sourceUrl: "",
  purchaseUrl: "",
  optionJson: null,
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
/** @type {ProductOption[]} */
const optionList = ref([]);

// 变体列表
/** @type {ProductVariant[]} */
const variants = ref([
  {
    variantId: null,
    sku: "",
    price: null,
    compareAtPrice: null,
    purchasePrice: null,
    purchaseUrl: "",
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
    isActualShipment: "0",
    unitCostPrice: null,
    remark: "",
  },
]);

// 标签列表
/** @type {TagDictMenu[]} */
const tagList = ref([]);

// 初始数据快照，用于检测数据变更
const initialDataSnapshot = ref(null);

// SPU 生成相关
// 获取 MENU 类型的标签 ID
// 获取 MENU 类型的标签 ID（支持级联多选）
const getMenuTag = (tagIds) => {
  if (!tagIds || tagIds.length === 0) {
    return null;
  }
  if (!menuTagList.value) {
    console.error("menuTagList is null");
    return null;
  }
  // 获取每条路径的叶子节点（最后一个元素）
  const leafTagIds = tagIds.map((path) => {
    return Array.isArray(path) ? path[path.length - 1] : path;
  });
  // 去重
  const uniqueLeafIds = [...new Set(leafTagIds)];

  // 查找并返回完整的 tag 对象
  const res = menuTagList.value.filter((t) => {
    return uniqueLeafIds.includes(t.tagId) && t.tagType === "MENU";
  });

  return res || null;
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

// 文本域展开/收起状态
const textareaExpand = reactive({
  size: false,
  material: false,
  note: true, // NOTE 默认展开
  packageInclude: false,
});

// 富文本编辑器状态
const richTextEditor = reactive({
  expanded: false,
  mode: "edit", // 'edit' | 'preview'
});

// 监听采购链接 变化，同步到变体的采购链接
// 如果采购链接 有值，变体采购链接等于旧值, 则同步
watch(
  () => formData.purchaseUrl,
  (newVal, oldVal) => {
    if (!variants.value || variants.value.length === 0) {
      return;
    }

    variants.value.forEach((variant) => {
      // 如果变体的采购链接为空，或者等于旧的采购链接值，则同步新值
      if (!variant.purchaseUrl || variant.purchaseUrl === oldVal) {
        variant.purchaseUrl = newVal;
      }
    });
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

// 监听弹窗可见性，动态管理快捷键监听器
watch(
  () => visible.value,
  (isVisible) => {
    if (isVisible) {
      if (activeStep.value === 0) {
        // 弹窗打开且在第一步时，挂载到 window
        window.addEventListener("keydown", handleSetp1Keydown);
        window.removeEventListener("keydown", handleSetp2Keydown);
      } else if (activeStep.value === 1) {
        // 弹窗打开且在第二步时，挂载到 window
        window.addEventListener("keydown", handleSetp2Keydown);
        window.removeEventListener("keydown", handleSetp1Keydown);
      }
    } else {
      // 弹窗关闭或切换到其他步骤时，移除监听器
      window.removeEventListener("keydown", handleSetp1Keydown);
      window.removeEventListener("keydown", handleSetp2Keydown);
    }
  },
);

// 监听步骤变化
watch(
  () => activeStep.value,
  (step) => {
    if (visible.value) {
      if (step === 0) {
        // 切换到第一步，添加监听器
        window.addEventListener("keydown", handleSetp1Keydown);
      } else if (step === 1) {
        // 切换到其他步骤，移除监听器
        window.removeEventListener("keydown", handleSetp1Keydown);
        window.addEventListener("keydown", handleSetp2Keydown);
      }
    } else {
      // 弹窗关闭或切换到其他步骤时，移除监听器
      window.removeEventListener("keydown", handleSetp1Keydown);
      window.removeEventListener("keydown", handleSetp2Keydown);
    }
  },
);

// 组件卸载时清理监听器
onUnmounted(() => {
  window.removeEventListener("keydown", handleSetp1Keydown);
  window.removeEventListener("keydown", handleSetp2Keydown);
});

// 处理来源 URL 失去焦点事件
function handleSourceUrlBlur() {
  if (formData.sourceUrl && !formData.purchaseUrl) {
    formData.purchaseUrl = formData.sourceUrl;
  }
}
// 处理第一步的键盘事件
function handleSetp1Keydown(event) {
  // 检查是否按下了 Ctrl (或 Mac 上的 Cmd) + '+' 或 '='
  if (
    (event.ctrlKey || event.metaKey) &&
    (event.key === "+" || event.key === "=")
  ) {
    event.preventDefault(); // 防止浏览器缩放
    addOption();
  }

  // Ctrl + S: 保存并关闭
  if ((event.ctrlKey || event.metaKey) && event.key === "s") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleSubmit("close");
    }
  }

  // Ctrl + Enter: 继续选品
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleSubmit("continue");
    }
  }

  // Ctrl + 右键: 下一步
  if ((event.ctrlKey || event.metaKey) && event.key === "ArrowRight") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleSubmit("next");
    }
  }
}

// 处理第二步的键盘事件
function handleSetp2Keydown(event) {
  // Ctrl + 左键: 上一步
  if ((event.ctrlKey || event.metaKey) && event.key === "ArrowLeft") {
    event.preventDefault();
    if (visible.value && activeStep.value === 1) {
      activeStep.value--;
    }
  }

  // Ctrl + S: 保存并关闭
  if ((event.ctrlKey || event.metaKey) && event.key === "s") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleSubmit("close");
    }
  }
}

const menuLoading = ref(false);
// 扁平化的标签列表（缓存）
/** @type {TagDictMenu[]} */
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
  await fetchTags();
  visible.value = true;

  if (productId == null) {
    title.value = "新增选品";
    if (selectedTagIds) {
      const selectedMenuTags = getMenuTag(selectedTagIds);
      if (!selectedMenuTags) {
        if (!auto) proxy.$modal.msgError("未选择标签");
        return;
      }
      if (selectedMenuTags.length > 1) {
        if (!auto)
          proxy.$modal.msgError("选择了多个标签，请确保每个选品只选择一个标签");
        return;
      }
      const selectedMenuTag = selectedMenuTags[0];
      if (selectedMenuTag) {
        selectedMenuTag.ancestors.split(",").forEach((ancestorId) => {
          if (
            ancestorId != "0" &&
            !formData.tagIds.some((path) => path.includes(ancestorId))
          ) {
            formData.tagIds.push(ancestorId);
          }
        });
      }
    }
    resetForm(selectedTagIds);

    generateSpu(true);

    // 优化1：打开弹窗时，来源 URL 自动获得焦点
    nextTick(() => {
      setTimeout(() => {
        sourceUrlInputRef.value.focus();
      }, 50); // 增加延迟以等待 Dialog 动画完成
    });

    // 保存初始数据快照
    saveInitialDataSnapshot();
    return;
  }

  title.value = "编辑商品";
  activeStep.value = 0;
  resetForm();

  // 加载商品表单数据
  await handleLoadData(productId);
};

// 加载商品表单数据
const handleLoadData = async (productId) => {
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
  if (productData.optionJson) {
    try {
      optionList.value = JSON.parse(productData.optionJson);
    } catch (e) {
      optionList.value = [];
    }
  }

  // 加载变体数据
  if (
    productData.productVariantList &&
    productData.productVariantList.length > 0
  ) {
    variants.value = productData.productVariantList.map((v, index) => ({
      variantId: v.variantId,
      productId: v.productId || productId,
      sku:
        v.sku ||
        productData.spu + "-" + (index + 1).toString().padStart(3, "0"),
      price: v.price,
      compareAtPrice: v.compareAtPrice,
      purchasePrice: v.purchasePrice,
      purchaseUrl: v.purchaseUrl || formData.purchaseUrl,
      optionValues: v.optionValues || "",
      optionValueList: v.optionValues ? JSON.parse(v.optionValues) : [],
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
  }

  // 保存初始数据快照
  saveInitialDataSnapshot();
};

// 保存初始数据快照
function saveInitialDataSnapshot() {
  initialDataSnapshot.value = {
    formData: JSON.parse(JSON.stringify(formData)),
    optionList: JSON.parse(JSON.stringify(optionList.value)),
    variants: JSON.parse(JSON.stringify(variants.value)),
  };
  console.log("保存初始数据快照:", initialDataSnapshot.value);
}

// 比较数据是否发生变化
function hasDataChanged() {
  if (!initialDataSnapshot.value) {
    return true; // 没有初始快照，默认为有变化
  }

  const currentData = {
    formData: JSON.parse(JSON.stringify(formData)),
    optionList: JSON.parse(JSON.stringify(optionList.value)),
    variants: JSON.parse(JSON.stringify(variants.value)),
  };
  console.log("当前数据:", currentData);

  // 比较数据是否相同
  return (
    JSON.stringify(currentData) !== JSON.stringify(initialDataSnapshot.value)
  );
}

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
      isActualShipment: "0",
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
    const selectedMenuTags = getMenuTag(formData.tagIds);

    if (!selectedMenuTags) {
      if (!auto) proxy.$modal.msgError("未选择标签");
      return;
    }
    if (selectedMenuTags.length > 1) {
      if (!auto)
        proxy.$modal.msgError("选择了多个标签，请确保每个选品只选择一个标签");
      return;
    }

    const selectedMenuTag = selectedMenuTags[0];
    if (selectedMenuTag.spuPrefix == null) {
      if (!auto)
        proxy.$modal.msgError("当前选中的标签没有配置 SPU 前缀，无法生成 SPU");
      return;
    }

    // ✅ 模拟异步操作（如果需要调用后端接口更新序列号，可在此处 await）
    await new Promise((resolve) => setTimeout(resolve, 300));

    const seq = selectedMenuTag.currentMaxSeq || 0;
    const nextSeq = seq + 1;
    const paddedSeq = String(nextSeq).padStart(3, "0");
    formData.spu = `${selectedMenuTag.spuPrefix}${paddedSeq}`;

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
    // 收起后，确保焦点保持在当前选项行上
    nextTick(() => {
      const optionRows = document.querySelectorAll(".option-row");
      if (optionRows[optIndex]) {
        optionRows[optIndex].focus();
      }
    });
  }
}

// 删除选项值
function removeOptionValue(optIndex, valIndex) {
  optionList.value[optIndex].values.splice(valIndex, 1);
}

// 处理 Enter 键导航
function handleTabKey(event, optIndex, valIndex, type) {
  const values = optionList.value[optIndex].values;
  if (valIndex == null) {
    // 选项名称跳转到第一个选项值
    focusNextInput(optIndex, 1, type);
    return;
  }
  // 从采购选项值跳到下一个采购选项值，从英文选项值跳到下一个英文选项值
  if (valIndex < values.length - 1) {
    focusNextInput(optIndex, valIndex + 2, type);
  } else {
    // 如果是最后一个，添加新行并聚焦
    addOptionValue(optIndex);
    focusNextInput(optIndex, values.length, type);
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
        isActualShipment: "0",
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
  const combinations = cartesianProduct(valueArrays);

  // 2. 映射生成变体行
  variants.value = combinations.map((combo, index) => {
    const existingVariant = variants.value[index];
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
      mediaId: existingVariant?.mediaId || null,
      position: existingVariant?.position || index,
      pkWidth: existingVariant?.pkWidth || null,
      pkHeight: existingVariant?.pkHeight || null,
      pkLength: existingVariant?.pkLength || null,
      materialWeight: existingVariant?.materialWeight || null,
      pkWeight: existingVariant?.pkWeight || null,
      freight: existingVariant?.freight || null,
      isActualShipment: existingVariant?.isActualShipment || "0",
      unitCostPrice: existingVariant?.unitCostPrice || null,
      remark: existingVariant?.remark || "",
    };
  });
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
  try {
    if (row.purchasePrice == null) {
      return;
    }
    console.log("calculateVariantCost", row);
    row.unitCostPrice = (row.purchasePrice || 0) + (row.shippingFee || 0);
    if (row.isActualShipment !== "0") {
      row.unitCostPrice = row.purchasePrice || 0;
    }
    // 自动计算销售价格
    if (row.unitCostPrice > 0) {
      // 假设美元汇率 7.2，成本价基础上加30%作为销售价 向上取整
      row.price = Math.ceil((row.unitCostPrice * 1.3) / 7.2); // 假设美元汇率 7.2
    }
  } catch (error) {
    console.error("计算变体成本失败:", error);
    proxy.$modal.msgError("计算变体成本失败，请检查输入数据");
  }
}

// 计算材积重
function calculateVolumetricWeight(row) {
  try {
    if (row.dimension) {
      const dims = row.dimension
        .split(/[\/\*x]/)
        .map((d) => parseFloat(d.trim()));
      if (dims.length >= 3 && dims.every((d) => !isNaN(d))) {
        row.volumetricWeight = (dims[0] * dims[1] * dims[2]) / 8000;
      } else {
        proxy.$modal.msgWarning(
          "尺寸格式不正确，请使用 L/W/H 格式，如: 10/10/10",
        );
      }
    }
  } catch (error) {
    console.error("计算材积重失败:", error);
    proxy.$modal.msgError("计算材积重失败，请检查尺寸格式");
  }
}

// 计算运费
async function calculateShippingFee(row) {
  try {
    console.log("calculateShippingFee", row);
    if (row.weight > 0) {
      const response = await calculateShipping({ weight: row.weight });
      row.shippingFee = response.data?.fee || 0;
      calculateVariantCost(row);
    }
  } catch (error) {
    console.error("计算运费失败", error);
    proxy.$modal.msgError("计算运费失败，请稍后重试");
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
  formData.mediaIdList.splice(index, 1);
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

// 提交表单continue、next、close
async function handleSubmit(action) {
  try {
    // 根据当前步骤只校验对应的表单
    if (activeStep.value === 0) {
      await step1FormRef.value?.validate();
    } else if (activeStep.value === 1) {
      // 第二步自定义校验：变体选项必填
      for (let i = 0; i < variants.value.length; i++) {
        const variant = variants.value[i];
        if (variant.optionValueList && variant.optionValueList.length > 0) {
          for (let j = 0; j < variant.optionValueList.length; j++) {
            const opt = variant.optionValueList[j];
            if (!opt.purchaseName || !opt.purchaseValue) {
              proxy.$modal.msgError(
                `第 ${i + 1} 个变体的第 ${j + 1} 个选项名称和值不能为空`,
              );
              return;
            }
          }
        }
      }
      await step2FormRef.value?.validate();
    }
    loading.value = true;

    let hasChanged = true;
    if (activeStep.value === 0) {
      hasChanged = hasDataChanged();
      if (hasChanged) {
        // 保存第一步数据
        const submitData = {
          ...formData,
          optionJson: JSON.stringify(optionList.value),
          productVariantList: variants.value,
        };

        const response = await addSelectionInfo(submitData);
        if (response.code !== 200) {
          proxy.$modal.msgError(response.msg || "保存失败");
          return;
        }
        // 重新加载商品表单数据
        await handleLoadData(response.data);
      }
    } else {
      // 保存第二步数据
      // 确保变体中的 optionValueList 同步更新到 optionValues 字符串（如果需要后端兼容）
      const variantsToSubmit = variants.value.map((v) => ({
        ...v,
        optionValues: JSON.stringify(v.optionValueList),
      }));

      const submitData = {
        ...formData,
        optionJson: JSON.stringify(optionList.value),
        productVariantList: variantsToSubmit,
        mediaIdList: formData.mediaIdList, // 使用 mediaIdList
      };
      await updateBaseInfo(submitData);
    }

    emit("submit", { action, hasChanged });

    if (action === "close") {
      visible.value = false;
    } else if (action === "continue") {
      resetForm(formData.tagIds);
      activeStep.value = 0;
    } else {
      // next
      activeStep.value = 1;
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
  emit("submit", { action: "cancel", hasChanged: false });
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

/* 当拖拽经过时高亮 */
.image-item.drag-over {
  border-color: #409eff;
  border-style: dashed;
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
  justify-content: space-between;
  align-items: center;
}

.footer-right-buttons {
  display: flex;
  gap: 10px;
}

/* 可展开文本域样式 */
.expandable-textarea {
  width: 100%;
}

.textarea-collapse {
  width: 100%;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s;
}

.textarea-collapse:hover {
  border-color: #c0c4cc;
  background: #ecf5ff;
}

.placeholder-text {
  color: #909399;
  font-size: 14px;
}

/* 富文本编辑器样式 */
.rich-text-editor-container {
  width: 100%;
}

.rich-text-editor-collapse {
  width: 100%;
  padding: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s;
}

.rich-text-editor-collapse:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.rich-text-editor-expanded {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.rich-text-editor-toolbar {
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rich-text-preview {
  padding: 12px;
  min-height: 150px;
  background: #fff;
}

.rich-text-preview .no-content {
  color: #909399;
}

/* 选项值列表样式 */
.option-value-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.option-value-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

/* 第二步表单美化样式 */
.step2-form {
  width: 100%;
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

/* 分割线样式优化 */
.step2-form .el-divider {
  margin: 24px 0;
}

.step2-form .el-divider__text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  background: #ffffff;
  padding: 0 16px;
}

/* 表单项样式优化 */
.step2-form .el-form-item {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.step2-form .el-form-item:hover {
  transform: translateY(-1px);
}

/* 输入框样式优化 */
.step2-form .el-input__wrapper {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.step2-form .el-input__wrapper:hover {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.step2-form .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 文本域样式优化 */
.step2-form .el-textarea__inner {
  border-radius: 6px;
  resize: vertical;
  min-height: 80px;
  transition: all 0.3s ease;
}

.step2-form .el-textarea__inner:focus {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 按钮样式优化 */
.step2-form .el-button {
  border-radius: 6px;
  transition: all 0.3s ease;
}

.step2-form .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

/* 表格样式优化 */
.step2-form .el-table {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  margin-top: 16px;
}

.step2-form .el-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #303133;
}

.step2-form .el-table tr {
  transition: background-color 0.3s ease;
}

.step2-form .el-table tr:hover {
  background-color: #f5f7fa !important;
}

/* 图片管理区域优化 */
.image-manager {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s ease;
}

.image-manager:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
}

.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: move;
}

.image-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: all 0.3s ease;
}

.image-item:hover .image-thumb {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.image-placeholder {
  width: 100%;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  transition: all 0.3s ease;
}

.image-placeholder:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.placeholder-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.image-placeholder:hover .placeholder-icon {
  color: #409eff;
  transform: scale(1.1);
}

/* 变体图片拖拽区域 */
.variant-image-drop {
  width: 80px;
  height: 80px;
  border: 2px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  background: #fafafa;
}

.variant-image-drop:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.variant-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.variant-image-drop:hover .variant-thumb {
  transform: scale(1.05);
}

/* 可展开文本域样式优化 */
.expandable-textarea {
  width: 100%;
}

.textarea-collapse {
  width: 100%;
  padding: 16px;
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  background: #f8f9fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;
}

.textarea-collapse:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.placeholder-text {
  color: #909399;
  font-size: 14px;
  transition: all 0.3s ease;
}

.textarea-collapse:hover .placeholder-text {
  color: #409eff;
}

/* 富文本编辑器样式优化 */
.rich-text-editor-container {
  width: 100%;
}

.rich-text-editor-collapse {
  width: 100%;
  padding: 32px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #f8f9fa;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.rich-text-editor-collapse:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.rich-text-editor-expanded {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.rich-text-editor-expanded:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.rich-text-editor-toolbar {
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rich-text-preview {
  padding: 20px;
  min-height: 200px;
  background: #ffffff;
  border-top: 1px solid #e4e7ed;
  transition: all 0.3s ease;
}

.rich-text-preview:hover {
  background: #fafafa;
}

.rich-text-preview .no-content {
  color: #909399;
  text-align: center;
  padding: 40px 0;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .step2-form {
    padding: 16px;
  }

  .el-row {
    flex-direction: column;
  }

  .el-col {
    width: 100% !important;
    margin-bottom: 16px;
  }

  .image-item {
    width: 100px;
    height: 100px;
  }

  .image-placeholder {
    height: 150px;
  }

  .rich-text-editor-collapse {
    padding: 24px;
  }

  .rich-text-preview {
    min-height: 150px;
  }
}
</style>
