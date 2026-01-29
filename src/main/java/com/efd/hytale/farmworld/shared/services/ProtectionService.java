package com.efd.hytale.farmworld.shared.services;

import com.efd.hytale.farmworld.shared.config.ProtectionActions;
import com.efd.hytale.farmworld.shared.config.ProtectionConfig;

public class ProtectionService {
  private final ProtectionConfig config;

  public ProtectionService(ProtectionConfig config) {
    this.config = config;
  }

  public boolean isActionAllowed(ProtectionCheckRequest request) {
    if (!config.enabled) {
      return true;
    }
    if (request.hasBypassPermission) {
      return true;
    }
    if (request.distanceFromSpawn > config.radius) {
      return true;
    }
    ProtectionActions actions = config.actions;
    return switch (request.action) {
      case PLACE -> !actions.place;
      case BREAK_BLOCK -> !actions.breakBlock;
      case INTERACT -> !actions.interact;
      case DAMAGE -> !actions.damage;
      case EXPLOSION -> !actions.explosion;
      case FIRE_SPREAD -> !actions.fireSpread;
      case LIQUID -> !actions.liquid;
    };
  }
}
