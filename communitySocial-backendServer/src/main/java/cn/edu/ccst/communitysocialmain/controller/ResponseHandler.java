package cn.edu.ccst.communitysocialmain.controller;

import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应处理
 */
@Slf4j
@ControllerAdvice
public class ResponseHandler implements ResponseBodyAdvice<Object> {
    
    @Override
    public boolean supports(MethodParameter returnType, 
                           Class<? extends HttpMessageConverter<?>> converterType) {
        // 只处理返回类型不是ResultVO的方法
        return !returnType.getParameterType().equals(ResultVO.class);
    }
    
    @Override
    public Object beforeBodyWrite(Object body, 
                                 MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request,
                                 ServerHttpResponse response) {
        
        // 如果已经是ResultVO格式，直接返回
        if (body instanceof ResultVO) {
            return body;
        }
        
        // 如果是null，返回成功但无数据
        if (body == null) {
            return ResultVO.success();
        }
        
        // 其他情况包装成ResultVO
        return ResultVO.success(body);
    }
}