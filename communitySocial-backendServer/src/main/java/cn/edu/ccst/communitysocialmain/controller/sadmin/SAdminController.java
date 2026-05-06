package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.dto.SensitiveWordDTO;
import cn.edu.ccst.communitysocialmain.dto.SystemConfigDTO;
import cn.edu.ccst.communitysocialmain.dto.UserCreateDTO;
import cn.edu.ccst.communitysocialmain.dto.UserUpdateDTO;
import cn.edu.ccst.communitysocialmain.dto.ContentReviewDTO;
import cn.edu.ccst.communitysocialmain.dto.PendingContentQueryDTO;
import cn.edu.ccst.communitysocialmain.dto.ResetPasswordDTO;
import cn.edu.ccst.communitysocialmain.dto.AdminResetPasswordDTO;
import cn.edu.ccst.communitysocialmain.entity.Category;

import cn.edu.ccst.communitysocialmain.entity.SensitiveWord;
import cn.edu.ccst.communitysocialmain.entity.SystemConfig;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.CategoryService;
import cn.edu.ccst.communitysocialmain.service.CommentService;
import cn.edu.ccst.communitysocialmain.service.impl.CommentServiceImpl;
import cn.edu.ccst.communitysocialmain.service.ContentReviewService;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.service.SensitiveWordService;
import cn.edu.ccst.communitysocialmain.service.SystemConfigService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PasswordEncoder;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.edu.ccst.communitysocialmain.vo.PostDetailVO;
import cn.edu.ccst.communitysocialmain.vo.CommentDetailVO;
import cn.edu.ccst.communitysocialmain.vo.ConversationVO;
import cn.edu.ccst.communitysocialmain.vo.ConversationDetailVO;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * 超级管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/sadmin")
public class SAdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private SensitiveWordService sensitiveWordService;
    
    @Autowired
    private SystemConfigService systemConfigService;
    

    
    @Autowired
    private ContentReviewService contentReviewService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private cn.edu.ccst.communitysocialmain.service.RegionCacheService regionCacheService;

    /**
     * 检查超级管理员权限
     */
    private void checkSAdminPermission(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        User user = userService.getUserById(userId);
        if (user == null || user.getUserRole() != 3) {
            throw new RuntimeException("权限不足，需要超级管理员权限");
        }
    }

    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return Long.parseLong(jwtUtil.getUserIdFromToken(token));
        }
        throw new RuntimeException("用户未登录");
    }

    // ==================== 用户管理 ====================

    /**
     * 获取省份列表（公开接口，无需权限）
     */
    @GetMapping("/user/provinces")
    public ResultVO<List<Map<String, Object>>> getProvinces(HttpServletRequest request) {
        try {
            // 从 Redis 缓存中获取省份列表
            List<Map<String, Object>> provinces = regionCacheService.getProvinces();
            
            log.info("从缓存获取省份列表成功：{}个省份", provinces.size());
            return ResultVO.success(provinces);
            
        } catch (Exception e) {
            log.error("获取省份列表失败：{}", e.getMessage(), e);
            return ResultVO.error("获取省份列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据省份代码获取城市列表（公开接口，无需权限）
     */
    @GetMapping("/user/cities/{provinceCode}")
    public ResultVO<List<Map<String, Object>>> getCitiesByProvince(@PathVariable String provinceCode, HttpServletRequest request) {
        try {
            // 从 Redis 缓存中获取该省份的城市列表
            List<Map<String, Object>> cities = regionCacheService.getCitiesByProvinceCode(provinceCode);
            
            log.info("从缓存获取 {} 省的城市成功：{}个城市", provinceCode, cities.size());
            return ResultVO.success(cities);
            
        } catch (Exception e) {
            log.error("获取城市列表失败：{}", e.getMessage(), e);
            return ResultVO.error("获取城市列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据城市代码获取区县列表（公开接口，无需权限）
     */
    @GetMapping("/user/districts/{cityCode}")
    public ResultVO<List<Map<String, Object>>> getDistrictsByCity(@PathVariable String cityCode, HttpServletRequest request) {
        try {
            List<Map<String, Object>> districts = regionCacheService.getDistrictsByCityCode(cityCode);
            
            log.info("从缓存获取 {} 市的区县成功：{}个区县", cityCode, districts.size());
            return ResultVO.success(districts);
            
        } catch (Exception e) {
            log.error("获取区县列表失败：{}", e.getMessage(), e);
            return ResultVO.error("获取区县列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取完整的省市区数据
     */
    @GetMapping("/user/regions")
    public ResultVO<Map<String, Object>> getRegions(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            
            Map<String, Object> regions = regionCacheService.getAllRegions();
            
            log.info("从缓存获取省市区数据成功：{}个省份，{}个城市，{}个区县", 
                    ((List<?>) regions.get("provinces")).size(),
                    ((List<?>) regions.get("cities")).size(),
                    ((List<?>) regions.get("districts")).size());
            return ResultVO.success(regions);
            
        } catch (Exception e) {
            log.error("获取省市区数据失败：{}", e.getMessage(), e);
            return ResultVO.error("获取省市区数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取社区管理员所在的省市区数据
     */
    @GetMapping("/user/admin-regions")
    public ResultVO<Map<String, Object>> getAdminRegions(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            
            // 从 MySQL 数据库中查询所有社区管理员(user_role=2)的省市区信息
            List<User> adminUsers = userService.getUsersByRole(2); // 2表示社区管理员
            
            log.info("查询到 {} 个社区管理员", adminUsers.size());
            
            java.util.Set<String> provinceNames = new java.util.HashSet<>();
            java.util.Set<String> cityNames = new java.util.HashSet<>();
            java.util.Set<String> districtNames = new java.util.HashSet<>();
            java.util.Set<String> communities = new java.util.HashSet<>();
            
            for (User user : adminUsers) {
                log.info("社区管理员用户: userId={}, province=[{}], city=[{}], district=[{}], community=[{}]",
                    user.getUserId(), user.getProvince(), user.getCity(), user.getDistrict(), user.getCommunity());
                
                if (user.getProvince() != null && !user.getProvince().isEmpty()) {
                    provinceNames.add(user.getProvince());
                }
                if (user.getCity() != null && !user.getCity().isEmpty() && user.getCity().length() > 1) {
                    cityNames.add(user.getCity());
                }
                if (user.getDistrict() != null && !user.getDistrict().isEmpty()) {
                    districtNames.add(user.getDistrict());
                }
                if (user.getCommunity() != null && !user.getCommunity().isEmpty()) {
                    communities.add(user.getCommunity());
                }
            }
            
            log.info("从数据库提取的唯一值 - 省份: {}, 城市: {}, 区县: {}, 社区: {}",
                provinceNames, cityNames, districtNames, communities);
            
            // 从 Redis 中获取完整的省市区数据，然后过滤出管理员所在的区域
            Map<String, Object> allRegions = regionCacheService.getAllRegions();
            List<Map<String, Object>> allProvinces = (List<Map<String, Object>>) allRegions.get("provinces");
            List<Map<String, Object>> allCities = (List<Map<String, Object>>) allRegions.get("cities");
            List<Map<String, Object>> allDistricts = (List<Map<String, Object>>) allRegions.get("districts");
            
            // 过滤省份（根据名称匹配）
            List<Map<String, Object>> adminProvinces = allProvinces.stream()
                .filter(p -> provinceNames.contains(String.valueOf(p.get("name"))))
                .collect(java.util.stream.Collectors.toList());
            
            // 过滤城市（根据名称匹配）
            List<Map<String, Object>> adminCities = allCities.stream()
                .filter(c -> cityNames.contains(String.valueOf(c.get("name"))))
                .collect(java.util.stream.Collectors.toList());
            
            // 过滤区县（根据名称匹配）
            List<Map<String, Object>> adminDistricts = allDistricts.stream()
                .filter(d -> districtNames.contains(String.valueOf(d.get("name"))))
                .collect(java.util.stream.Collectors.toList());
            
            // 构建返回结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("provinces", adminProvinces);
            result.put("cities", adminCities);
            result.put("districts", adminDistricts);
            result.put("communities", new java.util.ArrayList<>(communities));
            
            log.info("从 MySQL 获取社区管理员区域成功：{}个省份，{}个城市，{}个区县，{}个社区",
                    adminProvinces.size(), adminCities.size(), adminDistricts.size(), communities.size());
            
            return ResultVO.success(result);
            
        } catch (Exception e) {
            log.error("获取社区管理员区域数据失败：{}", e.getMessage(), e);
            return ResultVO.error("获取社区管理员区域数据失败：" + e.getMessage());
        }
    }

    /**
     * 手动刷新省市区数据缓存
     */
    @PostMapping("/user/regions/refresh")
    public ResultVO<String> refreshRegionCache(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            
            log.info("管理员手动刷新省市区数据缓存");
            regionCacheService.refreshCache();
            
            // 验证刷新后的数据
            List<Map<String, Object>> provinces = regionCacheService.getProvinces();
            List<Map<String, Object>> cities = regionCacheService.getCities();
            List<Map<String, Object>> districts = regionCacheService.getDistricts();
            
            log.info("刷新后缓存状态 - 省份: {}个, 城市: {}个, 区县: {}个", 
                    provinces.size(), cities.size(), districts.size());
            
            return ResultVO.success("省市区数据缓存刷新成功");
            
        } catch (Exception e) {
            log.error("刷新省市区数据缓存失败：{}", e.getMessage(), e);
            return ResultVO.error("刷新缓存失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查省市区缓存状态
     */
    @GetMapping("/user/regions/status")
    public ResultVO<Map<String, Object>> checkRegionCacheStatus(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            
            List<Map<String, Object>> provinces = regionCacheService.getProvinces();
            List<Map<String, Object>> cities = regionCacheService.getCities();
            List<Map<String, Object>> districts = regionCacheService.getDistricts();
            
            status.put("provincesCount", provinces.size());
            status.put("citiesCount", cities.size());
            status.put("districtsCount", districts.size());
            status.put("hasData", !provinces.isEmpty() && !cities.isEmpty() && !districts.isEmpty());
            
            if (!provinces.isEmpty()) {
                status.put("sampleProvince", provinces.get(0));
            }
            if (!cities.isEmpty()) {
                status.put("sampleCity", cities.get(0));
            }
            if (!districts.isEmpty()) {
                status.put("sampleDistrict", districts.get(0));
            }
            
            log.info("缓存状态检查 - 省份: {}个, 城市: {}个, 区县: {}个", 
                    provinces.size(), cities.size(), districts.size());
            
            return ResultVO.success(status);
            
        } catch (Exception e) {
            log.error("检查缓存状态失败：{}", e.getMessage(), e);
            return ResultVO.error("检查失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/user/list")
    public ResultVO<PageVO<UserInfoVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        
        checkSAdminPermission(request);
        
        User condition = new User();
        if (nickname != null && !nickname.isEmpty()) {
            condition.setNickname(nickname);
        }
        if (status != null) {
            condition.setStatus(status);
        }
        
        PageVO<UserInfoVO> users = userService.getUsersByCondition(condition, page, size);
        return ResultVO.success(users);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/user/detail/{userId}")
    public ResultVO<UserInfoVO> getUserDetail(@PathVariable Long userId, HttpServletRequest request) {
        try {
            System.out.println("=== 开始获取用户详情，userId: " + userId);
            checkSAdminPermission(request);
            
            User user = userService.getUserById(userId);
            if (user != null) {
                // 将 User 转换为 UserInfoVO
                UserInfoVO userInfoVO = new UserInfoVO();
                userInfoVO.setUserId(user.getUserId());
                userInfoVO.setNickname(user.getNickname());
                userInfoVO.setGender(user.getGender());
                userInfoVO.setPhone(user.getPhone());
                userInfoVO.setAvatarUrl(user.getAvatarUrl());
                userInfoVO.setUserRole(user.getUserRole());
                userInfoVO.setStatus(user.getStatus());
                userInfoVO.setAuthStatus(user.getAuthStatus());
                userInfoVO.setCommunity(user.getCommunity());
                userInfoVO.setCreateTime(user.getCreateTime());
                userInfoVO.setLastLoginTime(user.getLastLoginTime());
                
                System.out.println("获取用户详情成功：" + user.getUserId());
                return ResultVO.success(userInfoVO);
            } else {
                System.out.println("用户不存在：" + userId);
                return ResultVO.error("用户不存在");
            }
        } catch (Exception e) {
            System.err.println("获取用户详情失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping("/user/create")
    public ResultVO<String> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO,
                                     HttpServletRequest request) {
        checkSAdminPermission(request);
        
        String userId = userService.createUserByAdmin(userCreateDTO);
        return ResultVO.success("用户创建成功", userId);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/user/update/{userId}")
    public ResultVO<UserInfoVO> updateUser(@PathVariable Long userId,
                                         @Valid @RequestBody UserUpdateDTO userUpdateDTO,
                                         HttpServletRequest request) {
        checkSAdminPermission(request);
        
        UserInfoVO updatedUser = userService.updateUserByAdmin(userId, userUpdateDTO);
        return ResultVO.success("用户更新成功", updatedUser);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/delete/{userId}")
    public ResultVO<Void> deleteUser(@PathVariable Long userId,
                                   HttpServletRequest request) {
        checkSAdminPermission(request);
        
        userService.deleteUser(userId);
        return ResultVO.success("用户删除成功", null);
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/user/status/{userId}")
    public ResultVO<Void> updateUserStatus(@PathVariable Long userId,
                                         @RequestParam Integer status,
                                         HttpServletRequest request) {
        checkSAdminPermission(request);
        
        userService.updateUserStatus(userId, status);
        return ResultVO.success("用户状态更新成功", null);
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/user/reset-password/{userId}")
    public ResultVO<Void> resetUserPassword(@PathVariable Long userId,
                                          @Valid @RequestBody AdminResetPasswordDTO resetPasswordDTO,
                                          HttpServletRequest request) {
        checkSAdminPermission(request);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResultVO.error("用户不存在");
        }
        
        // 更新密码（使用 BCrypt 加密）
        user.setPassword(PasswordEncoder.encrypt(resetPasswordDTO.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            return ResultVO.error("重置密码失败");
        }
        
        return ResultVO.success("密码重置成功", null);
    }

    /**
     * 获取分类列表
     */
    @GetMapping("/category/list")
    public ResultVO<List<Category>> getCategoryList(HttpServletRequest request) {
        checkSAdminPermission(request);
        
        List<Category> categories = categoryService.getAllCategories();
        return ResultVO.success(categories);
    }

    /**
     * 创建分类
     */
    @PostMapping("/category/create")
    public ResultVO<Category> createCategory(@Valid @RequestBody Category category,
                                           HttpServletRequest request) {
        checkSAdminPermission(request);
        
        Category createdCategory = categoryService.createCategory(category);
        return ResultVO.success("分类创建成功", createdCategory);
    }

    /**
     * 更新分类
     */
    @PutMapping("/category/update/{categoryId}")
    public ResultVO<Category> updateCategory(@PathVariable Integer categoryId,
                                           @Valid @RequestBody Category category,
                                           HttpServletRequest request) {
        checkSAdminPermission(request);
        
        category.setCategoryId(categoryId);
        Category updatedCategory = categoryService.updateCategory(category);
        return ResultVO.success("分类更新成功", updatedCategory);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/category/delete/{categoryId}")
    public ResultVO<Void> deleteCategory(@PathVariable Integer categoryId,
                                       HttpServletRequest request) {
        checkSAdminPermission(request);
        
        categoryService.deleteCategory(categoryId);
        return ResultVO.success("分类删除成功", null);
    }


    /**
     * 获取敏感词列表
     */
    @GetMapping("/sensitive-word/list")
    public ResultVO<PageVO<SensitiveWord>> getSensitiveWordList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        
        checkSAdminPermission(request);
        
        // 将字符串状态转换为Integer
        Integer statusInt = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusInt = Integer.parseInt(status);
            } catch (NumberFormatException e) {
            }
        }
        
        PageVO<SensitiveWord> words = sensitiveWordService.getSensitiveWords(page, size, keyword, type, statusInt);
        return ResultVO.success(words);
    }

    /**
     * 添加敏感词
     */
    @PostMapping("/sensitive-word/create")
    public ResultVO<SensitiveWord> createSensitiveWord(@Valid @RequestBody SensitiveWordDTO sensitiveWordDTO,
                                                     HttpServletRequest request) {
        checkSAdminPermission(request);
        
        SensitiveWord word = sensitiveWordService.createSensitiveWord(sensitiveWordDTO);
        return ResultVO.success("敏感词添加成功", word);
    }

    /**
     * 更新敏感词
     */
    @PutMapping("/sensitive-word/update/{wordId}")
    public ResultVO<SensitiveWord> updateSensitiveWord(@PathVariable Integer wordId,
                                                     @Valid @RequestBody SensitiveWordDTO sensitiveWordDTO,
                                                     HttpServletRequest request) {
        checkSAdminPermission(request);
        
        SensitiveWord updatedWord = sensitiveWordService.updateSensitiveWord(wordId, sensitiveWordDTO);
        return ResultVO.success("敏感词更新成功", updatedWord);
    }

    /**
     * 删除敏感词（物理删除）
     */
    @DeleteMapping("/sensitive-word/delete/{wordId}")
    public ResultVO<Void> deleteSensitiveWord(@PathVariable Integer wordId,
                                            HttpServletRequest request) {
        checkSAdminPermission(request);
        
        sensitiveWordService.hardDeleteSensitiveWord(wordId);
        return ResultVO.success("敏感词删除成功", null);
    }

    /**
     * 批量删除敏感词
     */
    @DeleteMapping("/sensitive-word/batch-delete")
    public ResultVO<Void> batchDeleteSensitiveWords(@RequestBody List<Integer> wordIds,
                                                  HttpServletRequest request) {
        checkSAdminPermission(request);
        
        sensitiveWordService.batchDeleteSensitiveWords(wordIds);
        return ResultVO.success("批量删除敏感词成功", null);
    }

    /**
     * 获取系统配置列表
     */
    @GetMapping("/config/list")
    public ResultVO<List<SystemConfig>> getConfigList(HttpServletRequest request) {
        checkSAdminPermission(request);
        
        List<SystemConfig> configs = systemConfigService.getAllConfigs();
        return ResultVO.success(configs);
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/config/update")
    public ResultVO<SystemConfig> updateConfig(@Valid @RequestBody SystemConfigDTO configDTO,
                                             HttpServletRequest request) {
        checkSAdminPermission(request);
        
        SystemConfig updatedConfig = systemConfigService.updateConfig(configDTO);
        return ResultVO.success("配置更新成功", updatedConfig);
    }

    /**
     * 重置配置为默认值
     */
    @PutMapping("/config/reset/{configKey}")
    public ResultVO<Void> resetConfig(@PathVariable String configKey,
                                    HttpServletRequest request) {
        checkSAdminPermission(request);
        
        systemConfigService.resetConfig(configKey);
        return ResultVO.success("配置重置成功", null);
    }

    /**
     * 获取待审核内容列表
     */
    @GetMapping("/content/pending-list")
    public ResultVO<PageVO<ContentReviewService.PendingContentVO>> getPendingContentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
            
        checkSAdminPermission(request);
            
        PendingContentQueryDTO queryDTO = new PendingContentQueryDTO();
        queryDTO.setPage(page);
        queryDTO.setSize(size);
        queryDTO.setContentType(contentType);
        queryDTO.setKeyword(keyword);
        queryDTO.setStartTime(startTime);
        queryDTO.setEndTime(endTime);
            
        PageVO<ContentReviewService.PendingContentVO> pendingContent = contentReviewService.getPendingContentList(queryDTO);
        return ResultVO.success(pendingContent);
    }
        
    /**
     * 审核单个内容
     */
    @PutMapping("/content/review")
    public ResultVO<Void> reviewContent(@Valid @RequestBody ContentReviewDTO reviewDTO,
                                      HttpServletRequest request) {
        checkSAdminPermission(request);
            
        Long adminUserId = getCurrentUserId(request);
        contentReviewService.reviewContent(adminUserId.toString(), reviewDTO);
        return ResultVO.success("审核完成", null);
    }
        
    /**
     * 批量审核内容
     */
    @PutMapping("/content/batch-review")
    public ResultVO<Void> batchReviewContent(@Valid @RequestBody List<ContentReviewDTO> reviewDTOs,
                                           HttpServletRequest request) {
        checkSAdminPermission(request);
            
        Long adminUserId = getCurrentUserId(request);
        contentReviewService.batchReviewContent(adminUserId.toString(), reviewDTOs);
        return ResultVO.success("批量审核完成", null);
    }
        
    /**
     * 获取审核统计信息
     */
    @GetMapping("/content/statistics")
    public ResultVO<ContentReviewService.ContentStatistics> getReviewStatistics(HttpServletRequest request) {
        checkSAdminPermission(request);
            
        ContentReviewService.ContentStatistics statistics = contentReviewService.getReviewStatistics();
        return ResultVO.success(statistics);
    }
    
    /**
     * 获取所有帖子列表
     */
    @GetMapping("/content/all-posts")
    public ResultVO<PageVO<PostDetailVO>> getAllPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkSAdminPermission(request);
            
        PageVO<PostDetailVO> posts = postService.getPostsByConditions(page, size, null, keyword, null);
        return ResultVO.success(posts);
    }
    
    /**
     * 获取所有评论列表
     */
    @GetMapping("/content/all-comments")
    public ResultVO<PageVO<CommentDetailVO>> getAllComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkSAdminPermission(request);
            
        PageVO<CommentDetailVO> comments = commentService.getAllComments(page, size, keyword);
        return ResultVO.success(comments);
    }
    
    /**
     * 获取所有私信列表
     */
    @GetMapping("/content/all-messages")
    public ResultVO<PageVO<ConversationVO>> getAllMessages(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        checkSAdminPermission(request);
            
        PageVO<ConversationVO> messages = messageService.getAllConversations(page, size, keyword);
        return ResultVO.success(messages);
    }
    
    /**
     * 获取帖子详情
     */
    @GetMapping("/content/post/{postId}")
    public ResultVO<PostDetailVO> getPostDetail(@PathVariable Long postId, HttpServletRequest request) {
        checkSAdminPermission(request);
            
        PostDetailVO post = postService.getPostDetail(postId, null);
        return ResultVO.success(post);
    }
    
    /**
     * 获取评论详情
     */
    @GetMapping("/content/comment/{commentId}")
    public ResultVO<CommentDetailVO> getCommentDetail(@PathVariable Long commentId, HttpServletRequest request) {
        checkSAdminPermission(request);
            
        CommentDetailVO comment = ((CommentServiceImpl) commentService).getCommentDetailForAdmin(commentId);
        return ResultVO.success(comment);
    }
    
    /**
     * 获取会话详情
     */
    @GetMapping("/content/message/{conversationId}")
    public ResultVO<ConversationDetailVO> getMessageDetail(
            @PathVariable String conversationId, 
            HttpServletRequest request) {
        checkSAdminPermission(request);
            
        ConversationDetailVO detail = messageService.getConversationDetail(conversationId);
        return ResultVO.success(detail);
    }
}