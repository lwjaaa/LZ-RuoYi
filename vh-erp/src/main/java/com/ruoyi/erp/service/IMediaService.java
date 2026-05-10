package com.ruoyi.erp.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.media.MediaQuery;
import com.ruoyi.erp.model.vo.media.MediaRenameVo;
import com.ruoyi.erp.model.vo.media.MediaVo;

import java.util.List;
/**
 * erp媒体Service接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface IMediaService extends IService<Media>
{
    //region mybatis代码
    /**
     * 查询erp媒体
     * 
     * @param mediaId erp媒体主键
     * @return erp媒体
     */
    public Media selectMediaByMediaId(Long mediaId);

    /**
     * 查询erp媒体列表
     * 
     * @param media erp媒体
     * @return erp媒体集合
     */
    public List<Media> selectMediaList(Media media);

    /**
     * 新增erp媒体
     * 
     * @param media erp媒体
     * @return 结果
     */
    public int insertMedia(Media media);

    /**
     * 修改erp媒体
     * 
     * @param media erp媒体
     * @return 结果
     */
    public int updateMedia(Media media);

    /**
     * 批量删除erp媒体
     * 
     * @param mediaIds 需要删除的erp媒体主键集合
     * @return 结果
     */
    public int deleteMediaByMediaIds(Long[] mediaIds);

    /**
     * 删除erp媒体信息
     * 
     * @param mediaId erp媒体主键
     * @return 结果
     */
    public int deleteMediaByMediaId(Long mediaId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param mediaQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<Media> getQueryWrapper(MediaQuery mediaQuery);

    /**
     * 转换vo
     *
     * @param mediaList Media集合
     * @return MediaVO集合
     */
    List<MediaVo> convertVoList(List<Media> mediaList);

    /**
     * 导入erp媒体数据
     *
     * @param mediaList erp媒体数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importMediaData(List<Media> mediaList, Boolean isUpdateSupport, String operName);

    /**
     * 扫描服务器指定路径返回媒体列表
     *
     * @return 媒体VO列表
     */
    List<Media> scanMediaToProduct(Long productId);

    void updateProductMedia(Product product);

    /**
     * 商品关键词变化后同步媒体目录、文件名和访问地址
     *
     * @param product     商品信息，使用 product.getKeyWord() 作为新关键词
     * @param oldKeyword  旧关键词
     * @param variantList 已按新关键词更新 SKU 的变体列表
     */
    void syncProductMediaKeyword(Product product, String oldKeyword, List<ProductVariant> variantList);

    void deleteMediaFilesFromDisk(List<Media> medias);

    /**
     * 执行媒体文件重命名
     *
     * @param toRenameList 需要重命名的媒体文件列表
     */
    void doRenameMediaFiles(List<MediaRenameVo> toRenameList);

    List<Media> listByProductId(Long productId);
}
