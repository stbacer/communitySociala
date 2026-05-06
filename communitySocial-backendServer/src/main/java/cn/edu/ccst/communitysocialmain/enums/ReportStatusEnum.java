package cn.edu.ccst.communitysocialmain.enums;

/**
 * 举报状态枚举
 */
public enum ReportStatusEnum {
    
    PENDING(0, "待处理"),
    HANDLED(1, "已处理"),
    REJECTED(2, "已驳回");
    
    private final Integer code;
    private final String description;
    
    ReportStatusEnum(Integer code, String description) {
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
    public static ReportStatusEnum fromCode(Integer code) {
        for (ReportStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 根据code获取描述
     */
    public static String getDescriptionByCode(Integer code) {
        ReportStatusEnum status = fromCode(code);
        return status != null ? status.getDescription() : "未知状态";
    }
    
    /**
     * 是否为待处理状态
     */
    public static boolean isPending(Integer code) {
        return PENDING.getCode().equals(code);
    }
    
    /**
     * 是否为已处理状态
     */
    public static boolean isHandled(Integer code) {
        return HANDLED.getCode().equals(code);
    }
    
    /**
     * 是否为已驳回状态
     */
    public static boolean isRejected(Integer code) {
        return REJECTED.getCode().equals(code);
    }
}