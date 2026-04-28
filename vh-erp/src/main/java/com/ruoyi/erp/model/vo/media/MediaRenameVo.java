package com.ruoyi.erp.model.vo.media;

import com.ruoyi.erp.model.domain.Media;
import lombok.Data;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/4/24 16:17
 **/
@Data
public class MediaRenameVo {
    Media media;
    String newFilename;

    public MediaRenameVo() {
    }

    public MediaRenameVo(Media media, String newFilename) {
        this.media = media;
        this.newFilename = newFilename;
    }
}
