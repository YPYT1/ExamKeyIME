package com.example.examkeyime.algorithm

import android.content.Context
import android.content.SharedPreferences

/**
 * 匹配算法类 - 提供三种不同严格度的匹配算法
 */
class MatchingAlgorithm(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("matching_settings", Context.MODE_PRIVATE)
    
    enum class MatchingLevel {
        LOW,      // 低严格度：2字符起，模糊匹配
        MEDIUM,   // 中等严格度：4字符起，相邻字符匹配
        HIGH      // 高严格度：6字符起，完全匹配
    }
    
    private var currentLevel: MatchingLevel = MatchingLevel.MEDIUM
    
    init {
        // 从SharedPreferences加载设置
        val levelName = prefs.getString("matching_level", "MEDIUM")
        currentLevel = try {
            MatchingLevel.valueOf(levelName ?: "MEDIUM")
        } catch (e: Exception) {
            MatchingLevel.MEDIUM
        }
    }
    
    /**
     * 设置匹配级别
     */
    fun setMatchingLevel(level: MatchingLevel) {
        currentLevel = level
        prefs.edit().putString("matching_level", level.name).apply()
    }
    
    /**
     * 获取当前匹配级别
     */
    fun getCurrentLevel(): MatchingLevel = currentLevel
    
    /**
     * 获取匹配级别的最小字符数
     */
    fun getMinimumCharacters(): Int {
        return when (currentLevel) {
            MatchingLevel.LOW -> 2
            MatchingLevel.MEDIUM -> 4
            MatchingLevel.HIGH -> 6
        }
    }
    
    /**
     * 执行匹配算法
     */
    fun matchPinyin(pinyinText: String, query: String): Boolean {
        if (query.length < getMinimumCharacters()) {
            return false
        }
        
        return when (currentLevel) {
            MatchingLevel.LOW -> matchLowStrict(pinyinText, query)
            MatchingLevel.MEDIUM -> matchMediumStrict(pinyinText, query)
            MatchingLevel.HIGH -> matchHighStrict(pinyinText, query)
        }
    }
    
    /**
     * 低严格度匹配：包含所有字符即可，不要求连续
     * 例如："masi"、"mayi" 都能匹配 "马克思主义" (makesizhuyi)
     */
    private fun matchLowStrict(pinyinText: String, query: String): Boolean {
        val pinyinLower = pinyinText.lowercase()
        val queryLower = query.lowercase()
        
        // 检查查询中的每个字符是否都在拼音文本中出现
        return queryLower.all { char ->
            pinyinLower.contains(char)
        }
    }
    
    /**
     * 中等严格度匹配：要求字符连续出现（子串匹配）
     */
    private fun matchMediumStrict(pinyinText: String, query: String): Boolean {
        val pinyinLower = pinyinText.lowercase()
        val queryLower = query.lowercase()
        
        // 直接检查是否包含连续的子串，不需要额外的边界检查
        return pinyinLower.contains(queryLower)
    }
    
    /**
     * 高严格度匹配：严格的音节边界匹配
     */
    private fun matchHighStrict(pinyinText: String, query: String): Boolean {
        val pinyinLower = pinyinText.lowercase()
        val queryLower = query.lowercase()
        
        // 首先检查基本的子串匹配
        if (!pinyinLower.contains(queryLower)) {
            return false
        }
        
        // 进一步检查：匹配位置应该在音节边界上
        val matchPositions = findAllMatchPositions(pinyinLower, queryLower)
        
        for (position in matchPositions) {
            if (isAtSyllableBoundary(pinyinLower, position, queryLower.length)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * 找到所有匹配位置
     */
    private fun findAllMatchPositions(text: String, query: String): List<Int> {
        val positions = mutableListOf<Int>()
        var startIndex = 0
        
        while (startIndex <= text.length - query.length) {
            val index = text.indexOf(query, startIndex)
            if (index == -1) break
            positions.add(index)
            startIndex = index + 1
        }
        
        return positions
    }
    
    /**
     * 检查匹配位置是否在音节边界上
     */
    private fun isAtSyllableBoundary(text: String, position: Int, matchLength: Int): Boolean {
        // 检查匹配开始位置是否在音节开头
        val isStartBoundary = position == 0 || 
            !text[position - 1].isLetter() || 
            isLikelySyllableStart(text, position)
            
        // 检查匹配结束位置是否在音节结尾
        val endPosition = position + matchLength
        val isEndBoundary = endPosition == text.length || 
            !text[endPosition].isLetter() || 
            isLikelySyllableEnd(text, endPosition - 1)
            
        return isStartBoundary || isEndBoundary
    }
    
    /**
     * 判断是否可能是音节开始
     */
    private fun isLikelySyllableStart(text: String, position: Int): Boolean {
        if (position == 0) return true
        
        val prevChar = text[position - 1]
        val currentChar = text[position]
        
        // 如果前一个字符是韵母结尾，当前字符是声母，可能是新音节开始
        val vowelEndings = setOf('a', 'o', 'e', 'i', 'u', 'n', 'g')
        val consonantStarts = setOf('b', 'p', 'm', 'f', 'd', 't', 'n', 'l', 'g', 'k', 'h', 'j', 'q', 'x', 'r', 'z', 'c', 's', 'y', 'w')
        
        return prevChar in vowelEndings && currentChar in consonantStarts
    }
    
    /**
     * 判断是否可能是音节结尾
     */
    private fun isLikelySyllableEnd(text: String, position: Int): Boolean {
        if (position == text.length - 1) return true
        
        val currentChar = text[position]
        val nextChar = text[position + 1]
        
        // 如果当前字符是韵母结尾，下一个字符是声母，可能是音节结尾
        val vowelEndings = setOf('a', 'o', 'e', 'i', 'u', 'n', 'g')
        val consonantStarts = setOf('b', 'p', 'm', 'f', 'd', 't', 'n', 'l', 'g', 'k', 'h', 'j', 'q', 'x', 'r', 'z', 'c', 's', 'y', 'w')
        
        return currentChar in vowelEndings && nextChar in consonantStarts
    }
    
    /**
     * 将拼音字符串分割成音节
     */
    private fun splitIntoPinyinSyllables(pinyin: String): List<String> {
        val syllables = mutableListOf<String>()
        var current = ""
        
        for (char in pinyin) {
            if (char.isLetter()) {
                current += char
                // 简单的音节分割逻辑
                if (current.length >= 2 && isCompleteSyllable(current)) {
                    syllables.add(current)
                    current = ""
                }
            } else {
                if (current.isNotEmpty()) {
                    syllables.add(current)
                    current = ""
                }
            }
        }
        
        if (current.isNotEmpty()) {
            syllables.add(current)
        }
        
        return syllables
    }
    
    /**
     * 检查是否是完整的拼音音节
     */
    private fun isCompleteSyllable(syllable: String): Boolean {
        // 常见的拼音音节结尾
        val endings = listOf("a", "o", "e", "i", "u", "n", "g", "ng", "an", "en", "in", "un", "ang", "eng", "ing", "ong")
        return endings.any { syllable.endsWith(it) }
    }
    
    /**
     * 获取算法级别描述
     */
    fun getLevelDescription(level: MatchingLevel): String {
        return when (level) {
            MatchingLevel.LOW -> "低严格度：2字符起，包含所有字符即可匹配（字符可分散）"
            MatchingLevel.MEDIUM -> "中等严格度：4字符起，要求字符连续出现（子串匹配）"
            MatchingLevel.HIGH -> "高严格度：6字符起，严格音节边界匹配"
        }
    }
    
    /**
     * 获取算法级别示例
     */
    fun getLevelExample(level: MatchingLevel): String {
        return when (level) {
            MatchingLevel.LOW -> "例：'masi'、'mayi'等字符分散匹配"
            MatchingLevel.MEDIUM -> "例：'make'、'kesi'、'zhuyi'等连续匹配"
            MatchingLevel.HIGH -> "例：严格按音节边界匹配"
        }
    }
}