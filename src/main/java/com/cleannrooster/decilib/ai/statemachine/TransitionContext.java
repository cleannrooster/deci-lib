package com.cleannrooster.decilib.ai.statemachine;

import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;

public record TransitionContext(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {}
