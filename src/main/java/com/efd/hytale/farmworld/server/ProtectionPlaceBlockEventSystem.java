package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ProtectionPlaceBlockEventSystem implements ISystem<EntityStore> {
  private final ProtectionBridge protectionBridge;
  private final FarmWorldConfig config;

  public ProtectionPlaceBlockEventSystem(ProtectionBridge protectionBridge, FarmWorldConfig config) {
    this.protectionBridge = protectionBridge;
    this.config = config;
  }
}
