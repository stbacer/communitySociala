package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class SystemConfigDTO {
    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置描述
     */
    private String description;
}