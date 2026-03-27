package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ProductTagRelMapper;
import com.ruoyi.erp.model.domain.ProductTagRel;
import com.ruoyi.erp.model.dto.productTagRel.ProductTagRelQuery;
import com.ruoyi.erp.model.vo.productTagRel.ProductTagRelVo;
import com.ruoyi.erp.service.IProductTagRelService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * erp商品与标签关联Service业务层处理
 *
 * @author lwj
 * @date 2026-03-24
 */
@Service
public class ProductTagRelServiceImpl extends ServiceImpl<ProductTagRelMapper, ProductTagRel> implements IProductTagRelService
{

    @Resource
    private ProductTagRelMapper productTagRelMapper;

    //region mybatis代码
    /**
     * 查询erp商品与标签关联
     *
     * @param relId erp商品与标签关联主键
     * @return erp商品与标签关联
     */
    @Override
    public ProductTagRel selectProductTagRelByRelId(Long relId)
    {
        return productTagRelMapper.selectProductTagRelByRelId(relId);
    }

    /**
     * 查询erp商品与标签关联列表
     *
     * @param productTagRel erp商品与标签关联
     * @return erp商品与标签关联
     */
    @Override
    public List<ProductTagRel> selectProductTagRelList(ProductTagRel productTagRel)
    {
        return productTagRelMapper.selectProductTagRelList(productTagRel);
    }

    /**
     * 新增erp商品与标签关联
     *
     * @param productTagRel erp商品与标签关联
     * @return 结果
     */
    @Override
    public int insertProductTagRel(ProductTagRel productTagRel)
    {
        productTagRel.setCreateTime(DateUtils.getNowDate());
        return productTagRelMapper.insertProductTagRel(productTagRel);
    }

    /**
     * 修改erp商品与标签关联
     *
     * @param productTagRel erp商品与标签关联
     * @return 结果
     */
    @Override
    public int updateProductTagRel(ProductTagRel productTagRel)
    {
        return productTagRelMapper.updateProductTagRel(productTagRel);
    }

    /**
     * 批量删除erp商品与标签关联
     *
     * @param relIds 需要删除的erp商品与标签关联主键
     * @return 结果
     */
    @Override
    public int deleteProductTagRelByRelIds(Long[] relIds)
    {
        return productTagRelMapper.deleteProductTagRelByRelIds(relIds);
    }

    /**
     * 删除erp商品与标签关联信息
     *
     * @param relId erp商品与标签关联主键
     * @return 结果
     */
    @Override
    public int deleteProductTagRelByRelId(Long relId)
    {
        return productTagRelMapper.deleteProductTagRelByRelId(relId);
    }
    //endregion
    @Override
    public QueryWrapper<ProductTagRel> getQueryWrapper(ProductTagRelQuery productTagRelQuery){
        QueryWrapper<ProductTagRel> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = productTagRelQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        Long productId = productTagRelQuery.getProductId();
        queryWrapper.eq( StringUtils.isNotNull(productId),"product_id",productId);

        Long tagId = productTagRelQuery.getTagId();
        queryWrapper.eq( StringUtils.isNotNull(tagId),"tag_id",tagId);

        return queryWrapper;
    }

    @Override
    public List<ProductTagRelVo> convertVoList(List<ProductTagRel> productTagRelList) {
        if (StringUtils.isEmpty(productTagRelList)) {
            return Collections.emptyList();
        }
        return productTagRelList.stream().map(ProductTagRelVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp商品与标签关联数据
     *
     * @param productTagRelList erp商品与标签关联数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importProductTagRelData(List<ProductTagRel> productTagRelList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(productTagRelList))
        {
            throw new ServiceException("导入erp商品与标签关联数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (ProductTagRel productTagRel : productTagRelList)
        {
            try
            {
                // 验证是否存在这个erp商品与标签关联
                Long relId = productTagRel.getRelId();
                ProductTagRel productTagRelExist = null;
                if (StringUtils.isNotNull(relId))
                {
                    productTagRelExist = productTagRelMapper.selectProductTagRelByRelId(relId);
                }
                if (StringUtils.isNull(productTagRelExist))
                {
                    productTagRel.setCreateTime(DateUtils.getNowDate());
                    productTagRelMapper.insertProductTagRel(productTagRel);
                    successNum++;
                    String relIdStr = StringUtils.isNotNull(relId) ? relId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp商品与标签关联 " + relIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    productTagRelMapper.updateProductTagRel(productTagRel);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp商品与标签关联 " + relId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String relIdStr = StringUtils.isNotNull(relId) ? relId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp商品与标签关联 " + relIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long relId = productTagRel.getRelId();
                String relIdStr = StringUtils.isNotNull(relId) ? relId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp商品与标签关联 " + relIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
