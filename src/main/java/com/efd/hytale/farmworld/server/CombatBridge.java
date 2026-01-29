package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.services.CombatTagService;
import java.util.logging.Logger;

public class CombatBridge {
  private final CombatTagService combatTagService;
  private final Logger logger;

  public CombatBridge(CombatTagService combatTagService, Logger logger) {
    this.combatTagService = combatTagService;
    this.logger = logger;
  }

  public void onDamage(String playerId, long durationSeconds, String reason) {
    combatTagService.tag(playerId, durationSeconds, reason);
    String durationLabel = durationSeconds > 0 ? durationSeconds + "s" : "default duration";
    logger.info("Combat tag applied to " + playerId + " for " + durationLabel + " (reason=" + reason + ").");
  }

  public void onDisconnect(String playerId) {
    if (combatTagService.isInCombat(playerId)) {
      logger.warning("Combat log detected for " + playerId + ".");
    }
  }
}
