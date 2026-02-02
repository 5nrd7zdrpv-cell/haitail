package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.List;

public final class ProtectionPermissionHelper {
  private ProtectionPermissionHelper() {}

  public static ResolvedWorld resolveWorld(World world, FarmWorldConfig config) {
    String worldName = world != null ? world.getName() : "";
    String fallbackInstanceId = config != null && config.farmWorld != null ? config.farmWorld.instanceId : "";
    String worldId = worldName == null ? "" : worldName;
    String instanceId = fallbackInstanceId == null ? "" : fallbackInstanceId;
    if (worldName != null) {
      int slash = worldName.indexOf('/');
      int colon = worldName.indexOf(':');
      int separator = slash >= 0 && colon >= 0 ? Math.min(slash, colon) : Math.max(slash, colon);
      if (separator > 0 && separator < worldName.length() - 1) {
        worldId = worldName.substring(0, separator);
        instanceId = worldName.substring(separator + 1);
      }
    }
    return new ResolvedWorld(worldId, instanceId);
  }

  public static String resolveActorName(Player player) {
    if (player == null) {
      return "unknown";
    }
    PlayerRef playerRef = PlayerRefResolver.fromPlayer(player);
    if (playerRef != null && playerRef.getUsername() != null && !playerRef.getUsername().isBlank()) {
      return playerRef.getUsername();
    }
    String displayName = player.getDisplayName();
    if (displayName != null && !displayName.isBlank()) {
      return displayName;
    }
    if (playerRef != null && playerRef.getUuid() != null) {
      return playerRef.getUuid().toString();
    }
    return "unknown";
  }

  public static List<String> collectBypassPermissions(Player player, FarmWorldConfig config) {
    if (player == null || config == null || config.protection == null) {
      return List.of();
    }
    List<String> permissions = new ArrayList<>();
    String legacyPermission = config.protection.bypassPermission;
    if (legacyPermission != null && !legacyPermission.isBlank() && player.hasPermission(legacyPermission)) {
      permissions.add(legacyPermission);
    }
    if (config.protection.bypassPermissions != null) {
      for (String permission : config.protection.bypassPermissions) {
        if (permission != null && !permission.isBlank() && player.hasPermission(permission)) {
          permissions.add(permission);
        }
      }
    }
    return permissions;
  }

  public static final class ResolvedWorld {
    public final String worldId;
    public final String instanceId;

    private ResolvedWorld(String worldId, String instanceId) {
      this.worldId = worldId == null ? "" : worldId;
      this.instanceId = instanceId == null ? "" : instanceId;
    }
  }
}
