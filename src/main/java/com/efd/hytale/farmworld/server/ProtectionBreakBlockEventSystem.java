package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
import java.util.List;

public class ProtectionBreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
  private final ProtectionBridge protectionBridge;
  private final FarmWorldConfig config;

  public ProtectionBreakBlockEventSystem(ProtectionBridge protectionBridge, FarmWorldConfig config) {
    super(BreakBlockEvent.class);
    this.protectionBridge = protectionBridge;
    this.config = config;
  }

  @Override
  public void handle(
      int index,
      ArchetypeChunk<EntityStore> chunk,
      Store<EntityStore> store,
      CommandBuffer<EntityStore> buffer,
      BreakBlockEvent event) {
    if (event.isCancelled()) {
      return;
    }
    Ref<EntityStore> actorRef = chunk.getReferenceTo(index);
    Player player = store.getComponent(actorRef, Player.getComponentType());
    if (player == null || player.getWorld() == null) {
      return;
    }
    Vector3i target = event.getTargetBlock();
    if (target == null) {
      return;
    }
    boolean allowed = protectionBridge.onBlockBreak(
        player.getDisplayName(),
        target.x,
        target.y,
        target.z,
        player.getWorld().getName(),
        config.farmWorld.instanceId,
        ProtectionPermissionHelper.collectBypassPermissions(player, config),
        List.of());
    if (!allowed) {
      event.setCancelled(true);
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }
}
