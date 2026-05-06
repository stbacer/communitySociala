package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 举报处理DTO
 */
@Data
public class ReportHandleDTO {
    /**
     * 举报ID
     */
    @NotNull(message = "举报ID不能为空")
    private Long reportId;
    
    /**
     * 处理状态：0待处理，1已处理，2已驳回
     */
    @NotNull(message = "处理状态不能为空")
    private Integer status;
    
    /**
     * 处理结果说明
     */
    private String handleResult;
}