package com.ruoyi.erp.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/29 16:02
 **/
public class PriceUtil {
    /**
     * 分转元
     * @param fen
     */
    public static BigDecimal fenToYuan(Integer fen) {
        return fen!= null
                ? new BigDecimal(fen).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                : null;
    }
}
