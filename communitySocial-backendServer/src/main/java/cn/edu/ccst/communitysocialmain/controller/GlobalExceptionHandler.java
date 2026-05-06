package cn.edu.ccst.communitysocialmain.controller;

import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResultVO<?> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: ", e);
        return ResultVO.error(e.getMessage());
    }
    
    /**
     * 处理参数校验异常（@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMsg = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败: {}", errorMsg);
        return ResultVO.error(400, "参数校验失败: " + errorMsg);
    }
    
    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResultVO<?> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMsg = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败: {}", errorMsg);
        return ResultVO.error(400, "参数绑定失败: " + errorMsg);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVO<?> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMsg = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("约束违反: {}", errorMsg);
        return ResultVO.error(400, "参数错误: " + errorMsg);
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResultVO<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResultVO.error(500, "系统内部错误");
    }
}