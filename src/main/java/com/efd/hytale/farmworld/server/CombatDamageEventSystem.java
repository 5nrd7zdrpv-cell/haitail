package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class CombatDamageEventSystem extends EntityEventSystem<EntityStore, Damage> {
  private final CombatTagService combatService;

  public CombatDamageEventSystem(CombatTagService combatService) {
    super(Damage.class);
    this.combatService = combatService;
  }

  @Override
  public void handle(
      int index,
      ArchetypeChunk<EntityStore> chunk,
      Store<EntityStore> store,
      CommandBuffer<EntityStore> buffer,
      Damage damage) {
    if (damage.isCancelled() || damage.getAmount() <= 0f) {
      return;
    }
    Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
    Player target = store.getComponent(targetRef, Player.getComponentType());
    if (target == null) {
      return;
    }
    PlayerRef targetRefComponent = target.getPlayerRef();
    if (targetRefComponent != null) {
      combatService.recordPlayer(targetRefComponent.getUuid(), target.getDisplayName());
      combatService.tag(targetRefComponent.getUuid());
    }
    Damage.Source source = damage.getSource();
    if (source instanceof Damage.EntitySource entitySource) {
      Ref<EntityStore> sourceRef = entitySource.getRef();
      if (sourceRef == null) {
        return;
      }
      Player attacker = store.getComponent(sourceRef, Player.getComponentType());
      if (attacker == null) {
        return;
      }
      PlayerRef attackerRef = attacker.getPlayerRef();
      if (attackerRef != null) {
        combatService.recordPlayer(attackerRef.getUuid(), attacker.getDisplayName());
        combatService.tag(attackerRef.getUuid());
      }
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }
}
