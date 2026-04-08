package org.example.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class UserLevelEvaluationTools {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户问卷信息生成专业的算法能力水平评估报告
     *
     * @param levelCode        用户水平编码：0=新手,1=入门,2=提高,3=进阶,4=大师
     * @param codingSpeed       编码速度等级：0=慢,1=一般,2=快,3=极快
     * @param thinkingAbility   思考能力等级：0=弱,1=一般,2=强,3=极强
     * @param weakPoints        薄弱知识点，多个用英文逗号分隔
     * @param dailyHours        每日可学习小时数
     * @param studyTarget       用户学习目标
     * @param description       用户补充描述
     * @return 包含完整评估结果的 JSON 格式字符串
     */
    @Tool(description = "Generate a professional algorithm skill evaluation report based on the user's questionnaire. "
            + "This tool only evaluates abilities and does NOT generate study plans.")
    public String evaluateByQuestionnaire(
            @ToolParam(description = "User level code: 0=beginner,1=entry,2=intermediate,3=advanced,4=expert") int levelCode,
            @ToolParam(description = "Coding speed: 0=slow,1=normal,2=fast,3=very fast") int codingSpeed,
            @ToolParam(description = "Thinking ability: 0=weak,1=normal,2=strong,3=very strong") int thinkingAbility,
            @ToolParam(description = "Weak knowledge points, separated by commas") String weakPoints,
            @ToolParam(description = "Daily learning hours") double dailyHours,
            @ToolParam(description = "User learning target or goal") String studyTarget,
            @ToolParam(description = "User's self-description") String description
    ) {
        try {
            LevelEvaluationResult result = new LevelEvaluationResult();
            result.setLevelCode(levelCode);
            result.setLevelName(getLevelName(levelCode));
            result.setCodingSpeed(codingSpeed);
            result.setThinkingAbility(thinkingAbility);
            result.setWeakPoints(weakPoints);
            result.setDailyHours(dailyHours);
            result.setStudyTarget(studyTarget);
            result.setDescription(description);
            result.setSummary(generateSummary(result));

            EvaluationOutput output = new EvaluationOutput();
            output.setSuccess(true);
            output.setResult(result);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Evaluation failed\"}";
        }
    }

    /**
     * 根据水平编码获取对应的英文水平名称
     *
     * @param code 水平编码：0=新手,1=入门,2=提高,3=进阶,4=大师
     * @return 标准英文水平名称
     */
    private String getLevelName(int code) {
        return switch (code) {
            case 0 -> "Beginner";
            case 1 -> "Entry Level";
            case 2 -> "Intermediate Level";
            case 3 -> "Advanced Level";
            case 4 -> "Expert Level";
            default -> "Unrated";
        };
    }

    /**
     * 生成用户算法能力的综合评估摘要
     * @param r 完整的能力评估结果对象
     * @return 包含水平、薄弱点、编码速度、思考能力的结构化摘要字符串
     */
    private String generateSummary(LevelEvaluationResult r) {
        return "Level: " + r.getLevelName()
                + " | Weak points: " + r.getWeakPoints()
                + " | Coding speed: " + r.getCodingSpeed()
                + " | Thinking ability: " + r.getThinkingAbility();
    }

    @Data
    public static class LevelEvaluationResult {
        @JsonProperty("levelCode") private int levelCode;
        @JsonProperty("levelName") private String levelName;
        @JsonProperty("codingSpeed") private int codingSpeed;
        @JsonProperty("thinkingAbility") private int thinkingAbility;
        @JsonProperty("weakPoints") private String weakPoints;
        @JsonProperty("dailyHours") private double dailyHours;
        @JsonProperty("studyTarget") private String studyTarget;
        @JsonProperty("description") private String description;
        @JsonProperty("summary") private String summary;
    }

    @Data
    public static class EvaluationOutput {
        @JsonProperty("success") private boolean success;
        @JsonProperty("result") private LevelEvaluationResult result;
    }
}