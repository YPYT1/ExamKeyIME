package com.example.examkeyime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.examkeyime.algorithm.MatchingAlgorithm
import com.example.examkeyime.data.QuestionRepository
import com.example.examkeyime.util.FileManager
import com.example.examkeyime.util.MarkdownParser
import com.google.gson.Gson
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var fileManager: FileManager
    private lateinit var markdownParser: MarkdownParser
    private lateinit var matchingAlgorithm: MatchingAlgorithm
    
    // 导航相关
    private lateinit var bottomNavigation: LinearLayout
    private lateinit var contentContainer: FrameLayout
    private var currentTab = 0
    
    // 页面内容
    private lateinit var homePageContent: LinearLayout
    private lateinit var uploadPageContent: LinearLayout
    private lateinit var algorithmPageContent: LinearLayout
    
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileUpload(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化工具类
        fileManager = FileManager(this)
        markdownParser = MarkdownParser()
        matchingAlgorithm = MatchingAlgorithm(this)
        
        // 创建现代化主布局
        createModernMainLayout()
        
        // 加载数据
        loadStats()
    }
    
    private fun createModernMainLayout() {
        // 主容器
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F9FA"))
        }
        
        // 顶部标题栏
        createTopBar(mainLayout)
        
        // 内容容器
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        mainLayout.addView(contentContainer)
        
        // 底部导航栏
        createBottomNavigation(mainLayout)
        
        // 创建各个页面内容
        createHomePageContent()
        createUploadPageContent()
        createAlgorithmPageContent()
        
        // 显示默认页面
        showTab(0)
        
        setContentView(mainLayout)
    }
    
    // 创建现代化顶部标题栏
    private fun createTopBar(parent: LinearLayout) {
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = createGradientDrawable("#2196F3", 24f)
            setPadding(24, 48, 24, 32)
            elevation = 8f
        }
        
        val titleText = TextView(this).apply {
            text = "ExamKey输入法"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        topBar.addView(titleText)
        parent.addView(topBar)
    }
    
    // 创建底部导航栏
    private fun createBottomNavigation(parent: LinearLayout) {
        bottomNavigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            )
            setBackgroundColor(Color.WHITE)
            elevation = 16f
        }
        
        // 首页标签
        val homeTab = createNavTab("🏠", "首页", 0)
        bottomNavigation.addView(homeTab)
        
        // 上传页标签
        val uploadTab = createNavTab("📤", "上传", 1)
        bottomNavigation.addView(uploadTab)
        
        // 算法页标签
        val algorithmTab = createNavTab("⚙️", "算法", 2)
        bottomNavigation.addView(algorithmTab)
        
        parent.addView(bottomNavigation)
    }
    
    // 创建导航标签
    private fun createNavTab(icon: String, text: String, tabIndex: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
            setPadding(8, 12, 8, 12)
            
            // 选中状态的背景
            if (tabIndex == currentTab) {
                background = createGradientDrawable("#E3F2FD", 16f)
            }
            
            setOnClickListener { showTab(tabIndex) }
            
            val iconView = TextView(this@MainActivity).apply {
                this.text = icon
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(if (tabIndex == currentTab) Color.parseColor("#2196F3") else Color.parseColor("#757575"))
            }
            addView(iconView)
            
            val textView = TextView(this@MainActivity).apply {
                this.text = text
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (tabIndex == currentTab) Color.parseColor("#2196F3") else Color.parseColor("#757575"))
                setPadding(0, 4, 0, 0)
            }
            addView(textView)
        }
    }
    
    // 显示指定页面
    private fun showTab(tabIndex: Int) {
        currentTab = tabIndex
        contentContainer.removeAllViews()
        
        when (tabIndex) {
            0 -> {
                contentContainer.addView(homePageContent)
                loadStats()
            }
            1 -> contentContainer.addView(uploadPageContent)
            2 -> contentContainer.addView(algorithmPageContent)
        }
        
        // 更新导航栏样式
        updateNavStyles()
    }
    
    // 更新导航栏样式
    private fun updateNavStyles() {
        for (i in 0 until bottomNavigation.childCount) {
            val tab = bottomNavigation.getChildAt(i) as LinearLayout
            if (i == currentTab) {
                tab.background = createGradientDrawable("#E3F2FD", 16f)
            } else {
                tab.background = null
            }
            
            val iconView = tab.getChildAt(0) as TextView
            val textView = tab.getChildAt(1) as TextView
            
            val color = if (i == currentTab) Color.parseColor("#2196F3") else Color.parseColor("#757575")
            iconView.setTextColor(color)
            textView.setTextColor(color)
        }
    }

    // 创建首页内容
    private fun createHomePageContent() {
        homePageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val scrollView = ScrollView(this)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // 欢迎卡片
        val welcomeCard = createModernCard()
        val welcomeContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val welcomeTitle = TextView(this).apply {
            text = "📊 题库统计"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 16)
        }
        welcomeContent.addView(welcomeTitle)
        
        val loadingText = TextView(this).apply {
            text = "正在加载题库统计..."
            textSize = 16f
            setTextColor(Color.parseColor("#666666"))
        }
        welcomeContent.addView(loadingText)
        
        welcomeCard.addView(welcomeContent)
        scrollContent.addView(welcomeCard)
        
        scrollView.addView(scrollContent)
        homePageContent.addView(scrollView)
    }

    // 创建上传页面内容
    private fun createUploadPageContent() {
        uploadPageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val scrollView = ScrollView(this)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // 上传卡片
        val uploadCard = createModernCard()
        val uploadContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val uploadTitle = TextView(this).apply {
            text = "📤 文档上传"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 16)
        }
        uploadContent.addView(uploadTitle)
        
        val uploadDesc = TextView(this).apply {
            text = "支持MD格式文件，自动解析题库内容"
            textSize = 16f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 0, 0, 24)
        }
        uploadContent.addView(uploadDesc)
        
        // 按钮区域
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24)
        }
        
        val uploadButton = createActionButton("选择文件", "#4CAF50") {
            filePickerLauncher.launch("*/*")
        }
        buttonContainer.addView(uploadButton)
        
        val formatButton = createActionButton("格式说明", "#FF9800") {
            showFormatReference()
        }
        buttonContainer.addView(formatButton)
        
        uploadContent.addView(buttonContainer)
        
        // 文件列表
        val fileListTitle = TextView(this).apply {
            text = "已上传文件"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 16)
        }
        uploadContent.addView(fileListTitle)
        
        val fileListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        updateFileList(fileListContainer)
        uploadContent.addView(fileListContainer)
        
        uploadCard.addView(uploadContent)
        scrollContent.addView(uploadCard)
        
        scrollView.addView(scrollContent)
        uploadPageContent.addView(scrollView)
    }

    // 创建算法页面内容
    private fun createAlgorithmPageContent() {
        algorithmPageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val scrollView = ScrollView(this)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // 算法设置卡片
        val algorithmCard = createModernCard()
        val algorithmContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val algorithmTitle = TextView(this).apply {
            text = "⚙️ 匹配算法设置"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 16)
        }
        algorithmContent.addView(algorithmTitle)
        
        // 当前级别显示
        val currentLevelText = TextView(this).apply {
            text = "当前级别：${getLevelDisplayName(matchingAlgorithm.getCurrentLevel())}"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2196F3"))
            setPadding(0, 0, 0, 24)
        }
        algorithmContent.addView(currentLevelText)
        
        // 级别选择
        MatchingAlgorithm.MatchingLevel.values().forEach { level ->
            val levelButton = createLevelSelectionCard(level, currentLevelText)
            algorithmContent.addView(levelButton)
        }
        
        // 说明文本
        val explanationText = TextView(this).apply {
            text = getAlgorithmExplanation()
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(16, 24, 16, 0)
            background = createGradientDrawable("#F5F5F5", 12f)
        }
        algorithmContent.addView(explanationText)
        
        algorithmCard.addView(algorithmContent)
        scrollContent.addView(algorithmCard)
        
        scrollView.addView(scrollContent)
        algorithmPageContent.addView(scrollView)
    }
    
    // 创建现代化卡片
    private fun createModernCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            background = createGradientDrawable("#FFFFFF", 20f)
            elevation = 6f
        }
    }
    
    // 创建圆角背景
    private fun createGradientDrawable(color: String, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radius
        }
    }
    
    // 创建图标按钮
    private fun createIconButton(icon: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = icon
            textSize = 20f
            layoutParams = LinearLayout.LayoutParams(60, 60)
            background = createGradientDrawable("#FFFFFF", 30f)
            setTextColor(Color.parseColor("#2196F3"))
            setOnClickListener { onClick() }
        }
    }
    
    // 创建操作按钮
    private fun createActionButton(text: String, color: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 16, 0)
            }
            background = createGradientDrawable(color, 25f)
            setTextColor(Color.WHITE)
            elevation = 4f
            setOnClickListener { onClick() }
        }
    }
    
    // 创建级别选择卡片
    private fun createLevelSelectionCard(level: MatchingAlgorithm.MatchingLevel, currentLevelText: TextView): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            setPadding(20, 16, 20, 16)
            
            val isSelected = level == matchingAlgorithm.getCurrentLevel()
            background = if (isSelected) {
                createGradientDrawable("#E3F2FD", 16f).apply {
                    setStroke(3, Color.parseColor("#2196F3"))
                }
            } else {
                createGradientDrawable("#F8F9FA", 16f)
            }
            
            setOnClickListener {
                matchingAlgorithm.setMatchingLevel(level)
                currentLevelText.text = "当前级别：${getLevelDisplayName(level)}"
                recreateAlgorithmPage()
            }
        }
        
        val titleText = TextView(this).apply {
            text = getLevelDisplayName(level)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
        }
        card.addView(titleText)
        
        val descText = TextView(this).apply {
            text = matchingAlgorithm.getLevelDescription(level)
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 8, 0, 4)
        }
        card.addView(descText)
        
        val exampleText = TextView(this).apply {
            text = matchingAlgorithm.getLevelExample(level)
            textSize = 13f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 4, 0, 0)
        }
        card.addView(exampleText)
        
        return card
    }
    
    // 重新创建算法页面
    private fun recreateAlgorithmPage() {
        createAlgorithmPageContent()
        if (currentTab == 2) {
            contentContainer.removeAllViews()
            contentContainer.addView(algorithmPageContent)
        }
    }
    
    // 获取级别显示名称
    private fun getLevelDisplayName(level: MatchingAlgorithm.MatchingLevel): String {
        return when (level) {
            MatchingAlgorithm.MatchingLevel.LOW -> "低严格度"
            MatchingAlgorithm.MatchingLevel.MEDIUM -> "中等严格度"
            MatchingAlgorithm.MatchingLevel.HIGH -> "高严格度"
        }
    }
    
    // 获取算法说明
    private fun getAlgorithmExplanation(): String {
        return """
💡 算法说明

• 低严格度：输入2个字符开始匹配，包含所有字符即可，长度无限制
• 中等严格度：输入4个字符开始匹配，要求字符相邻出现，长度无限制  
• 高严格度：输入6个字符开始匹配，严格按拼音音节匹配，长度无限制

⚠️ 修改设置后立即生效，影响搜索行为
        """.trimIndent()
    }
    
    // 加载统计数据
    private fun loadStats() {
        val repository = QuestionRepository(this)
        mainScope.launch {
            repository.loadQuestions()
            val stats = repository.getStats()
            val total = repository.getTotalCount()
            displayStats(total, stats)
        }
    }
    
    // 显示统计数据
    private fun displayStats(total: Int, stats: Map<String, Int>) {
        if (currentTab != 0) return
        
        val scrollView = homePageContent.getChildAt(0) as ScrollView
        val scrollContent = scrollView.getChildAt(0) as LinearLayout
        val welcomeCard = scrollContent.getChildAt(0) as LinearLayout
        val welcomeContent = welcomeCard.getChildAt(0) as LinearLayout
        
        // 清除加载文本
        if (welcomeContent.childCount > 1) {
            welcomeContent.removeViewAt(1)
        }
        
        // 添加统计卡片
        val statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // 总数卡片
        statsContainer.addView(createStatCard("题目总数", total.toString(), "#4CAF50"))
        
        // 分类统计
        stats.forEach { (name, count) ->
            statsContainer.addView(createStatCard(name, count.toString(), "#2196F3"))
        }
        
        // 数据源提示
        val sourceText = TextView(this).apply {
            text = if (fileManager.getParsedFiles().isNotEmpty()) "📁 当前使用：用户上传题库" else "📚 当前使用：默认题库"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 24, 0, 24)
            gravity = Gravity.CENTER
        }
        statsContainer.addView(sourceText)
        
        // 输入法设置按钮容器
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }
        val settingsButton = createActionButton("⚙️ 输入法设置", "#2196F3") {
            showInputMethodSettings()
        }
        buttonContainer.addView(settingsButton)
        statsContainer.addView(buttonContainer)
        
        welcomeContent.addView(statsContainer)
    }
    
    // 创建统计卡片
    private fun createStatCard(title: String, value: String, valueColor: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            setPadding(20, 16, 20, 16)
            background = createGradientDrawable("#F8F9FA", 12f)

            val titleView = TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            addView(titleView)

            val valueView = TextView(this@MainActivity).apply {
                text = value
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(valueColor))
            }
            addView(valueView)
        }
    }
    
    // 更新文件列表
    private fun updateFileList(container: LinearLayout) {
        container.removeAllViews()
        
        val files = fileManager.getAllFiles()
        if (files.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "📄 暂无文件"
                textSize = 16f
                setTextColor(Color.parseColor("#999999"))
                setPadding(20, 20, 20, 20)
                gravity = Gravity.CENTER
                background = createGradientDrawable("#F8F9FA", 12f)
            }
            container.addView(emptyText)
        } else {
            files.forEach { fileInfo ->
                val fileItem = createFileItem(fileInfo) {
                    if (fileManager.deleteFile(fileInfo.name, fileInfo.type)) {
                        Toast.makeText(this, "文件已删除", Toast.LENGTH_SHORT).show()
                        updateFileList(container)
                        if (currentTab == 0) loadStats()
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }
                container.addView(fileItem)
            }
        }
    }
    
    // 创建文件项
    private fun createFileItem(fileInfo: com.example.examkeyime.util.FileInfo, onDelete: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            background = createGradientDrawable("#FFFFFF", 12f)
            elevation = 2f
            
            val fileIcon = TextView(this@MainActivity).apply {
                text = if (fileInfo.type.name == "UPLOADED") "📄" else "📊"
                textSize = 20f
                setPadding(0, 0, 16, 0)
            }
            addView(fileIcon)
            
            val fileText = TextView(this@MainActivity).apply {
                text = "${fileInfo.name}\n${if (fileInfo.type.name == "UPLOADED") "原始文件" else "解析文件"}"
                textSize = 14f
                setTextColor(Color.parseColor("#1A1A1A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(fileText)
            
            val deleteButton = Button(this@MainActivity).apply {
                text = "🗑"
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(50, 50)
                background = createGradientDrawable("#FF5252", 25f)
                setTextColor(Color.WHITE)
                setOnClickListener { onDelete() }
            }
            addView(deleteButton)
        }
    }

    // 处理文件上传
    private fun handleFileUpload(uri: Uri) {
        mainScope.launch {
            try {
                val fileName = getFileName(uri) ?: "unknown.md"
                if (!fileName.endsWith(".md", ignoreCase = true)) {
                    Toast.makeText(this@MainActivity, "请选择MD格式文件", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val savedFileName = fileManager.saveUploadedFile(uri, fileName)
                if (savedFileName != null) {
                    Toast.makeText(this@MainActivity, "文件上传成功", Toast.LENGTH_SHORT).show()
                    
                    val content = fileManager.readUploadedFile(savedFileName)
                    if (content != null) {
                        val questions = markdownParser.parseMarkdownToQuestions(content)
                        if (questions.isNotEmpty()) {
                            val jsonContent = Gson().toJson(questions)
                            val parsedFileName = fileManager.saveParsedQuestions(jsonContent, savedFileName)
                            if (parsedFileName != null) {
                                Toast.makeText(this@MainActivity, "题库解析完成，共${questions.size}道题", Toast.LENGTH_LONG).show()
                                refreshPageContent()
                            }
                        } else {
                            showParseErrorDialog(fileName, "未能解析出有效题目，请检查文件格式")
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity, "文件上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val fileName = getFileName(uri) ?: "unknown.md"
                showParseErrorDialog(fileName, "解析失败：${e.message}")
            }
        }
    }
    
    private fun refreshPageContent() {
        when (currentTab) {
            0 -> loadStats()
            1 -> {
                val scrollView = uploadPageContent.getChildAt(0) as ScrollView
                val scrollContent = scrollView.getChildAt(0) as LinearLayout
                val uploadCard = scrollContent.getChildAt(0) as LinearLayout
                val uploadContent = uploadCard.getChildAt(0) as LinearLayout
                val fileListContainer = uploadContent.getChildAt(4) as LinearLayout
                updateFileList(fileListContainer)
            }
        }
    }
    
    private fun showParseErrorDialog(fileName: String, errorMessage: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("解析失败")
            .setMessage("文件 $fileName 解析失败\n\n错误信息：$errorMessage\n\n请检查文件格式是否符合规范。")
            .setPositiveButton("查看格式说明") { _, _ -> 
                showFormatReference()
            }
            .setNegativeButton("确定", null)
            .create()
        dialog.show()
    }
    
    private fun showFormatReference() {
        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val title = TextView(this).apply {
            text = "📋 MD文件格式参考"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            setPadding(0, 0, 0, 20)
        }
        content.addView(title)
        
        val formatGuide = TextView(this).apply {
            text = getFormatGuideText()
            textSize = 12f
            setTextColor(Color.parseColor("#333333"))
            typeface = Typeface.MONOSPACE
            background = createGradientDrawable("#F5F5F5", 8f)
            setPadding(16, 16, 16, 16)
        }
        content.addView(formatGuide)
        
        scrollView.addView(content)
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("关闭", null)
            .create()
        dialog.show()
    }
    
    private fun getFormatGuideText(): String {
        return """
【重要说明】
请严格按照以下格式创建MD文件，建议分别创建：
• 单选题.md
• 多选题.md  
• 判断题.md

【单选题格式】
1、马克思主义是___________。
A、关于无产阶级和人类解放的科学理论
B、人民大众思想的科学体系
C、革命阶级思想的科学体系
D、革命政党思想的科学体系
答案：A

【多选题格式】
1、马克思主义的组成部分包括（    ）。
A、马克思主义哲学
B、马克思主义政治经济学
C、科学社会主义
D、马克思主义文艺理论
答案：ABC

【判断题格式】
1、时间和空间是运动着的物质的基本存在形式。
答案：正确

【格式要求】
1. 题目编号：支持 1、1. 1) (1) 第1题 等格式
2. 选项：必须用 A、B、C、D 开头
3. 答案行：必须以"答案："开头
4. 判断题答案：用"正确"、"错误"、"对"、"错"、"T"、"F"等
5. 多选题答案：直接写字母组合，如"ABC"
6. 每题之间用空行分隔
7. 文件编码必须是UTF-8
        """.trimIndent()
    }
    
    private fun showInputMethodSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }
    
    private fun getFileName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }
}