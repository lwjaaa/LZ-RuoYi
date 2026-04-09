package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.dto.media.MediaQuery;
import com.ruoyi.erp.model.vo.media.MediaVo;
import com.ruoyi.erp.service.IMediaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * erp媒体Service业务层处理
 *
 * @author lwj
 * @date 2026-03-26
 */
@Slf4j
@Service
public class MediaServiceImpl extends ServiceImpl<MediaMapper, Media> implements IMediaService
{

    @Resource
    private MediaMapper mediaMapper;
    @Resource
    private ProductMapper productMapper;

    //region mybatis代码
    /**
     * 查询erp媒体
     *
     * @param mediaId erp媒体主键
     * @return erp媒体
     */
    @Override
    public Media selectMediaByMediaId(Long mediaId)
    {
        return mediaMapper.selectMediaByMediaId(mediaId);
    }

    /**
     * 查询erp媒体列表
     *
     * @param media erp媒体
     * @return erp媒体
     */
    @Override
    public List<Media> selectMediaList(Media media)
    {
        return mediaMapper.selectMediaList(media);
    }

    /**
     * 新增erp媒体
     *
     * @param media erp媒体
     * @return 结果
     */
    @Override
    public int insertMedia(Media media)
    {
        media.setCreateTime(DateUtils.getNowDate());
        return mediaMapper.insertMedia(media);
    }

    /**
     * 修改erp媒体
     *
     * @param media erp媒体
     * @return 结果
     */
    @Override
    public int updateMedia(Media media)
    {
        media.setUpdateTime(DateUtils.getNowDate());
        return mediaMapper.updateMedia(media);
    }

    /**
     * 批量删除erp媒体
     *
     * @param mediaIds 需要删除的erp媒体主键
     * @return 结果
     */
    @Override
    public int deleteMediaByMediaIds(Long[] mediaIds)
    {
        return mediaMapper.deleteMediaByMediaIds(mediaIds);
    }

    /**
     * 删除erp媒体信息
     *
     * @param mediaId erp媒体主键
     * @return 结果
     */
    @Override
    public int deleteMediaByMediaId(Long mediaId)
    {
        return mediaMapper.deleteMediaByMediaId(mediaId);
    }
    //endregion
    @Override
    public QueryWrapper<Media> getQueryWrapper(MediaQuery mediaQuery){
        QueryWrapper<Media> queryWrapper = new QueryWrapper<>();
        //如果不使用params可以删除
        Map<String, Object> params = mediaQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        Long productId = mediaQuery.getProductId();
        queryWrapper.eq( StringUtils.isNotNull(productId),"product_id",productId);

        String shopifyMediaId = mediaQuery.getShopifyMediaId();
        queryWrapper.eq(StringUtils.isNotEmpty(shopifyMediaId) ,"shopify_media_id",shopifyMediaId);

        String filename = mediaQuery.getFilename();
        queryWrapper.like(StringUtils.isNotEmpty(filename) ,"filename",filename);

        String alt = mediaQuery.getAlt();
        queryWrapper.like(StringUtils.isNotEmpty(alt) ,"alt",alt);

        return queryWrapper;
    }

    @Override
    public List<MediaVo> convertVoList(List<Media> mediaList) {
        if (StringUtils.isEmpty(mediaList)) {
            return Collections.emptyList();
        }
        return mediaList.stream().map(MediaVo::objToVo).collect(Collectors.toList());
    }

    /**
     * 导入erp媒体数据
     *
     * @param mediaList erp媒体数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importMediaData(List<Media> mediaList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isEmpty(mediaList))
        {
            throw new ServiceException("导入erp媒体数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Media media : mediaList)
        {
            try
            {
                // 验证是否存在这个erp媒体
                Long mediaId = media.getMediaId();
                Media mediaExist = null;
                if (StringUtils.isNotNull(mediaId))
                {
                    mediaExist = mediaMapper.selectMediaByMediaId(mediaId);
                }
                if (StringUtils.isNull(mediaExist))
                {
                    media.setCreateTime(DateUtils.getNowDate());
                    mediaMapper.insertMedia(media);
                    successNum++;
                    String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp媒体 " + mediaIdStr + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    media.setUpdateTime(DateUtils.getNowDate());
                    mediaMapper.updateMedia(media);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp媒体 " + mediaId.toString() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp媒体 " + mediaIdStr + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                Long mediaId = media.getMediaId();
                String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp媒体 " + mediaIdStr + " 导入失败：";
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Media> scanMediaToProduct(String dirPath, String productId) {
        log.info("开始扫描媒体目录");
        List<Media> mediaList = new ArrayList<>();

        // 如果未指定路径，使用默认上传目录
        String basePath = RuoYiConfig.getMediaPath() + dirPath;

        File dir = new File(basePath);

        // 检查目录是否存在
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ServiceException("目录不存在：" + basePath);
        }

        // 获取目录下所有文件
        // 获取目录下所有文件（包括图片和视频）
        File[] files = dir.listFiles((f, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".png") ||
                    lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".webp") ||
                    lowerName.endsWith(".bmp") ||
                    lowerName.endsWith(".mp4") ||
                    lowerName.endsWith(".avi") ||
                    lowerName.endsWith(".mov") ||
                    lowerName.endsWith(".wmv") ||
                    lowerName.endsWith(".flv") ||
                    lowerName.endsWith(".mkv");
        });

        if (files != null) {
            this.remove(new LambdaQueryWrapper<>(Media.class).eq(Media::getProductId, productId));
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                Media media = new Media();
                media.setProductId(Long.parseLong(productId));
                media.setFilename(file.getName());
                media.setNasMediaUrl(getFileUrl(file,dirPath));
                media.setPosition(i);

                // 根据文件扩展名判断媒体类型
                String mediaType = getMediaType(file.getName());
                media.setMediaContentType(mediaType);

                this.save(media);
                mediaList.add(media);
            }
            Product product = new Product();
            product.setProductId(Long.parseLong(productId));
            product.setImageSearchKeyword(dirPath);
            productMapper.updateById(product);
        }
        return mediaList;
    }

    /**
     * 根据文件名判断媒体类型
     */
    private String getMediaType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp") || lowerName.endsWith(".bmp")) {
            return "image";
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
                lowerName.endsWith(".mov") || lowerName.endsWith(".wmv") ||
                lowerName.endsWith(".flv") || lowerName.endsWith(".mkv")) {
            return "video";
        }
        return "unknown";
    }

    /**
     * 将文件路径转换为可访问的 URL
     */
    private String getFileUrl(File file,String dirPath) {
        String profile = RuoYiConfig.getProfile();
        String absolutePath = file.getAbsolutePath();

        // 将本地路径转换为资源访问路径
        if (absolutePath.startsWith(profile)) {
            return Constants.RESOURCE_PREFIX + absolutePath.substring(profile.length());
        }
        return Constants.RESOURCE_PREFIX + "/media/" +dirPath + "/"+ file.getName();
    }

    @Override
    public List<Media> listByProductId(Long productId) {
        return this.list(new LambdaQueryWrapper<>(Media.class).eq(Media::getProductId, productId));
    }

}
