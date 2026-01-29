package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.List;

public final class ProtectionPermissionHelper {
  private ProtectionPermissionHelper() {}

  public static List<String> collectBypassPermissions(Player player, FarmWorldConfig config) {
    List<String> permissions = new ArrayList<>();
    if (player == null || config == null) {
      return permissions;
    }
    String legacy = config.protection.bypassPermission;
    if (legacy != null && !legacy.isBlank() && player.hasPermission(legacy)) {
      permissions.add(legacy);
    }
    for (String permission : config.protection.bypassPermissions) {
      if (permission != null && player.hasPermission(permission)) {
        permissions.add(permission);
      }
    }
    return permissions;
  }

  public static String resolveActorName(Player player) {
    if (player == null) {
      return "Unbekannt";
    }
    PlayerRef ref = player.getPlayerRef();
    if (ref != null && ref.getUsername() != null && !ref.getUsername().isBlank()) {
      return ref.getUsername();
    }
    String name = player.getDisplayName();
    return name != null && !name.isBlank() ? name : "Unbekannt";
  }
}
