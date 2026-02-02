package com.efd.hytale.farmworld.server;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ServerFarmWorldWorldAdapter extends LoggingFarmWorldWorldAdapter {
  private final Logger logger;

  public ServerFarmWorldWorldAdapter(Logger logger) {
    super(logger);
    this.logger = logger;
  }

  @Override
  public boolean resetWorld(String worldId, String instanceId) {
    if (worldId == null || worldId.isBlank()) {
      if (logger != null) {
        logger.warning("[FarmWorld] Reset abgebrochen: worldId fehlt.");
      }
      return false;
    }
    Universe universe = Universe.get();
    if (universe == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Reset abgebrochen: Universe nicht verfügbar.");
      }
      return false;
    }
    String resolvedWorldId = worldId.trim();
    String resolvedInstanceId = instanceId == null ? "" : instanceId.trim();
    int slashIndex = resolvedWorldId.indexOf('/');
    int colonIndex = resolvedWorldId.indexOf(':');
    int separatorIndex = slashIndex >= 0 && colonIndex >= 0
        ? Math.min(slashIndex, colonIndex)
        : Math.max(slashIndex, colonIndex);
    if (separatorIndex > 0 && separatorIndex < resolvedWorldId.length() - 1) {
      if (resolvedInstanceId.isBlank()) {
        resolvedInstanceId = resolvedWorldId.substring(separatorIndex + 1);
      }
      resolvedWorldId = resolvedWorldId.substring(0, separatorIndex);
    }
    World world = universe.getWorld(resolvedWorldId);
    if (world == null && !resolvedInstanceId.isBlank()) {
      String composedId = resolvedWorldId + "/" + resolvedInstanceId;
      world = universe.getWorld(composedId);
      if (world == null) {
        composedId = resolvedWorldId + ":" + resolvedInstanceId;
        world = universe.getWorld(composedId);
      }
      if (world != null) {
        resolvedWorldId = world.getName();
      }
    }
    Path worldPath = resolveWorldPath(universe, resolvedWorldId, world);
    if (world != null) {
      drainPlayers(world, universe.getDefaultWorld());
      try {
        boolean removed = universe.removeWorld(resolvedWorldId);
        if (!removed) {
          if (logger != null) {
            logger.warning("[FarmWorld] Welt konnte nicht entfernt werden: " + resolvedWorldId + ".");
          }
          return false;
        }
      } catch (RuntimeException ex) {
        if (logger != null) {
          logger.log(Level.WARNING, "[FarmWorld] Entfernen der Welt fehlgeschlagen: " + resolvedWorldId + ".", ex);
        }
        return false;
      }
    }
    if (worldPath != null) {
      try {
        deleteWorldPath(worldPath);
      } catch (IOException ex) {
        if (logger != null) {
          logger.log(Level.WARNING, "[FarmWorld] Speicherverzeichnis konnte nicht gelöscht werden: " + worldPath + ".", ex);
        }
        return false;
      }
    }
    try {
      universe.addWorld(resolvedWorldId).join();
      return true;
    } catch (CompletionException | IllegalArgumentException ex) {
      if (logger != null) {
        logger.log(Level.WARNING, "[FarmWorld] Welt konnte nicht neu erstellt werden: " + resolvedWorldId + ".", ex);
      }
      return false;
    }
  }

  @Override
  public boolean loadPrefab(String prefabSpawnId, com.efd.hytale.farmworld.shared.config.FarmWorldSpawn spawnPosition) {
    if (prefabSpawnId == null || prefabSpawnId.isBlank() || spawnPosition == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Laden abgebrochen: Prefab oder Spawn ist leer.");
      }
      return false;
    }
    Universe universe = Universe.get();
    if (universe == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Laden abgebrochen: Universe nicht verfügbar.");
      }
      return false;
    }
    String worldId = spawnPosition.worldId == null ? "" : spawnPosition.worldId.trim();
    String instanceId = spawnPosition.instanceId == null ? "" : spawnPosition.instanceId.trim();
    World world = universe.getWorld(worldId);
    if (world == null && !instanceId.isBlank()) {
      world = universe.getWorld(worldId + "/" + instanceId);
      if (world == null) {
        world = universe.getWorld(worldId + ":" + instanceId);
      }
    }
    if (world == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Laden abgebrochen: Welt nicht gefunden (" +
            worldId + "/" + instanceId + ").");
      }
      return false;
    }
    PrefabStore prefabStore = PrefabStore.get();
    BlockSelection prefab = prefabStore.getAssetPrefabFromAnyPack(prefabSpawnId);
    if (prefab == null) {
      try {
        prefab = prefabStore.getServerPrefab(prefabSpawnId);
      } catch (RuntimeException ex) {
        if (logger != null) {
          logger.log(Level.WARNING, "[FarmWorld] Prefab-Laden abgebrochen: Prefab nicht gefunden (" +
              prefabSpawnId + ").", ex);
        }
        return false;
      }
    }
    if (prefab == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Laden abgebrochen: Prefab nicht gefunden (" + prefabSpawnId + ").");
      }
      return false;
    }
    Vector3i position = new Vector3i(
        (int) Math.round(spawnPosition.x),
        (int) Math.round(spawnPosition.y),
        (int) Math.round(spawnPosition.z));
    try {
      Store<EntityStore> entityStore = world.getEntityStore().getStore();
      prefab.placeNoReturn(world, position, entityStore);
      return true;
    } catch (RuntimeException ex) {
      if (logger != null) {
        logger.log(Level.WARNING, "[FarmWorld] Prefab konnte nicht platziert werden.", ex);
      }
      return false;
    }
  }

  @Override
  public boolean savePrefab(String prefabSpawnId, com.efd.hytale.farmworld.shared.config.FarmWorldSpawn centerPosition,
      int radius) {
    if (prefabSpawnId == null || prefabSpawnId.isBlank() || centerPosition == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Speichern abgebrochen: Prefab oder Zentrum ist leer.");
      }
      return false;
    }
    Universe universe = Universe.get();
    if (universe == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Speichern abgebrochen: Universe nicht verfügbar.");
      }
      return false;
    }
    String worldId = centerPosition.worldId == null ? "" : centerPosition.worldId.trim();
    String instanceId = centerPosition.instanceId == null ? "" : centerPosition.instanceId.trim();
    World world = universe.getWorld(worldId);
    if (world == null && !instanceId.isBlank()) {
      world = universe.getWorld(worldId + "/" + instanceId);
      if (world == null) {
        world = universe.getWorld(worldId + ":" + instanceId);
      }
    }
    if (world == null) {
      if (logger != null) {
        logger.warning("[FarmWorld] Prefab-Speichern abgebrochen: Welt nicht gefunden (" +
            worldId + "/" + instanceId + ").");
      }
      return false;
    }
    int resolvedRadius = Math.max(1, radius);
    int centerX = (int) Math.round(centerPosition.x);
    int centerY = (int) Math.round(centerPosition.y);
    int centerZ = (int) Math.round(centerPosition.z);
    int minX = centerX - resolvedRadius;
    int maxX = centerX + resolvedRadius;
    int minZ = centerZ - resolvedRadius;
    int maxZ = centerZ + resolvedRadius;
    int minY = Math.max(ChunkUtil.MIN_Y, centerY - resolvedRadius);
    int maxY = Math.min(ChunkUtil.MIN_Y + ChunkUtil.HEIGHT - 1, centerY + resolvedRadius);

    BlockSelection selection = new BlockSelection();
    selection.setSelectionArea(new Vector3i(minX, minY, minZ), new Vector3i(maxX, maxY, maxZ));
    selection.setAnchorAtWorldPos(centerX, centerY, centerZ);

    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        for (int y = minY; y <= maxY; y++) {
          int blockId = world.getBlock(x, y, z);
          if (blockId == 0) {
            continue;
          }
          int rotation = world.getBlockRotationIndex(x, y, z);
          Holder<ChunkStore> blockComponent = world.getBlockComponentHolder(x, y, z);
          if (blockComponent != null) {
            selection.addBlockAtWorldPos(x, y, z, blockId, rotation, 0, 0, blockComponent);
          } else {
            selection.addBlockAtWorldPos(x, y, z, blockId, rotation, 0, 0);
          }
        }
      }
    }

    try {
      PrefabStore.get().saveServerPrefab(prefabSpawnId, selection, true);
      return true;
    } catch (RuntimeException ex) {
      if (logger != null) {
        logger.log(Level.WARNING, "[FarmWorld] Prefab konnte nicht gespeichert werden.", ex);
      }
      return false;
    }
  }

  private void drainPlayers(World source, World fallback) {
    if (source == null || fallback == null || source == fallback || source.getPlayerCount() == 0) {
      return;
    }
    try {
      source.drainPlayersTo(fallback).join();
    } catch (CompletionException ex) {
      if (logger != null) {
        logger.log(Level.WARNING, "[FarmWorld] Spieler konnten nicht umgezogen werden.", ex);
      }
    }
  }

  private Path resolveWorldPath(Universe universe, String worldId, World world) {
    if (world != null) {
      return world.getSavePath();
    }
    if (universe == null || worldId == null || worldId.isBlank()) {
      return null;
    }
    Path basePath = universe.getPath();
    if (basePath == null) {
      return null;
    }
    return basePath.resolve("worlds").resolve(worldId);
  }

  private void deleteWorldPath(Path path) throws IOException {
    if (path == null || !Files.exists(path)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder()).forEach(entry -> {
        try {
          Files.deleteIfExists(entry);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      });
    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof IOException ioException) {
        throw ioException;
      }
      throw ex;
    }
  }
}
