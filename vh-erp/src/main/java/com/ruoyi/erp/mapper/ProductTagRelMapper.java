package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.ProductTagRel;

import java.util.List;

/**
 * erp商品与标签关联Mapper接口
 * 
 * @author lwj
 * @date 2026-03-24
 */
public interface ProductTagRelMapper extends BaseMapper<ProductTagRel>
{
    /**
     * 查询erp商品与标签关联
     * 
     * @param relId erp商品与标签关联主键
     * @return erp商品与标签关联
     */
    public ProductTagRel selectProductTagRelByRelId(Long relId);

    /**
     * 查询erp商品与标签关联列表
     * 
     * @param productTagRel erp商品与标签关联
     * @return erp商品与标签关联集合
     */
    public List<ProductTagRel> selectProductTagRelList(ProductTagRel productTagRel);

    /**
     * 新增erp商品与标签关联
     * 
     * @param productTagRel erp商品与标签关联
     * @return 结果
     */
    public int insertProductTagRel(ProductTagRel productTagRel);

    /**
     * 修改erp商品与标签关联
     * 
     * @param productTagRel erp商品与标签关联
     * @return 结果
     */
    public int updateProductTagRel(ProductTagRel productTagRel);

    /**
     * 删除erp商品与标签关联
     * 
     * @param relId erp商品与标签关联主键
     * @return 结果
     */
    public int deleteProductTagRelByRelId(Long relId);

    /**
     * 批量删除erp商品与标签关联
     * 
     * @param relIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductTagRelByRelIds(Long[] relIds);

    /**
     * 根据商品id查询标签code列表
     * @param productId
     * @return
     */
    List<String> selectTagCodeListByProductId(Long productId);
}
