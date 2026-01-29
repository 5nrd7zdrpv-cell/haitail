package com.efd.hytale.farmworld.shared.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {
  private ConfigValidator() {}

  public static List<String> validate(FarmWorldConfig config) {
    List<String> warnings = new ArrayList<>();
    if (config.farmWorld == null) {
      warnings.add("farmWorld section missing; defaults applied.");
      config.farmWorld = new FarmWorldSettings();
    }
    if (config.farmWorld.resetIntervalDays <= 0) {
      warnings.add("farmWorld.resetIntervalDays must be > 0; defaulting to 7.");
      config.farmWorld.resetIntervalDays = 7;
    }
    if (config.farmWorld.spawn == null) {
      warnings.add("farmWorld.spawn missing; defaults applied.");
      config.farmWorld.spawn = new FarmWorldSpawn();
    }
    if (config.protection == null) {
      warnings.add("protection section missing; defaults applied.");
      config.protection = new ProtectionConfig();
    }
    if (config.protection.radius < 0) {
      warnings.add("protection.radius must be >= 0; defaulting to 64.");
      config.protection.radius = 64;
    }
    if (config.protection.actions == null) {
      warnings.add("protection.actions missing; defaults applied.");
      config.protection.actions = new ProtectionActions();
    }
    if (config.combat == null) {
      warnings.add("combat section missing; defaults applied.");
      config.combat = new CombatConfig();
    }
    if (config.combat.tagSeconds < 0) {
      warnings.add("combat.tagSeconds must be >= 0; defaulting to 20.");
      config.combat.tagSeconds = 20;
    }
    if (config.combat.penaltySeconds < 0) {
      warnings.add("combat.penaltySeconds must be >= 0; defaulting to 30.");
      config.combat.penaltySeconds = 30;
    }
    if (config.combat.onQuit == null || config.combat.onQuit.isBlank()) {
      warnings.add("combat.onQuit missing; defaulting to NONE.");
      config.combat.onQuit = "NONE";
    }
    return warnings;
  }
}
