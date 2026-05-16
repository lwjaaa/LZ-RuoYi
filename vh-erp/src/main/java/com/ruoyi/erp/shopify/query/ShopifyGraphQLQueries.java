package com.ruoyi.erp.shopify.query;

public enum ShopifyGraphQLQueries {
    SHOP_INFO("""
        query shopInfo {
          shop {
            id
            name
            myshopifyDomain
          }
        }
        """),

    STAGED_UPLOADS_CREATE("""
        mutation stagedUploadsCreate($input: [StagedUploadInput!]!) {
          stagedUploadsCreate(input: $input) {
            userErrors { field message code }
            stagedTargets {
              url
              resourceUrl
              parameters { name value }
            }
          }
        }
        """),

    MEDIA_CREATE("""
        mutation mediaCreate($input: MediaInput!) {
          mediaCreate(input: $input) {
            userErrors { field message code }
            media {
              ... on MediaImage {
                id
              }
            }
          }
        }
        """),

    PRODUCT_CREATE("""
        mutation productCreate($product: ProductCreateInput!, $media: [CreateMediaInput!]) {
          productCreate(product: $product, media: $media) {
            userErrors { field message code }
            product {
              id
              title
              media(first: 250) {
                edges {
                  node {
                    id
                    alt
                    ... on MediaImage {
                      image {
                        originalSrc
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """),

    PRODUCT_UPDATE("""
        mutation productUpdate($product: ProductUpdateInput!, $media: [CreateMediaInput!]) {
          productUpdate(product: $product, media: $media) {
            userErrors { field message code }
            product {
              id
              media(first: 250) {
                edges {
                  node {
                    id
                  }
                }
              }
            }
          }
        }
        """),

    PRODUCT_VARIANTS_BULK_CREATE("""
        mutation productVariantsBulkCreate($productId: ID!, $variants: [ProductVariantsBulkInput!]!, $strategy: ProductVariantsBulkCreateStrategy) {
          productVariantsBulkCreate(productId: $productId, variants: $variants, strategy: $strategy) {
            userErrors { field message code }
            productVariants {
              id
              title
            }
          }
        }
        """),

    LOCATIONS("""
        query locations($first: Int!) {
          locations(first: $first) {
            edges {
              node {
                id
                name
              }
            }
          }
        }
        """),

    PUBLICATIONS("""
        query publications($first: Int!) {
          publications(first: $first) {
            edges {
              node {
                id
                name
              }
            }
          }
        }
        """),

    CHANNELS("""
        query channels($first: Int!) {
          channels(first: $first) {
            edges {
              node {
                id
                name
                isPublished
              }
            }
          }
        }
        """),

    PUBLISHABLE_PUBLISH("""
        mutation publishablePublish($id: ID!, $input: [PublicationInput!]!) {
          publishablePublish(id: $id, input: $input) {
            userErrors { field message code }
            publishable {
              ... on Product {
                id
              }
            }
          }
        }
        """),

    RESOURCE_PUBLICATION_SET("""
        mutation resourcePublicationSet($resourceId: ID!, $publications: [PublicationInput!]!) {
          resourcePublicationSet(resourceId: $resourceId, publications: $publications) {
            userErrors { field message code }
            resourcePublications {
              isPublished
              publishDate
              publication {
                channel {
                  id
                  name
                }
              }
            }
          }
        }
        """),

    PUBLICATION_UPDATE("""
        mutation publicationUpdate($id: ID!, $input: PublicationUpdateInput!) {
          publicationUpdate(id: $id, input: $input) {
            userErrors { field message code }
            publication {
              id
              autoPublish
            }
          }
        }
        """),

    PRODUCT_IMPORT_PAGE("""
        query productsForImport($first: Int!, $after: String, $query: String) {
          products(first: $first, after: $after, query: $query, sortKey: UPDATED_AT) {
            pageInfo {
              hasNextPage
              endCursor
            }
            edges {
              node {
                id
                title
                handle
                descriptionHtml
                productType
                vendor
                tags
                status
                updatedAt
                seo {
                  title
                  description
                }
                metafield(namespace: "custom", key: "SPU") {
                  value
                }
                options {
                  id
                  name
                  position
                  optionValues {
                    id
                    name
                  }
                }
                variants(first: 250) {
                  edges {
                    node {
                      id
                      sku
                      price
                      compareAtPrice
                      position
                      inventoryItem {
                        id
                        sku
                      }
                      selectedOptions {
                        name
                        value
                      }
                      media(first: 10) {
                        edges {
                          node {
                            id
                          }
                        }
                      }
                    }
                  }
                }
                media(first: 250) {
                  edges {
                    node {
                      id
                      alt
                      mediaContentType
                      preview {
                        image {
                          url
                        }
                      }
                      ... on MediaImage {
                        image {
                          url
                          originalSrc
                        }
                      }
                      ... on Video {
                        sources {
                          url
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """),

    BULK_OPERATION_RUN_QUERY("""
        mutation bulkOperationRunQuery($query: String!, $groupObjects: Boolean) {
          bulkOperationRunQuery(query: $query, groupObjects: $groupObjects) {
            bulkOperation {
              id
              status
            }
            userErrors {
              field
              message
              code
            }
          }
        }
        """),

    BULK_OPERATION_STATUS("""
        query bulkOperationStatus($id: ID!) {
          node(id: $id) {
            ... on BulkOperation {
              id
              status
              errorCode
              objectCount
              rootObjectCount
              url
              partialDataUrl
            }
          }
        }
        """),

    BULK_PRODUCT_IMPORT("""
        {
          products {
            edges {
              node {
                __typename
                id
                title
                handle
                descriptionHtml
                productType
                vendor
                tags
                status
                updatedAt
                seo {
                  title
                  description
                }
                metafield(namespace: "custom", key: "SPU") {
                  value
                }
                options {
                  id
                  name
                  position
                  optionValues {
                    id
                    name
                  }
                }
                variants {
                  edges {
                    node {
                      id
                      sku
                      price
                      compareAtPrice
                      position
                      inventoryItem {
                        id
                        sku
                      }
                      selectedOptions {
                        name
                        value
                      }
                      media {
                        edges {
                          node {
                            id
                          }
                        }
                      }
                    }
                  }
                }
                media {
                  edges {
                    node {
                      id
                      alt
                      mediaContentType
                      preview {
                        image {
                          url
                        }
                      }
                      ... on MediaImage {
                        image {
                          url
                          originalSrc
                        }
                      }
                      ... on Video {
                        sources {
                          url
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """),

    ORDER_IMPORT_PAGE("""
        query ordersForImport($first: Int!, $after: String, $query: String) {
          orders(first: $first, after: $after, query: $query, sortKey: UPDATED_AT) {
            pageInfo {
              hasNextPage
              endCursor
            }
            edges {
              node {
                id
                name
                orderNumber: number
                email
                phone
                displayFinancialStatus
                displayFulfillmentStatus
                currencyCode
                createdAt
                updatedAt
                cancelledAt
                closedAt
                totalPriceSet { shopMoney { amount currencyCode } }
                subtotalPriceSet { shopMoney { amount currencyCode } }
                totalTaxSet { shopMoney { amount currencyCode } }
                totalShippingPriceSet { shopMoney { amount currencyCode } }
                totalRefundedSet { shopMoney { amount currencyCode } }
                customer {
                  id
                  email
                  phone
                  firstName
                  lastName
                  displayName
                }
                shippingAddress {
                  name
                  phone
                  country
                  province
                  city
                  zip
                  address1
                  address2
                }
                lineItems(first: 250) {
                  edges {
                    node {
                      id
                      title
                      variantTitle
                      sku
                      quantity
                      originalUnitPriceSet { shopMoney { amount currencyCode } }
                      discountedTotalSet { shopMoney { amount currencyCode } }
                      product { id }
                      variant { id sku }
                    }
                  }
                }
                refunds {
                  id
                  createdAt
                  note
                  totalRefundedSet { shopMoney { amount currencyCode } }
                }
              }
            }
          }
        }
        """),

    FULFILLMENT_ORDERS_BY_ORDER("""
        query fulfillmentOrdersByOrder($id: ID!) {
          node(id: $id) {
            ... on Order {
              fulfillmentOrders(first: 50) {
                edges {
                  node {
                    id
                    status
                  }
                }
              }
            }
          }
        }
        """),

    FULFILLMENT_CREATE("""
        mutation fulfillmentCreate($fulfillment: FulfillmentInput!, $message: String) {
          fulfillmentCreate(fulfillment: $fulfillment, message: $message) {
            userErrors { field message code }
            fulfillment {
              id
              status
            }
          }
        }
        """);

    private final String query;

    ShopifyGraphQLQueries(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }
}
