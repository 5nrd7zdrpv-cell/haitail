package com.efd.hytale.farmworld.shared.services;

public class ProtectionCheckRequest {
  public final String actorId;
  public final ProtectionAction action;
  public final double x;
  public final double y;
  public final double z;
  public final double fallbackCenterX;
  public final double fallbackCenterY;
  public final double fallbackCenterZ;
  public final boolean hasBypassPermission;
  public final String worldId;
  public final String instanceId;

  public ProtectionCheckRequest(
      String actorId,
      ProtectionAction action,
      double x,
      double y,
      double z,
      double fallbackCenterX,
      double fallbackCenterY,
      double fallbackCenterZ,
      boolean hasBypassPermission,
      String worldId,
      String instanceId) {
    this.actorId = actorId;
    this.action = action;
    this.x = x;
    this.y = y;
    this.z = z;
    this.fallbackCenterX = fallbackCenterX;
    this.fallbackCenterY = fallbackCenterY;
    this.fallbackCenterZ = fallbackCenterZ;
    this.hasBypassPermission = hasBypassPermission;
    this.worldId = worldId;
    this.instanceId = instanceId;
  }
}
