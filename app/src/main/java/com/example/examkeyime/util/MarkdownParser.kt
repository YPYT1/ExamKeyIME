package com.example.examkeyime.util

import com.example.examkeyime.model.Question
import com.example.examkeyime.model.QuestionType
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import java.util.regex.Pattern

class MarkdownParser {
    
    private val pinyinFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
    }
    
    private fun convertToPinyin(text: String): String {
        return text.map { char ->
            try {
                val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(char, pinyinFormat)
                pinyinArray?.firstOrNull() ?: char.toString()
            } catch (e: Exception) {
                char.toString()
            }
        }.joinToString("")
    }
    
    fun parseMarkdownToQuestions(content: String): List<Question> {
        val questions = mutableListOf<Question>()
        val lines = content.split("\n")
        
        android.util.Log.d("MarkdownParser", "开始解析，总行数: ${lines.size}")
        
        var currentQuestion: String? = null
        val currentOptions = mutableListOf<String>()
        var currentAnswer: String? = null
        var currentType: QuestionType? = null
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // 检查是否是题目行（以数字开头，如 "1、", "2.", "1)", "（1）"等）
            if (isQuestionLine(trimmedLine)) {
                // 保存前一个题目
                if (currentQuestion != null && currentAnswer != null && currentType != null) {
                    questions.add(createQuestion(currentQuestion, currentOptions.toList(), currentAnswer, currentType))
                }
                
                // 开始新题目
                currentQuestion = extractQuestionText(trimmedLine)
                currentOptions.clear()
                currentAnswer = null
                currentType = determineQuestionType(currentQuestion)
            }
            // 检查是否是选项行（A、B、C、D等）
            else if (isOptionLine(trimmedLine)) {
                currentOptions.add(trimmedLine)
            }
            // 检查是否是答案行
            else if (isAnswerLine(trimmedLine)) {
                currentAnswer = extractAnswer(trimmedLine)
            }
            // 如果没有明确标记，可能是题目的续行
            else if (currentQuestion != null && !trimmedLine.startsWith("答案") && !trimmedLine.startsWith("解析")) {
                currentQuestion += trimmedLine
            }
        }
        
        // 处理最后一个题目
        if (currentQuestion != null && currentAnswer != null && currentType != null) {
            questions.add(createQuestion(currentQuestion, currentOptions.toList(), currentAnswer, currentType))
        }
        
        android.util.Log.d("MarkdownParser", "解析完成，共生成 ${questions.size} 道题目")
        return questions
    }
    
    private fun isQuestionLine(line: String): Boolean {
        // 匹配各种题目编号格式
        val patterns = listOf(
            "^\\d+[、．.].*",          // 1、 1. 1．
            "^\\d+\\).*",             // 1)
            "^\\(\\d+\\).*",          // (1)
            "^（\\d+）.*",             // （1）
            "^第\\d+题.*"             // 第1题
        )
        return patterns.any { Pattern.matches(it, line) }
    }
    
    private fun extractQuestionText(line: String): String {
        // 移除题目编号，保留题目内容
        return line.replaceFirst("^\\d+[、．.]", "")
                  .replaceFirst("^\\d+\\)", "")
                  .replaceFirst("^\\(\\d+\\)", "")
                  .replaceFirst("^（\\d+）", "")
                  .replaceFirst("^第\\d+题[：:]?", "")
                  .trim()
    }
    
    private fun isOptionLine(line: String): Boolean {
        // 检查是否是选项行
        return line.matches("^[ABCD][、．.)].*".toRegex()) ||
               line.matches("^[abcd][、．.)].*".toRegex())
    }
    
    private fun isAnswerLine(line: String): Boolean {
        return line.startsWith("答案") || line.matches("^答[：:].*".toRegex())
    }
    
    private fun extractAnswer(line: String): String {
        return line.replaceFirst("^答案[：:]?", "")
                  .replaceFirst("^答[：:]", "")
                  .trim()
    }
    
    private fun determineQuestionType(questionText: String?): QuestionType {
        if (questionText == null) return QuestionType.SINGLE_CHOICE
        
        val lowerQuestion = questionText.lowercase()
        
        // 判断题关键词
        val trueFalseKeywords = listOf("正确", "错误", "对还是错", "是否正确", "判断", "对错")
        if (trueFalseKeywords.any { lowerQuestion.contains(it) }) {
            return QuestionType.TRUE_FALSE
        }
        
        // 多选题关键词
        val multipleChoiceKeywords = listOf("多选", "以下哪些", "包括", "哪几个", "选择正确的")
        if (multipleChoiceKeywords.any { lowerQuestion.contains(it) }) {
            return QuestionType.MULTIPLE_CHOICE
        }
        
        // 默认为单选题
        return QuestionType.SINGLE_CHOICE
    }
    
    private fun createQuestion(text: String, options: List<String>, answer: String, type: QuestionType): Question {
        val pinyinText = convertToPinyin(text)
        
        // 处理答案格式
        val processedAnswers = when (type) {
            QuestionType.TRUE_FALSE -> {
                // 判断题答案处理
                when {
                    answer.contains("正确") || answer.contains("对") || answer.contains("T") || answer.contains("√") -> listOf("T")
                    answer.contains("错误") || answer.contains("错") || answer.contains("F") || answer.contains("×") -> listOf("F")
                    else -> listOf("T") // 默认正确
                }
            }
            QuestionType.MULTIPLE_CHOICE -> {
                // 多选题答案可能是 "ABC" 或 "A,B,C" 或 "A B C"
                answer.replace(",", "").replace(" ", "").toCharArray().map { it.toString() }
            }
            QuestionType.SINGLE_CHOICE -> {
                // 单选题答案通常是单个字母
                listOf(answer.trim().first().toString().uppercase())
            }
        }
        
        return Question(
            type = type,
            text = text,
            pinyinText = pinyinText,
            options = options,
            correct = processedAnswers
        )
    }
}