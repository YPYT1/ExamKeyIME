package com.example.examkeyime

import com.example.examkeyime.util.StringFuzzy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringFuzzyTest {

    @Test
    fun `test exact match`() {
        assertTrue(StringFuzzy.isSimilar("理论", "马克思主义理论"))
    }

    @Test
    fun `test partial match`() {
        assertTrue(StringFuzzy.isSimilar("其他理论", "马克思主义理论区别于其他理论的根本特征"))
    }

    @Test
    fun `test match with punctuation`() {
        assertTrue(StringFuzzy.isSimilar("根本特征", "马克思主义理论，区别于其他理论的根本特征。"))
    }

    @Test
    fun `test no match`() {
        assertFalse(StringFuzzy.isSimilar("毛泽东思想", "马克思主义理论区别于其他理论的根本特征"))
    }

    @Test
    fun `test very short query`() {
        assertTrue(StringFuzzy.isSimilar("马", "马克思主义理论"))
    }

    @Test
    fun `test levenshtein distance for similar words`() {
        // "马克思" vs "马克恩" - distance should be 1
        assertTrue(StringFuzzy.isSimilar("马克恩", "马克思主义理论"))
    }

    @Test
    fun `test long text with contained query`() {
        val longText = "在信息技术飞速发展的今天，人工智能（AI）作为引领新一轮科技革命和产业变革的核心驱动力，正深刻改变着人们的生产、生活方式，并对社会治理、经济结构乃至国际格局产生深远影响。"
        val query = "人工智能的核心驱动力"
        assertTrue(StringFuzzy.isSimilar(query, longText))
    }

    @Test
    fun `test completely different long texts`() {
        val longText1 = "保护生物多样性对于维护地球生态平衡、保障人类可持续发展具有不可替代的重要作用。"
        val longText2 = "量子计算是一种遵循量子力学规律调控量子信息单元进行计算的新型计算模式。"
        assertFalse(StringFuzzy.isSimilar(longText1, longText2))
    }
}