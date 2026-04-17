declare module "@/utils/ruoyi.js" {
  export function parseTime(time: string | number | Date, pattern?: string): string | null;
  export function addDateRange(params: Record<string, any>, dateRange: string[] | null, propName?: string): Record<string, any>;
  export function selectDictLabel(datas: { dictValue: string; dictLabel: string }[], value: string): string;
  export function selectDictLabels(datas: { dictValue: string; dictLabel: string }[], value: string, separator?: string): string;
  export function handleTree(data: any[], id?: string, parentId?: string, children?: string): any[];
  export function getDictLabel(dictType: string, dictValue: string): string;
  export function resetForm(ref: any): void;
  export function tansParams(params: Record<string, any>): string;
  export function getNormalPath(path: string): string;
}

declare module "@/utils/auth.js" {
  export function getToken(): string;
  export function setToken(token: string): void;
  export function removeToken(): void;
}

declare module "@/utils/validate.js" {
  export function isExternal(path: string): boolean;
  export function validUsername(str: string): boolean;
  export function validURL(url: string): boolean;
  export function validLowerCase(str: string): boolean;
  export function validUpperCase(str: string): boolean;
  export function validAlphabets(str: string): boolean;
  export function validEmail(email: string): boolean;
  export function isString(str: any): str is string;
  export function isArray(arg: any): arg is any[];
}

declare module "@/utils/request.js" {
  import type { AxiosRequestConfig, AxiosPromise } from "axios";

  interface ApiResponse<T = unknown> {
    code: number;
    msg: string;
    data: T;
    rows?: T[];
    total?: number;
  }

  interface RequestConfig extends AxiosRequestConfig {
    isToken?: boolean;
    repeatSubmit?: boolean;
    interval?: number;
  }

  export default function request<T = unknown>(config: RequestConfig): AxiosPromise<ApiResponse<T>>;

  export function download(url: string, params: Record<string, unknown>, filename: string, config?: RequestConfig): Promise<void>;
}

declare module "@/utils/dict.js" {
  export interface DictData {
    dictValue: string;
    dictLabel: string;
    cssClass?: string;
    listClass?: string;
    isDefault?: string;
    status?: string;
    default?: boolean;
    remark?: string;
  }

  export function useDict(...dictTypes: string[]): Record<string, DictData[]>;
}

declare module "@/utils/permission.js" {
  export function checkPermi(value: string[]): boolean;
  export function checkRole(value: string[]): boolean;
}

declare module "@/utils/index.js" {
  export function getNormalPath(path: string): string;
  export function deepClone(source: any): any;
  export function objectMerge(target: any, source: any): any;
  export function debounce(func: Function, wait: number, immediate?: boolean): Function;
  export function throttle(func: Function, wait: number): Function;
  export function param2Obj(url: string): Record<string, any>;
  export function toggleClass(element: Element, className: string): void;
  export function getTime(type: string): number;
  export function deepMerge(target: any, source: any): any;
}

declare module "@/utils/theme.js" {
  export function getThemeCluster(theme: string): string[];
  export function updateStyle(style: string, oldCluster: string[], newCluster: string[]): string;
  export function getCSSVar(type: string, value: string): string;
  export function tintColor(color: string, tint: number): string;
}

declare module "@/utils/scroll-to.js" {
  export default function scrollTo(to: number, duration: number, callback?: () => void): void;
}

declare module "@/utils/jsencrypt.js" {
  export function encrypt(data: string): string;
  export function decrypt(data: string): string;
}

declare module "@/utils/errorCode.js" {
  export const errorCode: Record<string, string>;
}

declare module "@/utils/dynamicTitle.js" {
  export function setDynamicTitle(title: string): void;
  export function getDynamicTitle(): string;
}
