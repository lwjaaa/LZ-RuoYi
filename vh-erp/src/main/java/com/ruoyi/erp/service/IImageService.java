package com.ruoyi.erp.service;

import java.util.List;
import com.ruoyi.erp.model.domain.Image;
import com.ruoyi.erp.model.vo.image.ImageVo;
import com.ruoyi.erp.model.dto.image.ImageQuery;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
/**
 * erp图片Service接口
 * 
 * @author lwj
 * @date 2026-03-24
 */
public interface IImageService extends IService<Image>
{
    //region mybatis代码
    /**
     * 查询erp图片
     * 
     * @param imageId erp图片主键
     * @return erp图片
     */
    public Image selectImageByImageId(Long imageId);

    /**
     * 查询erp图片列表
     * 
     * @param image erp图片
     * @return erp图片集合
     */
    public List<Image> selectImageList(Image image);

    /**
     * 新增erp图片
     * 
     * @param image erp图片
     * @return 结果
     */
    public int insertImage(Image image);

    /**
     * 修改erp图片
     * 
     * @param image erp图片
     * @return 结果
     */
    public int updateImage(Image image);

    /**
     * 批量删除erp图片
     * 
     * @param imageIds 需要删除的erp图片主键集合
     * @return 结果
     */
    public int deleteImageByImageIds(Long[] imageIds);

    /**
     * 删除erp图片信息
     * 
     * @param imageId erp图片主键
     * @return 结果
     */
    public int deleteImageByImageId(Long imageId);
    //endregion
    /**
     * 获取查询条件
     *
     * @param imageQuery 查询条件对象
     * @return 查询条件
     */
    QueryWrapper<Image> getQueryWrapper(ImageQuery imageQuery);

    /**
     * 转换vo
     *
     * @param imageList Image集合
     * @return ImageVO集合
     */
    List<ImageVo> convertVoList(List<Image> imageList);

    /**
     * 导入erp图片数据
     *
     * @param imageList erp图片数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    public String importImageData(List<Image> imageList, Boolean isUpdateSupport, String operName);
}
