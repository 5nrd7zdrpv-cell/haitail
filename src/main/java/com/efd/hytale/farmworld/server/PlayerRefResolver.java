package com.efd.hytale.farmworld.server;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class PlayerRefResolver {
  private PlayerRefResolver() {}

  public static PlayerRef fromPlayer(Player player) {
    if (player == null) {
      return null;
    }
    World world = player.getWorld();
    if (world == null || world.getEntityStore() == null) {
      return null;
    }
    Ref<EntityStore> reference = player.getReference();
    if (reference == null) {
      return null;
    }
    Store<EntityStore> store = world.getEntityStore().getStore();
    if (store == null) {
      return null;
    }
    return store.getComponent(reference, PlayerRef.getComponentType());
  }

  public static PlayerRef fromEntity(
      Store<EntityStore> store,
      ArchetypeChunk<EntityStore> chunk,
      int entity) {
    if (store == null || chunk == null) {
      return null;
    }
    Ref<EntityStore> reference = chunk.getReferenceTo(entity);
    if (reference == null) {
      return null;
    }
    return store.getComponent(reference, PlayerRef.getComponentType());
  }

  public static PlayerRef fromRef(Store<EntityStore> store, Ref<EntityStore> reference) {
    if (store == null || reference == null) {
      return null;
    }
    return store.getComponent(reference, PlayerRef.getComponentType());
  }
}
