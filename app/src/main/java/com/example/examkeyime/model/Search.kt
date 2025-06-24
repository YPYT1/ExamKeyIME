package com.example.examkeyime.model

/**
 * 搜索结果的数据类，包含匹配到的问题原文和其所有答案的列表。
 */
data class SearchResult(val question: String, val answers: List<String>)

/**
 * 模糊搜索匹配器的接口，定义了搜索功能的契约。
 */
interface FuzzyMatcher {
    /**
     * 根据给定的关键字执行搜索。
     * @param keyword 搜索关键字。
     * @return 如果找到匹配项，则返回 [SearchResult]；否则返回 null。
     */
    fun search(keyword: String): SearchResult?
} 