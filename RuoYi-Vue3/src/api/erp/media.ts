import request from "@/utils/request";
import type {
  ApiResponse,
  Media,
  PageQuery,
  ShippingCalcData,
  ShippingResult,
} from "@/types/erp";

export interface MediaQuery extends PageQuery {
  productId?: number;
  filename?: string;
  mediaContentType?: string;
}

export function listMedia(query: MediaQuery): Promise<ApiResponse<Media[]>> {
  return request({
    url: "/erp/media/list",
    method: "get",
    params: query,
  });
}

export function getMedia(mediaId: number): Promise<ApiResponse<Media>> {
  return request({
    url: "/erp/media/" + mediaId,
    method: "get",
  });
}

export function addMedia(data: Media): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/media",
    method: "post",
    data: data,
  });
}

export function updateMedia(data: Media): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/media",
    method: "put",
    data: data,
  });
}

export function delMedia(mediaId: number): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/media/" + mediaId,
    method: "delete",
  });
}

export function importMedia(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/media/importData",
    method: "post",
    data: data,
  });
}

export function importTemplateMedia(): Promise<Blob> {
  return request({
    url: "/erp/media/importTemplate",
    method: "post",
    responseType: "blob",
  });
}

export interface ScanMediaResult {
  scannedCount: number;
  importedCount: number;
  errors?: string[];
}

export function scanMedia(params: {
  dirPath: string;
  productId?: number | null;
}): Promise<ApiResponse<Media[]>> {
  return request({
    url: "/erp/media/scan",
    method: "get",
    params: { dirPath: params.dirPath, productId: params.productId },
  });
}

export function calculateShipping(
  data: ShippingCalcData,
): Promise<ApiResponse<ShippingResult>> {
  return request({
    url: "/vh-erp/media/calculateShipping",
    method: "post",
    data: data,
  });
}
