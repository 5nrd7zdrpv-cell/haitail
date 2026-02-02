package com.efd.hytale.farmworld.server;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
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
    World world = universe.getWorld(resolvedWorldId);
    if (world == null && instanceId != null && !instanceId.isBlank()) {
      String composedId = resolvedWorldId + "/" + instanceId.trim();
      world = universe.getWorld(composedId);
      if (world == null) {
        composedId = resolvedWorldId + ":" + instanceId.trim();
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
      prefab = prefabStore.getServerPrefab(prefabSpawnId);
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
