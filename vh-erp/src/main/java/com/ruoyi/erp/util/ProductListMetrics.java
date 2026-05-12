package com.ruoyi.erp.util;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.vo.product.ProductVo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 计算商品列表工作台需要的汇总字段，保持 Controller 和前端都只处理稳定的列表数据。
 */
public final class ProductListMetrics {

            public static final List<String> ALL_MISSING_FIELD_CODES = List.of(
            "TITLE",
            "SPU",
            "MAIN_MEDIA",
            "DESCRIPTION",
            "BODY_HTML",
            "PRODUCT_TYPE",
            "CATEGORY",
            "VARIANT",
            "PRICE",
            "FREIGHT",
            "PURCHASE_PRICE",
            "VARIANT_MEDIA",
            "SKU"
    );

    public static final String DEFAULT_REQUIRED_FIELD_CODES = "TITLE,SPU,MAIN_MEDIA,DESCRIPTION,BODY_HTML,VARIANT,PRICE,FREIGHT,VARIANT_MEDIA,SKU";

    private ProductListMetrics() {
    }

    public static void apply(ProductVo vo, Product product, List<ProductVariant> variants, List<Media> medias, ShopifyTask latestTask) {
        List<ProductVariant> activeVariants = variants == null ? List.of() : variants.stream()
                .filter(ProductListMetrics::isActiveVariant)
                .toList();
        List<Media> mediaList = medias == null ? List.of() : medias;
        List<Media> imageMedias = mediaList.stream().filter(ProductListMetrics::isImageMedia).toList();

        vo.setProductVariantList(variants);
        vo.setVariantCount(activeVariants.size());
        vo.setMediaCount(mediaList.size());
        vo.setMediaUrlList(mediaList.stream().map(ProductListMetrics::resolveMediaUrl).filter(StringUtils::isNotEmpty).toList());
        vo.setMainMediaUrl(resolveMainMediaUrl(product, imageMedias));
        vo.setSkuPreview(activeVariants.stream().map(ProductVariant::getSku).filter(StringUtils::isNotEmpty).findFirst().orElse(null));

        vo.setPriceMin(minInteger(activeVariants.stream().map(ProductVariant::getPrice).filter(Objects::nonNull).toList()));
        vo.setPriceMax(maxInteger(activeVariants.stream().map(ProductVariant::getPrice).filter(Objects::nonNull).toList()));
        vo.setPurchasePriceMin(minInteger(activeVariants.stream().map(ProductVariant::getPurchasePrice).filter(Objects::nonNull).toList()));
        vo.setPurchasePriceMax(maxInteger(activeVariants.stream().map(ProductVariant::getPurchasePrice).filter(Objects::nonNull).toList()));
        vo.setProfitRateMin(activeVariants.stream().map(ProductVariant::getProfitRate).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null));
        vo.setProfitRateMax(activeVariants.stream().map(ProductVariant::getProfitRate).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        vo.setNeedResync(needResync(product, activeVariants, mediaList));
        vo.setMissingFields(parseMissingFields(product.getMissingFieldCodes()));

        if (latestTask != null) {
            vo.setLatestTaskId(latestTask.getTaskId());
            vo.setLatestTaskStatus(latestTask.getTaskStatus());
            vo.setLatestTaskError(latestTask.getErrorMessage());
        }
    }

    private static boolean isActiveVariant(ProductVariant variant) {
        return variant != null && !"0".equals(variant.getIsActiveAvailable());
    }

    private static boolean isImageMedia(Media media) {
        if (media == null) {
            return false;
        }
        String contentType = media.getMediaContentType();
        String filename = media.getFilename();
        return "IMAGE".equalsIgnoreCase(contentType)
                || (StringUtils.isNotEmpty(filename) && filename.matches("(?i).+\\.(jpg|jpeg|png|gif|webp|bmp)$"));
    }

    private static String resolveMainMediaUrl(Product product, List<Media> imageMedias) {
        if (imageMedias.isEmpty()) {
            return null;
        }
        Long mainMediaId = product.getMainMediaId();
        Optional<Media> mainMedia = imageMedias.stream()
                .filter(media -> Objects.equals(media.getMediaId(), mainMediaId))
                .findFirst();
        return resolveMediaUrl(mainMedia.orElse(imageMedias.get(0)));
    }

    private static String resolveMediaUrl(Media media) {
        if (media == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(media.getNasMediaUrl())) {
            return media.getNasMediaUrl();
        }
        if (StringUtils.isNotEmpty(media.getShopifyMediaUrl())) {
            return media.getShopifyMediaUrl();
        }
        if (StringUtils.isNotEmpty(media.getTranscodedMediaUrl())) {
            return media.getTranscodedMediaUrl();
        }
        return null;
    }

    private static Integer minInteger(List<Integer> values) {
        return values.stream().min(Integer::compareTo).orElse(null);
    }

    private static Integer maxInteger(List<Integer> values) {
        return values.stream().max(Integer::compareTo).orElse(null);
    }

    private static boolean needResync(Product product, List<ProductVariant> variants, List<Media> medias) {
        Date lastSyncTime = product.getLastSyncTime();
        if (lastSyncTime == null || StringUtils.isEmpty(product.getShopifyProductId())) {
            return false;
        }
        Date latestLocalUpdate = latestDate(product.getUpdateTime(), product.getCreateTime());
        for (ProductVariant variant : variants) {
            latestLocalUpdate = latestDate(latestLocalUpdate, variant.getUpdateTime(), variant.getCreateTime());
        }
        for (Media media : medias) {
            latestLocalUpdate = latestDate(latestLocalUpdate, media.getUpdateTime(), media.getCreateTime());
        }
        return latestLocalUpdate != null && latestLocalUpdate.after(lastSyncTime);
    }

    private static Date latestDate(Date... dates) {
        Date latest = null;
        for (Date date : dates) {
            if (date != null && (latest == null || date.after(latest))) {
                latest = date;
            }
        }
        return latest;
    }

    public static List<String> resolveMissingFields(Product product, List<ProductVariant> variants, List<Media> medias) {
        List<ProductVariant> activeVariants = variants == null ? List.of() : variants.stream()
                .filter(ProductListMetrics::isActiveVariant)
                .toList();
        List<Media> imageMedias = medias == null ? List.of() : medias.stream()
                .filter(ProductListMetrics::isImageMedia)
                .toList();
        return resolvePreparedMissingFields(product, activeVariants, imageMedias);
    }

    public static String joinMissingFields(List<String> missingFields) {
        if (missingFields == null || missingFields.isEmpty()) {
            return "";
        }
        Set<String> orderedFields = new LinkedHashSet<>(missingFields);
        orderedFields.retainAll(ALL_MISSING_FIELD_CODES);
        return String.join(",", orderedFields);
    }

    public static List<String> parseMissingFields(String missingFieldCodes) {
        if (StringUtils.isEmpty(missingFieldCodes)) {
            return new ArrayList<>();
        }
        Set<String> allowedFields = new LinkedHashSet<>(ALL_MISSING_FIELD_CODES);
        return List.of(missingFieldCodes.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .filter(allowedFields::contains)
                .distinct()
                .toList();
    }

    public static String normalizeRequiredFields(String requiredFields) {
        List<String> fields = parseMissingFields(requiredFields);
        return fields.isEmpty() ? DEFAULT_REQUIRED_FIELD_CODES : String.join(",", fields);
    }

    private static List<String> resolvePreparedMissingFields(Product product, List<ProductVariant> activeVariants, List<Media> imageMedias) {
        List<String> missingFields = new ArrayList<>();
        if (StringUtils.isEmpty(product.getProductTitle())) {
            missingFields.add("TITLE");
        }
        if (StringUtils.isEmpty(product.getSpu())) {
            missingFields.add("SPU");
        }
        if (imageMedias.isEmpty()) {
            missingFields.add("MAIN_MEDIA");
        }
        if (StringUtils.isEmpty(product.getDescription())) {
            missingFields.add("DESCRIPTION");
        }
        if (StringUtils.isEmpty(product.getBodyHtml())) {
            missingFields.add("BODY_HTML");
        }
        if (StringUtils.isEmpty(product.getProductType())) {
            missingFields.add("PRODUCT_TYPE");
        }
        if (StringUtils.isEmpty(product.getCategory())) {
            missingFields.add("CATEGORY");
        }
        if (activeVariants.isEmpty()) {
            missingFields.add("VARIANT");
            return missingFields;
        }
        if (activeVariants.stream().anyMatch(variant -> variant.getPrice() == null || variant.getPrice() <= 0)) {
            missingFields.add("PRICE");
        }
        if (activeVariants.stream().anyMatch(variant -> variant.getFreight() == null)) {
            missingFields.add("FREIGHT");
        }
        if (activeVariants.stream().anyMatch(variant -> variant.getPurchasePrice() == null)) {
            missingFields.add("PURCHASE_PRICE");
        }
        if(activeVariants.size() > 1 ) {
            if( activeVariants.stream().anyMatch(variant -> variant.getMediaId() == null)){
                missingFields.add("VARIANT_MEDIA");
            }
            if (activeVariants.stream().anyMatch(variant -> StringUtils.isEmpty(variant.getSku()))) {
                missingFields.add("SKU");
            }
        }
        return missingFields;
    }
}
