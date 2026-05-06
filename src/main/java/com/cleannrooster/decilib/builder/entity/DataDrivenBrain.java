package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.brain.AbstractMobBrain;
import com.cleannrooster.decilib.ai.goal.GoalBinding;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.statemachine.DefaultStateMachine;
import com.cleannrooster.decilib.ai.statemachine.Phase;
import com.cleannrooster.decilib.ai.statemachine.StanceTransition;
import com.cleannrooster.decilib.ai.statemachine.StateTransition;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.stimulus.BaseAIStimulusBuilder;
import com.cleannrooster.decilib.builder.MobDefinition;
import com.cleannrooster.decilib.builder.MobStance;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;


final class DataDrivenBrain
        extends AbstractMobBrain<DataDrivenMob, MobStance, MobState> {

    static final Phase NORMAL = new Phase("normal", 0);

    private static final double ALLY_SCAN_RADIUS   = 12.0;
    private static final double THREAT_SCAN_RADIUS = 8.0;

    DataDrivenBrain(MobDefinition def) {
        super(new DefaultStateMachine<>(def.initialStance(), def.initialState()), NORMAL);

        var composer = def.composer();

        // Register all ability IDs declared by goals so strict-mode validation
        // can detect typos. Goals start with cooldown 0 (immediately ready).
        for (BehaviorComposer.GoalEntry entry : composer.goals()) {
            for (String id : entry.goal().declaredAbilityIds()) {
                cooldowns.register(id, 0);
            }
        }

        // State transitions
        for (BehaviorComposer.TransitionEntry te : composer.transitions()) {
            stateMachine.registerTransition(new StateTransition<>(
                    te.from(), te.condition(), te.to(), te.stanceFilter(), te.priority()
            ));
        }

        // Stance transitions
        for (BehaviorComposer.StanceTransitionEntry ste : composer.stanceTransitions()) {
            stateMachine.registerStanceTransition(new StanceTransition<>(
                    ste.from(), ste.condition(), ste.to(), ste.priority()
            ));
        }

        for (BehaviorComposer.GoalEntry entry : composer.goals()) {
            @SuppressWarnings("unchecked")
            MobBrainGoal<DataDrivenMob> typedGoal = (MobBrainGoal<DataDrivenMob>) entry.goal();
            addGoalBinding(new GoalBinding<>(typedGoal, entry.stances(), entry.states(), entry.priority()));
        }
    }

    @Override
    protected AIStimulus buildStimulus(DataDrivenMob entity,
                                       ServerWorld world,
                                       BaseAIStimulusBuilder builder) {
        var meleeRange = (float) entity.getDefinition().stats().meleeRange();

        builder.selfHealthPct(entity.getHealth() / entity.getMaxHealth())
               .selfIsOnGround(entity.isOnGround())
               .selfIsInFluid(entity.isTouchingWater() || entity.isInLava())
               .selfIsInWater(entity.isTouchingWater())
               .selfIsInLava(entity.isInLava())
               .selfIsHidden(entity.isHidden())
               .ticksSinceLastHit(entity.getTicksSinceLastHit())
               .ticksSinceLastAttack(entity.getTicksSinceLastAttack())
               .recentDamageTaken(entity.getRecentDamageTaken())
               .lastHitWasProjectile(entity.wasLastHitProjectile());

        LivingEntity target = entity.getTarget();
        if (target != null && target.isAlive()) {
            var dist = entity.distanceTo(target);
            builder.hasTarget(true)
                   .targetDistance(dist)
                   .targetInMeleeRange(dist <= meleeRange)
                   .targetInEngagementRange(dist <= 16.0f)
                   .hasLineOfSight(entity.canSee(target))
                   .targetHealthPct(target.getHealth() / target.getMaxHealth())
                   .targetIsBlocking(target.isBlocking());

            // nearbyThreatCount: hostile entities near the target that are also targeting us
            int threats = world.getEntitiesByClass(HostileEntity.class,
                    target.getBoundingBox().expand(THREAT_SCAN_RADIUS),
                    e -> e != entity && e.isAlive() && e.getTarget() == entity).size();
            builder.nearbyThreatCount(threats);
        }

        // isNearAnchor: within follow range of spawn anchor
        BlockPos anchor = entity.getAnchorPos();
        double followRange = entity.getDefinition().stats().followRange();
        boolean nearAnchor = anchor == null
                || entity.squaredDistanceTo(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5)
                   <= followRange * followRange;
        builder.isNearAnchor(nearAnchor);

        // fightProgressPct: totalDamageTaken / maxHealth, capped at 1.0
        builder.fightProgressPct(Math.min(1f, entity.getTotalDamageTaken() / entity.getMaxHealth()));

        // nearbyAllyCount: allied DataDrivenMob within ALLY_SCAN_RADIUS blocks
        int allyCount = world.getEntitiesByClass(DataDrivenMob.class,
                entity.getBoundingBox().expand(ALLY_SCAN_RADIUS),
                e -> e != entity && e.isAlive()).size();
        builder.nearbyAllyCount(allyCount);

        return builder.build();
    }
}
