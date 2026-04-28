import request from '@/utils/request'

/**
 * 批量获取多个字段的提示词列表
 * @param fields - 字段配置列表，每个配置包含 fieldName 和 limit
 * @returns Promise
 * 
 * 使用示例：
 * batchGetSuggestions([
 *   { fieldName: 'size', limit: 30 },
 *   { fieldName: 'material', limit: 30 },
 *   { fieldName: 'note', limit: 30 },
 *   { fieldName: 'noteCn', limit: 30 }
 * ])
 */
export function batchGetSuggestions(fields: Array<{ fieldName: string; limit?: number }>) {
  return request({
    url: '/erp/formSuggestion/batchList',
    method: 'post',
    data: {
      fields: fields
    }
  })
}

/**
 * 批量更新提示词使用记录
 * @param updates - 更新列表，每个元素包含 fieldName, oldValue, newValue
 * @returns Promise
 * 
 * 使用示例：
 * batchUpdateUsage([
 *   { fieldName: 'size', oldValue: 'M', newValue: 'L' },
 *   { fieldName: 'material', oldValue: '棉', newValue: '丝绸' }
 * ])
 */
export function batchUpdateUsage(updates: Array<{ fieldName: string; oldValue?: string; newValue: string }>) {
  return request({
    url: '/erp/formSuggestion/batchUpdateUsage',
    method: 'post',
    data: {
      updates: updates
    }
  })
}
