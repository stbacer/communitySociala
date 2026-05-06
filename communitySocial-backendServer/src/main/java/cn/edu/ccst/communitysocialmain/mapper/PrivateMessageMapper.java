package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 私信Mapper接口
 */
@Mapper
public interface PrivateMessageMapper {
    
    /**
     * 根据消息ID查询消息
     */
    PrivateMessage selectById(@Param("messageId") Long messageId);
    
    /**
     * 查询用户之间的私信对话列表
     */
    List<PrivateMessage> selectConversation(@Param("userId1") String userId1,
                                          @Param("userId2") String userId2,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);
    
    /**
     * 查询用户的私信列表（按最新消息排序）
     */
    List<PrivateMessage> selectUserMessages(@Param("userId") Long userId,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);
    
    /**
     * 查询未读消息数量
     */
    Long countUnreadMessages(@Param("userId") Long userId);
    
    /**
     * 统计两个用户之间的消息总数
     */
    Long countConversationMessages(@Param("userId1") String userId1,
                                  @Param("userId2") String userId2);
    
    /**
     * 插入私信
     */
    int insert(PrivateMessage message);
    
    /**
     * 更新消息状态
     */
    int updateStatus(@Param("messageId") Long messageId, @Param("status") Integer status);
    
    /**
     * 批量更新消息状态为已读
     */
    int batchUpdateToRead(@Param("receiverId") Long receiverId, 
                         @Param("senderId") Long senderId);
    
    /**
     * 删除消息
     */
    int deleteById(@Param("messageId") Long messageId);
    
    /**
     * 撤回消息
     */
    int recallMessage(@Param("messageId") Long messageId);
    
    /**
     * 查询所有消息（管理员使用）
     */
    List<PrivateMessage> selectAllMessages(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 统计会话中的未读消息数
     */
    Long countUnreadInConversation(@Param("userId1") String userId1, @Param("userId2") String userId2);
}