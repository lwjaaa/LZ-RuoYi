package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.Media;
import com.ruoyi.erp.model.dto.media.MediaEdit;
import com.ruoyi.erp.model.dto.media.MediaInsert;
import com.ruoyi.erp.model.dto.media.MediaQuery;
import com.ruoyi.erp.model.vo.media.MediaVo;
import com.ruoyi.erp.service.IMediaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * erp媒体Controller
 *
 * @author lwj
 * @date 2026-03-26
 */
@RestController
@RequestMapping("/erp/media")
public class MediaController extends BaseController
{
    @Resource
    private IMediaService mediaService;

    /**
     * 查询erp媒体列表
     */
    @PreAuthorize("@ss.hasPermi('erp:media:list')")
    @GetMapping("/list")
    public TableDataInfo list(MediaQuery mediaQuery)
    {
        Media media = MediaQuery.queryToObj(mediaQuery);
        startPage();
        List<Media> list = mediaService.selectMediaList(media);
        List<MediaVo> listVo= list.stream().map(MediaVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出erp媒体列表
     */
    @PreAuthorize("@ss.hasPermi('erp:media:export')")
    @Log(title = "erp媒体", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MediaQuery mediaQuery)
    {
        Media media = MediaQuery.queryToObj(mediaQuery);
        List<Media> list = mediaService.selectMediaList(media);
        ExcelUtil<Media> util = new ExcelUtil<Media>(Media.class);
        util.exportExcel(response, list, "erp媒体数据");
    }

    /**
     * 获取erp媒体详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:media:query')")
    @GetMapping(value = "/{mediaId}")
    public AjaxResult getInfo(@PathVariable("mediaId") Long mediaId)
    {
        Media media = mediaService.selectMediaByMediaId(mediaId);
        return success(MediaVo.objToVo(media));
    }

    /**
     * 新增erp媒体
     */
    @PreAuthorize("@ss.hasPermi('erp:media:add')")
    @Log(title = "erp媒体", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MediaInsert mediaInsert)
    {
        Media media = MediaInsert.insertToObj(mediaInsert);
        return toAjax(mediaService.insertMedia(media));
    }

    /**
     * 修改erp媒体
     */
    @PreAuthorize("@ss.hasPermi('erp:media:edit')")
    @Log(title = "erp媒体", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MediaEdit mediaEdit)
    {
        Media media = MediaEdit.editToObj(mediaEdit);
        return toAjax(mediaService.updateMedia(media));
    }

    /**
     * 删除erp媒体
     */
    @PreAuthorize("@ss.hasPermi('erp:media:remove')")
    @Log(title = "erp媒体", businessType = BusinessType.DELETE)
	@DeleteMapping("/{mediaIds}")
    public AjaxResult remove(@PathVariable Long[] mediaIds)
    {
        return toAjax(mediaService.deleteMediaByMediaIds(mediaIds));
    }

    /**
     * 导入erp媒体数据
     */
    @PreAuthorize("@ss.hasPermi('erp:media:import')")
    @Log(title = "erp媒体", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<Media> util = new ExcelUtil<Media>(Media.class);
        List<Media> mediaList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = mediaService.importMediaData(mediaList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp媒体导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Media> util = new ExcelUtil<Media>(Media.class);
        util.importTemplateExcel(response, "erp媒体数据");
    }

    /**
     * 扫描服务器指定路径返回媒体列表
     */
    @PreAuthorize("@ss.hasPermi('vh-erp:media:scan')")
    @GetMapping("/vh-erp/media/scan")
    public AjaxResult scanMedia()
    {
        List<MediaVo> mediaList = mediaService.scanMediaFromDirectory();
        return success(mediaList);
    }
}
