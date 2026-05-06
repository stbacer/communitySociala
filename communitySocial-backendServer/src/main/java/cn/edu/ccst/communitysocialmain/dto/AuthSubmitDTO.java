package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;
import javax.validation.constraints.NotEmpty;


/**
 * 实名认证提交DTO
 */
@Data
public class AuthSubmitDTO {
    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;
    
    /**
     * 身份证号
     */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$", 
             message = "身份证号格式不正确")
    private String idCard;
    
    /**
     * 所在社区
     */
    @NotBlank(message = "所在社区不能为空")
    @Size(max = 100, message = "社区名称长度不能超过 100 个字符")
  private String community;
    
    /**
     * 所在省份
     */
    @NotBlank(message = "所在省份不能为空")
    @Size(max = 50, message = "省份名称长度不能超过 50 个字符")
  private String province;
    
    /**
     * 所在城市
     */
    @NotBlank(message = "所在城市不能为空")
    @Size(max = 50, message = "城市名称长度不能超过 50 个字符")
  private String city;
    
    /**
     * 所在区县
     */
    @NotBlank(message = "所在区县不能为空")
    @Size(max = 50, message = "区县名称长度不能超过 50 个字符")
  private String district;
    
    /**
     * 身份证明图片URL列表
     */
    @NotEmpty(message = "请上传至少一张身份证明图片")
    private List<String> identityImages;
}