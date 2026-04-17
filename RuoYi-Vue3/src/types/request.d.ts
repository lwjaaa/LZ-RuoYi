import type { AxiosRequestConfig, AxiosPromise } from "axios";

export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
  rows?: T[];
  total?: number;
}

export interface RequestConfig extends AxiosRequestConfig {
  isToken?: boolean;
  repeatSubmit?: boolean;
  interval?: number;
}

export interface DownloadConfig {
  url: string;
  params?: Record<string, unknown>;
  filename: string;
  config?: RequestConfig;
}

declare function request<T = unknown>(
  config: RequestConfig,
): AxiosPromise<ApiResponse<T>>;

export declare function download(
  url: string,
  params: Record<string, unknown>,
  filename: string,
  config?: RequestConfig,
): Promise<void>;

export default request;
