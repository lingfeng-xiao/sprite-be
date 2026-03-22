package com.lingfeng.sprite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.EvolutionEngine;
import com.lingfeng.sprite.Sprite;

/**
 * 进化应用服务
 *
 * 负责将进化引擎的结果应用到 Sprite
 */
@Service
public class EvolutionService {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionService.class);

    /**
     * 应用进化结果到 Sprite
     */
    public void applyEvolution(Sprite sprite) {
        // 执行进化
        EvolutionEngine.EvolutionResult result = sprite.evolve();

        if (result == null || !result.success()) {
            return;
        }

        // 记录进化洞察
        if (result.insight() != null) {
            logger.debug("Evolution insight: {} - {}",
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
}
