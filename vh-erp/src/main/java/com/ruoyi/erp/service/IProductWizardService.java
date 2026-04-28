package com.ruoyi.erp.service;

import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.product.ProductSaveByExtentionEdit;
import com.ruoyi.erp.model.dto.shipping.ShippingFeeQuery;
import com.ruoyi.erp.model.vo.shipping.ShippingFeeVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * erp商品Service接口
 *
 * @author lwj
 * @date 2026-03-26
 */
public interface IProductWizardService {

    /**
     * 保存商品及相关数据（选品向导）
     *
     * @param product 商品对象
     * @return 结果
     */
    Long saveProductWithWizard(Product product,int step);

    /**
     * 运费查询
     *
     * @param shippingFeeQuery 运费查询参数
     * @return 运费信息VO对象列表
     * @author lwj
     **/
    List<ShippingFeeVo> calculateShipping(ShippingFeeQuery shippingFeeQuery);

    /**
     * 通过浏览器插件保存商品信息（包含媒体文件异步下载）
     *
     * @param productSaveByExtentionEdit 插件导入的商品数据
     * @return 商品ID
     */
    Long saveProductFromExtension(ProductSaveByExtentionEdit productSaveByExtentionEdit);

    /**
     * 通过JSON文件批量保存商品信息
     *
     * @param file JSON文件
     * @return 成功保存的商品数量
     */
    Integer saveProductsFromJsonFile(MultipartFile file);

    /**
     * 通用商品保存方法（不做参数校验）
     * <p>
     * 该方法封装了商品保存的核心逻辑，包括：
     * 1. SPU生成和管理 - 如果SPU为空，根据标签自动生成；如果不为空，更新标签字典流水号
     * 2. 媒体文件处理 - 主图、规格图绑定和文件名统一管理
     * 3. 规格数据处理 - 选项值序列化、规格图校验
     * 4. SKU生成 - 格式：SPU-序号（如 ABC0001-001）
     * 5. 数据持久化 - 商品主表、变体、标签关联的保存/更新
     * </p>
     *
     * 
     * <p><b>注意事项：</b></p>
     * <ul>
     *   <li>此方法不做参数校验，调用方需确保数据完整性</li>
     *   <li>新增时 productId 应为 null，编辑时 productId 必须有值</li>
     *   <li>编辑时如果需要更新媒体，必须设置 mediaList</li>
     *   <li>变体的 optionValueList 会被自动序列化为 optionValues 字段</li>
     *   <li>SKU 会自动生成，格式为：SPU-三位序号（如 ABC0001-001）</li>
     *   <li>方法包含事务，任何步骤失败都会回滚</li>
     * </ul>
     *
     * @param product 商品对象（包含完整的商品信息、变体列表、媒体列表、标签ID列表）
     * @return 商品ID
     * @author lwj
     * @date 2026-04-23
     */
    Long saveProductCommon(Product product, Integer wizardStep);
}
