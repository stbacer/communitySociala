package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发布评论DTO
 */
@Data
public class CommentCreateDTO {
    /**
     * 帖子ID
     */
    @NotNull(message = "帖子ID不能为空")
    private Long postId;
    
    /**
     * 父评论ID
     */
    private Long parentId;
    
    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过1000个字符")
    private String content;
    
    /**
     * 评论图片URL
     */
    private String imageUrl;
}