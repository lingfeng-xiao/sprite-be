package com.lingfeng.sprite.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lingfeng.sprite.MemorySystem;
import com.lingfeng.sprite.MemorySystem.Pattern;
import com.lingfeng.sprite.MemorySystem.StoreType;
import com.lingfeng.sprite.MemorySystem.WorkingMemoryItem;

/**
 * 记忆整合服务
 *
 * 负责将感官记忆中的模式固化为工作记忆，
 * 并定期将工作记忆存入长期记忆
 */
@Service
public class MemoryConsolidationService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryConsolidationService.class);

    /**
     * 执行记忆整合
     *
     * @param memory 要整合的记忆系统
     */
    public void consolidateIfNeeded(MemorySystem.Memory memory) {
        if (memory == null) {
            return;
        }

        // 1. 从感官记忆检测模式
        List<Pattern> patterns = memory.getSensory().detectPattern();

        if (!patterns.isEmpty()) {
            logger.debug("Detected {} patterns from sensory memory", patterns.size());
        }

        // 2. 将模式固化为工作记忆
        for (Pattern pattern : patterns) {
            String abstraction = "Pattern: " + pattern.type() + " (freq=" + pattern.frequency() + ")";
            String content = pattern.description();

            WorkingMemoryItem item = memory.consolidateToWorking(pattern, abstraction, content);
            logger.debug("Consolidated pattern to working memory: {}", abstraction);
        }

        // 3. 检查工作记忆中的项是否需要存入长期记忆
        for (WorkingMemoryItem item : memory.getWorking().getAll()) {
            // 如果项的相关性足够高，存入长期记忆
            if (item.relevance() > 0.6f) {
                // 根据刺激类型决定存储类型
                StoreType storeType = inferStoreType(item);
                memory.storeToLongTerm(item, storeType);
                logger.debug("Stored item to long-term memory: {} -> {}",
                        item.abstraction(), storeType);
            }
        }
    }

    /**
     * 从工作记忆项推断存储类型
     */
    private StoreType inferStoreType(WorkingMemoryItem item) {
        if (item.source() == null || item.source().type() == null) {
            return StoreType.EPISODIC;
        }

        return switch (item.source().type()) {
            case COMMAND -> StoreType.PROCEDURAL;
            case EMOTIONAL -> StoreType.EPISODIC;
            case ENVIRONMENT -> StoreType.SEMANTIC;
            default -> StoreType.EPISODIC;
        };
    }

    /**
     * 强制将所有工作记忆项存入长期记忆
     *
     * @param memory 要整合的记忆系统
     */
    public void forceConsolidateAll(MemorySystem.Memory memory) {
        if (memory == null) {
            return;
        }

        for (WorkingMemoryItem item : memory.getWorking().getAll()) {
            StoreType storeType = inferStoreType(item);
            memory.storeToLongTerm(item, storeType);
        }
        logger.info("Force consolidated {} items to long-term memory",
                memory.getWorking().size());
    }
}
