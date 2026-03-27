package com.ruoyi.erp.mapper;

import java.util.List;
import com.ruoyi.erp.model.domain.Media;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * erp媒体Mapper接口
 * 
 * @author lwj
 * @date 2026-03-26
 */
public interface MediaMapper extends BaseMapper<Media>
{
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
     * 删除erp媒体
     * 
     * @param mediaId erp媒体主键
     * @return 结果
     */
    public int deleteMediaByMediaId(Long mediaId);

    /**
     * 批量删除erp媒体
     * 
     * @param mediaIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMediaByMediaIds(Long[] mediaIds);
}
