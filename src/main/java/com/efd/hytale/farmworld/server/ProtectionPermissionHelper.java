package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.List;

public final class ProtectionPermissionHelper {
  private ProtectionPermissionHelper() {}

  public static String resolveActorName(Player player) {
    if (player == null) {
      return "unknown";
    }
    PlayerRef playerRef = player.getPlayerRef();
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
}
