package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布帖子DTO
 */
@Data
public class PostCreateDTO {
    /**
     * 帖子ID（更新时使用）
     */
    private Long postId;
    
    /**
     * 板块分类 ID
     */
    @NotNull(message = "板块分类不能为空")
    private Integer categoryId;
    
    /**
     * 帖子标题
     */
    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
    
    /**
     * 帖子内容
     */
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;
    
    /**
     * 图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 发布位置经度
     */
    private BigDecimal longitude;
    
    /**
     * 发布位置纬度
     */
    private BigDecimal latitude;
    
    /**
     * 是否匿名：0 否，1 是
     */
    private Integer isAnonymous = 0;
    
    /**
     * 物品价格（元）- 二手交易专用
     */
    private BigDecimal price;
    
    /**
     * 交易方式：1 自提，2 快递，3 两者皆可 - 二手交易专用
     */
    private Integer transactionMode;
    
    /**
     * 联系方式 - 二手交易专用
     */
    @Size(max = 20, message = "联系方式长度不能超过 20 个字符")
    private String contactInfo;
}