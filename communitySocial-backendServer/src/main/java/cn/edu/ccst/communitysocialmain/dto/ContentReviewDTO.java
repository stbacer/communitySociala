package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 内容审核DTO
 */
@Data
public class ContentReviewDTO {
    /**
     * 内容ID
     */
    @NotBlank(message = "内容ID不能为空")
    private String contentId;
    
    /**
     * 内容类型：post帖子，comment评论，user用户
     */
    @NotBlank(message = "内容类型不能为空")
    private String contentType;
    
    /**
     * 审核状态：0拒绝，1通过，2驳回
     */
    @NotNull(message = "审核状态不能为空")
    private Integer status;
    
    /**
     * 审核备注
     */
    private String remark;
}