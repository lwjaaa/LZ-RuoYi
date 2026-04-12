<template>
  <el-dialog
    v-model="dialogVisible"
    :title="video?.filename || '视频播放'"
    :width="dialogWidth"
    append-to-body
    :close-on-click-modal="false"
    class="video-dialog"
    :show-close="true"
    :destroy-on-close="true"
  >
    <!-- style="padding: 0" -->

    <div class="video-player-container">
      <vue-plyr ref="videoPlayerRef" :options="plyrOptions">
        <video>
          <source
            :src="baseUrl + (video?.nasMediaUrl || video?.shopifyMediaUrl)"
          />
        </video>
      </vue-plyr>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from "vue";
import VuePlyr from "vue-plyr";
import "vue-plyr/dist/vue-plyr.css";
import { VideoPlay } from "@element-plus/icons-vue";

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  video: {
    type: Object,
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
const dialogVisible = ref(props.visible);
const dialogWidth = ref("800px");
const dialogHeight = ref("500px");
const videoPlayerRef = ref(null);

// 拖拽相关状态
const isDragging = ref(false);
const isResizing = ref(false);
const startX = ref(0);
const startY = ref(0);
const startWidth = ref(0);
const startHeight = ref(0);
const dialogElement = ref(null);

// 监听visible变化
watch(
  () => props.visible,
  (newVal) => {
    dialogVisible.value = newVal;
  },
);

// 监听dialogVisible变化
watch(dialogVisible, (newVal) => {
  emit("update:visible", newVal);
});

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

// 初始化拖拽和调整大小功能
onMounted(() => {
  nextTick(() => {
    // 使用更具体的选择器
    dialogElement.value = document.querySelector(".video-dialog.el-dialog");
    if (dialogElement.value) {
      // 确保对话框使用fixed定位
      dialogElement.value.style.position = "fixed";
      dialogElement.value.style.left = "50%";
      dialogElement.value.style.top = "50%";
      dialogElement.value.style.transform = "translate(-50%, -50%)";
      dialogElement.value.style.margin = "0";

      initDragAndResize();
    }
  });
});

// 清理事件监听器
onUnmounted(() => {
  document.removeEventListener("mousemove", handleMouseMove);
  document.removeEventListener("mouseup", handleMouseUp);
});

// 初始化拖拽和调整大小
function initDragAndResize() {
  console.log("initDragAndResize");
  if (!dialogElement.value) return;

  const header = dialogElement.value.querySelector(".el-dialog__header");
  const body = dialogElement.value.querySelector(".el-dialog__body");

  if (header) {
    header.style.cursor = "move";
    header.style.userSelect = "none";
    header.addEventListener("mousedown", startDrag, { capture: true });
  }

  if (body) {
    body.style.position = "relative";
    const resizeHandle = document.createElement("div");
    resizeHandle.className = "resize-handle";
    body.appendChild(resizeHandle);
    resizeHandle.addEventListener("mousedown", startResize, { capture: true });
  }

  document.addEventListener("mousemove", handleMouseMove, { capture: true });
  document.addEventListener("mouseup", handleMouseUp, { capture: true });
}

// 开始拖拽
function startDrag(e) {
  // 只在鼠标左键点击时触发
  if (e.button !== 0) return;

  isDragging.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;

  if (dialogElement.value) {
    const rect = dialogElement.value.getBoundingClientRect();
    startX.value = e.clientX - rect.left;
    startY.value = e.clientY - rect.top;
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();
}

// 开始调整大小
function startResize(e) {
  console.log(e);
  // 只在鼠标左键点击时触发
  if (e.button !== 0) return;

  isResizing.value = true;
  startX.value = e.clientX;
  startY.value = e.clientY;

  if (dialogElement.value) {
    const rect = dialogElement.value.getBoundingClientRect();
    startWidth.value = rect.width;
    startHeight.value = rect.height;
  }

  // 阻止默认行为和冒泡
  e.preventDefault();
  e.stopPropagation();
}

// 处理鼠标移动
function handleMouseMove(e) {
  if (isDragging.value && dialogElement.value) {
    const newLeft = e.clientX - startX.value;
    const newTop = e.clientY - startY.value;

    // 使用固定定位
    dialogElement.value.style.position = "fixed";
    dialogElement.value.style.left = `${newLeft}px`;
    dialogElement.value.style.top = `${newTop}px`;
    dialogElement.value.style.transform = "none";
    dialogElement.value.style.margin = "0";

    // 阻止默认行为和冒泡
    e.preventDefault();
    e.stopPropagation();
  }

  if (isResizing.value && dialogElement.value) {
    const width = startWidth.value + (e.clientX - startX.value);
    const height = startHeight.value + (e.clientY - startY.value);

    // 最小尺寸限制
    if (width > 400 && height > 300) {
      dialogElement.value.style.width = `${width}px`;
      dialogElement.value.style.height = `${height}px`;
      dialogWidth.value = `${width}px`;
      dialogHeight.value = `${height}px`;
    }

    // 阻止默认行为和冒泡
    e.preventDefault();
    e.stopPropagation();
  }
}

// 处理鼠标抬起
function handleMouseUp() {
  isDragging.value = false;
  isResizing.value = false;
}
</script>

<style scoped>
/* 视频播放器容器样式 */
.video-player-container {
  width: 100%;
  aspect-ratio: 16/9;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.video-player-container :deep(.plyr) {
  width: 100%;
  height: 100%;
}

/* 视频弹框样式 */
:deep(.el-dialog) {
  padding: 0 !important;
  --el-dialog-padding-primary: 0 !important;
}
:deep(.video-dialog.el-dialog) {
  --el-dialog-padding-primary: 0 !important;
  border-radius: 12px !important;
  overflow: hidden !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3) !important;
  animation: dialogFadeIn 0.3s ease-out;
  min-width: 400px !important;
  min-height: 300px !important;
  padding: 0 !important;
  z-index: 9999 !important;
  margin: 0 !important;
  padding: 0 !important;
}

.video-dialog :deep(.el-dialog__header) {
  background-color: #f5f7fa !important;
  border-bottom: 1px solid #e4e7ed !important;
  padding: 0 !important;
  border-radius: 12px 12px 0 0 !important;
  margin: 0 !important;
}

:deep(.video-dialog .el-dialog__title) {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #303133 !important;
}

:deep(.video-dialog .el-dialog__body) {
  padding: 20px !important;
  background-color: #ffffff !important;
  position: relative !important;
  margin: 0 !important;
}

:deep(.video-dialog .el-dialog__close) {
  font-size: 20px !important;
  color: #909399 !important;
  transition: all 0.3s ease !important;
}

:deep(.video-dialog .el-dialog__close:hover) {
  color: #409eff !important;
  transform: rotate(90deg) !important;
}

/* 调整大小手柄 */
.resize-handle {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 20px;
  height: 20px;
  background-color: #409eff;
  cursor: nwse-resize;
  border-radius: 0 0 12px 0;
  opacity: 0.5;
  transition: opacity 0.3s ease;
  z-index: 10000;
}

.resize-handle:hover {
  opacity: 1;
}

/* 动画效果 */
@keyframes dialogFadeIn {
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
  :deep(.video-dialog.el-dialog) {
    width: 95vw !important;
    max-width: none !important;
  }

  .video-player-container {
    aspect-ratio: 16/9;
  }

  :deep(.video-dialog .el-dialog__header) {
    padding: 12px 16px !important;
  }

  :deep(.video-dialog .el-dialog__body) {
    padding: 16px !important;
  }

  :deep(.video-dialog .el-dialog__title) {
    font-size: 14px !important;
  }
}
</style>
