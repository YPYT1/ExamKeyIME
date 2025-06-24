buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.belerweb:pinyin4j:2.5.1")
    }
}

import com.google.gson.GsonBuilder
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.examkeyime"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.examkeyime"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
     buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Define data classes for JSON structure
enum class SimpleQuestionType {
    SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE
}

data class SimpleQuestion(
    val type: SimpleQuestionType,
    val text: String,
    val pinyinText: String,
    val options: List<String>,
    val correct: List<String>,
    val explanation: String?
)

// Pinyin conversion function
fun getPinyin(text: String): String {
    val format = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.LOWERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }
    return text.map { char ->
        try {
            PinyinHelper.toHanyuPinyinStringArray(char, format)?.firstOrNull() ?: char.toString()
        } catch (e: Exception) {
            char.toString()
        }
    }.joinToString("")
}


tasks.register("convertMarkdownToJson") {
    group = "Custom"
    description = "Converts question markdown files to a single JSON file in assets."

    doLast {
        val assetsDir = "src/main/assets"
        val markdownDir = project.file("${assetsDir}/timushuju")
        val outputFile = project.file("${assetsDir}/question-bank.json")

        if (!markdownDir.exists()) {
            println("Markdown directory not found: ${markdownDir.path}")
            return@doLast
        }

        val allQuestions = mutableListOf<SimpleQuestion>()

        markdownDir.listFiles { file -> file.extension == "md" }?.forEach { file ->
            println("Processing file: ${file.name}")
            val type = when (file.nameWithoutExtension) {
                "单选题" -> SimpleQuestionType.SINGLE_CHOICE
                "多选题" -> SimpleQuestionType.MULTIPLE_CHOICE
                "判断题" -> SimpleQuestionType.TRUE_FALSE
                else -> null
            }

            if (type != null) {
                val text = file.readText(Charsets.UTF_8)
                val questionBlocks = text.split(Regex("\\n(?=\\d+[、．.])")).map { it.trim() }.filter { it.isNotBlank() }

                for (block in questionBlocks) {
                    try {
                        val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
                        if (lines.isEmpty()) continue

                        val firstLine = lines[0].replace(Regex("^\\d+[、．.]\\s*"), "")
                        
                        val answerLineIndex = lines.indexOfFirst { it.startsWith("答案") }
                        if (answerLineIndex == -1) {
                            println("Skipping block without answer: $block")
                            continue
                        }

                        val questionText: String
                        val options: List<String>
                        val answers = mutableListOf<String>()

                        if (type == SimpleQuestionType.TRUE_FALSE) {
                            if (answerLineIndex == 0) continue
                            questionText = (listOf(firstLine) + lines.subList(1, answerLineIndex)).joinToString(" ").trim()
                            options = emptyList()
                            val answerContent = lines[answerLineIndex].replace(Regex("^答案[:：]?\\s*"), "").trim()
                            answers.add(if (answerContent == "是" || answerContent == "T" || answerContent == "正确" || answerContent.contains("正确")) "T" else "F")
                        } else {
                            val firstOptionIndex = lines.indexOfFirst { it.matches(Regex("^[A-Z][、．.].*")) }
                            if (firstOptionIndex == -1 || firstOptionIndex >= answerLineIndex) {
                                questionText = (listOf(firstLine) + lines.subList(1, answerLineIndex)).joinToString(" ").trim()
                                options = emptyList()
                            } else {
                                val questionLines = if (firstLine.matches(Regex("^[A-Z][、．.].*"))) {
                                    emptyList()
                                } else {
                                    listOf(firstLine) + lines.subList(1, firstOptionIndex)
                                }
                                questionText = questionLines.joinToString(" ").trim()
                                options = lines.subList(firstOptionIndex, answerLineIndex).map { it.trim() }
                            }
                            
                            val answerLine = lines[answerLineIndex]
                            val answerContent = answerLine.replace(Regex("^答案[:：]?\\s*"), "").trim()
                            answers.addAll(if (type == SimpleQuestionType.MULTIPLE_CHOICE) answerContent.map { it.toString() } else listOf(answerContent))
                        }
                        
                        val pinyinText = getPinyin(questionText)

                        if (questionText.isNotEmpty() && answers.isNotEmpty()) {
                            allQuestions.add(
                                SimpleQuestion(
                                    type = type,
                                    text = questionText,
                                    pinyinText = pinyinText,
                                    options = options,
                                    correct = answers,
                                    explanation = null
                                )
                            )
                        }
                    } catch (e: Exception) {
                        println("Error parsing block in ${file.name}: ${e.message}\\nFor block:\\n$block")
                        e.printStackTrace()
                    }
                }
            }
        }

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = gson.toJson(allQuestions)

        outputFile.writeText(jsonString, Charsets.UTF_8)
        println("Successfully converted ${allQuestions.size} questions to ${outputFile.name}.")
    }
}

tasks.named("preBuild") {
    dependsOn("convertMarkdownToJson")
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Pinyin4j
    implementation("com.belerweb:pinyin4j:2.5.1")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

}