import apiClient from './index'

/**
 * 获取所有帖子列表
 */
export function getAllPosts(params) {
  return apiClient.get('/sadmin/content/all-posts', { params })
}

/**
 * 获取所有评论列表
 */
export function getAllComments(params) {
  return apiClient.get('/sadmin/content/all-comments', { params })
}

/**
 * 获取所有私信列表
 */
export function getAllMessages(params) {
  return apiClient.get('/sadmin/content/all-messages', { params })
}

/**
 * 获取帖子详情
 */
export function getPostDetail(postId) {
  return apiClient.get(`/sadmin/content/post/${postId}`)
}

/**
 * 获取评论详情
 */
export function getCommentDetail(commentId) {
  return apiClient.get(`/sadmin/content/comment/${commentId}`)
}

/**
 * 获取会话详情
 */
export function getMessageDetail(conversationId) {
  return apiClient.get(`/sadmin/content/message/${conversationId}`)
}
