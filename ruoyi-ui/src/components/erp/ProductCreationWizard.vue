<template>
  <el-dialog
    title="商品选品向导"
    :visible.sync="visible"
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
        <el-input v-model="step1Form.spu" placeholder="自动生成或手动输入"></el-input>
      </el-form-item>
      <el-form-item label="来源URL" prop="sourceUrl">
        <el-input v-model="step1Form.sourceUrl"></el-input>
      </el-form-item>
      <el-form-item label="采购链接" prop="purchaseLink">
        <el-input v-model="step1Form.purchaseLink"></el-input>
      </el-form-item>
      <el-form-item label="采购商品选项">
        <div v-for="(option, index) in step1Form.purchaseOptions" :key="index" class="option-item">
          <el-input v-model="option.name" placeholder="选项名称" style="width: 30%; margin-right: 10px;"></el-input>
          <el-input
            v-model="option.values"
            placeholder="选项值，用逗号分隔"
            style="width: 50%; margin-right: 10px;"
          ></el-input>
          <el-button @click="removeOption(index)" type="danger" size="small">删除</el-button>
        </div>
        <el-button @click="addOption" type="primary" size="small">添加选项</el-button>
      </el-form-item>

      <!-- 变体预览 -->
      <el-form-item label="变体列表">
        <draggable v-model="variants" group="variants" handle=".drag-handle">
          <div v-for="(variant, index) in variants" :key="index" class="variant-item">
            <i class="el-icon-rank drag-handle"></i>
            <span>{{ variant.title }}</span>
            <el-input
              v-model="variant.purchaseLink"
              placeholder="采购链接"
              style="width: 200px; margin-left: 10px;"
            ></el-input>
            <el-input-number
              v-model="variant.purchasePrice"
              :precision="2"
              :min="0"
              placeholder="采购价"
              style="width: 100px; margin-left: 10px;"
            ></el-input-number>
            <el-button @click="removeVariant(index)" type="danger" size="small" style="margin-left: 10px;">删除</el-button>
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
        <quill-editor v-model="step2Form.bodyHtml" :options="editorOption"></quill-editor>
      </el-form-item>

      <!-- 商品图片列表 -->
      <el-form-item label="商品图片列表">
        <el-button @click="scanMedia" type="primary" size="small">导入媒体</el-button>
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
          <el-table-column prop="title" label="选项" width="150"></el-table-column>
          <el-table-column label="SKU" width="150">
            <template slot-scope="scope">
              <el-input v-model="scope.row.sku"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="图片" width="200">
            <template slot-scope="scope">
              <div
                class="drop-zone"
                @drop="onDrop($event, scope.row)"
                @dragover.prevent
                @dragenter.prevent
              >
                <img v-if="scope.row.media" :src="scope.row.media.url" alt="variant image" />
                <span v-else>拖拽图片到此处</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="包装宽度" width="100">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.pkWidth"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="包装高度" width="100">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.pkHeight"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="包装长度" width="100">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.pkLength"
                :min="0"
                @change="calculateVolumeWeight(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="材积重" width="100">
            <template slot-scope="scope">
              {{ scope.row.materialWeight }}
            </template>
          </el-table-column>
          <el-table-column label="常规包装重量" width="120">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.pkWeight"
                :min="0"
                @change="calculateCostPrice(scope.row)"
              ></el-input-number>
            </template>
          </el-table-column>
          <el-table-column label="运费" width="100">
            <template slot-scope="scope">
              {{ scope.row.freight }}
            </template>
          </el-table-column>
          <el-table-column label="商品成本价" width="120">
            <template slot-scope="scope">
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

    <span slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button v-if="activeStep === 0" @click="nextStep" type="primary">下一步</el-button>
      <el-button v-if="activeStep === 1" @click="prevStep">上一步</el-button>
      <el-button v-if="activeStep === 0" @click="saveAndNew" type="success">保存并新增</el-button>
      <el-button v-if="activeStep === 0" @click="saveAndClose" type="primary">保存并关闭</el-button>
      <el-button v-if="activeStep === 1" @click="saveProduct" type="primary">保存商品</el-button>
    </span>
  </el-dialog>
</template>

<script>
import draggable from 'vuedraggable'
import 'quill/dist/quill.snow.css'
import { quillEditor } from 'vue-quill-editor'
import { listTag } from '@/api/erp/tag'
import { scanMedia } from '@/api/erp/media'
import { addProduct, calculateFreight, updateProduct } from '@/api/erp/product'
import { updateTag } from '@/api/erp/tag'

export default {
  name: 'ProductCreationWizard',
  components: {
    draggable,
    quillEditor
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    selectedTags: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      activeStep: 0,
      step1Form: {
        spu: '',
        sourceUrl: '',
        purchaseLink: '',
        purchaseOptions: []
      },
      step1Rules: {
        spu: [{ required: true, message: '请输入 SPU', trigger: 'blur' }],
        sourceUrl: [{ required: true, message: '请输入来源URL', trigger: 'blur' }],
        purchaseLink: [{ required: true, message: '请输入采购链接', trigger: 'blur' }]
      },
      step2Form: {
        productTitle: '',
        category: '',
        productType: '',
        description: '',
        size: '',
        material: '',
        note: '',
        packageInclude: '',
        bodyHtml: ''
      },
      step2Rules: {
        productTitle: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
        category: [{ required: true, message: '请输入商品类别ID', trigger: 'blur' }],
        productType: [{ required: true, message: '请输入商品类型', trigger: 'blur' }]
      },
      variants: [],
      selectedMedia: [],
      draggedMedia: null,
      editorOption: {
        // quill-editor 选项
        theme: 'snow'
      },
      isEdit: false // 新增的属性，用于标识是否为编辑状态
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initWizard()
      }
    },
    'step1Form.purchaseOptions': {
      handler() {
        this.generateVariants()
      },
      deep: true
    }
  },
  methods: {
    initWizard() {
      this.activeStep = 0
      this.generateSPU()
      this.step1Form.purchaseOptions = []
      this.variants = [{ title: 'Default Title', purchaseLink: '', purchasePrice: 0, sku: '', media: null, pkWidth: 0, pkHeight: 0, pkLength: 0, materialWeight: 0, pkWeight: 0, freight: 0, unitCostPrice: 0, price: 0 }]
      this.step2Form = {
        productTitle: '',
        category: '',
        productType: '',
        description: '',
        size: '',
        material: '',
        note: '',
        packageInclude: '',
        bodyHtml: ''
      }
      this.selectedMedia = []
      this.isEdit = false // 重置编辑状态
    },
    generateSPU() {
      const menuTag = this.selectedTags.find(tag => tag.tagType === 'MENU')
      if (menuTag) {
        // 假设有 API 获取标签详情，包括 spuPrefix 和 currentMaxSeq
        // 这里简化，直接使用 tag ��据
        const prefix = menuTag.spuPrefix
        const seq = menuTag.currentMaxSeq + 1
        this.step1Form.spu = `${prefix}-${String(seq).padStart(3, '0')}`
      }
    },
    addOption() {
      this.step1Form.purchaseOptions.push({ name: '', values: '' })
    },
    removeOption(index) {
      this.step1Form.purchaseOptions.splice(index, 1)
    },
    generateVariants() {
      const options = this.step1Form.purchaseOptions.filter(opt => opt.name && opt.values)
      if (options.length === 0) {
        this.variants = [{ title: 'Default Title', purchaseLink: this.step1Form.purchaseLink, purchasePrice: 0, sku: '', media: null, pkWidth: 0, pkHeight: 0, pkLength: 0, materialWeight: 0, pkWeight: 0, freight: 0, unitCostPrice: 0, price: 0 }]
        return
      }
      const combinations = this.cartesianProduct(options.map(opt => opt.values.split(',').map(v => v.trim())))
      this.variants = combinations.map(combo => {
        const title = combo.join(' / ')
        return {
          title,
          purchaseLink: this.step1Form.purchaseLink,
          purchasePrice: 0,
          sku: '',
          media: null,
          pkWidth: 0,
          pkHeight: 0,
          pkLength: 0,
          materialWeight: 0,
          pkWeight: 0,
          freight: 0,
          unitCostPrice: 0,
          price: 0
        }
      })
    },
    cartesianProduct(arrays) {
      return arrays.reduce((acc, curr) => acc.flatMap(a => curr.map(b => [...a, b])), [[]])
    },
    removeVariant(index) {
      this.variants.splice(index, 1)
    },
    nextStep() {
      this.$refs.step1Form.validate(valid => {
        if (valid) {
          this.activeStep = 1
        }
      })
    },
    prevStep() {
      this.activeStep = 0
    },
    saveAndNew() {
      this.saveProduct(true)
    },
    saveAndClose() {
      this.saveProduct(false)
    },
    saveProduct(isNew = false) {
      const form = this.activeStep === 0 ? this.$refs.step1Form : this.$refs.step2Form
      form.validate(valid => {
        if (valid) {
          const productData = {
            ...this.step1Form,
            ...this.step2Form,
            selectedTagIds: this.selectedTags.map(tag => tag.tagId),
            variants: this.variants,
            media: this.selectedMedia
          }
          const api = this.isEdit ? updateProduct : addProduct
          api(productData).then(() => {
            this.$message.success('保存成功')
            // 更新标签的 currentMaxSeq 只在新增时
            if (!this.isEdit) {
              const menuTag = this.selectedTags.find(tag => tag.tagType === 'MENU')
              if (menuTag) {
                updateTag({ ...menuTag, currentMaxSeq: menuTag.currentMaxSeq + 1 }).then(() => {
                  // 可选：重新加载标签
                })
              }
            }
            if (isNew) {
              this.initWizard()
            } else {
              this.$emit('update:visible', false)
            }
          })
        }
      })
    },
    handleClose() {
      this.$emit('update:visible', false)
    },
    loadProductData(product) {
      // 加载产品数据用于编辑
      this.step1Form = {
        spu: product.spu,
        sourceUrl: product.sourceUrl,
        purchaseLink: product.purchaseLink,
        purchaseOptions: product.purchaseOptions || []
      }
      this.step2Form = {
        productTitle: product.productTitle,
        category: product.category,
        productType: product.productType,
        description: product.description,
        size: product.size,
        material: product.material,
        note: product.note,
        packageInclude: product.packageInclude,
        bodyHtml: product.bodyHtml
      }
      this.variants = product.variants || []
      this.selectedMedia = product.media || []
      // 设置为编辑模式
      this.isEdit = true
    }
  }
}
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
