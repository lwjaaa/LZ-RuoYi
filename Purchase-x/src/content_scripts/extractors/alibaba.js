import "./base.js";

class AlibabaExtractor extends BaseExtractor {
  constructor() {
    super();
    this.platform = "1688";
    this._scriptData = null;
    this._scriptDataParsed = false;
  }

  _getPageScriptData() {
    if (this._scriptDataParsed) return this._scriptData;
    this._scriptDataParsed = true;
    const contextRegex = /\}\)\(window\.contextPath,\s*(\{[\s\S]*?\})\s*\);/;
    try {
      const script = Array.from(document.querySelectorAll("script")).find((s) =>
        s.textContent.includes("window.contextPath"),
      );

      if (script) {
        // 2. 创建临时函数执行脚本，拿到 context
        const match = script.textContent.match(contextRegex);
        if (match && match[1]) {
          this._scriptData = JSON.parse(match[1].trim());
          Logger.info("Script 数据", "从页面 script 标签提取成功");
        }
      }
    } catch (e) {
      Logger.warn("Script 数据", "读取页面脚本数据失败：" + e.message);
    }

    Logger.info("Script 数据", "未找到页面脚本数据，将使用 DOM 提取");
    return null;
  }

  _getResData() {
    const scriptData = this._getPageScriptData();
    if (!scriptData) return null;
    try {
      return scriptData.result.data;
    } catch (e) {
      Logger.warn("Script 数据", "解析 result.data 路径失败");
      return null;
    }
  }

  async extractProductName() {
    Logger.step("商品名称", "正在提取1688商品名称...");

    try {
      const resData = this._getResData();
      if (resData) {
        let name = "";
        if (resData.tempModel && resData.tempModel.offerTitle) {
          name = resData.tempModel.offerTitle;
        } else if (
          resData.offer &&
          resData.offer.fields &&
          resData.offer.fields.subject
        ) {
          name = resData.offer.fields.subject;
        }
        if (name) {
          Logger.success("商品名称 [Script]", name);
          return name;
        }
      }
    } catch (e) {
      Logger.warn("Script 数据提取商品名称失败，回退到 DOM", e);
    }

    try {
      const titleEl =
        document.querySelector(".module-od-title .title-content h1") ||
        document.querySelector("#productTitle h1") ||
        document.querySelector('[class*="title-content"] h1');

      if (titleEl) {
        const name = titleEl.textContent.trim();
        Logger.success("商品名称 [DOM]", name);
        return name;
      }

      return document.title;
    } catch (e) {
      Logger.error("提取商品名称失败", e);
      return document.title;
    }
  }

  async extractMediaUrlList() {
    Logger.step("主图列表", "正在提取1688商品主图...");
    const images = [];
    const seenUrls = new Set();

    try {
      const resData = this._getResData();
      if (resData) {
        if (
          resData.gallery &&
          resData.gallery.fields &&
          resData.gallery.fields.mainImage &&
          resData.gallery.fields.mainImage.length > 0
        ) {
          for (const imgUrl of resData.gallery.fields.mainImage) {
            const normalizedUrl = normalizeImageUrl(imgUrl);
            if (normalizedUrl && !seenUrls.has(normalizedUrl)) {
              seenUrls.add(normalizedUrl);
              images.push(normalizedUrl);
            }
          }
        }

        if (
          resData.offer &&
          resData.offer.fields &&
          resData.offer.fields.imageList &&
          resData.offer.fields.imageList.length > 0
        ) {
          for (const imgItem of resData.offer.fields.imageList) {
            const imgUrl = imgItem.fullPathImageURI;
            if (imgUrl) {
              const normalizedUrl = normalizeImageUrl(imgUrl);
              if (normalizedUrl && !seenUrls.has(normalizedUrl)) {
                seenUrls.add(normalizedUrl);
                images.push(normalizedUrl);
              }
            }
          }
        }

        if (resData.skuModel && resData.skuModel.skuProps) {
          for (const prop of resData.skuModel.skuProps) {
            if (prop.value) {
              for (const val of prop.value) {
                if (val.imageUrl) {
                  const normalizedUrl = normalizeImageUrl(val.imageUrl);
                  if (normalizedUrl && !seenUrls.has(normalizedUrl)) {
                    seenUrls.add(normalizedUrl);
                    images.push(normalizedUrl);
                  }
                }
              }
            }
          }
        }

        if (images.length > 0) {
          Logger.success("主图列表 [Script]", `提取到 ${images.length} 张图片`);
          if (images.length > 0) {
            Logger.info("主图 URL 示例:", images[0]);
          }
          return images;
        }
      }
    } catch (e) {
      Logger.warn("Script 数据提取主图失败，回退到 DOM", e);
    }

    try {
      const previewImgs = document.querySelectorAll(
        ".od-gallery-preview .ant-image-img.preview-img",
      );

      for (const img of previewImgs) {
        const src = img.getAttribute("src") || "";
        if (src) {
          const normalizedUrl = normalizeImageUrl(src);
          if (!seenUrls.has(normalizedUrl)) {
            seenUrls.add(normalizedUrl);
            images.push(normalizedUrl);
          }
        }
      }

      if (images.length === 0) {
        const scrollerItems = document.querySelectorAll(
          ".od-scroller-item .v-image-cover",
        );
        for (const item of scrollerItems) {
          const bgImage = item.style.backgroundImage || "";
          const match = bgImage.match(/url\(["']?([^"')]+)["']?\)/);
          if (match) {
            const url = match[1].replace(/_b\.jpg/, ".jpg");
            const normalizedUrl = normalizeImageUrl(url);
            if (!seenUrls.has(normalizedUrl)) {
              seenUrls.add(normalizedUrl);
              images.push(normalizedUrl);
            }
          }
        }
      }

      Logger.success("主图列表 [DOM]", `提取到 ${images.length} 张图片`);
    } catch (e) {
      Logger.error("提取主图列表失败", e);
    }

    return images;
  }

  async extractOptionList() {
    Logger.step("规格选项", "正在提取1688规格选项...");

    try {
      const resData = this._getResData();
      if (
        resData &&
        resData.skuModel &&
        resData.skuModel.skuProps &&
        resData.skuModel.skuProps.length > 0
      ) {
        const optionList = [];

        for (const prop of resData.skuModel.skuProps) {
          if (!prop.prop || !prop.value || prop.value.length === 0) continue;

          const chineseName = prop.prop.replace(/[：:]/g, "").trim();
          if (!chineseName) continue;

          const englishName = generateEnglishName(chineseName, "1688");

          const values = [];
          for (const val of prop.value) {
            const chineseValue = val.name ? val.name.trim() : "";
            if (!chineseValue) continue;

            values.push({
              chineseValue,
              englishValue: generateEnglishValue(
                chineseValue,
                chineseName,
                "1688",
              ),
              isDisabled: false,
              imageUrl: val.imageUrl ? normalizeImageUrl(val.imageUrl) : "",
              fid: prop.fid || null,
            });
          }

          if (values.length > 0) {
            optionList.push({
              chineseName,
              englishName,
              values,
              type: "filter",
            });
          }
        }

        if (optionList.length > 0) {
          Logger.success(
            "规格选项 [Script]",
            `提取到 ${optionList.length} 个规格维度`,
          );
          for (const opt of optionList) {
            Logger.info(`  - ${opt.chineseName}: ${opt.values.length} 个选项`);
          }
          return optionList;
        }
      }
    } catch (e) {
      Logger.warn("Script 数据提取规格选项失败，回退到 DOM", e);
    }

    const optionList = [];

    try {
      const featureItems = document.querySelectorAll(
        "#skuSelection .feature-item",
      );
      Logger.info(`找到 ${featureItems.length} 个规格维度`);

      for (const featureItem of featureItems) {
        const labelEl = featureItem.querySelector(".feature-item-label h3");
        const chineseName = labelEl?.textContent?.trim() || "";
        if (!chineseName) continue;

        const englishName = generateEnglishName(chineseName);

        const filterButtons =
          featureItem.querySelectorAll(".sku-filter-button");
        const expandItems = featureItem.querySelectorAll(".expand-view-item");

        const values = [];

        if (filterButtons.length > 0) {
          for (const btn of filterButtons) {
            const nameEl = btn.querySelector(".label-name");
            const chineseValue = nameEl?.textContent?.trim() || "";
            if (!chineseValue) continue;

            let imageUrl = "";
            const imgEl = btn.querySelector(".label-image-wrap img");
            if (imgEl) {
              imageUrl = normalizeImageUrl(imgEl.getAttribute("src") || "");
            }

            const isActive = btn.classList.contains("active");

            values.push({
              chineseValue,
              englishValue: generateEnglishValue(chineseValue),
              isDisabled: false,
              imageUrl,
              element: btn,
              isActive,
            });
          }
        } else if (expandItems.length > 0) {
          for (const item of expandItems) {
            const labelSpan = item.querySelector(".item-label");
            const chineseValue = labelSpan?.textContent?.trim() || "";
            if (!chineseValue) continue;

            const priceSpan = item.querySelector(".item-price-stock");
            const priceText = priceSpan?.textContent?.trim() || "";

            values.push({
              chineseValue,
              englishValue: generateEnglishValue(chineseValue),
              isDisabled: false,
              imageUrl: "",
              priceText,
              element: item,
            });
          }
        }

        if (values.length > 0) {
          optionList.push({
            chineseName,
            englishName,
            values,
            type: filterButtons.length > 0 ? "filter" : "expand",
          });
        }
      }

      Logger.success(
        "规格选项 [DOM]",
        `提取到 ${optionList.length} 个规格维度`,
      );
    } catch (e) {
      Logger.error("提取规格选项失败", e);
    }

    return optionList;
  }

  _parseSpecAttrs(specAttrs) {
    if (!specAttrs) return [];
    const decoded = specAttrs.replace(/&gt;/g, ">");
    return decoded.split(">").filter(Boolean);
  }

  _extractVariantsFromScript(optionList, resData) {
    Logger.info("变体提取 [Script]", "开始从 Script JSON 数据构建变体列表");

    const skuInfoMap = resData.skuModel.skuInfoMap;
    const skuMap = resData.skuModel.skuMap;

    const variantList = [];

    let defaultPrice = null;
    if (
      resData.orderParamModel &&
      resData.orderParamModel.orderParam &&
      resData.orderParamModel.orderParam.skuParam &&
      resData.orderParamModel.orderParam.skuParam.skuRangePrices
    ) {
      const rangePrices =
        resData.orderParamModel.orderParam.skuParam.skuRangePrices;
      if (rangePrices && rangePrices.length > 0) {
        defaultPrice = parseFloat(rangePrices[0].price);
      }
    }

    const combinations = this.generateCombinations(optionList);

    for (const combo of combinations) {
      const comboKey = combo.map((c) => c.chineseValue).join(">");

      let matchedSkuInfo = null;

      if (skuInfoMap) {
        for (const [mapKey, skuInfo] of Object.entries(skuInfoMap)) {
          const parts = this._parseSpecAttrs(skuInfo.specAttrs);
          if (parts.length !== combo.length) continue;

          let match = true;
          for (let i = 0; i < combo.length; i++) {
            if (combo[i].chineseValue !== parts[i]) {
              match = false;
              break;
            }
          }

          if (match) {
            matchedSkuInfo = skuInfo;
            break;
          }
        }
      }

      if (!matchedSkuInfo && skuMap) {
        for (const skuItem of skuMap) {
          const parts = this._parseSpecAttrs(skuItem.specAttrs);
          if (parts.length !== combo.length) continue;

          let match = true;
          for (let i = 0; i < combo.length; i++) {
            if (combo[i].chineseValue !== parts[i]) {
              match = false;
              break;
            }
          }

          if (match) {
            matchedSkuInfo = skuItem;
            break;
          }
        }
      }

      let price = defaultPrice;
      if (matchedSkuInfo) {
        const discountPrice = parseFloat(matchedSkuInfo.discountPrice);
        const normalPrice = parseFloat(matchedSkuInfo.price);
        if (!isNaN(discountPrice) && discountPrice > 0) {
          price = discountPrice;
        } else if (!isNaN(normalPrice) && normalPrice > 0) {
          price = normalPrice;
        }
      }

      let mediaUrl = "";
      if (combo.length > 0 && combo[0].imageUrl) {
        mediaUrl = combo[0].imageUrl;
      }

      const isAvailable = matchedSkuInfo
        ? matchedSkuInfo.canBookCount > 0
        : true;

      variantList.push({
        purchaseUrl: window.location.href,
        purchasePrice: price,
        mediaUrl,
        isActiveAvailable: isAvailable ? 1 : 0,
        optionValueList: combo.map((c) => ({
          chineseValue: c.chineseValue,
          englishValue: c.englishValue,
          chineseName: c.chineseName,
          englishName: c.englishName,
        })),
      });
    }

    Logger.success("商品变体 [Script]", `提取到 ${variantList.length} 个变体`);
    return variantList;
  }

  async extractProductVariantList(optionList) {
    Logger.step("商品变体", "正在提取1688商品变体信息...");

    const resData = this._getResData();
    const hasScriptData = !!(
      resData &&
      resData.skuModel &&
      (resData.skuModel.skuInfoMap || resData.skuModel.skuMap)
    );

    if (hasScriptData && optionList && optionList.length > 0) {
      Logger.info(
        "变体提取",
        "检测到 Script 数据，优先从 JSON 直接构建变体列表",
      );
      return this._extractVariantsFromScript(optionList, resData);
    }

    if (!optionList || optionList.length === 0) {
      Logger.info("无规格选项，生成单一变体");
      const price = await this.getCurrentPrice();
      const mediaUrl = await this.getCurrentMainImage();
      return [
        {
          purchaseUrl: window.location.href,
          purchasePrice: price,
          mediaUrl: mediaUrl || "",
          optionValueList: [],
        },
      ];
    }

    const hasFilterOptions = optionList.some((opt) => opt.type === "filter");
    const hasExpandOptions = optionList.some((opt) => opt.type === "expand");

    if (hasExpandOptions && !hasFilterOptions) {
      return this._extractExpandVariants(optionList);
    }

    const combinations = this.generateCombinations(optionList);
    const totalCombinations = combinations.length;

    let speedProfile = null;
    let effectiveCombinations = combinations;

    if (typeof BehaviorSimulator !== "undefined") {
      speedProfile = BehaviorSimulator.getSpeedProfile(totalCombinations);
      Logger.info(
        `速度模式: ${speedProfile.name}, 总组合数: ${totalCombinations}`,
      );

      if (
        speedProfile.maxCombinations &&
        totalCombinations > speedProfile.maxCombinations
      ) {
        effectiveCombinations = combinations.slice(
          0,
          speedProfile.maxCombinations,
        );
        Logger.warn(
          `组合数超过上限，仅提取前 ${speedProfile.maxCombinations} 个`,
        );
      }
    } else {
      Logger.info(`共 ${totalCombinations} 个规格组合需要遍历`);
    }

    const variantList = [];

    const shuffledCombinations = this.shuffleCombinations(
      effectiveCombinations,
    );
    const extractionMap = new Map();

    for (let i = 0; i < shuffledCombinations.length; i++) {
      const combo = shuffledCombinations[i];
      const displayIndex = i + 1;
      const comboKey = this.generateComboKey(combo);

      if (typeof AntiDetection !== "undefined") {
        const status = AntiDetection.check();
        if (status !== "ok") {
          Logger.warn("反爬拦截", `检测到: ${status}，停止遍历`);
          break;
        }
      }

      Logger.info(
        `处理组合 ${displayIndex}/${shuffledCombinations.length}: ${combo.map((c) => c.chineseValue).join(" / ")}`,
      );

      const hasDisabled = combo.some((c) => c.isDisabled);

      if (hasDisabled) {
        Logger.info(`  组合包含禁用规格，记录空数据`);
        extractionMap.set(comboKey, {
          purchaseUrl: window.location.href,
          purchasePrice: null,
          mediaUrl: "",
          optionValueList: combo.map((c) => ({
            chineseValue: c.chineseValue,
            englishValue: c.englishValue,
            chineseName: c.chineseName,
            englishName: c.englishName,
          })),
        });
        continue;
      }

      try {
        if (
          typeof BehaviorSimulator !== "undefined" &&
          speedProfile &&
          i > 0 &&
          i % speedProfile.behaviorFrequency === 0
        ) {
          await BehaviorSimulator.randomPageBehavior();
        }

        if (typeof BehaviorSimulator !== "undefined") {
          await BehaviorSimulator.microBreak();
        }

        await this._selectCombo(combo, optionList, speedProfile);

        const priceChanged = await waitForDOMChange(
          ".price-comp, .od-price-container, [class*='price-comp']",
          { timeout: 1000 },
        );
        if (!priceChanged) {
          await BehaviorSimulator.waitForDelay(300, 600);
        }

        const price = await this.getCurrentPrice();
        const mediaUrl = await this.getCurrentMainImage();

        extractionMap.set(comboKey, {
          purchaseUrl: window.location.href,
          purchasePrice: price,
          mediaUrl: mediaUrl || "",
          optionValueList: combo.map((c) => ({
            chineseValue: c.chineseValue,
            englishValue: c.englishValue,
            chineseName: c.chineseName,
            englishName: c.englishName,
          })),
        });

        Logger.info(`  价格: ${price}, 图片: ${mediaUrl ? "有" : "无"}`);
      } catch (e) {
        Logger.error(`  组合选择失败: ${e.message}`);
        extractionMap.set(comboKey, {
          purchaseUrl: window.location.href,
          purchasePrice: null,
          mediaUrl: "",
          optionValueList: combo.map((c) => ({
            chineseValue: c.chineseValue,
            englishValue: c.englishValue,
            chineseName: c.chineseName,
            englishName: c.englishName,
          })),
        });
      }
    }

    const sortedVariantList = this.restoreOriginalOrder(
      effectiveCombinations,
      extractionMap,
    );

    Logger.success(
      "商品变体 [DOM]",
      `提取到 ${sortedVariantList.length} 个变体`,
    );
    return sortedVariantList;
  }

  shuffleCombinations(combinations) {
    const shuffled = [...combinations];

    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }

    return shuffled;
  }

  generateComboKey(combo) {
    return combo.map((c) => `${c.chineseName}:${c.chineseValue}`).join("|");
  }

  restoreOriginalOrder(originalCombinations, extractionMap) {
    const result = [];

    for (const combo of originalCombinations) {
      const key = this.generateComboKey(combo);
      const data = extractionMap.get(key);

      if (data) {
        result.push(data);
      } else {
        Logger.warn(`未找到组合数据: ${key}`);
        result.push({
          purchaseUrl: window.location.href,
          purchasePrice: null,
          mediaUrl: "",
          optionValueList: combo.map((c) => ({
            chineseValue: c.chineseValue,
            englishValue: c.englishValue,
            chineseName: c.chineseName,
            englishName: c.englishName,
          })),
        });
      }
    }

    return result;
  }

  async _extractExpandVariants(optionList) {
    Logger.info("使用展开视图模式提取变体...");
    const variantList = [];

    const filterOptions = optionList.filter((opt) => opt.type === "filter");
    const expandOptions = optionList.filter((opt) => opt.type === "expand");

    const filterCombos = this.generateCombinations(filterOptions);

    for (const filterCombo of filterCombos.length > 0 ? filterCombos : [[]]) {
      if (filterCombo.length > 0) {
        await this._selectFilterCombo(filterCombo, filterOptions);
        const priceChanged = await waitForDOMChange(
          ".price-comp, .od-price-container, [class*='price-comp']",
          { timeout: 1200 },
        );
        if (!priceChanged) {
          await delay(80);
        }
      }

      const price = await this.getCurrentPrice();
      const mediaUrl = await this.getCurrentMainImage();

      if (expandOptions.length === 0) {
        variantList.push({
          purchaseUrl: window.location.href,
          purchasePrice: price,
          mediaUrl: mediaUrl || "",
          optionValueList: filterCombo.map((c) => ({
            chineseValue: c.chineseValue,
            englishValue: c.englishValue,
            chineseName: c.chineseName,
            englishName: c.englishName,
          })),
        });
        continue;
      }

      for (const expandOption of expandOptions) {
        const featureItems = document.querySelectorAll(
          "#skuSelection .feature-item",
        );
        for (const featureItem of featureItems) {
          const labelEl = featureItem.querySelector(".feature-item-label h3");
          const label = labelEl?.textContent?.trim() || "";

          if (label === expandOption.chineseName) {
            const expandItems =
              featureItem.querySelectorAll(".expand-view-item");
            for (const item of expandItems) {
              const labelSpan = item.querySelector(".item-label");
              const chineseValue = labelSpan?.textContent?.trim() || "";
              const priceSpan = item.querySelector(".item-price-stock");
              const priceText = priceSpan?.textContent?.trim() || "";
              const itemPrice = extractPriceFromText(priceText);

              variantList.push({
                purchaseUrl: window.location.href,
                purchasePrice: itemPrice || price,
                mediaUrl: mediaUrl || "",
                optionValueList: [
                  ...filterCombo.map((c) => ({
                    chineseValue: c.chineseValue,
                    englishValue: c.englishValue,
                    chineseName: c.chineseName,
                    englishName: c.englishName,
                  })),
                  {
                    chineseValue,
                    englishValue: generateEnglishValue(chineseValue),
                    chineseName: expandOption.chineseName,
                    englishName: expandOption.englishValue,
                  },
                ],
              });
            }
            break;
          }
        }
      }
    }

    Logger.success("商品变体", `提取到 ${variantList.length} 个变体`);
    return variantList;
  }

  async _selectCombo(combo, optionList, speedProfile) {
    const filterOptions = optionList.filter((opt) => opt.type === "filter");
    const filterCombo = combo.filter((c) =>
      filterOptions.some((fo) => fo.chineseName === c.chineseName),
    );

    if (filterCombo.length > 0) {
      await this._selectFilterCombo(filterCombo, filterOptions, speedProfile);
    }
  }

  async _selectFilterCombo(filterCombo, filterOptions, speedProfile) {
    for (const option of filterCombo) {
      const featureItems = document.querySelectorAll(
        "#skuSelection .feature-item",
      );
      for (const featureItem of featureItems) {
        const labelEl = featureItem.querySelector(".feature-item-label h3");
        const label = labelEl?.textContent?.trim() || "";

        if (label === option.chineseName) {
          const filterButtons =
            featureItem.querySelectorAll(".sku-filter-button");
          for (const btn of filterButtons) {
            const nameEl = btn.querySelector(".label-name");
            const btnName = nameEl?.textContent?.trim() || "";

            if (btnName === option.chineseValue) {
              if (!btn.classList.contains("active")) {
                if (typeof BehaviorSimulator !== "undefined") {
                  await BehaviorSimulator.clickWithHumanDelay(
                    btn,
                    speedProfile,
                  );
                } else {
                  btn.click();
                  await waitForDOMChange(btn, { timeout: 800 });
                }
              }
              break;
            }
          }
          break;
        }
      }
    }
  }

  async getCurrentPrice() {
    try {
      const priceSelectors = [
        ".price-comp .price-info .currency",
        ".price-comp .price-info",
        ".od-price-container .price-info",
        '[class*="price-comp"] [class*="currency"]',
      ];

      for (const selector of priceSelectors) {
        const priceEls = document.querySelectorAll(selector);
        let priceText = "";
        for (const el of priceEls) {
          priceText += el.textContent.trim();
        }
        const price = extractPriceFromText(priceText);
        if (price !== null && price > 0) {
          return price;
        }
      }
    } catch (e) {
      Logger.error("获取当前价格失败", e);
    }
    return null;
  }

  async getCurrentMainImage() {
    try {
      const activePreview = document.querySelector(
        ".od-gallery-preview .ant-image-img.active-preview-img",
      );
      if (activePreview) {
        return normalizeImageUrl(activePreview.getAttribute("src") || "");
      }

      const previewImgs = document.querySelectorAll(
        ".od-gallery-preview .ant-image-img.preview-img",
      );
      for (const img of previewImgs) {
        const src = img.getAttribute("src") || "";
        if (src) {
          return normalizeImageUrl(src);
        }
      }
    } catch (e) {
      Logger.error("获取当前主图失败", e);
    }
    return "";
  }
}

if (typeof window !== "undefined") {
  window.AlibabaExtractor = AlibabaExtractor;
}
