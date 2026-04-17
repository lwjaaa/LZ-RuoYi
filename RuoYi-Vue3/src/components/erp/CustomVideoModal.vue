<template>
  <div v-if="visible" class="custom-modal-overlay" @click="handleOverlayClick">
    <div
      ref="modalRef"
      class="custom-modal"
      :style="{
        width: modalWidth,
        height: modalHeight,
        left: modalLeft,
        top: modalTop,
      }"
      @mouseenter="isHovering = true"
      @mouseleave="isHovering = false"
    >
      <!-- 弹窗头部 -->
      <div
        class="modal-header"
        @mousedown="startDrag"
        :class="{ 'is-visible': isHovering }"
      >
        <h3 class="modal-title">{{ video?.filename || "视频播放" }}</h3>
        <button class="modal-close" @click="closeModal">
          <el-icon><Close /></el-icon>
        </button>
      </div>

      <!-- 弹窗内容 -->
      <div class="modal-body">
        <div class="video-player-container">
          <vue-plyr ref="videoPlayerRef" :options="plyrOptions">
            <video>
              <source
                :src="baseUrl + (video?.nasMediaUrl || video?.shopifyMediaUrl)"
              />
            </video>
          </vue-plyr>
        </div>
      </div>

      <!-- 调整大小手柄 -->
      <div
        class="resize-handle"
        @mousedown="startResize"
        :class="{ 'is-visible': isHovering }"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from "vue";
import VuePlyr from "vue-plyr";
import "vue-plyr/dist/vue-plyr.css";
import { Close } from "@element-plus/icons-vue";

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  video: {
    type: Object as () => Record<string, any> | null | undefined,
    default: null,
  },
  baseUrl: {
    type: String,
    default: "",
  },
});

// Emits
const emit = defineEmits(["update:visible"]);

// 响应式状态
const modalRef = ref<HTMLElement | null>(null);
const videoPlayerRef = ref<any>(null);

// 弹窗位置和尺寸
const modalWidth = ref("800px");
const modalHeight = ref("500px");
const modalLeft = ref("50%");
const modalTop = ref("50%");

// 鼠标悬浮状态
const isHovering = ref(false);

// 拖拽相关状态
const isDragging = ref(false);
const isResizing = ref(false);
const startX = ref(0);
const startY = ref(0);
const startWidth = ref(0);
const startHeight = ref(0);
const startLeft = ref(0);
const startTop = ref(0);
const dragPosition = ref({ x: 0, y: 0 });
const resizeDimensions = ref({ width: 0, height: 0 });
let animationFrameId: number | null = null;

// 监听visible变化
watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      nextTick(() => {
        initModalPosition();
      });
    }
  },
);

// 初始化弹窗位置
function initModalPosition() {
  if (modalRef.value) {
    // 居中显示
    modalLeft.value = "50%";
    modalTop.value = "50%";
    modalRef.value.style.transform = "translate(-50%, -50%)";
  }
}

// 关闭弹窗
function closeModal() {
  emit("update:visible", false);
}

// 点击遮罩不关闭
function handleOverlayClick(e) {
  // 移除点击遮罩关闭的功能
}

// VuePlyr配置
const plyrOptions = {
  controls: [
    "play-large",
    "play",
    "progress",
    "current-time",
    "mute",
    "volume",
    "settings",
    "fullscreen",
  ],
  settings: ["speed", "quality"],
  loop: {
    active: false,
  },
  keyboard: {
    focused: true,
    global: false,
  },
  tooltips: {
    controls: true,
    seek: true,
  },
  ratio: "16:9",
  muted: false,
  volume: 0.5,
  speed: {
    selected: 1,
    options: [0.5, 0.75, 1, 1.25, 1.5, 2],
  },
  autoplay: true,
};

// 开始拖拽
function startDrag(e) {
  // 只在鼠标左键点击时触发
  if (e.button !== 0) return;

  isDragging.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;

  if (modalRef.value) {
    const rect = modalRef.value.getBoundingClientRect();
    startLeft.value = rect.left;
    startTop.value = rect.top;
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();

  // 添加鼠标移动和抬起事件监听器
  document.addEventListener("mousemove", handleDrag);
  document.addEventListener("mouseup", stopDrag);
}

// 处理拖拽
function handleDrag(e) {
  if (!isDragging.value || !modalRef.value) return;

  dragPosition.value = {
    x: e.clientX - startX.value,
    y: e.clientY - startY.value,
  };

  // 使用requestAnimationFrame优化拖拽动画
  if (!animationFrameId) {
    animationFrameId = requestAnimationFrame(updateDragPosition);
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();
}

// 更新拖拽位置
function updateDragPosition() {
  if (isDragging.value && modalRef.value) {
    const newLeft = startLeft.value + dragPosition.value.x;
    const newTop = startTop.value + dragPosition.value.y;

    // 更新位置
    modalLeft.value = `${newLeft}px`;
    modalTop.value = `${newTop}px`;
    modalRef.value.style.transform = "none";

    animationFrameId = requestAnimationFrame(updateDragPosition);
  } else if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }
}

// 停止拖拽
function stopDrag() {
  isDragging.value = false;
  document.removeEventListener("mousemove", handleDrag);
  document.removeEventListener("mouseup", stopDrag);

  // 取消动画帧请求
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }
}

// 开始调整大小
function startResize(e) {
  // 只在鼠标左键点击时触发
  if (e.button !== 0) return;

  isResizing.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;

  if (modalRef.value) {
    const rect = modalRef.value.getBoundingClientRect();
    startWidth.value = rect.width;
    startHeight.value = rect.height;
    startLeft.value = rect.left;
    startTop.value = rect.top;
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();

  // 添加鼠标移动和抬起事件监听器
  document.addEventListener("mousemove", handleResize);
  document.addEventListener("mouseup", stopResize);
}

// 处理调整大小
function handleResize(e) {
  if (!isResizing.value || !modalRef.value) return;

  resizeDimensions.value = {
    width: e.clientX - startX.value,
    height: e.clientY - startY.value,
  };

  // 使用requestAnimationFrame优化调整大小动画
  if (!animationFrameId) {
    animationFrameId = requestAnimationFrame(updateResizeDimensions);
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();
}

// 更新调整大小
function updateResizeDimensions() {
  if (isResizing.value && modalRef.value) {
    // 计算新的宽度和高度，保持左上角位置固定
    const newWidth = startWidth.value + resizeDimensions.value.width;
    const newHeight = startHeight.value + resizeDimensions.value.height;

    // 最小尺寸限制
    if (newWidth > 400 && newHeight > 300) {
      // 保持左上角位置固定
      modalLeft.value = `${startLeft.value}px`;
      modalTop.value = `${startTop.value}px`;
      modalWidth.value = `${newWidth}px`;
      modalHeight.value = `${newHeight}px`;
      modalRef.value.style.transform = "none";
    }

    animationFrameId = requestAnimationFrame(updateResizeDimensions);
  } else if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }
}

// 停止调整大小
function stopResize() {
  isResizing.value = false;
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);

  // 取消动画帧请求
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
    animationFrameId = null;
  }
}

// 处理ESC键关闭
function handleKeydown(e) {
  if (e.key === "Escape" && props.visible) {
    closeModal();
  }
}

// 组件挂载时添加事件监听器
onMounted(() => {
  document.addEventListener("keydown", handleKeydown);
});

// 清理事件监听器
onUnmounted(() => {
  document.removeEventListener("mousemove", handleDrag);
  document.removeEventListener("mouseup", stopDrag);
  document.removeEventListener("mousemove", handleResize);
  document.removeEventListener("mouseup", stopResize);
  document.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
/* 遮罩层 */
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}

/* 弹窗容器 */
.custom-modal {
  position: fixed;
  background-color: #ffffff;
  /* border-radius: 12px; */
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  z-index: 10000;
  animation: slideIn 0.3s ease;
  min-width: 400px;
  min-height: 300px;
  padding: 0;
}

/* 弹窗头部 */
.modal-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.8);
  color: #ffffff;
  padding: 12px 20px;
  cursor: move;
  user-select: none;
  transform: translateY(-100%);
  transition: transform 0.4s ease-in-out;
  z-index: 10001;
}

.modal-header.is-visible {
  transform: translateY(0);
}

.modal-title {
  font-size: 14px;
  font-weight: 500;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 16px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.modal-close:hover {
  background-color: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

/* 弹窗内容 */
.modal-body {
  padding: 0;
  position: relative;
  height: 100%;
}

/* 视频播放器容器 */
.video-player-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.video-player-container :deep(.plyr) {
  width: 100%;
  height: 100%;
  /* border-radius: 12px; */
}

/* 调整大小手柄 */
.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 14px;
  height: 14px;
  background-color: rgba(64, 158, 255, 0.8);
  cursor: nwse-resize;
  border-radius: 14px 0 0 0;
  opacity: 0;
  transition: all 0.3s ease-in-out;
  z-index: 10001;
}

.resize-handle.is-visible {
  opacity: 1;
}

.resize-handle:hover {
  background-color: #409eff;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .custom-modal {
    width: 95vw !important;
    max-width: none !important;
  }

  .modal-header {
    padding: 10px 16px;
  }

  .modal-title {
    font-size: 12px;
  }

  .modal-close {
    font-size: 16px;
    width: 28px;
    height: 28px;
  }
}
</style>
