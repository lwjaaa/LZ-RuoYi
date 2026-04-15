package com.ruoyi.erp.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("erp_exchange_rate")
public class ExchangeRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("base_currency")
    private String baseCurrency;

    @TableField("target_currency")
    private String targetCurrency;

    @TableField("rate")
    private BigDecimal rate;

    @TableField("rate_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String rateDate;
}
