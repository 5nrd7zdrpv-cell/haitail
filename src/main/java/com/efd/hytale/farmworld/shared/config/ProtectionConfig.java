package com.efd.hytale.farmworld.shared.config;

public class ProtectionConfig {
  public boolean enabled = true;
  public int radius = 64;
  public FarmWorldSpawn center = null;
  public java.util.List<String> bypassRoles = java.util.List.of();
  public java.util.List<String> bypassPermissions = java.util.List.of();
  public String bypassPermission = "efd.farmworld.admin";
  public ProtectionActions actions = new ProtectionActions();
}
