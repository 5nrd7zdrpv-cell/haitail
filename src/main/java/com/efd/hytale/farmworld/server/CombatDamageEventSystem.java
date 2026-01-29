package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
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
    Player victim = chunk.getComponent(entity, Player.getComponentType());
    if (victim == null) {
      return;
    }
    tagPlayer(victim);
    Damage.Source source = event.getSource();
    if (source instanceof Damage.EntitySource entitySource) {
      Player attacker = store.getComponent(entitySource.getRef(), Player.getComponentType());
      if (attacker != null) {
        tagPlayer(attacker);
      }
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.any();
  }

  private void tagPlayer(Player player) {
    if (player == null || player.getPlayerRef() == null || player.getPlayerRef().getUuid() == null) {
      return;
    }
    UUID playerId = player.getPlayerRef().getUuid();
    combatService.recordPlayer(playerId, player.getPlayerRef().getUsername());
    combatService.tag(playerId);
  }
}
