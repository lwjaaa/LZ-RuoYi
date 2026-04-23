<template>
  <div class="product-wizard-container">
    <div class="product-wizard-shell">
      <!-- 页面头部 -->
      <el-page-header @back="handleBack" class="wizard-header">
        <template #content>
          <div class="header-content">
            <div class="header-title-group">
              <span class="page-title">商品创建工作台</span>
              <span class="page-subtitle">
                以标准化流程完成商品建档、资料补全与变体成本配置
              </span>
            </div>
            <div class="header-status">
              <span class="status-pill">ERP Product Studio</span>
              <span class="status-pill status-pill--active">
                {{
                  activeStep === 0
                    ? "Step 1 / 2 基础选品"
                    : "Step 2 / 2 信息录入"
                }}
              </span>
            </div>
          </div>
        </template>
        <template #extra>
          <div class="right-buttons">
            <el-tooltip
              v-if="activeStep === 1"
              content="快捷键: Ctrl+←"
              placement="top"
            >
              <el-button @click="handlePrev()" class="header-action-btn">
                上一步
              </el-button>
            </el-tooltip>
            <el-tooltip
              v-if="activeStep === 0"
              content="快捷键: Ctrl+Enter"
              placement="top"
            >
              <el-button
                type="primary"
                @click="handleContinue()"
                :loading="loading"
                class="header-action-btn header-action-btn--ghost"
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
                @click="handleNext()"
                :loading="loading"
                class="header-action-btn"
              >
                下一步
              </el-button>
            </el-tooltip>
            <el-tooltip content="快捷键: Ctrl+S" placement="top">
              <el-button
                type="success"
                @click="handleSave()"
                :loading="loading"
                class="header-action-btn header-action-btn--strong"
              >
                保存
              </el-button>
            </el-tooltip>
          </div>
        </template>
      </el-page-header>
      <div class="wizard-progress-card">
        <div class="wizard-progress-copy">
          <div class="wizard-progress-copy__eyebrow">Workflow</div>
          <div class="wizard-progress-copy__title">商品创建双阶段协作流程</div>
          <div class="wizard-progress-copy__desc">
            先完成选品与规格建模，再统一处理媒体、详情和变体定价，减少重复录入与信息遗漏。
          </div>
        </div>
        <el-steps
          :active="activeStep"
          finish-status="success"
          align-center
          class="wizard-steps"
        >
          <el-step title="选品基础信息" />
          <el-step title="信息录入" />
        </el-steps>
      </div>

      <!-- 第一步：选品基础信息 -->
      <div v-show="activeStep === 0" class="step-content">
        <el-form
          ref="step1FormRef"
          :model="step1FormData"
          :rules="step1Rules"
          label-width="120px"
          class="step-form step1-form"
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
                  @blur="generateSpu(true)"
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
              <el-form-item label="商品名称" prop="productName">
                <el-input
                  v-model="step1FormData.productName"
                  placeholder="请输入商品名称（中文）"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
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
          </el-row>

          <el-row :gutter="20">
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

          <!-- 商品选项 -->
          <el-form-item label="商品选项" class="form-item-block">
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
                        v-model="option.chineseName"
                        placeholder="采购选项名称"
                        class="input-left"
                        size="small"
                        @keydown.tab.prevent="
                          handleTabKey($event, optIndex, null, 'purchase')
                        "
                      />
                      <el-input
                        v-model="option.englishName"
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
                        v-model="value.chineseValue"
                        placeholder="采购选项值"
                        class="input-left"
                        size="small"
                        @keydown.tab.prevent="
                          handleTabKey($event, optIndex, valIndex, 'purchase')
                        "
                      />
                      <el-input
                        v-model="value.englishValue"
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
                      <strong
                        >{{ option.chineseName || "采购选项" }} &nbsp</strong
                      >
                      <span v-if="option.englishName" class="en-name"
                        >[{{ option.englishName || "商品选项" }}]</span
                      >
                    </span>
                    <div class="option-values-display">
                      <template
                        v-for="(value, idx) in option.values"
                        :key="idx"
                      >
                        <el-tag
                          v-if="value.chineseValue || value.englishValue"
                          size="small"
                          class="value-tag"
                        >
                          {{ value.chineseValue || "-" }}&nbsp
                          <span v-if="value.englishValue" class="en-value"
                            >[{{ value.englishValue || "-" }}]</span
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
          <el-form-item label="变体分录" class="form-item-block">
            <el-table
              :data="step1Variants"
              border
              style="width: 100%"
              row-key="variantId"
              @row-drop="handleRowDrop"
              class="variant-table variant-table--selection"
            >
              <!-- 动态生成选项列 -->
              <template v-if="getActiveOptions().length > 0">
                <el-table-column
                  v-for="(opt, idx) in getActiveOptions()"
                  :key="opt.optionId || idx"
                  :label="opt.chineseName || '选项'"
                  min-width="150"
                  align="center"
                >
                  <template #default="{ row }">
                    <!-- <span>{{
                    `${row.optionValueList[idx]?.chineseName}[${row.optionValueList[idx]?.optionValue}]` ||
                    "-"
                  }}</span> -->

                    <span>
                      {{ row.optionValueList[idx]?.chineseValue || "-" }}
                      <span v-if="row.optionValueList[idx]?.englishValue">
                        [{{ row.optionValueList[idx]?.englishValue }}]
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
              >
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
              <el-table-column label="是否可用" width="100" align="center">
                <template #default="{ row }">
                  <el-switch
                    v-model="row.isActiveAvailable"
                    :active-value="'1'"
                    :inactive-value="'0'"
                    active-text="是"
                    inactive-text="否"
                    size="small"
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
          class="step-form step2-form"
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
          <el-form-item
            label="商品详情"
            prop="detailTabs"
            class="form-item-block"
          >
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
                      <el-radio-group
                        v-model="richTextEditor.mode"
                        size="small"
                      >
                        <el-radio-button value="edit"
                          >HTML 输入</el-radio-button
                        >
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
          <el-divider
            content-position="left"
            class="media-section-divider"
            :class="{ 'is-hidden': isMediaFloating }"
            >商品媒体文件管理</el-divider
          >
          <el-form-item class="form-item-block media-form-item">
            <!-- 媒体面板（统一元素，通过CSS切换悬浮/内嵌） -->
            <div
              class="media-panel"
              :class="{ 'is-floating': isMediaFloating }"
              ref="mediaPanelRef"
              :style="
                isMediaFloating
                  ? {
                      width: mediaPanelWidth + 'px',
                      height: mediaPanelHeight + 'px',
                      left: mediaPanelLeft + 'px',
                      top: mediaPanelTop + 'px',
                    }
                  : {}
              "
            >
              <div
                class="media-panel-header"
                :class="{ 'is-draggable': isMediaFloating }"
                @mousedown="
                  isMediaFloating ? startDragMediaPanel($event) : null
                "
              >
                <span class="media-panel-title">媒体文件</span>
                <div class="media-panel-toolbar">
                  <el-input
                    v-model="step2FormData.imageSearchKeyword"
                    placeholder="输入媒体文件所在目录搜索"
                    :style="{ width: isMediaFloating ? '180px' : '240px' }"
                    :size="isMediaFloating ? 'small' : 'default'"
                    clearable
                    @keyup.enter="loadServerImages"
                  />
                  <el-button
                    type="primary"
                    icon="Upload"
                    @click="loadServerImages"
                    :loading="imageLoading"
                    :size="isMediaFloating ? 'small' : 'default'"
                  >
                    {{ isMediaFloating ? "导入" : "从服务器导入" }}
                  </el-button>
                  <el-button
                    v-if="!isMediaFloating"
                    type="default"
                    icon="Rank"
                    @click="toggleMediaFloating"
                    size="default"
                    title="悬浮媒体面板"
                  >
                    悬浮
                  </el-button>
                  <el-button
                    v-else
                    type="danger"
                    size="small"
                    circle
                    icon="Close"
                    @click="toggleMediaFloating"
                    title="收起悬浮面板"
                  />
                </div>
              </div>
              <div class="media-panel-content">
                <div
                  class="image-grid"
                  :class="{ 'is-floating-grid': isMediaFloating }"
                  @dragover.prevent
                  @drop="handleImageDrop($event)"
                >
                  <div
                    v-for="(media, index) in step2FormData.mediaList"
                    :key="'media-' + (media.mediaId || index)"
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
                            baseUrl +
                              (media.nasMediaUrl || media.shopifyMediaUrl),
                          )
                        "
                        lazy
                        show-progress
                        preview-teleported
                        fit="cover"
                      />
                    </template>
                    <template v-else-if="isVideo(media)">
                      <div
                        class="video-thumbnail"
                        @click="openVideoModal(media)"
                      >
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
                    <span class="placeholder-text">{{
                      isMediaFloating
                        ? '点击"导入"加载媒体'
                        : '点击"从服务器导入"'
                    }}</span>
                  </div>
                </div>
              </div>
              <div
                v-if="isMediaFloating"
                class="media-panel-resize"
                @mousedown="startResizeMediaPanel"
              ></div>
            </div>
          </el-form-item>

          <!-- 变体详细设置 -->
          <el-divider content-position="left">变体详细设置</el-divider>

          <el-row :gutter="10" class="mb8">
            <right-toolbar
              :showSearch="false"
              :showrRefresh="false"
              :search="false"
              :columns="columns"
            ></right-toolbar>
          </el-row>

          <el-table
            :data="step2Variants"
            border
            style="width: 100%"
            :loading="loading"
            row-key="variantId"
            :row-class-name="tableRowClassName"
            class="variant-table variant-table--detail"
          >
            <!-- 动态生成选项列 -->
            <template v-if="columns[0].visible">
              <template v-if="getActiveOptions().length > 0">
                <el-table-column
                  v-for="(opt, idx) in getActiveOptions()"
                  :key="opt.optionId || idx"
                  :label="opt.chineseName || '选项'"
                  width="120"
                  align="center"
                  fixed="left"
                >
                  <template #default="{ row }">
                    <span>
                      {{ row.optionValueList[idx]?.chineseValue || "-" }}
                      <span v-if="row.optionValueList[idx]?.englishValue">
                        [{{ row.optionValueList[idx]?.englishValue }}]
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
            </template>

            <el-table-column
              label="SKU"
              width="150"
              fixed="left"
              v-if="columns[1].visible"
            >
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

            <el-table-column
              label="采购价"
              align="center"
              width="110"
              v-if="columns[2].visible"
            >
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
            <el-table-column
              label="包装长度"
              align="center"
              width="90"
              v-if="columns[3].visible"
            >
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
            <el-table-column
              label="包装宽度"
              align="center"
              width="90"
              v-if="columns[3].visible"
            >
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
            <el-table-column
              label="包装高度"
              align="center"
              width="90"
              v-if="columns[3].visible"
            >
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
            <el-table-column
              label="实重"
              align="center"
              width="94"
              v-if="columns[4].visible"
            >
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
            <el-table-column
              label="材积重"
              align="right"
              width="80"
              v-if="columns[5].visible"
            >
              <template #header>
                <el-tooltip
                  content="材积重 = (L * W * H) / 8000"
                  placement="top"
                >
                  <span>材积重</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <span>{{ row.materialWeight || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column
              label="实际发货"
              align="center"
              width="80"
              v-if="columns[6].visible"
            >
              <template #default="{ row }">
                <el-switch
                  v-model="row.isActualShipment"
                  :active-value="'1'"
                  :inactive-value="'0'"
                  @change="calculateVariantCost(row)"
                />
              </template>
            </el-table-column>
            <el-table-column
              label="运费"
              align="center"
              width="110"
              v-if="columns[7].visible"
            >
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
            <el-table-column
              label="成本价"
              align="center"
              width="110"
              v-if="columns[8].visible"
            >
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
            <el-table-column
              label="汇率"
              align="center"
              width="110"
              v-if="columns[9].visible"
            >
              <template #header>
                <el-tooltip content="首次默认填充今日美国汇率" placement="top">
                  <span>汇率</span>
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
            <el-table-column
              label="建议售价"
              align="center"
              width="110"
              v-if="columns[10].visible"
            >
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
            <!-- 利润率 -->
            <el-table-column
              label="利润"
              align="center"
              width="110"
              v-if="columns[11].visible"
            >
              <template #header>
                <el-tooltip placement="top">
                  <template #content>
                    单位：元<br />实际利润 = 售价 * 汇率 - 成本价</template
                  >
                  <span>利润</span>
                </el-tooltip>
              </template>
              <template #default="{ row }">
                <span>{{ row.profit || "" }}</span>
              </template>
            </el-table-column>
            <!-- 利润率 -->
            <el-table-column
              label="利润率"
              align="center"
              width="110"
              v-if="columns[12].visible"
            >
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
            <!-- 销售价格 -->
            <el-table-column
              label="售价"
              align="center"
              width="110"
              fixed="right"
              v-if="columns[13].visible"
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
            <!-- 对比价 -->
            <el-table-column
              label="对比价"
              align="center"
              width="110"
              fixed="right"
              v-if="columns[14].visible"
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
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  ref,
  reactive,
  computed,
  watch,
  onMounted,
  onUnmounted,
  getCurrentInstance,
  nextTick,
  type Ref,
  type ComputedRef,
} from "vue";
import {
  Picture,
  ArrowDown,
  Edit,
  VideoPlay,
  Document,
  Close,
  Rank,
} from "@element-plus/icons-vue";
import {
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadUserFile,
} from "element-plus";
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
import type {
  Product,
  ProductOption,
  ProductOptionValue,
  ProductVariant,
  TagDictMenu,
  Media,
} from "@/types/erp";

const { proxy } = getCurrentInstance() as any;

// 汇率状态管理
const exchangeRateStore = useExchangeRateStore();

const baseUrl = import.meta.env.VITE_APP_BASE_API;

// Emit
// 新增：返回处理
const emit = defineEmits(["submit", "back"]);

/**
 * 处理返回操作逻辑
 * 对接大厂 ERP 商品编辑规范：
 * 1. 存在未保存变更时，弹出包含三个选项（保存并返回、不保存并返回、取消）的二次确认弹窗
 * 2. 选择“保存并返回”时，等待后端保存成功后再回退；若失败则停留当前页并提示错误
 * 3. 选择“不保存并返回”时，直接重置 dirty 状态并回退
 * 4. 选择“取消”时，关闭弹窗保持现状
 * 5. 无变更时直接回退
 */
async function handleBack() {
  // 确认是否保存
  const hasChanged =
    activeStep.value === 0 ? hasStep1DataChanged() : hasStep2DataChanged();

  if (hasChanged) {
    try {
      // 弹出符合企业级设计规范的二次确认弹窗
      await ElMessageBox.confirm(
        "您有未保存的商品信息，是否立即保存后再离开？",
        "提示",
        {
          confirmButtonText: "保存并返回",
          cancelButtonText: "不保存并返回",
          distinguishCancelAndClose: true,
          type: "warning",
          closeOnClickModal: false,
          closeOnPressEscape: false,
        },
      );
      // 点击【保存并返回】
      const success = await handleSave();
      if (success) {
        emit("back");
      }
    } catch (action) {
      if (action === "cancel") {
        // 点击【不保存并返回】
        emit("back");
      } else {
        // 点击【取消】 (action === 'close')
        // 保持当前页面状态不变
        console.log("已取消返回操作，停留当前页");
      }
    }
  } else {
    // 无变更，直接返回
    emit("back");
  }
}

// 修改：关闭对话框改为触发返回事件
// function handleClose() {
//   emit("back");
// }

// 注册组件
const components = {
  CustomVideoModal,
};

// 状态
const visible = ref<boolean>(false);

const activeStep = ref<number>(0);
const loading = ref<boolean>(false);
const step1FormRef = ref<FormInstance>();
const step2FormRef = ref<FormInstance>();
const sourceUrlInputRef = ref<HTMLInputElement>(); // 来源 URL 输入框引用

// 拖拽相关状态
const draggedVariantRow = ref<ProductVariant | null>(null); // 存储拖拽的变体行数据
const dragOverVariant = ref<ProductVariant | null>(null); // 存储当前拖拽经过的变体

// 表单数据

interface Step1FormData {
  productId: number | undefined;
  spu: string;
  productName: string;
  sourceUrl: string;
  purchaseUrl: string;
  tagIds: number[];
}

interface Step2FormData {
  productId: number | undefined;
  spu: string;
  productTitle: string;
  category: string;
  productType: string;
  description: string;
  size: string;
  material: string;
  note: string;
  packageInclude: string;
  bodyHtml: string;
  mediaList: Media[];
  mainMediaId: number | undefined;
  remark: string;
  imageSearchKeyword: string;
}

/** 第一步表单数据 */
const step1FormData = reactive<Step1FormData>({
  productId: undefined,
  spu: "",
  productName: "",
  sourceUrl: "",
  purchaseUrl: "",
  tagIds: [],
});

/** 第二步表单数据 */
const step2FormData = reactive<Step2FormData>({
  productId: undefined,
  spu: "",
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
  mainMediaId: undefined,
  remark: "",
  imageSearchKeyword: "",
});

// 计算属性：所有图片的 URL 列表（不包含视频）
const imagePreviewList = computed<string[]>(() => {
  return step2FormData.mediaList
    .filter((item) => isImage(item))
    .map((item) => baseUrl + (item.nasMediaUrl || item.shopifyMediaUrl));
});

// 采购商品选项
const optionList = ref<ProductOption[]>([]);

interface Step1Variant {
  variantId: number | null;
  purchaseUrl?: string;
  optionValues?: string;
  optionValueList: ProductVariant["optionValueList"];
  position: number;
  remark: string;
  isActiveAvailable?: string;
}

const step1Variants = ref<Step1Variant[]>([
  {
    variantId: null,
    purchaseUrl: "",
    optionValues: "",
    optionValueList: [],
    position: 0,
    remark: "",
    isActiveAvailable: "1",
  },
]);

// 变体列表
const step2Variants = ref<ProductVariant[]>([
  {
    variantId: 0,
    productId: 0,
    sku: "",
    price: 0,
    compareAtPrice: undefined,
    optionValues: "",
    optionValueList: [],
    mediaId: undefined,
    position: 0,
    pkWidth: undefined,
    pkHeight: undefined,
    pkLength: undefined,
    materialWeight: undefined,
    pkWeight: undefined,
    freight: undefined,
    isActualShipment: "0",
    isActiveAvailable: "1",
    unitCostPrice: undefined,
    remark: "",
  },
]);

// 标签列表
const tagList = ref<TagDictMenu[]>([]);

interface Step1DataSnapshot {
  step1FormData: Step1FormData;
  optionList: ProductOption[];
  step1Variants: Step1Variant[];
}

interface Step2DataSnapshot {
  step2FormData: Step2FormData;
  step2Variants: ProductVariant[];
}

// 初始数据快照，用于检测数据变更
const step1DataSnapshot = ref<Step1DataSnapshot | null>(null);
const step2DataSnapshot = ref<Step2DataSnapshot | null>(null);

// SPU 生成相关
// 获取 MENU 类型的标签 ID
// 获取 MENU 类型的标签 ID（支持级联多选）
const getMenuTag = (tagIds: number[] | number[][]): TagDictMenu[] | null => {
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
const step1Rules: FormRules = {
  spu: [{ required: true, message: "SPU 不能为空", trigger: "blur" }],
  productName: [
    { required: true, message: "商品名称不能为空", trigger: "blur" },
    {
      min: 2,
      max: 100,
      message: "商品名称长度在 2 到 100 个字符",
      trigger: "blur",
    },
  ],
  tagIds: [{ required: true, message: "标签不能为空", trigger: "change" }],
  sourceUrl: [
    { required: true, message: "来源 URL 不能为空", trigger: "blur" },
    { type: "url", message: "请输入有效的 URL 地址", trigger: "blur" },
  ],
  purchaseUrl: [
    { type: "url", message: "请输入有效的 URL 地址", trigger: "blur" },
  ],
};

// 第二步校验规则
const step2Rules: FormRules = {};

// 图片加载状态
const imageLoading = ref<boolean>(false);
const draggedImage = ref<Media | null>(null);
const dragOverIndex = ref<number>(-1);

// 悬浮媒体面板状态
const isMediaFloating = ref<boolean>(false);
const mediaPanelWidth = ref<number>(520);
const mediaPanelHeight = ref<number>(420);
const mediaPanelLeft = ref<number>(100);
const mediaPanelTop = ref<number>(100);
const mediaPanelRef = ref<HTMLElement | null>(null);
let isDraggingMediaPanel = false;
let isResizingMediaPanel = false;
let mediaDragStartX = 0;
let mediaDragStartY = 0;
let mediaPanelStartLeft = 0;
let mediaPanelStartTop = 0;

const toggleMediaFloating = () => {
  isMediaFloating.value = !isMediaFloating.value;
  if (isMediaFloating.value) {
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    mediaPanelLeft.value = Math.max(
      20,
      Math.min(
        viewportWidth - mediaPanelWidth.value - 20,
        viewportWidth / 2 - mediaPanelWidth.value / 2,
      ),
    );
    mediaPanelTop.value = Math.max(
      20,
      Math.min(
        viewportHeight - mediaPanelHeight.value - 20,
        viewportHeight / 2 - mediaPanelHeight.value / 2,
      ),
    );
  }
};

const startDragMediaPanel = (e: MouseEvent) => {
  if (!mediaPanelRef.value) return;
  e.preventDefault();
  isDraggingMediaPanel = true;
  mediaDragStartX = e.clientX;
  mediaDragStartY = e.clientY;
  mediaPanelStartLeft = mediaPanelLeft.value;
  mediaPanelStartTop = mediaPanelTop.value;
  document.body.style.userSelect = "none";
  document.body.style.cursor = "move";
  document.addEventListener("mousemove", onDragMediaPanel);
  document.addEventListener("mouseup", stopDragMediaPanel);
};

const onDragMediaPanel = (e: MouseEvent) => {
  if (!isDraggingMediaPanel) return;
  const deltaX = e.clientX - mediaDragStartX;
  const deltaY = e.clientY - mediaDragStartY;
  const viewportWidth = window.innerWidth;
  const viewportHeight = window.innerHeight;
  mediaPanelLeft.value = Math.max(
    0,
    Math.min(
      viewportWidth - mediaPanelWidth.value,
      mediaPanelStartLeft + deltaX,
    ),
  );
  mediaPanelTop.value = Math.max(
    0,
    Math.min(
      viewportHeight - mediaPanelHeight.value,
      mediaPanelStartTop + deltaY,
    ),
  );
};

const stopDragMediaPanel = () => {
  isDraggingMediaPanel = false;
  document.body.style.userSelect = "";
  document.body.style.cursor = "";
  document.removeEventListener("mousemove", onDragMediaPanel);
  document.removeEventListener("mouseup", stopDragMediaPanel);
};

const startResizeMediaPanel = (e: MouseEvent) => {
  e.preventDefault();
  e.stopPropagation();
  isResizingMediaPanel = true;
  const startX = e.clientX;
  const startY = e.clientY;
  const startW = mediaPanelWidth.value;
  const startH = mediaPanelHeight.value;
  document.body.style.userSelect = "none";
  document.body.style.cursor = "se-resize";
  const onResize = (ev: MouseEvent) => {
    if (!isResizingMediaPanel) return;
    const deltaX = ev.clientX - startX;
    const deltaY = ev.clientY - startY;
    mediaPanelWidth.value = Math.max(
      360,
      Math.min(window.innerWidth - mediaPanelLeft.value - 20, startW + deltaX),
    );
    mediaPanelHeight.value = Math.max(
      280,
      Math.min(window.innerHeight - mediaPanelTop.value - 20, startH + deltaY),
    );
  };
  const stopResize = () => {
    isResizingMediaPanel = false;
    document.body.style.userSelect = "";
    document.body.style.cursor = "";
    document.removeEventListener("mousemove", onResize);
    document.removeEventListener("mouseup", stopResize);
  };
  document.addEventListener("mousemove", onResize);
  document.addEventListener("mouseup", stopResize);
};

// 富文本编辑器状态
interface RichTextEditorState {
  expanded: boolean;
  mode: "edit" | "preview";
}

const richTextEditor = reactive<RichTextEditorState>({
  expanded: false,
  mode: "edit", // 'edit' | 'preview'
});

// 视频播放相关状态
const videoModalVisible = ref<boolean>(false);
const currentVideo = ref<Media | null>(null);

// 商品详情选项卡当前激活项，description 排在第一位
const activeDetailTab = ref<string>("description");

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
        window.addEventListener("keydown", handleStep1Keydown);
        window.removeEventListener("keydown", handleStep2Keydown);
      } else if (activeStep.value === 1) {
        // 弹窗打开且在第二步时，挂载到 window
        window.addEventListener("keydown", handleStep2Keydown);
        window.removeEventListener("keydown", handleStep1Keydown);
      }
    } else {
      // 弹窗关闭或切换到其他步骤时，移除监听器
      window.removeEventListener("keydown", handleStep1Keydown);
      window.removeEventListener("keydown", handleStep2Keydown);
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
        window.addEventListener("keydown", handleStep1Keydown);
        window.removeEventListener("keydown", handleStep2Keydown);
      } else if (step === 1) {
        // 切换到其他步骤，移除监听器
        window.removeEventListener("keydown", handleStep1Keydown);
        window.addEventListener("keydown", handleStep2Keydown);
      }
    } else {
      // 弹窗关闭或切换到其他步骤时，移除监听器
      window.removeEventListener("keydown", handleStep1Keydown);
      window.removeEventListener("keydown", handleStep2Keydown);
    }
  },
);

// 组件卸载时清理监听器
onUnmounted(() => {
  window.removeEventListener("keydown", handleStep1Keydown);
  window.removeEventListener("keydown", handleStep2Keydown);
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
function handleStep1Keydown(event: KeyboardEvent): void {
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
    if (visible.value) {
      handleSaveStep1();
    }
  }

  // Ctrl + Enter: 继续选品
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleContinue();
    }
  }

  // Ctrl + 右键: 下一步
  if ((event.ctrlKey || event.metaKey) && event.key === "ArrowRight") {
    event.preventDefault();
    if (visible.value && activeStep.value === 0) {
      handleNext();
    }
  }
}

// 处理第二步的键盘事件
function handleStep2Keydown(event: KeyboardEvent): void {
  // Ctrl + 左键: 上一步
  if ((event.ctrlKey || event.metaKey) && event.key === "ArrowLeft") {
    event.preventDefault();
    if (visible.value && activeStep.value === 1) {
      handlePrev();
    }
  }

  // Ctrl + S: 保存并关闭
  if ((event.ctrlKey || event.metaKey) && event.key === "s") {
    event.preventDefault();
    if (visible.value) {
      handleSaveStep2();
    }
  }
}

const menuLoading = ref<boolean>(false);
// 扁平化的标签列表（缓存）
const menuTagList = ref<TagDictMenu[]>([]);
// 加载标签列表
async function fetchTags(): Promise<void> {
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

function flattenTagList(tagList: TagDictMenu[]): TagDictMenu[] {
  const result: TagDictMenu[] = [];
  function traverse(list: TagDictMenu[]): void {
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
const currentProductId = ref<number | null>(null);
const selectedTagIds = ref<number[]>([]);
// 打开编辑页面
const open = async (
  outsizeTagIds: number[] | null,
  productId: number | null,
  step: number = 0,
): Promise<void> => {
  currentProductId.value = productId;
  selectedTagIds.value = outsizeTagIds || [];

  // 重置第一步表单并加载数据
  await resetAndLoadData(step);

  activeStep.value = step;
  visible.value = true;
};

// 重置表单并加载数据
const resetAndLoadData = async (step = 0) => {
  if (step == 0) {
    await fetchTags();
    if (currentProductId.value == null) {
      resetForm(step);
      step1FormData.tagIds = selectedTagIds.value;
      generateSpu(true);
      // 优化1：打开弹窗时，来源 URL 自动获得焦点
      nextTick(() => {
        setTimeout(() => {
          sourceUrlInputRef.value?.focus();
        }, 50); // 增加延迟以等待 Dialog 动画完成
      });
      step1DataSnapshot.value = null;
      step2DataSnapshot.value = null;
    } else {
      resetForm(step);

      // 加载商品表单数据
      await handleLoadStep1Data();
    }
  } else {
    resetForm(step);
    // 加载商品表单数据
    await handleLoadStep2Data();
  }
};

// 加载商品表单数据
const handleLoadStep2Data = async (): Promise<void> => {
  // 加载商品详情
  if (!currentProductId.value) return;
  const response = await getProduct(currentProductId.value);
  const productData = response.data;

  // 填充第二步表单
  Object.assign(step2FormData, productData);

  // 解析采购商品选项
  if (productData.optionJson) {
    try {
      optionList.value = JSON.parse(productData.optionJson);
    } catch (e) {
      optionList.value = [];
    }
  }
  console.log("采购商品选项:", optionList.value);

  // 加载变体数据
  if (
    productData.productVariantList &&
    productData.productVariantList.length > 0
  ) {
    const step2V: ProductVariant[] = [];
    for (const v of productData.productVariantList) {
      const optionValueList = v.optionValues ? JSON.parse(v.optionValues) : [];
      const variant: ProductVariant = {
        ...v,
        sku:
          v.sku ||
          productData.spu +
            "-" +
            (productData.productVariantList.indexOf(v) + 1)
              .toString()
              .padStart(3, "0"),
        optionValueList,
      };
      if (!variant.exchangeRate) {
        await fillExchangeRate(variant);
      }
      step2V.push(variant);
    }
    step2Variants.value = step2V;
  }

  // 保存初始数据快照
  saveStep2DataSnapshot();
  step1DataSnapshot.value = null;
};

const handleLoadStep1Data = async (): Promise<void> => {
  // 加载商品详情
  if (!currentProductId.value) return;
  const response = await getProduct(currentProductId.value);
  const productData = response.data;

  // 填充第一步表单
  Object.assign(step1FormData, {
    productId: productData.productId,
    spu: productData.spu || null,
    sourceUrl: productData.sourceUrl || null,
    purchaseUrl: productData.purchaseUrl || null,
    tagIds: productData.tagIds || [],
  });

  // 解析采购商品选项
  if (productData.optionJson) {
    try {
      optionList.value = JSON.parse(productData.optionJson);
      // 默认收起
      optionList.value.forEach((option) => {
        option.collapsed = true;
      });
    } catch (e) {
      optionList.value = [];
    }
  }

  // 加载变体数据
  if (
    productData.productVariantList &&
    productData.productVariantList.length > 0
  ) {
    const step1V: Step1Variant[] = [];
    productData.productVariantList.forEach(async (v, index) => {
      const optionValueList = v.optionValues ? JSON.parse(v.optionValues) : [];
      step1V.push({
        variantId: v.variantId,
        purchaseUrl: v.purchaseUrl,
        optionValues: v.optionValues,
        optionValueList,
        position: v.position,
        remark: v.remark || "",
      });
      step1Variants.value = step1V;
    });
  }

  // 保存初始数据快照
  saveStep1DataSnapshot();
  step2DataSnapshot.value = null;
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
  const hasChanged =
    JSON.stringify(currentData) !== JSON.stringify(step1DataSnapshot.value);
  if (hasChanged) {
    console.log("步骤1数据有变化");
  }
  return hasChanged;
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
  const hasChanged =
    JSON.stringify(currentData) !== JSON.stringify(step2DataSnapshot.value);
  if (hasChanged) {
    console.log("步骤2数据有变化");
  }
  return hasChanged;
}

// 重置表单
function resetForm(step) {
  // 先重置表单字段
  if (step == 0) {
    step1FormRef.value?.resetFields();

    // 重置第一步表单数据
    Object.assign(step1FormData, {
      productId: null,
      spu: "",
      productName: "",
      category: "",
      productType: "",
      sourceUrl: "",
      purchaseUrl: "",
      tagIds: [],
    });

    step1Variants.value = [
      {
        variantId: null,
        purchaseUrl: "",
        optionValues: "",
        optionValueList: [],
        position: 0,
        remark: "",
        isActiveAvailable: "1",
      },
    ];
  } else {
    step2FormRef.value?.resetFields();
    imageLoading.value = false;
    // 重置第二步表单数据
    Object.assign(step2FormData, {
      productId: undefined,
      productTitle: "",
      note: "",
      packageInclude: "",
      bodyHtml: "",
      size: "",
      material: "",
      mediaList: [],
      mainMediaId: undefined,
      remark: "",
      description: "",
      imageSearchKeyword: "",
    });

    step2Variants.value = [
      {
        variantId: 0,
        productId: 0,
        sku: "",
        price: 0,
        compareAtPrice: undefined,
        optionValues: "",
        optionValueList: [],
        mediaId: undefined,
        position: 0,
        pkWidth: undefined,
        pkHeight: undefined,
        pkLength: undefined,
        materialWeight: undefined,
        pkWeight: undefined,
        freight: undefined,
        isActualShipment: "0",
        isActiveAvailable: "1",
        unitCostPrice: undefined,
        remark: "",
      },
    ];
  }

  optionList.value = [];
  // loading.value = false;
}

// 生成 SPU
const spuGenerating = ref(false); // ✅ 新增：SPU 生成中状态

const generateSpu = async (auto) => {
  // 防止重复点击
  if (spuGenerating.value) return;

  spuGenerating.value = true;

  try {
    const selectedMenuTags = getMenuTag(step1FormData.tagIds);
    console.log("选中的标签:", selectedMenuTags);

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
    if (!selectedMenuTag || selectedMenuTag.spuPrefix == null) {
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
    chineseValue: "",
    englishValue: "",
  });
}
function toggleCollapse(optIndex: number): void {
  optionList.value[optIndex].collapsed = !optionList.value[optIndex].collapsed;
  if (optionList.value[optIndex].collapsed) {
    generateVariants();
    // 收起后，确保焦点保持在当前选项行上
    nextTick(() => {
      const optionRows = document.querySelectorAll<HTMLElement>(".option-row");
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
function focusNextInput(
  optIndex: number,
  valIndex: number,
  type: string,
): void {
  setTimeout(() => {
    const inputs = document.querySelectorAll<HTMLElement>(
      `.option-row:nth-child(${optIndex + 1}) .option-value-row`,
    );
    if (inputs[valIndex]) {
      const inputClass =
        type === "purchase" ? ".input-left input" : ".input-right input";
      const targetInput =
        inputs[valIndex].querySelector<HTMLInputElement>(inputClass);
      if (targetInput) {
        targetInput.focus();
      }
    }
  }, 50);
}

// 添加选项
const addOption = (): void => {
  optionList.value.push({
    chineseName: "",
    englishName: "",
    values: [
      {
        chineseValue: "",
        englishValue: "",
      },
    ],
    collapsed: false,
  });

  // 优化3：点击添加选项时，新添加的选项名称自动获得光标
  nextTick(() => {
    // 使用 setTimeout 确保 DOM 完全渲染
    setTimeout(() => {
      const optionRows = document.querySelectorAll<HTMLElement>(".option-row");
      if (optionRows.length > 0) {
        const allInputs = optionRows[
          optionRows.length - 1
        ].querySelectorAll<HTMLInputElement>(
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
  const optionLists = optionList.value.filter(
    (opt) =>
      (opt.chineseName || opt.englishName) &&
      opt.values?.some((v) => v.chineseValue || v.englishValue),
  );
  return optionLists;
}

// 生成变体（笛卡尔积）
function generateVariants(): void {
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
        remark: "",
      },
    ];
    return;
  }
  // 计算笛卡尔积（使用 values 数组，并保留 chineseValue 和 englishValue）
  const valueArrays = activeOptions.map((opt) =>
    opt.values
      .filter((v) => v.chineseValue || v.englishValue)
      .map((v) => ({
        chineseValue: v.chineseValue,
        englishValue: v.englishValue,
      })),
  );
  const combinations = cartesianProduct(valueArrays);

  // 2. 映射生成变体行
  step1Variants.value = combinations.map((combo, index) => {
    const existingVariant = step1Variants.value[index];
    const optionValueList = combo.map((val, idx) => {
      const opt = activeOptions[idx];
      return {
        englishName: opt.englishName,
        englishValue: val.englishValue,
        chineseName: opt.chineseName,
        chineseValue: val.chineseValue,
      };
    });
    return {
      variantId: existingVariant?.variantId,
      purchaseUrl: existingVariant?.purchaseUrl || step1FormData.purchaseUrl,
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
function handleRowDrop(
  { row, $index }: { row: ProductVariant; $index: number },
  targetIndex: number,
): void {
  const sourceIndex = step2Variants.value.findIndex(
    (v) => v.position === row.position,
  );
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
    row.profit = parseFloat((rmbPrice - row.unitCostPrice).toFixed(2));
    row.profitRate = parseFloat(((row.profit / rmbPrice) * 100).toFixed(2));
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
function handleVariantImageDragStart(
  event: DragEvent,
  row: ProductVariant,
): void {
  event.dataTransfer!.effectAllowed = "move";
  if (row.mediaId) {
    // 保存当前行数据到全局变量，用于删除操作
    draggedVariantRow.value = row;
  }
  // 添加全局dragend事件监听器
  document.addEventListener("dragend", handleVariantImageDragEnd);
}

// 变体图片拖拽进入
function handleVariantImageDragEnter(
  event: DragEvent,
  row: ProductVariant,
): void {
  event.preventDefault();
  dragOverVariant.value = row;
}

// 变体图片拖拽离开
function handleVariantImageDragLeave(event: DragEvent): void {
  event.preventDefault();
  dragOverVariant.value = null;
}

// 变体图片拖拽结束
function handleVariantImageDragEnd(event: DragEvent): void {
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
          const draggedId = draggedVariantRow.value?.variantId;
          if (draggedId !== undefined) {
            step2Variants.value.forEach((row) => {
              if (row.variantId === draggedId) {
                row.mediaId = undefined;
              }
            });
          }
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
function isImage(media: Media | null | undefined): boolean {
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
  draggedImage.value = media;
  event.dataTransfer.effectAllowed = "move";
  event.dataTransfer.setData("media-id", media.mediaId);
  event.dataTransfer.setData(
    "media-index",
    step2FormData.mediaList.indexOf(media),
  );
  event.dataTransfer.setData("media-json", JSON.stringify(media));
  document.addEventListener("dragend", handleImageDragEnd);

  // 延迟添加拖拽样式以确保dataTransfer正常工作
  setTimeout(() => {
    const target = event.target as HTMLElement;
    target?.classList?.add("dragging");
  }, 0);
}

// 图片拖拽进入
function handleImageDragEnter(event, index) {
  event.preventDefault();
  dragOverIndex.value = index;
}

// 图片拖拽离开
function handleImageDragLeave(event) {
  event.preventDefault();
}

// 图片拖拽结束
function handleImageDragEnd(event) {
  // 移除拖拽样式
  const draggingEl = document.querySelector(".image-item.dragging");
  draggingEl?.classList.remove("dragging");

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

  if (!isInside && draggedImage.value) {
    try {
      ElMessageBox.confirm("确定要删除这张图片吗？", "删除确认", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          const draggedImg = draggedImage.value;
          if (draggedImg) {
            const index = step2FormData.mediaList.indexOf(draggedImg);
            if (index > -1) {
              step2FormData.mediaList.splice(index, 1);
            }
          }
        })
        .catch(() => {})
        .finally(() => {
          draggedImage.value = null;
          dragOverIndex.value = -1;
        });
    } catch (error) {
      console.error("处理图片拖拽删除时出错:", error);
      draggedImage.value = null;
      dragOverIndex.value = -1;
    }
  } else {
    draggedImage.value = null;
    dragOverIndex.value = -1;
  }

  document.removeEventListener("dragend", handleImageDragEnd);
}

// 图片拖拽放下（排序）
function handleImageDrop(event: DragEvent): void {
  event.preventDefault();

  if (draggedImage.value) {
    const fromIndex = step2FormData.mediaList.indexOf(draggedImage.value);
    const toIndex = dragOverIndex.value;

    if (fromIndex >= 0 && toIndex >= 0 && fromIndex !== toIndex) {
      const item = step2FormData.mediaList.splice(fromIndex, 1)[0];
      step2FormData.mediaList.splice(toIndex, 0, item);
    }
  }

  dragOverIndex.value = -1;
  draggedImage.value = null;
}

// 图片拖拽到变体
function handleVariantImageDrop(event: DragEvent, row: ProductVariant): void {
  event.preventDefault();

  // 优先从dataTransfer获取数据（从变体图片拖拽）
  try {
    const mediaData = event.dataTransfer!.getData("variant-media");
    if (mediaData) {
      const media: Media = JSON.parse(mediaData);
      if (media) {
        row.mediaId = media.mediaId;
        return;
      }
    }
  } catch (error) {
    console.error("处理变体图片拖拽放下时出错:", error);
  }

  // 从draggedImage获取数据（从图片列表拖拽）
  if (draggedImage.value) {
    row.mediaId = draggedImage.value.mediaId;
    draggedImage.value = null;
  }

  // 清空拖拽状态
  dragOverVariant.value = null;
}

// 提交表单continue、next、save
async function handleSave() {
  if (activeStep.value === 0) {
    return await handleSaveStep1();
  } else if (activeStep.value === 1) {
    return await handleSaveStep2();
  }
  return false;
}

async function handleSaveStep1() {
  try {
    // 根据当前步骤只校验对应的表单
    await step1FormRef.value?.validate();
    loading.value = true;

    const hasChanged = hasStep1DataChanged();

    // 保存productId到当前ProductId.value
    currentProductId.value = await handleSubmitData();

    emit("submit", { action: "save", hasChanged });

    // 重新加载数据，并保存快照
    await handleLoadStep1Data();
    return true;
  } catch (error) {
    console.error("提交失败", error);
    return false;
  } finally {
    loading.value = false;
  }
}

async function handleSaveStep2() {
  try {
    // 根据当前步骤只校验对应的表单
    await step2FormRef.value?.validate();
    loading.value = true;

    const hasChanged = hasStep2DataChanged();

    await handleSubmitData();

    emit("submit", { action: "save", hasChanged });

    // 重新加载数据，并保存快照
    await handleLoadStep2Data();
    return true;
  } catch (error) {
    console.error("提交失败", error);
    return false;
  } finally {
    loading.value = false;
  }
}

async function handlePrev() {
  try {
    // 根据当前步骤只校验对应的表单
    await step2FormRef.value?.validate();
    loading.value = true;

    const hasChanged = hasStep2DataChanged();
    if (hasChanged) {
      // 如果新增，直接保存。如果非新增，则弹窗确认是否保存
      const action = await proxy.$modal.confirm(
        "当前页面数据已修改，是否保存？",
      );
      if (action === "cancel") {
        return;
      }

      await handleSubmitData();

      emit("submit", { action: "prev", hasChanged });
    }
    // prev
    activeStep.value = 0;

    // 重置表单，加载数据，保存快照
    await resetAndLoadData(0);
  } catch (error) {
    console.error("提交失败", error);
  } finally {
    loading.value = false;
  }
}

// 提交表单continue、next、close
async function handleContinue() {
  try {
    // 根据当前步骤只校验对应的表单
    await step1FormRef.value?.validate();
    loading.value = true;

    const hasChanged = hasStep1DataChanged();
    if (hasChanged) {
      // 如果新增，直接保存。如果非新增，则弹窗确认是否保存
      if (currentProductId.value) {
        const action = await proxy.$modal.confirm(
          "当前页面数据已修改，是否保存？",
        );
        if (action === "cancel") {
          return;
        }
      }

      await handleSubmitData();
    }

    emit("submit", { action: "continue", hasChanged });
    // 重置表单，加载数据，保存快照
    await resetAndLoadData(0);
    activeStep.value = 0;
  } catch (error) {
    console.error("提交失败", error);
  } finally {
    loading.value = false;
  }
}

const handleNext = async () => {
  try {
    // 根据当前步骤只校验对应的表单
    await step1FormRef.value?.validate();
    loading.value = true;

    const hasChanged = hasStep1DataChanged();
    if (hasChanged) {
      // 如果新增，直接保存。如果非新增，则弹窗确认是否保存
      if (currentProductId.value) {
        const action = await proxy.$modal.confirm(
          "当前页面数据已修改，是否保存？",
        );
        if (action === "cancel") {
          return;
        }
      }

      // 保存productId到当前ProductId.value
      currentProductId.value = await handleSubmitData();
    }

    emit("submit", { action: "next", hasChanged });

    // 重置表单，加载数据，保存快照
    await resetAndLoadData(1);
    // 切换第二步
    activeStep.value = 1;
  } catch (error) {
    console.error("提交失败", error);
  } finally {
    loading.value = false;
  }
};

// 处理表单提交
const handleSubmitData = async (): Promise<number> => {
  if (activeStep.value === 0) {
    // 保存第一步
    // 确保变体中的 optionValueList 同步更新到 optionValues 字符串（如果需要后端兼容）
    const variantsToSubmit = step1Variants.value.map((v) => ({
      ...v,
      optionValues: JSON.stringify(v.optionValueList),
    }));
    const submitData = {
      ...step1FormData,
      optionList: optionList.value,
      productVariantList: variantsToSubmit,
    };
    console.log("handleSubmitData", submitData);
    const response = await addSelectionInfo(submitData);
    return response.data;
  } else if (activeStep.value === 1) {
    // 保存第二步
    const submitData = {
      productId: step2FormData.productId!,
      productTitle: step2FormData.productTitle,
      bodyHtml: step2FormData.bodyHtml,
      mediaList: step2FormData.mediaList,
      productVariantList: step2Variants.value,
    };
    await updateBaseInfo(submitData);
    return step2FormData.productId!;
  }
  return 0;
};

// 关闭对话框
/*function handleClose() {
  visible.value = false;
  emit("submit", { action: "cancel", hasChanged: false });
}*/

//表格展示列
const columns = ref([
  { key: 0, label: "规格", visible: true },
  { key: 1, label: "SKU", visible: true },
  { key: 2, label: "采购价", visible: true },
  { key: 3, label: "包装", visible: true },
  { key: 4, label: "实重", visible: true },
  { key: 5, label: "材积重", visible: false },
  { key: 6, label: "实际发货", visible: false },
  { key: 7, label: "运费", visible: true },
  { key: 8, label: "成本价", visible: false },
  { key: 9, label: "汇率", visible: false },
  { key: 10, label: "建议售价", visible: false },
  { key: 11, label: "利润", visible: false },
  { key: 12, label: "利润率", visible: true },
  { key: 13, label: "售价", visible: true },
  { key: 14, label: "对比价", visible: false },
]);

// 暴露方法给父组件
defineExpose({
  open,
});
</script>

<style scoped>
.product-wizard-container {
  min-height: calc(100vh - 84px);
  padding: 24px;
  background: radial-gradient(
      circle at top left,
      rgba(30, 64, 175, 0.12),
      transparent 30%
    ),
    radial-gradient(
      circle at top right,
      rgba(34, 197, 94, 0.1),
      transparent 24%
    ),
    linear-gradient(180deg, #f4f7fb 0%, #eef2f8 100%);
}

.product-wizard-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.wizard-header,
.wizard-progress-card,
.step-form,
.image-manager,
.rich-text-editor-expanded,
.variant-table,
.option-row {
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.wizard-header {
  position: sticky;
  top: 0;
  z-index: 20;
  margin-bottom: 0;
  padding: 20px 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  font-size: 24px;
  line-height: 1.2;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: #0f172a;
}

.page-subtitle {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.header-status {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(248, 250, 252, 0.94);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.status-pill--active {
  border-color: rgba(37, 99, 235, 0.24);
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  color: #1d4ed8;
}

.right-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.header-action-btn {
  min-width: 108px;
  height: 40px;
  border-radius: 12px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.header-action-btn:hover {
  transform: translateY(-1px);
}

.header-action-btn--ghost {
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.12);
}

.header-action-btn--strong {
  box-shadow: 0 12px 24px rgba(34, 197, 94, 0.18);
}

.wizard-progress-card {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 24px;
  align-items: center;
  padding: 24px 28px;
  border-radius: 24px;
  background: radial-gradient(
      circle at top right,
      rgba(34, 197, 94, 0.22),
      transparent 28%
    ),
    linear-gradient(135deg, #0f172a 0%, #1e293b 56%, #1e3a8a 100%);
}

.wizard-progress-copy {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #f8fafc;
}

.wizard-progress-copy__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(191, 219, 254, 0.84);
}

.wizard-progress-copy__title {
  font-size: 22px;
  line-height: 1.35;
  font-weight: 700;
}

.wizard-progress-copy__desc {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(226, 232, 240, 0.88);
}

.wizard-steps {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.06);
}

.step-content {
  flex: 1;
}

.step-form {
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
}

.step1-form {
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.98),
    rgba(248, 250, 252, 0.94)
  );
}

.step2-form {
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.98),
    rgba(246, 249, 252, 0.94)
  );
}

.form-item-block {
  padding: 18px 20px 20px;
  margin: 12px 0 0;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.mb-4 {
  margin-bottom: 0;
}

.mb-2 {
  margin-bottom: 8px;
}

.options-container,
.image-manager,
.detail-tabs,
.rich-text-editor-container {
  width: 100%;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 0;
  overflow: hidden;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.option-row:hover,
.option-row:focus-within {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 20px 36px rgba(37, 99, 235, 0.08);
}

.option-row-collapsed {
  padding: 14px 16px;
  background: linear-gradient(135deg, #f8fbff 0%, #f2f7ff 100%);
}

.option-values-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.option-value-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.input-label {
  width: 88px;
  flex-shrink: 0;
  text-align: right;
  font-size: 13px;
  line-height: 40px;
  font-weight: 600;
  color: #475569;
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
  border-radius: 12px 0 0 12px;
  border-right: none;
}

.option-value-row .input-combined .input-right :deep(.el-input__wrapper) {
  border-radius: 0 12px 12px 0;
}

.delete-button-wrapper {
  width: 36px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-left: 8px;
}

.option-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
}

.option-collapsed-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.collapsed-content {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  flex-wrap: wrap;
}

.option-name-display {
  font-size: 14px;
  color: #1e293b;
  white-space: nowrap;
}

.option-name-display strong {
  font-weight: 700;
  color: #0f172a;
}

.option-name-display .en-name,
.value-tag .en-value {
  color: #64748b;
  font-size: 12px;
}

.option-values-display {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

.value-tag {
  background: rgba(219, 234, 254, 0.74) !important;
  border-color: rgba(96, 165, 250, 0.24) !important;
  color: #1d4ed8 !important;
  font-size: 12px;
  padding: 2px 8px;
}

.expand-btn {
  flex-shrink: 0;
}

.ml-2 {
  margin-left: 8px !important;
}

.image-manager {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.96),
    rgba(241, 245, 249, 0.98)
  );
}

.image-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.media-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: linear-gradient(
    135deg,
    rgba(99, 102, 241, 0.08) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  margin-bottom: 16px;
}

.media-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e1b4b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.media-title::before {
  content: "";
  display: inline-block;
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #6366f1, #3b82f6);
  border-radius: 2px;
}

.media-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.media-toolbar .el-input {
  --el-input-border-color: rgba(99, 102, 241, 0.3);
  --el-input-hover-border-color: rgba(99, 102, 241, 0.5);
  --el-input-focus-border-color: #6366f1;
}

.media-form-item .el-form-item__content {
  display: block !important;
}

/* 媒体面板统一样式（通过.is-floating切换悬浮状态） */
.media-panel {
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(
    180deg,
    #ffffff 0%,
    rgba(248, 250, 252, 0.8) 100%
  );
  border: 1px solid rgba(99, 102, 241, 0.1);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.25s ease, transform 0.2s ease;
}

.media-panel.is-floating {
  position: fixed;
  z-index: 9999;
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 60px rgba(15, 23, 42, 0.25),
    0 12px 28px rgba(15, 23, 42, 0.15), 0 0 0 1px rgba(99, 102, 241, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.media-panel.is-floating:hover {
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.3),
    0 16px 35px rgba(15, 23, 42, 0.2), 0 0 0 1px rgba(99, 102, 241, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 媒体面板头部 */
.media-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: linear-gradient(
    135deg,
    rgba(99, 102, 241, 0.08) 0%,
    rgba(59, 130, 246, 0.05) 100%
  );
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  margin-bottom: 16px;
}

.media-panel.is-floating .media-panel-header {
  margin: 16px 16px 0 16px;
  cursor: move;
  user-select: none;
}

.media-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1e1b4b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.media-panel-title::before {
  content: "";
  display: inline-block;
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #6366f1, #3b82f6);
  border-radius: 2px;
}

.media-panel-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.media-panel-toolbar .el-input {
  --el-input-border-color: rgba(99, 102, 241, 0.3);
  --el-input-hover-border-color: rgba(99, 102, 241, 0.5);
  --el-input-focus-border-color: #6366f1;
}

.media-panel-toolbar .el-button--primary {
  background: #fff;
  border-color: rgba(99, 102, 241, 0.3);
  color: #6366f1;
}

.media-panel-toolbar .el-button--primary:hover {
  background: rgba(99, 102, 241, 0.08);
  border-color: rgba(99, 102, 241, 0.5);
}

.media-panel.is-floating .media-panel-toolbar .el-button.is-circle {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
  background: #fff;
}

.media-panel.is-floating .media-panel-toolbar .el-button.is-circle:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.5);
}

.media-panel-content {
  flex: 1;
  overflow: auto;
}

.media-panel.is-floating .media-panel-content {
  padding: 16px;
}

/* 悬浮面板拖拽调整大小手柄 */
.media-panel-resize {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 28px;
  height: 28px;
  cursor: se-resize;
  background: linear-gradient(
    135deg,
    transparent 40%,
    rgba(99, 102, 241, 0.7) 40%,
    rgba(99, 102, 241, 0.7) 60%,
    transparent 60%
  );
  border-radius: 0 0 20px 0;
  transition: background 0.2s ease;
}

.media-panel-resize:hover {
  background: linear-gradient(
    135deg,
    transparent 35%,
    #6366f1 35%,
    #6366f1 65%,
    transparent 65%
  );
}

/* 隐藏悬浮状态下的分割线 */
.media-section-divider.is-hidden {
  display: none;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 18px;
  min-height: 140px;
  padding: 20px;
  border-radius: 20px;
  border: 2px dashed rgba(99, 102, 241, 0.2);
  background: rgba(255, 255, 255, 0.6);
  transition: border-color 0.25s ease, background 0.25s ease,
    box-shadow 0.25s ease;
}

.image-grid.is-floating-grid {
  min-height: 140px;
  padding: 20px;
  border-radius: 20px;
}

.image-grid:hover {
  border-color: rgba(99, 102, 241, 0.4);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 2px 8px rgba(99, 102, 241, 0.05);
}

.image-item {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.2s ease, border-color 0.2s ease, opacity 0.2s ease;
  cursor: pointer;
  background: #fff;
}

.image-item:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  border-color: rgba(99, 102, 241, 0.4);
}

.image-item.dragging {
  opacity: 0.5;
  transform: scale(0.95);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

.image-item.drag-over {
  border: 2px dashed #6366f1;
  transform: scale(1.05);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.25);
  background: rgba(99, 102, 241, 0.05);
}

.image-item:active {
  transform: scale(0.98);
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.image-item:hover .image-thumb {
  transform: scale(1.08);
}

.image-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 12px;
  background: linear-gradient(
    180deg,
    rgba(15, 23, 42, 0.02) 50%,
    rgba(15, 23, 42, 0.75) 100%
  );
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.media-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: rgba(99, 102, 241, 0.9);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 20px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  backdrop-filter: blur(4px);
}

.image-placeholder {
  width: 100%;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  border: 2px dashed rgba(99, 102, 241, 0.25);
  border-radius: 20px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.9) 0%,
    rgba(241, 245, 249, 0.95) 100%
  );
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease,
    box-shadow 0.2s ease;
  cursor: pointer;
}

.image-placeholder:hover {
  transform: translateY(-2px);
  border-color: rgba(99, 102, 241, 0.5);
  background: linear-gradient(
    180deg,
    rgba(238, 242, 255, 0.95) 0%,
    rgba(224, 231, 255, 0.9) 100%
  );
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.1);
}

.placeholder-icon {
  font-size: 48px;
  color: #94a3b8;
  transition: color 0.2s ease, transform 0.2s ease;
}

.image-placeholder:hover .placeholder-icon {
  color: #6366f1;
  transform: scale(1.1);
}

.placeholder-text {
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

.variant-image-item {
  position: relative;
  width: 88px;
  height: 66px;
  border: 1px dashed rgba(148, 163, 184, 0.42);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.92),
    rgba(255, 255, 255, 0.98)
  );
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.variant-image-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.1);
}

.variant-image-item.drag-over {
  border-color: rgba(37, 99, 235, 0.72);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.14);
}

.variant-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.22s ease;
}

.variant-image-item:hover .variant-thumb {
  transform: scale(1.06);
}

.drop-hint {
  padding: 0 6px;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
  color: #64748b;
}

.detail-tabs {
  border-radius: 18px;
  overflow: hidden;
}

.rich-text-editor-expanded {
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.rich-text-editor-expanded:hover {
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.08);
}

.rich-text-editor-toolbar {
  padding: 14px 16px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.96),
    rgba(241, 245, 249, 0.96)
  );
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.rich-text-preview {
  min-height: 220px;
  padding: 20px;
  background: #ffffff;
  color: #334155;
}

.rich-text-preview .no-content {
  display: inline-flex;
  width: 100%;
  justify-content: center;
  padding: 40px 0;
  color: #94a3b8;
  font-style: italic;
}

.variant-table {
  margin-top: 12px;
  border-radius: 20px;
  overflow: hidden;
  background: #ffffff;
}

.file-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.96),
    rgba(241, 245, 249, 0.96)
  );
  color: #64748b;
  font-size: 12px;
}

.file-placeholder .el-icon {
  font-size: 30px;
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
  top: 10px;
  left: 10px;
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.video-thumbnail {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-play-button {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: rgba(15, 23, 42, 0.64);
  transform: translate(-50%, -50%);
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.video-thumbnail:hover .video-play-button {
  background: rgba(15, 23, 42, 0.82);
  transform: translate(-50%, -50%) scale(1.06);
}

.footer-right-buttons {
  display: flex;
  gap: 10px;
}

:deep(.el-page-header__content) {
  width: 100%;
}

:deep(.el-page-header__left) {
  align-items: flex-start;
}

:deep(.wizard-progress-card .el-step__title) {
  color: rgba(226, 232, 240, 0.88);
  font-weight: 600;
}

:deep(.wizard-progress-card .is-process .el-step__title),
:deep(.wizard-progress-card .is-finish .el-step__title) {
  color: #ffffff;
}

:deep(.wizard-progress-card .el-step__icon) {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.24);
  color: #ffffff;
}

:deep(.wizard-progress-card .is-process .el-step__icon),
:deep(.wizard-progress-card .is-finish .el-step__icon) {
  background: linear-gradient(135deg, #2563eb, #22c55e);
  border-color: transparent;
}

:deep(.step-form .el-form-item) {
  margin-bottom: 20px;
}

:deep(.step-form .el-divider) {
  margin: 22px 0 14px;
}

:deep(.step-form .el-divider__text) {
  padding: 0 14px;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.9);
}

:deep(.step-form .el-form-item__label) {
  padding-right: 14px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

:deep(.step-form .el-input__wrapper),
:deep(.step-form .el-textarea__inner),
:deep(.step-form .el-select__wrapper),
:deep(.step-form .el-cascader .el-input__wrapper),
:deep(.step-form .el-input-number .el-input__wrapper) {
  border-radius: 12px;
  transition: box-shadow 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
}

:deep(.step-form .el-input__wrapper:hover),
:deep(.step-form .el-textarea__inner:hover),
:deep(.step-form .el-select__wrapper:hover),
:deep(.step-form .el-cascader .el-input__wrapper:hover),
:deep(.step-form .el-input-number .el-input__wrapper:hover) {
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

:deep(.step-form .el-input__wrapper.is-focus),
:deep(.step-form .el-textarea__inner:focus),
:deep(.step-form .el-select__wrapper.is-focused),
:deep(.step-form .el-cascader .is-focus .el-input__wrapper),
:deep(.step-form .el-input-number .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

:deep(.step-form .el-textarea__inner) {
  min-height: 92px;
  resize: vertical;
}

:deep(.step-form .el-button) {
  border-radius: 12px;
  font-weight: 600;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

:deep(.step-form .el-button:hover) {
  transform: translateY(-1px);
}

:deep(.step-form .el-button--primary) {
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.16);
}

:deep(.step-form .el-button--success) {
  box-shadow: 0 10px 18px rgba(34, 197, 94, 0.16);
}

:deep(.detail-tabs .el-tabs__header) {
  margin-bottom: 0;
  background: linear-gradient(
    180deg,
    rgba(248, 250, 252, 0.92),
    rgba(241, 245, 249, 0.92)
  );
}

:deep(.detail-tabs .el-tabs__nav-wrap) {
  padding: 0 10px;
}

:deep(.detail-tabs .el-tabs__item) {
  height: 46px;
  padding: 0 16px;
  font-weight: 600;
  color: #64748b;
}

:deep(.detail-tabs .el-tabs__item.is-active) {
  color: #1d4ed8;
}

:deep(.detail-tabs .el-tabs__content) {
  padding: 18px;
  background: #ffffff;
}

:deep(.variant-table .el-table__header-wrapper th) {
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  color: #334155;
  font-weight: 700;
}

:deep(.variant-table .el-table__row td) {
  transition: background-color 0.2s ease;
}

:deep(.variant-table .el-table__row:hover td) {
  background: rgba(239, 246, 255, 0.72) !important;
}

:deep(.variant-table .el-input-number) {
  width: 100% !important;
}

:deep(.variant-table .el-switch) {
  --el-switch-on-color: #22c55e;
}

:deep(.el-table .set-row) {
  --el-table-tr-bg-color: rgba(219, 234, 254, 0.8) !important;
}

@media (max-width: 1200px) {
  .wizard-progress-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .product-wizard-container {
    padding: 14px;
  }

  .wizard-header,
  .wizard-progress-card,
  .step-form,
  .image-manager,
  .variant-table,
  .option-row {
    border-radius: 18px;
  }

  .wizard-header {
    padding: 16px;
  }

  .page-title {
    font-size: 20px;
  }

  .right-buttons {
    width: 100%;
  }

  .header-action-btn {
    flex: 1 1 calc(50% - 10px);
    min-width: 0;
  }

  .wizard-progress-card {
    padding: 18px;
  }

  .step-form {
    padding: 16px;
  }

  .form-item-block {
    padding: 14px;
  }

  .option-value-row,
  .option-footer,
  .option-collapsed-hint {
    flex-direction: column;
    align-items: stretch;
  }

  .input-label {
    width: 100%;
    text-align: left;
    line-height: 1.5;
  }

  .delete-button-wrapper {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
  }

  .image-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  :deep(.step-form .el-form-item__content) {
    min-width: 0;
  }
}

@media (max-width: 576px) {
  .header-action-btn {
    flex-basis: 100%;
  }

  .status-pill {
    width: 100%;
    justify-content: center;
  }

  .image-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wizard-header,
  .wizard-progress-card,
  .step-form,
  .image-manager,
  .variant-table,
  .option-row,
  .image-item,
  .variant-image-item,
  .header-action-btn,
  .video-play-button,
  :deep(.step-form .el-button),
  :deep(.step-form .el-input__wrapper),
  :deep(.step-form .el-textarea__inner) {
    transition: none !important;
    animation: none !important;
  }
}
</style>
