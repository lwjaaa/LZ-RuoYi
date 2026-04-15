# ExchangeRate-API 集成说明

## 概述
已将汇率服务从 Fixer API 迁移至 ExchangeRate-API (v6)，用于获取美元与人民币之间的汇率。

## 主要变更

### 1. API提供商变更
- **原API**: Fixer API (http://data.fixer.io/api)
- **新API**: ExchangeRate-API v6 (https://v6.exchangerate-api.com/v6)

### 2. 配置变更
配置文件位置: `ruoyi-admin/src/main/resources/application-dev.yml`

```yaml
exchange-rate:
    # API Key (从 https://www.exchangerate-api.com/ 获取)
    api-key: YOUR_API_KEY_HERE
    # API基础URL
    base-url: https://v6.exchangerate-api.com/v6
    # 连接超时时间（毫秒）
    connect-timeout: 5000
    # 读取超时时间（毫秒）
    read-timeout: 10000
    # 默认基准货币
    default-base-currency: USD
    # 目标货币列表，多个用英文逗号隔开
    target-currencies: CNY
```

### 3. 新增文件

#### ExchangeRateApiResponse.java
- 路径: `vh-erp/src/main/java/com/ruoyi/erp/model/dto/base/ExchangeRateApiResponse.java`
- 用途: ExchangeRate-API 响应数据DTO

#### ExchangeRateTask.java
- 路径: `vh-erp/src/main/java/com/ruoyi/erp/task/ExchangeRateTask.java`
- 用途: 汇率同步定时任务

### 4. 修改文件

#### FixerConfig.java
- 配置前缀从 `fixer` 改为 `exchange-rate`
- 默认基准货币从 `CNY` 改为 `USD`
- 默认目标货币从 `USD` 改为 `CNY`
- API基础URL更新为 ExchangeRate-API

#### ExchangeRateServiceImpl.java
- 改用 ExchangeRate-API 接口
- URL格式: `https://v6.exchangerate-api.com/v6/{API_KEY}/latest/{BASE_CODE}`
- 响应解析改用 `ExchangeRateApiResponse`
- 优化缓存键格式: `erp:exchange_rate:{date}:{baseCurrency}:{targetCurrency}`
- 添加汇率过滤功能

#### ExchangeRateController.java
- 默认基准货币从 `CNY` 改为 `USD`

## 使用步骤

### 1. 获取API Key
1. 访问 https://www.exchangerate-api.com/
2. 注册账号
3. 获取免费的API Key（每月1500次请求）
4. 如需更多请求次数，可升级付费套餐

### 2. 配置API Key
在 `application-dev.yml` 中配置:
```yaml
exchange-rate:
    api-key: your_actual_api_key_here
```

### 3. 配置定时任务（可选）
在若依系统的定时任务管理中添加：

**任务名称**: 每日汇率同步
**调用目标**: `exchangeRateTask.syncDailyRates`
**Cron表达式**: `0 0 8 * * ?` (每天早上8点执行)
**执行策略**: 允许并发执行

### 4. API接口使用

#### 获取今日汇率
```
GET /erp/exchange-rate/today/CNY?baseCurrency=USD
```
返回:
```json
{
  "code": 200,
  "data": {
    "baseCurrency": "USD",
    "targetCurrency": "CNY",
    "rate": 7.25,
    "rateDate": "2024-01-15"
  }
}
```

#### 货币转换
```
POST /erp/exchange-rate/convert
Content-Type: application/json

{
  "amount": 1000,
  "fromCurrency": "USD",
  "toCurrency": "CNY"
}
```
返回:
```json
{
  "code": 200,
  "data": {
    "fromCurrency": "USD",
    "toCurrency": "CNY",
    "amount": 1000,
    "convertedAmount": 7250.00
  }
}
```

## 技术特性

### 缓存机制
- **缓存键格式**: `erp:exchange_rate:{date}:{baseCurrency}:{targetCurrency}`
- **缓存有效期**: 24小时
- **缓存策略**: 
  - 首次查询时调用API并缓存所有目标货币的汇率
  - 后续查询直接从Redis缓存获取
  - 每天自动更新缓存

### 错误处理
- API调用失败时抛出 RuntimeException
- 详细的日志记录
- 参数校验（金额、货币代码等）

### 汇率过滤
- 支持通过 `symbols` 参数指定需要获取的货币
- 自动过滤只返回配置的目标货币

## 注意事项

1. **API限制**: 免费版每月1500次请求，请合理使用
2. **缓存策略**: 建议启用定时任务，避免频繁调用API
3. **货币方向**: 当前配置为 USD -> CNY，如需其他货币对，修改配置文件
4. **时区问题**: 汇率日期使用服务器当地时区

## 扩展其他货币对

如需添加其他货币（如EUR、GBP等），修改配置：

```yaml
exchange-rate:
    target-currencies: CNY,EUR,GBP
```

系统会自动获取并缓存这些货币对的汇率。

## 故障排查

### 问题1: API调用失败
- 检查API Key是否正确
- 确认网络连接正常
- 查看日志中的详细错误信息

### 问题2: 汇率数据为空
- 确认目标货币代码正确（使用ISO 4217标准）
- 检查API配额是否用完
- 查看ExchangeRate-API状态页面

### 问题3: 缓存未生效
- 检查Redis连接是否正常
- 确认缓存键格式正确
- 查看Redis中是否有对应的key
