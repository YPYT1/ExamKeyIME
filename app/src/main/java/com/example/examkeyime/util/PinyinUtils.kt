package com.example.examkeyime.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object PinyinUtils {
    private val pinyinMap = HashMap<Char, String>()
    private var isInitialized = false
    private const val TAG = "PinyinUtils"

    fun init(context: Context) {
        if (isInitialized) {
            return
        }
        
        synchronized(this) {
            if (isInitialized) return
            
            Log.d(TAG, "Initializing PinyinUtils...")
            try {
                context.assets.open("pinyin_data.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).forEachLine { line ->
                        // Format: U+3400: pàn # 㐀
                        // We need to handle lines with and without comments
                        if (!line.startsWith("U+")) return@forEachLine

                        val parts = line.split("#")
                        val hanziPart = if (parts.size > 1) parts[1].trim() else ""
                        if (hanziPart.isEmpty()) return@forEachLine
                        
                        val char = hanziPart.first()

                        val pinyinPart = parts[0].split(":")[1].trim()
                        val pinyin = pinyinPart.split(",")[0]

                        if (pinyin.isNotEmpty()) {
                            pinyinMap[char] = pinyin
                        }
                    }
                }
                isInitialized = true
                Log.d(TAG, "PinyinUtils initialized with ${pinyinMap.size} characters.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing PinyinUtils", e)
            }
        }
    }

    fun toPinyin(text: String): String {
        if (!isInitialized) {
            Log.w(TAG, "PinyinUtils has not been initialized! Call init() first.")
            // Fallback for non-initialized state, might return original text
            return text 
        }
        val pinyin = StringBuilder()
        for (char in text) {
            // Append pinyin if found, otherwise append the original character.
            // This handles mixed strings (e.g., "C++语言").
            pinyin.append(pinyinMap[char] ?: char)
        }
        return pinyin.toString()
    }
} 