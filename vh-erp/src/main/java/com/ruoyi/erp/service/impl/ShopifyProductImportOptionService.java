package com.ruoyi.erp.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.domain.ProductOption;
import com.ruoyi.erp.model.domain.ProductOptionValue;
import com.ruoyi.erp.model.domain.ProductVariantOption;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedOption;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedOptionValue;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedVariant;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shopify 商品导入选项转换服务。
 */
@Service
public class ShopifyProductImportOptionService {

    /**
     * 构建商品选项上下文，用于同时生成商品 optionJson 和变体 optionValues。
     *
     * @param remote Shopify 商品数据
     * @return 商品选项上下文
     */
    public OptionContext buildOptionContext(ShopifyImportedProduct remote) {
        Map<String, OptionMeta> optionMap = new LinkedHashMap<>();
        appendRemoteOptions(optionMap, remote == null ? null : remote.getOptions());
        appendVariantSelectedOptions(optionMap, remote == null ? null : remote.getVariants());
        List<ProductOption> options = optionMap.values().stream()
                .map(OptionMeta::toProductOption)
                .toList();
        String optionJson = options.isEmpty() ? null : JSON.toJSONString(options);
        return new OptionContext(optionJson, optionMap);
    }

    /**
     * 按 ProductVariantOption 格式构建变体选项 JSON。
     *
     * @param context 商品选项上下文
     * @param variant Shopify 变体数据
     * @return 变体选项 JSON
     */
    public String buildVariantOptionValues(OptionContext context, ShopifyImportedVariant variant) {
        if (context == null || variant == null || variant.getSelectedOptions() == null || variant.getSelectedOptions().isEmpty()) {
            return null;
        }
        List<ProductVariantOption> options = new ArrayList<>();
        for (Map.Entry<String, String> entry : variant.getSelectedOptions().entrySet()) {
            String optionName = normalize(entry.getKey());
            String valueName = normalize(entry.getValue());
            if (StringUtils.isEmpty(optionName) || StringUtils.isEmpty(valueName)) {
                continue;
            }
            OptionMeta option = context.optionMap().computeIfAbsent(optionName, this::createFallbackOption);
            ValueMeta value = option.valueMap.computeIfAbsent(valueName, name -> createFallbackValue(option.optionId, name));
            ProductVariantOption variantOption = new ProductVariantOption();
            variantOption.setOptionId(option.optionId);
            variantOption.setValueId(value.valueId);
            variantOption.setEnglishName(option.name);
            variantOption.setChineseName(option.name);
            variantOption.setEnglishValue(value.name);
            variantOption.setChineseValue(value.name);
            options.add(variantOption);
        }
        return options.isEmpty() ? null : JSON.toJSONString(options);
    }

    /**
     * 追加 Shopify 商品选项。
     *
     * @param optionMap 选项映射
     * @param remoteOptions Shopify 选项列表
     */
    private void appendRemoteOptions(Map<String, OptionMeta> optionMap, List<ShopifyImportedOption> remoteOptions) {
        if (remoteOptions == null || remoteOptions.isEmpty()) {
            return;
        }
        remoteOptions.stream()
                .filter(option -> option != null && StringUtils.isNotEmpty(normalize(option.getName())))
                .sorted(Comparator.comparing(option -> option.getPosition() == null ? Integer.MAX_VALUE : option.getPosition()))
                .forEach(option -> appendRemoteOption(optionMap, option));
    }

    /**
     * 追加单个 Shopify 商品选项。
     *
     * @param optionMap 选项映射
     * @param remoteOption Shopify 选项
     */
    private void appendRemoteOption(Map<String, OptionMeta> optionMap, ShopifyImportedOption remoteOption) {
        String optionName = normalize(remoteOption.getName());
        OptionMeta option = optionMap.computeIfAbsent(optionName, name ->
                new OptionMeta(firstNonEmpty(remoteOption.getId(), stableId("option:" + name)), name));
        if (remoteOption.getValues() == null) {
            return;
        }
        for (ShopifyImportedOptionValue remoteValue : remoteOption.getValues()) {
            if (remoteValue == null || StringUtils.isEmpty(normalize(remoteValue.getName()))) {
                continue;
            }
            String valueName = normalize(remoteValue.getName());
            option.valueMap.computeIfAbsent(valueName, name ->
                    new ValueMeta(firstNonEmpty(remoteValue.getId(), stableId("option-value:" + option.optionId + ":" + name)), name));
        }
    }

    /**
     * 从变体 selectedOptions 中补齐商品选项和值。
     *
     * @param optionMap 选项映射
     * @param variants Shopify 变体列表
     */
    private void appendVariantSelectedOptions(Map<String, OptionMeta> optionMap, List<ShopifyImportedVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }
        for (ShopifyImportedVariant variant : variants) {
            if (variant == null || variant.getSelectedOptions() == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : variant.getSelectedOptions().entrySet()) {
                String optionName = normalize(entry.getKey());
                String valueName = normalize(entry.getValue());
                if (StringUtils.isEmpty(optionName) || StringUtils.isEmpty(valueName)) {
                    continue;
                }
                OptionMeta option = optionMap.computeIfAbsent(optionName, this::createFallbackOption);
                option.valueMap.computeIfAbsent(valueName, name -> createFallbackValue(option.optionId, name));
            }
        }
    }

    /**
     * 创建本地兜底选项。
     *
     * @param optionName 选项名
     * @return 选项元数据
     */
    private OptionMeta createFallbackOption(String optionName) {
        return new OptionMeta(stableId("option:" + optionName), optionName);
    }

    /**
     * 创建本地兜底选项值。
     *
     * @param optionId 选项 ID
     * @param valueName 选项值名
     * @return 选项值元数据
     */
    private ValueMeta createFallbackValue(String optionId, String valueName) {
        return new ValueMeta(stableId("option-value:" + optionId + ":" + valueName), valueName);
    }

    /**
     * 标准化选项文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本
     */
    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 取第一个非空字符串。
     *
     * @param first 优先值
     * @param second 兜底值
     * @return 非空字符串
     */
    private String firstNonEmpty(String first, String second) {
        return StringUtils.isNotEmpty(first) ? first : second;
    }

    /**
     * 生成稳定本地 ID。
     *
     * @param seed ID 种子
     * @return 稳定 UUID 字符串
     */
    private String stableId(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 商品选项转换上下文，保存商品 JSON 与后续变体匹配所需的映射。
     */
    public record OptionContext(String optionJson, Map<String, OptionMeta> optionMap) {
    }

    /**
     * 商品选项值的中间映射数据。
     */
    private record ValueMeta(String valueId, String name) {
    }

    /**
     * 商品选项的中间映射数据。
     */
    public static class OptionMeta {
        private final String optionId;
        private final String name;
        private final Map<String, ValueMeta> valueMap = new LinkedHashMap<>();

        private OptionMeta(String optionId, String name) {
            this.optionId = optionId;
            this.name = name;
        }

        /**
         * 转换为 ProductOption。
         *
         * @return 商品选项
         */
        private ProductOption toProductOption() {
            ProductOption option = new ProductOption();
            option.setOptionId(optionId);
            option.setEnglishName(name);
            option.setChineseName(name);
            option.setValues(valueMap.values().stream().map(value -> {
                ProductOptionValue optionValue = new ProductOptionValue();
                optionValue.setValueId(value.valueId());
                optionValue.setEnglishValue(value.name());
                optionValue.setChineseValue(value.name());
                return optionValue;
            }).toList());
            return option;
        }
    }
}
