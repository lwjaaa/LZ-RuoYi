package com.ruoyi.erp.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.erp.model.dto.base.ExchangeRateDTO;
import com.ruoyi.erp.service.IExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erp/exchange-rate")
public class ExchangeRateController extends BaseController {

    @Autowired
    private IExchangeRateService exchangeRateService;


    @PreAuthorize("@ss.hasPermi('erp:rate:query')")
    @GetMapping("/today/{baseCurrency}")
    public AjaxResult getTodayRate(@PathVariable String baseCurrency) {
        ExchangeRateDTO rate = exchangeRateService.getTodayRate(baseCurrency);
        return success(rate);
    }

}
