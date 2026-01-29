package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import java.util.UUID;
import java.util.logging.Logger;

public class CombatBridge {
  private final CombatTagService combatTagService;
  private final Logger logger;

  public CombatBridge(CombatTagService combatTagService, Logger logger) {
    this.combatTagService = combatTagService;
    this.logger = logger;
  }

  public void onDamage(UUID playerId, long durationSeconds, String playerName) {
    combatTagService.recordPlayer(playerId, playerName);
    combatTagService.tag(playerId, durationSeconds);
    String durationLabel = durationSeconds > 0 ? durationSeconds + "s" : "Standarddauer";
    logger.info("[FarmWorld] Kampftag gesetzt für " + playerName + " (" + durationLabel + ").");
  }

  public void onDisconnect(UUID playerId, String playerName) {
    if (combatTagService.isInCombat(playerId)) {
      logger.warning("[FarmWorld] Combat-Logout: " + playerName);
    }
  }
}
