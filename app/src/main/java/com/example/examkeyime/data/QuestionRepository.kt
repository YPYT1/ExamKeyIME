package com.example.examkeyime.data

import android.content.Context
import com.example.examkeyime.algorithm.MatchingAlgorithm
import com.example.examkeyime.model.Question
import com.example.examkeyime.model.QuestionType
import com.example.examkeyime.util.FileManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class QuestionRepository(private val context: Context) {

    private var questions: List<Question> = emptyList()
    private val fileManager = FileManager(context)
    private val matchingAlgorithm = MatchingAlgorithm(context)

    suspend fun loadQuestions() {
        withContext(Dispatchers.IO) {
            try {
                // 先尝试加载用户上传的题库
                val userQuestions = loadUserQuestions()
                
                if (userQuestions.isNotEmpty()) {
                    // 如果有用户题库，使用用户题库
                    questions = userQuestions
                } else {
                    // 否则使用默认题库
                    val jsonString = context.assets.open("question-bank.json").bufferedReader().use { it.readText() }
                    val listType = object : TypeToken<List<Question>>() {}.type
                    questions = Gson().fromJson(jsonString, listType)
                }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                questions = emptyList()
            }
        }
    }
    
    private fun loadUserQuestions(): List<Question> {
        val allQuestions = mutableListOf<Question>()
        
        // 获取所有解析后的文件
        val parsedFiles = fileManager.getParsedFiles()
        
        for (fileInfo in parsedFiles) {
            try {
                val content = fileManager.readParsedFile(fileInfo.name)
                if (content != null) {
                    val listType = object : TypeToken<List<Question>>() {}.type
                    val questions: List<Question> = Gson().fromJson(content, listType)
                    allQuestions.addAll(questions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return allQuestions
    }

    fun searchByPinyin(pinyinQuery: String): List<Question> {
        if (pinyinQuery.isBlank() || pinyinQuery.length < matchingAlgorithm.getMinimumCharacters()) {
            return emptyList()
        }
        
        val query = pinyinQuery.lowercase().trim()
        
        return questions.filter { question ->
            val pinyinText = question.pinyinText.lowercase()
            val questionText = question.text.lowercase()
            
            // 使用MatchingAlgorithm类的实际匹配方法
            val pinyinMatch = matchingAlgorithm.matchPinyin(pinyinText, query)
            
            // 中文直接匹配（仅用于后备）
            val chineseMatch = questionText.contains(query)
            
            pinyinMatch || chineseMatch
        }.sortedWith { q1, q2 ->
            val score1 = calculateMatchScoreWithAlgorithm(q1, query)
            val score2 = calculateMatchScoreWithAlgorithm(q2, query)
            score2.compareTo(score1)
        }
    }
    
    fun searchByPinyinAndType(pinyinQuery: String, questionType: QuestionType?): List<Question> {
        if (pinyinQuery.isBlank() || pinyinQuery.length < matchingAlgorithm.getMinimumCharacters()) {
            return emptyList()
        }
        
        val query = pinyinQuery.lowercase().trim()
        
        return questions.filter { question ->
            val matchesType = questionType == null || question.type == questionType
            if (!matchesType) return@filter false
            
            val pinyinText = question.pinyinText.lowercase()
            val questionText = question.text.lowercase()
            
            // 使用MatchingAlgorithm类的实际匹配方法
            val pinyinMatch = matchingAlgorithm.matchPinyin(pinyinText, query)
            
            // 中文直接匹配（仅用于后备）
            val chineseMatch = questionText.contains(query)
            
            pinyinMatch || chineseMatch
        }.sortedWith { q1, q2 ->
            val score1 = calculateMatchScoreWithAlgorithm(q1, query)
            val score2 = calculateMatchScoreWithAlgorithm(q2, query)
            score2.compareTo(score1)
        }
    }
    

    

    

    

    

    

    

    



    
    // 新的评分方法，考虑算法级别
    private fun calculateMatchScoreWithAlgorithm(question: Question, query: String): Int {
        val pinyinText = question.pinyinText.lowercase()
        val questionText = question.text.lowercase()
        
        // 基础分数
        var score = 0
        
        // 中文直接匹配（最高优先级）
        if (questionText.contains(query)) {
            score += 100
        }
        
        // 使用算法匹配
        if (matchingAlgorithm.matchPinyin(pinyinText, query)) {
            score += when (matchingAlgorithm.getCurrentLevel()) {
                MatchingAlgorithm.MatchingLevel.HIGH -> 95  // 高严格度匹配得分高
                MatchingAlgorithm.MatchingLevel.MEDIUM -> 85  // 中等严格度
                MatchingAlgorithm.MatchingLevel.LOW -> 75   // 低严格度
            }
        }
        
        // 前缀匹配加分
        if (pinyinText.startsWith(query)) {
            score += 20
        }
        
        if (questionText.startsWith(query)) {
            score += 15
        }
        
        // 查询长度加分（更长的查询匹配更有价值）
        score += query.length * 2
        
        return score
    }

    fun getStats(): Map<String, Int> {
        return questions.groupBy { it.type }
            .mapValues { it.value.size }
            .mapKeys { entry ->
                when (entry.key) {
                    QuestionType.SINGLE_CHOICE -> "单选题"
                    QuestionType.MULTIPLE_CHOICE -> "多选题"
                    QuestionType.TRUE_FALSE -> "判断题"
                }
            }
    }

    fun getTotalCount(): Int = questions.size

    fun getAllQuestions(): List<Question> = questions
    

    

    

    

    

}