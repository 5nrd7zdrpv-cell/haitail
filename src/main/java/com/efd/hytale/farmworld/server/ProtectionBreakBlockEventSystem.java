package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.Message;
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
      int entity,
      ArchetypeChunk<EntityStore> chunk,
      Store<EntityStore> store,
      CommandBuffer<EntityStore> commandBuffer,
      BreakBlockEvent event) {
    Player player = chunk.getComponent(entity, Player.getComponentType());
    if (player == null || player.getWorld() == null || event.getTargetBlock() == null) {
      return;
    }
    Vector3i target = event.getTargetBlock();
    String actorName = ProtectionPermissionHelper.resolveActorName(player);
    ProtectionPermissionHelper.ResolvedWorld resolvedWorld =
        ProtectionPermissionHelper.resolveWorld(player.getWorld(), config);
    boolean allowed = protectionBridge.onBlockBreak(
        actorName,
        target.x,
        target.y,
        target.z,
        resolvedWorld.worldId,
        resolvedWorld.instanceId,
        ProtectionPermissionHelper.collectBypassPermissions(player, config),
        List.of());
    if (!allowed) {
      event.setCancelled(true);
      if (protectionBridge.shouldNotify(actorName)) {
        player.sendMessage(Message.raw("[FarmWorld] [FEHLER] Du befindest dich in einer Schutzzone."));
      }
    }
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.any();
  }
}
