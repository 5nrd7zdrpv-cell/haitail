package com.efd.hytale.farmworld.shared.config;

public class ProtectionConfig {
  public boolean enabled = true;
  public boolean debugLog = false;
  public int radius = 64;
  public FarmWorldSpawn center = null;
  public java.util.List<ProtectionPoint> points = new java.util.ArrayList<>();
  public java.util.List<String> bypassRoles = java.util.List.of();
  public java.util.List<String> bypassPermissions = java.util.List.of();
  public String bypassPermission = "farmworld.admin";
  public ProtectionActions actions = new ProtectionActions();
}
