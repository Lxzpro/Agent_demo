package org.example.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class StudyPlanGenerationTools {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据用户的能力评估结果生成个性化、分阶段的学习计划
     *
     * @param levelName        用户水平名称：新手、入门、提高、进阶、大师
     * @param weakPoints       薄弱知识点，多个用英文逗号分隔
     * @param codingSpeed      编码速度等级：0=慢,1=一般,2=快,3=极快
     * @param thinkingAbility  思考能力等级：0=弱,1=一般,2=强,3=极强
     * @param dailyHours       每日可学习小时数
     * @param studyTarget      用户学习目标
     * @param totalDays        总学习天数
     * @return 包含完整学习计划的 JSON 格式字符串
     */
    @Tool(description = "Generate a personalized, structured study plan based on the user's skill evaluation result. "
            + "This tool only generates plans and does NOT evaluate abilities.")
    public String generateStudyPlan(
            @ToolParam(description = "User level name") String levelName,
            @ToolParam(description = "Weak knowledge points") String weakPoints,
            @ToolParam(description = "Coding speed level") int codingSpeed,
            @ToolParam(description = "Thinking ability level") int thinkingAbility,
            @ToolParam(description = "Daily learning hours available") double dailyHours,
            @ToolParam(description = "User learning target") String studyTarget,
            @ToolParam(description = "Total study days") int totalDays
    ) {
        try {
            StudyPlan plan = new StudyPlan();
            plan.setLevelName(levelName);
            plan.setWeakPoints(weakPoints);
            plan.setCodingSpeed(codingSpeed);
            plan.setThinkingAbility(thinkingAbility);
            plan.setDailyHours(dailyHours);
            plan.setStudyTarget(studyTarget);
            plan.setTotalDays(totalDays);
            plan.setPhaseList(generatePhases(plan));

            PlanOutput output = new PlanOutput();
            output.setSuccess(true);
            output.setPlan(plan);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"Plan generation failed\"}";
        }
    }

    /**
     * 根据用户水平、薄弱点和总天数，自动生成分阶段的学习计划
     * @param plan 包含用户完整信息的学习计划对象
     * @return 分阶段的学习阶段列表
     */
    private List<Phase> generatePhases(StudyPlan plan) {
        List<Phase> phases = new ArrayList<>();
        String level = plan.getLevelName();
        String weak = plan.getWeakPoints();
        int days = plan.getTotalDays();

        if (level.contains("Beginner") || level.contains("Entry")) {
            phases.add(new Phase("Fundamental Learning", days / 2,
                    List.of("Array","Simulation","String","Recursion"), "Master basic logic"));
            phases.add(new Phase("Weak Point Breakthrough", days - days / 2,
                    List.of(weak), "Targeted improvement"));
        } else if (level.contains("Intermediate")) {
            phases.add(new Phase("Core Algorithm Training", days / 2,
                    List.of("DFS","BFS","DP Basic","Greedy"), "Build algorithm thinking"));
            phases.add(new Phase("Weak Point Specialization", days - days / 2,
                    List.of(weak), "Reinforce weaknesses"));
        } else {
            phases.add(new Phase("Advanced Algorithm Sprint", days * 6 / 10,
                    List.of("DP Optimization","Graph Theory","Segment Tree","Contest Questions"), "High difficulty improvement"));
            phases.add(new Phase("Weakness Review & Speed Up", days - phases.get(0).getDays(),
                    List.of(weak, "Coding Speed"), "Final enhancement"));
        }
        return phases;
    }

    @Data
    public static class Phase {
        @JsonProperty("phaseName") private String phaseName;
        @JsonProperty("days") private int days;
        @JsonProperty("focusPoints") private List<String> focusPoints;
        @JsonProperty("objective") private String objective;

        public Phase(String phaseName, int days, List<String> focusPoints, String objective) {
            this.phaseName = phaseName;
            this.days = days;
            this.focusPoints = focusPoints;
            this.objective = objective;
        }
    }

    @Data
    public static class StudyPlan {
        @JsonProperty("levelName") private String levelName;
        @JsonProperty("weakPoints") private String weakPoints;
        @JsonProperty("codingSpeed") private int codingSpeed;
        @JsonProperty("thinkingAbility") private int thinkingAbility;
        @JsonProperty("dailyHours") private double dailyHours;
        @JsonProperty("studyTarget") private String studyTarget;
        @JsonProperty("totalDays") private int totalDays;
        @JsonProperty("phaseList") private List<Phase> phaseList;
    }

    @Data
    public static class PlanOutput {
        @JsonProperty("success") private boolean success;
        @JsonProperty("plan") private StudyPlan plan;
    }
}