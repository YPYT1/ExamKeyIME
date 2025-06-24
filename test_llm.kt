import com.example.examkeyime.llm.*
import kotlinx.coroutines.runBlocking

fun main() {
    println("开始测试百度千帆大模型连接...")
    
    runBlocking {
        try {
            // 1. 首先测试获取访问令牌
            println("正在获取访问令牌...")
            val accessToken = BaiduQianfanClient.getAccessToken()
            println("✓ 成功获取访问令牌: ${accessToken.take(20)}...")
            
            // 2. 测试发送聊天请求
            println("\n正在发送测试消息...")
            val testRequest = ChatRequest(
                messages = listOf(
                    Message(
                        role = "user",
                        content = "你好，请简单介绍一下你自己。"
                    )
                )
            )
            
            val response = BaiduQianfanClient.qianfanApi.getChatCompletions(
                request = testRequest,
                accessToken = accessToken
            )
            
            println("✓ 成功收到响应!")
            println("模型: ${response.model}")
            println("回复内容: ${response.choices.firstOrNull()?.message?.content}")
            println("使用的token数: ${response.usage.totalTokens}")
            
        } catch (e: Exception) {
            println("✗ 测试失败: ${e.message}")
            e.printStackTrace()
        }
    }
}
