package cn.edu.ccst.communitysocialmain.controller.admin;

import cn.edu.ccst.communitysocialmain.dto.AuthSubmitDTO;
import cn.edu.ccst.communitysocialmain.dto.ReviewDTO;
import cn.edu.ccst.communitysocialmain.dto.UserLoginDTO;
import cn.edu.ccst.communitysocialmain.dto.UserRegisterDTO;
import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.CategoryService;
import cn.edu.ccst.communitysocialmain.service.DataStatisticsService;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostDetailVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 管理员端控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private DataStatisticsService dataStatisticsService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private MessageService messageService;
    
    /**
     * 管理员登录
     */
    @PostMapping("/auth/login")
    public ResultVO<String> adminLogin(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            // 1. 校验验证码
            if (loginDTO.getCaptchaId() == null || loginDTO.getCaptchaCode() == null) {
                return ResultVO.error("验证码不能为空");
            }
            
            String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + loginDTO.getCaptchaId());
            if (storedCaptcha == null) {
                return ResultVO.error("验证码已过期，请重新获取");
            }
            
            if (!storedCaptcha.equalsIgnoreCase(loginDTO.getCaptchaCode())) {
                return ResultVO.error("验证码错误");
            }
            
            // 2. 删除已使用的验证码
            redisTemplate.delete("captcha:" + loginDTO.getCaptchaId());
            
            // 3. 先进行普通登录验证
            String token = userService.login(loginDTO);
            
            // 4. 验证用户是否为管理员
            String userIdStr = jwtUtil.getUserIdFromToken(token);
            Long userId = Long.parseLong(userIdStr);
            User user = userService.getUserById(userId);
            
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 5. 检查用户角色是否为管理员或超级管理员
            if (user.getUserRole() != 2 && user.getUserRole() != 3) {
                throw new RuntimeException("权限不足，仅管理员可登录");
            }
            
            return ResultVO.success("登录成功", token);
        } catch (Exception e) {
            log.error("管理员登录失败", e);
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 社区管理员注册
     */
    @PostMapping("/auth/register")
    public ResultVO<Void> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        try {
            // 1. 校验验证码
            if (registerDTO.getCaptchaId() == null || registerDTO.getCaptchaCode() == null) {
                return ResultVO.error("验证码不能为空");
            }
            
            String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + registerDTO.getCaptchaId());
            if (storedCaptcha == null) {
                return ResultVO.error("验证码已过期，请重新获取");
            }
            
            // 不区分大小写比较验证码
            if (!storedCaptcha.equalsIgnoreCase(registerDTO.getCaptchaCode())) {
                return ResultVO.error("验证码错误");
            }
            
            // 2. 删除已使用的验证码
            redisTemplate.delete("captcha:" + registerDTO.getCaptchaId());
            
            // 3. 调用 UserService 的注册方法
            userService.registerAdmin(registerDTO);
            return ResultVO.success("注册成功，请等待审核", null);
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            log.error("注册失败", e);
            throw new RuntimeException("注册失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current-user")
    public ResultVO<UserInfoVO> getCurrentUser(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserInfoVO userInfo = userService.getUserInfo(userId);
        return ResultVO.success(userInfo);
    }
    
    /**
     * 获取分类列表
     */
    @GetMapping("/category/list")
    public ResultVO<List<Category>> getCategoryList() {
        try {
            List<Category> categories = categoryService.getEnabledCategories();
            return ResultVO.success(categories);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return ResultVO.error("获取分类列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 社区管理员提交实名认证
     */
    @PostMapping("/user/auth-submit")
    public ResultVO<Void> submitAuth(@Valid @RequestBody AuthSubmitDTO authSubmitDTO,
                                    HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            
            // 验证用户角色是否为社区管理员
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResultVO.error("用户不存在");
            }
            
            if (user.getUserRole() != 2) {
                return ResultVO.error("仅社区管理员可进行实名认证");
            }
            
            // 调用服务层提交认证
            userService.submitAuth(userId, authSubmitDTO);
            
            // 发送系统消息通知
            String messageContent = "您的社区管理员实名认证申请已提交，请耐心等待超级管理员审核。";
            messageService.sendSystemMessage(userId, messageContent, 1);
            
            log.info("社区管理员{}提交实名认证申请", userId);
            return ResultVO.success("认证申请提交成功", null);
            
        } catch (Exception e) {
            log.error("社区管理员提交实名认证失败", e);
            return ResultVO.error("提交失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取待审核的实名认证用户列表
     */
    @GetMapping("/user/pending-auth")
    public ResultVO<PageVO<UserInfoVO>> getPendingAuthUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        try {
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            PageVO<UserInfoVO> users;
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                // 社区管理员只能看到本社区的待审核用户
                users = userService.getPendingAuthUsersByCommunity(adminUser.getCommunity(), page, size);
                log.info("社区管理员{}查看本社区{}的待审核用户", adminUserId, adminUser.getCommunity());
            } else {
                // 超级管理员可以看到所有待审核用户
                users = userService.getPendingAuthUsers(page, size);
                log.info("超级管理员{}查看所有待审核用户", adminUserId);
            }
            
            return ResultVO.success(users);
        } catch (Exception e) {
            log.error("获取待审核用户列表失败", e);
            return ResultVO.error("获取待审核用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取待审核的管理员账号列表
     */
    @GetMapping("/user/pending-admins")
    public ResultVO<List<UserInfoVO>> getPendingAdmins(
            @RequestParam(required = false) Integer userRole,
            @RequestParam(required = false) Integer authStatus,
            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            // 调用 UserService 获取待审核管理员列表
            List<UserInfoVO> admins = userService.getPendingAdmins(userRole, authStatus);
            return ResultVO.success(admins);
        } catch (Exception e) {
            log.error("获取待审核管理员列表失败", e);
            return ResultVO.error("获取待审核管理员列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 通过管理员审核
     */
    @PutMapping("/user/approve-admin/{userId}")
    public ResultVO<Void> approveAdmin(@PathVariable Long userId,
                                      HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Long adminUserId = getCurrentUserId(request);
            userService.approveAdmin(userId, adminUserId);
            return ResultVO.success("审核通过", null);
        } catch (Exception e) {
            log.error("通过管理员审核失败", e);
            return ResultVO.error("通过管理员审核失败：" + e.getMessage());
        }
    }
    
    /**
     * 拒绝管理员审核
     */
    @PutMapping("/user/reject-admin/{userId}")
    public ResultVO<Void> rejectAdmin(@PathVariable Long userId,
                                     @RequestBody(required = false) Map<String, String> body,
                                     HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Long adminUserId = getCurrentUserId(request);
            String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "";
            userService.rejectAdmin(userId, adminUserId, reason);
            return ResultVO.success("已拒绝", null);
        } catch (Exception e) {
            log.error("拒绝管理员审核失败", e);
            return ResultVO.error("拒绝管理员审核失败：" + e.getMessage());
        }
    }
    
    /**
     * 审核实名认证
     */
    @PutMapping("/user/auth-review")
    public ResultVO<Void> reviewAuth(@Valid @RequestBody ReviewDTO reviewDTO,
                                   HttpServletRequest request) {
        checkAdminPermission(request);
        Long adminUserId = getCurrentUserId(request);
        userService.reviewAuth(adminUserId, reviewDTO);
        return ResultVO.success("审核完成", null);
    }
    
    /**
     * 获取待审核帖子列表
     */
    @GetMapping("/post/pending")
    public ResultVO<PageVO<PostDetailVO>> getPendingPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        try {
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            PageVO<PostDetailVO> posts;
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                posts = postService.getPendingPostsByCommunity(adminUser.getCommunity(), page, size, adminUserId);
                log.info("社区管理员{}查看本社区{}的待审核帖子", adminUserId, adminUser.getCommunity());
            } else {
                posts = postService.getPendingPosts(page, size);
            }
            
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取待审核帖子列表失败", e);
            return ResultVO.error("获取待审核帖子列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据状态获取帖子列表
     */
    @GetMapping("/posts/list")
    public ResultVO<PageVO<PostDetailVO>> getPostsByStatus(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            HttpServletRequest request) {
        try {
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            PageVO<PostDetailVO> posts;
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                posts = postService.getPostsByConditionsWithCommunity(page, size, status, keyword, categoryId, adminUser.getCommunity());
                log.info("社区管理员{}查看本社区{}的帖子", adminUserId, adminUser.getCommunity());
            } else {
                posts = postService.getPostsByConditions(page, size, status, keyword, categoryId);
                log.info("超级管理员{}查看所有帖子", adminUserId);
            }
            
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取帖子列表失败", e);
            return ResultVO.error("获取帖子列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取帖子统计信息
     */
    @GetMapping("/posts/statistics")
    public ResultVO<Map<String, Object>> getPostStatistics(HttpServletRequest request) {
        checkAdminPermission(request);
        Map<String, Object> statistics = postService.getPostStatistics();
        return ResultVO.success(statistics);
    }
    
    /**
     * 获取帖子审核历史
     */
    @GetMapping("/posts/{postId}/review-history")
    public ResultVO<List<Map<String, Object>>> getReviewHistory(@PathVariable Long postId,
                                                               HttpServletRequest request) {
        checkAdminPermission(request);
        List<Map<String, Object>> history = postService.getReviewHistory(postId);
        return ResultVO.success(history);
    }
    
    /**
     * 审核帖子
     */
    @PutMapping("/post/review")
    public ResultVO<Void> reviewPost(@Valid @RequestBody ReviewDTO reviewDTO,
                                   HttpServletRequest request) {
        checkAdminPermission(request);
        Long adminUserId = getCurrentUserId(request);
        postService.reviewPost(adminUserId, reviewDTO);
        return ResultVO.success("审核完成", null);
    }
    
    /**
     * 置顶帖子
     */
    @PutMapping("/post/top/{postId}")
    public ResultVO<Void> topPost(@PathVariable Long postId,
                                HttpServletRequest request) {
        checkAdminPermission(request);
        postService.topPost(postId);
        return ResultVO.success("置顶成功", null);
    }
    
    /**
     * 删除帖子
     */
    @DeleteMapping("/post/{postId}")
    public ResultVO<Void> deletePost(@PathVariable Long postId,
                                   HttpServletRequest request) {
        checkAdminPermission(request);
        Long adminUserId = getCurrentUserId(request);
        postService.deletePost(postId, adminUserId);
        return ResultVO.success("删除成功", null);
    }
    
    /**
     * 获取管理员首页统计数据
     */
    @GetMapping("/dashboard/stats")
    public ResultVO<DashboardStatsVO> getDashboardStats(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
                
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
                
            DashboardStatsVO stats = new DashboardStatsVO();
                
            // 判断是否为社区管理员
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                // 社区管理员：获取本社区的统计数据
                String community = adminUser.getCommunity();
                log.info("社区管理员{}查看本社区{}的首页统计数据", adminUserId, community);
                
                // 待审核用户数（本社区）
                Long pendingUsers = userService.getPendingAuthUsersByCommunity(community, 1, 1000).getTotal();
                stats.setPendingUsers(pendingUsers);
                    
                // 待审核帖子数（本社区）
                Long pendingPosts = postService.getPendingPostsByCommunity(community, 1, 1000, adminUserId).getTotal();
                stats.setPendingPosts(pendingPosts);
                    
                // 总用户数（本社区）
                Long totalUsers = userMapper.countUsersByCommunity(community);
                stats.setTotalUsers(totalUsers);
                    
                // 今日活跃用户数（本社区）
                LocalDateTime todayStart = LocalDate.now().atStartOfDay();
                LocalDateTime todayEnd = todayStart.plusDays(1);
                Long todayActiveUsers = countActiveUsersByCommunityAndTimeRange(community, todayStart, todayEnd);
                stats.setTodayActiveUsers(todayActiveUsers);
                    
                // 今日新增帖子（本社区）
                Long todayPosts = postService.countPostsByCommunityAndTimeRange(community, todayStart, todayEnd);
                stats.setTodayPosts(todayPosts);
                    
                // 总帖子数（本社区）
                Long totalPosts = postService.countPostsByCommunity(community, adminUserId);
                stats.setTotalPosts(totalPosts);
                    
            } else {
                // 超级管理员：获取平台总览数据
                log.info("超级管理员{}查看平台首页统计数据", adminUserId);
                    
                DataStatisticsService.PlatformOverviewVO platformOverview = dataStatisticsService.getPlatformOverview();
                    
                // 待审核用户数（全平台）
                Long pendingUsers = userService.getPendingAuthUsers(1, 1000).getTotal();
                stats.setPendingUsers(pendingUsers);
                    
                // 待审核帖子数（全平台）
                Long pendingPosts = postService.getPendingPosts(1, 1000).getTotal();
                stats.setPendingPosts(pendingPosts);
                    
                // 平台总览数据
                stats.setTotalUsers(platformOverview.getTotalUsers());
                stats.setTotalPosts(platformOverview.getTotalPosts());
                stats.setTodayActiveUsers(platformOverview.getTodayActiveUsers());
                stats.setTodayPosts(platformOverview.getTodayPosts());
            }
                
            return ResultVO.success("获取统计数据成功", stats);
                
        } catch (Exception e) {
            log.error("获取管理员首页统计数据失败", e);
            return ResultVO.error("获取统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取分类统计（带社区过滤）
     */
    @GetMapping("/statistics/category")
    public ResultVO<DataStatisticsService.CategoryStatisticsVO> getCategoryStatistics(
            @RequestParam(required = false) String timeRange,
            HttpServletRequest request) {
        try {
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            // 解析时间范围参数
            Integer days = parseTimeRange(timeRange);
            
            DataStatisticsService.CategoryStatisticsVO stats;
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                // 社区管理员只能看到本社区的分类统计
                stats = dataStatisticsService.getCategoryStatisticsByTimeRangeAndCommunity(days, adminUser.getCommunity());
                log.info("社区管理员{}查看本社区{}的分类统计数据", adminUserId, adminUser.getCommunity());
            } else {
                // 超级管理员可以看到所有分类统计
                stats = dataStatisticsService.getCategoryStatisticsByTimeRange(days);
                log.info("超级管理员{}查看分类统计数据", adminUserId);
            }
            
            return ResultVO.success("获取分类统计成功", stats);
        } catch (Exception e) {
            log.error("获取分类统计失败", e);
            return ResultVO.error("获取分类统计失败：" + e.getMessage());
        }
    }
        
    /**
     * 获取社区统计数据（社区管理员用）
     */
    @GetMapping("/statistics/community")
    public ResultVO<CommunityStatisticsVO> getCommunityStatistics(HttpServletRequest request) {
        try {
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            if (adminUser == null || adminUser.getUserRole() != 2 || adminUser.getCommunity() == null) {
                return ResultVO.error("只有社区管理员可以访问此接口");
            }
            
            String community = adminUser.getCommunity();
            log.info("社区管理员{}查看本社区{}的统计数据", adminUserId, community);
            
            // 构建社区统计数据
            CommunityStatisticsVO stats = new CommunityStatisticsVO();
            
            // 1. 总用户数（该社区的所有用户）- 使用 Mapper 直接查询
            Long totalUsers = userMapper.countUsersByCommunity(community);
            stats.setTotalUsers(totalUsers);
            
            // 2. 总发帖量（该社区用户发布的所有帖子）
            Long totalPosts = postService.countPostsByCommunity(community, adminUserId);
            stats.setTotalPosts(totalPosts);
            
            // 3. 待审核帖子数
            Long pendingPosts = postService.getPendingPostsByCommunity(community, 1, 1000, adminUserId).getTotal();
            stats.setPendingPosts(pendingPosts);
            
            // 4. 待审核用户数
            Long pendingUsers = userService.getPendingAuthUsersByCommunity(community, 1, 1000).getTotal();
            stats.setPendingUsers(pendingUsers);
            
            // 5. 今日新增帖子
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = todayStart.plusDays(1);
            Long todayPosts = postService.countPostsByCommunityAndTimeRange(community, todayStart, todayEnd);
            stats.setTodayPosts(todayPosts);
            
            // 6. 今日新增用户
            Long todayNewUsers = userMapper.countNewUsersByCommunityAndTimeRange(community, todayStart, todayEnd);
            stats.setTodayNewUsers(todayNewUsers);
            
            // 7. 今日活跃用户数（DAU）- 统计今天有登录、发帖、评论、点赞、收藏等行为的用户
            LocalDateTime yesterdayStart = todayStart.minusDays(1);
            LocalDateTime yesterdayEnd = todayStart;
            
            Long todayActiveUsers = countActiveUsersByCommunityAndTimeRange(community, todayStart, todayEnd);
            stats.setTodayActiveUsers(todayActiveUsers);
            
            Long yesterdayActiveUsers = countActiveUsersByCommunityAndTimeRange(community, yesterdayStart, yesterdayEnd);
            stats.setYesterdayActiveUsers(yesterdayActiveUsers);
            
            // 计算 DAU 增长率
            if (yesterdayActiveUsers != null && yesterdayActiveUsers > 0) {
                Double dauGrowthRate = ((double)(todayActiveUsers - yesterdayActiveUsers) / yesterdayActiveUsers) * 100;
                stats.setDauGrowthRate(dauGrowthRate);
            } else {
                stats.setDauGrowthRate(0.0);
            }
            
            // 8. 举报统计（暂时用 0 填充）
            stats.setTotalReports(0L);
            stats.setPendingReports(0L);
            
            return ResultVO.success("获取社区统计数据成功", stats);
            
        } catch (Exception e) {
            log.error("获取社区统计数据失败", e);
            return ResultVO.error("获取社区统计数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户发帖量排名
     */
    @GetMapping("/statistics/user-posts")
    public ResultVO<List<UserPostRankingVO>> getUserPostRankings(
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            if (adminUser == null || adminUser.getUserRole() != 2 || adminUser.getCommunity() == null) {
                return ResultVO.error("只有社区管理员可以访问此接口");
            }
            
            String community = adminUser.getCommunity();
            
            // 获取该社区的用户列表 - 通过 Mapper 查询
            List<User> users = userMapper.selectByCommunity(community);
            
            // 构建用户发帖排名
            List<UserPostRankingVO> rankings = new ArrayList<>();
            for (User user : users) {
                UserPostRankingVO ranking = new UserPostRankingVO();
                ranking.setUserId(user.getUserId());
                ranking.setUserName(user.getNickname());
                ranking.setAvatar(user.getAvatarUrl());
                
                // 统计该用户的发帖量
                Long postCount = postService.countPostsByUserId(user.getUserId());
                ranking.setPostCount(postCount);
                
                rankings.add(ranking);
            }
            
            // 按发帖量排序
            rankings.sort((a, b) -> b.getPostCount().compareTo(a.getPostCount()));
            
            // 限制返回数量
            if (rankings.size() > limit) {
                rankings = rankings.subList(0, limit);
            }
            
            return ResultVO.success("获取用户发帖排名成功", rankings);
            
        } catch (Exception e) {
            log.error("获取用户发帖排名失败", e);
            return ResultVO.error("获取用户发帖排名失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析时间范围参数
     */
    private Integer parseTimeRange(String timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return 7; // 默认一周
        }
                
        switch (timeRange.toLowerCase()) {
            case "today":
                return 1;
            case "week":
                return 7;
            case "month":
                return 30;
            default:
                return 7;
        }
    }
    
    /**
     * 获取近 N 日的活跃用户趋势数据
     */
    @GetMapping("/statistics/dau-trend")
    public ResultVO<List<Map<String, Object>>> getDauTrend(
            @RequestParam(defaultValue = "7") Integer days,
            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            // 获取当前管理员信息
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
            
            if (adminUser == null || adminUser.getUserRole() != 2 || adminUser.getCommunity() == null) {
                return ResultVO.error("只有社区管理员可以访问此接口");
            }
            
            String community = adminUser.getCommunity();
            List<Map<String, Object>> dauTrend = new ArrayList<>();
            
            LocalDateTime endDate = LocalDate.now().atStartOfDay();
            LocalDateTime startDate = endDate.minusDays(days - 1);
            
            // 循环获取每天的活跃用户数
            for (int i = 0; i < days; i++) {
                LocalDateTime dayStart = startDate.plusDays(i);
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                Long activeUsers = countActiveUsersByCommunityAndTimeRange(community, dayStart, dayEnd);
                
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", dayStart.toLocalDate().toString());
                dataPoint.put("activeUsers", activeUsers);
                
                dauTrend.add(dataPoint);
            }
            
            return ResultVO.success("获取 DAU 趋势数据成功", dauTrend);
            
        } catch (Exception e) {
            log.error("获取 DAU 趋势数据失败", e);
            return ResultVO.error("获取 DAU 趋势数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 统计社区在指定时间范围内的活跃用户数（有登录、发帖、评论、点赞、收藏等行为的用户）
     */
    private Long countActiveUsersByCommunityAndTimeRange(String community, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 使用 Set 合并多个数据源，去重后统计
            java.util.Set<Long> activeUserIds = new java.util.HashSet<>();
            
            // 1. 获取该社区的所有用户
            List<User> communityUsers = userMapper.selectByCommunity(community);
            if (communityUsers == null || communityUsers.isEmpty()) {
                return 0L;
            }
            
            java.util.Set<Long> communityUserIds = new java.util.HashSet<>();
            for (User user : communityUsers) {
                communityUserIds.add(user.getUserId());
            }
            
            // 2. 统计时间范围内发帖的用户
            List<Post> timeRangePosts = postMapper.selectByTimeRange(startTime, endTime);
            for (Post post : timeRangePosts) {
                if (communityUserIds.contains(post.getUserId())) {
                    activeUserIds.add(post.getUserId());
                }
            }
            
            // 3. 统计时间范围内有点赞行为的用户（从 like_record 表）
            try {
                List<Map<String, Object>> likeRecords = userMapper.selectUserIdsWithActionInTimeRange(
                    "like_record", "user_id", "like_time", startTime, endTime);
                for (Map<String, Object> record : likeRecords) {
                    Long userId = ((Number) record.get("user_id")).longValue();
                    if (communityUserIds.contains(userId)) {
                        activeUserIds.add(userId);
                    }
                }
            } catch (Exception e) {
                log.warn("查询点赞记录失败，跳过此统计项", e);
            }
            
            // 4. 统计时间范围内有评论行为的用户（从 comment 表）
            try {
                List<Map<String, Object>> comments = userMapper.selectUserIdsWithActionInTimeRange(
                    "comment", "user_id", "comment_time", startTime, endTime);
                for (Map<String, Object> record : comments) {
                    Long userId = ((Number) record.get("user_id")).longValue();
                    if (communityUserIds.contains(userId)) {
                        activeUserIds.add(userId);
                    }
                }
            } catch (Exception e) {
                log.warn("查询评论记录失败，跳过此统计项", e);
            }
            
            // 5. 统计时间范围内有收藏行为的用户（从 collection 表）
            try {
                List<Map<String, Object>> collections = userMapper.selectUserIdsWithActionInTimeRange(
                    "collection", "user_id", "collect_time", startTime, endTime);
                for (Map<String, Object> record : collections) {
                    Long userId = ((Number) record.get("user_id")).longValue();
                    if (communityUserIds.contains(userId)) {
                        activeUserIds.add(userId);
                    }
                }
            } catch (Exception e) {
                log.warn("查询收藏记录失败，跳过此统计项", e);
            }
            
            // 6. 统计时间范围内登录过的用户（从 user 表的 last_login_time 字段）
            try {
                List<User> loginUsers = userMapper.selectUsersByLoginTimeRange(community, startTime, endTime);
                for (User user : loginUsers) {
                    activeUserIds.add(user.getUserId());
                }
            } catch (Exception e) {
                log.warn("查询登录记录失败，跳过此统计项", e);
            }
            
            // 返回活跃用户数
            return (long) activeUserIds.size();
            
        } catch (Exception e) {
            log.error("统计社区活跃用户失败", e);
            return 0L;
        }
    }
    
    /**
     * 管理员首页统计数据 VO
     */
    public static class DashboardStatsVO {
        private Long totalUsers;        // 总用户数
        private Long totalPosts;        // 总帖子数
        private Long pendingUsers;      // 待审核用户数
        private Long pendingPosts;      // 待审核帖子数
        private Long todayActiveUsers;  // 今日活跃用户数
        private Long todayPosts;        // 今日新增帖子数
            
        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
            
        public Long getTotalPosts() { return totalPosts; }
        public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
            
        public Long getPendingUsers() { return pendingUsers; }
        public void setPendingUsers(Long pendingUsers) { this.pendingUsers = pendingUsers; }
            
        public Long getPendingPosts() { return pendingPosts; }
        public void setPendingPosts(Long pendingPosts) { this.pendingPosts = pendingPosts; }
            
        public Long getTodayActiveUsers() { return todayActiveUsers; }
        public void setTodayActiveUsers(Long todayActiveUsers) { this.todayActiveUsers = todayActiveUsers; }
            
        public Long getTodayPosts() { return todayPosts; }
        public void setTodayPosts(Long todayPosts) { this.todayPosts = todayPosts; }
    }
        
    /**
     * 社区统计数据 VO
     */
    public static class CommunityStatisticsVO {
        private Long totalUsers;        // 社区总用户数
        private Long totalPosts;        // 社区总发帖量
        private Long pendingPosts;      // 待审核帖子数
        private Long pendingUsers;      // 待审核用户数
        private Long todayPosts;        // 今日新增帖子
        private Long todayNewUsers;     // 今日新增用户
        private Long todayActiveUsers;  // 今日活跃用户数（DAU）
        private Long yesterdayActiveUsers;  // 昨日活跃用户数（用于计算增长率）
        private Double dauGrowthRate;   // DAU 增长率（百分比）
        private Long totalReports;      // 总举报数
        private Long pendingReports;    // 待处理举报数
            
        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
            
        public Long getTotalPosts() { return totalPosts; }
        public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
            
        public Long getPendingPosts() { return pendingPosts; }
        public void setPendingPosts(Long pendingPosts) { this.pendingPosts = pendingPosts; }
            
        public Long getPendingUsers() { return pendingUsers; }
        public void setPendingUsers(Long pendingUsers) { this.pendingUsers = pendingUsers; }
            
        public Long getTodayPosts() { return todayPosts; }
        public void setTodayPosts(Long todayPosts) { this.todayPosts = todayPosts; }
            
        public Long getTodayNewUsers() { return todayNewUsers; }
        public void setTodayNewUsers(Long todayNewUsers) { this.todayNewUsers = todayNewUsers; }
        
        public Long getTodayActiveUsers() { return todayActiveUsers; }
        public void setTodayActiveUsers(Long todayActiveUsers) { this.todayActiveUsers = todayActiveUsers; }
        
        public Long getYesterdayActiveUsers() { return yesterdayActiveUsers; }
        public void setYesterdayActiveUsers(Long yesterdayActiveUsers) { this.yesterdayActiveUsers = yesterdayActiveUsers; }
        
        public Double getDauGrowthRate() { return dauGrowthRate; }
        public void setDauGrowthRate(Double dauGrowthRate) { this.dauGrowthRate = dauGrowthRate; }
            
        public Long getTotalReports() { return totalReports; }
        public void setTotalReports(Long totalReports) { this.totalReports = totalReports; }
            
        public Long getPendingReports() { return pendingReports; }
        public void setPendingReports(Long pendingReports) { this.pendingReports = pendingReports; }
    }
        
    /**
     * 用户发帖排名 VO
     */
    public static class UserPostRankingVO {
        private Long userId;
        private String userName;
        private String avatar;
        private Long postCount;
            
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
            
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
            
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
            
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
    }
    
    /**
     * 检查管理员权限
     */
    private void checkAdminPermission(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户角色是否为管理员或超级管理员
        if (user.getUserRole() != 2 && user.getUserRole() != 3) {
            throw new RuntimeException("权限不足，需要管理员权限");
        }
    }
    
    /**
     * 获取当前登录管理员的信息
     */
    @GetMapping("/info")
    public ResultVO<UserInfoVO> getCurrentAdminInfo(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            UserInfoVO userInfo = userService.getUserInfo(userId);
            
            log.info("获取管理员信息成功：userId={}, community={}", userId, userInfo.getCommunity());
            return ResultVO.success("获取管理员信息成功", userInfo);
            
        } catch (Exception e) {
            log.error("获取管理员信息失败", e);
            return ResultVO.error("获取管理员信息失败：" + e.getMessage());
        }
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
    
    /**
     * 获取图片验证码
     */
    @GetMapping("/captcha/image")
    public ResultVO<Map<String, Object>> getCaptchaImage() {
        try {
            // 使用 Hutool 创建线条验证码，宽 100，高 50，4 位验证码，10 条干扰线
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 50, 4, 30);
            
            // 设置验证码样式
            captcha.setBackground(Color.WHITE);
            captcha.setFont(new Font("Arial", Font.BOLD, 36));
            
            // 生成验证码文本
            String captchaCode = captcha.getCode();
            
            // 生成唯一标识符
            String captchaId = UUID.randomUUID().toString();
            
            // 将验证码存入 Redis，有效期 5 分钟
            redisTemplate.opsForValue().set(
                "captcha:" + captchaId, 
                captchaCode, 
                5, 
                TimeUnit.MINUTES
            );
            
            // 将验证码图片转换为 Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(captcha.getImage(), "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            
            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("captchaId", captchaId);
            result.put("captchaImage", base64Image);
            
            log.info("生成验证码：captchaId={}, code={}", captchaId, captchaCode);
            
            return ResultVO.success("获取验证码成功", result);
        } catch (Exception e) {
            log.error("获取验证码失败", e);
            return ResultVO.error("获取验证码失败：" + e.getMessage());
        }
    }
}