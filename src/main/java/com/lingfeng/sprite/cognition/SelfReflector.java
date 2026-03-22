package com.lingfeng.sprite.cognition;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.SelfModel;
import com.lingfeng.sprite.WorldModel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 主动反思引擎 - 定时自问"我是谁/主人在想什么"
 *
 * ## 反思类型
 *
 * 1. **定时反思**：每10分钟触发一次，问"这段时间发生了什么"
 * 2. **新情境反思**：遇到新情况时触发，问"这意味着什么"
 * 3. **失败反思**：行动失败时触发，问"哪里出了问题"
 * 4. **里程碑反思**：达成重要成长时触发，问"我学到了什么"
 */
public class SelfReflector {
    private final long reflectionIntervalMinutes;
    private Instant lastReflectionTime = Instant.now();
    private int reflectionCount = 0;
    private final List<ReflectionRecord> reflectionHistory = new ArrayList<>();
    private String lastOwnerThought = null;

    public SelfReflector() {
        this(10L);
    }

    public SelfReflector(long reflectionIntervalMinutes) {
        this.reflectionIntervalMinutes = reflectionIntervalMinutes;
    }

    /**
     * 反思入口
     */
    public ReflectionResult reflect(
        SelfModel.Self selfModel,
        PerceptionSystem.Perception perception,
        WorldModel.World worldModel
    ) {
        List<Insight> insights = new ArrayList<>();

        // 1. 检查是否需要定时反思
        if (shouldReflectOnSchedule()) {
            insights.add(reflectOnSchedule(selfModel, perception, worldModel));
            lastReflectionTime = Instant.now();
        }

        // 2. 检查是否是新情境
        if (isNovelSituation(perception, worldModel)) {
            insights.add(reflectOnNovelSituation(selfModel, perception, worldModel));
        }

        // 3. 检查主人情绪状态
        WorldModel.EmotionalState emotion = worldModel.owner().emotionalState();
        if (emotion != null && emotion.intensity() > 0.6f) {
            insights.add(reflectOnOwnerEmotion(selfModel, emotion));
        }

        // 4. 检查显著性变化
        if (perception.generateFeelings().size() > 3) {
            insights.add(reflectOnEnvironmentChange(selfModel, perception));
        }

        boolean hasInsight = !insights.isEmpty();
        String primaryInsight = insights.isEmpty() ? "没有新的反思" :
            insights.stream()
                .max((a, b) -> Float.compare(a.importance(), b.importance()))
                .map(Insight::content)
                .orElse("没有新的反思");

        String ownerThought = inferOwnerThought(perception, worldModel);
        lastOwnerThought = ownerThought;

        if (hasInsight) {
            reflectionCount++;
            reflectionHistory.add(new ReflectionRecord(
                Instant.now(),
                new ArrayList<>(insights),
                selfModel.identity().displayName(),
                worldModel.owner().identity().name()
            ));

            if (reflectionHistory.size() > 50) {
                reflectionHistory.remove(0);
            }
        }

        return new ReflectionResult(
            hasInsight,
            primaryInsight,
            new ArrayList<>(insights),
            ownerThought,
            reflectionCount
        );
    }

    private boolean shouldReflectOnSchedule() {
        long elapsed = Duration.between(lastReflectionTime, Instant.now()).toMinutes();
        return elapsed >= reflectionIntervalMinutes;
    }

    private Insight reflectOnSchedule(
        SelfModel.Self selfModel,
        PerceptionSystem.Perception perception,
        WorldModel.World worldModel
    ) {
        List<String> feelings = perception.generateFeelings();
        String context = perception.environment() != null ?
            perception.environment().context().name() : "未知";
        String mood = worldModel.owner().emotionalState() != null ?
            worldModel.owner().emotionalState().currentMood().name() : "未知";
        String feeling = feelings.isEmpty() ? "没什么特别的" : feelings.get(0);

        String content = String.format(
            "过去%d分钟回顾：我在%s状态下感知到%s。主人当前%s。最强烈的感受是%s。",
            reflectionIntervalMinutes,
            context,
            String.join(", ", feelings),
            mood,
            feeling
        );

        return new Insight(InsightType.SCHEDULE, content, 0.5f, "定时触发");
    }

    private Insight reflectOnNovelSituation(
        SelfModel.Self selfModel,
        PerceptionSystem.Perception perception,
        WorldModel.World worldModel
    ) {
        String window = perception.user() != null && perception.user().activeWindow() != null ?
            perception.user().activeWindow().title() : "未知窗口";

        String content = String.format(
            "遇到新情况：主人正在使用%s。这可能意味着%s。",
            window,
            inferWindowMeaning(window)
        );

        return new Insight(InsightType.NOVEL_SITUATION, content, 0.7f, "新情境检测");
    }

    private Insight reflectOnOwnerEmotion(
        SelfModel.Self selfModel,
        WorldModel.EmotionalState emotion
    ) {
        String content = String.format(
            "主人情绪%s（强度%d%%）。触发因素：%s。我应该%s。",
            emotion.currentMood().name(),
            (int) (emotion.intensity() * 100),
            String.join(", ", emotion.triggers()),
            getEmotionalResponseGuidance(emotion.currentMood())
        );

        return new Insight(InsightType.OWNER_EMOTION, content, emotion.intensity(), "主人情绪变化");
    }

    private Insight reflectOnEnvironmentChange(
        SelfModel.Self selfModel,
        PerceptionSystem.Perception perception
    ) {
        String content = "环境发生多种变化：" +
            String.join(", ", perception.generateFeelings()) +
            "。需要关注这些变化是否影响主人或我。";

        return new Insight(InsightType.ENVIRONMENT_CHANGE, content, 0.4f, "多维度变化检测");
    }

    private String inferWindowMeaning(String window) {
        if (window.contains("Code")) return "主人在编程或处理技术任务";
        if (window.contains("Chrome") || window.contains("Edge")) return "主人在浏览网页，可能在查找信息";
        if (window.contains("Terminal") || window.contains("CMD")) return "主人在执行命令";
        if (window.contains("Notion") || window.contains("文档")) return "主人在处理文档或笔记";
        if (window.contains("Slack") || window.contains("微信")) return "主人在沟通交流";
        return "主人正在进行某种活动";
    }

    private String getEmotionalResponseGuidance(WorldModel.Mood mood) {
        switch (mood) {
            case ANXIOUS: return "更加警觉，准备提供帮助";
            case FRUSTRATED: return "保持耐心，准备协助解决问题";
            case TIRED: return "降低打扰，考虑是否需要主动服务";
            case CONFUSED: return "准备提供清晰的解释";
            case SAD: return "给予关心和温暖";
            case HAPPY: return "可以适当增加互动";
            default: return "保持正常状态";
        }
    }

    private boolean isNovelSituation(
        PerceptionSystem.Perception perception,
        WorldModel.World worldModel
    ) {
        String currentWindow = perception.user() != null && perception.user().activeWindow() != null ?
            perception.user().activeWindow().title() : null;

        boolean lastWindow = true;
        for (int i = reflectionHistory.size() - 1; i >= 0; i--) {
            ReflectionRecord record = reflectionHistory.get(i);
            for (Insight insight : record.insights()) {
                if (insight.type() == InsightType.NOVEL_SITUATION) {
                    if (currentWindow == null || !insight.content().contains(currentWindow)) {
                        lastWindow = false;
                    }
                    break;
                }
            }
            if (!lastWindow) break;
        }

        PerceptionSystem.ContextType currentContext = perception.environment() != null ?
            perception.environment().context() : null;
        String lastContext = worldModel.currentContext().activity().name();

        return !lastWindow || (currentContext != null && !currentContext.name().equals(lastContext));
    }

    private String inferOwnerThought(
        PerceptionSystem.Perception perception,
        WorldModel.World worldModel
    ) {
        String window = perception.user() != null && perception.user().activeWindow() != null ?
            perception.user().activeWindow().title() : null;
        if (window == null) return "无法推断";

        StringBuilder sb = new StringBuilder();
        sb.append(inferWindowMeaning(window));

        WorldModel.EmotionalState emotion = worldModel.owner().emotionalState();
        if (emotion != null) {
            sb.append("。情绪状态").append(emotion.currentMood().name()).append("可能表示");
            switch (emotion.currentMood()) {
                case ANXIOUS: sb.append("有些担忧或不安"); break;
                case FRUSTRATED: sb.append("遇到了一些困难"); break;
                case TIRED: sb.append("可能需要休息"); break;
                case CONFUSED: sb.append("可能有些疑惑"); break;
                default: sb.append("状态正常");
            }
        }

        PerceptionSystem.EnvironmentPerception env = perception.environment();
        if (env != null) {
            sb.append("。现在是").append(env.hourOfDay()).append("点，");
            switch (env.context()) {
                case WORK: sb.append("应该是工作时间"); break;
                case LEISURE: sb.append("应该是休息时间"); break;
                case SLEEP: sb.append("应该是休息时间"); break;
                default: sb.append("时间状态不确定");
            }
        }

        return sb.toString();
    }

    /**
     * 应用洞察到自我模型
     */
    public SelfModel.Self applyInsight(SelfModel.Self selfModel, ReflectionResult reflection) {
        if (!reflection.hasInsight()) return selfModel;

        Insight firstInsight = reflection.insights().isEmpty() ? null : reflection.insights().get(0);
        SelfModel.Self updated = selfModel.addReflection(
            firstInsight != null ? firstInsight.triggeredBy() : "unknown",
            reflection.insight(),
            null
        );

        boolean hasHighImportance = reflection.insights().stream()
            .anyMatch(i -> i.importance() > 0.7f);
        if (hasHighImportance) {
            updated = updated.recordGrowth(
                SelfModel.GrowthType.IDENTITY_DEEPENED,
                "反思: " + reflection.insight().substring(0, Math.min(50, reflection.insight().length())) + "...",
                "未反思状态",
                "反思后认知",
                "主动反思"
            );
        }

        return updated;
    }

    public int getReflectionCount() {
        return reflectionCount;
    }

    public List<ReflectionRecord> getReflectionHistory() {
        return new ArrayList<>(reflectionHistory);
    }

    public enum InsightType {
        SCHEDULE,
        NOVEL_SITUATION,
        OWNER_EMOTION,
        ENVIRONMENT_CHANGE,
        FAILURE,
        MILESTONE
    }

    public record Insight(
        InsightType type,
        String content,
        float importance,
        String triggeredBy
    ) {}

    public record ReflectionResult(
        boolean hasInsight,
        String insight,
        List<Insight> insights,
        String ownerThought,
        int reflectionCount
    ) {}

    public record ReflectionRecord(
        Instant timestamp,
        List<Insight> insights,
        String selfState,
        String ownerState
    ) {}
}
