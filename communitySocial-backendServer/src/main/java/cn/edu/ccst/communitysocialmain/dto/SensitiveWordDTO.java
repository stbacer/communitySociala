package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SensitiveWordDTO {
    /**
     * 敏感词内容
     */
    @NotBlank(message = "敏感词不能为空")
    private String word;

    /**
     * 敏感词类型
     */
    private String type;

    /**
     * 是否启用：0禁用，1启用
     */
    @NotNull(message = "启用状态不能为空")
    private Integer status = 1;

    /**
     * 描述说明
     */
    private String description;
}