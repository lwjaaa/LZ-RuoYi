package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.model.domain.ShopifyStore;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import com.ruoyi.erp.shopify.model.Parameter;
import com.ruoyi.erp.shopify.model.StagedUploadInput;
import com.ruoyi.erp.shopify.model.StagedUploadResult;
import com.ruoyi.erp.shopify.query.ShopifyGraphQLQueries;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shopify 媒体上传 GraphQL 客户端。
 */
@Slf4j
@Component
public class ShopifyMediaGraphQLClient {
    private static final String FIELD_QUERY = "query";
    private static final String FIELD_VARIABLES = "variables";
    private static final String PATH_STAGED_TARGETS = "stagedTargets";
    private static final String PATH_PARAMETERS = "parameters";
    private static final String PATH_NAME = "name";
    private static final String PATH_VALUE = "value";
    private static final String PATH_URL = "url";
    private static final String PATH_RESOURCE_URL = "resourceUrl";
    private static final String MUTATION_STAGED_UPLOADS_CREATE = "stagedUploadsCreate";
    private static final String RESOURCE_TYPE_IMAGE = "IMAGE";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String MULTIPART_BOUNDARY_PREFIX = "----ShopifyBoundary";
    private static final String FILE_FIELD_NAME = "file";
    private static final String FILE_UPLOAD_NAME = "upload";
    private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    @Resource
    private ShopifyGraphQLTransport transport;

    /**
     * 上传媒体文件并返回 Shopify 媒体信息 (指定店铺)
     */
    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, InputStream inputStream, long fileSize) {
        return stagedUploadMedia(storeId, filename, mimeType, RESOURCE_TYPE_IMAGE, inputStream, fileSize);
    }

    /**
     * 上传媒体文件并返回 Shopify staged upload 信息。
     */
    public StagedUploadResult stagedUploadMedia(Long storeId, String filename, String mimeType, String resource, InputStream inputStream, long fileSize) {
        String uploadResource = StringUtils.isEmpty(resource) ? RESOURCE_TYPE_IMAGE : resource;
        log.info("获取媒体文件上传地址: storeId={}, filename={}, mimeType={}, resource={}, fileSize={}",
                storeId, filename, mimeType, uploadResource, fileSize);
        String mutation = ShopifyGraphQLQueries.STAGED_UPLOADS_CREATE.getQuery();

        StagedUploadInput input = StagedUploadInput.builder()
                .resource(uploadResource)
                .filename(filename)
                .mimeType(mimeType)
                .httpMethod(HTTP_METHOD_POST)
                .fileSize(String.valueOf(fileSize))
                .build();

        JsonNode data = transport.execute(storeId, mutation, Map.of("input", List.of(input)));
        transport.checkUserErrors(data, MUTATION_STAGED_UPLOADS_CREATE);
        log.info("获取媒体文件上传地址成功: storeId={}, data={}", storeId, data);

        JsonNode target = data.path(MUTATION_STAGED_UPLOADS_CREATE).path(PATH_STAGED_TARGETS).get(0);
        String uploadUrl = target.path(PATH_URL).asText();
        String resourceUrl = target.path(PATH_RESOURCE_URL).asText();

        List<Parameter> parameters = parseParameters(target.path(PATH_PARAMETERS));

        try {
            byte[] fileBytes = inputStream.readAllBytes();
            uploadToStagedTarget(storeId, uploadUrl, parameters, fileBytes, mimeType);
            return new StagedUploadResult(uploadUrl, resourceUrl);
        } catch (IOException e) {
            throw new ShopifyApiException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传文件到 staged target URL (指定店铺)
     */
    private void uploadToStagedTarget(Long storeId, String uploadUrl, List<Parameter> parameters, byte[] fileData, String mimeType) {
        log.info("上传文件到 staged target URL: storeId={}, uploadUrl={}, parameters={}", storeId, uploadUrl, parameters);
        ShopifyStore store = transport.getStore(storeId);
        String boundary = MULTIPART_BOUNDARY_PREFIX + System.currentTimeMillis();
        byte[] body = buildMultipartBody(parameters, fileData, boundary);

        WebClient.create(uploadUrl)
                .post()
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Content-Length", String.valueOf(body.length))
                .header("X-Shopify-Access-Token", store.getAccessToken())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
        log.info("文件上传成功");
    }

    private Map<String, Object> buildRequestBody(String query, Object variables) {
        return Map.of(FIELD_QUERY, query, FIELD_VARIABLES, variables != null ? variables : Map.of());
    }

    private List<Parameter> parseParameters(JsonNode parametersNode) {
        List<Parameter> parameters = new ArrayList<>();
        for (JsonNode param : parametersNode) {
            parameters.add(new Parameter(
                    param.path(PATH_NAME).asText(),
                    param.path(PATH_VALUE).asText()
            ));
        }
        return parameters;
    }

    private byte[] buildMultipartBody(List<Parameter> parameters, byte[] fileData, String boundary) {
        StringBuilder sb = new StringBuilder();
        for (Parameter param : parameters) {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"").append(param.getName()).append("\"\r\n\r\n");
            sb.append(param.getValue()).append("\r\n");
        }
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(FILE_FIELD_NAME).append("\"; filename=\"")
                .append(FILE_UPLOAD_NAME).append("\"\r\n");
        sb.append("Content-Type: ").append(CONTENT_TYPE_OCTET_STREAM).append("\r\n\r\n");

        byte[] header = sb.toString().getBytes();
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] result = new byte[header.length + fileData.length + footer.length];

        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(fileData, 0, result, header.length, fileData.length);
        System.arraycopy(footer, 0, result, header.length + fileData.length, footer.length);
        return result;
    }
}
