import type { Product, Platform } from "@/types";

declare class BaseExtractor {
  platform: string;
  extractAll(): Promise<{
    productName: string;
    sourceUrl: string;
    mediaUrlList: string[];
    optionList: Array<{
      chineseName: string;
      englishValue: string;
      values: Array<{
        chineseValue: string;
        englishValue: string;
        isDisabled?: boolean;
      }>;
    }>;
    productVariantList: Array<{
      purchasePrice: number | null;
      mediaUrl: string;
      isActiveAvailable?: string;
      optionValueList: Array<{
        chineseName: string;
        englishName: string;
        chineseValue: string;
        englishValue: string;
      }>;
    }>;
  }>;
}

declare class TaobaoExtractor extends BaseExtractor {}
declare class TmallExtractor extends BaseExtractor {}
declare class AlibabaExtractor extends BaseExtractor {}

function detectPlatform(): Platform {
  const url = window.location.href;
  if (url.includes("taobao.com")) return "taobao";
  if (url.includes("tmall.com")) return "tmall";
  if (url.includes("1688.com")) return "1688";
  return "taobao";
}

function getExtractor(platform: Platform): BaseExtractor | null {
  switch (platform) {
    case "taobao":
      return typeof TaobaoExtractor !== "undefined"
        ? new TaobaoExtractor()
        : null;
    case "tmall":
      return typeof TmallExtractor !== "undefined"
        ? new TmallExtractor()
        : null;
    case "1688":
      return typeof AlibabaExtractor !== "undefined"
        ? new AlibabaExtractor()
        : null;
    default:
      return null;
  }
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

function transformToProduct(
  data: Awaited<ReturnType<BaseExtractor["extractAll"]>>,
  _platform: Platform,
): Product {
  const product: Product = {
    id: generateId(),
    productName: data.productName,
    sourceUrl: data.sourceUrl,
    tagIds: [],
    mediaUrlList: data.mediaUrlList,
    optionList: data.optionList.map((opt) => ({
      chineseName: opt.chineseName,
      englishValue: opt.englishValue,
      values: opt.values.map((v) => ({
        chineseValue: v.chineseValue,
        englishValue: v.englishValue,
      })),
    })),
    productVariantList: data.productVariantList.map((variant) => ({
      purchasePrice: Math.round((variant.purchasePrice ?? 0) * 100),
      mediaUrl: variant.mediaUrl,
      isActiveAvailable: variant.isActiveAvailable,
      optionValueList: variant.optionValueList,
    })),
  };
  return product;
}

async function sendProgress(
  taskId: string,
  total: number,
  current: number,
): Promise<void> {
  try {
    await chrome.runtime.sendMessage({
      type: "FETCH_PROGRESS",
      payload: { taskId, totalVariants: total, currentVariants: current },
    });
    console.log("[PurchaseX] Progress sent:", taskId, total, current);
  } catch (error) {
    console.error("[PurchaseX] Failed to send progress:", error);
  }
}

async function sendComplete(taskId: string, product: Product): Promise<void> {
  try {
    console.log("[PurchaseX] Sending complete:", taskId, product);
    await chrome.runtime.sendMessage({
      type: "FETCH_COMPLETE",
      payload: { taskId, product },
    });
    console.log("[PurchaseX] Complete sent:", taskId, product);
  } catch (error) {
    console.error("[PurchaseX] Failed to send complete:", error);
  }
}

async function sendError(taskId: string, errorMessage: string): Promise<void> {
  try {
    await chrome.runtime.sendMessage({
      type: "FETCH_ERROR",
      payload: { taskId, errorMessage },
    });
  } catch (error) {
    console.error("[PurchaseX] Failed to send error:", error);
  }
}

async function fetchProduct(taskId: string): Promise<void> {
  const platform = detectPlatform();
  console.log(`[PurchaseX] Starting fetch for ${platform}, taskId: ${taskId}`);

  const extractor = getExtractor(platform);
  if (!extractor) {
    console.error("[PurchaseX] No extractor found for platform:", platform);
    await sendError(taskId, `不支持的平台: ${platform}`);
    return;
  }

  try {
    const data = await extractor.extractAll();
    // 模拟获取数据
    // const data: Awaited<ReturnType<BaseExtractor["extractAll"]>> = {
    //   productName: "测试商品",
    //   sourceUrl: "https://example.com/product",
    //   mediaUrlList: ["https://example.com/image1.jpg"],
    //   optionList: [
    //     {
    //       chineseName: "颜色",
    //       englishValue: "Color",
    //       values: [
    //         {
    //           chineseValue: "蓝色",
    //           englishValue: "Blue",
    //         },
    //       ],
    //     },
    //   ],
    //   productVariantList: [
    //     {
    //       purchasePrice: 100,
    //       mediaUrl: "https://example.com/image1.jpg",
    //       isActiveAvailable: "1",
    //       optionValueList: [
    //         {
    //           chineseName: "颜色",
    //           englishValue: "Blue",
    //           chineseValue: "蓝色",
    //           englishName: "Blue",
    //         },
    //       ],
    //     },
    //   ],
    // };
    console.log("[PurchaseX] Extracted data:", data);

    const totalVariants = data.productVariantList.length;
    let currentVariants = 0;

    for (const _variant of data.productVariantList) {
      currentVariants++;
      if (currentVariants % 5 === 0 || currentVariants === totalVariants) {
        await sendProgress(taskId, totalVariants, currentVariants);
      }
    }

    const product = transformToProduct(data, platform);
    await sendComplete(taskId, product);

    console.log("[PurchaseX] Fetch completed successfully");
  } catch (error) {
    console.error("[PurchaseX] Fetch failed:", error);
    await sendError(
      taskId,
      error instanceof Error ? error.message : "Unknown error",
    );
  }
}

console.log("[PurchaseX] Content script loaded");

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  console.log("[PurchaseX] Received message:", message);

  if (message.type === "START_FETCH") {
    const taskId = message.payload?.taskId;
    if (taskId) {
      sendResponse({ status: "ok" });
      fetchProduct(taskId).catch((error) => {
        console.error("[PurchaseX] Unhandled error in fetchProduct:", error);
      });
    } else {
      sendResponse({ status: "error", message: "Missing taskId" });
    }
    return true;
  }

  if (message.type === "PING") {
    sendResponse({ status: "ok", platform: detectPlatform() });
    return true;
  }

  sendResponse({ status: "error", message: "Unknown message type" });
  return true;
});
