package com.efd.hytale.farmworld.server;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
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
