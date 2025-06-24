package com.example.examkeyime.ime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.examkeyime.model.KeyboardMode
import com.example.examkeyime.model.Question
import com.example.examkeyime.model.QuestionType
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// 搜索状态枚举
enum class SearchState {
    TYPE_SELECTION,  // 选择题型
    SEARCHING,       // 正在搜索
    SHOWING_DETAIL   // 显示题目详情
}

// 现代化UI主题 - 低饱和度设计
object ModernUITheme {
    // 背景色
    val backgroundColor = Color.parseColor("#F8F9FA")
    val keyboardBackgroundColor = Color.parseColor("#F0F1F3")
    val outputBackgroundColor = Color.parseColor("#FFFFFF")
    
    // 按键颜色 - 降低饱和度
    val keyNormalColor = Color.parseColor("#FAFBFC")
    val keyPressedColor = Color.parseColor("#E1E4E8")
    val keySpecialColor = Color.parseColor("#6C7B7F")  // 降低蓝色饱和度
    val keyDeleteColor = Color.parseColor("#B85450")   // 降低红色饱和度
    
    // 文字颜色
    val textPrimaryColor = Color.parseColor("#2F3942")
    val textSecondaryColor = Color.parseColor("#8B949E")
    val textAccentColor = Color.parseColor("#6C7B7F")
    val textOnAccentColor = Color.parseColor("#FFFFFF")
    
    // 尺寸
    const val keyCornerRadius = 6f
    const val keyElevation = 1f
    const val keyMargin = 3
    const val keyPadding = 10
}

class ExamKeyIME_UI(private val ime: ExamKeyIME, private val context: ExamKeyIME) {
    
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var rootView: LinearLayout
    private lateinit var expandButtonArea: LinearLayout  // 展开按钮区域
    private lateinit var outputArea: LinearLayout  // 动态输出区域
    private lateinit var toolbarArea: LinearLayout  // 快捷功能栏
    private lateinit var keyboardArea: LinearLayout  // 键盘区域
    
    // 搜索状态管理
    private var searchState = SearchState.TYPE_SELECTION
    private var selectedQuestionType: QuestionType? = null
    private var currentSearchQuery = ""
    
    // 折叠状态管理
    private var isOutputCollapsed = false
    private var collapseButton: Button? = null
    
    // 搜索模式下的组件
    private var searchInputView: EditText? = null
    private var searchResultsList: LinearLayout? = null
    
    // 保存搜索结果，用于从详情页返回时恢复
    private var savedSearchResults: List<Question> = emptyList()
    
    fun createView(): View {
        rootView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 16) // 底部间距
            setBackgroundColor(ModernUITheme.backgroundColor)
        }
        
        // 创建展开按钮容器（收起状态时显示）
        createExpandButtonArea()
        
        // 创建动态输出区域（只在搜索模式下显示）
        createOutputArea()
        
        // 创建快捷功能栏
        createToolbarArea()
        
        // 创建键盘区域
        createKeyboardArea()
        
        // 监听状态变化
        observeStateChanges()
        
        return rootView
    }
    
    private fun createExpandButtonArea() {
        expandButtonArea = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE  // 默认隐藏
        }
        rootView.addView(expandButtonArea)
    }
    
    private fun createOutputArea() {
        outputArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ModernUITheme.outputBackgroundColor)
            visibility = View.GONE  // 默认隐藏
            
            // 添加圆角和阴影效果
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ModernUITheme.keyCornerRadius
                setColor(ModernUITheme.outputBackgroundColor)
            }
            background = drawable
        }
        
        rootView.addView(outputArea)
    }
    
    private fun createToolbarArea() {
        toolbarArea = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(12, 10, 12, 10)
            setBackgroundColor(ModernUITheme.keyboardBackgroundColor)
        }
        
        updateToolbar()
        rootView.addView(toolbarArea)
    }
    
    private fun updateToolbar() {
        toolbarArea.removeAllViews()
        
        when (ime.currentMode.value) {
            KeyboardMode.PINYIN -> createNormalModeToolbar()
            KeyboardMode.SEARCH -> createSearchModeToolbar()
        }
    }
    
    private fun createNormalModeToolbar() {
        // 快速切换输入法按钮
        val quickSwitchButton = createStyledButton("quik") {
            ime.quickSwitchInputMethod()
        }
        
        // 返回按钮
        val backButton = createStyledButton("bac") {
            ime.requestHideSelf(0)
        }
        
        // 搜索按钮
        val searchButton = createStyledButton("sea", isSpecial = true) {
            ime.switchMode()
        }
        
        toolbarArea.addView(quickSwitchButton)
        toolbarArea.addView(backButton)
        toolbarArea.addView(searchButton)
    }
    
    private fun createSearchModeToolbar() {
        when (searchState) {
            SearchState.TYPE_SELECTION -> createTypeSelectionToolbar()
            SearchState.SEARCHING -> createSearchingToolbar()
            SearchState.SHOWING_DETAIL -> createDetailToolbar()
        }
    }
    
    private fun createTypeSelectionToolbar() {
        // 返回按钮（常驻）
        val backButton = createStyledButton("bac") {
            ime.switchMode() // 回到普通模式
        }
        
        // 单选题按钮
        val singleChoiceButton = createStyledButton("dan") {
            selectedQuestionType = QuestionType.SINGLE_CHOICE
            searchState = SearchState.SEARCHING
            updateToolbar()
            outputArea.visibility = View.VISIBLE
        }
        
        // 多选题按钮
        val multipleChoiceButton = createStyledButton("duo") {
            selectedQuestionType = QuestionType.MULTIPLE_CHOICE
            searchState = SearchState.SEARCHING
            updateToolbar()
            outputArea.visibility = View.VISIBLE
        }
        
        // 判断题按钮
        val trueFalseButton = createStyledButton("pan") {
            selectedQuestionType = QuestionType.TRUE_FALSE
            searchState = SearchState.SEARCHING
            updateToolbar()
            outputArea.visibility = View.VISIBLE
        }
        
        toolbarArea.addView(backButton)
        toolbarArea.addView(singleChoiceButton)
        toolbarArea.addView(multipleChoiceButton)
        toolbarArea.addView(trueFalseButton)
    }
    
    private fun createSearchingToolbar() {
        // 返回按钮（常驻）
        val backButton = createStyledButton("bac") {
            searchState = SearchState.TYPE_SELECTION
            selectedQuestionType = null
            currentSearchQuery = ""
            updateToolbar()
            outputArea.visibility = View.GONE
            clearSearchResults()
        }
        
        // 搜索输入框
        searchInputView = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "输入拼音搜索${getQuestionTypeName()}..."
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.WHITE)
            textSize = 14f
            
            // 禁用系统输入法
            showSoftInputOnFocus = false
            
            // 恢复之前的搜索内容
            setText(currentSearchQuery)
            setSelection(currentSearchQuery.length)
            
            // 监听文本变化进行实时搜索
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    currentSearchQuery = s?.toString() ?: ""
                    performSearch(currentSearchQuery)
                }
            })
        }
        
        // 发送按钮
        val sendButton = createStyledButton("send") {
            val text = searchInputView?.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                ime.currentInputConnection?.commitText(text, 1)
                // 不切换模式，保持当前状态和输入内容
            }
        }
        
        toolbarArea.addView(backButton)
        toolbarArea.addView(searchInputView)
        toolbarArea.addView(sendButton)
    }
    
    private fun createDetailToolbar() {
        // 返回按钮（常驻）
        val backButton = createStyledButton("bac") {
            searchState = SearchState.TYPE_SELECTION
            selectedQuestionType = null
            currentSearchQuery = ""
            updateToolbar()
            outputArea.visibility = View.GONE
            clearSearchResults()
        }
        
        // 返回题目列表按钮
        val backToListButton = createStyledButton("list") {
            searchState = SearchState.SEARCHING
            updateToolbar()
            // 恢复之前保存的搜索结果
            if (savedSearchResults.isNotEmpty()) {
                updateSearchResults(savedSearchResults)
            } else {
                // 如果没有保存的结果，重新搜索
                val query = currentSearchQuery
                if (query.length >= 4) {
                    performSearch(query)
                } else {
                    updateSearchResults(emptyList())
                }
            }
        }
        
        toolbarArea.addView(backButton)
        toolbarArea.addView(backToListButton)
    }
    
    private fun createStyledButton(text: String, isSpecial: Boolean = false, onClick: () -> Unit): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                68 // 适中的高度
            ).apply {
                setMargins(ModernUITheme.keyMargin, 2, ModernUITheme.keyMargin, 2)
            }
            this.text = text
            textSize = 12f
            setPadding(ModernUITheme.keyPadding, 4, ModernUITheme.keyPadding, 4)
            
            // 低饱和度按钮样式
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ModernUITheme.keyCornerRadius
                setColor(if (isSpecial) ModernUITheme.keySpecialColor else ModernUITheme.keyNormalColor)
                setStroke(1, Color.parseColor("#E1E4E8"))
            }
            background = drawable
            
            setTextColor(if (isSpecial) ModernUITheme.textOnAccentColor else ModernUITheme.textPrimaryColor)
            setOnClickListener { onClick() }
        }
    }
    
    private fun createModernKeyButton(text: String, isSpecial: Boolean = false, onClick: () -> Unit): Button {
        return Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                140, // 大幅增大按键高度
                1f
            ).apply {
                setMargins(4, 4, 4, 4) // 适当增加边距
            }
            this.text = text
            textSize = 22f // 大幅增大字体
            setPadding(12, 12, 12, 12) // 增加内边距
            
            // 低饱和度按键样式
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ModernUITheme.keyCornerRadius
                setColor(when {
                    isSpecial -> ModernUITheme.keySpecialColor
                    text == "删除" -> ModernUITheme.keyDeleteColor
                    else -> ModernUITheme.keyNormalColor
                })
                setStroke(1, Color.parseColor("#E1E4E8"))
            }
            background = drawable
            
            setTextColor(when {
                isSpecial || text == "删除" -> ModernUITheme.textOnAccentColor
                else -> ModernUITheme.textPrimaryColor
            })
            setOnClickListener { onClick() }
        }
    }
    
    private fun getQuestionTypeName(): String {
        return when (selectedQuestionType) {
            QuestionType.SINGLE_CHOICE -> "单选题"
            QuestionType.MULTIPLE_CHOICE -> "多选题"
            QuestionType.TRUE_FALSE -> "判断题"
            else -> "题目"
        }
    }
    
    private fun resetSearchState() {
        searchState = SearchState.TYPE_SELECTION
        selectedQuestionType = null
        currentSearchQuery = ""
        searchInputView = null
        isOutputCollapsed = false
        expandButtonArea.visibility = View.GONE
        clearSearchResults()
    }
    
    private fun createKeyboardArea() {
        keyboardArea = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ModernUITheme.keyboardBackgroundColor)
            setPadding(16, 20, 16, 20) // 增大内边距
        }
        
        // 添加键盘行
        createKeyboardRows()
        
        rootView.addView(keyboardArea)
    }
    
    private fun createKeyboardRows() {
        // 第一行：qwertyuiop
        createKeyRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        
        // 第二行：asdfghjkl
        createKeyRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        
        // 第三行：zxcvbnm + 删除
        val thirdRowKeys = mutableListOf("z", "x", "c", "v", "b", "n", "m")
        createKeyRow(thirdRowKeys, hasDeleteKey = true)
        
        // 第四行：空格键和功能键
        createBottomRow()
    }
    
    private fun createKeyRow(keys: List<String>, hasDeleteKey: Boolean = false) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(4, 6, 4, 6) // 增加垂直间距
        }
        
        for (key in keys) {
            val keyButton = createModernKeyButton(key, false) {
                when (ime.currentMode.value) {
                    KeyboardMode.PINYIN -> ime.onKey(key[0].code)
                    KeyboardMode.SEARCH -> {
                        // 只有在搜索状态下才处理键盘输入到搜索框
                        if (searchState == SearchState.SEARCHING) {
                            handleSearchInput(key)
                        }
                        // 其他状态下忽略键盘输入
                    }
                }
            }
            row.addView(keyButton)
        }
        
        if (hasDeleteKey) {
            val deleteButton = Button(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    110, // 与其他按键保持一致
                    1.5f
                ).apply {
                    setMargins(ModernUITheme.keyMargin, 3, ModernUITheme.keyMargin, 3)
                }
                text = "删除"
                textSize = 13f
                
                // 删除按钮特殊样式
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ModernUITheme.keyCornerRadius
                    setColor(ModernUITheme.keyDeleteColor)
                    setStroke(1, Color.parseColor("#E1E4E8"))
                }
                background = drawable
                setTextColor(ModernUITheme.textOnAccentColor)
                
                setOnClickListener { 
                    when (ime.currentMode.value) {
                        KeyboardMode.PINYIN -> ime.onKey(-5)
                        KeyboardMode.SEARCH -> {
                            // 只有在搜索状态下才处理删除到搜索框
                            if (searchState == SearchState.SEARCHING) {
                                handleSearchDelete()
                            }
                            // 其他状态下忽略删除操作
                        }
                    }
                }
            }
            row.addView(deleteButton)
        }
        
        keyboardArea.addView(row)
    }
    
    
    private fun createBottomRow() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(4, 4, 4, 4)
        }
        
        // 常用符号按钮
        val commaButton = createModernKeyButton(",", false) {
            when (ime.currentMode.value) {
                KeyboardMode.PINYIN -> ime.onKey(44) // 逗号
                KeyboardMode.SEARCH -> {
                    if (searchState == SearchState.SEARCHING) {
                        handleSearchInput(",")
                    }
                }
            }
        }
        row.addView(commaButton)
        
        // 句号按钮
        val periodButton = createModernKeyButton(".", false) {
            when (ime.currentMode.value) {
                KeyboardMode.PINYIN -> ime.onKey(46) // 句号
                KeyboardMode.SEARCH -> {
                    if (searchState == SearchState.SEARCHING) {
                        handleSearchInput(".")
                    }
                }
            }
        }
        row.addView(periodButton)
        
        // 空格键（占据更多空间）
        val spaceButton = Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                140, // 与其他按键保持一致的大高度
                4f // 占据更多权重
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            text = "空格"
            textSize = 20f // 增大字体
            
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ModernUITheme.keyCornerRadius
                setColor(ModernUITheme.keyNormalColor)
                setStroke(1, Color.parseColor("#E1E4E8"))
            }
            background = drawable
            setTextColor(ModernUITheme.textPrimaryColor)
            
            setOnClickListener { 
                when (ime.currentMode.value) {
                    KeyboardMode.PINYIN -> ime.onKey(32)
                    KeyboardMode.SEARCH -> {
                        if (searchState == SearchState.SEARCHING) {
                            handleSearchInput(" ")
                        }
                    }
                }
            }
        }
        row.addView(spaceButton)
        
        // 换行按钮
        val enterButton = createModernKeyButton("换行", true) {
            when (ime.currentMode.value) {
                KeyboardMode.PINYIN -> ime.onKey(10) // 回车键
                KeyboardMode.SEARCH -> {
                    if (searchState == SearchState.SEARCHING) {
                        handleSearchInput("\n")
                    }
                }
            }
        }
        row.addView(enterButton)
        
        keyboardArea.addView(row)
    }
    
    private fun handleSearchInput(char: String) {
        searchInputView?.let { editText ->
            val currentText = editText.text.toString()
            val newText = currentText + char
            editText.setText(newText)
            editText.setSelection(newText.length)
        }
    }
    
    private fun handleSearchDelete() {
        searchInputView?.let { editText ->
            val currentText = editText.text.toString()
            if (currentText.isNotEmpty()) {
                val newText = currentText.dropLast(1)
                editText.setText(newText)
                editText.setSelection(newText.length)
            }
        }
    }
    
    private fun performSearch(query: String) {
        if (searchState != SearchState.SEARCHING) return
        
        if (query.length >= 4) { // 改为最少4字符才开始搜索
            // 进行分类型搜索
            ime.searchQuestionsByType(query, selectedQuestionType)
        } else {
            // 清空结果
            clearSearchResults()
        }
    }
    
    private fun observeStateChanges() {
        uiScope.launch {
            ime.currentMode.collect { mode ->
                updateModeDisplay(mode)
            }
        }
        
        uiScope.launch {
            ime.candidates.collect { candidates ->
                if (ime.currentMode.value == KeyboardMode.PINYIN) {
                    updateCandidates(candidates)
                }
            }
        }
        
        uiScope.launch {
            ime.searchResults.collect { results ->
                // 更加精确的条件判断，确保在正确的状态下更新
                if (ime.currentMode.value == KeyboardMode.SEARCH && 
                    searchState == SearchState.SEARCHING && 
                    selectedQuestionType != null) {
                    updateSearchResults(results)
                }
            }
        }
    }
    
    private fun updateModeDisplay(mode: KeyboardMode) {
        when (mode) {
            KeyboardMode.PINYIN -> {
                outputArea.visibility = View.VISIBLE
                expandButtonArea.visibility = View.GONE
                isOutputCollapsed = false
                resetSearchState()
                // 显示拼音候选词
                updateCandidates(ime.candidates.value)
            }
            KeyboardMode.SEARCH -> {
                searchState = SearchState.TYPE_SELECTION
                outputArea.visibility = View.GONE
                expandButtonArea.visibility = View.GONE
                isOutputCollapsed = false
                clearSearchResults()
            }
        }
        updateToolbar()
    }
    
    private fun updateSearchResults(results: List<Question>) {
        // 保存搜索结果，用于从详情页返回时恢复
        savedSearchResults = results
        
        outputArea.removeAllViews()
        
        // 添加折叠按钮到输出框左上角
        addCollapseButton()
        
        if (results.isEmpty()) {
            val noResultsView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = "没有找到相关${getQuestionTypeName()}"
                textSize = 14f
                setPadding(16, 20, 16, 20)
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
            }
            outputArea.addView(noResultsView)
            return
        }
        
        // 创建滚动容器
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300 // 约4行文字的高度，降低了100像素
            )
        }
        
        searchResultsList = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 显示前10个结果
        for (question in results.take(10)) {
            val questionView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = question.text
                textSize = 14f
                setPadding(16, 12, 16, 12)
                setBackgroundColor(Color.WHITE)
                
                // 添加分割线效果
                val params = layoutParams as LinearLayout.LayoutParams
                params.setMargins(0, 0, 0, 2)
                layoutParams = params
                
                setOnClickListener { showQuestionDetail(question) }
            }
            searchResultsList!!.addView(questionView)
        }
        
        scrollView.addView(searchResultsList)
        outputArea.addView(scrollView)
    }
    
    private fun clearSearchResults() {
        outputArea.removeAllViews()
        searchResultsList = null
    }
    
    private fun updateCandidates(candidates: List<String>) {
        if (ime.currentMode.value != KeyboardMode.PINYIN) return
        
        outputArea.removeAllViews()
        
        if (candidates.isEmpty()) {
            outputArea.visibility = View.GONE
            return
        }
        
        outputArea.visibility = View.VISIBLE
        
        // 添加折叠按钮到输出框左上角（仅在有候选词时）
        addCollapseButton()
        
        // 创建横向滚动的候选词
        val horizontalScrollView = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        val candidateRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
        }
        
        for (candidate in candidates.take(8)) {
            val candidateView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(6, 6, 6, 6)
                }
                text = candidate
                textSize = 16f
                setPadding(16, 10, 16, 10)
                
                // 候选词现代化样式
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ModernUITheme.keyCornerRadius
                    setColor(ModernUITheme.keyNormalColor)
                    setStroke(1, Color.parseColor("#D1D1D6"))
                }
                background = drawable
                setTextColor(ModernUITheme.textPrimaryColor)
                setOnClickListener { ime.onCandidateSelected(candidate) }
            }
            candidateRow.addView(candidateView)
        }
        
        horizontalScrollView.addView(candidateRow)
        outputArea.addView(horizontalScrollView)
    }
    
    private fun showQuestionDetail(question: Question) {
        searchState = SearchState.SHOWING_DETAIL
        updateToolbar()
        
        outputArea.removeAllViews()
        
        // 添加折叠按钮到输出框左上角
        addCollapseButton()
        
        // 创建滚动容器
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300 // 调整为和搜索结果一致的高度
            )
        }
        
        val detailContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }
        
        // 题目详情
        val detailView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = buildQuestionDetailText(question)
            textSize = 14f
            setBackgroundColor(Color.WHITE)
        }
        
        detailContainer.addView(detailView)
        scrollView.addView(detailContainer)
        outputArea.addView(scrollView)
    }
    
    private fun buildQuestionDetailText(question: Question): String {
        val sb = StringBuilder()
        sb.append("【题目】${question.text}\n\n")
        
        if (question.options.isNotEmpty()) {
            sb.append("【选项】\n")
            question.options.forEach { option ->
                sb.append("$option\n")
            }
            sb.append("\n")
        }
        
        sb.append("【答案】")
        when (question.type) {
            QuestionType.TRUE_FALSE -> {
                sb.append(if (question.correct.firstOrNull() == "T") "正确" else "错误")
            }
            else -> {
                question.correct.forEach { answer ->
                    val option = question.options.find { it.startsWith("$answer、") }
                    if (option != null) {
                        sb.append("$option ")
                    } else {
                        sb.append("$answer ")
                    }
                }
            }
        }
        
        return sb.toString()
    }
    
    private fun addCollapseButton() {
        // 创建折叠按钮容器
        val buttonContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 4, 8, 0)
        }
        
        // 折叠按钮（小圆圈）
        val collapseBtn = Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(50, 50).apply { // 稍微大一点
                setMargins(30, 0, 0, 0) // 离左边30像素
            }
            text = "−"
            textSize = 16f
            setBackgroundColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setOnClickListener {
                toggleOutputCollapse()
            }
        }
        
        buttonContainer.addView(collapseBtn)
        outputArea.addView(buttonContainer, 0) // 添加到第一个位置
        collapseButton = collapseBtn
    }
    
    private fun toggleOutputCollapse() {
        if (isOutputCollapsed) {
            // 展开：显示输出区域，隐藏展开按钮区域
            outputArea.visibility = View.VISIBLE
            expandButtonArea.visibility = View.GONE
            isOutputCollapsed = false
        } else {
            // 收起：隐藏输出区域，显示展开按钮区域
            outputArea.visibility = View.GONE
            showExpandButton()
            isOutputCollapsed = true
        }
    }
    
    private fun showExpandButton() {
        expandButtonArea.removeAllViews()
        
        // 创建展开按钮（位于返回按钮上方）
        val expandBtn = Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(50, 50).apply { // 稍微大一点
                setMargins(30, 0, 0, 0) // 离左边30像素
            }
            text = "+"
            textSize = 16f
            setBackgroundColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setOnClickListener {
                toggleOutputCollapse()
            }
        }
        
        expandButtonArea.addView(expandBtn)
        expandButtonArea.visibility = View.VISIBLE
    }
}