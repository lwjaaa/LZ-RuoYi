# ERP 系统 API 接口文档

## 目录

- [1. 概述](#1-概述)
- [2. 标签级联列表接口](#2-标签级联列表接口)
- [3. 商品创建接口](#3-商品创建接口)
- [4. 错误码说明](#4-错误码说明)

---

## 1. 概述

本文档描述了 ERP 系统中标签管理及商品创建相关的 API 接口。接口采用 RESTful 风格设计，支持 JSON 数据格式进行请求与响应交互。

**基础信息：**

- 基础 URL：`http://127.0.0.1:8080/api/erp`
- 数据格式：`JSON`
- 字符编码：`UTF-8`

---

## 2. 标签级联列表接口

### 接口基本信息

| 属性     | 值                                                     |
| -------- | ------------------------------------------------------ |
| 接口名称 | 获取标签级联列表                                       |
| 请求方式 | `GET`                                                  |
| 请求路径 | `/tag/treelist/ALL`                                    |
| 功能描述 | 获取系统中所有标签的级联列表结构，支持多级父子关系展示 |

### 请求参数

该接口无需请求参数。

### 请求示例

```http
GET http://127.0.0.1:8080/api/erp/tag/treelist/ALL
```

### 响应参数

| 参数名 | 类型      | 必填 | 说明                     |
| ------ | --------- | ---- | ------------------------ |
| `code` | `integer` | 是   | 响应状态码，200 表示成功 |
| `msg`  | `string`  | 是   | 响应信息描述             |
| `data` | `array`   | 是   | 标签列表数据             |

**data 数组元素说明：**

| 参数名          | 类型      | 必填 | 说明                          |
| --------------- | --------- | ---- | ----------------------------- |
| `tagId`         | `integer` | 是   | 标签唯一标识                  |
| `tagName`       | `string`  | 是   | 标签名称                      |
| `tagCode`       | `string`  | 是   | 标签编码                      |
| `tagType`       | `string`  | 是   | 标签类型                      |
| `sortOrder`     | `integer` | 是   | 排序序号，数值越小越靠前      |
| `parentId`      | `integer` | 是   | 父级标签 ID，0 表示根节点     |
| `ancestors`     | `string`  | 是   | 祖先节点路径，格式如："0,1,2" |
| `menuLevel`     | `integer` | 是   | 菜单层级，1 表示一级          |
| `spuPrefix`     | `string`  | 是   | 商品SPU前缀                   |
| `currentMaxSeq` | `integer` | 是   | 当前最大序号                  |
| `children`      | `array`   | 是   | 子标签列表，递归结构          |

### 响应示例

**成功响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "tagId": 1,
      "tagName": "杯子",
      "tagCode": "CUP",
      "tagType": "MENU",
      "sortOrder": 1,
      "parentId": 0,
      "ancestors": "",
      "menuLevel": 1,
      "spuPrefix": "ACUP",
      "currentMaxSeq": 12,
      "children": []
    }
  ]
}
```

**响应字段补充说明：**

- `ancestors`：空字符串表示该标签为根节点，非空时格式为父级 ID 逗号分隔的路径
- `children`：子标签数组，结构与父级相同，支持无限层级嵌套
- `tagType`：枚举值，当前示例为 `MENU`，可根据业务扩展其他类型

---

## 3. 商品创建接口

### 接口基本信息

| 属性     | 值                                                      |
| -------- | ------------------------------------------------------- |
| 接口名称 | 商品创建                                                |
| 请求方式 | `POST`                                                  |
| 请求路径 | `/product/selectionInfo`                                |
| 功能描述 | 根据商品名称、标签、SPU编码、规格选项及变体信息创建商品 |

### 请求参数

| 参数名               | 类型             | 必填 | 说明                                 |
| -------------------- | ---------------- | ---- | ------------------------------------ |
| `productName`        | `string`         | 是   | 商品名称                             |
| `tagIds`             | `array[integer]` | 是   | 标签 ID 数组，用于商品分类筛选       |
| `sourceUrl`          | `string`         | 是   | 数据来源 URL 地址                    |
| `spu`                | `string`         | 是   | 商品 SPU 编码，用于精确匹配商品      |
| `mediaUrlList`       | `array[string]`  | 是   | 商品媒体文件 URL 列表（图片、视频）  |
| `optionList`         | `array[object]`  | 是   | 商品规格选项列表                     |
| `productVariantList` | `array[object]`  | 是   | 商品变体列表，定义不同规格组合的变体 |

**mediaUrlList 数组元素说明：**

| 参数名 | 类型     | 必填 | 说明                                 |
| ------ | -------- | ---- | ------------------------------------ |
| -      | `string` | 是   | 商品媒体文件 URL，支持多张图片或视频 |

**optionList 数组元素说明：**

| 参数名        | 类型            | 必填 | 说明                      |
| ------------- | --------------- | ---- | ------------------------- |
| `chineseName` | `string`        | 是   | 规格中文名称，如："颜色"  |
| `englishName` | `string`        | 是   | 规格英文标识，如："color" |
| `values`      | `array[object]` | 是   | 规格可选值列表            |

**values 数组元素说明：**

| 参数名         | 类型     | 必填 | 说明           |
| -------------- | -------- | ---- | -------------- |
| `chineseValue` | `string` | 是   | 规格值中文描述 |
| `englishValue` | `string` | 是   | 规格值英文标识 |

**productVariantList 数组元素说明：**

| 参数名              | 类型            | 必填 | 说明                           |
| ------------------- | --------------- | ---- | ------------------------------ |
| `purchaseUrl`       | `string`        | 是   | 采购链接 URL                   |
| `purchasePrice`     | `number`        | 是   | 采购价格，精确到小数点后2位    |
| `mediaUrl`          | `string`        | 是   | 变体对应的媒体文件 URL         |
| `isActiveAvailable` | `integer`       | 是   | 是否可购买，1 表示是，0 表示否 |
| `optionValueList`   | `array[object]` | 是   | 变体关联的规格值列表           |

**optionValueList 数组元素说明：**

| 参数名         | 类型     | 必填 | 说明                 |
| -------------- | -------- | ---- | -------------------- |
| `chineseValue` | `string` | 是   | 规格值中文描述，不传 |
| `englishValue` | `string` | 是   | 规格值英文标识       |
| `chineseName`  | `string` | 是   | 规格中文名称         |
| `englishName`  | `string` | 是   | 规格英文标识         |

### 请求示例

```http
POST http://127.0.0.1:8080/api/erp/product/selectionInfo
Content-Type: application/json

{
  "productName": "商品名称",
  "tagIds": [1, 2, 3],
  "sourceUrl": "http://127.0.0.1:8080/api/erp/tag/treelist/ALL",
  "spu": "ACUP001",
  "mediaUrlList": [
    "https://example.com/cup0.jpg",
    "https://example.com/cup1.png",
    "https://example.com/cup2.jpg"
  ],
  "optionList": [
    {
      "chineseName": "颜色",
      "englishName": "color",
      "values": [
        {
          "chineseValue": "红色",
          "englishValue": "red"
        },
        {
          "chineseValue": "绿色",
          "englishValue": "green"
        }
      ]
    },
    {
      "chineseName": "尺寸",
      "englishName": "size",
      "values": [
        {
          "chineseValue": "大号",
          "englishValue": "BIG"
        },
        {
          "chineseValue": "小号",
          "englishValue": "SMALL"
        }
      ]
    },
  ],
  "productVariantList": [
    {
      "purchaseUrl": "http://127.0.0.1:8080/api/erp/product/selectionInfo/121654964894",
      "purchasePrice": 100.00,
      "mediaUrl": "https://example.com/cup0.jpg",
      "isActiveAvailable": 1,
      "optionValueList": [
        {
          "chineseValue": "红色",
          "englishValue": "red",
          "chineseName": "颜色",
          "englishName": "color"
        },{
          "chineseValue": "大号",
          "englishValue": "BIG",
          "chineseName": "尺寸",
          "englishName": "size"
        }
      ]
    }
  ]
}
```

### 响应参数

| 参数名 | 类型      | 必填 | 说明                     |
| ------ | --------- | ---- | ------------------------ |
| `code` | `integer` | 是   | 响应状态码，200 表示成功 |
| `msg`  | `string`  | 是   | 响应信息描述             |
| `data` | `integer` | 是   | 商品信息标识 ID          |

### 响应示例

**成功响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": 121654964894
}
```

**响应字段补充说明：**

- `data`：返回的商品信息标识 ID，可用于后续商品详情查询

---

## 4. 错误码说明

### 通用错误码

| 错误码 | 说明           | 处理建议                         |
| ------ | -------------- | -------------------------------- |
| `200`  | 请求成功       | -                                |
| `400`  | 请求参数错误   | 检查请求参数格式、必填项是否完整 |
| `401`  | 未授权访问     | 检查是否已登录或 Token 是否有效  |
| `403`  | 权限不足       | 检查用户权限配置                 |
| `404`  | 接口不存在     | 检查请求路径是否正确             |
| `500`  | 服务器内部错误 | 联系技术支持排查                 |

### 业务错误码

| 错误码 | 说明             | 处理建议                            |
| ------ | ---------------- | ----------------------------------- |
| `1001` | 标签不存在       | 检查 tagIds 是否有效                |
| `1002` | 商品不存在       | 检查 spu 编码是否正确               |
| `1003` | 规格选项无效     | 检查 optionList 格式和值是否合法    |
| `1004` | 变体信息不完整   | 检查 productVariantList 是否完整    |
| `1005` | 媒体链接失效     | 检查 mediaUrlList 中的 URL 是否有效 |
| `1006` | 商品名称不能为空 | 检查 productName 是否已填写         |

### 常见问题排查

1. **返回 400 错误**
   - 检查请求 JSON 格式是否正确
   - 确认中文字符编码为 UTF-8
   - 验证必填参数是否完整

2. **返回空 data 数组**
   - 确认查询条件是否正确
   - 检查标签 ID 是否存在

3. **响应时间过长**
   - 检查网络连接状态
   - 确认服务器负载情况

4. **商品创建失败**
   - 检查 spu 编码格式是否正确
   - 确认 mediaUrlList 中的 URL 可访问
   - 验证 productVariantList 中变体数量与规格值匹配
   - 确保 productName 已填写且不为空

---

## 更新记录

| 版本 | 日期       | 更新说明                                                       |
| ---- | ---------- | -------------------------------------------------------------- |
| 1.0  | 2026-04-18 | 初始版本，包含标签级联列表及商品创建接口                       |
| 1.1  | 2026-04-18 | 完善商品创建接口，新增 mediaUrlList、productVariantList 等字段 |
| 1.2  | 2026-04-18 | 新增 productName 字段，调整 productVariantList 为顶层参数      |

---

_文档生成时间：2026-04-18_
