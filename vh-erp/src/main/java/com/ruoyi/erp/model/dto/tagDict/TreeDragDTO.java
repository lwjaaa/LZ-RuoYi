package com.ruoyi.erp.model.dto.tagDict;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author lwj
 * @version 1.0.0
 * @description TODO
 * @date 2026/3/27 22:41
 **/

@Data
public class TreeDragDTO {

    @NotNull(message = "被拖拽节点 ID 不能为空")
    private Long dragId;       // 被拖拽节点 ID

    @NotNull(message = "目标节点 ID 不能为空")
    private Long targetId;     // 目标节点 ID

    @NotNull(message = "放置类型不能为空")
    private String dropType;   // before/after/inner

}