package com.efd.hytale.farmworld.shared.config;

import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {
  private static final String PREFIX = "[FarmWorld] ";

  private ConfigValidator() {}

  public static List<String> validate(FarmWorldConfig config) {
    List<String> warnings = new ArrayList<>();
    if (config.farmWorld == null) {
      warnings.add(PREFIX + "farmWorld-Bereich fehlt; Standardwerte wurden gesetzt.");
      config.farmWorld = new FarmWorldSettings();
    }
    if (config.farmWorld.resetIntervalDays <= 0) {
      warnings.add(PREFIX + "farmWorld.resetIntervalDays muss > 0 sein; Standardwert 7 wird gesetzt.");
      config.farmWorld.resetIntervalDays = 7;
    }
    if (config.farmWorld.prefabSpawnId == null || config.farmWorld.prefabSpawnId.isBlank()) {
      warnings.add(PREFIX + "farmWorld.prefabSpawnId fehlt; Standardwert prefabs/farm_spawn.prefab wird gesetzt.");
      config.farmWorld.prefabSpawnId = "prefabs/farm_spawn.prefab";
    }
    if (config.farmWorld.spawn == null) {
      warnings.add(PREFIX + "farmWorld.spawn fehlt; Standardwerte wurden gesetzt.");
      config.farmWorld.spawn = new FarmWorldSpawn();
    }
    if (config.farmWorld.spawn.worldId == null) {
      warnings.add(PREFIX + "farmWorld.spawn.worldId fehlt; Standardwert leer wird gesetzt.");
      config.farmWorld.spawn.worldId = "";
    }
    if (config.farmWorld.spawn.instanceId == null) {
      warnings.add(PREFIX + "farmWorld.spawn.instanceId fehlt; Standardwert leer wird gesetzt.");
      config.farmWorld.spawn.instanceId = "";
    }
    if (config.farmWorld.worldId == null || config.farmWorld.worldId.isBlank()) {
      warnings.add(PREFIX + "farmWorld.worldId fehlt; Standardwert farm wird gesetzt.");
      config.farmWorld.worldId = "farm";
    }
    if (config.farmWorld.instanceId == null || config.farmWorld.instanceId.isBlank()) {
      warnings.add(PREFIX + "farmWorld.instanceId fehlt; Standardwert default wird gesetzt.");
      config.farmWorld.instanceId = "default";
    }
    if (config.protection == null) {
      warnings.add(PREFIX + "protection-Bereich fehlt; Standardwerte wurden gesetzt.");
      config.protection = new ProtectionConfig();
    }
    if (config.protection.radius < 0) {
      warnings.add(PREFIX + "protection.radius muss >= 0 sein; Standardwert 64 wird gesetzt.");
      config.protection.radius = 64;
    }
    if (config.protection.center != null) {
      if (config.protection.center.worldId == null) {
        warnings.add(PREFIX + "protection.center.worldId fehlt; Standardwert leer wird gesetzt.");
        config.protection.center.worldId = "";
      }
      if (config.protection.center.instanceId == null) {
        warnings.add(PREFIX + "protection.center.instanceId fehlt; Standardwert leer wird gesetzt.");
        config.protection.center.instanceId = "";
      }
    }
    if (config.protection.bypassRoles == null) {
      warnings.add(PREFIX + "protection.bypassRoles fehlt; Standardwert leer wird gesetzt.");
      config.protection.bypassRoles = java.util.List.of();
    }
    if (config.protection.bypassPermissions == null) {
      warnings.add(PREFIX + "protection.bypassPermissions fehlt; Standardwert leer wird gesetzt.");
      config.protection.bypassPermissions = java.util.List.of();
    }
    if (config.protection.bypassPermission == null) {
      warnings.add(PREFIX + "protection.bypassPermission fehlt; Standardwert efd.farmworld.admin wird gesetzt.");
      config.protection.bypassPermission = "efd.farmworld.admin";
    }
    if (config.protection.actions == null) {
      warnings.add(PREFIX + "protection.actions fehlt; Standardwerte wurden gesetzt.");
      config.protection.actions = new ProtectionActions();
    }
    if (config.combat == null) {
      warnings.add(PREFIX + "combat-Bereich fehlt; Standardwerte wurden gesetzt.");
      config.combat = new CombatConfig();
    }
    if (config.nextResetEpochSeconds < 0) {
      warnings.add(PREFIX + "nextResetEpochSeconds muss >= 0 sein; Standardwert 0 wird gesetzt.");
      config.nextResetEpochSeconds = 0L;
    }
    if (config.combat.tagSeconds < 0) {
      warnings.add(PREFIX + "combat.tagSeconds muss >= 0 sein; Standardwert 15 wird gesetzt.");
      config.combat.tagSeconds = 15;
    }
    if (config.combat.penaltySeconds < 0) {
      warnings.add(PREFIX + "combat.penaltySeconds muss >= 0 sein; Standardwert 30 wird gesetzt.");
      config.combat.penaltySeconds = 30;
    }
    if (config.combat.onQuit == null || config.combat.onQuit.isBlank()) {
      warnings.add(PREFIX + "combat.onQuit fehlt; Standardwert NONE wird gesetzt.");
      config.combat.onQuit = "NONE";
    }
    return warnings;
  }

  public static List<String> validateSevere(FarmWorldConfig config) {
    List<String> errors = new ArrayList<>();
    if (config.farmWorld == null) {
      errors.add(PREFIX + "farmWorld-Bereich fehlt; prefabSpawnId kann nicht ermittelt werden.");
      return errors;
    }
    if (config.farmWorld.prefabSpawnId == null || config.farmWorld.prefabSpawnId.isBlank()) {
      errors.add(PREFIX + "farmWorld.prefabSpawnId ist leer; Prefab-Spawn wird übersprungen.");
    }
    if (config.farmWorld.spawn == null) {
      errors.add(PREFIX + "farmWorld.spawn fehlt; Spawn-Position wird übersprungen.");
      return errors;
    }
    if (Double.isNaN(config.farmWorld.spawn.x) || Double.isNaN(config.farmWorld.spawn.y)
        || Double.isNaN(config.farmWorld.spawn.z)) {
      errors.add(PREFIX + "farmWorld.spawn enthält NaN-Koordinaten; Spawn-Position wird übersprungen.");
    }
    return errors;
  }
}
