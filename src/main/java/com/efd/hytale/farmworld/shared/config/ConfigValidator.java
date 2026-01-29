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
    if (config.farmWorld.prefabSpawnId == null || config.farmWorld.prefabSpawnId.isBlank()) {
      warnings.add("farmWorld.prefabSpawnId missing; defaulting to prefabs/farm_spawn.prefab.");
      config.farmWorld.prefabSpawnId = "prefabs/farm_spawn.prefab";
    }
    if (config.farmWorld.spawn == null) {
      warnings.add("farmWorld.spawn missing; defaults applied.");
      config.farmWorld.spawn = new FarmWorldSpawn();
    }
    if (config.farmWorld.spawn.worldId == null) {
      warnings.add("farmWorld.spawn.worldId missing; defaulting to empty.");
      config.farmWorld.spawn.worldId = "";
    }
    if (config.farmWorld.spawn.instanceId == null) {
      warnings.add("farmWorld.spawn.instanceId missing; defaulting to empty.");
      config.farmWorld.spawn.instanceId = "";
    }
    if (config.farmWorld.worldId == null || config.farmWorld.worldId.isBlank()) {
      warnings.add("farmWorld.worldId missing; defaulting to farm.");
      config.farmWorld.worldId = "farm";
    }
    if (config.farmWorld.instanceId == null || config.farmWorld.instanceId.isBlank()) {
      warnings.add("farmWorld.instanceId missing; defaulting to default.");
      config.farmWorld.instanceId = "default";
    }
    if (config.protection == null) {
      warnings.add("protection section missing; defaults applied.");
      config.protection = new ProtectionConfig();
    }
    if (config.protection.radius < 0) {
      warnings.add("protection.radius must be >= 0; defaulting to 64.");
      config.protection.radius = 64;
    }
    if (config.protection.center != null) {
      if (config.protection.center.worldId == null) {
        warnings.add("protection.center.worldId missing; defaulting to empty.");
        config.protection.center.worldId = "";
      }
      if (config.protection.center.instanceId == null) {
        warnings.add("protection.center.instanceId missing; defaulting to empty.");
        config.protection.center.instanceId = "";
      }
    }
    if (config.protection.bypassRoles == null) {
      warnings.add("protection.bypassRoles missing; defaulting to empty.");
      config.protection.bypassRoles = java.util.List.of();
    }
    if (config.protection.bypassPermissions == null) {
      warnings.add("protection.bypassPermissions missing; defaulting to empty.");
      config.protection.bypassPermissions = java.util.List.of();
    }
    if (config.protection.bypassPermission == null) {
      warnings.add("protection.bypassPermission missing; defaulting to efd.farmworld.admin.");
      config.protection.bypassPermission = "efd.farmworld.admin";
    }
    if (config.protection.actions == null) {
      warnings.add("protection.actions missing; defaults applied.");
      config.protection.actions = new ProtectionActions();
    }
    if (config.combat == null) {
      warnings.add("combat section missing; defaults applied.");
      config.combat = new CombatConfig();
    }
    if (config.nextResetEpochSeconds < 0) {
      warnings.add("nextResetEpochSeconds must be >= 0; defaulting to 0.");
      config.nextResetEpochSeconds = 0L;
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

  public static List<String> validateSevere(FarmWorldConfig config) {
    List<String> errors = new ArrayList<>();
    if (config.farmWorld == null) {
      errors.add("farmWorld section missing; unable to resolve prefabSpawnId.");
      return errors;
    }
    if (config.farmWorld.prefabSpawnId == null || config.farmWorld.prefabSpawnId.isBlank()) {
      errors.add("farmWorld.prefabSpawnId is blank; prefab spawn will be skipped.");
    }
    if (config.farmWorld.spawn == null) {
      errors.add("farmWorld.spawn missing; spawn position will be skipped.");
      return errors;
    }
    if (Double.isNaN(config.farmWorld.spawn.x) || Double.isNaN(config.farmWorld.spawn.y)
        || Double.isNaN(config.farmWorld.spawn.z)) {
      errors.add("farmWorld.spawn contains NaN coordinates; spawn position will be skipped.");
    }
    return errors;
  }
}
