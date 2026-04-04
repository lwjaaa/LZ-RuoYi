package com.ruoyi.erp.model.vo.media;

import lombok.Data;

import java.io.Serializable;

/**
 * @author lwj
 * @version 1.0.0
 * @description 服务器媒体资源对象
 * @date 2026/4/3 11:15
 **/
@Data
public class ServerMediaVo implements Serializable {


    private static final long serialVersionUID = 1L;
    /**
     * 文件名
     */
    private String filename;
    /**
     * 文件url
     */
    private String url;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 最后修改时间
     */
    private String lastModified;
}
