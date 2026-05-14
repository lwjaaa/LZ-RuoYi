package com.ruoyi.erp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.erp.mapper.MediaMapper;
import com.ruoyi.erp.mapper.ProductMapper;
import com.ruoyi.erp.mapper.ProductVariantMapper;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.domain.Product;
import com.ruoyi.erp.model.domain.ProductVariant;
import com.ruoyi.erp.model.dto.media.MediaQuery;
import com.ruoyi.erp.model.vo.media.MediaRenameVo;
import com.ruoyi.erp.model.vo.media.MediaVo;
import com.ruoyi.erp.model.vo.media.RenameOperationVo;
import com.ruoyi.erp.service.IMediaService;
import com.ruoyi.erp.service.IProductQualityService;
import com.ruoyi.erp.utils.MediaFileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Resource
    private ProductVariantMapper productVariantMapper;
    @Resource
    private IProductQualityService productQualityService;

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

        Long storeId = mediaQuery.getStoreId();
        queryWrapper.eq(StringUtils.isNotNull(storeId), "store_id", storeId);

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
        this.collectAllMediaRenameTasks(newMediaList, variantList, variantMediaIds, product.getKeyWord(), toRenameList);

        // ==================== 3. 处理新媒体列表（根据下标设置 position，同时更新文件名和URL） ====================
        if (!CollectionUtils.isEmpty(newMediaList)) {
            // 构建重命名任务的映射，方便快速查找
            Map<Long, MediaRenameVo> renameTaskMap = toRenameList.stream()
                    .filter(task -> task.getMedia().getMediaId() != null)
                    .collect(Collectors.toMap(
                            task -> task.getMedia().getMediaId(),
                            task -> task,
                            (first, second) -> first
                    ));

            for (int i = 0; i < newMediaList.size(); i++) {
                Media media = newMediaList.get(i);
                Long mediaId = media.getMediaId();
                Media editMedia = new Media();
                BeanUtils.copyBeanProp(editMedia, media);
                editMedia.setStoreId(product.getStoreId());

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
                    editMedia.setStoreId(product.getStoreId());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncProductMediaKeyword(Product product, String oldKeyword, List<ProductVariant> variantList) {
        if (product == null || product.getProductId() == null || StringUtils.isEmpty(oldKeyword)) {
            return;
        }

        String newKeyword = product.getKeyWord();
        if (StringUtils.isEmpty(newKeyword) || Objects.equals(oldKeyword, newKeyword)) {
            return;
        }

        List<Media> mediaList = this.listByProductId(product.getProductId());
        if (CollectionUtils.isEmpty(mediaList)) {
            log.info("商品没有可同步的媒体记录，商品ID: {}, 旧关键词: {}, 新关键词: {}",
                    product.getProductId(), oldKeyword, newKeyword);
            return;
        }

        Path targetDir = Paths.get(RuoYiConfig.getMediaPath(), newKeyword).normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            throw new ServiceException("创建目标媒体目录失败: " + targetDir);
        }

        Map<Long, ProductVariant> variantMediaMap = buildVariantMediaMap(variantList);
        List<MediaMoveOperation> operations = buildMediaKeywordMoveOperations(mediaList, variantMediaMap, oldKeyword, newKeyword, targetDir);

        for (MediaMoveOperation operation : operations) {
            try {
                if (!operation.oldPath.equals(operation.newPath)) {
                    Files.move(operation.oldPath, operation.newPath);
                }
                if (operation.oldTranscodedPath != null && operation.newTranscodedPath != null
                        && !operation.oldTranscodedPath.equals(operation.newTranscodedPath)) {
                    Files.createDirectories(operation.newTranscodedPath.getParent());
                    Files.move(operation.oldTranscodedPath, operation.newTranscodedPath);
                }
            } catch (IOException e) {
                throw new ServiceException("移动媒体文件失败: " + operation.oldPath + " -> " + operation.newPath);
            }

            Media mediaUpdate = new Media();
            mediaUpdate.setMediaId(operation.media.getMediaId());
            mediaUpdate.setFilename(operation.newFilename);
            mediaUpdate.setNasMediaUrl(operation.newNasMediaUrl);
            mediaUpdate.setTranscodedMediaUrl(operation.newTranscodedMediaUrl);
            mediaUpdate.setUpdateTime(DateUtils.getNowDate());
            this.updateById(mediaUpdate);
        }

        deleteEmptyShopifySyncDirectory(Paths.get(RuoYiConfig.getMediaPath(), oldKeyword, "_shopify_sync").normalize());
        deleteEmptyOldKeywordDirectory(oldKeyword, targetDir);
        log.info("商品媒体关键词同步完成，商品ID: {}, 旧关键词: {}, 新关键词: {}, 媒体数量: {}",
                product.getProductId(), oldKeyword, newKeyword, operations.size());
    }

    private Map<Long, ProductVariant> buildVariantMediaMap(List<ProductVariant> variantList) {
        Map<Long, ProductVariant> variantMediaMap = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(variantList)) {
            return variantMediaMap;
        }

        for (ProductVariant variant : variantList) {
            Long mediaId = variant.getMediaId();
            if (mediaId != null && !variantMediaMap.containsKey(mediaId)) {
                variantMediaMap.put(mediaId, variant);
            }
        }
        return variantMediaMap;
    }

    private List<MediaMoveOperation> buildMediaKeywordMoveOperations(List<Media> mediaList,
                                                                     Map<Long, ProductVariant> variantMediaMap,
                                                                     String oldKeyword,
                                                                     String newKeyword,
                                                                     Path targetDir) {
        List<MediaMoveOperation> operations = new ArrayList<>();
        Set<Path> plannedTargetPaths = new HashSet<>();
        Set<Path> plannedTranscodedTargetPaths = new HashSet<>();
        int sequence = 1;

        for (Media media : mediaList) {
            String extension = MediaFileUtil.getFileExtensionByFilename(media.getFilename());
            ProductVariant variant = variantMediaMap.get(media.getMediaId());
            String newFilename;
            if (variant != null && StringUtils.isNotEmpty(variant.getSku())) {
                newFilename = MediaFileUtil.concatFilename(variant.getSku(), extension);
            } else {
                newFilename = MediaFileUtil.getOtherMediaFilename(newKeyword, sequence, extension);
                sequence++;
            }

            String oldFilePath = MediaFileUtil.convertUrlToFilePath(media.getNasMediaUrl());
            if (StringUtils.isEmpty(oldFilePath)) {
                oldFilePath = Paths.get(RuoYiConfig.getMediaPath(), oldKeyword, media.getFilename()).toString();
            }
            Path oldPath = Paths.get(oldFilePath).normalize();
            Path newPath = targetDir.resolve(newFilename).normalize();

            if (!Files.exists(oldPath) || !Files.isRegularFile(oldPath)) {
                throw new ServiceException("媒体文件不存在: " + oldPath);
            }
            if (!plannedTargetPaths.add(newPath)) {
                throw new ServiceException("目标媒体文件名重复: " + newPath);
            }
            if (!oldPath.equals(newPath) && Files.exists(newPath)) {
                throw new ServiceException("目标媒体文件已存在: " + newPath);
            }

            String newNasUrl = MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(newPath.toString()));
            TranscodedMoveInfo transcodedMoveInfo = buildTranscodedMoveInfo(media, oldPath, newPath);
            if (transcodedMoveInfo.newPath() != null) {
                if (!plannedTranscodedTargetPaths.add(transcodedMoveInfo.newPath())) {
                    throw new ServiceException("目标转码媒体文件名重复: " + transcodedMoveInfo.newPath());
                }
                if (!transcodedMoveInfo.oldPath().equals(transcodedMoveInfo.newPath()) && Files.exists(transcodedMoveInfo.newPath())) {
                    throw new ServiceException("目标转码媒体文件已存在: " + transcodedMoveInfo.newPath());
                }
            }

            operations.add(new MediaMoveOperation(
                    media,
                    oldPath,
                    newPath,
                    newFilename,
                    newNasUrl,
                    transcodedMoveInfo.oldPath(),
                    transcodedMoveInfo.newPath(),
                    transcodedMoveInfo.newNasUrl()));
        }

        return operations;
    }

    private TranscodedMoveInfo buildTranscodedMoveInfo(Media media, Path oldPath, Path newPath) {
        Path oldTranscodedPath = resolveExistingTranscodedPath(media, oldPath);
        if (oldTranscodedPath == null) {
            return new TranscodedMoveInfo(null, null, null);
        }

        String newTranscodedFilename = buildTranscodedFilename(newPath.getFileName().toString());
        Path newTranscodedPath = newPath.getParent()
                .resolve("_shopify_sync")
                .resolve(newTranscodedFilename)
                .normalize();
        String newTranscodedNasUrl = MediaFileUtil.generateNasUrl(MediaFileUtil.fixFileSeparator(newTranscodedPath.toString()));
        return new TranscodedMoveInfo(oldTranscodedPath, newTranscodedPath, newTranscodedNasUrl);
    }

    private Path resolveExistingTranscodedPath(Media media, Path oldPath) {
        if (StringUtils.isNotEmpty(media.getTranscodedMediaUrl())) {
            String transcodedFilePath = MediaFileUtil.convertUrlToFilePath(media.getTranscodedMediaUrl());
            if (StringUtils.isNotEmpty(transcodedFilePath)) {
                Path path = Paths.get(transcodedFilePath).normalize();
                if (Files.isRegularFile(path)) {
                    return path;
                }
            }
        }

        Path expectedPath = oldPath.getParent()
                .resolve("_shopify_sync")
                .resolve(buildTranscodedFilename(oldPath.getFileName().toString()))
                .normalize();
        return Files.isRegularFile(expectedPath) ? expectedPath : null;
    }

    private String buildTranscodedFilename(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        String basename = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return basename + ".webp";
    }

    private void deleteEmptyOldKeywordDirectory(String oldKeyword, Path targetDir) {
        Path oldDir = Paths.get(RuoYiConfig.getMediaPath(), oldKeyword).normalize();
        if (oldDir.equals(targetDir) || !Files.isDirectory(oldDir)) {
            return;
        }

        try (Stream<Path> children = Files.list(oldDir)) {
            if (children.findAny().isEmpty()) {
                Files.delete(oldDir);
            }
        } catch (IOException e) {
            log.warn("删除旧媒体空目录失败: {}, 错误: {}", oldDir, e.getMessage());
        }
    }

    private static class MediaMoveOperation {
        private final Media media;
        private final Path oldPath;
        private final Path newPath;
        private final String newFilename;
        private final String newNasMediaUrl;
        private final Path oldTranscodedPath;
        private final Path newTranscodedPath;
        private final String newTranscodedMediaUrl;

        private MediaMoveOperation(Media media, Path oldPath, Path newPath, String newFilename, String newNasMediaUrl,
                                   Path oldTranscodedPath, Path newTranscodedPath, String newTranscodedMediaUrl) {
            this.media = media;
            this.oldPath = oldPath;
            this.newPath = newPath;
            this.newFilename = newFilename;
            this.newNasMediaUrl = newNasMediaUrl;
            this.oldTranscodedPath = oldTranscodedPath;
            this.newTranscodedPath = newTranscodedPath;
            this.newTranscodedMediaUrl = newTranscodedMediaUrl;
        }
    }

    private record TranscodedMoveInfo(Path oldPath, Path newPath, String newNasUrl) {
    }

    private void collectAllMediaRenameTasks(List<Media> mediaList,
                                            List<ProductVariant> variantList,
                                            Set<Long> variantMediaIds,
                                            String keyword,
                                            List<MediaRenameVo> toRenameList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return;
        }

        this.renameVariantMediaFiles(variantList, mediaList, toRenameList);

        // 收集主图和其他媒体的重命名任务
        this.renameOtherMediaAndMainMediaFiles(mediaList, variantMediaIds, keyword, toRenameList);
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
                deleteDerivedMediaFile(media, file);

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
     */
    private void deleteDerivedMediaFile(Media media, File sourceFile) {
        Set<Path> derivedPaths = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(media.getTranscodedMediaUrl())) {
            String transcodedFilePath = MediaFileUtil.convertUrlToFilePath(media.getTranscodedMediaUrl());
            if (StringUtils.isNotEmpty(transcodedFilePath)) {
                derivedPaths.add(Paths.get(transcodedFilePath).normalize());
            }
        }
        Path expectedDerivedPath = buildExpectedDerivedWebpPath(sourceFile);
        if (expectedDerivedPath != null) {
            derivedPaths.add(expectedDerivedPath);
        }

        for (Path derivedPath : derivedPaths) {
            deleteFileIfExists(derivedPath, media.getMediaId());
        }
    }

    private Path buildExpectedDerivedWebpPath(File sourceFile) {
        if (sourceFile == null || sourceFile.getParentFile() == null) {
            return null;
        }
        String filename = sourceFile.getName();
        String extension = getLowerExtension(filename);
        if (!Set.of("jpg", "jpeg", "png", "bmp", "webp").contains(extension)) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        String basename = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return sourceFile.getParentFile().toPath()
                .resolve("_shopify_sync")
                .resolve(basename + ".webp")
                .normalize();
    }

    private String getLowerExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteFileIfExists(Path filePath, Long mediaId) {
        try {
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                Files.delete(filePath);
                log.info("删除 Shopify 同步派生媒体文件成功: {}", filePath);
                deleteEmptyShopifySyncDirectory(filePath.getParent());
            }
        } catch (Exception e) {
            log.warn("删除 Shopify 同步派生媒体文件失败，媒体ID: {}, 文件: {}, 错误: {}",
                    mediaId, filePath, e.getMessage());
        }
    }

    private void deleteEmptyShopifySyncDirectory(Path directory) {
        if (directory == null || !"_shopify_sync".equals(directory.getFileName().toString())) {
            return;
        }
        try (Stream<Path> children = Files.list(directory)) {
            if (children.findAny().isEmpty()) {
                Files.delete(directory);
            }
        } catch (Exception e) {
            log.debug("清理空的 Shopify 同步派生目录失败: {}, 错误: {}", directory, e.getMessage());
        }
    }

    private void renameVariantMediaFiles(List<ProductVariant> variantList, List<Media> mediaList,
                                         List<MediaRenameVo> toRenameList) {
        // 第一步：收集需要重命名的媒体文件
        Map<Long, Media> mediaMap = mediaList.stream().collect(Collectors.toMap(Media::getMediaId, media -> media));

        if (CollectionUtils.isEmpty(variantList)) {
            return;
        }
        Set<Long> renamedMediaIds = new HashSet<>();
        for (ProductVariant variant : variantList) {
            Long mediaId = variant.getMediaId();
            if (mediaId == null) {
                continue;
            }
            if (renamedMediaIds.contains(mediaId)) {
                log.debug("同一个媒体已按第一个 SKU 生成重命名任务，跳过后续变体，媒体ID: {}, SKU: {}", mediaId, variant.getSku());
                continue;
            }

            try {
                // 查询媒体信息
                Media media = mediaMap.get(mediaId);
                if (media == null) {
                    log.warn("变体绑定的媒体不在当前媒体列表中，跳过 SKU 图重命名，媒体ID: {}, SKU: {}", mediaId, variant.getSku());
                    continue;
                }
                String sku = variant.getSku();
                if (StringUtils.isEmpty(sku)) {
                    log.warn("变体 SKU 为空，跳过 SKU 图重命名，变体ID: {}, 媒体ID: {}", variant.getVariantId(), mediaId);
                    continue;
                }

                // 生成新文件名
                String fileExtension = MediaFileUtil.getFileExtensionByFilename(media.getFilename());
                String newFilename = MediaFileUtil.concatFilename(sku, fileExtension);
                if (StringUtils.isEmpty(newFilename)) {
                    log.warn("无法生成新文件名，跳过重命名，媒体ID: {}", mediaId);
                    continue;
                }

                // 如果文件名已经相同，跳过
                renamedMediaIds.add(mediaId);
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
     * @param filenamePrefix  文件名前缀
     */
    private void renameOtherMediaAndMainMediaFiles(List<Media> mediaList, Set<Long> variantMediaIds, String filenamePrefix, List<MediaRenameVo> toRenameList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return;
        }

        int sequence = 1;

        for (int i = 0; i < mediaList.size(); i++) {
            Media media = mediaList.get(i);
            Long mediaId = media.getMediaId();
            String extension = MediaFileUtil.getFileExtensionByFilename(media.getFilename());

            // 跳过被变体绑定的规格图
            if (variantMediaIds.contains(mediaId)) {
                log.debug("跳过规格图，媒体ID: {}", mediaId);
                continue;
            }

            // 生成新文件名
            String newFilename = MediaFileUtil.getOtherMediaFilename(filenamePrefix, sequence, extension);

            // 如果文件名已经相同，跳过
            if (!newFilename.equals(media.getFilename())) {
                toRenameList.add(new MediaRenameVo(media, newFilename));
            } else {
                log.debug("文件名已正确，无需重命名，媒体ID: {}", mediaId);
            }

            sequence++;
        }

        log.info("其他媒体文件重命名完成, 处理数量: {}", toRenameList.size());

    }


    @Override
    public void doRenameMediaFiles(List<MediaRenameVo> toRenameList) {
        if (toRenameList.isEmpty()) {
            log.info("没有需要重命名的其他媒体文件");
            return;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        List<RenameOperationVo> renameOperations = new ArrayList<>();

        // 第一阶段先把所有源文件移到唯一临时名，避免文件名互换或环形重命名互相占位。
        for (int i = 0; i < toRenameList.size(); i++) {
            MediaRenameVo task = toRenameList.get(i);
            try {
                Media media = task.getMedia();
                String newFilename = task.getNewFilename();
                String oldFilePath = MediaFileUtil.convertUrlToFilePath(media.getNasMediaUrl());
                File oldFile = new File(oldFilePath);

                if (!oldFile.exists() || !oldFile.isFile()) {
                    log.warn("原文件不存在，跳过重命名: {}", oldFilePath);
                    continue;
                }

                String newFilePath = MediaFileUtil.fixFileSeparator(oldFile.getParent() + "/" + newFilename);
                if (MediaFileUtil.fixFileSeparator(oldFile.getAbsolutePath()).equals(newFilePath)) {
                    log.debug("文件名已是目标名称，跳过磁盘重命名，媒体ID: {}", media.getMediaId());
                    continue;
                }

                String extension = MediaFileUtil.getFileExtensionByFilename(oldFile.getName());
                String tempFilename = ".rename_" + timestamp + "_" + media.getMediaId() + "_" + i + "." + extension;
                String tempFilePath = MediaFileUtil.fixFileSeparator(oldFile.getParent() + "/" + tempFilename);
                File tempFile = new File(tempFilePath);

                boolean tempRenamed = oldFile.renameTo(tempFile);
                if (!tempRenamed) {
                    log.error("无法将源文件移动到临时位置: {} -> {}", oldFilePath, tempFilePath);
                    continue;
                }

                renameOperations.add(new RenameOperationVo(tempFilePath, newFilePath, tempFile));
                log.debug("源文件已移动到临时位置: {} -> {}", oldFilePath, tempFilePath);
            } catch (Exception e) {
                log.error("移动媒体临时文件异常，媒体ID: {}, 错误: {}", task.getMedia().getMediaId(), e.getMessage(), e);
            }
        }

        // 第二阶段再落到最终文件名，此时本轮源文件都已让出原路径。
        for (RenameOperationVo operation : renameOperations) {
            File tempFile = operation.getFile();
            File newFile = new File(operation.getOriginFilePath());
            if (newFile.exists()) {
                log.error("目标文件已存在，跳过重命名以避免覆盖未知文件: {}", operation.getOriginFilePath());
                continue;
            }

            boolean renamed = tempFile.renameTo(newFile);
            if (renamed) {
                log.info("重命名媒体文件成功: {} -> {}", operation.getTempFilePath(), operation.getOriginFilePath());
            } else {
                log.error("重命名媒体文件失败: {} -> {}", operation.getTempFilePath(), operation.getOriginFilePath());
            }
        }

    }

    @Override
    public List<Media> listByProductId(Long productId) {
        return this.list(new LambdaQueryWrapper<>(Media.class)
                .eq(Media::getProductId, productId)
                .orderByAsc(Media::getPosition));
    }

    /**
     * 根据商品spu或者id，扫描媒体目录，将媒体文件添加到商品中，并返回媒体列表
     * 扫描规则：1.扫描指定目录下的所有媒体文件。如果数据库中已存在对应文件的媒体记录，文件信息,并保留原有的Shopify关联和变体绑定。
     *          2.如果不存在，则新增媒体记录。对于数据库中存在但在目录中缺失的媒体记录，删除数据库记录和变体绑定,主图绑定。
     * @param productId
     * @return java.util.List<com.ruoyi.erp.model.domain.Media>
     * @author lwj
     **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Media> scanMediaToProduct(Long productId) {
        log.info("开始扫描媒体目录");

        // 1.获取商品信息，构建媒体目录路径
        Product product = productMapper.selectById(productId);
        if(product == null){
            throw new ServiceException("商品不存在");
        }
        String dirPathAndPrefix = product.getKeyWord();
        String basePath = RuoYiConfig.getMediaPath() + dirPathAndPrefix;

        File dir = new File(basePath);

        // 检查目录是否存在
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ServiceException("目录不存在：" + basePath);
        }

        // 2.获取目录下所有文件（包括图片和视频）
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
        // 3.构建磁盤媒体文件的路径URL到媒体记录的映射
        Map<String, Media> fileMediaMap = new HashMap<>();
        if (files != null) {
            fileMediaMap = Arrays.stream(files).map(file -> {
                Media media = new Media();
                media.setNasMediaUrl(MediaFileUtil.generateNasUrl(file, dirPathAndPrefix));
                media.setMediaContentType(MediaFileUtil.getMediaType(file.getName()));
                media.setFilename(file.getName());
                return media;
            }).collect(Collectors.toMap(Media::getNasMediaUrl, media -> media, (first, second) -> first));
        }

        // 4.遍历数据库的文件列表，如果磁盘文件不存在，就删除数据，删除变体、主图的绑定。如果文件存在就更新数据和下标
        List<Media> existingMediaList = this.listByProductId(productId);
        Set<Long> deleteMediaIds = new HashSet<>();
        for (int i = 0; i < existingMediaList.size(); i++) {
            Media media = existingMediaList.get(i);
            Media fileMedia = fileMediaMap.get(media.getNasMediaUrl());
            fileMediaMap.remove(media.getNasMediaUrl());
            if(fileMedia != null){
                media.setMediaContentType(fileMedia.getMediaContentType());
                media.setUpdateTime(DateUtils.getNowDate());
                media.setPosition(i);
                this.updateById(media);
            }else{
                deleteMediaIds.add(media.getMediaId());
                log.info("扫描媒体时发现数据库记录缺失对应文件，准备删除媒体记录，商品ID: {}, 媒体ID: {}, 文件URL: {}",
                        productId, media.getMediaId(), media.getNasMediaUrl());
            }
        }
        if (!deleteMediaIds.isEmpty()) {
            clearProductMediaReferences(productId, deleteMediaIds);
            this.removeByIds(deleteMediaIds);
            log.info("扫描媒体时删除缺失媒体数据库记录，商品ID: {}, 媒体IDs: {}", productId, deleteMediaIds);
        }

        // 5.遍历磁盘剩余的文件进行保存，这部分是新增的媒体记录
        int positionOffset = existingMediaList.size();
        for (Media media : fileMediaMap.values()) {
            positionOffset++;
            media.setProductId(productId);
            media.setPosition(positionOffset);
            media.setCreateTime(DateUtils.getNowDate());
            this.save(media);
        }

        productQualityService.refreshProductMissingFields(productId);
        return this.listByProductId(productId);
    }

    private void clearProductMediaReferences(Long productId, Set<Long> deleteMediaIds) {
        productMapper.update(null, new UpdateWrapper<Product>()
                .set("main_media_id", null)
                .eq("product_id", productId)
                .in("main_media_id", deleteMediaIds));
        productVariantMapper.update(null, new UpdateWrapper<ProductVariant>()
                .set("media_id", null)
                .eq("product_id", productId)
                .in("media_id", deleteMediaIds));
    }


}
