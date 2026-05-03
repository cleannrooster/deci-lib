package com.cleannrooster.decilib.ai.brain;

import com.cleannrooster.decilib.ai.DebugConfig;
import com.cleannrooster.decilib.ai.goal.GoalBinding;
import net.minecraft.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.Map;

final class GoalCircuitBreaker<T extends LivingEntity, S extends Enum<S>, C extends Enum<C>> {
    private static final Logger LOGGER              = LoggerFactory.getLogger(GoalCircuitBreaker.class);
    static final         int    FAILURE_THRESHOLD   = 3;
    static final         int    PERMANENT_THRESHOLD = 6;
    static final         int    BACKOFF_TICKS       = 200;

    private final Map<GoalBinding<T, S, C>, Integer> failureCounts = new IdentityHashMap<>();
    private final Map<GoalBinding<T, S, C>, Integer> backoffUntil  = new IdentityHashMap<>();

    /**
     * Records one failure for the given binding and applies backoff or permanent
     * disable if the failure count crosses a threshold.
     */
    void recordFailure(GoalBinding<T, S, C> binding, int currentTick) {
        var count = failureCounts.merge(binding, 1, Integer::sum);

        if (count >= PERMANENT_THRESHOLD) {
            if (DebugConfig.isEnabled()) {
                LOGGER.error("[deci-lib][DEBUG] Circuit breaker: goal {} has failed {} times"
                        + " — permanently disabled for this session.",
                        binding.goal().getClass().getSimpleName(), count);
            }
        } else if (count >= FAILURE_THRESHOLD) {
            var until = currentTick + BACKOFF_TICKS;
            backoffUntil.put(binding, until);
            if (DebugConfig.isEnabled()) {
                LOGGER.warn("[deci-lib][DEBUG] Circuit breaker: goal {} has failed {} times"
                        + " — backing off for {} ticks (until tick {}).",
                        binding.goal().getClass().getSimpleName(), count, BACKOFF_TICKS, until);
            }
        }
    }
    boolean isBlocked(GoalBinding<T, S, C> binding, int currentTick) {
        var count = failureCounts.getOrDefault(binding, 0);
        if (count >= PERMANENT_THRESHOLD) return true;
        return backoffUntil.getOrDefault(binding, 0) > currentTick;
    }

    /** Clears all failure state. Call if the brain is re-initialized. */
    void clear() {
        failureCounts.clear();
        backoffUntil.clear();
    }
}
