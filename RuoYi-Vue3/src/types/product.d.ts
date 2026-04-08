// 产品类型
export interface Product {
  /** 本地主键 */
  productId: number;

  /** Shopify平台商品ID (唯一映射) */
  shopifyProductId?: string;

  /** 商品标题 */
  productTitle: string;

  /** SPU */
  spu: string;

  /** 商品类别ID (Category) */
  category?: number;

  /** 商品类型 */
  productType?: string;

  /** 来源URL */
  sourceUrl?: string;

  /** 采购链接 */
  purchaseUrl?: string;

  /** 商品选项json */
  optionJson?: string;

  /** 商品选项列表 */
  optionList?: ProductOption[];

  /** 发布状态 */
  status?: string;

  /** 商品详情描述 */
  bodyHtml?: string;

  /** 主图ID，仅用户erp后台展示 */
  mainMediaId?: number;

  /** 同步状态 */
  syncStatus?: string;

  /** 最后一次同步错误信息或结果 */
  syncMessage?: string;

  /** 最后同步时间 */
  lastSyncTime?: Date;

  /** 乐观锁版本号 */
  version?: number;

  /** 描述 */
  description?: string;

  /** 大小 */
  size?: string;

  /** 材质 */
  material?: string;

  /** 备注 */
  note?: string;

  /** 包含的包材 */
  packageInclude?: string;

  /** 创建者 */
  createBy?: string;

  /** 创建时间 */
  createTime?: Date;

  /** 更新者 */
  updateBy?: string;

  /** 更新时间 */
  updateTime?: Date;

  /** 备注 */
  remark?: string;

  /** 删除标志 (0代表存在 2代表删除) */
  delFlag?: string;

  /** erp商品变体信息,非数据库字段 */
  productVariantList?: ProductVariant[];

  /** 商品标签ID列表,非数据库字段 */
  tagIds?: number[];

  /** 请求参数 */
  params?: Record<string, any>;
}

// 商品选项类型
export interface ProductOption {
  name: string;
  values: ProductOptionValue[];
}

// 商品选项值类型
export interface ProductOptionValue {
  /** 选项值 */
  value: string;
}

// 产品变体类型
export interface ProductVariant {
  /** 本地主键 */
  variantId: number;

  /** 关联商品主表ID */
  productId: number;

  /** Shopify平台变体ID */
  shopifyVariantId?: string;

  /** SKU */
  sku: string;

  /** 销售价格(美元*100) */
  price: number;

  /** 原价/对比价(美元*100) */
  compareAtPrice?: number;

  /** 采购价（分） */
  purchasePrice?: number;

  /** 采购链接 */
  purchaseUrl?: string;

  /** 变体对应的选项 */
  optionValues: string;

  /** 变体选项列表 */
  optionValueList: ProductVariantOption[];

  /** 关联的图片ID (若有) */
  mediaId?: number;

  /** 排序位置 列表中的第一个位置是 1 */
  position: number;

  /** 包装宽度 */
  pkWidth?: number;

  /** 包装高度 */
  pkHeight?: number;

  /** 包装长度 */
  pkLength?: number;

  /** 材积重 */
  materialWeight?: number;

  /** 常规包装重量 */
  pkWeight?: number;

  /** 运费 */
  freight?: number;

  /** 运费是否来自实际发货数据(0:否, 1:是) */
  isActualShipment?: string;

  /** 商品成本价（分） */
  unitCostPrice?: number;

  /** 创建者 */
  createBy?: string;

  /** 创建时间 */
  createTime?: Date;

  /** 更新者 */
  updateBy?: string;

  /** 更新时间 */
  updateTime?: Date;

  /** 备注 */
  remark?: string;

  /** 删除标志 (0代表存在 2代表删除) */
  delFlag?: string;

  /** 请求参数 */
  params?: Record<string, any>;
}

// 产品变体选项类型
export interface ProductVariantOption {
  /** 商品选项ID */
  optionId: string;

  /** 商品选项名称 */
  optionName: string;

  /** 商品选项值 */
  name: string;

  /** 采购商品选项名称 */
  purchaseOptionName?: string;

  /** 采购商品选项值 */
  purchaseName?: string;
}

// 标签菜单类型
export interface TagDictMenu {
  /** 标签主键 */
  tagId: number;

  /** 标签名称 */
  tagName: string;

  /** 标签编码 */
  tagCode?: string;

  /** 标签类型 */
  tagType?: string;

  /** 排序 */
  sortOrder?: number;

  /** 父级ID (0表示顶级菜单) */
  parentId?: number;

  /** 所有父级ID路径 (如: 0,10,25)，方便快速查询子树 */
  ancestors?: string;

  /** 菜单层级 */
  menuLevel?: number;

  /** SPU 前缀 */
  spuPrefix?: string;

  /** 当前最大流水号 */
  currentMaxSeq?: number;

  /** 子节点列表 */
  children?: TagDictMenu[];
}
