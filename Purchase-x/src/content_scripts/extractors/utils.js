const Logger = {
  step: (step, message) => console.log(`[PurchaseX][${step}] ${message}`),
  success: (label, value) => console.log(`[PurchaseX] ✓ ${label}: ${value}`),
  info: (label, value) => console.log(`[PurchaseX] ℹ ${label}: ${value || ""}`),
  warn: (label, value) =>
    console.warn(`[PurchaseX] ⚠ ${label}: ${value || ""}`),
  error: (label, error) => console.error(`[PurchaseX] ✗ ${label}:`, error),
};

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function normalizeImageUrl(url, platform = "taobao") {
  if (!url) return "";

  let normalizedUrl = url.trim();

  // 处理协议相对 URL
  if (normalizedUrl.startsWith("//")) {
    normalizedUrl = "https:" + normalizedUrl;
  }

  // 处理无协议的 URL
  if (!normalizedUrl.startsWith("http")) {
    normalizedUrl = "https:" + normalizedUrl;
  }

  // 针对不同平台优化图片 URL，获取高清原图
  try {
    const urlObj = new URL(normalizedUrl);
    const hostname = urlObj.hostname.toLowerCase();

    // 淘宝/天猫/1688 图片优化规则
    if (
      hostname.includes("alicdn.com") ||
      hostname.includes("taobaocdn.com") ||
      hostname.includes("tbcdn.cn") ||
      hostname.includes("1688.com")
    ) {
      // 保存原始路径用于后续处理
      const originalPath = normalizedUrl;

      // 处理 .webp 格式，转为 .jpg
      if (normalizedUrl.toLowerCase().endsWith(".webp")) {
        normalizedUrl = normalizedUrl.replace(/\.webp$/i, ".jpg");
      }

      // 移除质量参数（_q50.jpg 等）
      normalizedUrl = normalizedUrl.replace(/_q\d+\.jpg/gi, ".jpg");

      // 移除缩放参数（_50x50.jpg 等），但保留文件名
      normalizedUrl = normalizedUrl.replace(/_\d+x\d+\.jpg/gi, ".jpg");

      // 移除 .sum、.middle 等后缀
      normalizedUrl = normalizedUrl.replace(/\.sum\.jpg/gi, ".jpg");
      normalizedUrl = normalizedUrl.replace(/\.middle\.jpg/gi, ".jpg");

      // 移除尺寸后缀（_s50、_s100 等），但不影响文件名
      normalizedUrl = normalizedUrl.replace(/_s\d+(?!\d)/gi, "");

      // 处理多个连续的 jpg 扩展名（如 .jpg_.jpg）
      normalizedUrl = normalizedUrl.replace(/\.jpg_+\.jpg/gi, ".jpg");
      normalizedUrl = normalizedUrl.replace(/\.jpg_+$/gi, ".jpg");

      // 确保使用正确的图片服务域名
      if (hostname.includes("taobaocdn.com") || hostname.includes("tbcdn.cn")) {
        normalizedUrl = normalizedUrl.replace(
          /(taobaocdn\.com|tbcdn\.cn)/gi,
          "alicdn.com",
        );
      }

      // 1688 图片特殊处理
      if (platform === "1688" && hostname.includes("1688.com")) {
        normalizedUrl = normalizedUrl
          .replace(/\.sum\.jpg/gi, ".jpg")
          .replace(/\.middle\.jpg/gi, ".jpg")
          .replace(/\?width=\d+&height=\d+/gi, "");
      }

      // 清理查询参数（保留路径）
      normalizedUrl = normalizedUrl.split("?")[0];

      // 确保 URL 以图片扩展名结尾
      if (!normalizedUrl.match(/\.(jpg|jpeg|png|webp|gif)($|\?)/i)) {
        if (originalPath.includes(".jpg")) {
          normalizedUrl = originalPath.split(".jpg")[0] + ".jpg";
        } else if (originalPath.includes(".png")) {
          normalizedUrl = originalPath.split(".png")[0] + ".png";
        } else {
          normalizedUrl = normalizedUrl.split("?")[0] + ".jpg";
        }
      }
    }
  } catch (e) {
    console.warn("[PurchaseX] URL 解析失败:", normalizedUrl);
  }

  return normalizedUrl;
}

// function generateEnglishName(chinese) {
//   const pinyinMap = {
//     颜色: "color",
//     尺寸: "size",
//     规格: "spec",
//     材质: "material",
//   };
//   return pinyinMap[chinese] || chinese.toLowerCase().replace(/[^a-z0-9]/g, "");
// }

// function generateEnglishValue(chinese) {
//   return chinese.toLowerCase().replace(/[^a-z0-9]/g, "_");
// }

/**
 * 从文本中提取价格
 * @param {string} text - 包含价格的文本
 * @param {string} platform - 平台：taobao/tmall/1688
 * @returns {number|null} 价格
 */
function extractPriceFromText(text, platform = "taobao") {
  if (!text) return null;

  const cleanText = text.trim();

  let priceText = cleanText
    .replace(/[￥¥$€£元]/g, "")
    .replace(/(起 | 价|￥|¥|\$|€|£)/g, "")
    .replace(
      /(券后价 | 券后价起 | 活动价 | 促销价 | 原价 | 现价 | 到手价)/g,
      "",
    )
    .replace(/\s+/g, "")
    .trim();

  if (priceText.includes("-") || priceText.includes("~")) {
    const range = priceText.split(/[-~]/);
    priceText = range[0].trim();
  }

  const match = priceText.match(/[\d,]+\.?\d*/);
  if (!match) return null;

  const priceStr = match[0].replace(/,/g, "");
  const price = parseFloat(priceStr);

  if (isNaN(price) || price < 0) return null;

  if (platform === "1688") {
    return price;
  }

  if (platform === "tmall") {
    return price;
  }

  return price;
}

async function waitForDOMChange(selector, options = { timeout: 1000 }) {
  return new Promise((resolve) => {
    const timeout = setTimeout(() => resolve(false), options.timeout);
    const observer = new MutationObserver(() => {
      clearTimeout(timeout);
      observer.disconnect();
      resolve(true);
    });
    const el =
      typeof selector === "string"
        ? document.querySelector(selector)
        : selector;
    if (el) {
      observer.observe(el.parentElement || document.body, {
        childList: true,
        subtree: true,
      });
    } else {
      resolve(false);
    }
  });
}

function gaussianRandom(mean, stddev) {
  let u = 0,
    v = 0;
  while (u === 0) u = Math.random();
  while (v === 0) v = Math.random();
  const num = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
  return num * stddev + mean;
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

const AntiDetection = {
  isCaptchaPresent() {
    const captchaSelectors = [
      "#nc_1_wrapper",
      ".nc-container",
      ".baxia-dialog",
      '[class*="captcha"]',
      '[class*="verify"]',
      "#J_MIDDLEWARE_FRAME_WIDGET",
      ".J_MIDDLEWARE_FRAME_WIDGET",
      "#smartCaptcha",
      '[class*="baxia"]',
      '[class*="slider-verify"]',
    ];
    for (const selector of captchaSelectors) {
      if (document.querySelector(selector)) return true;
    }
    return false;
  },

  isBlocked() {
    const blockKeywords = [
      "访问受限",
      "频繁操作",
      "稍后再试",
      "异常",
      "security",
      "forbidden",
    ];
    const bodyText = document.body?.innerText?.toLowerCase() || "";
    return blockKeywords.some((kw) => bodyText.includes(kw));
  },

  check() {
    if (this.isCaptchaPresent()) {
      Logger.warn("反爬检测", "检测到验证码，暂停操作");
      return "captcha";
    }
    if (this.isBlocked()) {
      Logger.warn("反爬检测", "页面可能被限制访问");
      return "blocked";
    }
    return "ok";
  },
};

function scrollToElement(el) {
  if (!el) return;
  const rect = el.getBoundingClientRect();
  const inViewport = rect.top >= 0 && rect.bottom <= window.innerHeight;
  if (!inViewport) {
    el.scrollIntoView({ behavior: "smooth", block: "center" });
  }
}

function simulateMouseEvent(el, eventType) {
  const rect = el.getBoundingClientRect();
  const eventInit = {
    bubbles: true,
    cancelable: true,
    view: window,
    clientX: rect.left + rect.width * (0.3 + Math.random() * 0.4),
    clientY: rect.top + rect.height * (0.3 + Math.random() * 0.4),
  };
  el.dispatchEvent(new MouseEvent(eventType, eventInit));
}

const BehaviorSimulator = {
  getSpeedProfile: (total) => {
    if (total <= 10) {
      return {
        name: "fast",
        maxCombinations: total,
        behaviorFrequency: 5,
        baseDelay: 400,
      };
    } else if (total <= 30) {
      return {
        name: "normal",
        maxCombinations: total,
        behaviorFrequency: 8,
        baseDelay: 500,
      };
    } else if (total <= 80) {
      return {
        name: "careful",
        maxCombinations: 80,
        behaviorFrequency: 6,
        baseDelay: 600,
      };
    } else {
      return {
        name: "stealth",
        maxCombinations: 60,
        behaviorFrequency: 4,
        baseDelay: 800,
      };
    }
  },

  clickWithHumanDelay: async (el, speedProfile) => {
    scrollToElement(el);

    const hoverDelay = clamp(gaussianRandom(80, 30), 30, 150);
    await delay(hoverDelay);
    simulateMouseEvent(el, "mouseover");

    const downDelay = clamp(gaussianRandom(60, 20), 20, 120);
    await delay(downDelay);
    simulateMouseEvent(el, "mousedown");

    const upDelay = clamp(gaussianRandom(40, 15), 15, 80);
    await delay(upDelay);
    simulateMouseEvent(el, "mouseup");
    simulateMouseEvent(el, "click");

    const baseDelay = speedProfile?.baseDelay || 500;
    const clickDelay = clamp(
      gaussianRandom(baseDelay, baseDelay * 0.3),
      baseDelay * 0.4,
      baseDelay * 2,
    );
    await delay(clickDelay);
  },

  randomPageBehavior: async () => {
    const roll = Math.random();
    if (roll < 0.3) {
      const scrollY = 50 + Math.random() * 150;
      window.scrollBy({ top: scrollY, behavior: "smooth" });
      await delay(200 + Math.random() * 300);
      window.scrollBy({ top: -scrollY * 0.5, behavior: "smooth" });
    } else if (roll < 0.5) {
      await delay(300 + Math.random() * 500);
    }
  },

  isInBackground: () => document.hidden,

  waitForDelay: async (min = 400, max = 900) => {
    const mean = (min + max) / 2;
    const stddev = (max - min) / 6;
    const randomDelay = clamp(gaussianRandom(mean, stddev), min, max);
    await delay(randomDelay);
  },

  microBreak: async () => {
    if (Math.random() < 0.15) {
      const breakTime = clamp(gaussianRandom(1500, 500), 800, 3000);
      Logger.info("微停顿", `${Math.round(breakTime)}ms`);
      await delay(breakTime);
    }
  },
};

// 将工具函数和对象挂载到全局对象，使其在其他文件中可访问
if (typeof window !== "undefined") {
  window.Logger = Logger;
  window.delay = delay;
  window.normalizeImageUrl = normalizeImageUrl;
  // window.generateEnglishName = generateEnglishName;  utils_enhanced实现
  // window.generateEnglishValue = generateEnglishValue; utils_enhanced实现
  window.extractPriceFromText = extractPriceFromText;
  window.waitForDOMChange = waitForDOMChange;
  window.BehaviorSimulator = BehaviorSimulator;
  window.AntiDetection = AntiDetection;
  window.gaussianRandom = gaussianRandom;
  window.clamp = clamp;
  window.scrollToElement = scrollToElement;
  window.simulateMouseEvent = simulateMouseEvent;
}
