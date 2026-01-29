package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

public class CombatDamageEventSystem extends DamageEventSystem {
  private final CombatTagService combatService;

  public CombatDamageEventSystem(CombatTagService combatService) {
    super();
    this.combatService = combatService;
  }

  @Override
  public void handle(
      int entity,
      ArchetypeChunk<EntityStore> chunk,
      Store<EntityStore> store,
      CommandBuffer<EntityStore> commandBuffer,
      Damage event) {
    PlayerRef victimRef = PlayerRefResolver.fromEntity(store, chunk, entity);
    if (victimRef == null) {
      return;
    }
    tagPlayer(victimRef);
    Damage.Source source = event.getSource();
    if (source instanceof Damage.EntitySource entitySource) {
      PlayerRef attackerRef = PlayerRefResolver.fromRef(store, entitySource.getRef());
      tagPlayer(attackerRef);
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.any();
  }

  private void tagPlayer(PlayerRef playerRef) {
    if (playerRef == null || playerRef.getUuid() == null) {
      return;
    }
    UUID playerId = playerRef.getUuid();
    combatService.recordPlayer(playerId, playerRef.getUsername());
    combatService.tag(playerId);
  }
}
