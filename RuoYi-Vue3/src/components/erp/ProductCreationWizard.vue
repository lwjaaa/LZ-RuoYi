<template>
  <el-dialog
    :title="title"
    v-model="visible"
    width="1200px"
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
        :model="step1FormData"
        :rules="step1Rules"
        label-width="120px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标签" prop="tagIds">
              <el-cascader
                v-model="step1FormData.tagIds"
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
              <el-input
                v-model="step1FormData.spu"
                placeholder="自动生成或手动输入"
              >
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
                v-model="step1FormData.sourceUrl"
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
                v-model="step1FormData.purchaseUrl"
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
            :data="step1Variants"
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
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row, $index }">
                <el-button
                  v-if="step1Variants.length > 1"
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
        :model="step2FormData"
        :rules="step2Rules"
        label-width="68px"
        class="step2-form"
        :label-position="'left'"
      >
        <!-- 主商品信息 -->
        <el-divider content-position="left">主商品信息</el-divider>

        <!-- SPU 与 商品标题 同行 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="SPU" prop="spu">
              <el-input
                v-model="step2FormData.spu"
                disabled
                placeholder="系统自动生成"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="商品类别" prop="category">
              <el-input
                v-model="step2FormData.category"
                placeholder="请输入商品类别"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品类型" prop="productType">
              <el-input
                v-model="step2FormData.productType"
                placeholder="请输入商品类型"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 商品类别 与 商品类型 同行 -->
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="商品标题" prop="productTitle">
              <el-input
                v-model="step2FormData.productTitle"
                placeholder="请输入商品标题"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 商品详情选项卡 -->
        <el-form-item label="商品详情" prop="detailTabs">
          <el-tabs
            v-model="activeDetailTab"
            type="border-card"
            class="detail-tabs"
          >
            <!-- NOTE 选项卡 -->
            <el-tab-pane label="DESCRIPTION" name="description">
              <el-input
                v-model="step2FormData.description"
                type="textarea"
                :rows="4"
                placeholder="请输入商品详情"
              />
            </el-tab-pane>
            <!-- SIZE 选项卡 -->
            <el-tab-pane label="SIZE" name="size">
              <el-input
                v-model="step2FormData.size"
                type="textarea"
                :rows="4"
                placeholder="请输入SIZE"
              />
            </el-tab-pane>
            <!-- MATERIAL 选项卡 -->
            <el-tab-pane label="MATERIAL" name="material">
              <el-input
                v-model="step2FormData.material"
                type="textarea"
                :rows="4"
                placeholder="请输入MATERIAL"
              />
            </el-tab-pane>
            <!-- NOTE 选项卡 -->
            <el-tab-pane label="NOTE" name="note">
              <el-input
                v-model="step2FormData.note"
                type="textarea"
                :rows="4"
                placeholder="请输入NOTE"
              />
            </el-tab-pane>
            <!-- PACKAGE_INCLUDE 选项卡 -->
            <el-tab-pane label="PACKAGE_INCLUDE" name="packageInclude">
              <el-input
                v-model="step2FormData.packageInclude"
                type="textarea"
                :rows="4"
                placeholder="请输入PACKAGE_INCLUDE"
              />
            </el-tab-pane>
            <!-- 商品详情描述选项卡 -->
            <el-tab-pane label="商品详情描述" name="bodyHtml">
              <div class="rich-text-editor-container">
                <div class="rich-text-editor-expanded">
                  <div class="rich-text-editor-toolbar">
                    <el-radio-group v-model="richTextEditor.mode" size="small">
                      <el-radio-button value="edit">HTML 输入</el-radio-button>
                      <el-radio-button value="preview">预览</el-radio-button>
                    </el-radio-group>
                  </div>
                  <el-input
                    v-if="richTextEditor.mode === 'edit'"
                    v-model="step2FormData.bodyHtml"
                    type="textarea"
                    :rows="6"
                    placeholder="请输入商品详情描述 (HTML)"
                  />
                  <div
                    v-else
                    class="rich-text-preview"
                    v-html="
                      step2FormData.bodyHtml ||
                      '<span class=no-content>暂无内容</span>'
                    "
                  ></div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-form-item>

        <!-- 媒体文件列表 -->
        <el-divider content-position="left">商品媒体文件管理</el-divider>
        <el-form-item label="媒体文件列表">
          <div class="image-manager">
            <div class="image-toolbar mb-2">
              <el-input
                v-model="step2FormData.imageSearchKeyword"
                placeholder="输入媒体文件所在目录搜索"
                style="width: 300px"
                clearable
                @keyup.enter="loadServerImages"
              />
              <el-button
                type="primary"
                icon="Upload"
                @click="loadServerImages"
                :loading="imageLoading"
                size="default"
              >
                从服务器导入
              </el-button>
            </div>
            <!-- 添加拖拽排序事件 -->
            <div
              class="image-grid"
              @dragover.prevent
              @drop="handleImageDrop($event)"
            >
              <div
                v-for="(media, index) in step2FormData.mediaList"
                :key="media.mediaId || index"
                class="image-item"
                draggable
                @dragstart="handleImageDragStart($event, media)"
                @dragenter.prevent="handleImageDragEnter($event, index)"
                @dragleave.prevent="handleImageDragLeave($event)"
                :title="media.alt || media.filename"
                :class="{
                  'drag-over':
                    dragOverIndex === index && draggedImage !== media,
                }"
              >
                <!-- 图片展示 -->
                <template v-if="isImage(media)">
                  <el-image
                    :src="
                      baseUrl + (media.nasMediaUrl || media.shopifyMediaUrl)
                    "
                    :alt="media.alt || media.filename"
                    class="image-thumb"
                    :preview-src-list="imagePreviewList"
                    :initial-index="
                      imagePreviewList.indexOf(
                        baseUrl + (media.nasMediaUrl || media.shopifyMediaUrl),
                      )
                    "
                    lazy
                    show-progress
                    preview-teleported
                    fit="cover"
                  />
                </template>
                <!-- 视频展示 -->
                <template v-else-if="isVideo(media)">
                  <div class="video-thumbnail" @click="openVideoModal(media)">
                    <el-image
                      :src="getVideoThumbnail(media)"
                      class="image-thumb"
                      fit="cover"
                    />
                    <div class="video-play-button">
                      <el-icon><VideoPlay /></el-icon>
                    </div>
                  </div>
                </template>
                <!-- 其他类型文件 -->
                <template v-else>
                  <div class="file-placeholder">
                    <el-icon><Document /></el-icon>
                    <span class="file-name">{{ media.filename }}</span>
                  </div>
                </template>
                <div class="image-overlay">
                  <span class="media-type-badge">{{
                    getMediaTypeLabel(media)
                  }}</span>
                </div>
              </div>
              <div
                class="image-placeholder"
                v-if="step2FormData.mediaList?.length === 0"
              >
                <el-icon class="placeholder-icon"><Picture /></el-icon>
                <span class="placeholder-text">点击"从服务器导入"</span>
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 变体详细设置 -->
        <el-divider content-position="left">变体详细设置</el-divider>
        <el-table
          :data="step2Variants"
          border
          style="width: 100%"
          :loading="loading"
          row-key="variantId"
          :row-class-name="tableRowClassName"
        >
          <!-- 动态生成选项列 -->
          <template v-if="getActiveOptions().length > 0">
            <el-table-column
              v-for="(opt, idx) in getActiveOptions()"
              :key="opt.purchaseName || idx"
              :label="opt.purchaseName || '选项'"
              width="120"
              align="center"
              fixed="left"
            >
              <template #default="{ row }">
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
          <el-table-column
            v-else
            label="默认规格"
            width="120"
            align="center"
            fixed="left"
          >
            <template #default>
              <span>-</span>
            </template>
          </el-table-column>
          <el-table-column label="SKU" width="150" fixed="left">
            <template #default="{ row }">
              <el-input v-model="row.sku" size="small" />
            </template>
          </el-table-column>
          <!-- 规格图 -->
          <el-table-column
            label="规格图"
            align="center"
            width="100"
            fixed="left"
          >
            <template #default="{ row }">
              <div
                class="variant-image-item"
                @dragover.prevent
                @drop="handleVariantImageDrop($event, row)"
                @dragstart="handleVariantImageDragStart($event, row)"
                @dragenter.prevent="handleVariantImageDragEnter($event, row)"
                @dragleave.prevent="handleVariantImageDragLeave($event)"
                draggable
                title="拖拽图片到此处或拖拽出区域删除"
                :class="{
                  'drag-over':
                    dragOverVariant === row && draggedVariantRow !== row,
                }"
              >
                <!-- 图片展示 -->
                <template v-if="row.media">
                  <el-image
                    :src="
                      baseUrl +
                      (row.media.nasMediaUrl || row.media.shopifyMediaUrl)
                    "
                    class="variant-thumb"
                    :alt="'Variant Image'"
                    :preview-src-list="[
                      baseUrl +
                        (row.media.nasMediaUrl || row.media.shopifyMediaUrl),
                    ]"
                    preview-teleported
                    fit="cover"
                  />
                </template>
                <span v-else class="drop-hint">无规格图</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="采购价" align="center" width="110">
            <template #header>
              <el-tooltip content="单位：元" placement="top">
                <span>采购价</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
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
          <el-table-column label="包装长度" align="center" width="90">
            <template #header>
              <el-tooltip content="单位：厘米" placement="top">
                <span>包装长度</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.pkLength"
                :min="0"
                :precision="0"
                size="small"
                style="width: 60px"
                @change="calculateMaterialWeight(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="包装宽度" align="center" width="90">
            <template #header>
              <el-tooltip content="单位：厘米" placement="top">
                <span>包装宽度</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.pkWidth"
                :min="0"
                :precision="0"
                size="small"
                style="width: 60px"
                @change="calculateMaterialWeight(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="包装高度" align="center" width="90">
            <template #header>
              <el-tooltip content="单位：厘米" placement="top">
                <span>包装高度</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.pkHeight"
                :min="0"
                :precision="0"
                size="small"
                style="width: 60px"
                @change="calculateMaterialWeight(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="实重" align="center" width="94">
            <template #header>
              <el-tooltip content="单位：KG" placement="top">
                <span>实重</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                v-model="row.pkWeight"
                :controls="false"
                :min="0"
                :precision="3"
                size="small"
                style="width: 65px"
                @change="calculateFreight(row)"
                placeholder="0.000"
              />
            </template>
          </el-table-column>
          <el-table-column label="材积重" align="right" width="80">
            <template #header>
              <el-tooltip content="材积重 = (L * W * H) / 8000" placement="top">
                <span>材积重</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <span>{{ row.materialWeight || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="实际发货" align="center" width="80">
            <template #default="{ row }">
              <el-switch
                v-model="row.isActualShipment"
                :active-value="'1'"
                :inactive-value="'0'"
                @change="calculateVariantCost(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="运费" align="center" width="110">
            <template #header>
              <el-tooltip placement="top">
                <template #content>
                  单位：元<br />输入包装尺寸和实重，自动计算运费</template
                >
                <span>运费</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.freight"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
                @change="calculateVariantCost(row)"
                placeholder="0.00"
              />
            </template>
          </el-table-column>
          <el-table-column label="成本价" align="center" width="110">
            <template #header>
              <el-tooltip placement="top">
                <template #content>
                  单位：元<br />成本价 = 采购价 + 运费</template
                >
                <span>成本价</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.unitCostPrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
                @change="calculateVariantCost(row)"
                placeholder="0.00"
              />
            </template>
          </el-table-column>
          <!-- 美国汇率 -->
          <el-table-column label="汇率" align="center" width="110">
            <template #header>
              <el-tooltip content="首次默认填充今日美国汇率" placement="top">
                <span>美国汇率</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.exchangeRate"
                :min="0"
                :precision="4"
                size="small"
                style="width: 80px"
                placeholder="0.0000"
              />
            </template>
          </el-table-column>
          <!-- 建议售价 -->
          <el-table-column label="建议售价" align="center" width="110">
            <template #header>
              <el-tooltip placement="top">
                <template #content> 单位：美元<br />按照30%利润计算</template>
                <span>建议售价</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <span>{{ row.suggestedPrice || "" }}</span>
            </template>
          </el-table-column>
          <!-- 销售价格 -->
          <el-table-column
            label="售价"
            align="center"
            width="110"
            fixed="right"
          >
            <template #header>
              <el-tooltip placement="top">
                <template #content>
                  单位：美元<br />默认按照30%利润计算</template
                >
                <span>售价</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.price"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
                placeholder="0.00"
                @change="calculateProfitRate(row)"
              />
            </template>
          </el-table-column>
          <!-- 利润率 -->
          <el-table-column label="利润率" align="center" width="110">
            <template #header>
              <el-tooltip content="单位：%" placement="top">
                <template #content>
                  单位：%<br />实际利润率 = 售价 * 汇率 - 成本价 / (销售价格 *
                  汇率）</template
                >
                <span>利润率</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <span>{{ row.profitRate || "" }}</span>
            </template>
          </el-table-column>
          <!-- 对比价 -->
          <el-table-column
            label="对比价"
            align="center"
            width="110"
            fixed="right"
          >
            <template #header>
              <el-tooltip content="单位：美元" placement="top">
                <span>对比价</span>
              </el-tooltip>
            </template>
            <template #default="{ row }">
              <el-input-number
                :controls="false"
                v-model="row.comparePrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 80px"
                placeholder="0.00"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-form>
    </div>

    <!-- 视频播放弹框 -->
    <CustomVideoModal
      v-model:visible="videoModalVisible"
      :video="currentVideo"
      :base-url="baseUrl"
    />

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
            <el-button @click="handleSubmit('prev')" type="primary"
              >上一步</el-button
            >
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
import {
  Picture,
  ArrowDown,
  Edit,
  VideoPlay,
  Document,
} from "@element-plus/icons-vue";
import { ElMessageBox } from "element-plus";
import CustomVideoModal from "./CustomVideoModal.vue";
import {
  addSelectionInfo,
  updateBaseInfo,
  calculateShipping,
} from "@/api/erp/productWizard";
import { getProduct } from "@/api/erp/product";
import { treeList } from "@/api/erp/tag";
import { scanMedia } from "@/api/erp/media";
import { useExchangeRateStore } from "@/store/modules/exchangeRate";
const { proxy } = getCurrentInstance();

// 汇率状态管理
const exchangeRateStore = useExchangeRateStore();

const baseUrl = import.meta.env.VITE_APP_BASE_API;

// Emit
const emit = defineEmits(["submit"]);

// 注册组件
const components = {
  CustomVideoModal,
};

// 状态
const visible = ref(false);

const activeStep = ref(0);
const title = ref("新增商品");
const loading = ref(false);
const step1FormRef = ref();
const step2FormRef = ref();
const sourceUrlInputRef = ref(); // 来源 URL 输入框引用

// 拖拽相关状态
const draggedVariantRow = ref(null); // 存储拖拽的变体行数据
const dragOverVariant = ref(null); // 存储当前拖拽经过的变体

// 表单数据

/** 第一步表单数据 */
const step1FormData = reactive({
  productId: null,
  spu: "",
  sourceUrl: "",
  purchaseUrl: "",
  tagIds: [],
});

/** 第二步表单数据 */
const step2FormData = reactive({
  productId: null,
  productTitle: "",
  category: "",
  productType: "",
  description: "",
  size: "",
  material: "",
  note: "",
  packageInclude: "",
  bodyHtml: "",
  mediaList: [],
  mainMediaId: null,
  remark: "",
  imageSearchKeyword: "",
});

// 计算属性：所有图片的 URL 列表（不包含视频）
const imagePreviewList = computed(() => {
  return step2FormData.mediaList
    .filter((item) => isImage(item))
    .map((item) => baseUrl + (item.nasMediaUrl || item.shopifyMediaUrl));
});

// 采购商品选项
/** @type {ProductOption[]} */
const optionList = ref([]);

const step1Variants = ref([
  {
    variantId: null,
    purchaseUrl: "",
    optionValues: "",
    optionValueList: [],
    position: 0,
    remark: "",
  },
]);

// 变体列表
/** @type {ProductVariant[]} */
const step2Variants = ref([
  {
    variantId: null,
    sku: "",
    price: null,
    compareAtPrice: null,
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
const step1DataSnapshot = ref(null);
const step2DataSnapshot = ref(null);

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
const draggedImage = ref(null);
const dragOverIndex = ref(-1); // 拖拽悬停索引，用于排序提示

// 富文本编辑器状态
const richTextEditor = reactive({
  expanded: false,
  mode: "edit", // 'edit' | 'preview'
});

// 视频播放相关状态
const videoModalVisible = ref(false);
const currentVideo = ref(null);

// 商品详情选项卡当前激活项，description 排在第一位
const activeDetailTab = ref("description");

// 监听采购链接 变化，同步到变体的采购链接
// 如果采购链接 有值，变体采购链接等于旧值, 则同步
watch(
  () => step1FormData.purchaseUrl,
  (newVal, oldVal) => {
    if (!step1Variants.value || step1Variants.value.length === 0) {
      return;
    }

    step1Variants.value.forEach((variant) => {
      // 如果变体的采购链接为空，或者等于旧的采购链接值，则同步新值
      if (!variant.purchaseUrl || variant.purchaseUrl === oldVal) {
        variant.purchaseUrl = newVal;
      }
    });
  },
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
        window.removeEventListener("keydown", handleSetp2Keydown);
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

// 组件挂载时加载缓存的汇率数据
onMounted(() => {
  exchangeRateStore.loadCachedRate();
});

// 处理来源 URL 失去焦点事件
function handleSourceUrlBlur() {
  if (step1FormData.sourceUrl && !step1FormData.purchaseUrl) {
    step1FormData.purchaseUrl = step1FormData.sourceUrl;
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
      handleSubmit("prev");
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
const open = async (selectedTagIds, productId, step = 0) => {
  await fetchTags();
  visible.value = true;

  if (productId == null) {
    title.value = "新增选品";

    resetForm(selectedTagIds, step);

    generateSpu(true);

    // 优化1：打开弹窗时，来源 URL 自动获得焦点
    nextTick(() => {
      setTimeout(() => {
        sourceUrlInputRef.value.focus();
      }, 50); // 增加延迟以等待 Dialog 动画完成
    });

    // 保存初始数据快照
    saveStep1DataSnapshot();
    return;
  } else {
    title.value = "编辑商品";

    resetForm(null, step);

    // 加载商品表单数据
    await handleLoadData(productId);
  }
};

// 加载商品表单数据
const handleLoadData = async (productId) => {
  // 加载商品详情
  const response = await getProduct(productId);
  const productData = response.data;

  // 填充第一步表单
  Object.assign(step1FormData, {
    productId: productData.productId,
    spu: productData.spu || null,
    category: productData.category || null,
    productType: productData.productType || null,
    sourceUrl: productData.sourceUrl || null,
    purchaseUrl: productData.purchaseUrl || null,
    tagIds: productData.tagIds || [],
  });

  // 填充第二步表单
  Object.assign(step2FormData, {
    productId: productData.productId,
    productTitle: productData.productTitle,
    category: productData.category,
    productType: productData.productType,
    description: productData.description,
    size: productData.size,
    material: productData.material,
    note: productData.note,
    packageInclude: productData.packageInclude,
    bodyHtml: productData.bodyHtml,
    mediaList: productData.mediaList || [],
    mainMediaId: productData.mainMediaId,
    remark: productData.remark,
    imageSearchKeyword: productData.imageSearchKeyword,
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
    const step1V = [];
    const step2V = [];
    productData.productVariantList.forEach((v, index) => {
      const optionValueList = v.optionValues ? JSON.parse(v.optionValues) : [];

      step1V.push({
        variantId: v.variantId,
        purchaseUrl: v.purchaseUrl,
        optionValues: v.optionValues,
        optionValueList,
        position: v.position,
        remark: v.remark,
      });

      // 检查并填充汇率
      const variant = {
        ...v,
        sku:
          v.sku ||
          productData.spu + "-" + (index + 1).toString().padStart(3, "0"),
        optionValueList: v.optionValues ? JSON.parse(v.optionValues) : [],
      };

      // 如果exchangeRate为空，自动填入缓存的汇率
      if (!variant.exchangeRate) {
        fillExchangeRate(variant);
      }

      step2V.push(variant);
      step1Variants.value = step1V;
      step2Variants.value = step2V;
    });
  }

  // 保存初始数据快照
  saveStep1DataSnapshot();
  saveStep2DataSnapshot();
};

// 保存初始数据快照
function saveStep1DataSnapshot() {
  step1DataSnapshot.value = {
    step1FormData: JSON.parse(JSON.stringify(step1FormData)),
    optionList: JSON.parse(JSON.stringify(optionList.value)),
    step1Variants: JSON.parse(JSON.stringify(step1Variants.value)),
  };
  console.log("保存步骤1初始数据快照:", step1DataSnapshot.value);
}
function saveStep2DataSnapshot() {
  step2DataSnapshot.value = {
    step2FormData: JSON.parse(JSON.stringify(step2FormData)),
    step2Variants: JSON.parse(JSON.stringify(step2Variants.value)),
  };
  console.log("保存步骤2初始数据快照:", step2DataSnapshot.value);
}

// 比较数据是否发生变化
function hasStep1DataChanged() {
  if (!step1DataSnapshot.value) {
    return true; // 没有初始快照，默认为有变化
  }

  const currentData = {
    step1FormData: step1FormData,
    optionList: optionList.value,
    step1Variants: step1Variants.value,
  };
  console.log("当前数据:", currentData);

  // 比较数据是否相同
  return (
    JSON.stringify(currentData) !== JSON.stringify(step1DataSnapshot.value)
  );
}
// 比较数据是否发生变化
function hasStep2DataChanged() {
  if (!step2DataSnapshot.value) {
    return true; // 没有初始快照，默认为有变化
  }

  const currentData = {
    step2FormData: step2FormData,
    step2Variants: step2Variants.value,
  };
  console.log("当前数据:", currentData);

  // 比较数据是否相同
  return (
    JSON.stringify(currentData) !== JSON.stringify(step2DataSnapshot.value)
  );
}

// 重置表单
function resetForm(selectedTagIds, step) {
  title.value = "新增商品";
  activeStep.value = step;

  // 重置第一步表单数据
  Object.assign(step1FormData, {
    productId: null,
    spu: "",
    category: "",
    productType: "",
    sourceUrl: "",
    purchaseUrl: "",
    tagIds: selectedTagIds || [],
  });

  // 重置第二步表单数据
  Object.assign(step2FormData, {
    productId: null,
    productTitle: "",
    note: "",
    packageInclude: "",
    bodyHtml: "",
    size: "",
    material: "",
    mediaList: [],
    mainMediaId: null,
    remark: "",
    description: "",
    imageSearchKeyword: "",
  });

  optionList.value = [];
  step1Variants.value = [
    {
      variantId: null,
      purchaseUrl: "",
      optionValues: "",
      optionValueList: [],
      position: 0,
      remark: "",
    },
  ];
  step2Variants.value = [
    {
      variantId: null,
      sku: "",
      price: null,
      compareAtPrice: null,
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
    const selectedMenuTags = getMenuTag(step1FormData.tagIds);

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
    step1FormData.spu = `${selectedMenuTag.spuPrefix}${paddedSeq}`;

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
    step1Variants.value = [
      {
        variantId: null,
        purchaseUrl: step1FormData.purchaseUrl,
        optionValues: "",
        optionValueList: [],
        position: 0,
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
  step1Variants.value = combinations.map((combo, index) => {
    const existingVariant = step1Variants.value[index];
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
      purchaseUrl: existingVariant?.purchaseUrl || step1FormData.purchaseUrl,
      optionValues: JSON.stringify(optionValueList),
      optionValueList: optionValueList,
      position: existingVariant?.position || index,
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
  if (step1Variants.value.length > 1) {
    step1Variants.value.splice(index, 1);
  }
}

// 变体行拖拽排序
function handleRowDrop({ row, $index }, targetIndex) {
  const sourceIndex = s.value.findIndex((v) => v.position === row.position);
  if (sourceIndex !== -1 && sourceIndex !== targetIndex) {
    const item = step2Variants.value.splice(sourceIndex, 1)[0];
    step2Variants.value.splice(targetIndex, 0, item);
  }
}

// 计算变体成本
async function calculateVariantCost(row) {
  try {
    if (row.purchasePrice == null || row.freight == null) {
      row.unitCostPrice = 0;
      return;
    }
    row.unitCostPrice = (row.purchasePrice || 0) + (row.freight || 0);

    // 如果exchangeRate为空，自动填入缓存的汇率
    if (!row.exchangeRate) {
      await fillExchangeRate(row);
    }

    // 自动计算推荐销售价格
    // 成本价基础上加30%作为销售价 向上取整
    if (row.exchangeRate) {
      row.suggestedPrice = Math.ceil(
        row.unitCostPrice / 0.7 / row.exchangeRate,
      );
      // 自动计算推荐销售价格
      if (row.price == null) {
        row.price = row.suggestedPrice;
      }
      // 计算利润率
      calculateProfitRate(row);
    }
  } catch (error) {
    console.error("计算变体成本失败:", error);
    proxy.$modal.msgError("计算变体成本失败，请检查输入数据");
  }
}

// 计算利润率
function calculateProfitRate(row) {
  try {
    if (row.unitCostPrice == null || row.price == null) {
      row.profitRate = null;
      return;
    }
    const rmbPrice = row.price * row.exchangeRate;
    // 保留两位小数
    row.profitRate = parseFloat(
      (((rmbPrice - row.unitCostPrice) / rmbPrice) * 100).toFixed(2),
    );
  } catch (error) {
    console.error("计算利润率失败:", error);
    proxy.$modal.msgError("计算利润率失败，请检查输入数据");
  }
}

// 计算材积重
async function calculateMaterialWeight(row) {
  try {
    // 检查 row 是否存在
    if (!row) {
      console.error("计算材积重失败: 缺少必要参数");
      return;
    }

    const { pkLength, pkWidth, pkHeight } = row;

    // 检查尺寸是否存在且为有效数字
    if (pkLength && pkWidth && pkHeight) {
      const dims = [Number(pkLength), Number(pkWidth), Number(pkHeight)];
      if (dims.every((d) => !isNaN(d) && d > 0)) {
        row.materialWeight = (dims[0] * dims[1] * dims[2]) / 8000;
      } else {
        row.materialWeight = 0;
        console.log("请检查输入数据，尺寸必须为正数");
      }
    } else {
      // 如果尺寸不完整，设置材积重为0
      row.materialWeight = 0;
    }
    // 计算运费
    await calculateFreight(row);
  } catch (error) {
    console.error("计算材积重失败:", error);
    proxy.$modal.msgError("计算材积重失败，请检查尺寸格式");
  }
}

// 计算运费
async function calculateFreight(row) {
  try {
    console.log("计算运费:", row);
    if (
      row.isActualShipment === "0" &&
      row.pkWeight > 0 &&
      row.materialWeight > 0
    ) {
      const param = {
        pkWidth: row.pkWidth,
        pkHeight: row.pkHeight,
        pkLength: row.pkLength,
        materialWeight: row.materialWeight,
        pkWeight: row.pkWeight,
      };
      const response = await calculateShipping(param);
      row.freight = response.data || 0;
      await calculateVariantCost(row);
    }
  } catch (error) {
    console.error("计算运费失败", error);
    proxy.$modal.msgError("计算运费失败，请稍后重试");
  }
}

// 填充汇率数据
async function fillExchangeRate(row) {
  try {
    // 检查并更新汇率
    const usdRate = await exchangeRateStore.checkAndUpdateRate();
    if (usdRate) {
      row.exchangeRate = usdRate;
    }
  } catch (error) {
    console.error("填充汇率失败:", error);
  }
}

const tableRowClassName = ({ row, rowIndex }) => {
  if (row.optionValues && row.optionValues.toUpperCase().indexOf("色") !== -1) {
    return "set-row";
  }
  return "";
};

// 加载服务器图片（直接添加到列表）
async function loadServerImages() {
  if (!step2FormData.imageSearchKeyword) {
    proxy.$modal.msgError("请输入搜索关键词");
    return;
  }
  imageLoading.value = true;
  try {
    const response = await scanMedia({
      dirPath: step2FormData.imageSearchKeyword,
      productId: step2FormData.productId,
    });

    const serverImages = response.data || [];

    if (serverImages.length === 0) {
      proxy.$modal.msgWarning("未找到图片");
      return;
    }
    step2FormData.mediaList = serverImages;
    proxy.$modal.msgSuccess(`已导入 ${serverImages.length} 张图片/视频`);
  } catch (error) {
    console.error("加载服务器图片失败", error);
    // proxy.$modal.msgError("加载服务器图片失败");
  } finally {
    imageLoading.value = false;
  }
}

// 移除图片
function removeImage(index) {
  step2FormData.mediaList.splice(index, 1);
}

// 变体图片拖拽开始
function handleVariantImageDragStart(event, row) {
  event.dataTransfer.effectAllowed = "move";
  if (row.media) {
    // 保存被拖拽的媒体对象
    event.dataTransfer.setData("variant-media", JSON.stringify(row.media));
    // 保存当前行数据到全局变量，用于删除操作
    draggedVariantRow.value = row;
  }
  // 添加全局dragend事件监听器
  document.addEventListener("dragend", handleVariantImageDragEnd);
}

// 变体图片拖拽进入
function handleVariantImageDragEnter(event, row) {
  event.preventDefault();
  dragOverVariant.value = row;
}

// 变体图片拖拽离开
function handleVariantImageDragLeave(event) {
  event.preventDefault();
  dragOverVariant.value = null;
}

// 变体图片拖拽结束
function handleVariantImageDragEnd(event) {
  // 检查是否拖拽出了变体图片区域
  const variantImageItems = document.querySelectorAll(".variant-image-item");
  let isInside = false;

  // 检查鼠标是否在任何变体图片区域内
  const mouseX = event.clientX;
  const mouseY = event.clientY;

  variantImageItems.forEach((item) => {
    const rect = item.getBoundingClientRect();
    if (
      mouseX >= rect.left &&
      mouseX <= rect.right &&
      mouseY >= rect.top &&
      mouseY <= rect.bottom
    ) {
      isInside = true;
    }
  });

  // 如果拖拽出了区域，显示删除确认对话框
  if (!isInside && draggedVariantRow.value) {
    try {
      ElMessageBox.confirm("确定要取消绑定这张规格图吗？", "确认取消绑定", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          // 找到对应的行并删除图片
          console.log(draggedVariantRow.value);
          step2Variants.value.forEach((row) => {
            if (row.variantId === draggedVariantRow.value.variantId) {
              row.media = null;
              row.mediaId = null;
            }
          });
        })
        .catch(() => {
          // 取消删除
        })
        .finally(() => {
          // 清空拖拽状态
          draggedVariantRow.value = null;
        });
    } catch (error) {
      console.error("处理拖拽删除时出错:", error);
      // 清空拖拽状态
      draggedVariantRow.value = null;
    }
  } else {
    // 清空拖拽状态
    draggedVariantRow.value = null;
  }

  // 清空拖拽状态
  dragOverVariant.value = null;

  // 移除全局事件监听器
  document.removeEventListener("dragend", handleVariantImageDragEnd);
}

// 判断是否为图片
function isImage(media) {
  if (!media) return false;
  const contentType = media.mediaContentType || "";
  return (
    contentType === "IMAGE" ||
    /\.(jpg|jpeg|png|gif|webp|svg)$/i.test(media.filename || "")
  );
}

// 判断是否为视频
function isVideo(media) {
  if (!media) return false;
  const contentType = media.mediaContentType || "";
  return (
    contentType === "VIDEO" ||
    /\.(mp4|webm|ogg|mov|avi)$/i.test(media.filename || "")
  );
}

// 获取媒体类型标签
function getMediaTypeLabel(media) {
  if (isVideo(media)) return "视频";
  if (isImage(media)) return "图片";
  return "文件";
}

// 获取视频缩略图
function getVideoThumbnail(media) {
  // 这里可以返回视频的第一帧作为缩略图
  // 暂时使用一个默认的视频缩略图
  return "https://via.placeholder.com/200x150/3498db/ffffff?text=Video";
}

// 打开视频播放弹框
function openVideoModal(media) {
  currentVideo.value = media;
  videoModalVisible.value = true;
}

// 图片拖拽开始
function handleImageDragStart(event, media) {
  console.log("handleImageDragStart", media);
  draggedImage.value = media;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("media-id", media.mediaId);
  event.dataTransfer.setData(
    "media-index",
    step2FormData.mediaList.indexOf(media),
  );
  // 添加全局dragend事件监听器
  document.addEventListener("dragend", handleImageDragEnd);
}

// 图片拖拽进入
function handleImageDragEnter(event, index) {
  console.log("handleImageDragEnter", index);
  event.preventDefault();
  dragOverIndex.value = index;
}

// 图片拖拽离开
function handleImageDragLeave(event) {
  console.log("handleImageDragLeave");
  event.preventDefault();

  // 检查鼠标是否真的离开了图片项，还是只是进入了子元素
  // const target = event.currentTarget;
  // const relatedTarget = event.relatedTarget;
  // // 如果relatedTarget是target的子元素，则不重置dragOverIndex
  // if (
  //   !relatedTarget ||
  //   (target !== relatedTarget && !target.contains(relatedTarget))
}

// 图片拖拽结束
function handleImageDragEnd(event) {
  // 检查是否拖拽出了图片列表区域
  const imageGrid = document.querySelector(".image-grid");
  let isInside = false;

  if (imageGrid) {
    const rect = imageGrid.getBoundingClientRect();
    const mouseX = event.clientX;
    const mouseY = event.clientY;
    if (
      mouseX >= rect.left &&
      mouseX <= rect.right &&
      mouseY >= rect.top &&
      mouseY <= rect.bottom
    ) {
      isInside = true;
    }
  }

  // 如果拖拽出了区域，显示删除确认对话框
  if (!isInside && draggedImage.value) {
    try {
      ElMessageBox.confirm("确定要删除这张图片吗？", "删除确认", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          // 找到对应的图片并删除
          const index = step2FormData.mediaList.indexOf(draggedImage.value);
          if (index > -1) {
            step2FormData.mediaList.splice(index, 1);
          }
        })
        .catch(() => {
          // 取消删除
        })
        .finally(() => {
          // 清空拖拽状态
          draggedImage.value = null;
          dragOverIndex.value = -1;
        });
    } catch (error) {
      console.error("处理图片拖拽删除时出错:", error);
      // 清空拖拽状态
      draggedImage.value = null;
      dragOverIndex.value = -1;
    }
  } else {
    // 清空拖拽状态
    draggedImage.value = null;
    dragOverIndex.value = -1;
  }

  // 移除全局事件监听器
  document.removeEventListener("dragend", handleImageDragEnd);
}

// 图片拖拽放下（排序）
function handleImageDrop(event) {
  console.log("handleImageDrop");
  event.preventDefault();

  // 处理内部拖拽排序
  if (draggedImage.value) {
    const fromIndex = step2FormData.mediaList.indexOf(draggedImage.value);
    const toIndex = dragOverIndex.value;

    if (fromIndex >= 0 && toIndex >= 0 && fromIndex !== toIndex) {
      // 重新排序
      const item = step2FormData.mediaList.splice(fromIndex, 1)[0];
      step2FormData.mediaList.splice(toIndex, 0, item);
    }
  }

  dragOverIndex.value = -1;
  draggedImage.value = null;
}

// 图片拖拽到变体
function handleVariantImageDrop(event, row) {
  console.log("handleVariantImageDrop", row);
  event.preventDefault();

  // 优先从dataTransfer获取数据（从变体图片拖拽）
  try {
    const mediaData = event.dataTransfer.getData("variant-media");
    if (mediaData) {
      const media = JSON.parse(mediaData);
      if (media) {
        row.media = media;
        row.mediaId = media.mediaId;
        return;
      }
    }
  } catch (error) {
    console.error("处理变体图片拖拽放下时出错:", error);
  }

  // 从draggedImage获取数据（从图片列表拖拽）
  if (draggedImage.value) {
    row.media = draggedImage.value;
    row.mediaId = draggedImage.value.mediaId;
    draggedImage.value = null;
  }

  // 清空拖拽状态
  dragOverVariant.value = null;
}

// 提交表单continue、next、close
async function handleSubmit(action) {
  try {
    // 根据当前步骤只校验对应的表单
    if (activeStep.value === 0) {
      await step1FormRef.value?.validate();
    } else if (activeStep.value === 1) {
      // 第二步自定义校验：变体选项必填
      // for (let i = 0; i < variants.value.length; i++) {
      //   const variant = variants.value[i];
      //   if (variant.optionValueList && variant.optionValueList.length > 0) {
      //     for (let j = 0; j < variant.optionValueList.length; j++) {
      //       const opt = variant.optionValueList[j];
      //       if (!opt.purchaseName || !opt.purchaseValue) {
      //         proxy.$modal.msgError(
      //           `第 ${i + 1} 个变体的第 ${j + 1} 个选项名称和值不能为空`,
      //         );
      //         return;
      //       }
      //     }
      //   }
      // }
      await step2FormRef.value?.validate();
    }
    loading.value = true;

    let hasChanged = true;
    if (activeStep.value === 0) {
      hasChanged = hasStep1DataChanged();
      if (hasStep1DataChanged()) {
        // 保存第一步数据
        const submitData = {
          ...step1FormData,
          optionJson: JSON.stringify(optionList.value),
          productVariantList: step1Variants.value,
        };

        const response = await addSelectionInfo(submitData);
        // 重新加载商品表单数据
        await handleLoadData(response.data);
      }
    } else {
      // 保存第二步数据
      hasChanged = hasStep2DataChanged();
      if (hasChanged) {
        // 确保变体中的 optionValueList 同步更新到 optionValues 字符串（如果需要后端兼容）
        const variantsToSubmit = step2Variants.value.map((v) => ({
          ...v,
          optionValues: JSON.stringify(v.optionValueList),
        }));

        const submitData = {
          ...step2FormData,
          productId: step1FormData.productId, // 确保productId一致
          optionJson: JSON.stringify(optionList.value),
          productVariantList: variantsToSubmit,
          //mediaList: step2FormData.mediaList, // 使用 mediaList
        };
        await updateBaseInfo(submitData);
      }
    }

    emit("submit", { action, hasChanged });

    if (action === "close") {
      visible.value = false;
    } else if (action === "continue") {
      resetForm(step1FormData.tagIds);
      activeStep.value = 0;
    } else if (action === "next") {
      // next
      activeStep.value = 1;
    } else {
      // prev
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
  pointer-events: none;
}

.image-overlay .el-button {
  pointer-events: auto;
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

.variant-image-item {
  position: relative;
  width: 80px;
  height: 60px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
}

.variant-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.variant-image-item:hover .variant-thumb {
  transform: scale(1.1);
}

/* 当拖拽经过时高亮 */
.variant-image-item.drag-over {
  border-color: #409eff;
  border-style: dashed;
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
  min-height: 100px;
  padding: 10px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  margin-top: 16px;
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

/* 商品详情选项卡样式 */
.detail-tabs {
  width: 100%;
}

.detail-tabs .el-tabs__header {
  margin-bottom: 0;
}

.detail-tabs .el-tabs__content {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-top: none;
  border-radius: 0 0 8px 8px;
}

.detail-tabs .el-tab-pane {
  min-height: 120px;
}

.detail-tabs .el-textarea__inner {
  border-radius: 4px;
  resize: vertical;
}

/* 媒体类型样式 */
.image-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.image-item.drag-over {
  border: 2px dashed #409eff;
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.image-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  pointer-events: none;
}

.file-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  padding: 8px;
}

.file-placeholder .el-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.file-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.media-type-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  padding: 2px 6px;
  background: rgba(64, 158, 255, 0.9);
  color: #fff;
  font-size: 10px;
  border-radius: 3px;
  pointer-events: none;
}

/* 视频缩略图样式 */
.video-thumbnail {
  position: relative;
  cursor: pointer;
  transition: transform 0.2s ease;
  width: 100%;
  height: 100%;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-thumbnail:hover {
  transform: scale(1.05);
}

.video-play-button {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background-color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  transition: all 0.2s ease;
}

.video-thumbnail:hover .video-play-button {
  background-color: rgba(0, 0, 0, 0.8);
  transform: translate(-50%, -50%) scale(1.1);
}

:deep(.el-table .set-row) {
  --el-table-tr-bg-color: var(--el-color-primary-light-9) !important;
}
</style>
