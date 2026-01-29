package com.efd.hytale.farmworld.shared.config;

public class ProtectionConfig {
  public boolean enabled = true;
  public int radius = 64;
  public String bypassPermission = "efd.farmworld.admin";
  public ProtectionActions actions = new ProtectionActions();
}
