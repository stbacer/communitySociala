package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 提交举报DTO
 */
@Data
public class ReportSubmitDTO {
    /**
     * 被举报目标类型：1帖子，2评论，3用户
     */
    @NotNull(message = "举报目标类型不能为空")
    private Integer targetType;
    
    /**
     * 目标ID
     */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
    
    /**
     * 举报原因
     */
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 500, message = "举报原因长度不能超过500个字符")
    private String reason;
}