package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 评论Mapper接口
 */
@Mapper
public interface CommentMapper {
    
    /**
     * 根据评论ID查询评论
     */
    Comment selectById(@Param("commentId") Long commentId);
    
    /**
     * 根据帖子ID查询评论列表
     */
    List<Comment> selectByPostId(@Param("postId") Long postId,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 根据用户ID查询评论列表
     */
    List<Comment> selectByUserId(@Param("userId") Long userId,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 根据父评论ID查询子评论
     */
    List<Comment> selectByParentId(@Param("parentId") String parentId,
                                  @Param("offset") Integer offset,
                                  @Param("limit") Integer limit);
    
    /**
     * 查询根评论（没有父评论的评论）
     */
    List<Comment> selectRootComments(@Param("postId") Long postId,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);
    
    /**
     * 统计帖子评论总数
     */
    Long countByPostId(@Param("postId") Long postId);
    
    /**
     * 统计用户评论总数
     */
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 根据状态统计评论数量
     */
    Long countByStatus(@Param("status") Integer status);
    
    /**
     * 插入评论
     */
    int insert(Comment comment);
    
    /**
     * 更新评论
     */
    int update(Comment comment);
    
    /**
     * 更新评论状态
     */
    int updateStatus(@Param("commentId") Long commentId, @Param("status") Integer status);
    
    /**
     * 增加点赞数
     */
    int incrementLikeCount(@Param("commentId") Long commentId);
    
    /**
     * 减少点赞数
     */
    int decrementLikeCount(@Param("commentId") Long commentId);
    
    /**
     * 删除评论
     */
    int deleteById(@Param("commentId") Long commentId);
    
    /**
     * 根据帖子ID删除所有评论
     */
    int deleteByPostId(@Param("postId") Long postId);
    
    /**
     * 统计评论总数
     */
    Long countAll();
    
    /**
     * 查询所有评论（管理员使用）
     */
    List<Comment> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 根据关键词搜索评论
     */
    List<Comment> selectByKeyword(@Param("keyword") String keyword,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);
    
    /**
     * 统计关键词搜索的评论数
     */
    Long countByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计指定时间段内的评论数
     */
    Long countByTimeRange(@Param("startTime") java.time.LocalDateTime startTime, 
                         @Param("endTime") java.time.LocalDateTime endTime);
}