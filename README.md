# deci-lib — Mob JSON Guide


WARNING: This guide's generation was assisted heavily by AI! It was heavily scrutinized, but might contain inaccuracies! Consult with me (Forg/Cleannrooster) directly if you are unsure.

This guide covers every field you can put in a mob data file under `data/<modid>/mobs/<id>.json`.

---

## Top-level structure

```json
{
  "id": "my_mob",
  "archetype": "bruiser",
  "theme": "beast",
  "form": "azurelib",
  "render": { ... },
  "scale": { ... },
  "tuning": { ... },
  "attributes": { ... },
  "sounds": { ... },
  "features": [ ... ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | yes | Unique mob identifier. Must match filename. |
| `archetype` | string | yes | Combat role. See [Archetypes](#archetypes). |
| `theme` | string | no | Cosmetic tag used by render/particle systems. |
| `form` | string | yes | Body shape / animation backend. See [Forms](#forms). |
| `render` | object | yes | Rendering config. See [Render](#render). |
| `scale` | object | no | Hitbox size. |
| `tuning` | object | no | Band-based stat tuning. |
| `attributes` | object | no | Explicit attribute overrides. |
| `sounds` | object | no | Sound events. |
| `features` | array | yes | Behavior list. See [Features](#features). |

---

## Archetypes

Archetypes wire up the high-level state machine. Pick one. Each archetype has a default AI profile (see [AI Profiles](#ai-profiles)); individual axes can be overridden per mob.

### `bruiser`
A heavy frontliner. Starts in `APPROACHING`, transitions to `ATTACKING_MELEE` when the target enters melee range, and returns to `APPROACHING` when out of range. Suitable for tanky melee fighters with charge abilities.

**Default AI profile:** `RELENTLESS / ANYWHERE / STATIC / SOLO_PREDATOR`

**Retreat behavior (when `aggression` is `CALCULATING`):** Enters `RETREATING` when health is more than 20% below the target's. Moves directly away to establish a fixed safety gap (~10 blocks) rather than fleeing to max range. Exits retreat when health recovers, LOS to the target is broken, or 4 seconds pass without taking a hit.

**Retreat behavior (when `aggression` is `OPPORTUNIST`):** Same entry condition. Actively seeks cover positions (raycasts candidate points to find obstacles that break LOS) rather than running in a straight line. Exits retreat only when LOS is broken — no time-based fallback.

### `skirmisher`
A mobile fighter. Has `APPROACHING` and `CHARGING` states wired up for dash/charge combos. Supports `ATTACKING_MELEE` for basic melee. Good for fast aggressive mobs. Also supports `FLEEING` (hard flee below 30% health regardless of aggression axis) and `RETREATING` (tactical retreat driven by the aggression model).

**Default AI profile:** `CALCULATING / ANYWHERE / STATIC / SOLO_PREDATOR`

**Retreat behavior (when `aggression` is `CALCULATING`):** Identical to bruiser — distance-establishing retreat, exits on health recovery, LOS break, or 4 s without being hit.

**Retreat behavior (when `aggression` is `OPPORTUNIST`):** Identical to bruiser OPPORTUNIST — cover-seeking retreat, exits only on LOS break.

### `ambusher`
A stealth attacker. Uses `HIDDEN → ACTIVE → REHIDING` state cycle. **Never enters `APPROACHING` or `ATTACKING_MELEE`** — attacks must be assigned to the `ACTIVE` state. Use `ambush` and `ambush_attack` features. Pair `melee` / `sweep` / `charge` features with `"state": "ACTIVE"`.

**Default AI profile:** `OPPORTUNIST / PREDATORY / STATIC / SOLO_PREDATOR`

---

## AI Profiles

An AI profile is a set of four independent decision axes that gate when a mob commits to offensive actions and how it behaves under pressure. Each axis evaluates the current combat situation and contributes a pass/fail vote; all four must pass for the combined offensive gate to open.

Profiles are set per-archetype and can be overridden per mob definition.

---

### Aggression axis

Controls *when* the mob commits to attacking and *how* it retreats when conditions are unfavorable.

| Value | Offensive gate | Retreat behavior | Last stand |
|---|---|---|---|
| `RELENTLESS` | Always commits. Never retreats. | None. | Yes — enters `LAST_STAND` on near-death or burst hit (unless crushed or surrounded). |
| `CALCULATING` | Commits when not at a >10% health disadvantage vs. target. | Enters `RETREATING` at >20% disadvantage. Moves directly away. Exits when health recovers, LOS breaks, or 4 s without being hit. | Yes — same conditions as RELENTLESS. |
| `OPPORTUNIST` | Commits only when target is below 50% health. | Enters `RETREATING` at >20% health disadvantage. Actively steers toward cover. Exits **only** when LOS is broken. | **Never.** Only fights on favourable odds; being cornered is definitionally unfavourable. |

**CALCULATING** is the tactician: it retreats when outmatched but commits again as soon as it is safe, regardless of whether it has fully recovered. The time-based exit (`ticksSinceLastHit > 80`) means a mob that successfully creates distance will re-engage even if the health balance has not shifted.

**OPPORTUNIST** is the stalker: it will not leave cover until it cannot be seen. It only attacks weakened targets, so retreat is a deliberate repositioning to find a blind spot, not a panic run.

### Last-stand conditions

`LAST_STAND` is a dedicated combat state entered when the mob faces its final moments but still has will to fight. All offensive features fire unconditionally from this state.

**Triggers** (any one activates):
- Near death: `selfHealthPct < 0.15`
- Burst hit: recent damage spike ≥ 20% of max health in the decay window

**Blockers** (any one suppresses, checked after triggers):
- `OPPORTUNIST` aggression — always blocked; a stalker never fights on cornered terms
- `COWARD` target-eval with any nearby threat — coward folds rather than fighting
- Crushed/hopeless: `fightProgressPct > 0.75` AND target health `> 0.65` — the mob has been slowly ground down against a dominant opponent and loses hope. A mob ambushed quickly to low health (low `fightProgressPct`) still has will to fight; one attritioned over a long fight does not.
- Hopelessly surrounded: `nearbyThreatCount >= 3`

---

### Spatial axis

Controls *where* the mob is willing to fight.

| Value | Behavior |
|---|---|
| `ANYWHERE` | No spatial restrictions. Attacks from any position. |
| `TERRITORIAL` | Only commits offensively when near its spawn anchor (within follow range). |
| `PREDATORY` | Prefers striking when the target is cornered near an obstacle. *(Phase 1: always passes — real raycasting implementation pending.)* |
| `SWARMER` | Only commits when at least 2 allied mobs are nearby (within ~12 blocks). |

---

### Adaptation axis

Controls how the mob's offensive behavior evolves *over the course of a fight*.

| Value | Behavior |
|---|---|
| `STATIC` | Behavior is identical throughout the fight. |
| `ESCALATING` | Offensive features are locked until the mob has taken ≥30% of its max health as cumulative damage. Models a mob that starts restrained and escalates after absorbing enough punishment. |
| `LEARNING` | Biases toward abilities that are landing and suppresses those being avoided. *(Phase 1 stub: always passes. Real implementation requires per-ability hit tracking — see [LEARNING implementation notes](#learning-implementation-notes).)* |

---

### Target evaluation axis

Controls *which targets* the mob prioritizes.

| Value | Behavior |
|---|---|
| `SOLO_PREDATOR` | Full aggression toward a single target. Ignores others. |
| `PACK_HUNTER` | Deprioritizes its primary target when allied mobs are under attack. *(Phase 1 stub: always passes — needs ally damage event tracking.)* |
| `COWARD` | Suppresses offensive actions when the target has companions nearby (within ~8 blocks). Only commits when facing a lone target. |

---

### LEARNING implementation notes

`LEARNING` is the next major adaptation axis to implement. Key design constraints noted during the CALCULATING/OPPORTUNIST retreat pass:

- **The combined gate is too coarse for per-ability learning.** `AdaptationModel.offensiveGate()` gates all offensive behavior at once. True LEARNING — biasing toward abilities with high hit rates and suppressing those being avoided — requires per-ability gating at the `GoalBinding` level, not via the combined gate.
- **`declaredAbilityIds()` is the correct key.** Every `MobBrainGoal` already declares its ability IDs. These are the right identifiers for associating hit tracking data with individual behaviors.
- **Hit tracking needs an explicit recording hook.** `ticksSinceLastHit` (already in stimulus) is a useful aggregate proxy, but per-ability tracking needs something like a `recordAbilityHit(String abilityId)` call that fires when an ability damages the target. This is not yet present; it needs a damage event callback at the goal or entity level.
- **Storage belongs on the entity, not the stimulus.** A `Map<String, Float>` (abilityId → rolling hit rate) stored on `DataDrivenMob` is the right home. `DataDrivenBrain.buildStimulus()` can read it if a summary field is ever added to `AIStimulus`, but the raw map should stay on the entity to avoid polluting the stimulus interface with ability-specific data.
- **Do not add per-ability data to `AIStimulus` yet.** Keep the interface clean until the shape of LEARNING is clear. The gate implementation may not need stimulus at all — it may operate directly on the entity reference passed to `GoalBinding`.

---

## Forms

`form` determines the hitbox shape and which animation backend is used.

| Value | Description |
|---|---|
| `AGILE_BIPED` | Slim humanoid |
| `ARMORED_BIPED` | Bulkier humanoid |
| `ARCANE_BIPED` | Caster-style humanoid |
| `STANDARD_BIPED` | Default humanoid |
| `HEAVY_BIPED` | Large slow humanoid |
| `ALIEN_BIPED` | Non-standard upright form |
| `HEAVY_QUADRUPED` | Large four-legged |
| `STANDARD_QUADRUPED` | Default four-legged |
| `SPECTRAL_QUADRUPED` | Ghostly four-legged |
| `STALKER_QUADRUPED` | Low crouching four-legged |
| `SWIFT_QUADRUPED` | Fast four-legged |
| `SMALL_QUADRUPED` | Small four-legged |
| `AZURELIB` | Use when `render.backend` is `"azurelib"` |

---

## Render

```json
"render": {
  "backend": "azurelib",
  "assets": {
    "geo":       "mymod:geo/my_mob.geo.json",
    "animation": "mymod:animations/my_mob.animations.json",
    "texture":   "mymod:textures/entity/my_mob.png"
  },
  "loops": {
    "idle":            "animation.mob.idle",
    "idle_hostile":    "animation.mob.idle_alert",
    "moving":          "animation.mob.walk",
    "moving_hostile":  "animation.mob.walk_alert",
    "running":         "animation.mob.run",
    "aiming":          "animation.mob.aim"
  }
}
```

| Field | Description |
|---|---|
| `backend` | Must be `"azurelib"` when using AzureLib models. |
| `assets.geo` | Resource location of the `.geo.json` model file. |
| `assets.animation` | Resource location of the `.animations.json` file. |
| `assets.texture` | Resource location of the texture. |
| `loops.idle` | **Required.** Animation played while idle and not hostile. |
| `loops.idle_hostile` | Animation while idle and in combat. Falls back to `idle`. |
| `loops.moving` | Animation while walking and not hostile. |
| `loops.moving_hostile` | Animation while walking in combat. |
| `loops.running` | Animation while running (e.g. during a charge). |
| `loops.aiming` | Animation while aiming a ranged attack. |

All loop animation names are the animation IDs exactly as they appear in your `.animations.json` file.

---

## Scale

```json
"scale": {
  "width": 0.6,
  "height": 1.8
}
```

Sets the mob's hitbox. Defaults: `width` = 0.6, `height` = 1.8.

---

## Tuning

Tuning bands give relative stat presets without setting exact numbers.

```json
"tuning": {
  "health":    "MEDIUM",
  "damage":    "HIGH",
  "speed":     "LOW",
  "detection": "MEDIUM"
}
```

Valid values for each band: `NONE`, `LOW`, `MEDIUM`, `HIGH`. All default to `MEDIUM`.

| Band | Affects |
|---|---|
| `health` | Base max health if not overridden in `attributes` |
| `damage` | Base attack damage if not overridden |
| `speed` | Movement speed; also affects idle wander speed |
| `detection` | Follow range / detection radius |

Explicit `attributes` values always take priority over tuning bands.

---

## Attributes

Override exact attribute values. All fields are optional.

```json
"attributes": {
  "max_health": 40.0,
  "attack_damage": 7.0,
  "movement_speed": 0.3,
  "follow_range": 20.0,
  "knockback_resistance": 0.5,
  "melee_range": 3.5
}
```

---

## Sounds

All sound fields are optional. Sounds can be a bare string (sound event ID) or an object:

```json
"sounds": {
  "idle":   "mymod:entity.my_mob.idle",
  "hurt":   { "id": "mymod:entity.my_mob.hurt", "volume": 1.0, "pitch": 1.0 },
  "death":  "mymod:entity.my_mob.death",
  "step":   "mymod:entity.my_mob.step",
  "attack": "mymod:entity.my_mob.attack"
}
```

---

## Features

`features` is a list of behavior objects. Each object has `"type"` which selects the feature, plus additional fields specific to that feature.

### Animations block

Many features accept an `"animations"` object with lifecycle hooks. The keys match the lifecycle event:

```json
"animations": {
  "on_windup":   "animation.mob.windup",
  "on_release":  "animation.mob.release",
  "on_action":   "animation.mob.strike",
  "on_complete": "animation.mob.recover",
  "on_cancel":   "animation.mob.flinch"
}
```

All animation keys are optional. Values are animation IDs from your `.animations.json` file.

### Particle styles

Particle effect fields accept one of these string values:

| Value | Effect |
|---|---|
| `none` | No particles |
| `smoke_ring` | Expanding ring of smoke |
| `portal_ring` | Portal-colored ring |
| `portal_pulse` | Portal pulse burst |
| `soul_ring` | Soul-fire ring |
| `sweep_arc` | Sword sweep arc |
| `crit_arc` | Critical hit arc |
| `poof_burst` | Poof burst |
| `enchant_ring` | Enchantment particle ring |
| `falling_iron` | Falling iron particles |
| `ember_trail` | Ember trail |
| `dust_trail` | Dust trail |
| `splash_burst` | Water splash burst |
| `smoke_burst` | Dense smoke burst |

### States

State names used in feature `"state"` fields:

| Value | Meaning |
|---|---|
| `IDLE` | Not in combat, wandering/standing |
| `WANDERING` | Actively walking to a wander point |
| `APPROACHING` | In combat, moving toward target |
| `HIDDEN` | Ambusher state: submerged/hidden |
| `ACTIVE` | Ambusher state: surfaced and attacking |
| `REHIDING` | Ambusher state: returning to hide |
| `ATTACKING_MELEE` | In melee range, executing basic attack |
| `CHARGING` | Mid-charge ability |
| `RETREATING` | Pulling back after an attack |
| `LAST_STAND` | Desperate final push — mob charges unconditionally when near death |

---

## Feature Reference

---

### `idle`

Controls what the mob does when not in combat.

```json
{
  "type": "idle",
  "mode": "wander",
  "wander_range": 8.0,
  "pause_ticks": 40,
  "scan_interval_ticks": 30,
  "movement_speed": 0.6,
  "priority": 2
}
```

| Field | Default | Description |
|---|---|---|
| `mode` | `"still"` | `"still"` — stands in place; `"wander"` — roams randomly; `"mixed"` — both behaviors alternate. |
| `wander_range` | `8.0` | Maximum distance from origin the mob will wander. Required for `wander` / `mixed`. |
| `pause_ticks` | `40` | Ticks to wait between wander targets. Clamped to minimum 10. |
| `scan_interval_ticks` | `0` | How often (in ticks) to scan for nearby players. `0` disables scanning. |
| `movement_speed` | `0.6` | Walk speed during idle wander. |
| `priority` | `2` | Goal priority. |

**Archetype note:** Ambusher with `wander_range > 6.0` will warn — consider `"still"` or a smaller range.

---

### `melee`

Basic melee attack. Adds a state transition into `ATTACKING_MELEE` when the target enters melee range.

```json
{
  "type": "melee",
  "ability_id": "melee",
  "cooldown_ticks": 20,
  "state": "ATTACKING_MELEE",
  "priority": 10,
  "animations": {
    "on_action": "animation.mob.punch"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"melee"` | Cooldown registry key. |
| `cooldown_ticks` | `20` | Ticks between attacks. Must be >= 1. |
| `state` | `"ATTACKING_MELEE"` | State this attack runs in. |
| `priority` | `10` | Goal priority. |
| `animations` | — | Supports `on_action`. |

**Archetype note:** Ambusher never enters `ATTACKING_MELEE`. Use `"state": "ACTIVE"`.

---

### `sweep`

Arc-shaped melee attack that hits all entities within a cone.

```json
{
  "type": "sweep",
  "ability_id": "sweep",
  "cooldown_ticks": 40,
  "min_cooldown": 10,
  "half_angle_deg": 45.0,
  "range": 3.5,
  "coeff": 0.8,
  "state": "ATTACKING_MELEE",
  "priority": 15,
  "on_complete_particles": "sweep_arc",
  "animations": {
    "on_action": "animation.mob.sweep"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"sweep"` | Cooldown registry key. |
| `cooldown_ticks` | `40` | Max cooldown ticks. |
| `min_cooldown` | `10` | Minimum cooldown ticks (for randomization). |
| `half_angle_deg` | `45.0` | Half-angle of the attack cone in degrees. Must be in (0, 180). |
| `range` | `3.5` | Reach of the sweep in blocks. |
| `coeff` | `0.8` | Damage multiplier applied to `attack_damage`. |
| `state` | `"ATTACKING_MELEE"` | State this runs in. |
| `priority` | `15` | Goal priority. |
| `on_complete_particles` | `"sweep_arc"` | Particles on completion. |
| `animations` | — | Supports `on_action`. |

**Archetype note:** Ambusher never enters `ATTACKING_MELEE`. Use `"state": "ACTIVE"`.

---

### `shockwave`

Radial AoE attack that damages all entities within a radius.

```json
{
  "type": "shockwave",
  "ability_id": "shockwave",
  "cooldown_ticks": 80,
  "min_cooldown": 20,
  "range": 4.0,
  "coeff": 1.0,
  "state": "ATTACKING_MELEE",
  "priority": 20,
  "on_start_particles": "none",
  "on_complete_particles": "smoke_ring",
  "animations": {
    "on_action": "animation.mob.slam"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"shockwave"` | Cooldown registry key. |
| `cooldown_ticks` | `80` | Max cooldown ticks. |
| `min_cooldown` | `20` | Minimum cooldown ticks. |
| `range` | `4.0` | Radius in blocks. |
| `coeff` | `1.0` | Damage multiplier applied to `attack_damage`. |
| `state` | `"ATTACKING_MELEE"` | State this runs in. |
| `priority` | `20` | Goal priority. |
| `on_start_particles` | `"none"` | Particles on start. |
| `on_complete_particles` | `"smoke_ring"` | Particles on completion. |
| `animations` | — | Supports `on_action`, `on_complete`. |

**Archetype note:** Ambusher never enters `ATTACKING_MELEE`. Use `"state": "ACTIVE"`.

---

### `charge`

A windup + dash + area release attack. The mob lunges toward its target during windup, then deals damage in an arc or radial burst. Automatically manages transitions: `APPROACHING → CHARGING → APPROACHING`.

```json
{
  "type": "charge",
  "ability_id": "ground_slam",
  "cooldown_ticks": 60,
  "windup_ticks": 8,
  "release_delay": 20,
  "release_type": "arc",
  "half_angle_deg": 50.0,
  "range": 4.5,
  "coeff": 1.2,
  "charge_speed": 2.0,
  "upwards_speed": 0.05,
  "state": "CHARGING",
  "priority": 100,
  "on_start_particles": "none",
  "on_complete_particles": "smoke_ring",
  "animations": {
    "on_windup": "animation.mob.charge",
    "on_release": "animation.mob.slam"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"charge"` | Cooldown registry key. |
| `cooldown_ticks` | `60` | Cooldown after use. |
| `windup_ticks` | `8` | Duration of the windup phase. Must be >= 1. |
| `release_delay` | `20` | Ticks after windup before damage is applied. |
| `release_type` | `"arc"` | `"arc"` — cone damage in front; `"radial"` — full 360° burst. |
| `half_angle_deg` | `50.0` | Half-angle of the arc cone (only used when `release_type` is `"arc"`). |
| `range` | `4.5` | Reach of the damage in blocks. Must be > 0. |
| `coeff` | `1.2` | Damage multiplier applied to `attack_damage`. |
| `charge_speed` | `2.0` | Horizontal velocity during windup lunge. |
| `upwards_speed` | `0.05` | Vertical velocity added during windup (for a slight hop). |
| `state` | `"CHARGING"` | State the goal runs in. |
| `priority` | `100` | Goal priority. |
| `on_start_particles` | `"none"` | Particles on ability start. |
| `on_complete_particles` | `"smoke_ring"` | Particles on completion. |
| `animations` | — | Supports `on_windup`, `on_release`, `on_complete`, `on_cancel`. |

**Archetype note:** Ambusher never enters `APPROACHING`, so the built-in transition never fires. Use `"state": "ACTIVE"`.

---

### `gapclose`

A leap or teleport that closes distance to the target when they are far away. The mob jumps toward the player; optionally deals radial landing damage. Automatically manages transitions: `APPROACHING → state → APPROACHING`.

```json
{
  "type": "gapclose",
  "ability_id": "leap",
  "cooldown_ticks": 80,
  "windup_ticks": 10,
  "release_delay": 15,
  "min_distance": 6.0,
  "leap_speed": 1.8,
  "leap_height": 0.6,
  "teleport_mode": false,
  "landing_damage_coeff": 0.0,
  "landing_range": 3.0,
  "state": "APPROACHING",
  "priority": 25,
  "on_start_particles": "none",
  "on_complete_particles": "smoke_ring",
  "animations": {
    "on_windup": "animation.mob.leap_prep",
    "on_release": "animation.mob.leap"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"gapclose"` | Cooldown registry key. |
| `cooldown_ticks` | `80` | Cooldown after use. Must be >= 1. |
| `windup_ticks` | `10` | Windup duration ticks. Must be >= 1. |
| `release_delay` | `15` | Ticks after windup before the leap fires. |
| `min_distance` | `6.0` | Minimum target distance to trigger the leap. Must be > 0. |
| `leap_speed` | `1.8` | Horizontal velocity of the leap. |
| `leap_height` | `0.6` | Vertical velocity of the leap. |
| `teleport_mode` | `false` | If `true`, teleports directly behind the target instead of leaping. |
| `landing_damage_coeff` | `0.0` | Damage multiplier on landing (0 = no landing damage). |
| `landing_range` | `3.0` | Radius of landing damage in blocks. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `25` | Goal priority. |
| `on_start_particles` | `"none"` | Particles on start. |
| `on_complete_particles` | `"smoke_ring"` | Particles on completion. |
| `animations` | — | Supports `on_windup`, `on_release`, `on_complete`, `on_cancel`. |

---

### `frenzy`

A sustained attack mode where the mob charges at the target and repeatedly hits them over a duration. Automatically transitions to the specified state when the ability is ready.

```json
{
  "type": "frenzy",
  "ability_id": "frenzy",
  "cooldown_ticks": 100,
  "duration_ticks": 60,
  "speed": 1.4,
  "movement_mode": "track",
  "attack_type": "arc",
  "attack_interval_ticks": 10,
  "range": 3.0,
  "half_angle_deg": 60.0,
  "coeff": 1.0,
  "damage": 0.0,
  "turn_ticks": 5,
  "frenzy_loop_anim": "",
  "state": "ATTACKING_MELEE",
  "priority": 20,
  "on_tick_particles": "none",
  "on_complete_particles": "none",
  "on_interrupt_particles": "none"
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | — | **Required.** Cooldown registry key. |
| `cooldown_ticks` | `100` | Cooldown after use. |
| `duration_ticks` | `60` | How long the frenzy lasts. |
| `speed` | `1.4` | Movement speed during frenzy. |
| `movement_mode` | `"track"` | `"track"` — constantly pathfinds to target; `"commit"` — locks direction on start; `"turn_then_commit"` — rotates for `turn_ticks` then commits. |
| `attack_type` | `"arc"` | `"arc"` — cone attack each interval; `"radial"` — 360° burst each interval. |
| `attack_interval_ticks` | `10` | Ticks between each hit during the frenzy. Must be >= 1. |
| `range` | `3.0` | Attack reach in blocks. Must be > 0. |
| `half_angle_deg` | `60.0` | Arc half-angle for `arc` attack type. Must be in (0, 180). |
| `coeff` | `1.0` | Multiplier on `attack_damage` for each hit. |
| `damage` | `0.0` | Flat damage added on top of `coeff * attack_damage`. |
| `turn_ticks` | `5` | Ticks to spend rotating before committing (for `turn_then_commit` mode). |
| `frenzy_loop_anim` | `""` | Animation played in a loop during the frenzy. |
| `state` | `"ATTACKING_MELEE"` | State this runs in. |
| `priority` | `20` | Goal priority. |
| `on_tick_particles` | `"none"` | Particles spawned every tick during frenzy. |
| `on_complete_particles` | `"none"` | Particles on completion. |
| `on_interrupt_particles` | `"none"` | Particles on interrupt. |
| `animations` | — | Supports `on_action`, `on_complete`, `on_cancel`. |

---

### `brace`

Reactive defensive ability. Triggers when one or more configurable conditions are met, reducing incoming damage for a duration.

```json
{
  "type": "brace",
  "ability_id": "brace",
  "cooldown_ticks": 100,
  "duration_ticks": 30,
  "damage_reduction": 0.5,
  "condition_mode": "any",
  "check_health_threshold": false,
  "health_threshold_pct": 0.5,
  "check_ranged_hit": true,
  "ranged_hit_window_ticks": 40,
  "ranged_hit_min_distance": 8.0,
  "check_burst_damage": false,
  "burst_damage_threshold": 10.0,
  "state": "APPROACHING",
  "priority": 50,
  "on_start_particles": "none",
  "on_complete_particles": "none",
  "animations": {
    "on_complete": "animation.mob.shield_down"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"brace"` | Cooldown registry key. |
| `cooldown_ticks` | `100` | Cooldown after use. Must be >= 1. |
| `duration_ticks` | `30` | How long the brace lasts. Must be >= 1. |
| `damage_reduction` | `0.5` | Fraction of damage blocked (0–1 exclusive). |
| `condition_mode` | `"any"` | `"any"` — trigger if any condition is true; `"all"` — trigger only if all are true. |
| `check_health_threshold` | `false` | Trigger when health falls below `health_threshold_pct`. |
| `health_threshold_pct` | `0.5` | Fraction of max health (0.0–1.0) below which to trigger. |
| `check_ranged_hit` | `true` | Trigger when hit by a ranged attack from at least `ranged_hit_min_distance` blocks away within the last `ranged_hit_window_ticks` ticks. |
| `ranged_hit_window_ticks` | `40` | Time window for the ranged hit condition. |
| `ranged_hit_min_distance` | `8.0` | Minimum distance from attacker to count as "ranged". |
| `check_burst_damage` | `false` | Trigger when taking `burst_damage_threshold` damage in a short window. |
| `burst_damage_threshold` | `10.0` | Damage amount to trigger burst condition. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `50` | Goal priority. |
| `on_start_particles` | `"none"` | Particles on activation. |
| `on_complete_particles` | `"none"` | Particles on expiry. |
| `animations` | — | Supports `on_complete`, `on_cancel`. |

---

### `bulwark`

Defensive stance that activates when the target comes within range. While active the mob slows and may reflect damage back at the attacker. Deactivates when the target moves outside the deactivation range.

```json
{
  "type": "bulwark",
  "ability_id": "bulwark",
  "cooldown_ticks": 20,
  "activation_range": 8.0,
  "deactivation_range": 5.6,
  "reflect_coeff": 0.0,
  "approach_speed": 1.2,
  "state": "APPROACHING",
  "priority": 40,
  "on_start_particles": "none",
  "on_stop_particles": "none",
  "animations": {
    "on_complete": "animation.mob.shield_drop"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"bulwark"` | Cooldown registry key. |
| `cooldown_ticks` | `20` | Cooldown ticks between re-activations. |
| `activation_range` | `8.0` | Target distance at which bulwark activates. Must be > 0. |
| `deactivation_range` | `activation_range * 0.7` | Target distance at which bulwark drops. Setting this >= `activation_range` causes rapid toggling. |
| `reflect_coeff` | `0.0` | Fraction of incoming damage reflected back (0 = no reflect). Must be >= 0. |
| `approach_speed` | `1.2` | Pathfinding speed while bulwark is active. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `40` | Goal priority. |
| `on_start_particles` | `"none"` | Particles when bulwark activates. |
| `on_stop_particles` | `"none"` | Particles when bulwark drops. |
| `animations` | — | Supports `on_complete`. |

---

### `chase`

Sprints after the target at high speed for a limited duration when they are within range. Intended as a supplemental pursuit tool.

```json
{
  "type": "chase",
  "ability_id": "chase",
  "cooldown_ticks": 60,
  "chase_speed": 1.8,
  "engage_range": 5.0,
  "idle_attack_window_ticks": 40,
  "max_duration_ticks": 100,
  "state": "APPROACHING",
  "priority": 30,
  "on_start_particles": "none",
  "on_complete_particles": "none",
  "animations": {
    "on_complete": "animation.mob.sprint_stop",
    "on_cancel": "animation.mob.sprint_stop"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"chase"` | Cooldown registry key. |
| `cooldown_ticks` | `60` | Cooldown after use. Must be >= 1. |
| `chase_speed` | `1.8` | Pathfinding speed during the chase. Must be > 0. |
| `engage_range` | `5.0` | Minimum target distance before the chase kicks in. Must be > 0. |
| `idle_attack_window_ticks` | `40` | Ticks to continue chasing after losing sight. |
| `max_duration_ticks` | `100` | Maximum chase duration. Must be >= 1. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `30` | Goal priority. |
| `on_start_particles` | `"none"` | Particles on start. |
| `on_complete_particles` | `"none"` | Particles on end. |
| `animations` | — | Supports `on_complete`, `on_cancel`. |

---

### `zone_denial`

Places persistent hazard areas around the target's position on a timer, discouraging the player from standing still.

```json
{
  "type": "zone_denial",
  "ability_id": "zone_denial",
  "cooldown_ticks": 120,
  "goal_duration_ticks": 80,
  "hazard_lifetime_ticks": 60,
  "hazard_radius": 2.0,
  "hazard_damage_per_tick": 0.5,
  "place_interval_ticks": 20,
  "max_hazards": 4,
  "min_place_distance": 4.0,
  "approach_speed": 1.2,
  "state": "APPROACHING",
  "priority": 20,
  "animations": {
    "on_complete": "animation.mob.lower_arms",
    "on_cancel": "animation.mob.lower_arms"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"zone_denial"` | Cooldown registry key. |
| `cooldown_ticks` | `120` | Cooldown after the ability ends. Must be >= 1. |
| `goal_duration_ticks` | `80` | How long the ability is active. Must be >= 1. |
| `hazard_lifetime_ticks` | `60` | How long each hazard persists. Must be >= 1. |
| `hazard_radius` | `2.0` | Radius of each hazard zone in blocks. Must be > 0. |
| `hazard_damage_per_tick` | `0.5` | Damage dealt per tick to entities in the hazard. |
| `place_interval_ticks` | `20` | Ticks between hazard placements. Must be >= 1. |
| `max_hazards` | `4` | Maximum simultaneous hazards. Must be >= 1. |
| `min_place_distance` | `4.0` | Minimum distance from mob when placing a hazard. Must be > 0. |
| `approach_speed` | `1.2` | Movement speed during the ability. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `20` | Goal priority. |
| `animations` | — | Supports `on_complete`, `on_cancel`. |

---

### `reset_punishment`

If the target stays at range for too long, the mob winds up and heals. Punishes "pillar and poke" playstyles.

```json
{
  "type": "reset_punishment",
  "ability_id": "reset_punishment",
  "cooldown_ticks": 200,
  "interrupt_cooldown_ticks": 40,
  "range_threshold": 10.0,
  "time_at_range_ticks": 100,
  "windup_ticks": 40,
  "interrupt_range": 6.0,
  "heal_amount": 0.0,
  "heal_fraction": 0.3,
  "approach_speed": 1.2,
  "state": "APPROACHING",
  "priority": 15,
  "on_complete_particles": "none",
  "on_interrupt_particles": "none",
  "animations": {
    "on_complete": "animation.mob.heal",
    "on_cancel": "animation.mob.flinch"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"reset_punishment"` | Cooldown registry key. |
| `cooldown_ticks` | `200` | Full cooldown after healing completes. Must be >= 1. |
| `interrupt_cooldown_ticks` | `40` | Short cooldown when interrupted. |
| `range_threshold` | `10.0` | Distance at which the target counts as "at range". Must be > 0. |
| `time_at_range_ticks` | `100` | Consecutive ticks target must stay at range before winding up. Must be >= 1. |
| `windup_ticks` | `40` | Duration of the windup before healing fires. Must be >= 1. |
| `interrupt_range` | `range_threshold * 0.6` | If the target closes to within this range during windup, the heal is interrupted. |
| `heal_amount` | `0.0` | Flat HP to restore on success. |
| `heal_fraction` | `0.0` | Fraction of max HP to restore on success. Both can be set; they stack. |
| `approach_speed` | `1.2` | Movement speed during the windup. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `15` | Goal priority. |
| `on_complete_particles` | `"none"` | Particles on successful heal. |
| `on_interrupt_particles` | `"none"` | Particles on interrupt. |
| `animations` | — | Supports `on_complete`, `on_cancel`. |

---

### `range_punish`

Fires an expanding wave outward from the mob. The wave passes through a safe zone near the mob, then damages anything it touches at range. Punishes players who try to camp at distance.

```json
{
  "type": "range_punish",
  "ability_id": "range_punish",
  "cooldown_ticks": 100,
  "interrupt_cooldown_ticks": 30,
  "windup_ticks": 20,
  "safe_zone_radius": 1.5,
  "wave_width": 2.0,
  "wave_speed": 1.0,
  "max_range": 16.0,
  "damage": 0.0,
  "coeff": 0.8,
  "state": "APPROACHING",
  "priority": 35,
  "on_start_particles": "none",
  "on_complete_particles": "none",
  "on_interrupt_particles": "none",
  "animations": {
    "on_complete": "animation.mob.release_pulse",
    "on_cancel": "animation.mob.flinch"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | `"range_punish"` | Cooldown registry key. |
| `cooldown_ticks` | `100` | Full cooldown after wave reaches max range. Must be >= 1. |
| `interrupt_cooldown_ticks` | `30` | Cooldown when interrupted during windup. |
| `windup_ticks` | `20` | Windup ticks before the wave launches. Must be >= 1. |
| `safe_zone_radius` | `1.5` | Radius in blocks around the mob where the wave deals no damage. |
| `wave_width` | `2.0` | Thickness of the damage ring in blocks. Must be > 0. |
| `wave_speed` | `1.0` | Blocks per tick the wave travels. Must be > 0. |
| `max_range` | `16.0` | Maximum range the wave travels before stopping. Must be > `safe_zone_radius`. |
| `damage` | `0.0` | Flat damage dealt to targets hit by the wave. |
| `coeff` | `0.8` | Multiplier on `attack_damage` added to `damage`. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `35` | Goal priority. |
| `on_start_particles` | `"none"` | Particles at windup start. |
| `on_complete_particles` | `"none"` | Particles when wave expires. |
| `on_interrupt_particles` | `"none"` | Particles when interrupted. |
| `animations` | — | Supports `on_complete`, `on_cancel`. |

---

### `ambush`

Full ambush lifecycle for a pure stealth mob. Sets up `HIDDEN → ACTIVE → REHIDING` state transitions and installs the necessary AmbushModule goals. Use this with the `ambusher` archetype.

The mob hides, waits until the target is within `ambush_radius`, surfaces, attacks, then re-hides after a delay.

```json
{
  "type": "ambush",
  "detection_range": 16.0,
  "ambush_radius": 6.0,
  "scan_interval_ticks": 10,
  "rehide_delay_ticks": 60,
  "surface_cooldown_ticks": 40,
  "on_hidden_tick_particles": "none",
  "on_rehide_complete_particles": "none",
  "animations": {
    "on_windup":   "animation.mob.surface",
    "on_complete": "animation.mob.dive"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `detection_range` | tuning `detection` | Radius in which the mob scans for a target while hidden. |
| `ambush_radius` | `6.0` | Distance at which the mob surfaces to attack. Must be > 0. |
| `scan_interval_ticks` | `10` | Ticks between target scans. Must be > 0. |
| `rehide_delay_ticks` | `60` | Ticks after surfacing before the mob tries to re-hide. |
| `surface_cooldown_ticks` | `40` | Minimum ticks between surfacing events. |
| `on_hidden_tick_particles` | `"none"` | Particles spawned each tick while hidden. |
| `on_rehide_complete_particles` | `"none"` | Particles when re-hiding finishes. |
| `animations` | — | `on_windup` = during surfacing; `on_complete` = during re-hiding. |

**Note:** This feature alone gives the mob melee attacks while active. Combine with `ambush_attack` for special attacks, or `sweep` / `charge` with `"state": "ACTIVE"` for more complex behaviors.

---

### `ambush_attack`

A stealthy attack where the mob briefly enters a hidden/burrowed state, repositions around the target, then surfaces to strike. Designed for skirmisher-style mobs that want to re-hide mid-fight.

```json
{
  "type": "ambush_attack",
  "ability_id": "sand_vanish",
  "cooldown_ticks": 120,
  "min_cooldown_ticks": 40,
  "hidden_duration_ticks": 60,
  "movement_mode": "strafe",
  "surface_mode": "behind",
  "strafe_radius": 4.0,
  "burrow_mode": false,
  "hide_visuals": true,
  "ambush_loop_anim": "",
  "priority": 15,
  "on_tick_particles": "none",
  "on_complete_particles": "none",
  "on_interrupt_particles": "none",
  "animations": {
    "on_complete": "animation.mob.strike"
  }
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | — | **Required.** Cooldown registry key. |
| `cooldown_ticks` | `120` | Max cooldown. Must be > 0. |
| `min_cooldown_ticks` | `40` | Cooldown applied when the ability is preempted before surfacing. |
| `hidden_duration_ticks` | `60` | How long the mob stays hidden before surfacing. Must be > 0. |
| `movement_mode` | `"approach"` | `"approach"` — moves directly toward surface point; `"strafe"` — orbits at `strafe_radius`. |
| `surface_mode` | `"behind"` | Where the mob surfaces relative to the target: `"behind"`, `"front"`, `"underneath"`, `"random"`. |
| `strafe_radius` | `4.0` | Orbit radius for `strafe` movement mode. Must be > 0. |
| `burrow_mode` | `false` | If `true`, plays a burrowing animation rather than fading out. |
| `hide_visuals` | `true` | If `true`, renders the mob invisible while hidden. |
| `ambush_loop_anim` | `""` | Animation looped while the mob is in the hidden phase. |
| `priority` | `15` | Goal priority. |
| `on_tick_particles` | `"none"` | Particles each tick while hidden. |
| `on_complete_particles` | `"none"` | Particles on surfacing. |
| `on_interrupt_particles` | `"none"` | Particles when interrupted. |
| `animations` | — | Supports `on_complete` (plays on surface strike). |

---

### `ranged_attack`

A persistent ranged combat mode. The mob winds up, fires a projectile, waits for the shot cooldown, then repeats. Supports magazine/reload mechanics and three movement modes.

```json
{
  "type": "ranged_attack",
  "ability_id": "bow_attack",
  "engage_cooldown_ticks": 20,
  "movement_mode": "strafe",
  "projectile_type": "arrow",
  "projectile_speed": 1.6,
  "projectile_divergence": 1.0,
  "windup_ticks": 15,
  "shot_cooldown_ticks": 20,
  "mag_size": -1,
  "reload_duration_ticks": 60,
  "reload_move_speed": -1.0,
  "approach_speed": 0.0,
  "preferred_distance": 8.0,
  "strafe_speed": 1.2,
  "retreat_range": 4.0,
  "retreat_speed": 1.5,
  "strafe_flip_interval": 60,
  "min_kite_distance": 8.0,
  "kite_speed": 1.4,
  "windup_anim": "",
  "fire_anim": "",
  "reload_start_anim": "",
  "reload_complete_anim": "",
  "on_fire_particles": "none",
  "on_windup_particles": "none",
  "on_reload_complete_particles": "none",
  "state": "APPROACHING",
  "priority": 10
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | — | **Required.** Cooldown registry key. |
| `engage_cooldown_ticks` | `20` | Cooldown triggered when the goal ends (target lost or reload complete). |
| `movement_mode` | `"basic"` | `"basic"` — stand still or approach; `"strafe"` — orbit at preferred distance; `"kite"` — flee when target is too close. |
| `projectile_type` | `"arrow"` | `"arrow"`, `"spectral_arrow"`, `"trident"`, `"snowball"`, `"egg"`, `"small_fireball"`. |
| `projectile_speed` | `1.6` | Projectile launch speed. Must be > 0. |
| `projectile_divergence` | `1.0` | Inaccuracy multiplier. Higher = less accurate. |
| `windup_ticks` | `15` | Ticks to wind up before each shot. Must be >= 1. |
| `shot_cooldown_ticks` | `20` | Ticks to wait between shots. Must be >= 0. With infinite ammo this is registered as a cooldown in the ability system after each shot, allowing other goals (e.g. `reposition`) to run during the pause. With finite ammo the wait is handled inside the goal. |
| `mag_size` | `-1` | Number of shots per magazine. Values <= 0 mean infinite ammo. Must not be 0. |
| `reload_duration_ticks` | `60` | Ticks to reload when the magazine is empty. Only used when `mag_size > 0`. |
| `reload_move_speed` | `-1.0` | Movement during reload: `< 0` = use normal movement mode, `0` = stop, `> 0` = approach target at this speed. |
| `winddown_ticks` | `0` | Ticks spent in a winddown phase immediately after each shot (before the shot cooldown starts). `0` disables winddown. Must be >= 0. |
| `winddown_movespeed` | `0.0` | Movement during winddown: `< 0` = use normal movement mode, `0` = stop, `> 0` = approach target at this speed. |
| `approach_speed` | `0.0` | (BASIC mode) Speed to approach target. `0` = stand still. |
| `preferred_distance` | `8.0` | (STRAFE mode) Ideal distance to maintain from target. Must be > 0. |
| `strafe_speed` | `1.2` | (STRAFE mode) Strafing movement speed. |
| `retreat_range` | `4.0` | (STRAFE mode) Distance below which the mob retreats. |
| `retreat_speed` | `1.5` | (STRAFE mode) Retreat movement speed. |
| `strafe_flip_interval` | `60` | (STRAFE mode) Ticks between switching strafe direction. |
| `min_kite_distance` | `8.0` | (KITE mode) Distance below which the mob flees and resets its windup. Must be > 0. |
| `max_kite_distance` | `min_kite_distance * 2` | (KITE mode) Distance above which the mob approaches the target. Must be > `min_kite_distance`. The mob shoots only inside the `[min_kite_distance, max_kite_distance]` band. |
| `kite_speed` | `1.4` | (KITE mode) Speed used both when fleeing (too close) and when approaching (too far). |
| `windup_anim` | `""` | Animation played at start of each windup phase. |
| `fire_anim` | `""` | Animation played when each shot fires. |
| `reload_start_anim` | `""` | Animation played when reload begins. |
| `reload_complete_anim` | `""` | Animation played when reload finishes. |
| `on_fire_particles` | `"none"` | Particles on each shot. |
| `on_windup_particles` | `"none"` | Particles at start of each windup. |
| `on_reload_complete_particles` | `"none"` | Particles when reload completes. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `10` | Goal priority. |

**KITE mode note:** The windup only counts down when the mob is at safe range. If the target closes in, the mob flees and resets the windup counter. A shot only fires after a complete uninterrupted windup.

**Ammo note:** Ammo count persists across goal activations. The mag reloads only when it is empty.

---

### `leave_hazard`

Reactive escape ability. Triggers when the mob detects it is in a hazardous situation and immediately attempts to escape toward its target via teleport or a velocity launch. Fires once per activation (instantaneous action) then goes on cooldown.

```json
{
  "type": "leave_hazard",
  "ability_id": "escape_hazard",
  "cooldown_ticks": 100,
  "check_in_water": true,
  "check_in_lava": true,
  "check_sustained_damage": true,
  "sustained_damage_threshold": 5.0,
  "sustained_damage_interaction_ticks": 60,
  "condition_mode": "any",
  "escape_mode": "teleport",
  "teleport_radius": 4.0,
  "teleport_min_radius": 1.5,
  "teleport_attempts": 16,
  "launch_vertical_speed": 0.6,
  "launch_horizontal_speed": 0.5,
  "state": "APPROACHING",
  "priority": 90,
  "on_escape_particles": "none"
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | — | **Required.** Cooldown registry key. |
| `cooldown_ticks` | `100` | Cooldown after the escape fires. Must be >= 1. |
| `check_in_water` | `true` | Trigger when the mob is touching water. |
| `check_in_lava` | `true` | Trigger when the mob is in lava. |
| `check_sustained_damage` | `true` | Trigger when the mob is taking damage with little combat interaction (environmental/hazard damage). |
| `sustained_damage_threshold` | `5.0` | Minimum recent damage that counts as "sustained". Must be > 0. |
| `sustained_damage_interaction_ticks` | `60` | If the mob was hit by its target within this many ticks, the damage counts as combat rather than hazard. Must be >= 1. |
| `condition_mode` | `"any"` | `"any"` — trigger if any enabled check is true; `"all"` — trigger only if all enabled checks are true simultaneously. |
| `escape_mode` | `"teleport"` | `"teleport"` — instantly moves the mob to a safe spot near the target; `"launch"` — applies a velocity burst upward and toward the target. |
| `teleport_radius` | `4.0` | (TELEPORT) Maximum distance from the target to place the mob. Must be > 0. |
| `teleport_min_radius` | `1.5` | (TELEPORT) Minimum distance from the target. Clamped to `teleport_radius`. |
| `teleport_attempts` | `16` | (TELEPORT) Number of random positions tested before giving up. Must be >= 1. |
| `launch_vertical_speed` | `0.6` | (LAUNCH) Upward velocity component applied on escape. |
| `launch_horizontal_speed` | `0.5` | (LAUNCH) Horizontal velocity toward the target on escape. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `90` | Goal priority. Should be high so hazard escape preempts normal combat goals. |
| `on_escape_particles` | `"none"` | Particles spawned when the escape fires. |
| `animations` | — | Supports `on_complete`. |

**Teleport note:** The teleport tries up to `teleport_attempts` random positions in the `[teleport_min_radius, teleport_radius]` band around the target. A position is valid if neither the feet nor head block is solid or fluid. If no valid position is found, the mob stays in place (the cooldown still triggers).

**Sustained damage note:** Fires when `recent_damage_taken >= sustained_damage_threshold` AND the mob has not been hit by its target in the last `sustained_damage_interaction_ticks` ticks. This distinguishes lava/fire/poison damage from active player combat.

---

### `reposition`

The mob picks a random destination within a radius and moves there via pathfinding or a velocity dash. Useful for dodge-rolling, repositioning between attacks, or general evasive movement.

```json
{
  "type": "reposition",
  "ability_id": "dodge",
  "cooldown_ticks": 60,
  "duration_ticks": 20,
  "radius": 6.0,
  "min_distance": 2.0,
  "arrival_radius": 1.5,
  "movement_mode": "pathfind",
  "speed_mode": "absolute",
  "speed": 1.8,
  "anchor": "self",
  "state": "APPROACHING",
  "priority": 15,
  "on_start_particles": "none",
  "on_complete_particles": "none",
  "on_interrupt_particles": "none"
}
```

| Field | Default | Description |
|---|---|---|
| `ability_id` | — | **Required.** Cooldown registry key. |
| `cooldown_ticks` | `60` | Cooldown after use. Must be >= 1. |
| `duration_ticks` | `20` | Maximum ticks before the ability times out. Must be >= 1. |
| `radius` | `6.0` | Maximum distance from the anchor point to pick a destination. Must be > 0. |
| `min_distance` | `2.0` | Minimum distance from the anchor point (clamped to radius if >= radius). |
| `arrival_radius` | `1.5` | Distance from destination at which the mob counts as "arrived". Must be > 0. |
| `movement_mode` | `"pathfind"` | `"pathfind"` — uses pathfinding (can be preempted by higher-priority goals); `"dash"` — applies direct velocity each tick (committed, cannot be preempted until done or timed out). |
| `speed_mode` | `"absolute"` | `"absolute"` — uses the `speed` value directly; `"dynamic"` — computes the speed needed to reach the destination within `duration_ticks`. |
| `speed` | `1.8` | Movement speed. Used in `absolute` mode, and as a fallback in `dynamic` mode. Must be > 0 if `speed_mode` is `"absolute"`. |
| `anchor` | `"self"` | Origin of the random position: `"self"` — relative to the mob; `"target"` — relative to the current target. |
| `state` | `"APPROACHING"` | State this runs in. |
| `priority` | `15` | Goal priority. |
| `on_start_particles` | `"none"` | Particles when repositioning begins. |
| `on_complete_particles` | `"none"` | Particles on arrival. |
| `on_interrupt_particles` | `"none"` | Particles when interrupted. |

**DASH note:** In `dash` mode the mob is committed — no other goals can preempt it until it arrives or times out. In `pathfind` mode a higher-priority goal can take over mid-move.

**DYNAMIC speed:** For DASH mode, computed as `distance / duration_ticks` (blocks per tick). For PATHFIND mode, computed as `distance / (duration_ticks * base_movement_speed)` capped at 5.0.

---

## Complete examples

### Bruiser — a heavy brawler

```json
{
  "id": "stone_guardian",
  "archetype": "bruiser",
  "theme": "stone",
  "form": "azurelib",
  "render": {
    "backend": "azurelib",
    "assets": {
      "geo":       "mymod:geo/stone_guardian.geo.json",
      "animation": "mymod:animations/stone_guardian.animations.json",
      "texture":   "mymod:textures/entity/stone_guardian.png"
    },
    "loops": {
      "idle":            "animation.guardian.idle",
      "idle_hostile":    "animation.guardian.idle_alert",
      "moving":          "animation.guardian.walk",
      "moving_hostile":  "animation.guardian.walk_alert",
      "running":         "animation.guardian.run"
    }
  },
  "scale": { "width": 1.4, "height": 3.0 },
  "tuning": {
    "health":    "HIGH",
    "damage":    "HIGH",
    "speed":     "LOW",
    "detection": "MEDIUM"
  },
  "attributes": {
    "max_health": 80.0,
    "attack_damage": 12.0,
    "melee_range": 3.5,
    "knockback_resistance": 0.8
  },
  "features": [
    {
      "type": "idle",
      "mode": "still",
      "scan_interval_ticks": 20
    },
    {
      "type": "melee",
      "cooldown_ticks": 25,
      "animations": { "on_action": "animation.guardian.punch" }
    },
    {
      "type": "shockwave",
      "ability_id": "stone_slam",
      "cooldown_ticks": 100,
      "min_cooldown": 40,
      "range": 5.0,
      "coeff": 1.5,
      "on_complete_particles": "poof_burst",
      "animations": { "on_action": "animation.guardian.slam" }
    },
    {
      "type": "charge",
      "ability_id": "boulder_charge",
      "cooldown_ticks": 140,
      "windup_ticks": 15,
      "release_delay": 12,
      "release_type": "radial",
      "range": 4.0,
      "coeff": 2.0,
      "charge_speed": 1.5,
      "upwards_speed": 0.0,
      "on_start_particles": "dust_trail",
      "on_complete_particles": "smoke_ring",
      "animations": { "on_windup": "animation.guardian.charge" }
    }
  ]
}
```

### Skirmisher — a ranged kiter

```json
{
  "id": "wind_archer",
  "archetype": "skirmisher",
  "theme": "wind",
  "form": "AGILE_BIPED",
  "render": {
    "backend": "azurelib",
    "assets": {
      "geo":       "mymod:geo/wind_archer.geo.json",
      "animation": "mymod:animations/wind_archer.animations.json",
      "texture":   "mymod:textures/entity/wind_archer.png"
    },
    "loops": {
      "idle":           "animation.archer.idle",
      "idle_hostile":   "animation.archer.aim",
      "moving":         "animation.archer.walk",
      "moving_hostile": "animation.archer.walk_aim",
      "running":        "animation.archer.run"
    }
  },
  "tuning": {
    "health":    "LOW",
    "damage":    "MEDIUM",
    "speed":     "HIGH",
    "detection": "HIGH"
  },
  "attributes": {
    "max_health": 25.0,
    "attack_damage": 4.0
  },
  "features": [
    {
      "type": "idle",
      "mode": "wander",
      "wander_range": 10.0,
      "pause_ticks": 30
    },
    {
      "type": "ranged_attack",
      "ability_id": "arrow_volley",
      "engage_cooldown_ticks": 30,
      "movement_mode": "kite",
      "projectile_type": "arrow",
      "projectile_speed": 2.0,
      "windup_ticks": 12,
      "shot_cooldown_ticks": 15,
      "min_kite_distance": 6.0,
      "kite_speed": 1.6,
      "windup_anim": "animation.archer.draw",
      "fire_anim": "animation.archer.release",
      "on_fire_particles": "none",
      "state": "APPROACHING",
      "priority": 12
    },
    {
      "type": "reposition",
      "ability_id": "dodge_roll",
      "cooldown_ticks": 80,
      "duration_ticks": 15,
      "radius": 5.0,
      "min_distance": 3.0,
      "movement_mode": "dash",
      "speed_mode": "absolute",
      "speed": 2.5,
      "anchor": "self",
      "state": "APPROACHING",
      "priority": 20,
      "on_start_particles": "poof_burst"
    }
  ]
}
```
