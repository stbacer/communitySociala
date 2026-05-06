package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子 VO（用于列表展示）
 */
@Data
public class PostVO {
    /**
     * 帖子 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;
    
    /**
     * 用户 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    
    /**
     * 板块分类 ID
     */
    private Integer categoryId;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 帖子内容
     */
    private String content;
    
    /**
     * 图片 URL 列表
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
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 二手交易相关字段
     */
    private java.math.BigDecimal price;
    private Integer transactionMode;
    private String contactInfo;
    
    /**
     * 帖子类型（1:求助, 2:分享, 3:资讯）- 用于前端显示
     */
    private Integer type;
    
    /**
     * 板块分类名称
     */
    private String categoryName;
}
