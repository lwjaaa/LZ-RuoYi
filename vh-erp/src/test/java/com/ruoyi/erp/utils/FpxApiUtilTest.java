package com.ruoyi.erp.utils;

import com.ruoyi.erp.config.FpxApiConfig;
import com.ruoyi.erp.model.vo.shipping.ShippingFeeVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * FpxApiUtil 测试类
 * 
 * @author lwj
 * @date 2026-04-26
 */
@SpringBootTest
public class FpxApiUtilTest {

    @Autowired
    private FpxApiUtil fpxApiUtil;

    @Autowired
    private FpxApiConfig fpxApiConfig;

    @BeforeEach
    public void setUp() {
        // 可以在这里设置测试配置
        System.out.println("开始测试4px API工具类");
    }

    @Test
    public void testQueryShippingFee() {
        // 测试查询运费
        List<ShippingFeeVo> resultList = fpxApiUtil.queryShippingFee(
            20,   // 宽度(cm)
            15,   // 高度(cm)
            30,   // 长度(cm)
            500,  // 重量(g)
            "US", // 国家代码
            "10001", // 邮政编码
          null
        );

        assertNotNull(resultList);
        System.out.println("运费查询结果数量: " + resultList.size());
        if (!resultList.isEmpty()) {
            System.out.println("第一个运费查询结果: " + resultList.get(0));
        }

        // 验证返回结果包含必要字段
//        assertTrue(result.get("shippingFee"));
    }

}
