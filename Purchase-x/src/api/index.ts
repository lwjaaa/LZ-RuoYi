import type { ApiResponse, TagNode } from "@/types";

const STORAGE_KEY = "purchasex_server_domain";
const DEFAULT_API_BASE_URL = "http://127.0.0.1:8080";

function getApiBaseUrl(): string {
  const savedDomain = localStorage.getItem(STORAGE_KEY);
  return savedDomain || DEFAULT_API_BASE_URL;
}

export function setApiBaseUrl(domain: string): void {
  if (domain) {
    localStorage.setItem(STORAGE_KEY, domain);
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export function getSavedDomain(): string | null {
  return localStorage.getItem(STORAGE_KEY);
}

export function hasValidDomain(): boolean {
  return !!localStorage.getItem(STORAGE_KEY);
}

export async function fetchTagTree(forceRefresh = false): Promise<TagNode[]> {
  if (!forceRefresh) {
    const cached = await chrome.storage.local.get("tagTree");
    if (cached.tagTree && cached.tagTree.length > 0) {
      return cached.tagTree;
    }
  }

  const apiBaseUrl = getApiBaseUrl();

  try {
    const response = await fetch(`${apiBaseUrl}/api/erp/tag/treelist/ALL`);
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

export async function syncProductToErp(product: {
  productName: string;
  tagIds: number[];
  sourceUrl: string;
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
  const apiBaseUrl = getApiBaseUrl();

  const response = await fetch(`${apiBaseUrl}/api/erp/product/selectionInfo`, {
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
