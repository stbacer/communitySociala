package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 审核操作DTO
 */
@Data
public class ReviewDTO {
    /**
     * 目标ID（帖子ID或用户ID）
     */
    @NotNull(message = "目标ID不能为空")
    private Long targetId;
    
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