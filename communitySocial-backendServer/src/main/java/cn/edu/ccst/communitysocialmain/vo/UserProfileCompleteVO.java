package cn.edu.ccst.communitysocialmain.vo;

import lombok.Data;
import java.util.List;

/**
 * 用户主页完整信息 VO（用于居民端查看他人主页）
 */
@Data
public class UserProfileCompleteVO {
    
    /**
     * 用户基本信息
     */
    private UserInfoVO userInfo;
    
    /**
     * 统计数据
     */
    private UserStatisticsVO statistics;
    
    /**
     * 帖子列表（第一页）
     */
    private PageVO<PostVO> posts;
    
    /**
     * 关注列表（第一页）
     */
    private PageVO<UserInfoVO> followingList;
    
    /**
     * 粉丝列表（第一页）
     */
    private PageVO<UserInfoVO> followerList;
}
