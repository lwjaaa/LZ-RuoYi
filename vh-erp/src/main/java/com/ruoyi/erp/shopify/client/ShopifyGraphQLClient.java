package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.erp.model.dto.shopifyProduct.ShopifyImportedProduct;
import com.ruoyi.erp.model.vo.shopifyStore.ShopifyApiCallResponseVo;
import com.ruoyi.erp.shopify.model.BulkOperationInfo;
import com.ruoyi.erp.shopify.model.ChannelInfo;
import com.ruoyi.erp.shopify.model.CreateMediaInput;
import com.ruoyi.erp.shopify.model.LocationInfo;
import com.ruoyi.erp.shopify.model.ProductCreateResult;
import com.ruoyi.erp.shopify.model.ProductInput;
import com.ruoyi.erp.shopify.model.ProductPage;
import com.ruoyi.erp.shopify.model.PublicationInfo;
import com.ruoyi.erp.shopify.model.PublicationInput;
import com.ruoyi.erp.shopify.model.PublicationResult;
import com.ruoyi.erp.shopify.model.StagedUploadResult;
import com.ruoyi.erp.shopify.model.VariantInput;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * Shopify GraphQL API 对外门面，保持原有调用入口稳定。
 */
@Component
public class ShopifyGraphQLClient {

    @Resource
    private ShopifyGraphQLTransport transport;
    @Resource
    private ShopifyMediaGraphQLClient mediaClient;
    @Resource
    private ShopifyProductGraphQLClient productClient;
    @Resource
    private ShopifyPublicationGraphQLClient publicationClient;
    @Resource
    private ShopifyProductImportGraphQLClient productImportClient;

    public void invalidateStoreCache(Long storeId) {
        transport.invalidateStoreCache(storeId);
    }

    public JsonNode execute(Long storeId, String query, Object variables) {
        return transport.execute(storeId, query, variables);
    }

    public ShopifyApiCallResponseVo callAdminApi(Long storeId, String method, String url, Object body) {
        return transport.callAdminApi(storeId, method, url, body);
    }

    public void checkUserErrors(JsonNode data, String mutationName) {
        transport.checkUserErrors(data, mutationName);
    }

    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, InputStream inputStream, long fileSize) {
        return mediaClient.stagedUploadMedia(storeId, filename, mimeType, inputStream, fileSize);
    }

    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, String resource, InputStream inputStream, long fileSize) {
        return mediaClient.stagedUploadMedia(storeId, filename, mimeType, resource, inputStream, fileSize);
    }

    public ProductCreateResult createProduct(Long storeId, ProductInput productInput, List<CreateMediaInput> mediaInputs) {
        return productClient.createProduct(storeId, productInput, mediaInputs);
    }

    public ProductCreateResult updateProduct(Long storeId, String shopifyProductId, ProductInput input, List<CreateMediaInput> mediaInputs) {
        return productClient.updateProduct(storeId, shopifyProductId, input, mediaInputs);
    }

    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants) {
        return productClient.createVariantsBulk(storeId, shopifyProductId, variants);
    }

    public List<String> createVariantsBulk(Long storeId, String shopifyProductId, List<VariantInput> variants, String strategy) {
        return productClient.createVariantsBulk(storeId, shopifyProductId, variants, strategy);
    }

    public void testConnection(Long storeId) {
        publicationClient.testConnection(storeId);
    }

    public List<LocationInfo> getLocations(Long storeId) {
        return publicationClient.getLocations(storeId);
    }

    public List<PublicationInfo> getPublications(Long storeId) {
        return publicationClient.getPublications(storeId);
    }

    public List<ChannelInfo> getChannels(Long storeId) {
        return publicationClient.getChannels(storeId);
    }

    public void updatePublicationAutoPublish(Long storeId, String publicationId, boolean autoPublish) {
        publicationClient.updatePublicationAutoPublish(storeId, publicationId, autoPublish);
    }

    public List<PublicationResult> publishProduct(Long storeId, String shopifyProductId, List<PublicationInput> publications) {
        return publicationClient.publishProduct(storeId, shopifyProductId, publications);
    }

    public ProductPage queryUpdatedProducts(Long storeId, Date since, String cursor, int first) {
        return productImportClient.queryUpdatedProducts(storeId, since, cursor, first);
    }

    public BulkOperationInfo startProductBulkImport(Long storeId) {
        return productImportClient.startProductBulkImport(storeId);
    }

    public BulkOperationInfo pollBulkOperation(Long storeId, String bulkOperationId) {
        return productImportClient.pollBulkOperation(storeId, bulkOperationId);
    }

    public BulkOperationInfo waitForBulkOperation(Long storeId, String bulkOperationId, Duration timeout) {
        return productImportClient.waitForBulkOperation(storeId, bulkOperationId, timeout);
    }

    public List<ShopifyImportedProduct> downloadBulkJsonlProducts(String url) {
        return productImportClient.downloadBulkJsonlProducts(url);
    }
}
