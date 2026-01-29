package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CombatDamageEventSystem implements ISystem<EntityStore> {
  private final CombatTagService combatService;

  public CombatDamageEventSystem(CombatTagService combatService) {
    this.combatService = combatService;
  }
}
