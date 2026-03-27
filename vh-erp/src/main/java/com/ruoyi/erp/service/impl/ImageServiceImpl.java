package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.ImageMapper;
import com.ruoyi.erp.model.domain.Image;
import com.ruoyi.erp.model.dto.image.ImageQuery;
import com.ruoyi.erp.model.vo.image.ImageVo;
import com.ruoyi.erp.service.IImageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * erp图片Service业务层处理
 *
 * @author lwj
 * @date 2026-03-24
 */
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image> implements IImageService
{

    @Resource
    private ImageMapper imageMapper;

    //region mybatis代码
    /**
     * 查询erp图片
     *
     * @param imageId erp图片主键
     * @return erp图片
     */
    @Override
    public Image selectImageByImageId(Long imageId)
    {
        return imageMapper.selectImageByImageId(imageId);
    }

    /**
     * 查询erp图片列表
     *
     * @param image erp图片
     * @return erp图片
     */
    @Override
    public List<Image> selectImageList(Image image)
    {
        return imageMapper.selectImageList(image);
    }

    /**
     * 新增erp图片
     *
     * @param image erp图片
     * @return 结果
     */
    @Override
    public int insertImage(Image image)
    {
        image.setCreateTime(DateUtils.getNowDate());
        return imageMapper.insertImage(image);
    }

    /**
     * 修改erp图片
     *
     * @param image erp图片
     * @return 结果
     */
    @Override
    public int updateImage(Image image)
    {
        image.setUpdateTime(DateUtils.getNowDate());
        return imageMapper.updateImage(image);
    }

    /**
     * 批量删除erp图片
     *
     * @param imageIds 需要删除的erp图片主键
     * @return 结果
     */
    @Override
    public int deleteImageByImageIds(Long[] imageIds)
    {
        return imageMapper.deleteImageByImageIds(imageIds);
    }

    /**
     * 删除erp图片信息
     *
     * @param imageId erp图片主键
     * @return 结果
     */
    @Override
    public int deleteImageByImageId(Long imageId)
    {
        return imageMapper.deleteImageByImageId(imageId);
    }
    //endregion
    @Override
    public QueryWrapper<Image> getQueryWrapper(ImageQuery imageQuery){
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = imageQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        Long productId = imageQuery.getProductId();
        queryWrapper.eq( StringUtils.isNotNull(productId),"product_id",productId);

        String filename = imageQuery.getFilename();
        queryWrapper.like(StringUtils.isNotEmpty(filename) ,"filename",filename);

        String alt = imageQuery.getAlt();
        queryWrapper.like(StringUtils.isNotEmpty(alt) ,"alt",alt);

        Long position = imageQuery.getPosition();
        queryWrapper.eq( StringUtils.isNotNull(position),"position",position);

        return queryWrapper;
    }

    @Override
    public List<ImageVo> convertVoList(List<Image> imageList) {
        if (StringUtils.isEmpty(imageList)) {
            return Collections.emptyList();
        }
        return imageList.stream().map(ImageVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp图片数据
     *
     * @param imageList erp图片数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importImageData(List<Image> imageList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(imageList))
        {
            throw new ServiceException("导入erp图片数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Image image : imageList)
        {
            try
            {
                // 验证是否存在这个erp图片
                Long imageId = image.getImageId();
                Image imageExist = null;
                if (StringUtils.isNotNull(imageId))
                {
                    imageExist = imageMapper.selectImageByImageId(imageId);
                }
                if (StringUtils.isNull(imageExist))
                {
                    image.setCreateTime(DateUtils.getNowDate());
                    imageMapper.insertImage(image);
                    successNum++;
                    String imageIdStr = StringUtils.isNotNull(imageId) ? imageId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp图片 " + imageIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    image.setUpdateTime(DateUtils.getNowDate());
                    imageMapper.updateImage(image);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp图片 " + imageId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String imageIdStr = StringUtils.isNotNull(imageId) ? imageId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp图片 " + imageIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long imageId = image.getImageId();
                String imageIdStr = StringUtils.isNotNull(imageId) ? imageId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp图片 " + imageIdStr + " 导入失败：";
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
