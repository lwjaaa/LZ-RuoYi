export type Platform = "taobao" | "tmall" | "1688";

export type TaskStatus = "pending" | "fetching" | "completed" | "failed";

export interface FetchTask {
  id: string;
  title: string;
  url: string;
  platform: Platform;
  status: TaskStatus;
  totalVariants: number;
  currentVariants: number;
  progress: number;
  startTime: number;
}

export interface ProductOptionValue {
  chineseValue: string;
  englishValue: string;
}

export interface ProductOption {
  chineseName: string;
  englishValue: string;
  values: ProductOptionValue[];
}

export interface VariantOptionValue {
  chineseName: string;
  englishName: string;
  chineseValue: string;
  englishValue: string;
}

export interface ProductVariant {
  purchasePrice: number;
  mediaUrl: string;
  isActiveAvailable?: string;
  optionValueList: VariantOptionValue[];
}

export interface Product {
  id: string;
  productName: string;
  sourceUrl: string;
  tagIds: number[];
  mediaUrlList: string[];
  optionList: ProductOption[];
  productVariantList: ProductVariant[];
}

export interface TagNode {
  tagId: number;
  tagName: string;
  tagCode: string;
  tagType: "MENU" | "OTHER";
  sortOrder: number;
  parentId: number;
  ancestors: string;
  menuLevel: number;
  spuPrefix: string;
  currentMaxSeq: number;
  children: TagNode[];
}

export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}
