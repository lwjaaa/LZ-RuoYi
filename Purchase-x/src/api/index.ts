import type { ApiResponse, TagNode } from "@/types";

const API_BASE_URL = "http://127.0.0.1:8080/api/erp";

export async function fetchTagTree(forceRefresh = false): Promise<TagNode[]> {
  if (!forceRefresh) {
    const cached = await chrome.storage.local.get("tagTree");
    if (cached.tagTree && cached.tagTree.length > 0) {
      return cached.tagTree;
    }
  }

  try {
    const response = await fetch(`${API_BASE_URL}/tag/treelist/ALL`);
    const result: ApiResponse<TagNode[]> = await response.json();

    if (result.code === 200 && result.data) {
      await chrome.storage.local.set({ tagTree: result.data });
      return result.data;
    }

    throw new Error(result.msg || "获取标签失败");
  } catch (error) {
    console.error("[API] Failed to fetch tag tree:", error);
    const cached = await chrome.storage.local.get("tagTree");
    return cached.tagTree || [];
  }
}

export async function generateSPU(tag: TagNode): Promise<string> {
  if (tag.tagType !== "MENU" || tag.children.length > 0) {
    return "";
  }

  const prefix = tag.spuPrefix;
  if (!prefix) {
    return "";
  }

  const newSeq = tag.currentMaxSeq + 1;
  const seqStr = newSeq.toString().padStart(4, "0");

  return `${prefix}${seqStr}`;
}

export async function syncProductToErp(product: {
  productName: string;
  tagIds: number[];
  sourceUrl: string;
  spu: string;
  mediaUrlList: string[];
  optionList: Array<{
    chineseName: string;
    englishValue: string;
    values: Array<{ chineseValue: string; englishValue: string }>;
  }>;
  productVariantList: Array<{
    purchasePrice: number;
    mediaUrl: string;
    isActiveAvailable?: string;
    optionValueList: Array<{
      chineseName: string;
      englishName: string;
      chineseValue: string;
      englishValue: string;
    }>;
  }>;
}): Promise<number> {
  const response = await fetch(`${API_BASE_URL}/product/selectionInfo`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(product),
  });

  const result: ApiResponse<number> = await response.json();

  if (result.code === 200) {
    return result.data;
  }

  throw new Error(result.msg || "同步商品失败");
}
