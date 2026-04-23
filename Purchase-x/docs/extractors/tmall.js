class TmallExtractor extends BaseExtractor {
  constructor() {
    super();
    this.platform = "tmall";
  }

  async extractProductName() {
    Logger.step("商品名称", "正在提取天猫商品名称...");
    try {
      const titleEl =
        document.querySelector('[class*="mainTitle--"]') ||
        document.querySelector('[class*="MainTitle--"]');
      if (titleEl) {
        const name =
          titleEl.getAttribute("title") || titleEl.textContent.trim();
        Logger.success("商品名称", name);
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

      Logger.success("主图列表", `提取到 ${images.length} 张图片`);
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
            englishValue: englishName,
            values,
          });
        }
      }

      Logger.success("规格选项", `提取到 ${optionList.length} 个规格维度`);
    } catch (e) {
      Logger.error("提取规格选项失败", e);
    }

    return optionList;
  }

  async extractProductVariantList(optionList) {
    Logger.step("商品变体", "正在提取天猫商品变体信息...");

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

      if (speedProfile.maxCombinations && totalCombinations > speedProfile.maxCombinations) {
        effectiveCombinations = combinations.slice(0, speedProfile.maxCombinations);
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

    const shuffledCombinations = this.shuffleCombinations(effectiveCombinations);
    const extractionMap = new Map();

    for (let i = 0; i < shuffledCombinations.length; i++) {
      const combo = shuffledCombinations[i];
      const displayIndex = i + 1;
      const comboKey = this.generateComboKey(combo);
      
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

        await this._selectCombo(combo, skuArea, speedProfile);
        
        if (typeof BehaviorSimulator !== "undefined" && !BehaviorSimulator.isInBackground()) {
          await BehaviorSimulator.waitForDelay(500, 1000);
        }
        
        const priceChanged = await waitForDOMChange(
          '[class*="priceWrap--"], [class*="highlightPrice--"], [class*="normalPrice--"]',
          { timeout: 1200 },
        );
        if (!priceChanged) {
          await delay(800);
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

    const sortedVariantList = this.restoreOriginalOrder(effectiveCombinations, extractionMap);

    Logger.success("商品变体", `提取到 ${sortedVariantList.length} 个变体`);
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
    return combo
      .map((c) => `${c.chineseName}:${c.chineseValue}`)
      .join("|");
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
              await BehaviorSimulator.clickWithHumanDelay(valueItem, speedProfile);
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
