// 增强的工具函数 - 针对淘宝、天猫、1688 平台优化

// ==================== 统一的规格名映射表（所有平台共用）====================
const OPTION_NAME_MAP = {
  // 颜色相关
  颜色: "COLOR",
  颜色分类: "COLOR",
  配色: "COLOR_SCHEME",
  花色: "PATTERN",
  
  // 尺寸相关
  尺寸: "SIZE",
  尺码: "SIZE",
  大小: "SIZE",
  
  // 规格型号
  规格: "SPECIFICATION",
  型号: "MODEL",
  款号: "STYLE_NO",
  
  // 款式风格
  款式: "STYLE",
  类型: "TYPE",
  风格: "STYLE_TYPE",
  
  // 材质面料
  材质: "MATERIAL",
  面料: "FABRIC",
  成分: "COMPOSITION",
  质地: "TEXTURE",
  
  // 形状图案
  形状: "SHAPE",
  图案: "PATTERN_TYPE",
  花纹: "DESIGN",
  
  // 尺寸属性
  厚度: "THICKNESS",
  长度: "LENGTH",
  宽度: "WIDTH",
  高度: "HEIGHT",
  深度: "DEPTH",
  直径: "DIAMETER",
  
  // 重量容量
  重量: "WEIGHT",
  容量: "CAPACITY",
  体积: "VOLUME",
  
  // 包装套装
  包装: "PACKAGE",
  套装: "SET",
  套餐: "COMBO",
  组合: "COMBINATION",
  
  // 版本配置
  版本: "VERSION",
  配置: "CONFIGURATION",
  等级: "GRADE",
  
  // 电子数码
  网络: "NETWORK",
  存储: "STORAGE",
  内存: "MEMORY",
  处理器: "PROCESSOR",
  屏幕: "SCREEN",
  
  // 适用对象
  适用场景: "SCENE",
  适用人群: "TARGET_AUDIENCE",
  适用性别: "GENDER",
  适用年龄: "AGE_GROUP",
  
  // 时间季节
  季节: "SEASON",
  年份: "YEAR",
  系列: "SERIES",
  
  // 品牌产地
  品牌: "BRAND",
  产地: "ORIGIN",
  
  // 工艺用途
  工艺: "CRAFT",
  用途: "USAGE",
  功能: "FUNCTION",
  
  // 其他
  包装方式: "PACKAGING",
  装箱数: "CARTON_QTY",
  颜色备注: "COLOR_NOTE",
};

// ==================== 统一的规格值映射表（所有平台共用）====================
const OPTION_VALUE_MAP = {
  // 颜色
  黑色: "BLACK",
  白色: "WHITE",
  红色: "RED",
  蓝色: "BLUE",
  绿色: "GREEN",
  黄色: "YELLOW",
  灰色: "GRAY",
  粉色: "PINK",
  紫色: "PURPLE",
  橙色: "ORANGE",
  棕色: "BROWN",
  金色: "GOLD",
  银色: "SILVER",
  米色: "BEIGE",
  藏青色: "NAVY",
  卡其色: "KHAKI",
  咖啡色: "COFFEE",
  杏色: "APRICOT",
  驼色: "CAMEL",
  多色: "MULTICOLOR",
  混色: "ASSORTED",
  随机: "RANDOM",
  彩色: "COLORFUL",
  
  // 尺寸
  S: "S",
  M: "M",
  L: "L",
  XL: "XL",
  XXL: "XXL",
  XXXL: "XXXL",
  XS: "XS",
  小: "SMALL",
  中: "MEDIUM",
  大: "LARGE",
  加大: "EXTRA_LARGE",
  加加大: "XXL",
  均码: "ONE_SIZE",
  自由: "FREE_SIZE",
  
  // 材质
  棉质: "COTTON",
  棉: "COTTON",
  涤纶: "POLYESTER",
  聚酯纤维: "POLYESTER",
  氨纶: "SPANDEX",
  锦纶: "NYLON",
  麻: "LINEN",
  亚麻: "LINEN",
  羊毛: "WOOL",
  羊绒: "CASHMERE",
  丝绸: "SILK",
  真丝: "SILK",
  雪纺: "CHIFFON",
  蕾丝: "LACE",
  牛仔: "DENIM",
  皮革: "LEATHER",
  PU: "PU_LEATHER",
  人造革: "FAUX_LEATHER",
  实木: "SOLID_WOOD",
  木质: "WOOD",
  金属: "METAL",
  不锈钢: "STAINLESS_STEEL",
  合金: "ALLOY",
  塑料: "PLASTIC",
  树脂: "RESIN",
  陶瓷: "CERAMIC",
  玻璃: "GLASS",
  水晶: "CRYSTAL",
  亚克力: "ACRYLIC",
  硅胶: "SILICONE",
  橡胶: "RUBBER",
  纸质: "PAPER",
  布艺: "FABRIC",
  帆布: "CANVAS",
  绒布: "VELVET",
  牛津布: "OXFORD",
  尼龙: "NYLON",
  
  // 风格
  中式: "CHINESE_STYLE",
  新中式: "NEW_CHINESE",
  欧式: "EUROPEAN",
  美式: "AMERICAN",
  现代: "MODERN",
  简约: "MINIMALIST",
  北欧: "NORDIC",
  日式: "JAPANESE",
  韩式: "KOREAN",
  复古: "VINTAGE",
  古典: "CLASSICAL",
  田园: "PASTORAL",
  工业风: "INDUSTRIAL",
  地中海: "MEDITERRANEAN",
  东南亚: "SOUTHEAST_ASIAN",
  轻奢: "LIGHT_LUXURY",
  
  // 性别
  男: "MALE",
  女: "FEMALE",
  中性: "UNISEX",
  情侣: "COUPLE",
  儿童: "KIDS",
  婴儿: "BABY",
  成人: "ADULT",
  
  // 季节
  春季: "SPRING",
  夏季: "SUMMER",
  秋季: "AUTUMN",
  冬季: "WINTER",
  四季: "ALL_SEASONS",
  春秋: "SPRING_AUTUMN",
  
  // 布尔值
  有: "YES",
  无: "NO",
  是: "YES",
  否: "NO",
  带: "WITH",
  不带: "WITHOUT",
  含: "INCLUDED",
  不含: "EXCLUDED",
  
  // 程度
  高: "HIGH",
  中: "MEDIUM",
  低: "LOW",
  厚: "THICK",
  薄: "THIN",
  长: "LONG",
  短: "SHORT",
  宽: "WIDE",
  窄: "NARROW",
  深: "DEEP",
  浅: "LIGHT",
  亮: "BRIGHT",
  暗: "DARK",
  
  // 透明度
  透明: "TRANSPARENT",
  半透明: "TRANSLUCENT",
  不透明: "OPAQUE",
  
  // 产品状态
  定制: "CUSTOM",
  成品: "READY_MADE",
  新款: "NEW",
  经典: "CLASSIC",
  基础: "BASIC",
  升级: "UPGRADED",
  豪华: "DELUXE",
  标准: "STANDARD",
  经济: "ECONOMY",
  专业: "PROFESSIONAL",
  
  // 使用场景
  家用: "HOUSEHOLD",
  商用: "COMMERCIAL",
  户外: "OUTDOOR",
  室内: "INDOOR",
  
  // 功能特性
  便携: "PORTABLE",
  折叠: "FOLDABLE",
  可调节: "ADJUSTABLE",
  固定: "FIXED",
  手动: "MANUAL",
  自动: "AUTOMATIC",
  电动: "ELECTRIC",
  智能: "SMART",
  无线: "WIRELESS",
  有线: "WIRED",
  蓝牙: "BLUETOOTH",
  USB: "USB",
  "Type-C": "TYPE_C",
};

/**
 * 生成英文名称（规格名）
 * @param {string} chinese - 中文规格名
 * @returns {string} 英文规格名（大写）
 */
function generateEnglishName(chinese) {
  if (!chinese) return "";

  const cleanName = chinese.trim().replace(/[：:]/g, "");

  // 1. 精确匹配
  if (OPTION_NAME_MAP[cleanName]) return OPTION_NAME_MAP[cleanName];

  // 2. 包含匹配（提高匹配率）
  for (const [key, value] of Object.entries(OPTION_NAME_MAP)) {
    if (cleanName.includes(key)) return value;
  }

  // 3. 反向包含匹配（如"商品规格"包含"规格"）
  for (const [key, value] of Object.entries(OPTION_NAME_MAP)) {
    if (key.includes(cleanName)) return value;
  }

  // 4. 默认处理：转大写
  return cleanName
    .toUpperCase()
    .replace(/[^A-Z0-9\s]/g, "_")
    .replace(/\s+/g, "_");
}

/**
 * 生成英文值（规格值）
 * @param {string} chinese - 中文规格值
 * @returns {string} 英文规格值（大写）
 */
function generateEnglishValue(chinese) {
  if (!chinese) return "";

  const cleanValue = chinese.trim();

  // 1. 精确匹配
  if (OPTION_VALUE_MAP[cleanValue]) return OPTION_VALUE_MAP[cleanValue];

  // 2. 包含匹配（提高匹配率）
  for (const [key, value] of Object.entries(OPTION_VALUE_MAP)) {
    if (cleanValue.includes(key)) return value;
  }

  // 3. 反向包含匹配
  for (const [key, value] of Object.entries(OPTION_VALUE_MAP)) {
    if (key.includes(cleanValue)) return value;
  }

  // 4. 数字 + 单位（大写）
  const unitMap = {
    cm: "CM",
    厘米: "CM",
    mm: "MM",
    毫米: "MM",
    m: "M",
    米: "M",
    kg: "KG",
    千克: "KG",
    公斤: "KG",
    g: "G",
    克: "G",
    L: "L",
    升: "L",
    ml: "ML",
    毫升: "ML",
    "cm³": "CM3",
    立方米: "M3",
    "㎡": "M2",
    平方米: "M2",
    寸: "INCH",
    英寸: "INCH",
    尺: "CHI",
    码: "YARD",
    元: "YUAN",
    "¥": "CNY",
    $: "USD",
    "€": "EUR",
  };

  const numUnitMatch = cleanValue.match(
    /^(\d+(?:\.\d+)?)\s*(cm|厘米 |mm|毫米|m|米|kg|千克 | 公斤|g|克|L|升|ml|毫升 | 寸 | 英寸 | 尺 | 码 | 元|¥|\$|€)?$/i,
  );
  if (numUnitMatch) {
    let num = parseFloat(numUnitMatch[1]);
    const unit = numUnitMatch[2] || "";
    const unitLower = unit.toLowerCase();
    
    // 单位转换：厘米转英寸
    if (unitLower === "cm" || unitLower === "厘米") {
      const inches = (num / 2.54).toFixed(2);
      return `${inches}INCH`;
    }
    
    const unitEn = unitMap[unit] || unit.toUpperCase();
    return `${num}${unitEn}`;
  }

  // 5. 默认处理：转大写
  return (
    cleanValue
      .toUpperCase()
      .replace(/[（(][^）)]*[）)]/g, "")
      .replace(/[^A-Z0-9\s\u4e00-\u9fa5]/gi, "")
      .replace(/\s+/g, "_")
      .trim() || "DEFAULT"
  );
}

// 导出函数
if (typeof window !== "undefined") {
  window.generateEnglishName = generateEnglishName;
  window.generateEnglishValue = generateEnglishValue;
}
