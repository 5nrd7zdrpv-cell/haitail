package com.efd.hytale.farmworld.shared.config;

public class FarmWorldConfig {
  public FarmWorldSettings farmWorld = new FarmWorldSettings();
  public long lastResetEpochSeconds = 0L;
  public ProtectionConfig protection = new ProtectionConfig();
  public CombatConfig combat = new CombatConfig();
}
