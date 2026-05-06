package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.CanBrace;
import com.cleannrooster.decilib.ai.CanBulwark;
import com.cleannrooster.decilib.ai.ambush.CanAmbush;
import com.cleannrooster.decilib.ai.brain.BrainGoalWrapper;
import com.cleannrooster.decilib.ai.brain.MobBrain;
import com.cleannrooster.decilib.builder.MobDefinition;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.archetype.BaseStats;
import com.cleannrooster.decilib.builder.sound.SoundConfig;
import com.cleannrooster.decilib.builder.sound.SoundEntry;
import com.cleannrooster.decilib.builder.visual.LoopAnimTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


public class DataDrivenMob extends HostileEntity implements CanAmbush, CanBrace, CanBulwark {

    // Carries the MobDefinition across the super() call boundary so that
    // initGoals() can read it before the constructor body runs.
    private static final ThreadLocal<MobDefinition> PENDING_DEF = new ThreadLocal<>();

    private final MobDefinition  definition;
    private DataDrivenBrain      brain;
    private final LoopAnimTracker loopAnimTracker = new LoopAnimTracker();

    private boolean hidden      = false;
    private int     emergeTicks = 0;

    // Saturate at Integer.MAX_VALUE / 2 to avoid overflow when incrementing each tick.
    private int ticksSinceLastHit    = Integer.MAX_VALUE / 2;
    private int ticksSinceLastAttack = Integer.MAX_VALUE / 2;

    // CanBrace
    private boolean bracing              = false;
    private float   braceReduction       = 0f;
    private boolean lastHitWasProjectile = false;
    private float   recentDamageTaken    = 0f;
    private static final float RECENT_DAMAGE_DECAY = 0.5f;

    // CanBulwark
    private boolean bulwarkActive  = false;
    private float   bulwarkReflect = 0f;

    @Override
    public float getPathfindingPenalty(PathNodeType nodeType) {
        if (nodeType == PathNodeType.WATER || nodeType == PathNodeType.LAVA) {
            return 48.0f;
        }
        return super.getPathfindingPenalty(nodeType);
    }

    @Override
    public float getScale() {
        return super.getScale();
    }

    @Override
    public void onAttacking(Entity target) {
        this.swingHand(Hand.MAIN_HAND);
        SoundConfig sc = definition.soundConfig();
        if (sc != null && sc.attack() != null && getWorld() instanceof ServerWorld sw) {
            playConfiguredSound(sw, sc.attack());
        }
        super.onAttacking(target);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        SoundConfig sc = definition.soundConfig();
        if (sc != null && sc.idle() != null) return SoundEvent.of(Identifier.of(sc.idle().soundId()));
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        SoundConfig sc = definition.soundConfig();
        if (sc != null && sc.hurt() != null) return SoundEvent.of(Identifier.of(sc.hurt().soundId()));
        return super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundConfig sc = definition.soundConfig();
        if (sc != null && sc.death() != null) return SoundEvent.of(Identifier.of(sc.death().soundId()));
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundConfig sc = definition.soundConfig();
        if (sc != null && sc.step() != null) {
            var e = sc.step();
            playSound(SoundEvent.of(Identifier.of(e.soundId())), e.volume(), e.pitch());
            return;
        }
        super.playStepSound(pos, state);
    }

    private void playConfiguredSound(ServerWorld world, SoundEntry entry) {
        world.playSound(null, getBlockPos(),
                SoundEvent.of(Identifier.of(entry.soundId())),
                SoundCategory.HOSTILE, entry.volume(), entry.pitch());
    }

    @Override
    public float getHandSwingProgress(float tickDelta) {
        return super.getHandSwingProgress(tickDelta);
    }

    public DataDrivenMob(EntityType<? extends DataDrivenMob> entityType,
                         World world,
                         MobDefinition definition) {
        super(capture(entityType, definition), world);
        PENDING_DEF.remove();

        this.definition = definition;

        BaseStats stats = definition.stats();
        applyAttribute(EntityAttributes.GENERIC_MAX_HEALTH,           stats.maxHealth());
        applyAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE,        stats.attackDamage());
        applyAttribute(EntityAttributes.GENERIC_MOVEMENT_SPEED,       stats.movementSpeed());
        applyAttribute(EntityAttributes.GENERIC_FOLLOW_RANGE,         stats.followRange());
        applyAttribute(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, stats.knockbackResistance());
        this.setHealth(this.getMaxHealth());
    }

    private static EntityType<? extends DataDrivenMob> capture(
            EntityType<? extends DataDrivenMob> type, MobDefinition def) {
        PENDING_DEF.set(def);
        return type;
    }


    public static DefaultAttributeContainer.Builder createDataDrivenAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH,           200.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,        30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,       0.5)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE,         40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        // Called by MobEntity's constructor before our constructor body runs.
        // Read the definition from the ThreadLocal set by capture() above.
        var def = PENDING_DEF.get();
        this.brain = new DataDrivenBrain(def);
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new BrainGoalWrapper<>(brain, this) {});
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(2, new RevengeGoal(this));
    }


    @Override
    public void enterHiddenState() {
        hidden = true;
        setInvisible(true);
    }

    @Override
    public void exitHiddenState() {
        hidden = false;
        setInvisible(false);
        emergeTicks = 15;
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        if (definition.initialState() == MobState.HIDDEN) {
            enterHiddenState();
        }
    }


    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
                                 SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        if (definition.initialState() == MobState.HIDDEN) {
            enterHiddenState();
        }
        return data;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Bulwark: block (and optionally reflect) incoming projectile damage
        if (bulwarkActive && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            if (bulwarkReflect > 0 && getWorld() instanceof ServerWorld sw
                    && source.getAttacker() instanceof LivingEntity attacker) {
                attacker.damage(sw.getDamageSources().thorns(this), amount * bulwarkReflect);
            }
            return false;
        }
        // Brace: reduce incoming damage
        if (bracing && amount > 0) {
            amount *= (1f - braceReduction);
        }
        var result = super.damage(source, amount);
        if (result) {
            ticksSinceLastHit    = 0;
            lastHitWasProjectile = source.isIn(DamageTypeTags.IS_PROJECTILE);
            recentDamageTaken   += amount;
        }
        return result;
    }

    // ── CanBrace ──────────────────────────────────────────────────────────────

    @Override public void enterBraceState(float reduction) { bracing = true; braceReduction = reduction; }
    @Override public void exitBraceState()                 { bracing = false; braceReduction = 0f; }
    @Override public boolean isBracing()                   { return bracing; }
    @Override public float   getBraceReduction()           { return braceReduction; }
    @Override public boolean wasLastHitProjectile()        { return lastHitWasProjectile; }
    @Override public float   getRecentDamageTaken()        { return recentDamageTaken; }

    // ── CanBulwark ────────────────────────────────────────────────────────────

    @Override public void    enterBulwark(float reflectCoeff) { bulwarkActive = true; bulwarkReflect = reflectCoeff; }
    @Override public void    exitBulwark()                    { bulwarkActive = false; bulwarkReflect = 0f; }
    @Override public boolean isBulwarkActive()                { return bulwarkActive; }
    @Override public float   getBulwarkReflectCoeff()         { return bulwarkReflect; }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        var result = super.tryAttack(target);
        if (result) ticksSinceLastAttack = 0;
        return result;
    }

    @Override
    public void tick() {
        if (!getWorld().isClient()) loopAnimTracker.tick(this);
        super.tick();
        if (emergeTicks > 0) emergeTicks--;
        if (ticksSinceLastHit    < Integer.MAX_VALUE / 2) ticksSinceLastHit++;
        if (ticksSinceLastAttack < Integer.MAX_VALUE / 2) ticksSinceLastAttack++;
        if (recentDamageTaken > 0) recentDamageTaken = Math.max(0f, recentDamageTaken - RECENT_DAMAGE_DECAY);
    }

    public boolean isEmerging()              { return emergeTicks > 0; }
    public float   getEmergeProgress()       { return emergeTicks / 15.0f; }
    public int     getTicksSinceLastHit()    { return ticksSinceLastHit; }
    public int     getTicksSinceLastAttack() { return ticksSinceLastAttack; }

    @Override
    public boolean isHidden() {
        return hidden;
    }


    @Override
    public @Nullable Vec3d getSurfacePosition() {
        var target = getTarget();
        if (target == null) return null;
        return target.getPos().subtract(target.getRotationVector().multiply(3.0));
    }



    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("DeciHidden", hidden);
        nbt.putInt("DeciEmergeTicks", emergeTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        emergeTicks = nbt.getInt("DeciEmergeTicks");
        if (nbt.getBoolean("DeciHidden")) {
            enterHiddenState();
        }
    }

    public MobDefinition getDefinition()             { return definition; }

    public MobBrain<DataDrivenMob> getMobBrain()     { return brain; }

    public LoopAnimTracker getLoopAnimTracker()      { return loopAnimTracker; }

    // -------------------------------------------------------------------------

    private void applyAttribute(RegistryEntry<EntityAttribute> attribute, double value) {
        var instance = getAttributeInstance(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
}
