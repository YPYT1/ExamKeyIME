package com.example.examkeyime.model

import com.google.gson.annotations.SerializedName

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
}

data class Question(
    val type: QuestionType,

    @SerializedName("text")
    val text: String,

    @SerializedName("pinyinText")
    val pinyinText: String = "",

    @SerializedName("options")
    val options: List<String>,

    @SerializedName("correct")
    val correct: List<String>,

    @SerializedName("explanation")
    val explanation: String? = null
) {
    val answerDescription: String
        get() {
            return when (type) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTIPLE_CHOICE -> {
                    correct.joinToString(", ") { answerChar ->
                        val option = options.find { it.startsWith(answerChar) }
                        option ?: answerChar
                    }
                }
                QuestionType.TRUE_FALSE -> if (correct.firstOrNull() == "T") "正确" else "错误"
            }
        }
}

fun formatAnswers(question: Question): List<String> {
    return when (question.type) {
        QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE ->
            question.correct
        QuestionType.MULTIPLE_CHOICE ->
            listOf(question.correct.sorted().joinToString(""))
    }
}