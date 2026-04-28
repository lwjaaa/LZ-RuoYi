 package com.ruoyi.erp.utils;

 import cn.hutool.http.HttpUtil;
 import com.alibaba.fastjson2.JSON;
 import com.alibaba.fastjson2.JSONObject;
 import com.fpx.api.model.AffterentParam;
 import com.fpx.api.utils.SignUtil;
 import com.ruoyi.common.exception.ServiceException;
 import com.ruoyi.erp.config.FpxApiConfig;
 import com.ruoyi.erp.model.vo.shipping.ShippingFeeVo;
 import jakarta.annotation.Resource;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Component;

 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

/**
 * 4px API SDK工具类
 * <p>
 * 封装4px物流API的调用，包括：
 * 1. Token获取和刷新
 * 2. 运费查询
 * 3. 其他物流接口调用
 * </p>
 * 
 * @author lwj
 * @date 2026-04-26
 */
@Slf4j
@Component
public class FpxApiUtil {

    @Resource
    private FpxApiConfig fpxApiConfig;

    public static final String VERSION = "1.0.0";
    public static final String FORMAT = "json";
    public static final String LANGUAGE = "cn";
    public static final String RESPONSE_SUCCESS_CODE = "1";
     public static final String API_ADDRESS = "https://open.4px.com/router/api/service";
    /**
     * 查询物流
     */
    private static final String METHOD_QUERY_LOGISTICS_LIST = "ds.xms.logistics_product.getlist";
    /**
     * 查询运费
     */
    private static final String METHOD_QUERY_SHIPPING_FEE = "ds.xms.estimated_cost.get";


    /**
     * 查询运费
     * <p>
     * 根据包裹尺寸和重量查询4px物流运费
     * </p>
     * 
     * @param pkWidth 包装宽度(cm)
     * @param pkHeight 包装高度(cm)
     * @param pkLength 包装长度(cm)
     * @param pkWeight 重量(g)
     * @param countryCode 国家代码（如US、CN等）
     * @param postalCode 邮政编码
     * @return 运费信息VO对象列表
     */
    public List<ShippingFeeVo> queryShippingFee(Integer pkWidth, Integer pkHeight,
                                       Integer pkLength, Integer pkWeight,
                                       String countryCode, String postalCode, List<String> logisticsCode) {
        log.info("开始查询4px运费，尺寸: {}x{}x{}cm, 重量: {}g, 国家: {}, 邮编: {}",
                pkWidth, pkHeight, pkLength, pkWeight, countryCode, postalCode);
        
        try {
            // 构建公共参数
            AffterentParam commonParam = this.buildCommonParam(METHOD_QUERY_SHIPPING_FEE);
            
            // 构建请求参数
            Map<String, Object> paramMap = new HashMap<>();
            // 请求单号(支持4PX单号、面单号、客户单号)；若填写了请求单号，则其余请求字段将不会生效
            paramMap.put("request_no", "");
            // 目的国家二字码（未填写请求单号时，必填）
            paramMap.put("country_code", countryCode != null ? countryCode : "US");
            // 实重(单位g，未填写请求单号时，必填)，填写实重需小于1000000g
            paramMap.put("weight", pkWeight);
            // 长(单位cm)；长宽高3个字段，填写了其中一个字段，其他2个字段需必填；小于1000cm并且保留2位小数
            paramMap.put("length", pkLength);
            // 宽(单位cm)；长宽高3个字段，填写了其中一个字段，其他2个字段需必填；小于1000cm并且保留2位小数
            paramMap.put("width", pkWidth);
            // 高(单位cm)；长宽高3个字段，填写了其中一个字段，其他2个字段需必填；小于1000cm并且保留2位小数
            paramMap.put("height", pkHeight);
            // 货物类型(包裹：P；文件：D）默认值：P；
            paramMap.put("cargocode", "P");
            // 物流产品代码列表；如填写了产品代码，则只会返回填写的产品代码的试算结果，最大200个产品
            paramMap.put("logistics_product_code", logisticsCode);
            // 可以根据需要添加更多参数，如物流服务代码等
            log.info("查询4px运费参数：{}, 公共参数：{}",paramMap, commonParam);
            String response = apiJsongPost(commonParam, paramMap);

            log.info("查询4px运费, 返回参数：{}",response);
            
            // 将JSON字符串转换为ShippingFeeVo对象列表
            List<ShippingFeeVo> shippingFeeList = convertToShippingFeeVoList(response);
            
            if (shippingFeeList != null && !shippingFeeList.isEmpty()) {
                ShippingFeeVo firstFee = shippingFeeList.get(0);
                log.info("运费查询成功，物流产品: {}, 费用: {}元, 时效: {}天",
                        firstFee.getLogisticsProductCode(),
                        firstFee.getLumpSumFee(),
                        firstFee.getEstimatedTime());
            } else {
                log.warn("运费查询结果为空");
            }
            
            return shippingFeeList;
            
        } catch (Exception e) {
            log.error("查询4px运费失败: {}", e.getMessage(), e);
            throw new ServiceException("查询4px运费失败: " + e.getMessage());
        }
    }

    /**
     * 将4px API返回的JSON字符串转换为ShippingFeeVo对象列表
     * 
     * @param jsonResponse API返回的JSON字符串（已经是data字段内的数组）
     * @return ShippingFeeVo对象列表
     */
    private List<ShippingFeeVo> convertToShippingFeeVoList(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new ServiceException("4px API返回数据为空");
        }
        
        try {
            // 直接解析为ShippingFeeVo列表，FastJSON2自动处理下划线转驼峰
            List<ShippingFeeVo> voList = JSON.parseArray(jsonResponse, ShippingFeeVo.class);
            
            if (voList == null || voList.isEmpty()) {
                throw new ServiceException("4px API返回数据为空或格式错误");
            }
            
            return voList;
            
        } catch (Exception e) {
            log.error("解析4px运费响应失败: {}", e.getMessage(), e);
            throw new ServiceException("解析4px运费响应失败: " + e.getMessage());
        }
    }


    /**
     * 查询物流
     */
    public JSONObject getLogisticsList() {
        log.info("开始查询4px物流商");

        try {

            // 构建请求参数
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("transport_mode", 1);
            log.info("查询4px物流商参数：{}",paramMap);
            // 构建公共参数
            AffterentParam commonParam = this.buildCommonParam(METHOD_QUERY_LOGISTICS_LIST);
            String response = apiJsongPost(commonParam, paramMap);
            log.info("查询4px物流商返回参数：{}",response);
            return JSONObject.parseObject(response);

        } catch (Exception e) {
            log.error("查询4px物流商失败: {}", e.getMessage(), e);
            throw new ServiceException("查询4px物流商失败: " + e.getMessage());
        }
    }

    private AffterentParam buildCommonParam(String method) {
        AffterentParam param = new AffterentParam();
        param.setMethod(method);
        param.setAppKey(fpxApiConfig.getAppKey());
        param.setVersion(VERSION);
        param.setFormat(FORMAT);
        param.setAppSecret(fpxApiConfig.getAppSecret());
        param.setLanguage(LANGUAGE);
        return param;
    }


    /**
     * 4px API POST请求方法
     * <p>
     * 调用4px官方SDK的API接口，发送POST请求并返回响应结果
     * </p>
     *
     * @param param 传入参数对象，包含appKey、method、version等公共参数
     * @param paramMap 业务参数Map，会被转换为JSON字符串作为请求体
     * @return API响应字符串，如果请求失败或返回为空则返回null
     * @throws ServiceException 当参数为空或HTTP请求异常时抛出
     */
    public static String apiJsongPost(AffterentParam param, Map<String, Object> paramMap) {
        // 1. 参数校验
        if (param == null) {
            log.error("4px API请求失败：param参数不能为空");
            throw new ServiceException("4px API请求参数param不能为空");
        }
        if (paramMap == null) {
            log.error("4px API请求失败：paramMap参数不能为空");
            throw new ServiceException("4px API请求参数paramMap不能为空");
        }

        try {
            // 2. 构建请求体JSON
            String bodyJsonStr = getBodyJson(paramMap);
            if (bodyJsonStr == null || bodyJsonStr.isEmpty()) {
                log.warn("4px API请求体为空，paramMap: {}", paramMap);
            }

            // 5. 生成时间戳和签名
            long timestamp = System.currentTimeMillis();
            String sign = SignUtil.getSingByParam(param, bodyJsonStr, timestamp);
            if (sign == null || sign.isEmpty()) {
                log.error("4px API签名生成失败");
                throw new ServiceException("4px API签名生成失败");
            }

            // 6. 构建完整请求URL（包含查询参数）
            String fullUrl = buildRequestUrl(param, API_ADDRESS, timestamp, sign);

            // 7. 记录请求日志
            log.debug("4px API请求 - URL: {}, Method: {}, Body: {}", fullUrl, param.getMethod(), bodyJsonStr);

            // 8. 发送HTTP POST请求（设置超时时间为30秒）
            String response = HttpUtil.createPost(fullUrl)
                    .body(bodyJsonStr)
                    .timeout(30000)
                    .execute()
                    .body();

            // 9. 记录响应日志
            if (response == null || response.isEmpty()) {
                throw new ServiceException("返回参数为空");
            } else {
                log.debug("4px API响应 - Method: {}, Response: {}", param.getMethod(), response);
            }
            JSONObject jsonObject = JSONObject.parseObject(response);
            if(!RESPONSE_SUCCESS_CODE.equals(jsonObject.getString("result"))){
                throw new ServiceException(jsonObject.getString("msg"));
            }

            return jsonObject.getString("data");

        } catch (Exception e) {
            log.error("4px API请求异常 - Method: {}, Error: {}",
                    param.getMethod(), e.getMessage(), e);
            throw new ServiceException("4px API请求失败: " + e.getMessage());
        }
    }

    /**
     * 构建完整的请求URL（包含查询参数）
     *
     * @param param 传入参数对象
     * @param baseUrl 基础URL
     * @param timestamp 时间戳
     * @param sign 签名
     * @return 完整的请求URL
     */
    private static String buildRequestUrl(AffterentParam param, String baseUrl, long timestamp, String sign) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?method=").append(param.getMethod());
        urlBuilder.append("&app_key=").append(param.getAppKey());
        urlBuilder.append("&v=").append(param.getVersion());
        urlBuilder.append("&timestamp=").append(timestamp);
        urlBuilder.append("&format=").append(param.getFormat());
        
        // accessToken可能为空，需要判断
        if (param.getAccessToken() != null && !param.getAccessToken().isEmpty()) {
            urlBuilder.append("&access_token=").append(param.getAccessToken());
        }
        
        urlBuilder.append("&sign=").append(sign);
        urlBuilder.append("&language=").append(param.getLanguage());
        
        return urlBuilder.toString();
    }

    private static String getBodyJson(Map<String, Object> paramMap) {
        JSONObject jsonObject = new JSONObject(paramMap);
        return jsonObject.toString();
    }

}
