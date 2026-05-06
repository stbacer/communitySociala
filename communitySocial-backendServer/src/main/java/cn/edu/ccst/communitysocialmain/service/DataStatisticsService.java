package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.vo.PageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务接口
 */
public interface DataStatisticsService {

    /**
     * 获取平台总览统计数据
     */
    PlatformOverviewVO getPlatformOverview();

    /**
     * 获取用户统计信息
     */
    UserStatisticsVO getUserStatistics();

    /**
     * 获取内容统计信息
     */
    ContentStatisticsVO getContentStatistics();

    /**
     * 获取互动统计信息
     */
    InteractionStatisticsVO getInteractionStatistics();

    /**
     * 获取时间趋势统计
     */
    TimeTrendVO getTimeTrendStatistics(Integer days);

    /**
     * 获取分类统计信息
     */
    CategoryStatisticsVO getCategoryStatistics();
    
    /**
     * 根据时间范围获取分类统计信息
     * @param days 天数（1=今日，7=一周，30=一月）
     * @return 分类统计数据
     */
    CategoryStatisticsVO getCategoryStatisticsByTimeRange(Integer days);
    
    /**
     * 根据时间范围和社区获取分类统计数据（社区管理员用）
     * @param days 天数
     * @param community 社区名称
     * @return 分类统计数据
     */
    CategoryStatisticsVO getCategoryStatisticsByTimeRangeAndCommunity(Integer days, String community);

    /**
     * 获取地域分布统计
     */
    LocationDistributionVO getLocationDistribution();

    /**
     * 获取活跃度统计
     */
    ActivityStatisticsVO getActivityStatistics();

    /**
     * 获取系统性能统计
     */
    PerformanceStatisticsVO getPerformanceStatistics();

    /**
     * 获取所有社区的统计数据列表
     * @return 社区统计数据列表
     */
    List<CommunityStatisticsItemVO> getAllCommunitiesStatistics();

    /**
     * 获取指定社区的详细统计信息
     * @param communityName 社区名称
     * @return 社区详细统计信息
     */
    CommunityDetailVO getCommunityDetail(String communityName);

    /**
     * 社区统计项 VO
     */
    class CommunityStatisticsItemVO {
        private String community;       // 社区名称
        private Long userCount;         // 用户总数
        private Long postCount;         // 帖子总数
        private Long todayNewUsers;     // 今日新增用户
        private Long todayNewPosts;     // 今日新增帖子
        
        // Getters and Setters
        public String getCommunity() { return community; }
        public void setCommunity(String community) { this.community = community; }
        
        public Long getUserCount() { return userCount; }
        public void setUserCount(Long userCount) { this.userCount = userCount; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Long getTodayNewUsers() { return todayNewUsers; }
        public void setTodayNewUsers(Long todayNewUsers) { this.todayNewUsers = todayNewUsers; }
        
        public Long getTodayNewPosts() { return todayNewPosts; }
        public void setTodayNewPosts(Long todayNewPosts) { this.todayNewPosts = todayNewPosts; }
    }

    /**
     * 社区详情 VO（包含更详细的统计信息）
     */
    class CommunityDetailVO {
        private String community;               // 社区名称
        private Long totalUsers;                // 总用户数
        private Long totalPosts;                // 总发帖量
        private List<UserPostRankingVO> top10Users;  // 前 10 名发帖排行榜
        private List<CategoryDistributionVO> categoryDistribution;  // 板块分布
        private ActiveUsersVO activeUsers;      // 活跃用户统计
        private List<DailyActiveTrendVO> sevenDayTrend;  // 近 7 日活跃趋势
        
        // Getters and Setters
        public String getCommunity() { return community; }
        public void setCommunity(String community) { this.community = community; }
        
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getTotalPosts() { return totalPosts; }
        public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
        
        public List<UserPostRankingVO> getTop10Users() { return top10Users; }
        public void setTop10Users(List<UserPostRankingVO> top10Users) { this.top10Users = top10Users; }
        
        public List<CategoryDistributionVO> getCategoryDistribution() { return categoryDistribution; }
        public void setCategoryDistribution(List<CategoryDistributionVO> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
        
        public ActiveUsersVO getActiveUsers() { return activeUsers; }
        public void setActiveUsers(ActiveUsersVO activeUsers) { this.activeUsers = activeUsers; }
        
        public List<DailyActiveTrendVO> getSevenDayTrend() { return sevenDayTrend; }
        public void setSevenDayTrend(List<DailyActiveTrendVO> sevenDayTrend) { this.sevenDayTrend = sevenDayTrend; }
    }

    /**
     * 活跃用户统计 VO
     */
    class ActiveUsersVO {
        private Long todayActive;     // 今日活跃用户
        private Long weekActive;      // 近 7 日活跃用户
        private Long monthActive;     // 月活跃用户
        
        // Getters and Setters
        public Long getTodayActive() { return todayActive; }
        public void setTodayActive(Long todayActive) { this.todayActive = todayActive; }
        
        public Long getWeekActive() { return weekActive; }
        public void setWeekActive(Long weekActive) { this.weekActive = weekActive; }
        
        public Long getMonthActive() { return monthActive; }
        public void setMonthActive(Long monthActive) { this.monthActive = monthActive; }
    }

    /**
     * 每日活跃趋势 VO
     */
    class DailyActiveTrendVO {
        private String date;        // 日期
        private Long activeCount;   // 活跃用户数
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getActiveCount() { return activeCount; }
        public void setActiveCount(Long activeCount) { this.activeCount = activeCount; }
    }

    /**
     * 用户发帖排行 VO
     */
    class UserPostRankingVO {
        private Long userId;
        private String nickname;
        private String avatarUrl;
        private Long postCount;
        
        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
    }

    /**
     * 分类分布 VO
     */
    class CategoryDistributionVO {
        private Integer categoryId;
        private String categoryName;
        private Long postCount;
        private Double percentage;
        
        // Getters and Setters
        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    /**
     * 平台总览统计VO
     */
    class PlatformOverviewVO {
        private Long totalUsers;        // 总用户数
        private Long communityCount;    // 社区数量
        private Long totalPosts;        // 总帖子数
        private Long totalComments;     // 总评论数
        private Long totalLikes;        // 总点赞数
        private Long onlineUsers;       // 在线用户数
        private Long todayActiveUsers;  // 今日活跃用户数
        private Long todayPosts;        // 今日新增帖子数
        private Long todayComments;     // 今日新增评论数
        
        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getCommunityCount() { return communityCount; }
        public void setCommunityCount(Long communityCount) { this.communityCount = communityCount; }
        
        public Long getTotalPosts() { return totalPosts; }
        public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
        
        public Long getTotalComments() { return totalComments; }
        public void setTotalComments(Long totalComments) { this.totalComments = totalComments; }
        
        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }
        
        public Long getOnlineUsers() { return onlineUsers; }
        public void setOnlineUsers(Long onlineUsers) { this.onlineUsers = onlineUsers; }
        
        public Long getTodayActiveUsers() { return todayActiveUsers; }
        public void setTodayActiveUsers(Long todayActiveUsers) { this.todayActiveUsers = todayActiveUsers; }
        
        public Long getTodayPosts() { return todayPosts; }
        public void setTodayPosts(Long todayPosts) { this.todayPosts = todayPosts; }
        
        public Long getTodayComments() { return todayComments; }
        public void setTodayComments(Long todayComments) { this.todayComments = todayComments; }
    }

    /**
     * 用户统计VO
     */
    class UserStatisticsVO {
        private Long totalUsers;        // 总用户数
        private Long activeUsers;       // 活跃用户数
        private Long newUsersToday;     // 今日新增用户
        private Long newUsersWeek;      // 本周新增用户
        private Long newUsersMonth;     // 本月新增用户
        private Double userGrowthRate;  // 用户增长率(%)
        private List<UserGrowthTrend> growthTrend; // 用户增长趋势
        private Map<String, Long> userRoles; // 用户角色分布
        private Map<String, Long> userStatus; // 用户状态分布
        
        // Getters and Setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getActiveUsers() { return activeUsers; }
        public void setActiveUsers(Long activeUsers) { this.activeUsers = activeUsers; }
        
        public Long getNewUsersToday() { return newUsersToday; }
        public void setNewUsersToday(Long newUsersToday) { this.newUsersToday = newUsersToday; }
        
        public Long getNewUsersWeek() { return newUsersWeek; }
        public void setNewUsersWeek(Long newUsersWeek) { this.newUsersWeek = newUsersWeek; }
        
        public Long getNewUsersMonth() { return newUsersMonth; }
        public void setNewUsersMonth(Long newUsersMonth) { this.newUsersMonth = newUsersMonth; }
        
        public Double getUserGrowthRate() { return userGrowthRate; }
        public void setUserGrowthRate(Double userGrowthRate) { this.userGrowthRate = userGrowthRate; }
        
        public List<UserGrowthTrend> getGrowthTrend() { return growthTrend; }
        public void setGrowthTrend(List<UserGrowthTrend> growthTrend) { this.growthTrend = growthTrend; }
        
        public Map<String, Long> getUserRoles() { return userRoles; }
        public void setUserRoles(Map<String, Long> userRoles) { this.userRoles = userRoles; }
        
        public Map<String, Long> getUserStatus() { return userStatus; }
        public void setUserStatus(Map<String, Long> userStatus) { this.userStatus = userStatus; }
    }

    /**
     * 用户增长趋势
     */
    class UserGrowthTrend {
        private String date;    // 日期
        private Long count;     // 新增用户数
        
        public UserGrowthTrend() {}
        
        public UserGrowthTrend(String date, Long count) {
            this.date = date;
            this.count = count;
        }
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 内容统计VO
     */
    class ContentStatisticsVO {
        private Long totalPosts;        // 总帖子数
        private Long publishedPosts;    // 已发布帖子数
        private Long pendingPosts;      // 待审核帖子数
        private Long deletedPosts;      // 已删除帖子数
        private Map<Integer, Long> postTypes; // 帖子类型分布
        private Map<Integer, Long> postStatus; // 帖子状态分布
        private List<DailyPostStat> dailyPosts; // 每日发帖统计
        private Double avgPostsPerDay;  // 日均发帖数
        
        // Getters and Setters
        public Long getTotalPosts() { return totalPosts; }
        public void setTotalPosts(Long totalPosts) { this.totalPosts = totalPosts; }
        
        public Long getPublishedPosts() { return publishedPosts; }
        public void setPublishedPosts(Long publishedPosts) { this.publishedPosts = publishedPosts; }
        
        public Long getPendingPosts() { return pendingPosts; }
        public void setPendingPosts(Long pendingPosts) { this.pendingPosts = pendingPosts; }
        
        public Long getDeletedPosts() { return deletedPosts; }
        public void setDeletedPosts(Long deletedPosts) { this.deletedPosts = deletedPosts; }
        
        public Map<Integer, Long> getPostTypes() { return postTypes; }
        public void setPostTypes(Map<Integer, Long> postTypes) { this.postTypes = postTypes; }
        
        public Map<Integer, Long> getPostStatus() { return postStatus; }
        public void setPostStatus(Map<Integer, Long> postStatus) { this.postStatus = postStatus; }
        
        public List<DailyPostStat> getDailyPosts() { return dailyPosts; }
        public void setDailyPosts(List<DailyPostStat> dailyPosts) { this.dailyPosts = dailyPosts; }
        
        public Double getAvgPostsPerDay() { return avgPostsPerDay; }
        public void setAvgPostsPerDay(Double avgPostsPerDay) { this.avgPostsPerDay = avgPostsPerDay; }
    }

    /**
     * 每日发帖统计
     */
    class DailyPostStat {
        private String date;    // 日期
        private Long count;     // 发帖数
        
        public DailyPostStat() {}
        
        public DailyPostStat(String date, Long count) {
            this.date = date;
            this.count = count;
        }
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    /**
     * 互动统计VO
     */
    class InteractionStatisticsVO {
        private Long totalViews;        // 总浏览量
        private Long totalLikes;        // 总点赞数
        private Long totalComments;     // 总评论数
        private Long totalCollections;  // 总收藏数
        private Long totalShares;       // 总分享数
        private Double avgViewsPerPost; // 平均每帖浏览量
        private Double avgLikesPerPost; // 平均每帖点赞数
        private Double avgCommentsPerPost; // 平均每帖评论数
        private List<DailyInteractionStat> dailyInteractions; // 每日互动统计
        
        // Getters and Setters
        public Long getTotalViews() { return totalViews; }
        public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }
        
        public Long getTotalLikes() { return totalLikes; }
        public void setTotalLikes(Long totalLikes) { this.totalLikes = totalLikes; }
        
        public Long getTotalComments() { return totalComments; }
        public void setTotalComments(Long totalComments) { this.totalComments = totalComments; }
        
        public Long getTotalCollections() { return totalCollections; }
        public void setTotalCollections(Long totalCollections) { this.totalCollections = totalCollections; }
        
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Double getAvgViewsPerPost() { return avgViewsPerPost; }
        public void setAvgViewsPerPost(Double avgViewsPerPost) { this.avgViewsPerPost = avgViewsPerPost; }
        
        public Double getAvgLikesPerPost() { return avgLikesPerPost; }
        public void setAvgLikesPerPost(Double avgLikesPerPost) { this.avgLikesPerPost = avgLikesPerPost; }
        
        public Double getAvgCommentsPerPost() { return avgCommentsPerPost; }
        public void setAvgCommentsPerPost(Double avgCommentsPerPost) { this.avgCommentsPerPost = avgCommentsPerPost; }
        
        public List<DailyInteractionStat> getDailyInteractions() { return dailyInteractions; }
        public void setDailyInteractions(List<DailyInteractionStat> dailyInteractions) { this.dailyInteractions = dailyInteractions; }
    }

    /**
     * 每日互动统计
     */
    class DailyInteractionStat {
        private String date;        // 日期
        private Long views;         // 浏览量
        private Long likes;         // 点赞数
        private Long comments;      // 评论数
        private Long collections;   // 收藏数
        
        public DailyInteractionStat() {}
        
        public DailyInteractionStat(String date, Long views, Long likes, Long comments, Long collections) {
            this.date = date;
            this.views = views;
            this.likes = likes;
            this.comments = comments;
            this.collections = collections;
        }
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getViews() { return views; }
        public void setViews(Long views) { this.views = views; }
        
        public Long getLikes() { return likes; }
        public void setLikes(Long likes) { this.likes = likes; }
        
        public Long getComments() { return comments; }
        public void setComments(Long comments) { this.comments = comments; }
        
        public Long getCollections() { return collections; }
        public void setCollections(Long collections) { this.collections = collections; }
    }

    /**
     * 时间趋势统计VO
     */
    class TimeTrendVO {
        private List<DailyStat> dailyStats;     // 每日统计
        private List<WeeklyStat> weeklyStats;   // 每周统计
        private List<MonthlyStat> monthlyStats; // 每月统计
        
        // Getters and Setters
        public List<DailyStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailyStat> dailyStats) { this.dailyStats = dailyStats; }
        
        public List<WeeklyStat> getWeeklyStats() { return weeklyStats; }
        public void setWeeklyStats(List<WeeklyStat> weeklyStats) { this.weeklyStats = weeklyStats; }
        
        public List<MonthlyStat> getMonthlyStats() { return monthlyStats; }
        public void setMonthlyStats(List<MonthlyStat> monthlyStats) { this.monthlyStats = monthlyStats; }
    }

    /**
     * 每日统计
     */
    class DailyStat {
        private String date;        // 日期
        private Long newUserCount;  // 新增用户数
        private Long newPostCount;  // 新增帖子数
        private Long newCommentCount; // 新增评论数
        private Long activeUserCount; // 活跃用户数
        
        public DailyStat() {}
        
        public DailyStat(String date, Long newUserCount, Long newPostCount, Long newCommentCount, Long activeUserCount) {
            this.date = date;
            this.newUserCount = newUserCount;
            this.newPostCount = newPostCount;
            this.newCommentCount = newCommentCount;
            this.activeUserCount = activeUserCount;
        }
        
        // Getters and Setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getNewUserCount() { return newUserCount; }
        public void setNewUserCount(Long newUserCount) { this.newUserCount = newUserCount; }
        
        public Long getNewPostCount() { return newPostCount; }
        public void setNewPostCount(Long newPostCount) { this.newPostCount = newPostCount; }
        
        public Long getNewCommentCount() { return newCommentCount; }
        public void setNewCommentCount(Long newCommentCount) { this.newCommentCount = newCommentCount; }
        
        public Long getActiveUserCount() { return activeUserCount; }
        public void setActiveUserCount(Long activeUserCount) { this.activeUserCount = activeUserCount; }
    }

    /**
     * 每周统计
     */
    class WeeklyStat {
        private String week;        // 周份
        private Long newUserCount;  // 新增用户数
        private Long newPostCount;  // 新增帖子数
        private Long newCommentCount; // 新增评论数
        private Long activeUserCount; // 活跃用户数
        
        public WeeklyStat() {}
        
        public WeeklyStat(String week, Long newUserCount, Long newPostCount, Long newCommentCount, Long activeUserCount) {
            this.week = week;
            this.newUserCount = newUserCount;
            this.newPostCount = newPostCount;
            this.newCommentCount = newCommentCount;
            this.activeUserCount = activeUserCount;
        }
        
        // Getters and Setters
        public String getWeek() { return week; }
        public void setWeek(String week) { this.week = week; }
        
        public Long getNewUserCount() { return newUserCount; }
        public void setNewUserCount(Long newUserCount) { this.newUserCount = newUserCount; }
        
        public Long getNewPostCount() { return newPostCount; }
        public void setNewPostCount(Long newPostCount) { this.newPostCount = newPostCount; }
        
        public Long getNewCommentCount() { return newCommentCount; }
        public void setNewCommentCount(Long newCommentCount) { this.newCommentCount = newCommentCount; }
        
        public Long getActiveUserCount() { return activeUserCount; }
        public void setActiveUserCount(Long activeUserCount) { this.activeUserCount = activeUserCount; }
    }

    /**
     * 每月统计
     */
    class MonthlyStat {
        private String month;       // 月份
        private Long newUserCount;  // 新增用户数
        private Long newPostCount;  // 新增帖子数
        private Long newCommentCount; // 新增评论数
        private Long activeUserCount; // 活跃用户数
        
        public MonthlyStat() {}
        
        public MonthlyStat(String month, Long newUserCount, Long newPostCount, Long newCommentCount, Long activeUserCount) {
            this.month = month;
            this.newUserCount = newUserCount;
            this.newPostCount = newPostCount;
            this.newCommentCount = newCommentCount;
            this.activeUserCount = activeUserCount;
        }
        
        // Getters and Setters
        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        
        public Long getNewUserCount() { return newUserCount; }
        public void setNewUserCount(Long newUserCount) { this.newUserCount = newUserCount; }
        
        public Long getNewPostCount() { return newPostCount; }
        public void setNewPostCount(Long newPostCount) { this.newPostCount = newPostCount; }
        
        public Long getNewCommentCount() { return newCommentCount; }
        public void setNewCommentCount(Long newCommentCount) { this.newCommentCount = newCommentCount; }
        
        public Long getActiveUserCount() { return activeUserCount; }
        public void setActiveUserCount(Long activeUserCount) { this.activeUserCount = activeUserCount; }
    }

    /**
     * 分类统计VO
     */
    class CategoryStatisticsVO {
        private List<CategoryStat> categoryStats; // 分类统计
        private Map<Integer, Long> postTypeDistribution; // 帖子类型分布
        
        // Getters and Setters
        public List<CategoryStat> getCategoryStats() { return categoryStats; }
        public void setCategoryStats(List<CategoryStat> categoryStats) { this.categoryStats = categoryStats; }
        
        public Map<Integer, Long> getPostTypeDistribution() { return postTypeDistribution; }
        public void setPostTypeDistribution(Map<Integer, Long> postTypeDistribution) { this.postTypeDistribution = postTypeDistribution; }
    }

    /**
     * 分类统计项
     */
    class CategoryStat {
        private Integer categoryId; // 分类ID
        private String categoryName; // 分类名称
        private Long postCount;     // 帖子数
        private Long viewCount;     // 浏览量
        private Long likeCount;     // 点赞数
        private Long commentCount;  // 评论数
        
        public CategoryStat() {}
        
        public CategoryStat(Integer categoryId, String categoryName, Long postCount, Long viewCount, Long likeCount, Long commentCount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.postCount = postCount;
            this.viewCount = viewCount;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
        }
        
        // Getters and Setters
        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Long getViewCount() { return viewCount; }
        public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
        
        public Long getLikeCount() { return likeCount; }
        public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
        
        public Long getCommentCount() { return commentCount; }
        public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    }

    /**
     * 地域分布统计VO
     */
    class LocationDistributionVO {
        private List<LocationStat> locationStats; // 地域统计
        private Long unknownLocationCount; // 未知位置数量
        
        // Getters and Setters
        public List<LocationStat> getLocationStats() { return locationStats; }
        public void setLocationStats(List<LocationStat> locationStats) { this.locationStats = locationStats; }
        
        public Long getUnknownLocationCount() { return unknownLocationCount; }
        public void setUnknownLocationCount(Long unknownLocationCount) { this.unknownLocationCount = unknownLocationCount; }
    }

    /**
     * 地域统计项
     */
    class LocationStat {
        private String location;    // 地区名称
        private Long userCount;     // 用户数
        private Long postCount;     // 帖子数
        private Double percentage;  // 占比(%)
        
        public LocationStat() {}
        
        public LocationStat(String location, Long userCount, Long postCount, Double percentage) {
            this.location = location;
            this.userCount = userCount;
            this.postCount = postCount;
            this.percentage = percentage;
        }
        
        // Getters and Setters
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public Long getUserCount() { return userCount; }
        public void setUserCount(Long userCount) { this.userCount = userCount; }
        
        public Long getPostCount() { return postCount; }
        public void setPostCount(Long postCount) { this.postCount = postCount; }
        
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    /**
     * 活跃度统计VO
     */
    class ActivityStatisticsVO {
        private Long dailyActiveUsers;  // 日活跃用户
        private Long weeklyActiveUsers; // 周活跃用户
        private Long monthlyActiveUsers; // 月活跃用户
        private Double dauWauRatio;     // DAU/WAU比率
        private Double wauMauRatio;     // WAU/MAU比率
        private List<UserActivityLevel> activityLevels; // 用户活跃等级分布
        
        // Getters and Setters
        public Long getDailyActiveUsers() { return dailyActiveUsers; }
        public void setDailyActiveUsers(Long dailyActiveUsers) { this.dailyActiveUsers = dailyActiveUsers; }
        
        public Long getWeeklyActiveUsers() { return weeklyActiveUsers; }
        public void setWeeklyActiveUsers(Long weeklyActiveUsers) { this.weeklyActiveUsers = weeklyActiveUsers; }
        
        public Long getMonthlyActiveUsers() { return monthlyActiveUsers; }
        public void setMonthlyActiveUsers(Long monthlyActiveUsers) { this.monthlyActiveUsers = monthlyActiveUsers; }
        
        public Double getDauWauRatio() { return dauWauRatio; }
        public void setDauWauRatio(Double dauWauRatio) { this.dauWauRatio = dauWauRatio; }
        
        public Double getWauMauRatio() { return wauMauRatio; }
        public void setWauMauRatio(Double wauMauRatio) { this.wauMauRatio = wauMauRatio; }
        
        public List<UserActivityLevel> getActivityLevels() { return activityLevels; }
        public void setActivityLevels(List<UserActivityLevel> activityLevels) { this.activityLevels = activityLevels; }
    }

    /**
     * 用户活跃等级
     */
    class UserActivityLevel {
        private String level;       // 活跃等级
        private Long userCount;     // 用户数
        private Double percentage;  // 占比(%)
        
        public UserActivityLevel() {}
        
        public UserActivityLevel(String level, Long userCount, Double percentage) {
            this.level = level;
            this.userCount = userCount;
            this.percentage = percentage;
        }
        
        // Getters and Setters
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public Long getUserCount() { return userCount; }
        public void setUserCount(Long userCount) { this.userCount = userCount; }
        
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }

    /**
     * 性能统计VO
     */
    class PerformanceStatisticsVO {
        private Double avgResponseTime; // 平均响应时间(ms)
        private Double maxResponseTime; // 最大响应时间(ms)
        private Double minResponseTime; // 最小响应时间(ms)
        private Long totalRequests;     // 总请求数
        private Long errorRequests;     // 错误请求数
        private Double errorRate;       // 错误率(%)
        private List<ApiPerformanceStat> apiStats; // API性能统计
        
        // Getters and Setters
        public Double getAvgResponseTime() { return avgResponseTime; }
        public void setAvgResponseTime(Double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
        
        public Double getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(Double maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public Double getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(Double minResponseTime) { this.minResponseTime = minResponseTime; }
        
        public Long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }
        
        public Long getErrorRequests() { return errorRequests; }
        public void setErrorRequests(Long errorRequests) { this.errorRequests = errorRequests; }
        
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
        
        public List<ApiPerformanceStat> getApiStats() { return apiStats; }
        public void setApiStats(List<ApiPerformanceStat> apiStats) { this.apiStats = apiStats; }
    }

    /**
     * API性能统计项
     */
    class ApiPerformanceStat {
        private String apiUrl;      // API路径
        private String method;      // 请求方法
        private Double avgTime;     // 平均响应时间
        private Double maxTime;     // 最大响应时间
        private Long requestCount;  // 请求数
        private Long errorCount;    // 错误数
        
        public ApiPerformanceStat() {}
        
        public ApiPerformanceStat(String apiUrl, String method, Double avgTime, Double maxTime, Long requestCount, Long errorCount) {
            this.apiUrl = apiUrl;
            this.method = method;
            this.avgTime = avgTime;
            this.maxTime = maxTime;
            this.requestCount = requestCount;
            this.errorCount = errorCount;
        }
        
        // Getters and Setters
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Double getAvgTime() { return avgTime; }
        public void setAvgTime(Double avgTime) { this.avgTime = avgTime; }
        
        public Double getMaxTime() { return maxTime; }
        public void setMaxTime(Double maxTime) { this.maxTime = maxTime; }
        
        public Long getRequestCount() { return requestCount; }
        public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
        
        public Long getErrorCount() { return errorCount; }
        public void setErrorCount(Long errorCount) { this.errorCount = errorCount; }
    }
}