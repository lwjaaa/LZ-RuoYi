package com.ruoyi.erp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.erp.model.domain.Image;

import java.util.List;

/**
 * erp图片Mapper接口
 * 
 * @author lwj
 * @date 2026-03-24
 */
public interface ImageMapper extends BaseMapper<Image>
{
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
     * 删除erp图片
     * 
     * @param imageId erp图片主键
     * @return 结果
     */
    public int deleteImageByImageId(Long imageId);

    /**
     * 批量删除erp图片
     * 
     * @param imageIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteImageByImageIds(Long[] imageIds);
}
