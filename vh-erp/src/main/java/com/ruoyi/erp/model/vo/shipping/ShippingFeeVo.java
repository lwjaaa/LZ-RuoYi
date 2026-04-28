package com.ruoyi.erp.model.vo.shipping;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 4px运费查询结果VO
 * 
 * @author lwj
 * @date 2026-04-26
 */
@Data
public class ShippingFeeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 4PX单号
     */
    @JSONField(name = "4px_tracking_no")
    private String trackingNo;

    /**
     *
     * 计费重/体积重(若是泡货，则返回相应的体积重；若非泡货，则不返回)
     */
    @JSONField(name = "charge_weight")
    private BigDecimal chargeWeight;

    /**
     * 预计送达天数(天)，如：7-12
     */
    @JSONField(name = "estimated_time")
    private String estimatedTime;

    /**
     * 轨迹是否可跟踪(Y/N)
     */
    @JSONField(name = "is_show_track")
    private String isShowTrack;

    /**
     * 是否泡货(Y/N)
     */
    @JSONField(name = "is_volume_cargo")
    private String isVolumeCargo;

    /**
     * 服务商单号
     */
    @JSONField(name = "logistics_channel_no")
    private String logisticsChannelNo;

    /**
     * 物流产品代码
     */
    @JSONField(name = "logistics_product_code")
    private String logisticsProductCode;

    /**
     *
     * 总费用(CNY)(单位：元)
     */
    @JSONField(name = "lump_sum_fee")
    private BigDecimal lumpSumFee;

    /**
     * 客户单号
     */
    @JSONField(name = "ref_no")
    private String refNo;

    /**
     * 备注信息
     */
    @JSONField(name = "remarks")
    private String remarks;
}
