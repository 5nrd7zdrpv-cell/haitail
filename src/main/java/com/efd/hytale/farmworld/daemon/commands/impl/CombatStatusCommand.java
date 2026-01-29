package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.CombatService;

import java.util.List;

public final class CombatStatusCommand implements ICommand {

    private final CombatService combat;

    public CombatStatusCommand(CombatService combat) {
        this.combat = combat;
    }

    @Override public String root() { return "combat"; }
    @Override public List<String> path() { return List.of("status"); }
    @Override public String help() { return "Shows combat state: combat status <playerId>"; }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) return "Usage: combat status <playerId>\n";
        String id = args[0];

        CombatService.CombatState s = combat.getState(id);
        boolean inCombat = combat.isInCombat(id);
        int rem = combat.getRemainingSeconds(id);
        boolean pen = combat.hasPenalty(id);
        int penRem = combat.getPenaltyRemainingSeconds(id);

        StringBuilder sb = new StringBuilder();
        sb.append("[Combat] player=").append(id).append("\n");
        sb.append("[Combat] inCombat=").append(inCombat).append(" remaining=").append(rem).append("s\n");
        sb.append("[Combat] penalty=").append(pen).append(" remaining=").append(penRem).append("s\n");
        if (s != null && s.lastReason() != null && !s.lastReason().isBlank()) {
            sb.append("[Combat] lastReason=").append(s.lastReason()).append("\n");
        }
        return sb.toString();
    }
}
