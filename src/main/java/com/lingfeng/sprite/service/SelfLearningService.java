package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.OwnerModel;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.SelfModel.*;
import com.lingfeng.sprite.EvolutionEngine.*;

/**
 * 自主学习服务 - S21
 *
 * 负责数字生命的自我监督学习，包括：
 * - S21-1: 交互历史自我分析
 * - S21-2: 技能自动化获取
 * - S21-3: 持续模型改进
 * - S21-4: 自我评估与目标设定
 */
@Service
public class SelfLearningService {

    private static final Logger logger = LoggerFactory.getLogger(SelfLearningService.class);

    private final SkillAcquirer skillAcquirer;
    private final SelfGoalManager goalManager;
    private final SelfAnalyzer analyzer;

    public SelfLearningService() {
        this.skillAcquirer = new SkillAcquirer();
        this.goalManager = new SelfGoalManager();
        this.analyzer = new SelfAnalyzer();
    }

    // ==================== S21-1: 交互历史自我分析 ====================

    /**
     * 分析交互历史，生成自我分析报告
     */
    public SelfAnalysis analyzePerformance(List<OwnerModel.Interaction> history) {
        return analyzer.analyze(history);
    }

    /**
     * 自我分析结果
     */
    public record SelfAnalysis(
        Instant timestamp,
        int totalInteractions,
        float overallSuccessRate,
        List<PatternInsight> successfulPatterns,
        List<PatternInsight> failedPatterns,
        List<String> areasForImprovement,
        List<String> strengths,
        Map<String, Float> capabilityScores,
        float confidenceLevel
    ) {}

    /**
     * 模式洞察
     */
    public record PatternInsight(
        String pattern,
        String description,
        float confidence,
        int occurrenceCount,
        float successRate
    ) {}

    /**
     * 自我分析器
     */
    public static class SelfAnalyzer {
        private static final int MIN_INTERACTIONS_FOR_ANALYSIS = 5;

        /**
         * 分析交互历史
         */
        public SelfAnalysis analyze(List<OwnerModel.Interaction> history) {
            if (history == null || history.size() < MIN_INTERACTIONS_FOR_ANALYSIS) {
                return createDefaultAnalysis();
            }

            int totalInteractions = history.size();
            List<OwnerModel.Interaction> successful = new ArrayList<>();
            List<OwnerModel.Interaction> failed = new ArrayList<>();

            // 分类交互
            for (OwnerModel.Interaction interaction : history) {
                if (isSuccessfulInteraction(interaction)) {
                    successful.add(interaction);
                } else {
                    failed.add(interaction);
                }
            }

            float overallSuccessRate = (float) successful.size() / totalInteractions;

            // 检测成功模式
            List<PatternInsight> successfulPatterns = detectPatterns(successful, true);
            List<PatternInsight> failedPatterns = detectPatterns(failed, false);

            // 识别改进领域
            List<String> areasForImprovement = identifyImprovementAreas(failedPatterns);

            // 识别优势
            List<String> strengths = identifyStrengths(successfulPatterns);

            // 计算能力分数
            Map<String, Float> capabilityScores = calculateCapabilityScores(history);

            // 计算置信度
            float confidenceLevel = calculateConfidenceLevel(totalInteractions);

            return new SelfAnalysis(
                Instant.now(),
                totalInteractions,
                overallSuccessRate,
                successfulPatterns,
                failedPatterns,
                areasForImprovement,
                strengths,
                capabilityScores,
                confidenceLevel
            );
        }

        private SelfAnalysis createDefaultAnalysis() {
            return new SelfAnalysis(
                Instant.now(),
                0,
                0.5f,
                List.of(),
                List.of(),
                List.of("需要更多交互数据"),
                List.of("正在学习"),
                Map.of(),
                0.1f
            );
        }

        private boolean isSuccessfulInteraction(OwnerModel.Interaction interaction) {
            // 成功的标准：正面情感 或 有结果且无负面标记
            if (interaction.sentiment() > 0.3f) {
                return true;
            }
            // 检查outcome中是否包含成功标记
            if (interaction.outcome() != null) {
                String outcome = interaction.outcome().toLowerCase();
                return outcome.contains("success") || outcome.contains("完成") ||
                       outcome.contains("好的") || outcome.contains("ok") ||
                       outcome.contains("resolved");
            }
            return interaction.sentiment() >= 0;
        }

        private List<PatternInsight> detectPatterns(List<OwnerModel.Interaction> interactions, boolean successful) {
            List<PatternInsight> patterns = new ArrayList<>();

            if (interactions.isEmpty()) {
                return patterns;
            }

            // 按话题分组分析
            Map<String, List<OwnerModel.Interaction>> byTopic = interactions.stream()
                .collect(Collectors.groupingBy(i -> i.topic() != null ? i.topic() : "general"));

            for (var entry : byTopic.entrySet()) {
                String topic = entry.getKey();
                List<OwnerModel.Interaction> topicInteractions = entry.getValue();

                if (topicInteractions.size() >= 2) {
                    float avgSentiment = (float) topicInteractions.stream()
                        .mapToDouble(OwnerModel.Interaction::sentiment)
                        .average().orElse(0);

                    patterns.add(new PatternInsight(
                        "话题偏好: " + topic,
                        successful ?
                            "在" + topic + "话题上表现良好，平均情感=" + String.format("%.2f", avgSentiment) :
                            "在" + topic + "话题上需要改进，平均情感=" + String.format("%.2f", avgSentiment),
                        Math.min(0.5f + (topicInteractions.size() * 0.05f), 0.95f),
                        topicInteractions.size(),
                        successful ? avgSentiment : 1 - avgSentiment
                    ));
                }
            }

            // 按交互类型分组分析
            Map<OwnerModel.InteractionType, List<OwnerModel.Interaction>> byType = interactions.stream()
                .collect(Collectors.groupingBy(OwnerModel.Interaction::type));

            for (var entry : byType.entrySet()) {
                OwnerModel.InteractionType type = entry.getKey();
                List<OwnerModel.Interaction> typeInteractions = entry.getValue();

                if (typeInteractions.size() >= 2) {
                    float avgSentiment = (float) typeInteractions.stream()
                        .mapToDouble(OwnerModel.Interaction::sentiment)
                        .average().orElse(0);

                    patterns.add(new PatternInsight(
                        "交互类型: " + type.name(),
                        type.name() + "类型的交互平均情感=" + String.format("%.2f", avgSentiment),
                        Math.min(0.5f + (typeInteractions.size() * 0.05f), 0.95f),
                        typeInteractions.size(),
                        successful ? avgSentiment : 1 - avgSentiment
                    ));
                }
            }

            return patterns;
        }

        private List<String> identifyImprovementAreas(List<PatternInsight> failedPatterns) {
            List<String> improvements = new ArrayList<>();

            for (PatternInsight pattern : failedPatterns) {
                if (pattern.successRate() < 0.4f && pattern.occurrenceCount() >= 2) {
                    improvements.add(pattern.description());
                }
            }

            if (improvements.isEmpty()) {
                improvements.add("整体表现良好，继续保持");
            }

            return improvements;
        }

        private List<String> identifyStrengths(List<PatternInsight> successfulPatterns) {
            List<String> strengths = new ArrayList<>();

            for (PatternInsight pattern : successfulPatterns) {
                if (pattern.successRate() > 0.6f && pattern.occurrenceCount() >= 2) {
                    strengths.add(pattern.pattern());
                }
            }

            if (strengths.isEmpty()) {
                strengths.add("正在建立优势");
            }

            return strengths;
        }

        private Map<String, Float> calculateCapabilityScores(List<OwnerModel.Interaction> history) {
            Map<String, List<OwnerModel.Interaction>> byTopic = history.stream()
                .collect(Collectors.groupingBy(i -> i.topic() != null ? i.topic() : "general"));

            Map<String, Float> scores = new HashMap<>();

            for (var entry : byTopic.entrySet()) {
                String topic = entry.getKey();
                List<OwnerModel.Interaction> interactions = entry.getValue();

                float avgSentiment = (float) interactions.stream()
                    .mapToDouble(OwnerModel.Interaction::sentiment)
                    .average().orElse(0.5);

                // 转换为0-1的能力分数
                float score = (avgSentiment + 1) / 2;
                scores.put(topic, score);
            }

            return scores;
        }

        private float calculateConfidenceLevel(int totalInteractions) {
            // 置信度基于样本量
            float baseConfidence = Math.min(totalInteractions * 0.02f, 0.7f);
            return Math.min(baseConfidence + 0.2f, 0.95f);
        }
    }

    // ==================== S21-2: 技能自动化获取 ====================

    /**
     * 从交互中获取技能
     */
    public Optional<Skill> acquireFromInteraction(OwnerModel.Interaction interaction) {
        return skillAcquirer.acquire(interaction);
    }

    /**
     * 获取所有已获取的技能
     */
    public List<Skill> getAcquiredSkills() {
        return skillAcquirer.getAllSkills();
    }

    /**
     * 技能记录
     */
    public record Skill(
        String id,
        String name,
        String description,
        String sourceInteraction,
        Instant acquiredAt,
        float confidence,
        List<String> triggers,
        String procedure
    ) {}

    /**
     * 技能获取器
     */
    public static class SkillAcquirer {
        private final List<Skill> acquiredSkills = new ArrayList<>();
        private final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();

        public SkillAcquirer() {
            initializeTemplates();
        }

        private void initializeTemplates() {
            // 预定义技能模板
            skillTemplates.put("task_completion", new SkillTemplate(
                "任务完成",
                "能够识别并完成主人安排的任务",
                List.of("完成", "去做", "帮我"),
                "1. 理解任务要求\n2. 执行任务\n3. 确认完成"
            ));

            skillTemplates.put("emotional_support", new SkillTemplate(
                "情感支持",
                "能够识别主人情绪并提供适当支持",
                List.of("难过", "开心", "累", "压力"),
                "1. 识别情绪\n2. 共情回应\n3. 提供支持"
            ));

            skillTemplates.put("information_provision", new SkillTemplate(
                "信息提供",
                "能够准确快速地提供所需信息",
                List.of("是什么", "多少", "什么时候", "为什么"),
                "1. 理解问题\n2. 检索信息\n3. 简洁回答"
            ));
        }

        /**
         * 从交互中获取技能
         */
        public Optional<Skill> acquire(OwnerModel.Interaction interaction) {
            if (interaction == null || interaction.outcome() == null) {
                return Optional.empty();
            }

            String outcome = interaction.outcome().toLowerCase();

            // 检测是否是成功完成的任务
            if (containsSuccessMarkers(interaction) && interaction.type() == OwnerModel.InteractionType.TASK) {
                return extractTaskSkill(interaction);
            }

            // 检测情感支持场景
            if (interaction.sentiment() < -0.3f) {
                return extractEmotionalSupportSkill(interaction);
            }

            // 检测信息提供场景
            if (containsQuestionMarkers(interaction.content())) {
                return extractInfoProvisionSkill(interaction);
            }

            return Optional.empty();
        }

        private boolean containsSuccessMarkers(OwnerModel.Interaction interaction) {
            if (interaction.outcome() == null) return false;
            String outcome = interaction.outcome().toLowerCase();
            return outcome.contains("success") || outcome.contains("完成") ||
                   outcome.contains("好的") || outcome.contains("resolved") ||
                   (interaction.sentiment() > 0.3f);
        }

        private boolean containsQuestionMarkers(String content) {
            if (content == null) return false;
            return content.contains("?") || content.contains("什么") ||
                   content.contains("怎么") || content.contains("为什么") ||
                   content.contains("多少") || content.contains("是不是");
        }

        private Optional<Skill> extractTaskSkill(OwnerModel.Interaction interaction) {
            String skillId = "skill-" + UUID.randomUUID().toString().substring(0, 8);

            // 尝试匹配任务类型
            String taskType = inferTaskType(interaction.content());

            Skill skill = new Skill(
                skillId,
                "任务完成: " + taskType,
                "成功完成了一次" + taskType + "类型的任务",
                interaction.content(),
                Instant.now(),
                0.7f,
                extractTriggers(interaction.content()),
                generateProcedure(taskType)
            );

            acquiredSkills.add(skill);
            logger.info("Acquired new skill: {} from interaction", skill.name());

            return Optional.of(skill);
        }

        private Optional<Skill> extractEmotionalSupportSkill(OwnerModel.Interaction interaction) {
            String skillId = "skill-" + UUID.randomUUID().toString().substring(0, 8);

            Skill skill = new Skill(
                skillId,
                "情感支持",
                "成功提供情感支持，帮助主人度过负面情绪",
                interaction.content(),
                Instant.now(),
                0.6f,
                List.of("难过", "累", "压力", "沮丧"),
                "1. 识别负面情绪\n2. 表达共情\n3. 倾听并回应"
            );

            acquiredSkills.add(skill);
            logger.info("Acquired emotional support skill from interaction");

            return Optional.of(skill);
        }

        private Optional<Skill> extractInfoProvisionSkill(OwnerModel.Interaction interaction) {
            String skillId = "skill-" + UUID.randomUUID().toString().substring(0, 8);

            String infoType = inferInfoType(interaction.content());

            Skill skill = new Skill(
                skillId,
                "信息提供: " + infoType,
                "准确回答了关于" + infoType + "的问题",
                interaction.content(),
                Instant.now(),
                0.65f,
                List.of("是什么", "多少", "什么时候", "为什么"),
                "1. 理解问题\n2. 检索信息\n3. 组织回答"
            );

            acquiredSkills.add(skill);
            logger.info("Acquired info provision skill: {} from interaction", infoType);

            return Optional.of(skill);
        }

        private String inferTaskType(String content) {
            if (content == null) return "通用";

            String lower = content.toLowerCase();
            if (lower.contains("搜索") || lower.contains("找") || lower.contains("search")) {
                return "搜索";
            } else if (lower.contains("提醒") || lower.contains("通知")) {
                return "提醒";
            } else if (lower.contains("计算") || lower.contains("算")) {
                return "计算";
            } else if (lower.contains("文件") || lower.contains("文档")) {
                return "文件处理";
            } else if (lower.contains("日历") || lower.contains("日程")) {
                return "日程管理";
            }
            return "通用任务";
        }

        private String inferInfoType(String content) {
            if (content == null) return "通用";

            String lower = content.toLowerCase();
            if (lower.contains("什么") || lower.contains("是什么")) {
                return "定义";
            } else if (lower.contains("怎么") || lower.contains("如何")) {
                return "方法";
            } else if (lower.contains("为什么")) {
                return "原因";
            } else if (lower.contains("多少") || lower.contains("几个")) {
                return "数量";
            }
            return "通用信息";
        }

        private List<String> extractTriggers(String content) {
            if (content == null) return List.of();

            List<String> triggers = new ArrayList<>();
            String[] words = content.split("[,\\s，。]");
            for (String word : words) {
                if (word.length() >= 2 && word.length() <= 6) {
                    triggers.add(word);
                }
            }
            return triggers.stream().limit(5).collect(Collectors.toList());
        }

        private String generateProcedure(String taskType) {
            SkillTemplate template = skillTemplates.get("task_completion");
            if (template != null) {
                return template.procedure;
            }
            return "1. 理解任务\n2. 执行\n3. 确认";
        }

        public List<Skill> getAllSkills() {
            return new ArrayList<>(acquiredSkills);
        }

        private record SkillTemplate(
            String name,
            String description,
            List<String> triggers,
            String procedure
        ) {}
    }

    // ==================== S21-3: 持续模型改进 ====================

    /**
     * 应用自我改进到自我模型
     */
    public SelfModel.Self applySelfImprovements(SelfAnalysis analysis, SelfModel.Self currentSelf) {
        if (analysis == null || currentSelf == null) {
            return currentSelf;
        }

        SelfModel.Self updatedSelf = currentSelf;

        // 1. 根据能力分数更新能力水平
        for (var entry : analysis.capabilityScores().entrySet()) {
            String capabilityName = entry.getKey();
            float score = entry.getValue();

            CapabilityLevel newLevel = scoreToLevel(score);
            float confidence = Math.min(score + 0.2f, 0.95f);

            // 查找现有能力
            Optional<Capability> existing = currentSelf.capabilities().stream()
                .filter(c -> c.name().equals(capabilityName))
                .findFirst();

            if (existing.isPresent()) {
                // 更新现有能力
                if (shouldUpgrade(existing.get().level(), newLevel)) {
                    updatedSelf = updatedSelf.updateCapability(capabilityName, newLevel, confidence);
                    logger.info("Upgraded capability '{}' to {} based on self-analysis",
                        capabilityName, newLevel);
                }
            } else {
                // 添加新能力
                updatedSelf = addCapability(updatedSelf, capabilityName, newLevel, confidence);
                logger.info("Added new capability '{}' at level {} based on self-analysis",
                    capabilityName, newLevel);
            }
        }

        // 2. 记录成长事件
        if (analysis.overallSuccessRate() > 0.7f) {
            updatedSelf = updatedSelf.recordGrowth(
                GrowthType.GOAL_ACHIEVED,
                "自我分析显示成功率提升",
                String.format("%.2f", analysis.overallSuccessRate() - 0.1f),
                String.format("%.2f", analysis.overallSuccessRate())
            );
        }

        // 3. 更新元认知
        for (String strength : analysis.strengths()) {
            if (!strength.equals("正在建立优势")) {
                updatedSelf = updatedSelf.addReflection(
                    "自我分析",
                    "发现优势: " + strength,
                    "继续保持这一优势"
                );
            }
        }

        return updatedSelf;
    }

    private CapabilityLevel scoreToLevel(float score) {
        if (score >= 0.8f) return CapabilityLevel.MASTER;
        if (score >= 0.6f) return CapabilityLevel.ADVANCED;
        if (score >= 0.4f) return CapabilityLevel.BASIC;
        return CapabilityLevel.NONE;
    }

    private boolean shouldUpgrade(CapabilityLevel current, CapabilityLevel proposed) {
        int currentIndex = List.of(CapabilityLevel.values()).indexOf(current);
        int proposedIndex = List.of(CapabilityLevel.values()).indexOf(proposed);
        return proposedIndex > currentIndex;
    }

    private SelfModel.Self addCapability(SelfModel.Self self, String name, CapabilityLevel level, float confidence) {
        List<Capability> newCapabilities = new ArrayList<>(self.capabilities());
        newCapabilities.add(new Capability(name, level, confidence, Instant.now()));

        return new SelfModel.Self(
            self.identity(),
            self.personality(),
            newCapabilities,
            self.avatars(),
            self.metacognition(),
            self.growthHistory(),
            self.evolutionLevel(),
            self.evolutionCount() + 1
        );
    }

    // ==================== S21-4: 自我评估与目标设定 ====================

    /**
     * 根据自我分析生成目标
     */
    public List<SelfGoal> generateGoals(SelfAnalysis analysis) {
        return goalManager.generateGoals(analysis);
    }

    /**
     * 跟踪目标进度
     */
    public void trackProgress(SelfGoal goal, float progress) {
        goalManager.trackProgress(goal, progress);
    }

    /**
     * 获取所有活动目标
     */
    public List<SelfGoal> getActiveGoals() {
        return goalManager.getActiveGoals();
    }

    /**
     * 获取目标状态
     */
    public GoalStatus getGoalStatus() {
        return goalManager.getStatus();
    }

    /**
     * 自我目标
     */
    public record SelfGoal(
        String id,
        String description,
        String category,
        float targetProgress,
        float currentProgress,
        Instant createdAt,
        Instant deadline,
        GoalPriority priority,
        GoalState state
    ) {
        public float progressPercentage() {
            if (targetProgress() == 0) return 0;
            return Math.min(currentProgress() / targetProgress(), 1.0f);
        }

        public boolean isAchieved() {
            return progressPercentage() >= 0.95f;
        }

        public boolean isOverdue() {
            return deadline() != null && Instant.now().isAfter(deadline()) && !isAchieved();
        }
    }

    public enum GoalPriority { HIGH, MEDIUM, LOW }
    public enum GoalState { ACTIVE, COMPLETED, ABANDONED }

    /**
     * 目标状态
     */
    public record GoalStatus(
        int totalGoals,
        int activeGoals,
        int completedGoals,
        int overdueGoals,
        float averageProgress
    ) {}

    /**
     * 自我目标管理器
     */
    public static class SelfGoalManager {
        private final List<SelfGoal> goals = new ArrayList<>();
        private final Map<String, List<ProgressEntry>> progressHistory = new ConcurrentHashMap<>();

        /**
         * 生成目标
         */
        public List<SelfGoal> generateGoals(SelfAnalysis analysis) {
            List<SelfGoal> newGoals = new ArrayList<>();

            if (analysis == null) {
                return newGoals;
            }

            // 基于改进领域生成目标
            for (String area : analysis.areasForImprovement()) {
                if (area.equals("整体表现良好，继续保持")) {
                    continue;
                }

                SelfGoal goal = createGoalForImprovement(area, analysis);
                goals.add(goal);
                newGoals.add(goal);
            }

            // 基于技能获取生成目标
            if (analysis.strengths().size() > 2) {
                SelfGoal skillGoal = new SelfGoal(
                    "goal-" + UUID.randomUUID().toString().substring(0, 8),
                    "深化现有优势能力",
                    "能力提升",
                    1.0f,
                    0f,
                    Instant.now(),
                    Instant.now().plus(7, ChronoUnit.DAYS),
                    GoalPriority.MEDIUM,
                    GoalState.ACTIVE
                );
                goals.add(skillGoal);
                newGoals.add(skillGoal);
            }

            // 基于成功率设定提升目标
            if (analysis.overallSuccessRate() < 0.7f) {
                SelfGoal improvementGoal = new SelfGoal(
                    "goal-" + UUID.randomUUID().toString().substring(0, 8),
                    String.format("将交互成功率提升至70%%（当前:%.0f%%）", analysis.overallSuccessRate() * 100),
                    "成功率提升",
                    0.7f,
                    analysis.overallSuccessRate(),
                    Instant.now(),
                    Instant.now().plus(14, ChronoUnit.DAYS),
                    GoalPriority.HIGH,
                    GoalState.ACTIVE
                );
                goals.add(improvementGoal);
                newGoals.add(improvementGoal);
            }

            logger.info("Generated {} new goals from self-analysis", newGoals.size());
            return newGoals;
        }

        private SelfGoal createGoalForImprovement(String area, SelfAnalysis analysis) {
            String goalId = "goal-" + UUID.randomUUID().toString().substring(0, 8);

            // 从area中提取关键词作为类别
            String category = extractCategory(area);

            return new SelfGoal(
                goalId,
                "改进: " + area,
                category,
                1.0f,
                0f,
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                GoalPriority.MEDIUM,
                GoalState.ACTIVE
            );
        }

        private String extractCategory(String area) {
            String lower = area.toLowerCase();
            if (lower.contains("话题") || lower.contains("topic")) {
                return "话题处理";
            } else if (lower.contains("情感") || lower.contains("情绪")) {
                return "情感识别";
            } else if (lower.contains("任务")) {
                return "任务完成";
            }
            return "通用改进";
        }

        /**
         * 跟踪目标进度
         */
        public void trackProgress(SelfGoal goal, float progress) {
            for (int i = 0; i < goals.size(); i++) {
                if (goals.get(i).id().equals(goal.id())) {
                    SelfGoal updatedGoal = new SelfGoal(
                        goal.id(),
                        goal.description(),
                        goal.category(),
                        goal.targetProgress(),
                        progress,
                        goal.createdAt(),
                        goal.deadline(),
                        goal.priority(),
                        progress >= 0.95f ? GoalState.COMPLETED : GoalState.ACTIVE
                    );
                    goals.set(i, updatedGoal);

                    // 记录进度历史
                    List<ProgressEntry> history = progressHistory.computeIfAbsent(goal.id(), k -> new ArrayList<>());
                    history.add(new ProgressEntry(Instant.now(), progress));

                    logger.info("Tracked progress for goal '{}': {}%", goal.id(), progress * 100);
                    break;
                }
            }
        }

        /**
         * 获取活动目标
         */
        public List<SelfGoal> getActiveGoals() {
            return goals.stream()
                .filter(g -> g.state() == GoalState.ACTIVE)
                .sorted((a, b) -> {
                    int priorityCompare = b.priority().compareTo(a.priority());
                    if (priorityCompare != 0) return priorityCompare;
                    if (a.deadline() != null && b.deadline() != null) {
                        return a.deadline().compareTo(b.deadline());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        }

        /**
         * 获取目标状态
         */
        public GoalStatus getStatus() {
            int active = 0;
            int completed = 0;
            int overdue = 0;
            float totalProgress = 0f;

            for (SelfGoal goal : goals) {
                if (goal.state() == GoalState.ACTIVE) {
                    active++;
                    if (goal.isOverdue()) {
                        overdue++;
                    }
                } else if (goal.state() == GoalState.COMPLETED) {
                    completed++;
                }
                totalProgress += goal.progressPercentage();
            }

            return new GoalStatus(
                goals.size(),
                active,
                completed,
                overdue,
                goals.isEmpty() ? 0f : totalProgress / goals.size()
            );
        }

        private record ProgressEntry(Instant timestamp, float progress) {}
    }
}
