package com.ruoyi.erp.executor;

import com.ruoyi.erp.shopify.client.ShopifyGraphQLClient;
import com.ruoyi.erp.shopify.enums.*;
import com.ruoyi.erp.constant.ProductConstants;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.exception.ShopifyUserError;
import com.ruoyi.erp.shopify.model.ProductCreateResult;
import com.ruoyi.erp.shopify.model.StagedUploadResult;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ShopifyStoreMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.model.domain.ShopifyTask;
import com.ruoyi.erp.model.domain.ShopifyTaskDetail;
import com.ruoyi.erp.model.vo.media.PreparedMediaUpload;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IMediaTranscodeService;
import com.ruoyi.erp.service.IProductTagRelService;
import com.ruoyi.erp.service.IProductVariantService;
import com.ruoyi.erp.service.IShopifyTaskDetailService;
import com.ruoyi.erp.service.IShopifyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.Invocation;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopifySyncExecutorTest {

    @TempDir
    Path tempDir;

    private ShopifyGraphQLClient shopifyGraphQLClient;
    private IMediaService mediaService;
    private IMediaTranscodeService mediaTranscodeService;
    private IProductVariantService productVariantService;
    private IShopifyTaskService shopifyTaskService;
    private IShopifyTaskDetailService shopifyTaskDetailService;
    private ProductMapper productMapper;
    private List<String> variantBulkResult;
    private ShopifySyncExecutor executor;

    @BeforeEach
    void setUp() {
        variantBulkResult = List.of("gid://shopify/ProductVariant/1001", "gid://shopify/ProductVariant/1002");
        shopifyGraphQLClient = mock(ShopifyGraphQLClient.class, invocation -> {
            if ("createVariantsBulk".equals(invocation.getMethod().getName())) {
                return variantBulkResult;
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        mediaService = mock(IMediaService.class);
        mediaTranscodeService = mock(IMediaTranscodeService.class);
        productVariantService = mock(IProductVariantService.class);
        shopifyTaskService = mock(IShopifyTaskService.class);
        shopifyTaskDetailService = mock(IShopifyTaskDetailService.class);
        productMapper = mock(ProductMapper.class);
        IProductTagRelService productTagRelService = mock(IProductTagRelService.class);
        ShopifyStoreMapper shopifyStoreMapper = mock(ShopifyStoreMapper.class);

        ShopifyStore store = new ShopifyStore();
        store.setStoreId(1L);
        store.setShopName("demo-shop");
        store.setInventoryTracked("0");
        when(shopifyStoreMapper.selectById(1L)).thenReturn(store);

        executor = new ShopifySyncExecutor();
        ReflectionTestUtils.setField(executor, "shopifyGraphQLClient", shopifyGraphQLClient);
        ReflectionTestUtils.setField(executor, "productVariantService", productVariantService);
        ReflectionTestUtils.setField(executor, "shopifyStoreMapper", shopifyStoreMapper);
        ReflectionTestUtils.setField(executor, "mediaService", mediaService);
        ReflectionTestUtils.setField(executor, "mediaTranscodeService", mediaTranscodeService);
        ReflectionTestUtils.setField(executor, "productMapper", productMapper);
        ReflectionTestUtils.setField(executor, "productTagRelService", productTagRelService);
        ReflectionTestUtils.setField(executor, "shopifyTaskService", shopifyTaskService);
        ReflectionTestUtils.setField(executor, "shopifyTaskDetailService", shopifyTaskDetailService);
    }

    @Test
    void syncVariantsSavesReturnedShopifyVariantIdsToLocalVariants() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        StringBuilder result = new StringBuilder();
        boolean failed = invokeSyncVariants(List.of(first, second), result, null);

        assertTrue(!failed);
        ArgumentCaptor<ProductVariant> captor = forClass(ProductVariant.class);
        verify(productVariantService, times(2)).updateProductVariant(captor.capture());

        List<ProductVariant> updates = captor.getAllValues();
        assertEquals(101L, updates.get(0).getVariantId());
        assertEquals("gid://shopify/ProductVariant/1001", updates.get(0).getShopifyVariantId());
        assertEquals(102L, updates.get(1).getVariantId());
        assertEquals("gid://shopify/ProductVariant/1002", updates.get(1).getShopifyVariantId());
        assertTrue(result.toString().contains("gid://shopify/ProductVariant/1002"));
    }

    @Test
    void syncVariantsReportsFailureWhenShopifyReturnsFewerIds() throws Exception {
        ProductVariant first = buildVariant(101L);
        ProductVariant second = buildVariant(102L);
        variantBulkResult = List.of("gid://shopify/ProductVariant/1001");

        StringBuilder result = new StringBuilder();
        boolean failed = invokeSyncVariants(List.of(first, second), result, null);

        assertTrue(failed);
        assertTrue(result.toString().contains("Shopify 返回的变体 ID 数量"));
        verify(productVariantService, never()).updateProductVariant(any());
    }

    @Test
    void syncProductUsesRemoveStandaloneVariantStrategyForNewShopifyProduct() {
        Product product = buildProduct(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(shopifyGraphQLClient.createProduct(eq(1L), any(), any()))
                .thenReturn(new ProductCreateResult("gid://shopify/Product/2001", List.of()));
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of(buildVariant(101L), buildVariant(102L)));
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        executor.syncProduct(1L, 10L, 1, 1);

        Object[] args = singleCreateVariantsBulkArguments();
        assertEquals(4, args.length);
        assertEquals("REMOVE_STANDALONE_VARIANT", args[3]);
    }

    @Test
    void syncProductDoesNotUseRemoveStandaloneVariantStrategyForExistingShopifyProduct() {
        Product product = buildProduct("gid://shopify/Product/2001");
        when(productMapper.selectById(10L)).thenReturn(product);
        when(shopifyGraphQLClient.updateProduct(eq(1L), eq("gid://shopify/Product/2001"), any(), any()))
                .thenReturn(new ProductCreateResult("gid://shopify/Product/2001", List.of()));
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of(buildVariant(101L), buildVariant(102L)));
        when(productVariantService.updateProductVariant(any())).thenReturn(1);

        executor.syncProduct(1L, 10L, 1, 1);

        Object[] args = singleCreateVariantsBulkArguments();
        assertEquals(4, args.length);
        assertNull(args[3]);
    }

    @Test
    void syncProductUploadsPreparedImageWebpAndVideoWithMatchingShopifyResource() throws Exception {
        Product product = buildProduct(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of());
        when(shopifyGraphQLClient.createProduct(eq(1L), any(), any()))
                .thenReturn(new ProductCreateResult("gid://shopify/Product/2001", List.of()));

        Media image = buildMedia(301L, "image.jpg", "IMAGE");
        Media video = buildMedia(302L, "video.mp4", "VIDEO");
        String imageOriginalNasMediaUrl = image.getNasMediaUrl();
        String imageOriginalFilename = image.getFilename();
        when(mediaService.listByProductId(10L)).thenReturn(List.of(image, video));

        File imageWebp = createPreparedFile("image.webp");
        File videoFile = createPreparedFile("video.mp4");
        when(mediaTranscodeService.prepareForShopifyUpload(image)).thenReturn(PreparedMediaUpload.builder()
                .file(imageWebp)
                .filename("image.webp")
                .mimeType("image/webp")
                .mediaContentType("IMAGE")
                .transcoded(true)
                .build());
        when(mediaTranscodeService.prepareForShopifyUpload(video)).thenReturn(PreparedMediaUpload.builder()
                .file(videoFile)
                .filename("video.mp4")
                .mimeType("video/mp4")
                .mediaContentType("VIDEO")
                .transcoded(false)
                .build());
        when(shopifyGraphQLClient.stagedUploadMedia(eq(1L), any(), any(), any(), any(), any(Long.class)))
                .thenReturn(new StagedUploadResult("https://upload", "https://resource"));

        executor.syncProduct(1L, 10L, 1, 1);

        verify(shopifyGraphQLClient).stagedUploadMedia(eq(1L), eq("image.webp"), eq("image/webp"), eq("IMAGE"), any(), eq(imageWebp.length()));
        verify(shopifyGraphQLClient).stagedUploadMedia(eq(1L), eq("video.mp4"), eq("video/mp4"), eq("VIDEO"), any(), eq(videoFile.length()));
        assertEquals(imageOriginalNasMediaUrl, image.getNasMediaUrl());
        assertEquals(imageOriginalFilename, image.getFilename());

        ArgumentCaptor<Media> mediaUpdateCaptor = forClass(Media.class);
        verify(mediaService, times(2)).updateMedia(mediaUpdateCaptor.capture());
        List<Media> mediaUpdates = mediaUpdateCaptor.getAllValues();
        assertTrue(mediaUpdates.stream().allMatch(update -> update.getNasMediaUrl() == null));
        assertTrue(mediaUpdates.stream().allMatch(update -> update.getFilename() == null));
    }

    @Test
    void executeMarksProductPartialAndRecordsVariantUserErrorWhenVariantCreationFails() {
        ShopifyTask task = new ShopifyTask();
        task.setTaskId(900L);
        task.setStoreId(1L);
        task.setShopName("demo-shop");
        task.setTotalCount(1);
        when(shopifyTaskService.selectShopifyTaskByTaskId(900L)).thenReturn(task);

        Product product = buildProduct(null);
        when(productMapper.selectById(10L)).thenReturn(product);
        when(mediaService.listByProductId(10L)).thenReturn(List.of());
        when(shopifyGraphQLClient.createProduct(eq(1L), any(), any()))
                .thenReturn(new ProductCreateResult("gid://shopify/Product/2001", List.of()));
        when(productVariantService.selectListByProductId(10L)).thenReturn(List.of(buildVariant(101L), buildVariant(102L)));
        when(shopifyGraphQLClient.createVariantsBulk(eq(1L), eq("gid://shopify/Product/2001"), any(), eq("REMOVE_STANDALONE_VARIANT")))
                .thenThrow(new ShopifyApiException(
                        "variants.1.optionValues.0.name: Option value does not exist",
                        List.of(new ShopifyUserError("variants.1.optionValues.0.name", "Option value does not exist", "INVALID", 1))));

        executor.execute(900L, List.of(10L));

        ArgumentCaptor<Product> productCaptor = forClass(Product.class);
        verify(productMapper, atLeastOnce()).updateById(productCaptor.capture());
        Product statusUpdate = productCaptor.getAllValues().stream()
                .filter(update -> ProductConstants.SYNC_STATUS_PART_SUCCESS.equals(update.getSyncStatus()))
                .findFirst()
                .orElseThrow();
        assertEquals(10L, statusUpdate.getProductId());

        ArgumentCaptor<ShopifyTask> taskCaptor = forClass(ShopifyTask.class);
        verify(shopifyTaskService, atLeastOnce()).updateShopifyTask(taskCaptor.capture());
        ShopifyTask finalTask = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(ShopifyTaskStatus.PART_SUCCESS.getCode(), finalTask.getTaskStatus());
        assertEquals(1, finalTask.getPartialCount());
        assertEquals(0, finalTask.getFailedCount());

        ArgumentCaptor<ShopifyTaskDetail> detailCaptor = forClass(ShopifyTaskDetail.class);
        verify(shopifyTaskDetailService, atLeastOnce()).save(detailCaptor.capture());
        ShopifyTaskDetail failedVariantDetail = detailCaptor.getAllValues().stream()
                .filter(detail -> ShopifyTaskDetailItemType.VARIANT.getCode().equals(detail.getItemType()))
                .filter(detail -> ShopifyTaskDetailStatus.FAILED.getCode().equals(detail.getStatus()))
                .findFirst()
                .orElseThrow();
        assertEquals(102L, failedVariantDetail.getItemId());
        assertEquals("variants.1.optionValues.0.name", failedVariantDetail.getErrorField());
        assertEquals("INVALID", failedVariantDetail.getErrorCode());
        assertEquals("Option value does not exist", failedVariantDetail.getErrorMessage());
    }

    private Product buildProduct(String shopifyProductId) {
        Product product = new Product();
        product.setProductId(10L);
        product.setProductTitle("Test Product");
        product.setShopifyProductId(shopifyProductId);
        product.setBodyHtml("<p>body</p>");
        product.setDescription("description");
        return product;
    }

    private Media buildMedia(Long mediaId, String filename, String mediaType) {
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setProductId(10L);
        media.setFilename(filename);
        media.setNasMediaUrl("/profile/media/ABC001/" + filename);
        media.setMediaContentType(mediaType);
        return media;
    }

    private File createPreparedFile(String filename) throws Exception {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, filename);
        return file.toFile();
    }

    private ProductVariant buildVariant(Long variantId) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(10L);
        variant.setPrice(1000);
        variant.setOptionValues("[]");
        return variant;
    }

    private boolean invokeSyncVariants(List<ProductVariant> variants, StringBuilder result, String variantCreateStrategy) throws Exception {
        Method method = ShopifySyncExecutor.class.getDeclaredMethod(
                "syncVariants", Long.class, String.class, List.class, StringBuilder.class, String.class);
        method.setAccessible(true);
        try {
            return (boolean) method.invoke(executor, 1L, "gid://shopify/Product/2001", variants, result, variantCreateStrategy);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private Object[] singleCreateVariantsBulkArguments() {
        return mockingDetails(shopifyGraphQLClient).getInvocations().stream()
                .filter(invocation -> "createVariantsBulk".equals(invocation.getMethod().getName()))
                .map(Invocation::getArguments)
                .findFirst()
                .orElseThrow();
    }
}
