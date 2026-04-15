package com.ruoyi.erp.model.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {

    private String baseCurrency;

    private String targetCurrency;

    private BigDecimal rate;

    private String rateDate;
}
