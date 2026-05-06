package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 敏感词实体类
 */
@Data
public class SensitiveWord {
    /**
     * 敏感词ID
     */
    private Integer wordId;
    
    /**
     * 敏感词
     */
    private String word;
    
    /**
     * 敏感词类型：1通用，2政治，3色情，4暴力，5广告
     */
    private String type;
    
    /**
     * 状态：0禁用，1启用
     */
    private Integer status;
    
    /**
     * 描述说明
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}