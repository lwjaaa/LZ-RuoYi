package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.utils.MediaFileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class MediaServiceImplTest {

    @TempDir
    Path tempDir;

    private MediaMapper mediaMapper;
    private ProductMapper productMapper;
    private ProductVariantMapper productVariantMapper;
    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        new RuoYiConfig().setProfile(tempDir.toString().replace("\\", "/"));

        mediaMapper = mock(MediaMapper.class);
        productMapper = mock(ProductMapper.class);
        productVariantMapper = mock(ProductVariantMapper.class);

        mediaService = spy(new MediaServiceImpl());
        ReflectionTestUtils.setField(mediaService, "baseMapper", mediaMapper);
        ReflectionTestUtils.setField(mediaService, "mediaMapper", mediaMapper);
        ReflectionTestUtils.setField(mediaService, "productMapper", productMapper);
        ReflectionTestUtils.setField(mediaService, "productVariantMapper", productVariantMapper);

        when(mediaMapper.updateById(any(Media.class))).thenReturn(1);
        when(mediaMapper.insert(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setMediaId(99L);
            return 1;
        });
        when(mediaMapper.deleteByIds(anyCollection())).thenReturn(1);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);
        when(productMapper.update(any(Product.class), any(Wrapper.class))).thenReturn(1);
        when(productVariantMapper.update(any(ProductVariant.class), any(Wrapper.class))).thenReturn(1);
    }

    @Test
    void scanMediaToProductReusesExistingMediaAndClearsDeletedReferencesWithoutDeletingFiles() throws Exception {
        String dirPath = "SPU001";
        Path mediaDir = tempDir.resolve("media").resolve(dirPath);
        Files.createDirectories(mediaDir);
        Files.writeString(mediaDir.resolve("keep.jpg"), "keep");
        Files.writeString(mediaDir.resolve("new.png"), "new");

        Product product = new Product();
        product.setProductId(100L);
        product.setSpu(dirPath);
        when(productMapper.selectById(100L)).thenReturn(product);

        Media keepMedia = new Media();
        keepMedia.setMediaId(11L);
        keepMedia.setProductId(100L);
        keepMedia.setFilename("keep.jpg");
        keepMedia.setNasMediaUrl(MediaFileUtil.generateNasUrl(mediaDir.resolve("keep.jpg").toFile(), dirPath));
        keepMedia.setShopifyMediaId("gid://shopify/MediaImage/keep");
        keepMedia.setShopifyMediaUrl("https://cdn.shopify.com/keep.jpg");
        keepMedia.setStagedUploadUrl("https://staged-upload/keep");
        keepMedia.setPosition(3);
        keepMedia.setMediaContentType("IMAGE");

        Media missingMedia = new Media();
        missingMedia.setMediaId(12L);
        missingMedia.setProductId(100L);
        missingMedia.setFilename("missing.jpg");
        missingMedia.setNasMediaUrl("/profile/media/SPU001/missing.jpg");
        missingMedia.setShopifyMediaId("gid://shopify/MediaImage/missing");

        List<Media> finalMediaList = new ArrayList<>();
        finalMediaList.add(keepMedia);
        when(mediaMapper.insert(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setMediaId(99L);
            finalMediaList.add(media);
            return 1;
        });
        when(mediaMapper.selectList(any())).thenReturn(List.of(keepMedia, missingMedia), finalMediaList);

        List<Media> result = mediaService.scanMediaToProduct(100L);

        Media reused = result.stream()
                .filter(media -> "keep.jpg".equals(media.getFilename()))
                .findFirst()
                .orElseThrow();
        assertEquals(11L, reused.getMediaId());
        assertEquals("gid://shopify/MediaImage/keep", reused.getShopifyMediaId());
        assertEquals("https://cdn.shopify.com/keep.jpg", reused.getShopifyMediaUrl());
        assertEquals("https://staged-upload/keep", reused.getStagedUploadUrl());
        assertEquals(0, reused.getPosition());

        Media inserted = result.stream()
                .filter(media -> "new.png".equals(media.getFilename()))
                .findFirst()
                .orElseThrow();
        assertEquals(99L, inserted.getMediaId());
        assertEquals(3, inserted.getPosition());

        verify(mediaMapper).deleteByIds(argThat((Collection<Long> mediaIds) -> mediaIds.equals(Set.of(12L))));
        verify(mediaService, never()).deleteMediaFilesFromDisk(any());

        verify(productMapper).update(isNull(), any(UpdateWrapper.class));
        verify(productVariantMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void updateProductMediaRenamesSkuImagesBySkuAndOtherImagesBySpuSequence() {
        Product product = buildProduct(100L, "ABC001", List.of(
                buildMedia(11L, "variant-old.jpg"),
                buildMedia(12L, "main-old.jpg"),
                buildMedia(13L, "detail-old.png")
        ), List.of(
                buildVariant(201L, 11L, "ABC001-001"),
                buildVariant(202L, 11L, "ABC001-002")
        ));
        when(mediaMapper.selectList(any())).thenReturn(product.getMediaList());
        doNothing().when(mediaService).doRenameMediaFiles(any());

        mediaService.updateProductMedia(product);

        Map<Long, String> updatedFilenames = captureUpdatedFilenames();
        assertEquals("ABC001-001.jpg", updatedFilenames.get(11L));
        assertEquals("ABC001-1.jpg", updatedFilenames.get(12L));
        assertEquals("ABC001-2.png", updatedFilenames.get(13L));

        ArgumentCaptor<List> renameCaptor = ArgumentCaptor.forClass(List.class);
        verify(mediaService).doRenameMediaFiles(renameCaptor.capture());
        assertEquals(3, renameCaptor.getValue().size());
    }

    @Test
    void updateProductMediaUsesProductIdPrefixWhenSpuIsBlank() {
        Product product = buildProduct(100L, null, List.of(
                buildMedia(21L, "main-old.jpg"),
                buildMedia(22L, "detail-old.png")
        ), List.of());
        when(mediaMapper.selectList(any())).thenReturn(product.getMediaList());
        doNothing().when(mediaService).doRenameMediaFiles(any());

        mediaService.updateProductMedia(product);

        Map<Long, String> updatedFilenames = captureUpdatedFilenames();
        assertEquals("100-1.jpg", updatedFilenames.get(21L));
        assertEquals("100-2.png", updatedFilenames.get(22L));
    }

    @Test
    void doRenameMediaFilesSupportsTwoFileSwap() throws Exception {
        Path mediaDir = tempDir.resolve("media").resolve("products").resolve("ABC001");
        Files.createDirectories(mediaDir);
        Files.writeString(mediaDir.resolve("a1.jpg"), "a-image");
        Files.writeString(mediaDir.resolve("b1.jpg"), "b-image");

        mediaService.doRenameMediaFiles(List.of(
                new com.ruoyi.erp.model.vo.media.MediaRenameVo(buildMediaWithPath(31L, "a1.jpg", mediaDir), "b1.jpg"),
                new com.ruoyi.erp.model.vo.media.MediaRenameVo(buildMediaWithPath(32L, "b1.jpg", mediaDir), "a1.jpg")
        ));

        assertTrue(Files.exists(mediaDir.resolve("a1.jpg")));
        assertTrue(Files.exists(mediaDir.resolve("b1.jpg")));
        assertEquals("b-image", Files.readString(mediaDir.resolve("a1.jpg")));
        assertEquals("a-image", Files.readString(mediaDir.resolve("b1.jpg")));
    }

    @Test
    void doRenameMediaFilesSupportsThreeFileCycle() throws Exception {
        Path mediaDir = tempDir.resolve("media").resolve("products").resolve("ABC002");
        Files.createDirectories(mediaDir);
        Files.writeString(mediaDir.resolve("a.jpg"), "a");
        Files.writeString(mediaDir.resolve("b.jpg"), "b");
        Files.writeString(mediaDir.resolve("c.jpg"), "c");

        mediaService.doRenameMediaFiles(List.of(
                new com.ruoyi.erp.model.vo.media.MediaRenameVo(buildMediaWithPath(41L, "a.jpg", mediaDir), "b.jpg"),
                new com.ruoyi.erp.model.vo.media.MediaRenameVo(buildMediaWithPath(42L, "b.jpg", mediaDir), "c.jpg"),
                new com.ruoyi.erp.model.vo.media.MediaRenameVo(buildMediaWithPath(43L, "c.jpg", mediaDir), "a.jpg")
        ));

        assertEquals("c", Files.readString(mediaDir.resolve("a.jpg")));
        assertEquals("a", Files.readString(mediaDir.resolve("b.jpg")));
        assertEquals("b", Files.readString(mediaDir.resolve("c.jpg")));
    }

    private Product buildProduct(Long productId, String spu, List<Media> mediaList, List<ProductVariant> variantList) {
        Product product = new Product();
        product.setProductId(productId);
        product.setSpu(spu);
        product.setMediaList(mediaList);
        product.setProductVariantList(variantList);
        return product;
    }

    private Media buildMedia(Long mediaId, String filename) {
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setProductId(100L);
        media.setFilename(filename);
        media.setNasMediaUrl("/profile/media/products/ABC001/" + filename);
        return media;
    }

    private Media buildMediaWithPath(Long mediaId, String filename, Path mediaDir) {
        Media media = new Media();
        media.setMediaId(mediaId);
        media.setProductId(100L);
        media.setFilename(filename);
        media.setNasMediaUrl(MediaFileUtil.generateNasUrl(mediaDir.resolve(filename).toString()));
        return media;
    }

    private ProductVariant buildVariant(Long variantId, Long mediaId, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(100L);
        variant.setMediaId(mediaId);
        variant.setSku(sku);
        return variant;
    }

    private Map<Long, String> captureUpdatedFilenames() {
        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaMapper, atLeastOnce()).updateById(mediaCaptor.capture());
        return mediaCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(Media::getMediaId))
                .collect(Collectors.toMap(Media::getMediaId, Media::getFilename, (first, second) -> second));
    }
}
