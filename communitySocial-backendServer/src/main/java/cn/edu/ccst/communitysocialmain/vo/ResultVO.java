package cn.edu.ccst.communitysocialmain.vo;

import lombok.Data;

/**
 * 统一响应结果VO
 */
@Data
public class ResultVO<T> {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public ResultVO() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ResultVO(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }
    
    public ResultVO(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> ResultVO<T> success() {
        return new ResultVO<>(200, "success");
    }
    
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(200, "success", data);
    }
    
    public static <T> ResultVO<T> success(String message, T data) {
        return new ResultVO<>(200, message, data);
    }
    
    /**
     * 失败响应
     */
    public static <T> ResultVO<T> error(Integer code, String message) {
        return new ResultVO<>(code, message);
    }
    
    public static <T> ResultVO<T> error(String message) {
        return new ResultVO<>(500, message);
    }
}