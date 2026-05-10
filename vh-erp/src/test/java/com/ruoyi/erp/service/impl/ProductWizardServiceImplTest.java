package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.service.IFormSuggestionService;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IProductService;
import com.ruoyi.erp.service.IProductTagRelService;
import com.ruoyi.erp.service.IProductVariantService;
import com.ruoyi.erp.service.ITagDictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ProductWizardServiceImplTest {

    private IProductService productService;
    private IProductVariantService productVariantService;
    private IProductTagRelService productTagRelService;
    private ITagDictService tagDictService;
    private IMediaService mediaService;
    private IFormSuggestionService formSuggestionService;
    private ProductWizardServiceImpl productWizardService;

    @BeforeEach
    void setUp() {
        productService = mock(IProductService.class);
        productVariantService = mock(IProductVariantService.class);
        productTagRelService = mock(IProductTagRelService.class);
        tagDictService = mock(ITagDictService.class);
        mediaService = mock(IMediaService.class);
        formSuggestionService = mock(IFormSuggestionService.class);

        productWizardService = new ProductWizardServiceImpl();
        ReflectionTestUtils.setField(productWizardService, "productService", productService);
        ReflectionTestUtils.setField(productWizardService, "productVariantService", productVariantService);
        ReflectionTestUtils.setField(productWizardService, "productTagRelService", productTagRelService);
        ReflectionTestUtils.setField(productWizardService, "tagDictService", tagDictService);
        ReflectionTestUtils.setField(productWizardService, "mediaService", mediaService);
        ReflectionTestUtils.setField(productWizardService, "formSuggestionService", formSuggestionService);

        when(productService.updateById(any(Product.class))).thenReturn(true);
        when(productService.getOne(any())).thenReturn(null);
        when(productVariantService.saveOrUpdateBatch(anyCollection())).thenReturn(true);
        when(productVariantService.list(any(Wrapper.class))).thenReturn(List.of());
    }

    @Test
    void saveProductCommonRewritesAllSkuAndSyncsMediaWhenSpuSavedForImportedProduct() {
        Product oldProduct = buildProduct(100L, null);
        when(productService.getById(100L)).thenReturn(oldProduct);

        Product product = buildProduct(100L, "ABC001");
        product.setProductVariantList(List.of(
                buildVariant(201L, 11L, "100-001"),
                buildVariant(202L, 12L, "CUSTOMSKU")
        ));

        productWizardService.saveProductCommon(product, 1);

        List<ProductVariant> savedVariants = captureSavedVariants();
        assertEquals("ABC001-001", savedVariants.get(0).getSku());
        assertEquals("ABC001-002", savedVariants.get(1).getSku());
        verify(mediaService).syncProductMediaKeyword(product, "100", savedVariants);
        verify(tagDictService).updateMaxSeqBySpuPrefix("ABC", 1);
    }

    @Test
    void saveProductCommonRewritesSkuAndSyncsMediaWhenSpuChangesAgain() {
        Product oldProduct = buildProduct(100L, "OLD001");
        when(productService.getById(100L)).thenReturn(oldProduct);

        Product product = buildProduct(100L, "NEW001");
        product.setProductVariantList(List.of(
                buildVariant(201L, 11L, "OLD001-RED"),
                buildVariant(202L, 12L, "MANUAL")
        ));

        productWizardService.saveProductCommon(product, 1);

        List<ProductVariant> savedVariants = captureSavedVariants();
        assertEquals("NEW001-RED", savedVariants.get(0).getSku());
        assertEquals("NEW001-002", savedVariants.get(1).getSku());
        verify(mediaService).syncProductMediaKeyword(product, "OLD001", savedVariants);
        verify(tagDictService).updateMaxSeqBySpuPrefix("NEW", 1);
    }

    @Test
    void saveProductCommonDoesNotSyncSkuOrMediaWhenKeywordIsUnchanged() {
        Product oldProduct = buildProduct(100L, "ABC001");
        when(productService.getById(100L)).thenReturn(oldProduct);

        Product product = buildProduct(100L, "ABC001");
        product.setProductVariantList(List.of(buildVariant(201L, 11L, "ABC001-001")));

        productWizardService.saveProductCommon(product, 1);

        List<ProductVariant> savedVariants = captureSavedVariants();
        assertEquals("ABC001-001", savedVariants.get(0).getSku());
        verify(mediaService, never()).syncProductMediaKeyword(any(), any(), any());
    }

    @Test
    void saveProductCommonRejectsDuplicateSpuBeforeUpdatingProduct() {
        Product oldProduct = buildProduct(100L, null);
        Product duplicatedProduct = buildProduct(200L, "ABC001");
        when(productService.getById(100L)).thenReturn(oldProduct);
        when(productService.getOne(any())).thenReturn(duplicatedProduct);

        Product product = buildProduct(100L, "ABC001");
        product.setProductVariantList(List.of(buildVariant(201L, 11L, "100-001")));

        assertThrows(ServiceException.class, () -> productWizardService.saveProductCommon(product, 1));
        verify(productService, never()).updateById(any(Product.class));
        verify(productVariantService, never()).saveOrUpdateBatch(anyCollection());
        verify(mediaService, never()).syncProductMediaKeyword(any(), any(), any());
    }

    private Product buildProduct(Long productId, String spu) {
        Product product = new Product();
        product.setProductId(productId);
        product.setSpu(spu);
        return product;
    }

    private ProductVariant buildVariant(Long variantId, Long mediaId, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(variantId);
        variant.setProductId(100L);
        variant.setMediaId(mediaId);
        variant.setSku(sku);
        return variant;
    }

    @SuppressWarnings("unchecked")
    private List<ProductVariant> captureSavedVariants() {
        ArgumentCaptor<Collection<ProductVariant>> variantCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(productVariantService).saveOrUpdateBatch(variantCaptor.capture());
        return List.copyOf(variantCaptor.getValue());
    }
}
