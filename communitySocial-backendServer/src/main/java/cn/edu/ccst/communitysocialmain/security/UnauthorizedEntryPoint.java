package cn.edu.ccst.communitysocialmain.security;

import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 未授权访问入口点
 */
@Slf4j
@Component
public class UnauthorizedEntryPoint implements AuthenticationEntryPoint {
    
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        log.warn("未授权访问: {} - {}", request.getRequestURI(), authException.getMessage());
        
        // 设置响应头
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 构建错误响应
        ResultVO<?> result = ResultVO.error(401, "未授权访问，请先登录");
        
        // 写入响应
        response.getWriter().write(JSON.toJSONString(result));
    }
}