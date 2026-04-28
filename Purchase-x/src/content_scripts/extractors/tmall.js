import "./base.js";

class TmallExtractor extends BaseExtractor {
  constructor() {
    super();
    this.platform = "tmall";
    this._scriptData = null;
    this._scriptDataParsed = false;
  }

  async extractSourceUrl() {
    try {
      const urlObj = new URL(window.location.href);
      
      // 天猫平台：从路径中提取id参数
      // 例如: /item.htm?id=123456 或 /item/123456.html
      const id = urlObj.searchParams.get('id');
      if (id) {
        return `${urlObj.protocol}//${urlObj.hostname}${urlObj.pathname}?id=${id}`;
      }
      
      // 如果没有id参数，返回原URL
      return window.location.href;
    } catch (e) {
      Logger.warn("URL精简失败", e.message);
      return window.location.href;
    }
  }

  _getPageScriptData() {
    if (this._scriptDataParsed) return this._scriptData;
    this._scriptDataParsed = true;

    try {
      const scripts = document.querySelectorAll("script");

      for (const script of scripts) {
        const text = script.textContent || "";

        if (
          text.includes("__ICE_APP_CONTEXT__") &&
          text.includes("loaderData")
        ) {
          const jsonMatch = text.match(/var\s+b\s*=\s*({[\s\S]*})\s*;/);

          if (jsonMatch && jsonMatch[1]) {
            try {
              this._scriptData = JSON.parse(jsonMatch[1]);
              Logger.info("Script 数据", "从页面 script 标签提取成功");
              return this._scriptData;
            } catch (e) {
              Logger.warn("Script 数据", "解析 JSON 失败：" + e.message);
            }
          }
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
      return scriptData.loaderData.home.data.res;
    } catch (e) {
      Logger.warn("Script 数据", "解析 res 数据路径失败");
      return null;
    }
  }

  async extractProductName() {
    Logger.step("商品名称", "正在提取天猫商品名称...");

    try {
      const resData = this._getResData();
      if (resData && resData.item && resData.item.title) {
        const name = resData.item.title;
        Logger.success("商品名称 [Script]", name);
        return name;
      }
    } catch (e) {
      Logger.warn("Script 数据提取商品名称失败，回退到 DOM", e);
    }

    try {
      const titleEl =
        document.querySelector('[class*="mainTitle--"]') ||
        document.querySelector('[class*="MainTitle--"]');
      if (titleEl) {
        const name =
          titleEl.getAttribute("title") || titleEl.textContent.trim();
        Logger.success("商品名称 [DOM]", name);
        return name;
      }
      const metaTitle = document.querySelector('meta[name="title"]');
      if (metaTitle) {
        return metaTitle.getAttribute("content") || document.title;
      }
      return document.title;
    } catch (e) {
      Logger.error("提取商品名称失败", e);
      return document.title;
    }
  }

  async extractMediaUrlList() {
    Logger.step("主图列表", "正在提取天猫商品主图...");
    const images = [];
    const seenUrls = new Set();

    try {
      const resData = this._getResData();
      if (
        resData &&
        resData.item &&
        resData.item.images &&
        resData.item.images.length > 0
      ) {
        for (const imgUrl of resData.item.images) {
          const normalizedUrl = normalizeImageUrl(imgUrl);
          if (normalizedUrl && !seenUrls.has(normalizedUrl)) {
            seenUrls.add(normalizedUrl);
            images.push(normalizedUrl);
          }
        }

        if (resData.skuBase && resData.skuBase.props) {
          for (const prop of resData.skuBase.props) {
            if (prop.values) {
              for (const val of prop.values) {
                if (val.image) {
                  const normalizedUrl = normalizeImageUrl(val.image);
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
      const thumbnailSelectors = [
        '[class*="thumbnailPic--"]',
        '[class*="ThumbnailPic--"]',
        ".PicGallery--thumbnails img",
        '[class*="picGallery--"] img',
        '[class*="PicGallery--"] img',
        ".main-img-wrapper img",
        "#J_UlThumb img",
        ".tb-img img",
        "#J_GalleryThumbs img",
      ];

      for (const selector of thumbnailSelectors) {
        const thumbnailImgs = document.querySelectorAll(selector);
        Logger.info(`选择器 ${selector} 找到 ${thumbnailImgs.length} 个元素`);

        for (const img of thumbnailImgs) {
          let src =
            img.getAttribute("src") ||
            img.getAttribute("data-src") ||
            img.dataset.src ||
            "";

          if (src && !src.includes("canshu") && !src.includes("placeholder")) {
            const normalizedUrl = normalizeImageUrl(src);
            if (!seenUrls.has(normalizedUrl)) {
              seenUrls.add(normalizedUrl);
              images.push(normalizedUrl);
            }
          }
        }

        if (images.length > 0) break;
      }

      if (images.length === 0) {
        Logger.info("缩略图未找到，尝试获取主图区域...");

        const mainPicSelectors = [
          "#mainPicImageEl",
          '[class*="mainPic--"]',
          '[class*="MainPic--"]',
          ".main-pic img",
          "#J_GalleryMain img",
          '[class*="picGalleryMain--"] img',
          ".detail-gallery-img",
        ];

        for (const selector of mainPicSelectors) {
          const mainPicEl = document.querySelector(selector);
          if (mainPicEl) {
            Logger.info(`主图选择器 ${selector} 找到元素`);

            let src =
              mainPicEl.getAttribute("src") ||
              mainPicEl.getAttribute("data-src") ||
              mainPicEl.dataset.src ||
              "";

            if (!src) {
              const bgImage = mainPicEl.style.backgroundImage || "";
              const match = bgImage.match(/url\(["']?([^"')]+)["']?\)/);
              if (match) {
                src = match[1];
              }
            }

            if (src) {
              const normalizedUrl = normalizeImageUrl(src);
              if (!seenUrls.has(normalizedUrl)) {
                seenUrls.add(normalizedUrl);
                images.push(normalizedUrl);
              }
            }
          }

          if (images.length > 0) break;
        }
      }

      if (images.length === 0) {
        Logger.info("尝试从页面所有图片中筛选...");
        const allImages = document.querySelectorAll("img");
        for (const img of allImages) {
          const src =
            img.getAttribute("src") || img.getAttribute("data-src") || "";
          if (
            src &&
            (src.includes("tmall.com") ||
              src.includes("alicdn.com") ||
              src.includes("taobaocdn.com"))
          ) {
            if (
              src.includes(".jpg") ||
              src.includes(".png") ||
              src.includes(".jpeg")
            ) {
              const normalizedUrl = normalizeImageUrl(src);
              if (
                !seenUrls.has(normalizedUrl) &&
                !src.includes("avatar") &&
                !src.includes("logo")
              ) {
                seenUrls.add(normalizedUrl);
                images.push(normalizedUrl);
                if (images.length >= 10) break;
              }
            }
          }
        }
      }

      Logger.success("主图列表 [DOM]", `提取到 ${images.length} 张图片`);
      if (images.length > 0) {
        Logger.info("主图URL示例:", images[0]);
      }
    } catch (e) {
      Logger.error("提取主图列表失败", e);
    }

    return images;
  }

  async extractOptionList() {
    Logger.step("规格选项", "正在提取天猫规格选项...");

    try {
      const resData = this._getResData();
      if (resData && resData.skuBase && resData.skuBase.props) {
        const optionList = [];

        for (const prop of resData.skuBase.props) {
          if (!prop.name || !prop.values || prop.values.length === 0) continue;

          const chineseName = prop.name.replace(/[：:]/g, "").trim();
          if (!chineseName) continue;

          const englishName = generateEnglishName(chineseName);

          const values = [];
          for (const val of prop.values) {
            const chineseValue = val.name ? val.name.trim() : "";
            if (!chineseValue) continue;

            values.push({
              chineseValue,
              englishValue: generateEnglishValue(chineseValue),
              isDisabled: !!val.empty,
              imageUrl: val.image ? normalizeImageUrl(val.image) : "",
              vid: val.vid || null,
            });
          }

          if (values.length > 0) {
            optionList.push({
              chineseName,
              englishName,
              pid: prop.pid || null,
              values,
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
      const skuItems = document.querySelectorAll('[class*="skuItem--"]');
      Logger.info(`找到 ${skuItems.length} 个SKU维度`);

      for (const skuItem of skuItems) {
        const labelEl =
          skuItem.querySelector('[class*="ItemLabel--"] span') ||
          skuItem.querySelector('[class*="labelWrap--"] span');
        const labelTitle = labelEl?.textContent?.trim() || "";

        const chineseName = labelTitle.replace(/[：:]/g, "").trim();
        if (!chineseName) continue;

        const englishName = generateEnglishName(chineseName);

        const valueItems = skuItem.querySelectorAll('[class*="valueItem--"]');
        const values = [];

        for (const valueItem of valueItems) {
          const textEl = valueItem.querySelector('[class*="valueItemText--"]');
          const chineseValue = textEl?.textContent?.trim() || "";
          if (!chineseValue) continue;

          const isDisabled = valueItem.classList
            .toString()
            .includes("isDisabled--");

          let imageUrl = "";
          const imgEl = valueItem.querySelector('[class*="valueItemImg--"]');
          if (imgEl) {
            imageUrl = normalizeImageUrl(imgEl.getAttribute("src") || "");
          }

          values.push({
            chineseValue,
            englishValue: generateEnglishValue(chineseValue),
            isDisabled,
            imageUrl,
            element: valueItem,
          });
        }

        if (values.length > 0) {
          optionList.push({
            chineseName,
            englishName,
            values,
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

  _buildSkuMap(resData) {
    const skuMap = new Map();
    const vidToNameMap = new Map();

    if (resData && resData.skuBase && resData.skuBase.props) {
      for (const prop of resData.skuBase.props) {
        if (prop.values) {
          for (const val of prop.values) {
            vidToNameMap.set(`${prop.pid}:${val.vid}`, {
              name: prop.name,
              value: val.name,
            });
          }
        }
      }
    }

    if (resData && resData.skuBase && resData.skuBase.skus) {
      for (const sku of resData.skuBase.skus) {
        if (sku.propPath && sku.skuId) {
          const parts = sku.propPath.split(";").filter(Boolean);
          const comboNames = [];
          for (const part of parts) {
            const info = vidToNameMap.get(part);
            if (info) {
              comboNames.push(info);
            }
          }
          skuMap.set(sku.skuId, comboNames);
        }
      }
    }

    return skuMap;
  }

  _parsePriceFromSkuInfo(skuInfo) {
    if (!skuInfo) return null;

    try {
      let priceObj = null;
      if (skuInfo.price && skuInfo.price.priceMoney) {
        priceObj = skuInfo.price;
      } else if (skuInfo.subPrice && skuInfo.subPrice.priceMoney) {
        priceObj = skuInfo.subPrice;
      }

      if (priceObj && priceObj.priceMoney) {
        const rawPrice = parseFloat(priceObj.priceMoney);
        if (!isNaN(rawPrice) && rawPrice > 0) {
          return rawPrice / 100;
        }
      }

      if (priceObj && priceObj.priceText) {
        const parsed = extractPriceFromText(priceObj.priceText, "tmall");
        if (parsed !== null && parsed > 0) return parsed;
      }
    } catch (e) {
      Logger.warn("解析 SKU 价格失败", e);
    }

    return null;
  }

  _extractVariantsFromScript(optionList, resData) {
    Logger.info("变体提取 [Script]", "开始从 Script JSON 数据构建变体列表");

    const sku2info = resData.skuCore.sku2info;
    const skuMap = this._buildSkuMap(resData);

    const variantList = [];
    const defaultPrice = this._parsePriceFromSkuInfo(sku2info["0"]);

    const combinations = this.generateCombinations(optionList);

    for (const combo of combinations) {
      let matchedSkuId = null;
      let matchedComboNames = null;

      outer: for (const [skuId, comboNames] of skuMap.entries()) {
        if (comboNames.length !== combo.length) continue;

        for (let i = 0; i < combo.length; i++) {
          if (combo[i].chineseValue !== comboNames[i].value) continue outer;
        }

        matchedSkuId = skuId;
        matchedComboNames = comboNames;
        break;
      }

      const skuInfo = matchedSkuId ? sku2info[matchedSkuId] : null;
      const price = this._parsePriceFromSkuInfo(skuInfo) || defaultPrice;

      let mediaUrl = "";
      if (combo.length > 0 && combo[0].imageUrl) {
        mediaUrl = combo[0].imageUrl;
      }

      const isAvailable = skuInfo
        ? skuInfo.quantityText !== "缺货" &&
          skuInfo.quantityText !== "已售罄" &&
          skuInfo.quantity > 0
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
    Logger.step("商品变体", "正在提取天猫商品变体信息...");

    const resData = this._getResData();
    const hasScriptData = !!(
      resData &&
      resData.skuCore &&
      resData.skuCore.sku2info &&
      resData.skuBase &&
      resData.skuBase.skus
    );

    if (hasScriptData && optionList && optionList.length > 0) {
      const hasVidInOptions = optionList.some(
        (opt) => opt.pid && opt.values.some((v) => v.vid),
      );

      if (hasVidInOptions) {
        Logger.info(
          "变体提取",
          "检测到 Script 数据，优先从 JSON 直接构建变体列表",
        );
        return this._extractVariantsFromScript(optionList, resData);
      }
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
    const skuArea =
      document.querySelector("#skuOptionsArea") ||
      document.querySelector('[class*="skuWrapper--"]');

    if (!skuArea) {
      Logger.warn("未找到SKU区域");
      return variantList;
    }

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

        await this._selectCombo(combo, skuArea, speedProfile);

        const priceChanged = await waitForDOMChange(
          '[class*="priceWrap--"], [class*="highlightPrice--"], [class*="normalPrice--"]',
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

  async _selectCombo(combo, skuArea, speedProfile) {
    for (const option of combo) {
      const skuItems = skuArea.querySelectorAll('[class*="skuItem--"]');
      let targetSkuItem = null;

      for (const skuItem of skuItems) {
        const labelEl =
          skuItem.querySelector('[class*="ItemLabel--"] span') ||
          skuItem.querySelector('[class*="labelWrap--"] span');
        const cleanLabel = (labelEl?.textContent?.trim() || "")
          .replace(/[：:]/g, "")
          .trim();

        if (cleanLabel === option.chineseName) {
          targetSkuItem = skuItem;
          break;
        }
      }

      if (!targetSkuItem) continue;

      const valueItems = targetSkuItem.querySelectorAll(
        '[class*="valueItem--"]',
      );
      for (const valueItem of valueItems) {
        const textEl = valueItem.querySelector('[class*="valueItemText--"]');
        const valueText = textEl?.textContent?.trim() || "";

        if (valueText === option.chineseValue) {
          const isDisabled = valueItem.classList
            .toString()
            .includes("isDisabled--");
          const isSelected = valueItem.classList
            .toString()
            .includes("isSelected--");

          if (!isDisabled && !isSelected) {
            if (typeof BehaviorSimulator !== "undefined") {
              await BehaviorSimulator.clickWithHumanDelay(
                valueItem,
                speedProfile,
              );
            } else {
              valueItem.click();
              await waitForDOMChange(valueItem, { timeout: 800 });
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
        '[class*="highlightPrice--"] [class*="text--"]',
        '[class*="normalPrice--"] [class*="text--"]',
        '[class*="beltPrice--"] [class*="text--"]',
        '[class*="priceWrap--"] [class*="text--"]',
      ];

      for (const selector of priceSelectors) {
        const priceEls = document.querySelectorAll(selector);
        for (const el of priceEls) {
          const text = el.textContent.trim();
          const price = extractPriceFromText(text);
          if (price !== null && price > 0) {
            return price;
          }
        }
      }
    } catch (e) {
      Logger.error("获取当前价格失败", e);
    }
    return null;
  }

  async getCurrentMainImage() {
    try {
      const mainPicEl =
        document.querySelector("#mainPicImageEl") ||
        document.querySelector('[class*="mainPic--"]');
      if (mainPicEl) {
        const src = mainPicEl.getAttribute("src") || "";
        return normalizeImageUrl(src);
      }
    } catch (e) {
      Logger.error("获取当前主图失败", e);
    }
    return "";
  }
}

if (typeof window !== "undefined") {
  window.TmallExtractor = TmallExtractor;
}
