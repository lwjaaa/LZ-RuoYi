import request from "@/utils/request";
import type { ApiResponse, TagDictMenu, DragNodeData } from "@/types/erp";

export interface TagQuery {
  tagName?: string;
  tagType?: string;
  parentId?: number;
}

export function listTag(query: TagQuery): Promise<ApiResponse<TagDictMenu[]>> {
  return request({
    url: "/erp/tag/list",
    method: "get",
    params: query,
  });
}

export function treeList(type: string): Promise<ApiResponse<TagDictMenu[]>> {
  return request({
    url: "/erp/tag/treelist/" + type,
    method: "get",
  });
}

export function getTag(tagId: number): Promise<ApiResponse<TagDictMenu>> {
  return request({
    url: "/erp/tag/" + tagId,
    method: "get",
  });
}

export function addTag(data: TagDictMenu): Promise<ApiResponse<number>> {
  return request({
    url: "/erp/tag",
    method: "post",
    data: data,
  });
}

export function updateTag(data: TagDictMenu): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag",
    method: "put",
    data: data,
  });
}

export function dragNode(data: DragNodeData): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag/dragNode",
    method: "post",
    data: data,
  });
}

export function delTag(tagId: number): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag/" + tagId,
    method: "delete",
  });
}

export function importTag(data: FormData): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag/importData",
    method: "post",
    data: data,
  });
}

export function importTemplateTag(): Promise<Blob> {
  return request({
    url: "/erp/tag/importTemplate",
    method: "post",
    responseType: "blob",
  });
}

export function top(id: number): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag/top/" + id,
    method: "post",
  });
}

export function refreshCache(): Promise<ApiResponse<void>> {
  return request({
    url: "/erp/tag/refreshCache",
    method: "delete",
  });
}
