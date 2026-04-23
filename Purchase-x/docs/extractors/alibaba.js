class AlibabaExtractor extends BaseExtractor {
  constructor() {
    super();
    this.platform = "1688";
  }

  async extractProductName() {
    Logger.step("商品名称", "正在提取1688商品名称...");
    try {
      const titleEl =
        document.querySelector(".module-od-title .title-content h1") ||
        document.querySelector("#productTitle h1") ||
        document.querySelector('[class*="title-content"] h1');

      if (titleEl) {
        const name = titleEl.textContent.trim();
        Logger.success("商品名称", name);
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

    try {
      const previewImgs = document.querySelectorAll(
        ".od-gallery-preview .ant-image-img.preview-img",
      );
      const seenUrls = new Set();

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

      Logger.success("主图列表", `提取到 ${images.length} 张图片`);
    } catch (e) {
      Logger.error("提取主图列表失败", e);
    }

    return images;
  }

  async extractOptionList() {
    Logger.step("规格选项", "正在提取1688规格选项...");
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
            englishValue: englishName,
            values,
            type: filterButtons.length > 0 ? "filter" : "expand",
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
    Logger.step("商品变体", "正在提取1688商品变体信息...");

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

        await this._selectCombo(combo, optionList, speedProfile);

        if (
          typeof BehaviorSimulator !== "undefined" &&
          !BehaviorSimulator.isInBackground()
        ) {
          await BehaviorSimulator.waitForDelay(500, 1000);
        }

        const priceChanged = await waitForDOMChange(
          ".price-comp, .od-price-container, [class*='price-comp']",
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

    const sortedVariantList = this.restoreOriginalOrder(
      effectiveCombinations,
      extractionMap,
    );

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
