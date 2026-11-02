package com.lethouse.task.job;

import com.lethouse.task.mapper.UsageReconcileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 计费用量对账补偿任务。
 *
 * 背景：对话链路里用量上报是旁路能力（失败只记 warn，不影响对话），因此可能丢流水。
 * 本任务定时扫描"有 token 消耗但没有计费流水"的 assistant 消息，按模型单价补录回来。
 *
 * 幂等：SQL 内 LEFT JOIN ... IS NULL 过滤缺口 + ON DUPLICATE KEY UPDATE 兜底，
 * 多实例并发执行也不会重复记账。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageReconcileJob {

    private static final String LOCK_KEY = "loh:job:usage-reconcile";
    /** 只补最近 N 天，避免每次全表扫 */
    private static final int WINDOW_DAYS = 7;
    /** 单批条数 */
    private static final int BATCH = 500;
    /** 单次最多跑几批，防止积压时把数据库打满 */
    private static final int MAX_ROUNDS = 10;

    private final UsageReconcileMapper usageReconcileMapper;
    private final StringRedisTemplate redisTemplate;

    /** 每小时第 7、37 分各跑一次，避开整点任务高峰 */
    @Scheduled(cron = "0 7,37 * * * ?")
    public void run() {
        // 分布式锁：多实例部署时只让一个实例执行，拿不到锁直接跳过（不排队、不阻塞）
        Boolean got = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(got)) {
            log.debug("对账任务已由其他实例执行，跳过");
            return;
        }
        try {
            int total = 0;
            for (int round = 0; round < MAX_ROUNDS; round++) {
                int n = usageReconcileMapper.backfill(WINDOW_DAYS, BATCH);
                total += n;
                if (n < BATCH) {
                    break;   // 没补满一批，说明已经补齐
                }
            }
            if (total > 0) {
                log.warn("对账补偿：本次补录 {} 条缺失流水，请检查计费服务可用性", total);
            } else {
                log.debug("对账补偿：无缺口");
            }
        } catch (Exception e) {
            log.error("对账补偿执行失败", e);
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
