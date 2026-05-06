package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 版块分类实体类
 */
@Data
public class Category {
    /**
     * 分类ID
     */
    private Integer categoryId;
    
    /**
     * 分类名称
     */
    private String name;
    
    /**
     * 分类描述
     */
    private String description;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态：0禁用，1启用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    

}