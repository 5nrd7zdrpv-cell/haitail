package com.efd.hytale.farmworld.shared.services;

public class ProtectionCheckRequest {
  public final ProtectionAction action;
  public final double distanceFromSpawn;
  public final boolean hasBypassPermission;

  public ProtectionCheckRequest(
      ProtectionAction action,
      double distanceFromSpawn,
      boolean hasBypassPermission) {
    this.action = action;
    this.distanceFromSpawn = distanceFromSpawn;
    this.hasBypassPermission = hasBypassPermission;
  }
}
