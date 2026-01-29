package com.efd.hytale.farmworld.shared.services;

public class ProtectionZoneResult {
  public final String name;
  public final double x;
  public final double y;
  public final double z;
  public final double radius;
  public final double distance;
  public final boolean inside;
  public final boolean usesPoint;

  public ProtectionZoneResult(
      String name,
      double x,
      double y,
      double z,
      double radius,
      double distance,
      boolean inside,
      boolean usesPoint) {
    this.name = name;
    this.x = x;
    this.y = y;
    this.z = z;
    this.radius = radius;
    this.distance = distance;
    this.inside = inside;
    this.usesPoint = usesPoint;
  }
}
