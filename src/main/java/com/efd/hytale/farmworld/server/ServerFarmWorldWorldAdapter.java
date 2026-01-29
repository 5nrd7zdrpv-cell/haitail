package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.services.FarmWorldWorldAdapter;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerFarmWorldWorldAdapter implements FarmWorldWorldAdapter {
  private final Logger logger;

  public ServerFarmWorldWorldAdapter(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean resetWorld(String worldId, String instanceId) {
    Universe universe = Universe.get();
    if (universe == null) {
      logger.warning("[FarmWorld] Universe ist nicht verfügbar; World-Reset abgebrochen.");
      return false;
    }
    World world = universe.getWorld(worldId);
    if (world != null) {
      world.validateDeleteOnRemove();
      world.stopIndividualWorld();
    } else {
      logger.warning("[FarmWorld] Welt " + worldId + " war nicht geladen; versuche dennoch Reset.");
    }
    boolean removed = universe.removeWorld(worldId);
    if (!removed) {
      logger.warning("[FarmWorld] Welt " + worldId + " konnte nicht entfernt werden.");
    }
    try {
      universe.loadWorld(worldId).join();
      logger.info("[FarmWorld] Welt " + worldId + " wurde neu geladen.");
      return true;
    } catch (RuntimeException ex) {
      logger.log(Level.WARNING, "[FarmWorld] Welt " + worldId + " konnte nicht neu geladen werden.", ex);
      return false;
    }
  }

  @Override
  public boolean loadPrefab(String prefabSpawnId, FarmWorldSpawn spawnPosition) {
    Universe universe = Universe.get();
    if (universe == null) {
      logger.warning("[FarmWorld] Universe ist nicht verfügbar; Prefab-Laden abgebrochen.");
      return false;
    }
    World world = universe.getWorld(spawnPosition.worldId);
    if (world == null) {
      logger.warning("[FarmWorld] Welt für Prefab-Laden nicht gefunden: " + spawnPosition.worldId + ".");
      return false;
    }
    PrefabStore prefabStore = PrefabStore.get();
    if (prefabStore == null) {
      logger.warning("[FarmWorld] PrefabStore ist nicht verfügbar; Prefab-Laden abgebrochen.");
      return false;
    }
    BlockSelection selection = prefabStore.getServerPrefab(prefabSpawnId);
    if (selection == null) {
      selection = prefabStore.getAssetPrefabFromAnyPack(prefabSpawnId);
    }
    if (selection == null) {
      Path path = prefabStore.findAssetPrefabPath(prefabSpawnId);
      if (path != null) {
        selection = prefabStore.getPrefab(path);
      }
    }
    if (selection == null) {
      logger.warning("[FarmWorld] Prefab nicht gefunden: " + prefabSpawnId + ".");
      return false;
    }
    Vector3i position = new Vector3i(
        (int) Math.round(spawnPosition.x),
        (int) Math.round(spawnPosition.y),
        (int) Math.round(spawnPosition.z));
    selection.placeNoReturn(world, position, world.getEntityStore());
    logger.info("[FarmWorld] Prefab " + prefabSpawnId + " wurde platziert.");
    return true;
  }
}
