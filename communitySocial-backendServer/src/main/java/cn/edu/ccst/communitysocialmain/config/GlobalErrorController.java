package cn.edu.ccst.communitysocialmain.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import cn.edu.ccst.communitysocialmain.vo.ResultVO;

/**
 * 全局错误处理控制器
 */
@Slf4j
@Controller
public class GlobalErrorController implements ErrorController {

    @RequestMapping("/error")
    @ResponseBody
    public ResultVO<String> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            log.error("Error occurred - Status: {}, Message: {}, URI: {}", 
                     statusCode, message, requestUri);
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return ResultVO.error("资源不存在: " + requestUri);
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return ResultVO.error("服务器内部错误");
            } else {
                return ResultVO.error("请求失败: " + statusCode);
            }
        }
        
        return ResultVO.error("未知错误");
    }
}