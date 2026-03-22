package com.lingfeng.sprite.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.Sprite;
import com.lingfeng.sprite.config.AppConfig;

/**
 * 进化应用服务
 *
 * 负责将进化引擎的结果应用到 Sprite
 * 支持定时触发和条件触发两种模式
 */
@Service
public class EvolutionService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionService.class);

    private final AppConfig appConfig;
    private Instant lastEvolutionTime = Instant.now();
    private Instant lastConditionCheckTime = Instant.now();

    public EvolutionService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 定时触发进化（每10分钟）
     * 也可以通过条件触发：反馈累积到一定数量时立即触发
     */
    @Scheduled(fixedRateString = "${sprite.evolution.interval-ms:600000}")
    public void scheduledEvolution() {
        logger.debug("Scheduled evolution check triggered");
        // This method is called by Spring scheduler but doesn't have direct sprite access
        // The actual evolution is applied through applyEvolution called by SpriteService
    }

    /**
     * 检查是否应该触发进化
     * 条件：1. 距离上次进化超过配置间隔
     *      2. 或者反馈累积超过最小阈值
     */
    public boolean shouldEvolve(Sprite sprite) {
        if (!appConfig.getEvolution().isEnabled()) {
            return false;
        }

        Instant now = Instant.now();
        long msSinceLastEvolution = ChronoUnit.MILLIS.between(lastEvolutionTime, now);

        // 条件1: 时间间隔到了
        boolean timeCondition = msSinceLastEvolution >= appConfig.getEvolution().getIntervalMs();

        // 条件2: 反馈累积够了
        EvolutionEngine.EvolutionStatus status = sprite.getEvolutionStatus();
        int recentFeedbackCount = 0;
        if (status != null && status.learningStats() != null) {
            recentFeedbackCount = status.learningStats().totalObservations();
        }
        boolean feedbackCondition = recentFeedbackCount >= appConfig.getEvolution().getMinFeedbackForEvolution();

        boolean shouldEvolve = timeCondition || feedbackCondition;

        if (shouldEvolve) {
            logger.info("Evolution triggered: timeCondition={}, feedbackCondition={}, recentFeedbackCount={}",
                    timeCondition, feedbackCondition, recentFeedbackCount);
        }

        return shouldEvolve;
    }

    /**
     * 应用进化结果到 Sprite
     */
    public void applyEvolution(Sprite sprite) {
        // 检查是否应该进化
        if (!shouldEvolve(sprite)) {
            return;
        }

        // 执行进化
        EvolutionEngine.EvolutionResult result = sprite.evolve();

        if (result == null || !result.success()) {
            return;
        }

        // 更新最后进化时间
        lastEvolutionTime = Instant.now();

        // 关键：应用更新后的自我模型到 Sprite
        if (result.updatedSelf() != null) {
            sprite.updateSelfModel(result.updatedSelf());
            logger.info("Applied evolution: new evolutionLevel={}, evolutionCount={}",
                    result.updatedSelf().evolutionLevel(),
                    result.updatedSelf().evolutionCount());
        }

        // 记录进化洞察
        if (result.insight() != null) {
            logger.info("Evolution insight: {} - {}",
                    result.insight().type(),
                    result.insight().hypothesis());
        }

        // 记录新原则
        if (result.principle() != null) {
            logger.info("New principle learned: {}", result.principle().statement());
        }

        // 记录行为改变
        if (result.appliedChange() != null) {
            logger.info("Behavior change: {} -> {}",
                    result.appliedChange().beforeBehavior(),
                    result.appliedChange().afterBehavior());
        }

        // 记录进化统计
        EvolutionEngine.EvolutionStatus status = sprite.getEvolutionStatus();
        if (status != null) {
            logger.debug("Evolution status - Level: {}, Count: {}",
                    status.evolutionLevel(), status.evolutionCount());
        }
    }

    /**
     * 手动触发一次进化（忽略条件检查）
     */
    public void forceEvolution(Sprite sprite) {
        logger.info("Force evolution triggered");
        lastEvolutionTime = Instant.now().minus(appConfig.getEvolution().getIntervalMs(), ChronoUnit.MILLIS);
        applyEvolution(sprite);
    }
}
