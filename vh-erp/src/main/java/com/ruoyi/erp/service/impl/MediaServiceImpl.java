package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.media.MediaQuery;
import com.ruoyi.erp.model.vo.media.MediaRenameVo;
import com.ruoyi.erp.model.vo.media.MediaVo;
import com.ruoyi.erp.model.vo.media.RenameOperationVo;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.utils.MediaFileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
public class MediaServiceImpl extends ServiceImpl<MediaMapper, Media> implements IMediaService {

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
    public Media selectMediaByMediaId(Long mediaId) {
        return mediaMapper.selectMediaByMediaId(mediaId);
    }

    /**
     * 查询erp媒体列表
     *
     * @param media erp媒体
     * @return erp媒体
     */
    @Override
    public List<Media> selectMediaList(Media media) {
        return mediaMapper.selectMediaList(media);
    }

    /**
     * 新增erp媒体
     *
     * @param media erp媒体
     * @return 结果
     */
    @Override
    public int insertMedia(Media media) {
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
    public int updateMedia(Media media) {
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
    public int deleteMediaByMediaIds(Long[] mediaIds) {
        return mediaMapper.deleteMediaByMediaIds(mediaIds);
    }

    /**
     * 删除erp媒体信息
     *
     * @param mediaId erp媒体主键
     * @return 结果
     */
    @Override
    public int deleteMediaByMediaId(Long mediaId) {
        return mediaMapper.deleteMediaByMediaId(mediaId);
    }

    //endregion
    @Override
    public QueryWrapper<Media> getQueryWrapper(MediaQuery mediaQuery) {
        QueryWrapper<Media> queryWrapper = new QueryWrapper<>();
        Map<String, Object> params = mediaQuery.getParams();
        if (StringUtils.isNull(params)) {
            params = new HashMap<>();
        }
        Long productId = mediaQuery.getProductId();
        queryWrapper.eq(StringUtils.isNotNull(productId), "product_id", productId);

        String shopifyMediaId = mediaQuery.getShopifyMediaId();
        queryWrapper.eq(StringUtils.isNotEmpty(shopifyMediaId), "shopify_media_id", shopifyMediaId);

        String filename = mediaQuery.getFilename();
        queryWrapper.like(StringUtils.isNotEmpty(filename), "filename", filename);

        String alt = mediaQuery.getAlt();
        queryWrapper.like(StringUtils.isNotEmpty(alt), "alt", alt);

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
     * @param mediaList       erp媒体数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importMediaData(List<Media> mediaList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isEmpty(mediaList)) {
            throw new ServiceException("导入erp媒体数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (Media media : mediaList) {
            try {
                Long mediaId = media.getMediaId();
                Media mediaExist = null;
                if (StringUtils.isNotNull(mediaId)) {
                    mediaExist = mediaMapper.selectMediaByMediaId(mediaId);
                }
                if (StringUtils.isNull(mediaExist)) {
                    media.setCreateTime(DateUtils.getNowDate());
                    mediaMapper.insertMedia(media);
                    successNum++;
                    String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "新记录";
                    successMsg.append("<br/>" + successNum + "、erp媒体 " + mediaIdStr + " 导入成功");
                } else if (isUpdateSupport) {
                    media.setUpdateTime(DateUtils.getNowDate());
                    mediaMapper.updateMedia(media);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、erp媒体 " + mediaId.toString() + " 更新成功");
                } else {
                    failureNum++;
                    String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "未知";
                    failureMsg.append("<br/>" + failureNum + "、erp媒体 " + mediaIdStr + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                Long mediaId = media.getMediaId();
                String mediaIdStr = StringUtils.isNotNull(mediaId) ? mediaId.toString() : "未知";
                String msg = "<br/>" + failureNum + "、erp媒体 " + mediaIdStr + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductMedia(Product product) {
        Long productId = product.getProductId();
        String spu = product.getSpu();
        List<Media> newMediaList = product.getMediaList();

        log.info("开始更新商品媒体，商品ID: {}, SPU: {}, 新媒体数量: {}",
                productId, spu, newMediaList != null ? newMediaList.size() : 0);

        // ==================== 1. 获取数据库中现有的媒体列表 ====================
        List<Media> existingMediaList = this.listByProductId(productId);
        if (existingMediaList == null || existingMediaList.isEmpty()) {
            log.info("商品没有现有媒体记录，直接新增");
            existingMediaList = new ArrayList<>();
        }

        // 构建现有媒体的 ID -> Media 映射
        Map<Long, Media> existingMediaMap = existingMediaList.stream()
                .collect(Collectors.toMap(Media::getMediaId, media -> media));

        // 收集需要保留的媒体 ID
        Set<Long> keepMediaIds = new HashSet<>();

        // ==================== 2. 先收集所有需要重命名的媒体文件（在实际更新数据库之前） ====================
        List<MediaRenameVo> toRenameList = new ArrayList<>();

        // 收集所有被变体绑定的媒体ID
        List<ProductVariant> variantList = product.getProductVariantList();
        Set<Long> variantMediaIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(variantList)) {
            variantList.stream()
                    .map(ProductVariant::getMediaId)
                    .filter(Objects::nonNull)
                    .forEach(variantMediaIds::add);
        }

        // 提前计算所有媒体的新文件名
        this.collectAllMediaRenameTasks(newMediaList, variantList, variantMediaIds, spu, toRenameList);

        // ==================== 3. 处理新媒体列表（根据下标设置 position，同时更新文件名和URL） ====================
        if (!CollectionUtils.isEmpty(newMediaList)) {
            // 构建重命名任务的映射，方便快速查找
            Map<Long, MediaRenameVo> renameTaskMap = toRenameList.stream()
                    .filter(task -> task.getMedia().getMediaId() != null)
                    .collect(Collectors.toMap(
                            task -> task.getMedia().getMediaId(),
                            task -> task
                    ));

            for (int i = 0; i < newMediaList.size(); i++) {
                Media media = newMediaList.get(i);
                Long mediaId = media.getMediaId();
                Media editMedia = new  Media();
                BeanUtils.copyBeanProp(editMedia,media);

                // 设置 position（从 0 开始）
                editMedia.setPosition(i);

                // 检查是否有对应的重命名任务
                MediaRenameVo renameTask = renameTaskMap.get(mediaId);
                if (renameTask != null) {
                    // 使用计算好的新文件名和URL
                    String newFilename = renameTask.getNewFilename();
                    String oldFilePath = MediaFileUtil.convertUrlToFilePath(editMedia.getNasMediaUrl());
                    File oldFile = new File(oldFilePath);
                    String newFilePath = MediaFileUtil.fixFileSeparator(oldFile.getParent() + "/" + newFilename);
                    String newNasUrl = MediaFileUtil.generateNasUrl(newFilePath);

                    editMedia.setFilename(newFilename);
                    editMedia.setNasMediaUrl(newNasUrl);

                    log.debug("媒体 {} 将使用新文件名: {}, 新URL: {}", mediaId, newFilename, newNasUrl);
                }

                if (mediaId != null && existingMediaMap.containsKey(mediaId)) {
                    // 已存在的媒体，更新
                    editMedia.setUpdateTime(DateUtils.getNowDate());
                    this.updateById(editMedia);
                    keepMediaIds.add(mediaId);
                    log.debug("更新媒体记录，ID: {}, Position: {}", mediaId, i);
                } else {
                    // 新增媒体
                    editMedia.setProductId(productId);
                    editMedia.setCreateTime(DateUtils.getNowDate());
                    this.save(editMedia);
                    keepMediaIds.add(editMedia.getMediaId());
                    log.debug("新增媒体记录，ID: {}, Position: {}", editMedia.getMediaId(), i);
                }
            }
        }

        // ==================== 4. 删除不在新媒体列表中的媒体记录和文件 ====================
        Set<Long> toDeleteMediaIds = existingMediaMap.keySet().stream()
                .filter(id -> !keepMediaIds.contains(id))
                .collect(Collectors.toSet());

        if (!toDeleteMediaIds.isEmpty()) {
            // 先获取要删除的媒体文件路径
            List<Media> toDeleteMedias = existingMediaList.stream()
                    .filter(m -> toDeleteMediaIds.contains(m.getMediaId()))
                    .collect(Collectors.toList());

            // 删除数据库记录
            this.removeByIds(toDeleteMediaIds);
            log.info("删除媒体数据库记录，数量: {}, IDs: {}", toDeleteMediaIds.size(), toDeleteMediaIds);

            // 删除磁盘文件
            deleteMediaFilesFromDisk(toDeleteMedias);
        }

        // ==================== 5. 执行文件重命名 ====================
        this.doRenameMediaFiles(toRenameList);

        log.info("商品媒体更新完成，商品ID: {}, 保留媒体数: {}, 删除媒体数: {}, 重命名数: {}",
                productId, keepMediaIds.size(), toDeleteMediaIds.size(), toRenameList.size());
    }

    /**
     * 收集所有媒体的重命名任务
     * 统一计算所有媒体应该使用的新文件名
     *
     * @param mediaList       媒体列表
     * @param variantList     变体列表
     * @param variantMediaIds 变体绑定的媒体ID集合
     * @param spu             商品SPU
     * @param toRenameList    重命名任务列表（输出参数）
     */
    private void collectAllMediaRenameTasks(List<Media> mediaList,
                                            List<ProductVariant> variantList,
                                            Set<Long> variantMediaIds,
                                            String spu,
                                            List<MediaRenameVo> toRenameList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return;
        }

        // 收集变体媒体的重命名任务
        this.renameVariantMediaFiles(variantList, mediaList, spu, toRenameList);

        // 收集主图和其他媒体的重命名任务
        this.renameOtherMediaAndMainMediaFiles(mediaList, variantMediaIds, spu, toRenameList);
    }

    /**
     * 从磁盘删除媒体文件
     *
     * @param medias 要删除的媒体列表
     */
    @Override
    public void deleteMediaFilesFromDisk(List<Media> medias) {
        if (CollectionUtils.isEmpty(medias)) {
            return;
        }

        for (Media media : medias) {
            String nasMediaUrl = media.getNasMediaUrl();
            if (StringUtils.isEmpty(nasMediaUrl)) {
                log.warn("媒体文件路径为空，跳过删除，媒体ID: {}", media.getMediaId());
                continue;
            }

            try {
                // 将 URL 转换为文件路径
                String filePath = MediaFileUtil.convertUrlToFilePath(nasMediaUrl);
                File file = new File(filePath);

                if (file.exists() && file.isFile()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.info("删除媒体文件成功: {}", filePath);
                    } else {
                        log.warn("删除媒体文件失败: {}", filePath);
                    }
                } else {
                    log.warn("媒体文件不存在，跳过删除: {}", filePath);
                }
            } catch (Exception e) {
                log.error("删除媒体文件异常，媒体ID: {}, URL: {}, 错误: {}",
                        media.getMediaId(), nasMediaUrl, e.getMessage());
            }
        }
    }

    /**
     * 重命名变体绑定的规格图文件
     * <p>
     * 命名规则：SPU_规格值1_规格值2...扩展名
     * 例如：ABC0001_Red_L.jpg
     * </p>
     *
     * @param variantList 变体列表
     * @param spu         商品 SPU
     */
    private void renameVariantMediaFiles(List<ProductVariant> variantList, List<Media> mediaList, String spu,
                                         List<MediaRenameVo> toRenameList) {
        // 第一步：收集需要重命名的媒体文件
        Map<Long, Media> mediaMap = mediaList.stream().collect(Collectors.toMap(Media::getMediaId, media -> media));

        for (ProductVariant variant : variantList) {
            Long mediaId = variant.getMediaId();
            if (mediaId == null) {
                continue;
            }

            try {
                // 查询媒体信息
                Media media = mediaMap.get(mediaId);

                // 生成新文件名
                String fileExtension = MediaFileUtil.getFileExtensionByFilename(media.getFilename());
                String newFilename = MediaFileUtil.getVariantMediaFilename(spu, variant, fileExtension);
                if (StringUtils.isEmpty(newFilename)) {
                    log.warn("无法生成新文件名，跳过重命名，媒体ID: {}", mediaId);
                    continue;
                }

                // 如果文件名已经相同，跳过
                if (!newFilename.equals(media.getFilename())) {
                    toRenameList.add(new MediaRenameVo(media, newFilename));
                } else {
                    log.debug("文件名已正确，无需重命名，媒体ID: {}", mediaId);
                }

            } catch (Exception e) {
                log.error("重命名变体媒体文件异常，变体ID: {}, 媒体ID: {}, 错误: {}",
                        variant.getVariantId(), mediaId, e.getMessage(), e);
            }
        }
    }

    /**
     * 重命名主图和非规格图的媒体文件
     * <p>
     * 命名规则：SPU-序号.扩展名（序号从1开始）
     * 例如：ABC0001-1.jpg, ABC0001-2.png
     * </p>
     *
     * @param mediaList       所有保留的媒体列表（按 position 排序）
     * @param variantMediaIds 被变体绑定的媒体ID集合
     * @param spu             商品 SPU
     */
    private void renameOtherMediaAndMainMediaFiles(List<Media> mediaList, Set<Long> variantMediaIds, String spu, List<MediaRenameVo> toRenameList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return;
        }

        int sequence = 1;

        for (int i = 0; i < mediaList.size(); i++) {
            Media media = mediaList.get(i);
            Long mediaId = media.getMediaId();
            String extension = MediaFileUtil.getFileExtensionByFilename(media.getFilename());
            // 跳过主图（第一个媒体）
            if (i == 0) {
                // 生成新文件名

                String newFilename = MediaFileUtil.getMainMediaFilename(spu, extension);


                // 如果文件名已经相同，跳过
                if (!newFilename.equals(media.getFilename())) {
                    toRenameList.add(new MediaRenameVo(media, newFilename));
                } else {
                    log.debug("文件名已正确，无需重命名，媒体ID: {}", mediaId);
                }
                continue;
            }

            // 跳过被变体绑定的规格图
            if (variantMediaIds.contains(mediaId)) {
                log.debug("跳过规格图，媒体ID: {}", mediaId);
                continue;
            }

            // 生成新文件名
            String newFilename =  MediaFileUtil.getOtherMediaFilename(spu, sequence, extension);

            // 如果文件名已经相同，跳过
            if (!newFilename.equals(media.getFilename())) {
                toRenameList.add(new MediaRenameVo(media, newFilename));
            } else {
                log.debug("文件名已正确，无需重命名，媒体ID: {}", mediaId);
            }

            sequence++;
        }

        log.info("其他媒体文件重命名完成，SPU: {}, 处理数量: {}", spu, toRenameList.size());

    }

   
    @Override
    public void doRenameMediaFiles(List<MediaRenameVo> toRenameList) {
        if (toRenameList.isEmpty()) {
            log.info("没有需要重命名的其他媒体文件");
            return;
        }

        // 使用临时文件名进行重命名，避免冲突
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 临时文件名映射
        Map<String, RenameOperationVo> tempFilenameMap = new HashMap<>();

        for (MediaRenameVo task : toRenameList) {
            try {
                Media media = task.getMedia();
                String newFilename = task.getNewFilename();

                String oldFilePath = MediaFileUtil.convertUrlToFilePath(media.getNasMediaUrl());
                File oldFile = new File(oldFilePath);

                if (!oldFile.exists() || !oldFile.isFile()) {
                    log.warn("原文件不存在，跳过重命名: {}", oldFilePath);
                    // 从临时文件名映射中获取临时文件名
                    RenameOperationVo renameOp = tempFilenameMap.get(newFilename);
                    if (renameOp.getFile() != null) {
                        oldFile = renameOp.getFile();
                    } else {
                        log.warn("无法获取原文件，跳过重命名，媒体ID: {}", media.getMediaId());
                        continue;
                    }
                }

                // 使用正斜杠作为路径分隔符，保证跨平台一致性
                String newFilePath = MediaFileUtil.fixFileSeparator(oldFile.getParent() + "/" + newFilename);
                File newFile = new File(newFilePath);

                // 如果目标文件已存在，先移动到临时文件名
                if (newFile.exists()) {
                    log.warn("目标文件已存在，将被移动到临时位置: {}", newFilePath);
                    String tempFilename = "temp_" + timestamp + "_" + newFilename;
                    String tempFilePath = MediaFileUtil.fixFileSeparator(oldFile.getParent() + "/" + tempFilename);;
                    File tempFile = new File(tempFilePath);

                    boolean tempRenamed = newFile.renameTo(tempFile);
                    if (!tempRenamed) {
                        log.error("无法将目标文件移动到临时位置: {} -> {}", newFilePath, tempFilePath);
                        continue;
                    }
                    // 记录临时文件名映射，方便后续找到进行重命名
                    tempFilenameMap.put(newFilename, new RenameOperationVo(tempFilePath, newFilePath, newFile));
                    log.debug("目标文件已移动到临时位置: {} -> {}", newFilePath, tempFilePath);
                }

                // 执行重命名
                boolean renamed = oldFile.renameTo(newFile);
                if (renamed) {
                    // 更新数据库记录
                    media.setFilename(newFilename);
                    media.setNasMediaUrl(MediaFileUtil.generateNasUrl(newFilePath));
                    media.setUpdateTime(DateUtils.getNowDate());
                    this.updateById(media);

                    log.info("重命名其他媒体文件成功: {} -> {}", oldFilePath, newFilePath);
                } else {
                    log.error("重命名其他媒体文件失败: {} -> {}", oldFilePath, newFilePath);
                }
            } catch (Exception e) {
                log.error("重命名其他媒体文件异常，媒体ID: {}, 错误: {}",
                        task.getMedia().getMediaId(), e.getMessage(), e);
            }
        }

    }

    @Override
    public List<Media> listByProductId(Long productId) {
        return this.list(new LambdaQueryWrapper<>(Media.class)
                .eq(Media::getProductId, productId)
                .orderByAsc(Media::getPosition));
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
            this.remove(new LambdaQueryWrapper<>(Media.class).eq(Media::getProductId, Long.parseLong(productId)));
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                Media media = new Media();
                media.setProductId(Long.parseLong(productId));
                media.setFilename(file.getName());
                media.setNasMediaUrl(MediaFileUtil.generateNasUrl(file, dirPath));

                // 根据文件扩展名判断媒体类型
                String mediaType = MediaFileUtil.getMediaType(file.getName());
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


}



