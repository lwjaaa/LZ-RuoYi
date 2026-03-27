package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.model.domain.ProductTagRel;
import com.ruoyi.erp.model.vo.productTagRel.ProductTagRelVo;
import com.ruoyi.erp.model.dto.productTagRel.ProductTagRelQuery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
/**
 * erp商品与标签关联Service接口
 * 
 * @author lwj
 * @date 2026-03-24
 */
public interface IProductTagRelService extends IService<ProductTagRel>
{
    //region mybatis代码
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
     * 批量删除erp商品与标签关联
     * 
     * @param relIds 需要删除的erp商品与标签关联主键集合
     * @return 结果
     */
    public int deleteProductTagRelByRelIds(Long[] relIds);

    /**
     * 删除erp商品与标签关联信息
     * 
     * @param relId erp商品与标签关联主键
     * @return 结果
     */
    public int deleteProductTagRelByRelId(Long relId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param productTagRelQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<ProductTagRel> getQueryWrapper(ProductTagRelQuery productTagRelQuery);

    /**
     * 转换vo
     *
     * @param productTagRelList ProductTagRel集合
     * @return ProductTagRelVO集合
     */
    List<ProductTagRelVo> convertVoList(List<ProductTagRel> productTagRelList);

    /**
     * 导入erp商品与标签关联数据
     *
     * @param productTagRelList erp商品与标签关联数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importProductTagRelData(List<ProductTagRel> productTagRelList, Boolean isUpdateSupport, String operName);
}
