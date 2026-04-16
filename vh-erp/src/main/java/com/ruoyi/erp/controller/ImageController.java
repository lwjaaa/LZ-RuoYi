package com.ruoyi.erp.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.erp.model.domain.Image;
import com.ruoyi.erp.model.dto.image.ImageEdit;
import com.ruoyi.erp.model.dto.image.ImageInsert;
import com.ruoyi.erp.model.dto.image.ImageQuery;
import com.ruoyi.erp.model.vo.image.ImageVo;
import com.ruoyi.erp.service.IImageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * erp图片Controller
 *
 * @author lwj
 * @date 2026-03-24
 */
@RestController
@RequestMapping("/erp/image")
public class ImageController extends BaseController
{
    @Resource
    private IImageService imageService;

    /**
     * 查询erp图片列表
     */
    @PreAuthorize("@ss.hasPermi('erp:image:list')")
    @GetMapping("/list")
    public TableDataInfo list(ImageQuery imageQuery)
    {
        Image image = ImageQuery.queryToObj(imageQuery);
        startPage();
        List<Image> list = imageService.selectImageList(image);
        List<ImageVo> listVo= list.stream().map(ImageVo::objToVo).collect(Collectors.toList());
        TableDataInfo table = getDataTable(list);
        table.setRows(listVo);
        return table;
    }

    /**
     * 导出erp图片列表
     */
    @PreAuthorize("@ss.hasPermi('erp:image:export')")
    @Log(title = "erp图片", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ImageQuery imageQuery)
    {
        Image image = ImageQuery.queryToObj(imageQuery);
        List<Image> list = imageService.selectImageList(image);
        ExcelUtil<Image> util = new ExcelUtil<Image>(Image.class);
        util.exportExcel(response, list, "erp图片数据");
    }

    /**
     * 获取erp图片详细信息
     */
    @PreAuthorize("@ss.hasPermi('erp:image:query')")
    @GetMapping(value = "/{imageId}")
    public AjaxResult getInfo(@PathVariable("imageId") Long imageId)
    {
        Image image = imageService.selectImageByImageId(imageId);
        return success(ImageVo.objToVo(image));
    }

    /**
     * 新增erp图片
     */
    @PreAuthorize("@ss.hasPermi('erp:image:add')")
    @Log(title = "erp图片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ImageInsert imageInsert)
    {
        Image image = ImageInsert.insertToObj(imageInsert);
        return toAjax(imageService.insertImage(image));
    }

    /**
     * 修改erp图片
     */
    @PreAuthorize("@ss.hasPermi('erp:image:edit')")
    @Log(title = "erp图片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ImageEdit imageEdit)
    {
        Image image = ImageEdit.editToObj(imageEdit);
        return toAjax(imageService.updateImage(image));
    }

    /**
     * 删除erp图片
     */
    @PreAuthorize("@ss.hasPermi('erp:image:remove')")
    @Log(title = "erp图片", businessType = BusinessType.DELETE)
	@DeleteMapping("/{imageIds}")
    public AjaxResult remove(@PathVariable Long[] imageIds)
    {
        return toAjax(imageService.deleteImageByImageIds(imageIds));
    }

    /**
     * 导入erp图片数据
     */
    @PreAuthorize("@ss.hasPermi('erp:image:import')")
    @Log(title = "erp图片", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<Image> util = new ExcelUtil<Image>(Image.class);
        List<Image> imageList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = imageService.importImageData(imageList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载erp图片导入模板
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Image> util = new ExcelUtil<Image>(Image.class);
        util.importTemplateExcel(response, "erp图片数据");
    }
}
