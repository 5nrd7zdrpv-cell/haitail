package com.efd.hytale.farmworld.shared.services;

public class CombatQuitResult {
  public enum QuitPolicy {
    NONE,
    TEMP_BAN,
    KILL
  }

  private final QuitPolicy policy;
  private final int penaltySeconds;

  private CombatQuitResult(QuitPolicy policy, int penaltySeconds) {
    this.policy = policy;
    this.penaltySeconds = penaltySeconds;
  }

  public static CombatQuitResult noPenalty() {
    return new CombatQuitResult(QuitPolicy.NONE, 0);
  }

  public static CombatQuitResult fromPolicy(String policyRaw, int penaltySeconds) {
    QuitPolicy policy = parse(policyRaw);
    return new CombatQuitResult(policy, Math.max(penaltySeconds, 0));
  }

  private static QuitPolicy parse(String policyRaw) {
    if (policyRaw == null) {
      return QuitPolicy.NONE;
    }
    try {
      return QuitPolicy.valueOf(policyRaw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return QuitPolicy.NONE;
    }
  }

  public QuitPolicy getPolicy() {
    return policy;
  }

  public int getPenaltySeconds() {
    return penaltySeconds;
  }
}
