package com.cleannrooster.decilib.ai.stimulus;

@SuppressWarnings("unchecked")
public abstract class AIStimulusBuilder<B extends AIStimulusBuilder<B, S>, S extends AIStimulus> {

    protected double targetDistance = Double.MAX_VALUE;
    protected boolean targetInMeleeRange = false;
    protected boolean targetInEngagementRange = false;
    protected boolean hasLineOfSight = false;
    protected float selfHealthPct = 1.0f;
    protected float targetHealthPct = Float.NaN;
    protected boolean targetIsBlocking = false;
    protected int ticksSinceLastHit = Integer.MAX_VALUE;
    protected int ticksSinceLastAttack = Integer.MAX_VALUE;
    protected boolean hasTarget = false;
    protected boolean selfIsOnGround = true;
    protected boolean selfIsInFluid = false;
    protected boolean selfIsInWater = false;
    protected boolean selfIsInLava  = false;
    protected boolean selfIsHidden          = false;
    protected float   recentDamageTaken     = 0f;
    protected boolean lastHitWasProjectile  = false;

    // AiProfile axes
    protected boolean isNearAnchor      = false;
    protected int     nearbyAllyCount   = 0;
    protected float   fightProgressPct  = 0f;
    protected int     nearbyThreatCount = 0;

    protected final B self() {
        return (B) this;
    }

    public B targetDistance(double v)          { this.targetDistance = v;          return self(); }
    public B targetInMeleeRange(boolean v)     { this.targetInMeleeRange = v;      return self(); }
    public B targetInEngagementRange(boolean v){ this.targetInEngagementRange = v; return self(); }
    public B hasLineOfSight(boolean v)         { this.hasLineOfSight = v;          return self(); }
    public B selfHealthPct(float v)            { this.selfHealthPct = v;           return self(); }
    public B targetHealthPct(float v)          { this.targetHealthPct = v;         return self(); }
    public B targetIsBlocking(boolean v)       { this.targetIsBlocking = v;        return self(); }
    public B ticksSinceLastHit(int v)          { this.ticksSinceLastHit = v;       return self(); }
    public B ticksSinceLastAttack(int v)       { this.ticksSinceLastAttack = v;    return self(); }
    public B hasTarget(boolean v)              { this.hasTarget = v;               return self(); }
    public B selfIsOnGround(boolean v)         { this.selfIsOnGround = v;          return self(); }
    public B selfIsInFluid(boolean v)          { this.selfIsInFluid = v;           return self(); }
    public B selfIsInWater(boolean v)          { this.selfIsInWater = v;           return self(); }
    public B selfIsInLava(boolean v)           { this.selfIsInLava  = v;           return self(); }
    public B selfIsHidden(boolean v)              { this.selfIsHidden         = v; return self(); }
    public B recentDamageTaken(float v)           { this.recentDamageTaken    = v; return self(); }
    public B lastHitWasProjectile(boolean v)      { this.lastHitWasProjectile = v; return self(); }
    public B isNearAnchor(boolean v)              { this.isNearAnchor      = v; return self(); }
    public B nearbyAllyCount(int v)               { this.nearbyAllyCount   = v; return self(); }
    public B fightProgressPct(float v)            { this.fightProgressPct  = v; return self(); }
    public B nearbyThreatCount(int v)             { this.nearbyThreatCount = v; return self(); }

    public abstract S build();
}
