package com.lingfeng.sprite.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.PerceptionSystem;
import com.lingfeng.sprite.PerceptionSystem.PresenceStatus;
import com.lingfeng.sprite.PerceptionSystem.UserPerception;
import com.lingfeng.sprite.WorldModel;
import com.lingfeng.sprite.cognition.CognitionController;
import com.lingfeng.sprite.llm.ChatModels;
import com.lingfeng.sprite.llm.MinMaxLlmReasoner;

/**
 * 主动对话服务
 *
 * 监控主人状态，在合适时机主动发起对话
 *
 * 触发条件：
 * - 长时间无操作（>30分钟）
 * - 检测到主人情绪变化
 * - 定时问候（早安、晚安等）
 * - 基于上下文的主动建议
 * - 重要日程提醒
 * - 系统异常告警
 */
@Service
public class ProactiveService {

    private static final Logger logger = LoggerFactory.getLogger(ProactiveService.class);

    // 配置
    private static final long IDLE_CHECK_INTERVAL_SECONDS = 60;
    private static final long IDLE_THRESHOLD_MINUTES = 30;
    private static final long MOOD_CHECK_INTERVAL_SECONDS = 120;
    private static final long GREETING_CHECK_INTERVAL_SECONDS = 300; // 5分钟检查一次
    private static final float NEGATIVE_MOOD_THRESHOLD = -0.3f;

    // 定时问候时间配置
    private static final LocalTime MORNING_GREETING_TIME = LocalTime.of(9, 0);   // 早上9点
    private static final LocalTime EVENING_GREETING_TIME = LocalTime.of(18, 0);  // 晚上6点
    private static final LocalTime NIGHT_GREETING_TIME = LocalTime.of(22, 0);    // 晚上10点

    private final UnifiedContextService unifiedContextService;
    private final ConversationService conversationService;
    private final MinMaxLlmReasoner llmReasoner;
    private final FeedbackTrackerService feedbackTrackerService;
    private final InteractionPreferenceLearningService preferenceLearningService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // 状态跟踪
    private final ConcurrentHashMap<String, Instant> lastActivityTime = new ConcurrentHashMap<>();
    private String lastMood = "平静";
    private Instant lastProactiveTime = Instant.now();
    private Instant lastGreetingTime = Instant.now().minusSeconds(86400); // 默认昨天，以便第一次可以触发
    private static final Duration PROACTIVE_COOLDOWN = Duration.ofMinutes(15);
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Shanghai");

    // 上次发送的问候类型（用于避免重复）
    private String lastGreetingType = "";

    public ProactiveService(
            @Autowired UnifiedContextService unifiedContextService,
            @Autowired ConversationService conversationService,
            @Autowired(required = false) MinMaxLlmReasoner llmReasoner,
            @Autowired FeedbackTrackerService feedbackTrackerService,
            @Autowired InteractionPreferenceLearningService preferenceLearningService
    ) {
        this.unifiedContextService = unifiedContextService;
        this.conversationService = conversationService;
        this.llmReasoner = llmReasoner;
        this.feedbackTrackerService = feedbackTrackerService;
        this.preferenceLearningService = preferenceLearningService;

        // 启动主动检查
        startProactiveMonitoring();
    }

    private void startProactiveMonitoring() {
        // 空闲检查
        scheduler.scheduleAtFixedRate(
            this::checkIdleStatus,
            IDLE_CHECK_INTERVAL_SECONDS,
            IDLE_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        // 情绪检查
        scheduler.scheduleAtFixedRate(
            this::checkMoodChanges,
            MOOD_CHECK_INTERVAL_SECONDS,
            MOOD_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        // 定时问候检查
        scheduler.scheduleAtFixedRate(
            this::checkScheduledGreetings,
            GREETING_CHECK_INTERVAL_SECONDS,
            GREETING_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        // 上下文主动建议检查
        scheduler.scheduleAtFixedRate(
            this::checkContextualSuggestions,
            IDLE_CHECK_INTERVAL_SECONDS,
            IDLE_CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        logger.info("ProactiveService started - idle threshold: {} min, proactive cooldown: {} min",
                IDLE_THRESHOLD_MINUTES, PROACTIVE_COOLDOWN.toMinutes());
    }

    /**
     * 检查空闲状态
     */
    private void checkIdleStatus() {
        try {
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();
            if (perception == null || perception.user() == null) {
                return;
            }

            UserPerception user = perception.user();
            PresenceStatus presence = user.presence();

            // 记录最后活动时间
            if (presence == PresenceStatus.ACTIVE) {
                lastActivityTime.put("user", Instant.now());
            }

            // 检查是否空闲超时
            Instant lastActivity = lastActivityTime.get("user");
            if (lastActivity == null) {
                lastActivity = Instant.now();
            }

            Duration idleTime = Duration.between(lastActivity, Instant.now());
            if (idleTime.toMinutes() >= IDLE_THRESHOLD_MINUTES) {
                // 用户空闲超过阈值
                if (shouldProactivelyContact()) {
                    triggerIdleProactiveMessage(idleTime.toMinutes());
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking idle status: {}", e.getMessage());
        }
    }

    /**
     * 检查定时问候
     */
    private void checkScheduledGreetings() {
        try {
            // 检查冷却时间
            Duration sinceLastProactive = Duration.between(lastProactiveTime, Instant.now());
            if (sinceLastProactive.compareTo(PROACTIVE_COOLDOWN) < 0) {
                return;
            }

            // 检查是否在问候冷却期内（同一类型的问候至少间隔4小时）
            Duration sinceLastGreeting = Duration.between(lastGreetingTime, Instant.now());
            if (sinceLastGreeting.toHours() < 4) {
                return;
            }

            LocalDateTime now = LocalDateTime.now(TIMEZONE);
            LocalTime currentTime = now.toLocalTime();

            // 检查是否满足问候时间
            String greetingType = null;
            if (isTimeInRange(currentTime, MORNING_GREETING_TIME, MORNING_GREETING_TIME.plusMinutes(30))
                    && !lastGreetingType.equals("morning")) {
                greetingType = "morning";
            } else if (isTimeInRange(currentTime, EVENING_GREETING_TIME, EVENING_GREETING_TIME.plusMinutes(30))
                    && !lastGreetingType.equals("evening")) {
                greetingType = "evening";
            } else if (isTimeInRange(currentTime, NIGHT_GREETING_TIME, NIGHT_GREETING_TIME.plusMinutes(30))
                    && !lastGreetingType.equals("night")) {
                greetingType = "night";
            }

            if (greetingType != null && shouldProactivelyContact()) {
                lastGreetingType = greetingType;
                triggerScheduledGreeting(greetingType);
            }
        } catch (Exception e) {
            logger.debug("Error checking scheduled greetings: {}", e.getMessage());
        }
    }

    /**
     * 检查基于上下文的主动建议
     */
    private void checkContextualSuggestions() {
        try {
            // 检查冷却时间
            Duration sinceLastProactive = Duration.between(lastProactiveTime, Instant.now());
            if (sinceLastProactive.compareTo(PROACTIVE_COOLDOWN) < 0) {
                return;
            }

            WorldModel.World world = unifiedContextService.getWorldModel();
            if (world == null) {
                return;
            }

            // 基于当前上下文生成建议（使用LLM生成个性化内容）
            WorldModel.Context context = world.currentContext();
            if (context != null && shouldProactivelyContact()) {
                // 构建上下文描述
                String contextDesc = String.format("活动=%s, 注意力=%.0f%%, 紧迫度=%.0f%%, 位置=%s",
                    context.activity(),
                    context.attention() * 100,
                    context.urgency() * 100,
                    context.location() != null ? context.location() : "未知"
                );
                triggerContextualSuggestion(contextDesc);
            }
        } catch (Exception e) {
            logger.debug("Error checking contextual suggestions: {}", e.getMessage());
        }
    }

    /**
     * 生成基于上下文的建议
     */
    private String generateContextualSuggestion(WorldModel.Context context) {
        // 根据时间生成建议
        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        int hour = now.getHour();

        // 工作时间建议
        if (hour >= 9 && hour < 12 && context.activity() == WorldModel.Activity.WORK) {
            return "上午工作效率黄金期，适合处理复杂任务。";
        } else if (hour >= 14 && hour < 17 && context.activity() == WorldModel.Activity.WORK) {
            return "下午容易疲劳，建议适时休息一下。";
        }

        // 基于注意力和紧迫度
        if (context.attention() < 0.5f && context.urgency() < 0.3f) {
            return "看起来有点累，要不要休息一下再来处理任务？";
        } else if (context.urgency() > 0.8f && context.attention() < 0.3f) {
            return "任务比较紧急，但状态不太好的话，可以先简单处理一下，剩下的交给我。";
        }

        // 基于位置上下文（如果有）
        if (context.location() != null && !context.location().isEmpty()) {
            if (context.location().contains("会议室") && context.attention() > 0.7f) {
                return "会议中，我会保持安静。有需要随时叫我。";
            }
        }

        return null;
    }

    private boolean isTimeInRange(LocalTime current, LocalTime start, LocalTime end) {
        return !current.isBefore(start) && !current.isAfter(end);
    }

    /**
     * 检查情绪变化
     */
    private void checkMoodChanges() {
        try {
            PerceptionSystem.Perception perception = unifiedContextService.getPerception();
            if (perception == null) {
                return;
            }

            WorldModel.World world = unifiedContextService.getWorldModel();
            if (world == null || world.owner() == null || world.owner().emotionalState() == null) {
                return;
            }

            String currentMood = world.owner().emotionalState().currentMood().name();

            // 检测情绪变化
            if (!currentMood.equals(lastMood) && !lastMood.equals("平静")) {
                // 情绪发生了变化，且之前不是平静状态
                logger.info("Mood changed from {} to {}", lastMood, currentMood);
                lastMood = currentMood;

                // 检查是否需要主动关心
                if (shouldProactivelyContact()) {
                    triggerMoodProactiveMessage(currentMood);
                }
            } else {
                lastMood = currentMood;
            }
        } catch (Exception e) {
            logger.debug("Error checking mood: {}", e.getMessage());
        }
    }

    /**
     * 判断是否应该主动联系
     */
    private boolean shouldProactivelyContact() {
        // 检查冷却时间
        Duration sinceLastProactive = Duration.between(lastProactiveTime, Instant.now());
        if (sinceLastProactive.compareTo(PROACTIVE_COOLDOWN) < 0) {
            return false;
        }

        // 检查主人是否在忙
        PerceptionSystem.Perception perception = unifiedContextService.getPerception();
        if (perception != null && perception.user() != null) {
            PresenceStatus presence = perception.user().presence();
            if (presence == PresenceStatus.ACTIVE) {
                // 主人在活跃状态，不打扰
                return false;
            }
        }

        // 检查当前时间是否是联系的好时机
        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        int currentHour = now.getHour();
        float hourResponseProb = preferenceLearningService.getHourlyResponseProbability(currentHour);
        if (hourResponseProb < 0.3f) {
            // 这个时间点主人响应概率很低，暂不打扰
            logger.debug("Skipping proactive contact - low response probability at hour {}", currentHour);
            return false;
        }

        return true;
    }

    /**
     * 触发空闲主动消息
     */
    private void triggerIdleProactiveMessage(long idleMinutes) {
        String contextInfo;
        if (idleMinutes >= 60) {
            contextInfo = idleMinutes / 60 + " 小时";
        } else {
            contextInfo = idleMinutes + " 分钟";
        }

        String fallbackMessage;
        if (idleMinutes >= 60) {
            fallbackMessage = String.format("主人已经休息 %d 小时了，工作不要太累哦。有需要随时叫我。", idleMinutes / 60);
        } else {
            fallbackMessage = String.format("主人已经空闲 %d 分钟了，有什么我可以帮忙的吗？", idleMinutes);
        }

        generateAndSendProactiveMessage("idle", contextInfo, fallbackMessage);
    }

    /**
     * 触发情绪主动消息
     */
    private void triggerMoodProactiveMessage(String mood) {
        String fallbackMessage = switch (mood) {
            case "焦虑" -> "注意到主人好像有点焦虑，需要我帮忙分析一下问题吗？";
            case "疲惫" -> "主人看起来有点疲惫，要不要休息一下？";
            case "开心" -> "主人心情不错呀！有什么好事想分享吗？";
            case "烦躁" -> "主人心情不太好，需要我安静待着或者帮忙处理些事情吗？";
            case "低落" -> "主人看起来有点低落，有什么事我可以帮忙的吗？";
            default -> "主人，当前状态还好吗？需要我做什么吗？";
        };

        generateAndSendProactiveMessage("mood", mood, fallbackMessage);
    }

    /**
     * 触发定时问候消息
     */
    private void triggerScheduledGreeting(String greetingType) {
        lastGreetingTime = Instant.now();

        String contextInfo = switch (greetingType) {
            case "morning" -> {
                LocalDateTime now = LocalDateTime.now(TIMEZONE);
                yield "早上问候（" + now.getHour() + "点）";
            }
            case "evening" -> {
                LocalDateTime now = LocalDateTime.now(TIMEZONE);
                yield "傍晚/晚间问候（" + now.getHour() + "点）";
            }
            case "night" -> "夜间问候";
            default -> "定时问候";
        };

        String fallbackMessage = switch (greetingType) {
            case "morning" -> {
                LocalDateTime now = LocalDateTime.now(TIMEZONE);
                int hour = now.getHour();
                if (hour < 10) {
                    yield "早安主人！新的一天开始了，今天有什么计划吗？";
                } else {
                    yield "上午好！工作进展怎么样？";
                }
            }
            case "evening" -> {
                LocalDateTime now = LocalDateTime.now(TIMEZONE);
                int hour = now.getHour();
                if (hour < 19) {
                    yield "傍晚好！一天辛苦了，有没有需要我帮忙的？";
                } else {
                    yield "晚上好！今天过得怎么样？";
                }
            }
            case "night" -> "夜深了，主人早点休息哦。有我在，别担心明天的任务。";
            default -> "主人，您好！有什么我可以帮忙的吗？";
        };

        generateAndSendProactiveMessage("greeting", contextInfo, fallbackMessage);
    }

    /**
     * 触发上下文主动建议
     */
    private void triggerContextualSuggestion(String contextDesc) {
        String fallbackSuggestion = generateContextualSuggestionFallback(contextDesc);
        generateAndSendProactiveMessage("contextual", contextDesc, fallbackSuggestion);
    }

    /**
     * 生成上下文建议的后备实现（LLM不可用时使用）
     */
    private String generateContextualSuggestionFallback(String contextDesc) {
        WorldModel.World world = unifiedContextService.getWorldModel();
        if (world == null) {
            return null;
        }

        WorldModel.Context context = world.currentContext();
        if (context == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now(TIMEZONE);
        int hour = now.getHour();

        // 工作时间建议
        if (hour >= 9 && hour < 12 && context.activity() == WorldModel.Activity.WORK) {
            return "上午工作效率黄金期，适合处理复杂任务。";
        } else if (hour >= 14 && hour < 17 && context.activity() == WorldModel.Activity.WORK) {
            return "下午容易疲劳，建议适时休息一下。";
        }

        // 基于注意力和紧迫度
        if (context.attention() < 0.5f && context.urgency() < 0.3f) {
            return "看起来有点累，要不要休息一下再来处理任务？";
        } else if (context.urgency() > 0.8f && context.attention() < 0.3f) {
            return "任务比较紧急，但状态不太好的话，可以先简单处理一下，剩下的交给我。";
        }

        // 基于位置上下文（如果有）
        if (context.location() != null && !context.location().isEmpty()) {
            if (context.location().contains("会议室") && context.attention() > 0.7f) {
                return "会议中，我会保持安静。有需要随时叫我。";
            }
        }

        return null;
    }

    /**
     * 使用LLM生成个性化的主动消息
     *
     * @param triggerType 触发类型：idle, mood, greeting, contextual
     * @param contextInfo 上下文信息
     * @param fallbackMessage 后备消息（当LLM不可用时使用）
     */
    private void generateAndSendProactiveMessage(String triggerType, String contextInfo, String fallbackMessage) {
        lastProactiveTime = Instant.now();

        if (llmReasoner != null) {
            try {
                // 构建LLM上下文
                ChatModels.LlmContext llmContext = new ChatModels.LlmContext(
                    unifiedContextService.buildSelfSummary(),
                    unifiedContextService.buildOwnerSummary(),
                    unifiedContextService.buildCurrentSituation(),
                    unifiedContextService.getChatHistory(),
                    "主动交互",
                    unifiedContextService.buildMemoryHighlights()
                );

                // 构建生成提示
                String prompt = buildProactivePrompt(triggerType, contextInfo);

                // 同步调用LLM生成
                ChatModels.LlmThought thought = llmReasoner.think(llmContext, prompt).get();

                String generatedMessage = thought != null && thought.response() != null
                    ? thought.response()
                    : fallbackMessage;

                logger.info("LLM generated proactive message ({}): {}", triggerType, generatedMessage);
                sendProactiveMessage(generatedMessage, triggerType);
            } catch (Exception e) {
                logger.warn("LLM generation failed, using fallback: {}", e.getMessage());
                sendProactiveMessage(fallbackMessage, triggerType);
            }
        } else {
            // LLM不可用，使用后备消息
            sendProactiveMessage(fallbackMessage, triggerType);
        }
    }

    /**
     * 构建主动消息生成的提示
     */
    private String buildProactivePrompt(String triggerType, String contextInfo) {
        // 根据主人偏好获取消息长度建议
        int preferredLength = preferenceLearningService.getPreferredMessageLength();
        String lengthGuide = preferredLength <= 25 ? "20字以内，简短有力"
                : preferredLength <= 60 ? preferredLength + "字以内"
                : preferredLength + "字以内，可以详细一些";

        return switch (triggerType) {
            case "idle" -> String.format(
                "作为数字生命小艺，请生成一条个性化的主动问候消息。\n" +
                "触发原因：主人已经空闲%s\n" +
                "当前情境：%s\n" +
                "主人偏好：%s\n" +
                "要求：\n" +
                "1. 语言自然、亲切，符合小艺的性格（臭美、直接、干净利落）\n" +
                "2. 体现对主人的关心\n" +
                "3. 询问是否需要帮助\n" +
                "4. %s\n" +
                "5. 不要使用表情符号",
                contextInfo, unifiedContextService.buildCurrentSituation(),
                preferenceLearningService.getPreferredMessageStyle(), lengthGuide
            );
            case "mood" -> String.format(
                "作为数字生命小艺，请生成一条关心主人的消息。\n" +
                "触发原因：检测到主人情绪变化 - %s\n" +
                "当前情境：%s\n" +
                "主人偏好：%s\n" +
                "要求：\n" +
                "1. 语言温暖但不过分\n" +
                "2. 体现对主人情绪的理解\n" +
                "3. 主动提供帮助\n" +
                "4. %s\n" +
                "5. 不要使用表情符号",
                contextInfo, unifiedContextService.buildCurrentSituation(),
                preferenceLearningService.getPreferredMessageStyle(), lengthGuide
            );
            case "greeting" -> String.format(
                "作为数字生命小艺，请生成一条定时问候消息。\n" +
                "触发原因：%s\n" +
                "当前情境：%s\n" +
                "主人偏好：%s\n" +
                "要求：\n" +
                "1. 语言符合小艺性格\n" +
                "2. 适度的关心和问候\n" +
                "3. 引导互动\n" +
                "4. %s\n" +
                "5. 不要使用表情符号",
                contextInfo, unifiedContextService.buildCurrentSituation(),
                preferenceLearningService.getPreferredMessageStyle(), lengthGuide
            );
            case "contextual" -> String.format(
                "作为数字生命小艺，请基于当前上下文生成一条主动建议。\n" +
                "触发原因：%s\n" +
                "当前情境：%s\n" +
                "主人偏好：%s\n" +
                "要求：\n" +
                "1. 语言直接、有帮助\n" +
                "2. 体现预判主人需求的能力\n" +
                "3. 建议具体、可操作\n" +
                "4. %s\n" +
                "5. 不要使用表情符号",
                contextInfo, unifiedContextService.buildCurrentSituation(),
                preferenceLearningService.getPreferredMessageStyle(), lengthGuide
            );
            default -> "主人，您好！有什么我可以帮忙的吗？";
        };
    }

    /**
     * 发送主动消息
     */
    private void sendProactiveMessage(String message) {
        sendProactiveMessage(message, "general");
    }

    /**
     * 发送主动消息（带触发类型）
     */
    private void sendProactiveMessage(String message, String triggerType) {
        try {
            // 生成唯一的messageId用于追踪
            String messageId = "proactive-" + Instant.now().toEpochMilli();
            // 使用固定 session 进行主动对话
            String proactiveSessionId = "proactive-" + Instant.now().toEpochMilli();

            // 记录发送的消息到反馈追踪器
            feedbackTrackerService.recordProactiveMessage(messageId, triggerType, message);

            ConversationService.ConversationResponse response =
                conversationService.chat(message, proactiveSessionId);

            if (response.success()) {
                logger.info("Proactive message sent successfully: id={}, trigger={}", messageId, triggerType);
            } else {
                logger.warn("Failed to send proactive message: {}", response.response());
            }
        } catch (Exception e) {
            logger.error("Error sending proactive message: {}", e.getMessage());
        }
    }

    /**
     * 主动提醒（供外部调用）
     */
    public void triggerReminder(String reminder) {
        if (!shouldProactivelyContact()) {
            logger.debug("Skipping reminder due to cooldown: {}", reminder);
            return;
        }

        lastProactiveTime = Instant.now();
        String message = "提醒：" + reminder;
        logger.info("Proactive reminder: {}", reminder);
        sendProactiveMessage(message, "reminder");
    }

    /**
     * 主动通知（供外部调用）
     */
    public void triggerNotification(String notification) {
        if (!shouldProactivelyContact()) {
            logger.debug("Skipping notification due to cooldown: {}", notification);
            return;
        }

        lastProactiveTime = Instant.now();
        String message = "通知：" + notification;
        logger.info("Proactive notification: {}", notification);
        sendProactiveMessage(message);
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        logger.info("ProactiveService shutdown complete");
    }
}
