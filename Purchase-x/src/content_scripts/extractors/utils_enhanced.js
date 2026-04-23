// 增强的工具函数 - 针对淘宝、天猫、1688 平台优化

/**
 * 生成英文名称（规格名）
 * @param {string} chinese - 中文规格名
 * @param {string} platform - 平台：taobao/tmall/1688
 * @returns {string} 英文规格名
 */
function generateEnglishName(chinese, platform = "taobao") {
  if (!chinese) return "";

  const platformMaps = {
    taobao: {
      颜色: "color",
      颜色分类: "color",
      配色: "color_scheme",
      花色: "pattern",
      尺寸: "size",
      尺码: "size",
      规格: "specification",
      型号: "model",
      款式: "style",
      类型: "type",
      材质: "material",
      面料: "fabric",
      成分: "composition",
      风格: "style_type",
      形状: "shape",
      图案: "pattern_type",
      厚度: "thickness",
      长度: "length",
      宽度: "width",
      高度: "height",
      重量: "weight",
      容量: "capacity",
      包装: "package",
      套装: "set",
      套餐: "combo",
      版本: "version",
      配置: "configuration",
      网络: "network",
      存储: "storage",
      内存: "memory",
    },
    1688: {
      颜色: "color",
      尺寸: "size",
      规格: "spec",
      型号: "model_no",
      款式: "style",
      材质: "material",
      等级: "grade",
      用途: "usage",
      工艺: "craft",
      品牌: "brand",
      产地: "origin",
      包装方式: "packaging",
      装箱数: "carton_qty",
    },
    tmall: {
      颜色: "color",
      尺码: "size",
      规格: "specification",
      型号: "model",
      款式: "style",
      材质: "material",
      适用场景: "scene",
      适用人群: "target_audience",
      适用性别: "gender",
      季节: "season",
      年份: "year",
      系列: "series",
    },
  };

  const cleanName = chinese.trim().replace(/[：:]/g, "");
  const map = platformMaps[platform] || platformMaps.taobao;

  if (map[cleanName]) return map[cleanName];

  for (const [key, value] of Object.entries(map)) {
    if (cleanName.includes(key)) return value;
  }

  const pinyinMap = {
    颜色: "color",
    尺寸: "size",
    规格: "spec",
    材质: "material",
    型号: "model",
    款式: "style",
    类型: "type",
    图案: "pattern",
    风格: "style_type",
  };

  for (const [key, value] of Object.entries(pinyinMap)) {
    if (cleanName.includes(key)) return value;
  }

  return cleanName
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, "_")
    .replace(/\s+/g, "_");
}

/**
 * 生成英文值（规格值）
 * @param {string} chinese - 中文规格值
 * @param {string} optionName - 规格名（用于上下文）
 * @param {string} platform - 平台：taobao/tmall/1688
 * @returns {string} 英文规格值
 */
function generateEnglishValue(chinese, optionName = "", platform = "taobao") {
  if (!chinese) return "";

  const cleanValue = chinese.trim();

  const valueMaps = {
    // 颜色
    黑色: "black",
    白色: "white",
    红色: "red",
    蓝色: "blue",
    绿色: "green",
    黄色: "yellow",
    灰色: "gray",
    粉色: "pink",
    紫色: "purple",
    橙色: "orange",
    棕色: "brown",
    金色: "gold",
    银色: "silver",
    米色: "beige",
    藏青色: "navy",
    卡其色: "khaki",
    咖啡色: "coffee",
    杏色: "apricot",
    驼色: "camel",
    多色: "multicolor",
    混色: "assorted",
    随机: "random",

    // 尺寸
    S: "s",
    M: "m",
    L: "l",
    XL: "xl",
    XXL: "xxl",
    XXXL: "xxxl",
    XS: "xs",
    小: "small",
    中: "medium",
    大: "large",
    加大: "extra_large",
    均码: "one_size",
    自由: "free_size",

    // 材质
    棉质: "cotton",
    棉: "cotton",
    涤纶: "polyester",
    聚酯纤维: "polyester",
    氨纶: "spandex",
    锦纶: "nylon",
    麻: "linen",
    亚麻: "linen",
    羊毛: "wool",
    羊绒: "cashmere",
    丝绸: "silk",
    真丝: "silk",
    雪纺: "chiffon",
    蕾丝: "lace",
    牛仔: "denim",
    皮革: "leather",
    PU: "pu_leather",
    人造革: "faux_leather",
    实木: "solid_wood",
    木质: "wood",
    金属: "metal",
    不锈钢: "stainless_steel",
    合金: "alloy",
    塑料: "plastic",
    树脂: "resin",
    陶瓷: "ceramic",
    玻璃: "glass",
    水晶: "crystal",
    亚克力: "acrylic",
    硅胶: "silicone",
    橡胶: "rubber",
    纸质: "paper",
    布艺: "fabric",
    帆布: "canvas",
    绒布: "velvet",
    牛津布: "oxford",
    尼龙: "nylon",

    // 风格
    中式: "chinese_style",
    新中式: "new_chinese",
    欧式: "european",
    美式: "american",
    现代: "modern",
    简约: "minimalist",
    北欧: "nordic",
    日式: "japanese",
    韩式: "korean",
    复古: "vintage",
    古典: "classical",
    田园: "pastoral",
    工业风: "industrial",
    地中海: "mediterranean",
    东南亚: "southeast_asian",
    轻奢: "light_luxury",

    // 性别
    男: "male",
    女: "female",
    中性: "unisex",
    情侣: "couple",
    儿童: "kids",
    婴儿: "baby",
    成人: "adult",

    // 季节
    春季: "spring",
    夏季: "summer",
    秋季: "autumn",
    冬季: "winter",
    四季: "all_seasons",
    春秋: "spring_autumn",

    // 其他
    有: "yes",
    无: "no",
    是: "yes",
    否: "no",
    带: "with",
    不带: "without",
    含: "included",
    不含: "excluded",
    高: "high",
    中: "medium",
    低: "low",
    厚: "thick",
    薄: "thin",
    长: "long",
    短: "short",
    宽: "wide",
    窄: "narrow",
    深: "deep",
    浅: "light",
    亮: "bright",
    暗: "dark",
    透明: "transparent",
    半透明: "translucent",
    不透明: "opaque",
    定制: "custom",
    成品: "ready_made",
    新款: "new",
    经典: "classic",
    基础: "basic",
    升级: "upgraded",
    豪华: "deluxe",
    标准: "standard",
    经济: "economy",
    专业: "professional",
    家用: "household",
    商用: "commercial",
    户外: "outdoor",
    室内: "indoor",
    便携: "portable",
    折叠: "foldable",
    可调节: "adjustable",
    固定: "fixed",
    手动: "manual",
    自动: "automatic",
    电动: "electric",
    智能: "smart",
    无线: "wireless",
    有线: "wired",
    蓝牙: "bluetooth",
    USB: "usb",
    "Type-C": "type_c",
  };

  if (valueMaps[cleanValue]) return valueMaps[cleanValue];

  for (const [key, value] of Object.entries(valueMaps)) {
    if (cleanValue.includes(key)) return value;
  }

  // 数字 + 单位
  const unitMap = {
    cm: "cm",
    厘米: "cm",
    mm: "mm",
    毫米: "mm",
    m: "m",
    米: "m",
    kg: "kg",
    千克: "kg",
    公斤: "kg",
    g: "g",
    克: "g",
    L: "l",
    升: "l",
    ml: "ml",
    毫升: "ml",
    "cm³": "cm3",
    立方米: "m3",
    "㎡": "m2",
    平方米: "m2",
    寸: "inch",
    英寸: "inch",
    尺: "chi",
    码: "yard",
    元: "yuan",
    "¥": "cny",
    $: "usd",
    "€": "eur",
  };

  const numUnitMatch = cleanValue.match(
    /^(\d+(?:\.\d+)?)\s*(cm|厘米 |mm|毫米|m|米|kg|千克 | 公斤|g|克|L|升|ml|毫升 | 寸 | 英寸 | 尺 | 码 | 元|¥|\$|€)?$/i,
  );
  if (numUnitMatch) {
    const num = numUnitMatch[1];
    const unit = numUnitMatch[2] || "";
    const unitEn = unitMap[unit] || unit.toLowerCase();
    return `${num}${unitEn}`;
  }

  return (
    cleanValue
      .toLowerCase()
      .replace(/[（(][^）)]*[）)]/g, "")
      .replace(/[^a-z0-9\s\u4e00-\u9fa5]/gi, "")
      .replace(/\s+/g, "_")
      .trim() || "default"
  );
}

// 导出函数
if (typeof window !== "undefined") {
  window.generateEnglishName = generateEnglishName;
  window.generateEnglishValue = generateEnglishValue;
}
