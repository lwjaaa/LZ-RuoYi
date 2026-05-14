package com.ruoyi.erp.shopify.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.erp.shopify.exception.ShopifyApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopifyGraphQLClientUserErrorsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void checkUserErrorsKeepsFieldCodeAndInputIndex() throws Exception {
        ShopifyGraphQLClient client = new ShopifyGraphQLClient();
        JsonNode data = objectMapper.readTree("""
                {
                  "productVariantsBulkCreate": {
                    "userErrors": [
                      {
                        "field": ["variants", "1", "optionValues", "0", "name"],
                        "message": "Option value does not exist",
                        "code": "INVALID"
                      }
                    ]
                  }
                }
                """);

        ShopifyApiException exception = assertThrows(ShopifyApiException.class,
                () -> client.checkUserErrors(data, "productVariantsBulkCreate"));

        assertEquals(1, exception.getUserErrors().size());
        assertEquals("variants.1.optionValues.0.name", exception.getUserErrors().get(0).getField());
        assertEquals("Option value does not exist", exception.getUserErrors().get(0).getMessage());
        assertEquals("INVALID", exception.getUserErrors().get(0).getCode());
        assertEquals(1, exception.getUserErrors().get(0).getInputIndex());
    }
}
