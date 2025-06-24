package com.example.examkeyime.ime

import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.*
import android.widget.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.example.examkeyime.algorithm.MatchingAlgorithm
import com.example.examkeyime.data.QuestionRepository
import com.example.examkeyime.model.KeyboardMode
import com.example.examkeyime.model.Question
import com.example.examkeyime.pinyin.PinyinEngine
import com.example.examkeyime.util.PinyinUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ExamKeyIME : InputMethodService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var questionRepository: QuestionRepository
    private lateinit var pinyinEngine: PinyinEngine
    private lateinit var imeUi: ExamKeyIME_UI
    private lateinit var matchingAlgorithm: MatchingAlgorithm

    private val _currentMode = MutableStateFlow(KeyboardMode.PINYIN)
    val currentMode: StateFlow<KeyboardMode> = _currentMode.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText: StateFlow<String> = _composingText.asStateFlow()

    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val candidates: StateFlow<List<String>> = _candidates.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Question>>(emptyList())
    val searchResults: StateFlow<List<Question>> = _searchResults.asStateFlow()

    private lateinit var mainLayout: LinearLayout
    internal lateinit var extensionBar: LinearLayout
    internal lateinit var questionPopup: LinearLayout
    internal lateinit var keyboardLayout: LinearLayout
    internal var searchInputView: EditText? = null
    
    internal var lastSearchResults: List<Question> = emptyList()

    private var currentInputConnection: InputConnection? = null

    override fun onCreate() {
        super.onCreate()
        PinyinUtils.init(this)
        pinyinEngine = PinyinEngine(this)
        questionRepository = QuestionRepository(this)
        matchingAlgorithm = MatchingAlgorithm(this)
        
        serviceScope.launch {
            pinyinEngine.initialize()
            questionRepository.loadQuestions()
        }
        setupSearchHandler()
    }

    override fun onCreateInputView(): View {
        imeUi = ExamKeyIME_UI(this, this)
        return imeUi.createView()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        _currentMode.value = KeyboardMode.PINYIN // Reset to PINYIN mode on new input field
        clearState()
    }

    fun onKey(keyCode: Int) {
        val inputConnection = currentInputConnection

        when (_currentMode.value) {
            KeyboardMode.PINYIN -> {
                // 普通英文输入模式（取消中文功能）
                if (keyCode == -5) { // Backspace
                    inputConnection?.deleteSurroundingText(1, 0)
                } else if (keyCode >= 32 && keyCode <= 126) { // 所有可打印ASCII字符
                    // 直接输入英文字符、数字和符号
                    inputConnection?.commitText(keyCode.toChar().toString(), 1)
                }
                // 清除所有中文相关状态
                _composingText.value = ""
                _candidates.value = emptyList()
            }
            KeyboardMode.SEARCH -> {
                // 搜索模式下的键盘输入由UI层处理，这里不做任何操作
                // 确保普通的键盘输入不会影响到目标输入框
            }
        }
    }

    fun onCandidateSelected(candidate: String) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(candidate, 1)
        clearState()
    }

    fun onQuestionSelected(question: Question) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(question.text, 1)
        clearState()
    }

    fun switchMode() {
        _currentMode.value = if (_currentMode.value == KeyboardMode.PINYIN) {
            KeyboardMode.SEARCH
        } else {
            KeyboardMode.PINYIN
        }
        clearState()
        Log.d("ExamKeyIME", "Switched mode to ${_currentMode.value}")
    }
    
    fun quickSwitchInputMethod() {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // 直接显示输入法选择器，更可靠
            imm.showInputMethodPicker()
            Log.d("ExamKeyIME", "Input method picker shown")
        } catch (e: Exception) {
            Log.e("ExamKeyIME", "Failed to show input method picker", e)
            // 尝试发送广播来触发输入法切换
            try {
                val intent = android.content.Intent("android.settings.SHOW_INPUT_METHOD_PICKER")
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e("ExamKeyIME", "All input method switch attempts failed", e2)
            }
        }
    }

    fun searchQuestions(query: String) {
        serviceScope.launch {
            // 使用新的匹配算法的最小字符数
            if (query.length >= matchingAlgorithm.getMinimumCharacters()) {
                _searchResults.value = questionRepository.searchByPinyin(query)
            } else {
                _searchResults.value = emptyList()
            }
        }
    }
    
    fun searchQuestionsByType(query: String, questionType: com.example.examkeyime.model.QuestionType?) {
        serviceScope.launch {
            // 使用新的匹配算法的最小字符数
            val minChars = matchingAlgorithm.getMinimumCharacters()
            
            if (query.length >= minChars) {
                val results = questionRepository.searchByPinyinAndType(query, questionType)
                _searchResults.value = results
                Log.d("ExamKeyIME", "Search results updated: ${results.size} items for query: $query, type: $questionType, minChars: $minChars")
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    private fun clearState() {
        _composingText.value = ""
        _candidates.value = emptyList()
        _searchResults.value = emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        pinyinEngine.destroy()
        serviceScope.cancel()
    }

    private fun setupSearchHandler() {
        // No-op for now - real-time search is handled in onKey method
    }
}