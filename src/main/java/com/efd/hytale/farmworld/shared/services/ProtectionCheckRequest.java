package com.efd.hytale.farmworld.shared.services;

public class ProtectionCheckRequest {
  public final String actorId;
  public final ProtectionAction action;
  public final double distanceFromSpawn;
  public final boolean hasBypassPermission;
  public final String worldId;
  public final String instanceId;

  public ProtectionCheckRequest(
      String actorId,
      ProtectionAction action,
      double distanceFromSpawn,
      boolean hasBypassPermission,
      String worldId,
      String instanceId) {
    this.actorId = actorId;
    this.action = action;
    this.distanceFromSpawn = distanceFromSpawn;
    this.hasBypassPermission = hasBypassPermission;
    this.worldId = worldId;
    this.instanceId = instanceId;
  }
}
