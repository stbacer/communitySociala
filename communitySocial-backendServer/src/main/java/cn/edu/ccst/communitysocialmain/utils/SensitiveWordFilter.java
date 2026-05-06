package cn.edu.ccst.communitysocialmain.utils;

import cn.edu.ccst.communitysocialmain.mapper.SensitiveWordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 敏感词过滤工具类 - 基于 DFA 算法实现
 * 
 * DFA（Deterministic Finite Automaton）确定性有穷自动机
 * 时间复杂度：O(n)，其中 n 为文本长度，只需遍历一次文本
 * 空间复杂度：O(m*k)，其中 m 为敏感词数量，k 为平均词长
 */
@Slf4j
@Component
public class SensitiveWordFilter {
    
    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    
    // DFA 树根节点
    private final TrieNode root = new TrieNode();
    
    // 敏感词缓存（用于快速重建 DFA 树）
    private final Set<String> sensitiveWordsCache = ConcurrentHashMap.newKeySet();
    
    /**
     * DFA 树节点
     */
    private static class TrieNode {
        // 子节点映射：字符 -> 节点
        private final Map<Character, TrieNode> children = new HashMap<>();
        // 是否为一个敏感词的结尾
        private boolean isEnd = false;
        // 敏感词类型（可选，用于分类处理）
        private Integer wordType;
        
        public TrieNode() {}
        
        public TrieNode(Integer wordType) {
            this.wordType = wordType;
        }
    }
    
    @PostConstruct
    public void initSensitiveWords() {
        loadSensitiveWords();
    }
    
    /**
     * 加载敏感词并构建 DFA 树
     */
    public void loadSensitiveWords() {
        try {
            List<cn.edu.ccst.communitysocialmain.entity.SensitiveWord> words = 
                sensitiveWordMapper.selectAllEnabled();
            
            // 清空现有数据
            sensitiveWordsCache.clear();
            rebuildDfaTree();
            
            // 重新加载所有敏感词
            if (!CollectionUtils.isEmpty(words)) {
                for (cn.edu.ccst.communitysocialmain.entity.SensitiveWord word : words) {
                    // 将 String 类型的 type 转换为 Integer
                    Integer wordType = null;
                    try {
                        wordType = Integer.parseInt(word.getType());
                    } catch (NumberFormatException e) {
                        wordType = 1; // 默认类型为 1（通用）
                    }
                    addWordToDfa(word.getWord(), wordType);
                    sensitiveWordsCache.add(word.getWord());
                }
            }
        } catch (Exception e) {
            log.error("加载敏感词失败", e);
        }
    }
    
    /**
     * 重建 DFA 树（清空后重新初始化）
     */
    private void rebuildDfaTree() {
        // 清空所有子节点
        root.children.clear();
        root.isEnd = false;
    }
    
    /**
     * 向 DFA 树中添加一个敏感词
     * @param word 敏感词
     * @param wordType 敏感词类型
     */
    private void addWordToDfa(String word, Integer wordType) {
        if (word == null || word.isEmpty()) {
            return;
        }
        
        TrieNode node = root;
        char[] chars = word.toCharArray();
        
        for (char c : chars) {
            // 跳过空格和特殊符号（可选优化）
            if (Character.isWhitespace(c)) {
                continue;
            }
            
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        
        node.isEnd = true;
        node.wordType = wordType;
    }
    
    /**
     * 从 DFA 树中移除一个敏感词
     * @param word 敏感词
     */
    private void removeWordFromDfa(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        
        removeWordFromDfaHelper(root, word.toCharArray(), 0);
    }
    
    /**
     * 递归删除敏感词（辅助方法）
     */
    private boolean removeWordFromDfaHelper(TrieNode node, char[] word, int index) {
        if (index == word.length) {
            node.isEnd = false;
            node.wordType = null;
            return node.children.isEmpty();
        }
        
        char c = word[index];
        if (Character.isWhitespace(c)) {
            return removeWordFromDfaHelper(node, word, index + 1);
        }
        
        TrieNode child = node.children.get(c);
        if (child == null) {
            return false;
        }
        
        boolean shouldDeleteChild = removeWordFromDfaHelper(child, word, index + 1);
        if (shouldDeleteChild) {
            node.children.remove(c);
            return node.children.isEmpty() && !node.isEnd;
        }
        
        return false;
    }
    
    /**
     * 过滤敏感词（DFA 算法核心实现）
     * @param content 待检测的文本
     * @return 过滤后的文本
     */
    public String filter(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        StringBuilder result = new StringBuilder();
        char[] chars = content.toCharArray();
        int i = 0;
        
        while (i < chars.length) {
            TrieNode node = root;
            int matchEnd = -1;
            int currentPos = i;
            
            // 沿着 DFA 树匹配
            while (currentPos < chars.length && node != null) {
                char c = chars[currentPos];
                
                // 跳过空格（可选优化）
                if (Character.isWhitespace(c)) {
                    currentPos++;
                    continue;
                }
                
                node = node.children.get(c);
                
                if (node != null) {
                    // 如果当前节点是敏感词结尾，记录位置（支持最长匹配）
                    if (node.isEnd) {
                        matchEnd = currentPos + 1;
                    }
                    currentPos++;
                }
            }
            
            // 如果找到敏感词，替换为 **
            if (matchEnd > -1) {
                result.append("**");
                i = matchEnd;
            } else {
                // 否则保留原字符
                result.append(chars[i]);
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 检查是否包含敏感词（DFA 算法）
     * @param content 待检测的文本
     * @return true-包含敏感词，false-不包含
     */
    public boolean containsSensitiveWord(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        char[] chars = content.toCharArray();
        int i = 0;
        
        while (i < chars.length) {
            TrieNode node = root;
            int currentPos = i;
            
            while (currentPos < chars.length && node != null) {
                char c = chars[currentPos];
                
                if (Character.isWhitespace(c)) {
                    currentPos++;
                    continue;
                }
                
                node = node.children.get(c);
                
                if (node != null && node.isEnd) {
                    return true; // 找到敏感词
                }
                
                currentPos++;
            }
            
            i++;
        }
        
        return false;
    }
    
    /**
     * 添加敏感词到缓存和 DFA 树
     * @param word 敏感词
     */
    public void addSensitiveWord(String word) {
        if (word != null && !word.isEmpty() && !sensitiveWordsCache.contains(word)) {
            sensitiveWordsCache.add(word);
            addWordToDfa(word, 1); // 默认类型为 1
            log.debug("添加敏感词：{}", word);
        }
    }
    
    /**
     * 从缓存和 DFA 树移除敏感词
     * @param word 敏感词
     */
    public void removeSensitiveWord(String word) {
        if (word != null && !word.isEmpty()) {
            sensitiveWordsCache.remove(word);
            removeWordFromDfa(word);
            log.debug("移除敏感词：{}", word);
        }
    }
    
    /**
     * 获取 DFA 树的深度（用于监控和调试）
     * @return 树的深度
     */
    private int getDfaTreeDepth() {
        return calculateDepth(root);
    }
    
    /**
     * 递归计算树的深度
     */
    private int calculateDepth(TrieNode node) {
        if (node == null || node.children.isEmpty()) {
            return 0;
        }
        
        int maxDepth = 0;
        for (TrieNode child : node.children.values()) {
            maxDepth = Math.max(maxDepth, calculateDepth(child));
        }
        
        return maxDepth + 1;
    }
    
    /**
     * 获取敏感词库大小
     * @return 敏感词数量
     */
    public int getSensitiveWordCount() {
        return sensitiveWordsCache.size();
    }
}