package cn.edu.ccst.communitysocialmain.enums;

/**
 * 举报类型枚举
 */
public enum ReportTypeEnum {
    
    POST(1, "帖子"),
    COMMENT(2, "评论"),
    USER(3, "用户");
    
    private final Integer code;
    private final String description;
    
    ReportTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据code获取枚举
     */
    public static ReportTypeEnum fromCode(Integer code) {
        for (ReportTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 根据code获取描述
     */
    public static String getDescriptionByCode(Integer code) {
        ReportTypeEnum type = fromCode(code);
        return type != null ? type.getDescription() : "未知类型";
    }
}