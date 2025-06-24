package com.example.examkeyime.pinyin

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.examkeyime.util.PinyinUtils

/**
 * 专业的拼音输入引擎
 * 提供拼音到汉字的智能转换功能
 */
open class PinyinEngine(protected val context: Context) {
    
    // 拼音词典：拼音 -> 候选词列表
    private val pinyinDict = mutableMapOf<String, MutableList<CandidateWord>>()
    
    // 候选词数据类
    data class CandidateWord(
        val word: String,       // 词语
        val frequency: Int = 0  // 使用频率
    )
    
    // 常用词库（简化版）
    private val commonWords = mapOf(
        // 单字
        "ni" to listOf("你", "泥", "倪", "尼", "呢", "妮", "逆", "匿", "拟", "腻"),
        "hao" to listOf("好", "号", "浩", "豪", "耗", "郝", "毫", "嚎", "壕", "蒿"),
        "ma" to listOf("吗", "妈", "马", "麻", "码", "蚂", "骂", "嘛", "玛", "蟆"),
        "de" to listOf("的", "得", "地", "德", "底"),
        "wo" to listOf("我", "窝", "沃", "握", "斡", "卧", "渥", "蜗", "涡", "挝"),
        "shi" to listOf("是", "时", "十", "使", "世", "市", "师", "诗", "式", "士", "事", "史", "识", "石", "拾", "食", "始", "试", "视"),
        "bu" to listOf("不", "部", "步", "布", "补", "捕", "卜", "哺", "埠", "簿"),
        "zai" to listOf("在", "再", "载", "栽", "灾", "宰", "哉", "仔", "崽"),
        "ren" to listOf("人", "任", "认", "仁", "忍", "韧", "刃", "纫", "壬", "饪"),
        "you" to listOf("有", "由", "又", "右", "油", "游", "友", "优", "尤", "忧", "幼", "诱", "悠", "邮", "犹", "佑", "釉"),
        "he" to listOf("和", "何", "合", "河", "核", "盒", "贺", "喝", "赫", "荷", "鹤", "褐"),
        "ta" to listOf("他", "她", "它", "踏", "塌", "塔", "獭", "挞", "蹋", "榻"),
        "men" to listOf("们", "门", "闷", "扪", "焖", "懑"),
        "zhe" to listOf("这", "着", "者", "折", "遮", "哲", "蔗", "锗", "褶", "辙"),
        "ge" to listOf("个", "各", "格", "歌", "哥", "割", "革", "葛", "隔", "戈", "鸽", "搁", "疙", "咯"),
        "zhong" to listOf("中", "种", "重", "众", "终", "钟", "忠", "衷", "肿", "仲", "踵"),
        "guo" to listOf("国", "过", "果", "郭", "锅", "裹", "帼", "椁", "蝈", "虢"),
        "shuo" to listOf("说", "硕", "朔", "烁", "蒴", "槊", "铄"),
        "dou" to listOf("都", "斗", "豆", "逗", "兜", "抖", "陡", "痘", "窦", "蚪"),
        "hui" to listOf("会", "回", "挥", "汇", "灰", "绘", "贿", "惠", "毁", "慧", "秽", "烩", "讳", "诲"),
        "yao" to listOf("要", "药", "遥", "腰", "瑶", "摇", "尧", "窑", "谣", "姚", "咬", "邀", "爻", "吆"),
        "jiu" to listOf("就", "九", "酒", "久", "救", "旧", "究", "纠", "舅", "灸", "疚", "鸠", "咎"),
        "xiang" to listOf("想", "向", "相", "像", "项", "象", "响", "乡", "香", "详", "享", "祥", "箱", "襄", "湘", "翔"),
        "kan" to listOf("看", "砍", "堪", "坎", "刊", "瞰", "侃", "勘", "龛", "戡"),
        "lai" to listOf("来", "赖", "莱", "濑", "籁", "涞", "徕", "睐"),
        "ke" to listOf("可", "科", "克", "客", "刻", "课", "颗", "棵", "柯", "磕", "咳", "渴", "坷", "苛"),
        "yi" to listOf("一", "以", "已", "意", "义", "益", "亿", "易", "医", "艺", "食", "仪", "衣", "依", "移"),
        "jing" to listOf("经", "京", "精", "惊", "晶", "睛", "景", "境", "静", "镜", "径", "竞", "净", "敬"),
        "chang" to listOf("常", "长", "场", "厂", "昌", "畅", "尝", "肠", "偿", "倡", "唱", "猖"),
        
        // 双字词
        "nihao" to listOf("你好", "泥蒿", "拟好"),
        "beijing" to listOf("北京", "背景", "背井"),
        "shanghai" to listOf("上海"),
        "zhongguo" to listOf("中国", "中过", "忠国"),
        "pengyou" to listOf("朋友"),
        "laoshi" to listOf("老师", "老是", "老实"),
        "xuesheng" to listOf("学生", "学声"),
        "dianhua" to listOf("电话", "电画"),
        "shouji" to listOf("手机", "收集", "收急"),
        "diannao" to listOf("电脑"),
        "gongzuo" to listOf("工作"),
        "xuexiao" to listOf("学校", "学笑", "学效"),
        "jiating" to listOf("家庭"),
        "shijian" to listOf("时间", "实践", "视奸"),
        "wenti" to listOf("问题"),
        "xiexie" to listOf("谢谢"),
        "zaijian" to listOf("再见", "在见"),
        "mingbai" to listOf("明白"),
        "zhidao" to listOf("知道", "直到"),
        "xihuan" to listOf("喜欢"),
        
        // 三字词
        "xiaopengyou" to listOf("小朋友"),
        "xiaojiejie" to listOf("小姐姐"),
        "daxuesheng" to listOf("大学生"),
        "jisuanji" to listOf("计算机"),
        
        // 四字词
        "tianqiyubao" to listOf("天气预报"),
        "shengrikuaile" to listOf("生日快乐"),
        "xinniankaule" to listOf("新年快乐"),
    )
    
    init {
        // 初始化词典
        loadDictionary()
    }
    
    /**
     * 加载词典
     */
    private fun loadDictionary() {
        // 将常用词加载到词典中
        commonWords.forEach { (pinyin, words) ->
            pinyinDict[pinyin] = words.mapIndexed { index, word ->
                CandidateWord(word, 1000 - index * 10) // 按顺序设置频率
            }.toMutableList()
        }
    }
    
    /**
     * 获取拼音候选词
     * @param pinyin 输入的拼音
     * @return 候选词列表
     */
    open fun getCandidates(pinyin: String): List<String> {
        if (pinyin.isEmpty()) return emptyList()
        
        val candidates = mutableListOf<String>()
        val lowerPinyin = pinyin.lowercase()
        
        // 1. 精确匹配
        pinyinDict[lowerPinyin]?.let { words ->
            candidates.addAll(words.sortedByDescending { it.frequency }.map { it.word })
        }
        
        // 2. 前缀匹配（如果精确匹配结果较少）
        if (candidates.size < 5) {
            pinyinDict.entries
                .filter { it.key.startsWith(lowerPinyin) && it.key != lowerPinyin }
                .sortedBy { it.key.length }
                .forEach { (_, words) ->
                    words.sortedByDescending { it.frequency }
                        .map { it.word }
                        .forEach { word ->
                            if (!candidates.contains(word)) {
                                candidates.add(word)
                            }
                        }
                }
        }
        
        // 3. 拼音分词（尝试将拼音分成多个词）
        if (lowerPinyin.length >= 4) {
            val segmentCandidates = segmentPinyin(lowerPinyin)
            segmentCandidates.forEach { candidate ->
                if (!candidates.contains(candidate)) {
                    candidates.add(candidate)
                }
            }
        }
        
        return candidates.take(20) // 最多返回20个候选词
    }
    
    /**
     * 拼音分词
     * 将连续的拼音尝试分割成多个词
     */
    private fun segmentPinyin(pinyin: String): List<String> {
        val results = mutableListOf<String>()
        
        // 简单的分词策略：尝试2-4个字符的分割
        for (firstLen in 2..minOf(4, pinyin.length - 2)) {
            val first = pinyin.substring(0, firstLen)
            val rest = pinyin.substring(firstLen)
            
            val firstCandidates = pinyinDict[first]?.map { it.word } ?: continue
            
            if (rest.length >= 2) {
                val restCandidates = getCandidates(rest)
                
                // 组合结果
                for (firstWord in firstCandidates.take(3)) {
                    for (restWord in restCandidates.take(3)) {
                        results.add(firstWord + restWord)
                        if (results.size >= 10) return results
                    }
                }
            }
        }
        
        return results
    }
    
    /**
     * 更新词频
     * 当用户选择某个候选词时调用，提高该词的使用频率
     */
    open fun updateFrequency(pinyin: String, word: String) {
        pinyinDict[pinyin]?.find { it.word == word }?.let { candidate ->
            val index = pinyinDict[pinyin]!!.indexOf(candidate)
            pinyinDict[pinyin]!![index] = candidate.copy(frequency = candidate.frequency + 1)
        }
    }
    
    /**
     * 获取联想词
     * 根据已输入的词获取可能的后续词
     */
    fun getAssociations(previousWord: String): List<String> {
        // 简单的联想词规则
        return when (previousWord) {
            "你" -> listOf("好", "是", "在", "有", "的", "们")
            "我" -> listOf("是", "的", "们", "要", "在", "有", "想", "爱")
            "这" -> listOf("是", "个", "里", "样", "些", "么")
            "那" -> listOf("是", "个", "里", "样", "些", "么")
            "什么" -> listOf("时候", "地方", "东西", "人")
            else -> emptyList()
        }
    }

    open fun initialize() {
        PinyinUtils.init(context)
    }

    open fun destroy() {
        // Base implementation can be empty
    }
} 