import request from '@/utils/request'

/**
 * 查询运费
 * @param {Object} data - 运费查询参数
 * @param {number} data.pkWidth - 包装宽度(cm)
 * @param {number} data.pkHeight - 包装高度(cm)
 * @param {number} data.pkLength - 包装长度(cm)
 * @param {number} data.pkWeight - 重量(g)
 * @param {string} [data.countryCode='US'] - 国家代码
 * @param {string} [data.postalCode='10001'] - 邮政编码
 * @returns {Promise} 运费信息
 */
export function calculateShipping(data) {
  return request({
    url: '/erp/shipping/calculate',
    method: 'post',
    data: data
  })
}

/**
 * 批量查询运费
 * @param {Array} dataList - 运费查询参数列表
 * @returns {Promise} 运费信息列表
 */
export function batchCalculateShipping(dataList) {
  return request({
    url: '/erp/shipping/batch-calculate',
    method: 'post',
    data: dataList
  })
}
