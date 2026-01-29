package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.config.FarmWorldSpawn;
import com.efd.hytale.farmworld.shared.config.ProtectionConfig;
import com.efd.hytale.farmworld.shared.services.ProtectionAction;
import com.efd.hytale.farmworld.shared.services.ProtectionCheckRequest;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ProtectionBridge {
  private final FarmWorldConfig config;
  private final ProtectionService protectionService;

  public ProtectionBridge(FarmWorldConfig config, ProtectionService protectionService) {
    this.config = config;
    this.protectionService = protectionService;
  }

  public boolean onBlockPlace(
      String actorId,
      double x,
      double y,
      double z,
      String worldId,
      String instanceId,
      Collection<String> permissions,
      Collection<String> roles) {
    return check(actorId, ProtectionAction.PLACE, x, y, z, worldId, instanceId, permissions, roles);
  }

  public boolean onBlockBreak(
      String actorId,
      double x,
      double y,
      double z,
      String worldId,
      String instanceId,
      Collection<String> permissions,
      Collection<String> roles) {
    return check(actorId, ProtectionAction.BREAK_BLOCK, x, y, z, worldId, instanceId, permissions, roles);
  }

  public boolean onInteract(
      String actorId,
      double x,
      double y,
      double z,
      String worldId,
      String instanceId,
      Collection<String> permissions,
      Collection<String> roles) {
    return check(actorId, ProtectionAction.INTERACT, x, y, z, worldId, instanceId, permissions, roles);
  }

  public boolean shouldNotify(String actorId) {
    return protectionService.shouldNotify(actorId);
  }

  private boolean check(
      String actorId,
      ProtectionAction action,
      double x,
      double y,
      double z,
      String worldId,
      String instanceId,
      Collection<String> permissions,
      Collection<String> roles) {
    ProtectionConfig protection = config.protection;
    FarmWorldSpawn center = protection.center != null ? protection.center : config.farmWorld.spawn;
    String centerWorld = resolveWorldId(center.worldId, config.farmWorld.worldId);
    String centerInstance = resolveWorldId(center.instanceId, config.farmWorld.instanceId);
    String resolvedWorldId = resolveWorldId(worldId, config.farmWorld.worldId);
    String resolvedInstanceId = resolveWorldId(instanceId, config.farmWorld.instanceId);
    if (!centerWorld.equalsIgnoreCase(resolvedWorldId) || !centerInstance.equalsIgnoreCase(resolvedInstanceId)) {
      return true;
    }
    boolean hasBypass = hasBypass(permissions, roles);
    ProtectionCheckRequest request = new ProtectionCheckRequest(
        actorId,
        action,
        x,
        y,
        z,
        center.x,
        center.y,
        center.z,
        hasBypass,
        resolvedWorldId,
        resolvedInstanceId);
    return protectionService.isActionAllowed(request);
  }

  private boolean hasBypass(Collection<String> permissions, Collection<String> roles) {
    Set<String> normalizedPermissions = normalize(permissions);
    Set<String> normalizedRoles = normalize(roles);
    String legacyPermission = config.protection.bypassPermission == null ? "" : config.protection.bypassPermission;
    if (!legacyPermission.isBlank()
        && normalizedPermissions.contains(legacyPermission.toLowerCase(Locale.ROOT))) {
      return true;
    }
    for (String permission : config.protection.bypassPermissions) {
      if (normalizedPermissions.contains(permission.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    for (String role : config.protection.bypassRoles) {
      if (normalizedRoles.contains(role.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private Set<String> normalize(Collection<String> values) {
    if (values == null) {
      return Set.of();
    }
    Set<String> normalized = new HashSet<>();
    for (String value : values) {
      if (value != null) {
        normalized.add(value.toLowerCase(Locale.ROOT));
      }
    }
    return normalized;
  }

  private String resolveWorldId(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value;
  }

}
