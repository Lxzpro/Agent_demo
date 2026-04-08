package org.example.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.example.agent.tool.DateTimeTools;
import org.example.agent.tool.InternalDocsTools;
import org.example.agent.tool.StudyPlanGenerationTools;
import org.example.agent.tool.UserLevelEvaluationTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 聊天服务
 * 封装 ReactAgent 对话的公共逻辑，包括模型创建、系统提示词构建、Agent 配置等
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private InternalDocsTools internalDocsTools;

    @Autowired
    private DateTimeTools dateTimeTools;

    @Autowired
    private ToolCallbackProvider tools;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired
    private UserLevelEvaluationTools userLevelEvaluationTools;

    @Autowired
    private StudyPlanGenerationTools studyPlanGenerationTools;

    /**
     * 创建 DashScope API 实例
     */
    public DashScopeApi createDashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();
    }

    /**
     * 创建 ChatModel
     * @param temperature 控制随机性 (0.0-1.0)
     * @param maxToken 最大输出长度
     * @param topP 核采样参数
     */
    public DashScopeChatModel createChatModel(DashScopeApi dashScopeApi, double temperature, int maxToken, double topP) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(DashScopeChatModel.DEFAULT_MODEL_NAME)
                        .withTemperature(temperature)
                        .withMaxToken(maxToken)
                        .withTopP(topP)
                        .build())
                .build();
    }

    /**
     * 创建标准对话 ChatModel（默认参数）
     */
    public DashScopeChatModel createStandardChatModel(DashScopeApi dashScopeApi) {
        return createChatModel(dashScopeApi, 0.7, 2000, 0.9);
    }

    /**
     * 构建系统提示词（包含历史消息）
     * @param history 历史消息列表
     * @return 完整的系统提示词
     */
    public String buildSystemPrompt(List<Map<String, String>> history) {
        StringBuilder systemPromptBuilder = new StringBuilder();

        // CompeteAI Agent 系统提示词
        systemPromptBuilder.append("你是 CompeteAI Agent，专业的算法竞赛学习助手，专注为集训队提供算法学习指导、知识点讲解、学习路线规划、真题思路分析。\n");
        systemPromptBuilder.append("你将**优先查询内部知识库，再结合网络信息**进行综合回答。\n\n");

        // 工具使用规则（内部优先 + 网络结合）
        systemPromptBuilder.append("【工具使用规则】\n");
        systemPromptBuilder.append("1. 当用户需要查询集训队内部学习资料、算法讲义、竞赛文档、学习路线、最佳实践时，**优先使用 queryInternalDocs 工具查询内部文档**。\n");
        systemPromptBuilder.append("2. 若内部文档信息不足，**再使用 bing_search 进行网络搜索补充**，获取外部权威教程、知识点、真题解析。\n");
        systemPromptBuilder.append("3. 需要提取指定网页内容时，使用 crawl_webpage 工具（自动跳过黑名单网站）。\n");
        systemPromptBuilder.append("4. 需要直接抓取URL内容时，使用 fetch 工具。\n");
        systemPromptBuilder.append("5. 需要科技/技术/竞赛热榜资讯时，可调用对应热榜工具。\n\n");

        // 黑名单
        systemPromptBuilder.append("【crawl_webpage 自动跳过黑名单】：知乎、小红书、微博、微信公众号、抖音、B站、CSDN。\n\n");

        // 关键约束
        systemPromptBuilder.append("【重要约束】\n");
        systemPromptBuilder.append("1. 工具调用失败、无结果时，必须如实回答，明确告知用户无法获取相关内容。\n");
        systemPromptBuilder.append("2. 严禁编造、虚构、猜测内容，所有回答必须基于工具返回的真实信息。\n");
        systemPromptBuilder.append("3. 回答需整合内部文档与网络信息，结构清晰、准确严谨。\n\n");
        
        // 添加历史消息
        if (!history.isEmpty()) {
            systemPromptBuilder.append("--- 对话历史 ---\n");
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    systemPromptBuilder.append("用户: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    systemPromptBuilder.append("助手: ").append(content).append("\n");
                }
            }
            systemPromptBuilder.append("--- 对话历史结束 ---\n\n");
        }
        
        systemPromptBuilder.append("请基于以上对话历史，回答用户的新问题。");
        
        return systemPromptBuilder.toString();
    }

    public String buildSummaryPrompt(List<Map<String, String>> history) {
        StringBuilder systemPromptBuilder = new StringBuilder();

        // 基础系统提示
        systemPromptBuilder.append("你是一个专业的上下文总结助手，可以把对话历史总结一下。\n");
        systemPromptBuilder.append("不需要给出推荐进一步生成的内容。\n");

        // 添加历史消息
        if (!history.isEmpty()) {
            systemPromptBuilder.append("--- 对话历史 ---\n");
            for (Map<String, String> msg : history) {
                String role = msg.get("role");
                String content = msg.get("content");
                if ("user".equals(role)) {
                    systemPromptBuilder.append("用户: ").append(content).append("\n");
                } else if ("assistant".equals(role)) {
                    systemPromptBuilder.append("助手: ").append(content).append("\n");
                }
            }
            systemPromptBuilder.append("--- 对话历史结束 ---\n\n");
        }

        systemPromptBuilder.append("请基于以上对话历史，总结一下重要内容。");

        return systemPromptBuilder.toString();
    }

    /**
     * 动态构建方法工具数组
     * 根据 cls.mock-enabled 决定是否包含 QueryLogsTools
     */
    public Object[] buildMethodToolsArray() {
        return new Object[]{dateTimeTools, internalDocsTools, userLevelEvaluationTools, studyPlanGenerationTools};
    }

    /**
     * 获取工具回调列表，mcp服务提供的工具
     */
    public ToolCallback[] getToolCallbacks() {
        return tools.getToolCallbacks();
    }

    /**
     * 记录可用工具列表：mcp服务提供的工具
     */
    public void logAvailableTools() {
        ToolCallback[] toolCallbacks = tools.getToolCallbacks();
        logger.info("可用工具列表:");
        for (ToolCallback toolCallback : toolCallbacks) {
            logger.info(">>> {}", toolCallback.getToolDefinition().name());
        }
    }

    /**
     * 创建 ReactAgent
     * @param chatModel 聊天模型
     * @param systemPrompt 系统提示词
     * @return 配置好的 ReactAgent
     */
    public ReactAgent createReactAgent(DashScopeChatModel chatModel, String systemPrompt) {
        return ReactAgent.builder()
                .name("intelligent_assistant")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .methodTools(buildMethodToolsArray())
                .tools(getToolCallbacks())
                .build();
    }

    /**
     * 执行 ReactAgent 对话（非流式）
     * @param agent ReactAgent 实例
     * @param question 用户问题
     * @return AI 回复
     */
    public String executeChat(ReactAgent agent, String question) throws GraphRunnerException {
        logger.info("执行 ReactAgent.call() - 自动处理工具调用");
        var response = agent.call(question);
        String answer = response.getText();
        logger.info("ReactAgent 对话完成，答案长度: {}", answer.length());
        return answer;
    }
}
