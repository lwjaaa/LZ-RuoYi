class BaseExtractor {
  constructor() {
    this.platform = "base";
  }

  async extractProductName() {
    throw new Error("extractProductName must be implemented");
  }

  async extractSourceUrl() {
    return window.location.href;
  }

  async extractMediaUrlList() {
    throw new Error("extractMediaUrlList must be implemented");
  }

  async extractOptionList() {
    throw new Error("extractOptionList must be implemented");
  }

  async extractProductVariantList(optionList) {
    throw new Error("extractProductVariantList must be implemented");
  }

  async extractAll() {
    Logger.step("START", `开始提取 ${this.platform} 平台商品数据`);

    const productName = await this.extractProductName();
    Logger.success("商品名称", productName);

    const sourceUrl = await this.extractSourceUrl();
    Logger.info("来源URL", sourceUrl);

    const mediaUrlList = await this.extractMediaUrlList();
    Logger.success("主图列表", `${mediaUrlList.length} 张图片`);

    const optionList = await this.extractOptionList();
    Logger.success("规格选项", `${optionList.length} 个规格维度`);
    console.log(optionList);
    console.log("optionList", optionList);
    const productVariantList = await this.extractProductVariantList(optionList);
    Logger.success("商品变体", `${productVariantList.length} 个变体`);

    const result = {
      productName,
      sourceUrl,
      mediaUrlList,
      optionList,
      productVariantList,
    };

    Logger.step("COMPLETE", "商品数据提取完成");
    return result;
  }

  async clickElementAndWait(element, waitTime = 800) {
    if (typeof BehaviorSimulator !== "undefined") {
      await BehaviorSimulator.clickWithHumanDelay(element);
    } else {
      element.click();
      await delay(waitTime);
    }
  }

  async waitForPriceUpdate(previousPrice, maxWait = 3000) {
    const startTime = Date.now();
    while (Date.now() - startTime < maxWait) {
      await delay(300);
      const currentPrice = await this.getCurrentPrice();
      if (currentPrice !== null && currentPrice !== previousPrice) {
        return currentPrice;
      }
    }
    return null;
  }

  async getCurrentPrice() {
    throw new Error("getCurrentPrice must be implemented");
  }

  async getCurrentMainImage() {
    throw new Error("getCurrentMainImage must be implemented");
  }

  generateCombinations(optionList) {
    if (!optionList || optionList.length === 0) return [[]];

    const result = [[]];
    for (const option of optionList) {
      const newResult = [];
      for (const existing of result) {
        for (const value of option.values) {
          newResult.push([
            ...existing,
            {
              chineseName: option.chineseName,
              englishName: option.englishValue,
              chineseValue: value.chineseValue,
              englishValue: value.englishValue,
              isDisabled: value.isDisabled || false,
            },
          ]);
        }
      }
      result.length = 0;
      result.push(...newResult);
    }
    return result;
  }
}
