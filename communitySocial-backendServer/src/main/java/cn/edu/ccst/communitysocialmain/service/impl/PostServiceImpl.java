package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.PostCreateDTO;
import cn.edu.ccst.communitysocialmain.dto.ReviewDTO;
import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.entity.LikeRecord;

import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.entity.UserCollection;
import cn.edu.ccst.communitysocialmain.mapper.CategoryMapper;
import cn.edu.ccst.communitysocialmain.mapper.CollectionMapper;
import cn.edu.ccst.communitysocialmain.mapper.LikeRecordMapper;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;

import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.service.MessageService;

import cn.edu.ccst.communitysocialmain.utils.SensitiveWordFilter;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostDetailVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 帖子服务实现类
 */
@Slf4j
@Service
public class PostServiceImpl implements PostService {
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;
    

    
    @Autowired
    private CollectionMapper collectionMapper;
    
    @Autowired
    private LikeRecordMapper likeRecordMapper;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private MessageService messageService;
    
    @Override
    @Transactional
    public PostDetailVO createPost(Long userId, PostCreateDTO postCreateDTO) {
        long startTime = System.currentTimeMillis();
        
        // 检查用户是否存在且已认证
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getAuthStatus() != 2) {
            throw new RuntimeException("请先完成实名认证");
        }
        
        // 创建帖子对象
        Post post = new Post();
        post.setPostId(generatePostId());
        post.setUserId(userId);
        post.setCategoryId(postCreateDTO.getCategoryId());
        post.setTitle(sensitiveWordFilter.filter(postCreateDTO.getTitle()));
        post.setContent(sensitiveWordFilter.filter(postCreateDTO.getContent()));
        post.setLongitude(postCreateDTO.getLongitude());
        post.setLatitude(postCreateDTO.getLatitude());
        post.setIsAnonymous(postCreateDTO.getIsAnonymous());
        post.setStatus(1); // 待审核
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        
        // 设置二手交易相关字段（如果存在）
        if (postCreateDTO.getPrice() != null) {
            post.setPrice(postCreateDTO.getPrice());
        }
        if (postCreateDTO.getTransactionMode() != null) {
            post.setTransactionMode(postCreateDTO.getTransactionMode());
        }
        if (postCreateDTO.getContactInfo() != null) {
            post.setContactInfo(postCreateDTO.getContactInfo());
        }
        
        post.setPublishTime(LocalDateTime.now());
        
        // 处理图片 URL
        if (!CollectionUtils.isEmpty(postCreateDTO.getImageUrls())) {
            post.setImageUrls(postCreateDTO.getImageUrls());
        }
        
        // 插入帖子
        int result = postMapper.insert(post);
        if (result <= 0) {
            throw new RuntimeException("发布帖子失败");
        }
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user.getNickname());
            log.setOperatorName(user.getNickname());
            String operationValue = "帖子";
            log.setOperation(operationValue); // 操作类型为帖子
            log.setContent(String.format("%s（id:%s）发布了帖子%s(id:%s)", 
                user.getNickname(), userId, post.getTitle(), post.getPostId()));
            log.setModule("POST");
            log.setSubModule("PUBLISH");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            System.out.println("=== 准备记录发帖日志 ===");
            System.out.println("operation 值：" + log.getOperation());
            System.out.println("content 值：" + log.getContent());
            
            operationLogService.logSuccess(log);
            System.out.println("发帖日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录发帖日志失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        return getPostDetail(post.getPostId(), userId);
    }
    
    @Override
    @Transactional
    public PostDetailVO updatePost(Long userId, PostCreateDTO postUpdateDTO) {
        long startTime = System.currentTimeMillis();
        
        // 检查帖子是否存在
        if (postUpdateDTO.getPostId() == null) {
            throw new RuntimeException("帖子ID不能为空");
        }
        
        Post existingPost = postMapper.selectById(postUpdateDTO.getPostId());
        if (existingPost == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 检查是否为帖子作者
        if (!existingPost.getUserId().equals(userId)) {
            throw new RuntimeException("无权编辑此帖子");
        }
        
        // 检查帖子状态，只有待审核(status=1)或已发布(status=2)的帖子可以编辑
        if (existingPost.getStatus() != 1 && existingPost.getStatus() != 2) {
            throw new RuntimeException("当前状态的帖子不允许编辑");
        }
        
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新帖子字段
        existingPost.setCategoryId(postUpdateDTO.getCategoryId());
        existingPost.setTitle(sensitiveWordFilter.filter(postUpdateDTO.getTitle()));
        existingPost.setContent(sensitiveWordFilter.filter(postUpdateDTO.getContent()));
        existingPost.setLongitude(postUpdateDTO.getLongitude());
        existingPost.setLatitude(postUpdateDTO.getLatitude());
        existingPost.setIsAnonymous(postUpdateDTO.getIsAnonymous());
        
        // 如果帖子是已发布状态，编辑后重新设置为待审核
        if (existingPost.getStatus() == 2) {
            existingPost.setStatus(1); // 重新审核
        }
        
        // 更新二手交易相关字段
        if (postUpdateDTO.getPrice() != null) {
            existingPost.setPrice(postUpdateDTO.getPrice());
        }
        if (postUpdateDTO.getTransactionMode() != null) {
            existingPost.setTransactionMode(postUpdateDTO.getTransactionMode());
        }
        if (postUpdateDTO.getContactInfo() != null) {
            existingPost.setContactInfo(postUpdateDTO.getContactInfo());
        }
        
        // 处理图片 URL
        if (!CollectionUtils.isEmpty(postUpdateDTO.getImageUrls())) {
            existingPost.setImageUrls(postUpdateDTO.getImageUrls());
        } else {
            existingPost.setImageUrls(null);
        }
        
        // 执行更新
        int result = postMapper.update(existingPost);
        if (result <= 0) {
            throw new RuntimeException("更新帖子失败");
        }
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user.getNickname());
            log.setOperatorName(user.getNickname());
            log.setOperation("帖子");
            log.setContent(String.format("%s（id:%s）编辑了帖子%s(id:%s)", 
                user.getNickname(), userId, existingPost.getTitle(), existingPost.getPostId()));
            log.setModule("POST");
            log.setSubModule("UPDATE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
            
            // 如果帖子从已发布变为待审核，发送系统消息通知用户
            if (existingPost.getStatus() == 1) {
                String messageContent = String.format("您的帖子《%s》已被修改，正在重新审核中。", existingPost.getTitle());
                messageService.sendSystemMessage(userId, messageContent, 1);
            }
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录编辑日志或发送系统消息失败：{}", e.getMessage());
        }
        
        return getPostDetail(existingPost.getPostId(), userId);
    }
    
    @Override
    public PostDetailVO getPostDetail(Long postId, Long currentUserId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 增加浏览次数（非本人查看时）
        if (!post.getUserId().equals(currentUserId)) {
            postMapper.incrementViewCount(postId);
            post.setViewCount(post.getViewCount() + 1);
        }
        
        return convertToPostDetailVO(post, currentUserId);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByUserId(Long userId, Integer page, Integer size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.selectByUserId(userId, offset, size);
        Long total = postMapper.countByUserId(userId);
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByType(Integer type, Integer page, Integer size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.selectByType(type, offset, size);
        Long total = postMapper.countByType(type);
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByStatus(Integer status, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.selectByStatus(status, offset, size);
        Long total = postMapper.countByStatus(status);
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, null))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPendingPosts(Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.selectPendingPosts(offset, size);
        Long total = postMapper.countByStatus(1);
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, null))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPendingPostsByCommunity(String community, Integer page, Integer size, Long userId) {
        int offset = (page - 1) * size;
        // 使用社区过滤的查询方法（需要添加到 Mapper）
        // 这里暂时使用内存过滤，后续可以优化为 SQL 过滤
        List<Post> allPendingPosts = postMapper.selectPendingPosts(0, Integer.MAX_VALUE);
        
        List<Post> filteredPosts = allPendingPosts.stream()
            .filter(post -> {
                User postAuthor = userMapper.selectById(post.getUserId());
                return postAuthor != null && community.equals(postAuthor.getCommunity());
            })
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
        
        Long total = allPendingPosts.stream()
            .filter(post -> {
                User postAuthor = userMapper.selectById(post.getUserId());
                return postAuthor != null && community.equals(postAuthor.getCommunity());
            })
            .count();
        
        List<PostDetailVO> postVOs = filteredPosts.stream()
                .map(post -> convertToPostDetailVO(post, null))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByConditions(Integer page, Integer size, Integer status, String keyword, Integer categoryId) {
        return getPostsByConditionsWithCommunity(page, size, status, keyword, categoryId, null);
    }
    
    /**
     * 根据条件查询帖子列表（支持社区过滤）
     */
    public PageVO<PostDetailVO> getPostsByConditionsWithCommunity(Integer page, Integer size, Integer status, String keyword, Integer categoryId, String community) {
        int offset = (page - 1) * size;
        List<Post> posts;
        Long total;
        
        // 如果有社区信息，使用社区过滤；否则使用原有的公开查询
        if (StringUtils.hasText(community)) {
            // 社区管理员：只显示同社区的帖子
            if (StringUtils.hasText(keyword)) {
                // 有搜索关键词，使用社区 + 关键词搜索
                posts = postMapper.selectByCommunityAndKeyword(community, keyword, status, categoryId, offset, size);
                total = countPostsInCommunity(community, keyword, status, categoryId);
            } else if (categoryId != null) {
                // 按板块分类 ID 筛选
                posts = postMapper.selectByCommunityAndCategory(community, categoryId, status, offset, size);
                total = countPostsInCommunityAndCategory(community, categoryId);
            } else if (status != null) {
                // 按状态筛选
                posts = filterPostsByCommunityAndStatus(status, community, offset, size);
                total = countPostsByCommunityAndStatus(status, community);
            } else {
                // 无筛选条件：获取该社区的所有帖子（包括所有状态）
                // 由于 selectByCommunity 硬编码了 status=2，需要使用其他方式
                posts = filterPostsByCommunity(community, offset, size);
                total = countAllPostsByCommunity(community);
            }
        } else {
            // 超级管理员或无社区信息：使用原有逻辑
            if (StringUtils.hasText(keyword)) {
                // 有搜索关键词，使用模糊搜索
                posts = postMapper.selectByKeyword(keyword, status, categoryId, offset, size);
                total = postMapper.countByKeyword(keyword, status, categoryId);
            } else if (categoryId != null) {
                // 有条件筛选
                posts = postMapper.selectByConditions(status, categoryId, offset, size);
                total = postMapper.countByConditions(status, categoryId);
            } else if (status != null) {
                // 按状态筛选
                posts = postMapper.selectByStatus(status, offset, size);
                total = postMapper.countByStatus(status);
            } else {
                // 获取所有帖子（包括所有状态）
                posts = postMapper.selectAll(offset, size);
                total = postMapper.countAll();
            }
        }
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, null))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostList(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts;
        Long total;
        
        // 构建查询条件
        Integer status = 2; // 只显示已审核通过的帖子
        
        // 获取当前用户所在社区（居民端需要过滤）
        String userCommunity = null;
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && currentUser.getAuthStatus() == 2) {
                userCommunity = currentUser.getCommunity();
                log.debug("当前用户{}所在社区：{}", currentUserId, userCommunity);
            }
        }
        
        // 如果有社区信息，使用社区过滤；否则使用原有的公开查询
        if (StringUtils.hasText(userCommunity)) {
            // 居民端：只显示同社区的帖子
            if (StringUtils.hasText(keyword)) {
                // 有搜索关键词，使用社区 + 关键词搜索
                posts = postMapper.selectByCommunityAndKeyword(userCommunity, keyword, status, categoryId, offset, size);
                total = countPostsInCommunity(userCommunity, keyword, status, categoryId);
            } else if (categoryId != null) {
                // 按板块分类 ID 筛选（优先使用 categoryId）
                // status 为 null 时查询该分类下所有状态的帖子
                posts = postMapper.selectByCommunityAndCategory(userCommunity, categoryId, status, offset, size);
                total = countPostsInCommunityAndCategory(userCommunity, categoryId);
            } else {
                // 获取该社区的所有已审核通过的帖子
                posts = postMapper.selectByCommunity(userCommunity, offset, size);
                total = postMapper.countByCommunity(userCommunity);
            }
        } else {
            // 游客或未认证用户：使用原有逻辑，但只显示已审核通过的帖子
            if (StringUtils.hasText(keyword)) {
                // 有搜索关键词，使用模糊搜索
                posts = postMapper.selectByKeyword(keyword, status, categoryId, offset, size);
                total = postMapper.countByKeyword(keyword, status, categoryId);
            } else if (categoryId != null) {
                // 按板块分类 ID 筛选（优先使用 categoryId）
                posts = postMapper.selectByCategoryId(categoryId, offset, size);
                total = postMapper.countByCategoryId(categoryId);
            } else if (type != null) {
                // 按类型筛选（兼容旧版本，type 实际就是 categoryId）
                posts = postMapper.selectByType(type, offset, size);
                total = postMapper.countByType(type);
            } else {
                // 获取所有已审核通过的帖子
                posts = postMapper.selectByStatus(status, offset, size);
                total = postMapper.countByStatus(status);
            }
        }
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByTime(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts;
        Long total;
        
        Integer status = 2; // 只显示已审核通过的帖子
        
        // 获取当前用户所在社区（居民端需要过滤）
        String userCommunity = null;
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && currentUser.getAuthStatus() == 2) {
                userCommunity = currentUser.getCommunity();
                log.debug("时间排序 - 当前用户{}所在社区：{}", currentUserId, userCommunity);
            }
        }
        
        // 使用社区过滤
        if (StringUtils.hasText(userCommunity)) {
            posts = postMapper.selectByCommunityAndKeyword(userCommunity, keyword, status, categoryId, offset, size);
            total = countPostsInCommunity(userCommunity, keyword, status, categoryId);
        } else {
            posts = postMapper.selectByTime(categoryId, keyword, status, offset, size);
            if (StringUtils.hasText(keyword)) {
                total = postMapper.countByKeyword(keyword, status, type);
            } else if (type != null) {
                total = postMapper.countByType(type);
            } else {
                total = postMapper.countByStatus(status);
            }
        }
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByHot(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts;
        Long total;
        
        Integer status = 2; // 只显示已审核通过的帖子
        
        // 获取当前用户所在社区（居民端需要过滤）
        String userCommunity = null;
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && currentUser.getAuthStatus() == 2) {
                userCommunity = currentUser.getCommunity();
                log.debug("热度排序 - 当前用户{}所在社区：{}", currentUserId, userCommunity);
            }
        }
        
        // 使用社区过滤
        if (StringUtils.hasText(userCommunity)) {
            // 注意：selectByHot 方法暂时不支持社区过滤，使用社区 + 关键词搜索代替
            posts = postMapper.selectByCommunityAndKeyword(userCommunity, keyword, status, categoryId, offset, size);
            total = countPostsInCommunity(userCommunity, keyword, status, categoryId);
        } else {
            posts = postMapper.selectByHot(categoryId, keyword, status, offset, size);
            if (StringUtils.hasText(keyword)) {
                total = postMapper.countByKeyword(keyword, status, type);
            } else if (type != null) {
                total = postMapper.countByType(type);
            } else {
                total = postMapper.countByStatus(status);
            }
        }
        
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> convertToPostDetailVO(post, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, postVOs);
    }
    
    @Override
    public PageVO<PostDetailVO> getPostsByNearby(Double longitude, Double latitude, Double radius, 
                                                Integer page, Integer size, Integer type, Integer categoryId, 
                                                String keyword, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Post> posts;
        Long total;
            
        Integer status = 2; // 只显示已审核通过的帖子
            
        // 附近帖子使用地理位置查询，不按社区过滤
        posts = postMapper.selectByNearby(longitude, latitude, radius, categoryId, keyword, status, offset, size);
        total = (long) posts.size(); // 附近帖子数量需要特殊处理
            
        List<PostDetailVO> postVOs = posts.stream()
                .map(post -> {
                    PostDetailVO vo = convertToPostDetailVO(post, currentUserId);
                    // 计算并设置距离
                    if (post.getLongitude() != null && post.getLatitude() != null) {
                        double distance = calculateDistance(latitude, longitude, 
                                                          post.getLatitude().doubleValue(), 
                                                          post.getLongitude().doubleValue());
                        vo.setDistance(distance); // 设置距离信息到 VO 中
                    }
                    return vo;
                })
                .collect(Collectors.toList());
            
        return new PageVO<>(page, size, total, postVOs);
    }
    
    /**
     * 计算两点间距离（单位：公里）
     * 使用Haversine公式
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371; // 地球半径，单位公里
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return EARTH_RADIUS * c;
    }
    
    @Override
    public Map<String, Object> getPostStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计各状态帖子数量
        statistics.put("pendingCount", postMapper.countByStatus(1));
        statistics.put("approvedCount", postMapper.countByStatus(2));
        statistics.put("rejectedCount", postMapper.countByStatus(3));
        
        // 统计今日审核数量（简化处理）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
        statistics.put("todayReviewed", postMapper.countReviewedToday(todayStart, todayEnd));
        
        return statistics;
    }
    
    @Override
    public List<Map<String, Object>> getReviewHistory(Long postId) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        // 这里应该查询操作日志表获取审核历史
        // 简化处理，返回模拟数据
        Map<String, Object> record = new HashMap<>();
        record.put("reviewId", "1");
        record.put("reviewerName", "管理员");
        record.put("reviewTime", LocalDateTime.now().minusDays(1));
        record.put("status", 2);
        record.put("remark", "内容合规，审核通过");
        history.add(record);
        
        return history;
    }
    
    @Override
    public PageVO<PostDetailVO> searchPosts(String keyword, Integer page, Integer size, Long currentUserId) {
        // 简化实现，实际应该使用全文搜索引擎如Elasticsearch
        int offset = (page - 1) * size;
        // 这里直接返回空结果，实际需要实现搜索逻辑
        return new PageVO<>(page, size, 0L, new ArrayList<>());
    }
    
    @Override
    public PageVO<PostDetailVO> getNearbyPosts(Double longitude, Double latitude, Double radius, 
                                              Integer page, Integer size, Long currentUserId) {
        // 该方法已废弃，请使用 getPostsByNearby
        return getPostsByNearby(longitude, latitude, radius, page, size, null, null, null, currentUserId);
    }
    
    @Override
    public PageVO<PostDetailVO> getHotPosts(Integer days, Integer page, Integer size, Long currentUserId) {
        // 该方法已废弃，请使用 getPostsByHot
        return getPostsByHot(page, size, null, null, null, currentUserId);
    }
    
    @Override
    @Transactional
    public void reviewPost(Long adminUserId, ReviewDTO reviewDTO) {
        long startTime = System.currentTimeMillis();
        
        Long targetId = reviewDTO.getTargetId();
        Post post = postMapper.selectById(targetId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
        
        postMapper.updateStatus(targetId, reviewDTO.getStatus(), String.valueOf(adminUserId));
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(adminUserId);
            log.setNickname(admin != null ? admin.getNickname() : "");
            log.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            log.setOperation("实名审核"); // 实名认证审核操作
            
            // 根据审核状态生成不同的描述
            String actionDesc = reviewDTO.getStatus() == 2 ? "通过" : "拒绝";
            log.setContent(String.format("%s（id:%s）%s了帖子%s(id:%s) 的审核申请，备注：%s", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                actionDesc,
                post.getTitle(),
                post.getPostId(),
                reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "无"));
            
            log.setModule("POST");
            log.setSubModule("REVIEW");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            System.out.println("=== 准备记录帖子审核日志 ===");
            System.out.println("operation 值：" + log.getOperation());
            System.out.println("content 值：" + log.getContent());
            System.out.println("审核结果：" + (reviewDTO.getStatus() == 2 ? "通过" : "拒绝"));
            
            operationLogService.logSuccess(log);
            System.out.println("帖子审核日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录帖子审核日志失败：" + e.getMessage());
            e.printStackTrace();
        }
        
        // 发送系统消息通知发帖人审核结果
        try {
            Long authorId = post.getUserId();
            User author = userMapper.selectById(authorId);
            
            String statusText = reviewDTO.getStatus() == 2 ? "通过" : "拒绝";
            String messageContent = String.format("您的帖子《%s》审核%s。审核管理员：%s，备注：%s", 
                post.getTitle(), 
                statusText,
                admin != null ? admin.getNickname() : "管理员",
                reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "无");
            
            messageService.sendSystemMessage(authorId, messageContent, 1); // messageType=1 表示文本消息
            log.info("已发送帖子审核结果系统消息给用户：{}，帖子 ID：{}", authorId, post.getPostId());
        } catch (Exception e) {
            // 发送消息失败不影响主业务
            log.warn("发送帖子审核系统消息失败：{}", e.getMessage());
        }
    }
    
    @Override
    public void topPost(Long postId) {
        // 置顶逻辑：使用 is_top 字段存储置顶状态
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 切换置顶状态（0->1 置顶，1->0 取消置顶）
        Integer currentTopStatus = post.getIsTop() != null ? post.getIsTop() : 0;
        Integer newTopStatus = currentTopStatus == 1 ? 0 : 1;
        
        // 更新帖子的置顶状态
        post.setIsTop(newTopStatus);
        postMapper.update(post);
        
        log.info("帖子{}置顶状态已{}", postId, newTopStatus == 1 ? "设置为置顶" : "取消置顶");
    }
    
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        long startTime = System.currentTimeMillis();
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 检查权限（只能删除自己的帖子或管理员操作）
        if (!post.getUserId().equals(userId)) {
            User currentUser = userMapper.selectById(userId);
            if (currentUser == null || !"admin".equals(currentUser.getUserRole()) && !"sadmin".equals(currentUser.getUserRole())) {
                throw new RuntimeException("无权限删除此帖子");
            }
        }
        
        postMapper.deleteById(postId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("DELETE"); // 删除操作
            log.setContent(String.format("%s（id:%s）删除了帖子%s(id:%s)", 
                user != null ? user.getNickname() : "未知用户", 
                userId, 
                post.getTitle(), 
                post.getPostId()));
            log.setModule("POST");
            log.setSubModule("DELETE");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录删除帖子日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    public void incrementViewCount(Long postId) {
        postMapper.incrementViewCount(postId);
    }
    
    @Override
    public Post getPostById(Long postId) {
        return postMapper.selectById(postId);
    }
    
    @Override
    public Long generatePostId() {
        return SnowflakeIdGenerator.nextId();
    }
    
    /**
     * 生成收藏 ID
     */
    private Long generateCollectionId() {
        return SnowflakeIdGenerator.nextId();
    }
    
    /**
     * 生成点赞 ID
     */
    private Long generateLikeId() {
        return SnowflakeIdGenerator.nextId();
    }
    
    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        long startTime = System.currentTimeMillis();
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 检查是否已经点赞
        LikeRecord existingLike = likeRecordMapper.selectByUserAndTarget(userId, 1, postId);
        if (existingLike != null) {
            log.info("用户{}已点赞帖子{}，无需重复点赞", userId, postId);
            return;
        }
        
        // 创建点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setLikeId(generateLikeId());
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(1); // 1-帖子
        likeRecord.setTargetId(postId);
        likeRecord.setLikeTime(LocalDateTime.now());
        
        int result = likeRecordMapper.insert(likeRecord);
        if (result <= 0) {
            throw new RuntimeException("点赞失败");
        }
        
        // 增加帖子点赞数
        postMapper.incrementLikeCount(postId);
        post.setLikeCount(post.getLikeCount() + 1);
        
        log.info("用户{}点赞帖子{}成功", userId, postId);
        
        // 记录操作日志并发送系统消息
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("LIKE"); // 点赞操作
            log.setContent(String.format("%s（id:%s）点赞了帖子%s(id:%s)", 
                user != null ? user.getNickname() : "未知用户", userId, post.getTitle(), postId));
            log.setModule("POST");
            log.setSubModule("LIKE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
            
            // 发送系统消息通知帖子作者
            Long authorId = post.getUserId();
            if (!authorId.equals(userId)) { // 不给自己发消息
                String messageContent = String.format("您的帖子《%s》收到了来自 %s 的点赞", 
                    post.getTitle(), 
                    user.getNickname());
                messageService.sendSystemMessage(authorId, messageContent, 1);
            }
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录点赞日志或发送系统消息失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {
        long startTime = System.currentTimeMillis();
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 检查是否有点赞记录
        LikeRecord existingLike = likeRecordMapper.selectByUserAndTarget(userId, 1, postId);
        if (existingLike == null) {
            log.warn("用户{}未点赞帖子{}，无法取消", userId, postId);
            return;
        }
        
        // 删除点赞记录
        int deleteResult = likeRecordMapper.deleteByUserAndTarget(userId, 1, postId);
        if (deleteResult <= 0) {
            throw new RuntimeException("取消点赞失败");
        }
        
        // 减少帖子点赞数
        if (post.getLikeCount() > 0) {
            postMapper.decrementLikeCount(postId);
            post.setLikeCount(post.getLikeCount() - 1);
        }
        
        log.info("用户{}取消点赞帖子{}成功", userId, postId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("UNLIKE"); // 取消点赞操作
            log.setContent(String.format("%s（id:%s）取消了对帖子%s(id:%s)的点赞", 
                user != null ? user.getNickname() : "未知用户", userId, post.getTitle(), postId));
            log.setModule("POST");
            log.setSubModule("UNLIKE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录取消点赞日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void collectPost(Long userId, Long postId) {
        long startTime = System.currentTimeMillis();
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 检查是否已经收藏
        UserCollection existingCollection = collectionMapper.selectByUserAndPost(userId, postId);
        if (existingCollection != null) {
            throw new RuntimeException("已经收藏过该帖子");
        }
        
        // 创建收藏记录
        UserCollection collection = new UserCollection();
        collection.setCollectionId(generateCollectionId());
        collection.setUserId(userId);
        collection.setPostId(postId);
        collection.setCollectTime(LocalDateTime.now());
        
        int result = collectionMapper.insert(collection);
        if (result <= 0) {
            throw new RuntimeException("收藏失败");
        }
        
        // 增加帖子收藏数
        postMapper.incrementCollectCount(postId);
        
        log.info("用户{}收藏帖子{}成功", userId, postId);
        
        // 记录操作日志并发送系统消息
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("COLLECT"); // 收藏操作
            log.setContent(String.format("%s（id:%s）收藏了帖子%s(id:%s)", 
                user != null ? user.getNickname() : "未知用户", userId, post.getTitle(), postId));
            log.setModule("POST");
            log.setSubModule("COLLECT");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
            
            // 发送系统消息通知帖子作者
            Long authorId = post.getUserId();
            if (!authorId.equals(userId)) { // 不给自己发消息
                String messageContent = String.format("您的帖子《%s》被 %s 收藏了", 
                    post.getTitle(), 
                    user.getNickname());
                messageService.sendSystemMessage(authorId, messageContent, 1);
            }
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录收藏日志或发送系统消息失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void uncollectPost(Long userId, Long postId) {
        long startTime = System.currentTimeMillis();
        
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 查询收藏记录
        UserCollection collection = collectionMapper.selectByUserAndPost(userId, postId);
        if (collection == null) {
            throw new RuntimeException("未收藏该帖子");
        }
        
        // 删除收藏记录
        int result = collectionMapper.deleteByUserAndPost(userId, postId);
        if (result <= 0) {
            throw new RuntimeException("取消收藏失败");
        }
        
        // 减少帖子收藏数
        postMapper.decrementCollectCount(postId);
        
        log.info("用户{}取消收藏帖子{}", userId, postId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("UNCOLLECT"); // 取消收藏操作
            log.setContent(String.format("%s（id:%s）取消了对帖子%s(id:%s)的收藏", 
                user != null ? user.getNickname() : "未知用户", userId, post.getTitle(), postId));
            log.setModule("POST");
            log.setSubModule("UNCOLLECT");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录取消收藏日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    public PageVO<Post> getUserCollections(Long userId, Integer page, Integer size) {
        log.info("获取用户{}的收藏列表，页码{}，每页数量{}", userId, page, size);
        
        // 计算偏移量
        int offset = (page - 1) * size;
        
        // 查询用户的收藏记录
        List<UserCollection> collections = collectionMapper.selectByUserId(userId, offset, size);
        
        // 查询帖子详情（逐个查询）
        List<Post> posts = new ArrayList<>();
        for (UserCollection collection : collections) {
            Post post = postMapper.selectById(collection.getPostId());
            if (post != null) {
                posts.add(post);
            }
        }
        
        // 查询总数
        Long total = collectionMapper.countByUserId(userId);
        
        return new PageVO<>(page, size, total, posts);
    }
    
    /**
     * 将 Post 实体转换为 PostDetailVO
     */
    private PostDetailVO convertToPostDetailVO(Post post, Long currentUserId) {
        log.debug("=== Post 对象调试信息 ===");
        log.debug("帖子 ID: {}", post.getPostId());
        log.debug("帖子标题：{}", post.getTitle());
        log.debug("isTop 值：{}", post.getIsTop());
        log.debug("is_top 字段是否存在：{}", post.getIsTop() != null);
        log.debug("isAnonymous 值：{}", post.getIsAnonymous());
        log.debug("status 值：{}", post.getStatus());
            
        PostDetailVO vo = new PostDetailVO();
        BeanUtils.copyProperties(post, vo);
        
        // 确保 isTop 不为 null，默认为 0
        if (vo.getIsTop() == null) {
            vo.setIsTop(0);
        }
            
        log.debug("=== VO 对象调试信息 ===");
        log.debug("VO 中 isTop 值：{}", vo.getIsTop());
            
        // 设置用户信息
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            // 匿名处理
            if (post.getIsAnonymous() == 1 && !post.getUserId().equals(currentUserId)) {
                userInfoVO.setNickname("匿名用户");
                userInfoVO.setAvatarUrl(null);
            }
            vo.setUserInfo(userInfoVO);
        }
            
        // 解析图片 URL
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            vo.setImageUrls(post.getImageUrls());
        }
            
        // 设置板块分类名称
        if (post.getCategoryId() != null) {
            Category category = categoryMapper.selectById(post.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
            
        // 设置当前用户交互状态
        if (currentUserId != null) {
            try {
                // 查询用户是否点赞（帖子 targetType=1）
                var likeRecord = likeRecordMapper.selectByUserAndTarget(currentUserId, 1, post.getPostId());
                vo.setIsLiked(likeRecord != null);
                
                // 查询用户是否收藏
                var collection = collectionMapper.selectByUserAndPost(currentUserId, post.getPostId());
                vo.setIsCollected(collection != null);
            } catch (Exception e) {
                log.warn("查询用户交互状态失败：{}", e.getMessage());
                vo.setIsLiked(false);
                vo.setIsCollected(false);
            }
        }
            
        return vo;
    }
    
    /**
     * 统计社区中指定条件的帖子数量
     */
    private Long countPostsInCommunity(String community, String keyword, Integer status, Integer categoryId) {
        // 简化实现，直接查询社区帖子总数
        // 实际应该根据关键词和分类进行更精确的统计
        return postMapper.countByCommunity(community);
    }
    
    /**
     * 统计社区中指定分类的帖子数量
     */
    private Long countPostsInCommunityAndCategory(String community, Integer categoryId) {
        // 查询所有指定分类的帖子（包括所有状态）
        List<Post> allPosts = postMapper.selectByCategoryId(categoryId, 0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子并计数
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .count();
    }
    
    /**
     * 根据状态和社区过滤帖子（内存过滤）
     */
    private List<Post> filterPostsByCommunityAndStatus(Integer status, String community, Integer offset, Integer size) {
        // 查询所有指定状态的帖子
        List<Post> allPosts = postMapper.selectByStatus(status, 0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据分类和社区过滤帖子（内存过滤）
     */
    private List<Post> filterPostsByCommunityAndCategory(Integer categoryId, String community, Integer offset, Integer size) {
        // 查询所有指定分类的帖子（包括所有状态）
        List<Post> allPosts = postMapper.selectByCategoryId(categoryId, 0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据社区过滤帖子（内存过滤，包括所有状态）
     */
    private List<Post> filterPostsByCommunity(String community, Integer offset, Integer size) {
        // 查询所有帖子
        List<Post> allPosts = postMapper.selectAll(0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
    }
    
    /**
     * 统计社区中所有状态的帖子数量
     */
    private Long countAllPostsByCommunity(String community) {
        // 查询所有帖子
        List<Post> allPosts = postMapper.selectAll(0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子并计数
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .count();
    }
    
    /**
     * 统计指定状态和社区的帖子数量
     */
    private Long countPostsByCommunityAndStatus(Integer status, String community) {
        // 查询所有指定状态的帖子
        List<Post> allPosts = postMapper.selectByStatus(status, 0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子并计数
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .count();
    }
    
    /**
     * 统计社区和分类的帖子数量（带状态筛选）
     */
    private Long countPostsByCommunityAndCategory(String community, Integer categoryId) {
        // 查询所有指定分类的帖子（包括所有状态）
        List<Post> allPosts = postMapper.selectByCategoryId(categoryId, 0, Integer.MAX_VALUE);
        
        // 内存过滤：只保留该社区的帖子并计数
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .count();
    }
    
    @Override
    public Long countPostsByCommunity(String community, Long userId) {
        return postMapper.countByCommunity(community);
    }
    
    @Override
    public Long countPostsByUserId(Long userId) {
        return postMapper.countByUserId(userId);
    }
    
    @Override
    public Long countPostsByCommunityAndTimeRange(String community, LocalDateTime startTime, LocalDateTime endTime) {
        return postMapper.countByCommunityAndTimeRange(community, startTime, endTime);
    }
}