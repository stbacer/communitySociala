# 我的评论功能说明

## 页面位置
`/pages/profile/mycomments/mycomments`

## 功能特性

### 1. 评论列表展示
- ✅ 显示用户发布的所有评论
- ✅ 支持审核状态显示 (审核中/已通过/未通过)
- ✅ 显示评论所在的帖子信息
- ✅ 显示回复目标 (如果有)
- ✅ 显示点赞数和回复数

### 2. 交互功能
- ✅ 点击评论跳转到对应帖子详情页
- ✅ 删除评论功能
- ✅ 回复评论功能
- ✅ 下拉刷新
- ✅ 上拉加载更多

### 3. UI/UX优化
- ✅ 骨架屏加载效果
- ✅ 空状态提示
- ✅ 响应式设计
- ✅ 渐变色彩设计

## API接口

### 获取用户评论列表
```javascript
GET /resident/comment/user/{userId}
```

**请求参数:**
- `page`: 页码
- `size`: 每页数量

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "commentId": 1,
        "postId": 1,
        "content": "评论内容",
        "status": 2,
        "likeCount": 5,
        "replyCount": 2,
        "createTime": "2024-01-01 12:00:00",
        "postInfo": {
          "title": "帖子标题"
        },
        "replyToNickname": "用户名",
        "replyContent": "回复内容"
      }
    ],
    "total": 100
  }
}
```

## 数据缓存策略

### 缓存机制
- 使用本地存储缓存评论数据
- 缓存有效期：10 分钟
- 自动后台刷新
- 5 分钟强制刷新

### 缓存键
```javascript
'my_comments_cache'
'my_comments_last_refresh'
```

## 页面状态

### 审核状态
- `pending` (1): 审核中 - 黄色标签
- `approved` (2): 已通过 - 绿色标签
- `rejected` (3): 未通过 - 红色标签

### 加载状态
- `loading`: 首次加载
- `refreshing`: 下拉刷新
- `hasMore`: 是否有更多数据

## 操作说明

### 删除评论
1. 点击评论项的"删除"按钮
2. 确认删除操作
3. 从列表中移除该评论

### 回复评论
1. 点击评论项的"回复"按钮
2. 跳转到帖子详情页
3. 在详情表中输入回复内容

## 注意事项

1. **权限要求**: 需要用户完成实名认证
2. **网络处理**: 支持重试机制，超时时间 10 秒
3. **错误处理**: 友好的错误提示信息
4. **性能优化**: 使用缓存减少服务器请求

## 待开发功能

- [ ] 评论搜索功能
- [ ] 评论排序 (按时间/按热度)
- [ ] 批量删除评论
- [ ] 评论导出功能
- [ ] 评论统计信息

## 相关文件

- 页面文件：`mycomments.wxml`, `mycomments.wxss`, `mycomments.js`, `mycomments.json`
- API 工具：`utils/api.js` - `getUserComments()` 方法
- 认证工具：`utils/auth.js`
