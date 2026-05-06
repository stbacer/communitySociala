package cn.edu.ccst.communitysocialmain.controller;

import cn.edu.ccst.communitysocialmain.service.AliyunOssService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 图片上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/image")
public class ImageController {
    
    @Autowired
    private AliyunOssService aliyunOssService;
    
    @Value("${file.upload.max-size:10485760}") // 10MB
    private long maxFileSize;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 通用文件上传接口
     */
    @PostMapping("/upload")
    public ResultVO<String> uploadFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "type", defaultValue = "other") String fileType,
                                     HttpServletRequest request) {
        // 验证用户身份
        Long userId = getCurrentUserId(request);
        
        // 验证文件
        if (file.isEmpty()) {
            return ResultVO.error("请选择要上传的文件");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResultVO.error("只支持图片文件上传");
        }
        
        // 验证文件大小
        if (file.getSize() > maxFileSize) {
            return ResultVO.error("文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        try {
            String fileUrl = aliyunOssService.uploadFile(file, fileType);
            
            log.info("用户{}上传{}文件成功：{}", userId, fileType, fileUrl);
            
            return ResultVO.success("上传成功", fileUrl);
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ResultVO.error("文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("上传过程中发生错误", e);
            return ResultVO.error("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 头像上传接口
     */
    @PostMapping("/avatar")
    public ResultVO<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        return uploadFile(file, "avatar", request);
    }
    
    /**
     * 帖子图片上传接口
     */
    @PostMapping("/post")
    public ResultVO<String> uploadPostImage(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        return uploadFile(file, "post", request);
    }
    
    /**
     * 从请求头中获取当前用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return Long.parseLong(jwtUtil.getUserIdFromToken(token));
        }
        throw new RuntimeException("用户未登录");
    }

}