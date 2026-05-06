package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情VO
 */
@Data
public class PostDetailVO {
    /**
     * 帖子ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;
    
    /**
     * 用户信息
     */
    private UserInfoVO userInfo;
    
    /**
     * 板块分类 ID
     */
    private Integer categoryId;
    
    /**
     * 板块分类名称
     */
    private String categoryName;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 帖子内容
     */
    private String content;
    
    /**
     * 图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 经纬度
     */
    private BigDecimal longitude;
    private BigDecimal latitude;
    
    /**
     * 统计数据
     */
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    
    /**
     * 是否匿名
     */
    private Integer isAnonymous;
    
    /**
     * 是否置顶
     */
    private Integer isTop;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;
    
    /**
     * 当前用户是否收藏
     */
    private Boolean isCollected;
    
    /**
     * 二手交易相关字段
     */
    private java.math.BigDecimal price;
    private Integer transactionMode;
    private String contactInfo;
    
    /**
     * 审核相关字段
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewRemark;
    
    /**
     * 距离（用于附近帖子排序，单位：公里）
     */
    private Double distance;
}
