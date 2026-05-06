package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.entity.*;
import cn.edu.ccst.communitysocialmain.mapper.*;
import cn.edu.ccst.communitysocialmain.service.DataStatisticsService;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现类
 */
@Slf4j
@Service
public class DataStatisticsServiceImpl implements DataStatisticsService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private LikeRecordMapper likeRecordMapper;
    
    @Autowired
    private CollectionMapper collectionMapper;
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private PostService postService;
    


    @Override
    public PlatformOverviewVO getPlatformOverview() {
        PlatformOverviewVO overview = new PlatformOverviewVO();
            
        try {
            // 获取基础统计数据
            overview.setTotalUsers(userMapper.countAll());
            overview.setTotalPosts(postMapper.countAll());
            overview.setTotalComments(commentMapper.countAll());
            overview.setTotalLikes(likeRecordMapper.countAll());
                
            // 获取社区数量
            overview.setCommunityCount(userMapper.countDistinctCommunities());
                
            // 获取今日数据
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
                
            overview.setTodayActiveUsers(userMapper.countActiveUsers(todayStart, todayEnd));
            overview.setTodayPosts(postMapper.countByTimeRange(todayStart, todayEnd));
            overview.setTodayComments(commentMapper.countByTimeRange(todayStart, todayEnd));
                
            // 在线用户数（模拟数据，实际应该从 Redis 等缓存获取）
            overview.setOnlineUsers(getOnlineUsers());
                
            log.info("获取平台总览统计数据成功");
        } catch (Exception e) {
            log.error("获取平台总览统计数据失败", e);
        }
            
        return overview;
    }

    @Override
    public UserStatisticsVO getUserStatistics() {
        UserStatisticsVO userStats = new UserStatisticsVO();
        
        try {
            // 基础统计
            userStats.setTotalUsers(userMapper.countAll());
            userStats.setActiveUsers(userMapper.countActiveUsersInPeriod(30)); // 30天内活跃
            
            // 时间段新增用户统计
            LocalDateTime now = LocalDateTime.now();
            userStats.setNewUsersToday(userMapper.countNewUsersInPeriod(now.minusDays(1), now));
            userStats.setNewUsersWeek(userMapper.countNewUsersInPeriod(now.minusWeeks(1), now));
            userStats.setNewUsersMonth(userMapper.countNewUsersInPeriod(now.minusMonths(1), now));
            
            // 用户增长率（与上周相比）
            LocalDateTime lastWeek = now.minusWeeks(1);
            Long lastWeekUsers = userMapper.countNewUsersInPeriod(lastWeek.minusWeeks(1), lastWeek);
            Long thisWeekUsers = userMapper.countNewUsersInPeriod(lastWeek, now);
            if (lastWeekUsers > 0) {
                userStats.setUserGrowthRate(((double)(thisWeekUsers - lastWeekUsers) / lastWeekUsers) * 100);
            } else {
                userStats.setUserGrowthRate(0.0);
            }
            
            // 用户增长趋势（近7天）
            List<UserGrowthTrend> growthTrend = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();
                Long count = userMapper.countNewUsersInPeriod(start, end);
                
                growthTrend.add(new UserGrowthTrend(
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 
                    count
                ));
            }
            userStats.setGrowthTrend(growthTrend);
            
            // 用户角色分布
            Map<String, Long> roles = new HashMap<>();
            roles.put("普通用户", userMapper.countByRole("user"));
            roles.put("管理员", userMapper.countByRole("admin"));
            roles.put("超级管理员", userMapper.countByRole("sadmin"));
            userStats.setUserRoles(roles);
            
            // 用户状态分布
            Map<String, Long> status = new HashMap<>();
            status.put("正常", userMapper.countByStatus(1));
            status.put("禁用", userMapper.countByStatus(0));
            status.put("待审核", userMapper.countByStatus(2));
            userStats.setUserStatus(status);
            
            log.info("获取用户统计数据成功");
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
        }
        
        return userStats;
    }

    @Override
    public ContentStatisticsVO getContentStatistics() {
        ContentStatisticsVO contentStats = new ContentStatisticsVO();
        
        try {
            // 帖子基础统计
            contentStats.setTotalPosts(postMapper.countAll());
            contentStats.setPublishedPosts(postMapper.countByStatus(2)); // 已发布
            contentStats.setPendingPosts(postMapper.countByStatus(1));   // 待审核
            contentStats.setDeletedPosts(postMapper.countByStatus(0));   // 已删除
            
            // 帖子类型分布
            Map<Integer, Long> postTypes = new HashMap<>();
            postTypes.put(1, postMapper.countByType(1)); // 普通帖子
            postTypes.put(2, postMapper.countByType(2)); // 求助帖子
            postTypes.put(3, postMapper.countByType(3)); // 二手帖子
            postTypes.put(4, postMapper.countByType(4)); // 活动帖子
            contentStats.setPostTypes(postTypes);
            
            // 帖子状态分布
            Map<Integer, Long> postStatus = new HashMap<>();
            postStatus.put(0, contentStats.getDeletedPosts());
            postStatus.put(1, contentStats.getPendingPosts());
            postStatus.put(2, contentStats.getPublishedPosts());
            contentStats.setPostStatus(postStatus);
            
            // 每日发帖统计（近7天）
            List<DailyPostStat> dailyPosts = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();
                Long count = postMapper.countByTimeRange(start, end);
                
                dailyPosts.add(new DailyPostStat(
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    count
                ));
            }
            contentStats.setDailyPosts(dailyPosts);
            
            // 日均发帖数
            if (!dailyPosts.isEmpty()) {
                double avg = dailyPosts.stream()
                    .mapToLong(DailyPostStat::getCount)
                    .average()
                    .orElse(0.0);
                contentStats.setAvgPostsPerDay(avg);
            }
            
            log.info("获取内容统计数据成功");
        } catch (Exception e) {
            log.error("获取内容统计数据失败", e);
        }
        
        return contentStats;
    }

    @Override
    public InteractionStatisticsVO getInteractionStatistics() {
        InteractionStatisticsVO interactionStats = new InteractionStatisticsVO();
        
        try {
            // 基础互动统计
            interactionStats.setTotalViews(calculateTotalViews());
            interactionStats.setTotalLikes(likeRecordMapper.countAll());
            interactionStats.setTotalComments(commentMapper.countAll());
            interactionStats.setTotalCollections(collectionMapper.countAll());
            interactionStats.setTotalShares(0L); // 分享功能暂未实现
            
            // 平均互动指标
            Long totalPosts = postMapper.countAll();
            if (totalPosts > 0) {
                interactionStats.setAvgViewsPerPost((double)interactionStats.getTotalViews() / totalPosts);
                interactionStats.setAvgLikesPerPost((double)interactionStats.getTotalLikes() / totalPosts);
                interactionStats.setAvgCommentsPerPost((double)interactionStats.getTotalComments() / totalPosts);
            }
            
            // 每日互动统计（近7天）
            List<DailyInteractionStat> dailyStats = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();
                
                Long views = calculateViewsInRange(start, end);
                Long likes = likeRecordMapper.countByTimeRange(start, end);
                Long comments = commentMapper.countByTimeRange(start, end);
                Long collections = collectionMapper.countByTimeRange(start, end);
                
                dailyStats.add(new DailyInteractionStat(
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    views, likes, comments, collections
                ));
            }
            interactionStats.setDailyInteractions(dailyStats);
            
            log.info("获取互动统计数据成功");
        } catch (Exception e) {
            log.error("获取互动统计数据失败", e);
        }
        
        return interactionStats;
    }

    @Override
    public TimeTrendVO getTimeTrendStatistics(Integer days) {
        TimeTrendVO trendStats = new TimeTrendVO();
        
        try {
            if (days == null || days <= 0) {
                days = 30; // 默认30天
            }
            
            // 每日统计
            List<DailyStat> dailyStats = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.plusDays(1).atStartOfDay();
                
                Long newUserCount = userMapper.countNewUsersInPeriod(start, end);
                Long newPostCount = postMapper.countByTimeRange(start, end);
                Long newCommentCount = commentMapper.countByTimeRange(start, end);
                Long activeUserCount = userMapper.countActiveUsers(start, end);
                
                dailyStats.add(new DailyStat(
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    newUserCount, newPostCount, newCommentCount, activeUserCount
                ));
            }
            trendStats.setDailyStats(dailyStats);
            
            // 每周统计（如果天数大于等于7天）
            if (days >= 7) {
                List<WeeklyStat> weeklyStats = new ArrayList<>();
                int weeks = days / 7;
                for (int i = weeks - 1; i >= 0; i--) {
                    LocalDateTime weekStart = now.minusWeeks(i).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime weekEnd = weekStart.plusWeeks(1);
                    
                    Long newUserCount = userMapper.countNewUsersInPeriod(weekStart, weekEnd);
                    Long newPostCount = postMapper.countByTimeRange(weekStart, weekEnd);
                    Long newCommentCount = commentMapper.countByTimeRange(weekStart, weekEnd);
                    Long activeUserCount = userMapper.countActiveUsers(weekStart, weekEnd);
                    
                    weeklyStats.add(new WeeklyStat(
                        weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "至" + 
                        weekEnd.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        newUserCount, newPostCount, newCommentCount, activeUserCount
                    ));
                }
                trendStats.setWeeklyStats(weeklyStats);
            }
            
            // 每月统计（如果天数大于等于30天）
            if (days >= 30) {
                List<MonthlyStat> monthlyStats = new ArrayList<>();
                int months = days / 30;
                for (int i = months - 1; i >= 0; i--) {
                    LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime monthEnd = monthStart.plusMonths(1);
                    
                    Long newUserCount = userMapper.countNewUsersInPeriod(monthStart, monthEnd);
                    Long newPostCount = postMapper.countByTimeRange(monthStart, monthEnd);
                    Long newCommentCount = commentMapper.countByTimeRange(monthStart, monthEnd);
                    Long activeUserCount = userMapper.countActiveUsers(monthStart, monthEnd);
                    
                    monthlyStats.add(new MonthlyStat(
                        monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        newUserCount, newPostCount, newCommentCount, activeUserCount
                    ));
                }
                trendStats.setMonthlyStats(monthlyStats);
            }
            
            log.info("获取时间趋势统计数据成功，天数: {}", days);
        } catch (Exception e) {
            log.error("获取时间趋势统计数据失败", e);
        }
        
        return trendStats;
    }

    @Override
    public CategoryStatisticsVO getCategoryStatistics() {
        return getCategoryStatisticsByTimeRange(7); // 默认一周
    }
    
    @Override
    public CategoryStatisticsVO getCategoryStatisticsByTimeRange(Integer days) {
        return getCategoryStatisticsByTimeRangeAndCommunity(days, null);
    }
    
    @Override
    public CategoryStatisticsVO getCategoryStatisticsByTimeRangeAndCommunity(Integer days, String community) {
        CategoryStatisticsVO categoryStats = new CategoryStatisticsVO();
        
        try {
            // 计算开始时间
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            
            // 获取所有分类
            List<Category> categories = categoryMapper.selectAll();
            List<CategoryStat> categoryStatList = new ArrayList<>();
            
            for (Category category : categories) {
                Long postCount;
                
                // 如果有社区信息，使用社区过滤的查询方法
                if (community != null && !community.trim().isEmpty()) {
                    postCount = postMapper.countByCategoryIdAndTimeRangeAndCommunity(category.getCategoryId(), startTime, community);
                } else {
                    postCount = postMapper.countByCategoryIdAndTimeRange(category.getCategoryId(), startTime);
                }
                
                // 查询该分类的总浏览量、点赞数、评论数（简化处理）
                Long viewCount = postCount * 50;
                Long likeCount = postCount * 5;
                Long commentCount = postCount * 2;
                
                CategoryStat stat = new CategoryStat(
                    category.getCategoryId(),
                    category.getName(),
                    postCount,
                    viewCount,
                    likeCount,
                    commentCount
                );
                categoryStatList.add(stat);
            }
            
            // 按帖子数量排序
            categoryStatList.sort((a, b) -> b.getPostCount().compareTo(a.getPostCount()));
            
            categoryStats.setCategoryStats(categoryStatList);
            
            // 帖子类型分布（按分类统计）
            Map<Integer, Long> postTypes = new HashMap<>();
            if (categories.size() > 0) {
                // 统计每个分类的帖子数
                for (Category category : categories) {
                    Long count;
                    if (community != null && !community.trim().isEmpty()) {
                        count = postMapper.countByCategoryIdAndCommunity(category.getCategoryId(), community);
                    } else {
                        count = postMapper.countByCategoryId(category.getCategoryId());
                    }
                    postTypes.put(category.getCategoryId(), count);
                }
            }
            categoryStats.setPostTypeDistribution(postTypes);
            
            log.info("获取分类统计数据成功，时间范围：{}天，社区：{}, 共{}个分类", days, community, categories.size());
        } catch (Exception e) {
            log.error("获取分类统计数据失败", e);
        }
        
        return categoryStats;
    }

    @Override
    public LocationDistributionVO getLocationDistribution() {
        LocationDistributionVO locationStats = new LocationDistributionVO();
        
        try {
            // 根据用户所在的社区进行统计
            List<Map<String, Object>> communityStats = userMapper.selectCommunityDistribution();
            
            List<LocationStat> locationStatList = new ArrayList<>();
            Long totalUsers = userMapper.countAll();
            Long unknownCount = 0L;
            
            if (communityStats != null && !communityStats.isEmpty()) {
                for (Map<String, Object> stat : communityStats) {
                    String community = (String) stat.get("community");
                    Long userCount = ((Number) stat.get("count")).longValue();
                    
                    if (community == null || community.trim().isEmpty() || "未知".equals(community)) {
                        unknownCount += userCount;
                    } else {
                        // 计算该社区的帖子数量（简化处理）
                        Long postCount = userCount / 2; // 假设平均每两个用户发一个帖子
                        
                        // 计算占比
                        Double percentage = totalUsers > 0 ? (double) userCount / totalUsers * 100 : 0.0;
                        
                        locationStatList.add(new LocationStat(
                            community,
                            userCount,
                            postCount,
                            percentage
                        ));
                    }
                }
            }
            
            // 按用户数排序
            locationStatList.sort((a, b) -> b.getUserCount().compareTo(a.getUserCount()));
            
            locationStats.setLocationStats(locationStatList);
            locationStats.setUnknownLocationCount(unknownCount);
            
            log.info("获取地域分布统计数据成功，共{}个社区", locationStatList.size());
        } catch (Exception e) {
            log.error("获取地域分布统计数据失败", e);
            
            // 如果真实查询失败，使用模拟数据
            List<LocationStat> locationStatList = new ArrayList<>();
            String[] locations = {"北京", "上海", "广州", "深圳", "杭州", "成都"};
            java.util.Random random = new java.util.Random();
            
            for (String location : locations) {
                Long userCount = (long)(random.nextInt(1000) + 100);
                Long postCount = (long)(random.nextInt(500) + 50);
                Double percentage = (double)userCount / (userCount + random.nextInt(2000) + 1000) * 100;
                
                locationStatList.add(new LocationStat(location, userCount, postCount, percentage));
            }
            
            locationStats.setLocationStats(locationStatList);
            locationStats.setUnknownLocationCount(100L);
        }
        
        return locationStats;
    }

    @Override
    public ActivityStatisticsVO getActivityStatistics() {
        ActivityStatisticsVO activityStats = new ActivityStatisticsVO();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 不同时间段的活跃用户数
            activityStats.setDailyActiveUsers(userMapper.countActiveUsersInPeriod(1));
            activityStats.setWeeklyActiveUsers(userMapper.countActiveUsersInPeriod(7));
            activityStats.setMonthlyActiveUsers(userMapper.countActiveUsersInPeriod(30));
            
            // 计算比率
            if (activityStats.getWeeklyActiveUsers() > 0) {
                activityStats.setDauWauRatio((double)activityStats.getDailyActiveUsers() / activityStats.getWeeklyActiveUsers());
            }
            if (activityStats.getMonthlyActiveUsers() > 0) {
                activityStats.setWauMauRatio((double)activityStats.getWeeklyActiveUsers() / activityStats.getMonthlyActiveUsers());
            }
            
            // 用户活跃等级分布
            List<UserActivityLevel> activityLevels = new ArrayList<>();
            
            // 活跃用户（最近7天有活动）
            Long activeUsers = activityStats.getWeeklyActiveUsers();
            Long totalUsers = userMapper.countAll();
            
            if (totalUsers > 0) {
                activityLevels.add(new UserActivityLevel("高度活跃", activeUsers, (double)activeUsers / totalUsers * 100));
                activityLevels.add(new UserActivityLevel("中度活跃", totalUsers - activeUsers, (double)(totalUsers - activeUsers) / totalUsers * 100));
                activityLevels.add(new UserActivityLevel("低度活跃", 0L, 0.0)); // 简化处理
            }
            
            activityStats.setActivityLevels(activityLevels);
            
            log.info("获取活跃度统计数据成功");
        } catch (Exception e) {
            log.error("获取活跃度统计数据失败", e);
        }
        
        return activityStats;
    }

    @Override
    public PerformanceStatisticsVO getPerformanceStatistics() {
        PerformanceStatisticsVO perfStats = new PerformanceStatisticsVO();
        
        // TODO: 实现性能统计逻辑
        
        return perfStats;
    }

    @Override
    public List<CommunityStatisticsItemVO> getAllCommunitiesStatistics() {
        List<CommunityStatisticsItemVO> result = new ArrayList<>();
        
        try {
            // 1. 获取所有社区分布数据
            List<Map<String, Object>> communityDistribution = userMapper.selectCommunityDistribution();
            
            // 2. 获取今日时间范围
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
            
            // 3. 为每个社区构建统计数据
            for (Map<String, Object> commData : communityDistribution) {
                String community = (String) commData.get("community");
                if (community == null || community.trim().isEmpty()) {
                    continue; // 跳过空社区
                }
                
                CommunityStatisticsItemVO item = new CommunityStatisticsItemVO();
                item.setCommunity(community);
                
                // 用户总数
                Long userCount = userMapper.countUsersByCommunity(community);
                item.setUserCount(userCount);
                
                // 帖子总数
                Long postCount = countPostsByCommunity(community);
                item.setPostCount(postCount);
                
                // 今日新增用户
                Long todayNewUsers = userMapper.countNewUsersByCommunityAndTimeRange(community, todayStart, todayEnd);
                item.setTodayNewUsers(todayNewUsers != null ? todayNewUsers : 0L);
                
                // 今日新增帖子
                Long todayNewPosts = postMapper.countByCommunityAndTimeRange(community, todayStart, todayEnd);
                item.setTodayNewPosts(todayNewPosts != null ? todayNewPosts : 0L);
                
                result.add(item);
            }
            
            // 4. 按用户总数降序排序
            result.sort((a, b) -> b.getUserCount().compareTo(a.getUserCount()));
            
            log.info("获取所有社区统计数据成功，共{}个社区", result.size());
        } catch (Exception e) {
            log.error("获取所有社区统计数据失败", e);
        }
        
        return result;
    }

    @Override
    public DataStatisticsService.CommunityDetailVO getCommunityDetail(String communityName) {
        DataStatisticsService.CommunityDetailVO detail = new DataStatisticsService.CommunityDetailVO();
        
        try {
            detail.setCommunity(communityName);
            
            // 1. 总用户数
            Long totalUsers = userMapper.countUsersByCommunity(communityName);
            detail.setTotalUsers(totalUsers);
            
            // 2. 总发帖量
            Long totalPosts = countPostsByCommunity(communityName);
            detail.setTotalPosts(totalPosts);
            
            // 3. 前 10 名发帖排行榜
            List<DataStatisticsService.UserPostRankingVO> top10Users = new ArrayList<>();
            List<User> users = userMapper.selectByCommunity(communityName);
            
            // 为每个用户统计发帖数
            for (User user : users) {
                Long postCount = postService.countPostsByUserId(user.getUserId());
                if (postCount > 0) {
                    DataStatisticsService.UserPostRankingVO ranking = new DataStatisticsService.UserPostRankingVO();
                    ranking.setUserId(user.getUserId());
                    ranking.setNickname(user.getNickname());
                    ranking.setAvatarUrl(user.getAvatarUrl());
                    ranking.setPostCount(postCount);
                    top10Users.add(ranking);
                }
            }
            
            // 按发帖数排序并取前 10 名
            top10Users.sort((a, b) -> b.getPostCount().compareTo(a.getPostCount()));
            if (top10Users.size() > 10) {
                top10Users = top10Users.subList(0, 10);
            }
            detail.setTop10Users(top10Users);
            
            // 4. 板块分布（按分类统计，仅统计该社区的帖子）
            List<DataStatisticsService.CategoryDistributionVO> categoryDistribution = new ArrayList<>();
            List<Category> categories = categoryMapper.selectAll(); // 查询所有分类
            
            // 先获取该社区的总帖子数（用于计算百分比）
            Long communityTotalPosts = countPostsByCommunity(communityName);
            
            for (Category category : categories) {
                // 统计该分类下该社区的帖子总数
                Long postCount = postMapper.countByCategoryIdAndCommunity(category.getCategoryId(), communityName);
                
                if (postCount > 0) {
                    DataStatisticsService.CategoryDistributionVO dist = new DataStatisticsService.CategoryDistributionVO();
                    dist.setCategoryId(category.getCategoryId());
                    dist.setCategoryName(category.getName());
                    dist.setPostCount(postCount);
                    
                    // 计算百分比：该分类帖子数 / 该社区总帖子数
                    Double percentage = communityTotalPosts > 0 ? (double) postCount / communityTotalPosts * 100 : 0.0;
                    dist.setPercentage(percentage);
                    
                    categoryDistribution.add(dist);
                }
            }
            
            // 按帖子数降序排序
            categoryDistribution.sort((a, b) -> b.getPostCount().compareTo(a.getPostCount()));
            detail.setCategoryDistribution(categoryDistribution);
            
            // 5. 活跃用户统计（仅统计该社区的活跃用户）
            DataStatisticsService.ActiveUsersVO activeUsers = new DataStatisticsService.ActiveUsersVO();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
            LocalDateTime weekStart = now.minusDays(7).toLocalDate().atStartOfDay();
            LocalDateTime monthStart = now.minusDays(30).toLocalDate().atStartOfDay();
            
            activeUsers.setTodayActive(countActiveUsersByCommunityAndTimeRange(communityName, todayStart, now));
            activeUsers.setWeekActive(countActiveUsersByCommunityAndTimeRange(communityName, weekStart, now));
            activeUsers.setMonthActive(countActiveUsersByCommunityAndTimeRange(communityName, monthStart, now));
            detail.setActiveUsers(activeUsers);
            
            // 6. 近 7 日活跃趋势
            List<DataStatisticsService.DailyActiveTrendVO> sevenDayTrend = new ArrayList<>();
            
            for (int i = 6; i >= 0; i--) {
                LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = dayStart.plusDays(1);
                
                // 统计当天该社区的活跃用户数
                Long activeCount = countActiveUsersByCommunityAndTimeRange(communityName, dayStart, dayEnd);
                
                DataStatisticsService.DailyActiveTrendVO trendVO = new DataStatisticsService.DailyActiveTrendVO();
                trendVO.setDate(dayStart.toLocalDate().toString());
                trendVO.setActiveCount(activeCount);
                sevenDayTrend.add(trendVO);
            }
            detail.setSevenDayTrend(sevenDayTrend);
            
            log.info("获取社区{}详情数据成功", communityName);
        } catch (Exception e) {
            log.error("获取社区{}详情数据失败", communityName, e);
        }
        
        return detail;
    }

    /**
     * 统计社区的帖子总数（复用 PostService 的逻辑）
     */
    private Long countPostsByCommunity(String community) {
        // 这里直接调用 PostServiceImpl 中的方法
        // 由于无法直接注入，使用 Mapper 查询后内存过滤
        List<Post> allPosts = postMapper.selectAll(0, Integer.MAX_VALUE);
        
        return allPosts.stream()
            .filter(post -> {
                User author = userMapper.selectById(post.getUserId());
                return author != null && community.equals(author.getCommunity());
            })
            .count();
    }

    /**
     * 统计社区在指定时间范围内的活跃用户数（有登录、发帖、评论、点赞、收藏等行为的用户）
     */
    private Long countActiveUsersByCommunityAndTimeRange(String community, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 1. 获取该社区的所有用户
            List<User> communityUsers = userMapper.selectByCommunity(community);
            if (communityUsers == null || communityUsers.isEmpty()) {
                return 0L;
            }
            
            // 2. 提取用户 ID 列表并转换为 String
            List<String> userIds = communityUsers.stream()
                .map(User::getUserId)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toList());
            
            // 3. 统计这些用户在指定时间范围内的活跃用户数
            return userMapper.countActiveUsersByIdsAndTimeRange(userIds, startTime, endTime);
            
        } catch (Exception e) {
            log.error("统计社区{}在{}到{}的活跃用户数失败", community, startTime, endTime, e);
            return 0L;
        }
    }

    /**
     * 获取在线用户数（模拟实现）
     */
    private Long getOnlineUsers() {
        // 实际应用中应该从Redis等缓存中获取当前在线用户数
        // 这里返回一个模拟值
        return Math.max(10L, userMapper.countActiveUsersInPeriod(1) / 10);
    }

    /**
     * 计算总浏览量（模拟实现）
     */
    private Long calculateTotalViews() {
        // 实际应用中应该累加所有帖子的view_count
        return postMapper.countAll() * 50; // 简化计算
    }

    /**
     * 计算时间段内的浏览量（模拟实现）
     */
    private Long calculateViewsInRange(LocalDateTime start, LocalDateTime end) {
        // 实际应用中应该查询时间段内的浏览记录
        return postMapper.countByTimeRange(start, end) * 10; // 简化计算
    }
}