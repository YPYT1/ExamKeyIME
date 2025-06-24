package com.example.examkeyime.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileManager(private val context: Context) {
    
    private val uploadedFilesDir = File(context.filesDir, "uploaded_files")
    private val parsedFilesDir = File(context.filesDir, "parsed_files")
    
    init {
        // 确保目录存在
        uploadedFilesDir.mkdirs()
        parsedFilesDir.mkdirs()
    }
    
    fun saveUploadedFile(uri: Uri, originalName: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${timestamp}_$originalName"
            val file = File(uploadedFilesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            fileName
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun readUploadedFile(fileName: String): String? {
        return try {
            val file = File(uploadedFilesDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun saveParsedQuestions(content: String, originalFileName: String): String? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "parsed_${timestamp}_${originalFileName.replace(".md", ".json")}"
            val file = File(parsedFilesDir, fileName)
            
            file.writeText(content)
            fileName
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    fun getUploadedFiles(): List<FileInfo> {
        return uploadedFilesDir.listFiles()?.map { file ->
            FileInfo(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                lastModified = file.lastModified(),
                type = FileType.UPLOADED
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }
    
    fun getParsedFiles(): List<FileInfo> {
        return parsedFilesDir.listFiles()?.map { file ->
            FileInfo(
                name = file.name,
                path = file.absolutePath,
                size = file.length(),
                lastModified = file.lastModified(),
                type = FileType.PARSED
            )
        }?.sortedByDescending { it.lastModified } ?: emptyList()
    }
    
    fun getAllFiles(): List<FileInfo> {
        return getUploadedFiles() + getParsedFiles()
    }
    
    fun deleteFile(fileName: String, type: FileType): Boolean {
        return try {
            val dir = when (type) {
                FileType.UPLOADED -> uploadedFilesDir
                FileType.PARSED -> parsedFilesDir
            }
            val file = File(dir, fileName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun readParsedFile(fileName: String): String? {
        return try {
            val file = File(parsedFilesDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val type: FileType
)

enum class FileType {
    UPLOADED,
    PARSED
}