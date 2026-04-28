package com.ruoyi.erp.model.vo.media;

import lombok.Data;

import java.io.File;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/24 16:19
 **/
@Data
public class RenameOperationVo {
    String tempFilePath;
    String originFilePath;
    File file;

    public RenameOperationVo() {
    }

    public RenameOperationVo(String tempFilePath, String originFilePath, File file) {
        this.tempFilePath = tempFilePath;
        this.originFilePath = originFilePath;
        this.file = file;
    }
}
