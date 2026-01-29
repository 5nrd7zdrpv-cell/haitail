package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.CombatService;

import java.util.List;

public final class CombatCanWarpCommand implements ICommand {

    private final CombatService combat;

    public CombatCanWarpCommand(CombatService combat) {
        this.combat = combat;
    }

    @Override public String root() { return "combat"; }
    @Override public List<String> path() { return List.of("canwarp"); }
    @Override public String help() { return "Checks warp allowance: combat canwarp <playerId>"; }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) return "Usage: combat canwarp <playerId>\n";
        String id = args[0];
        if (!combat.isValidPlayerId(id)) return "Invalid playerId.\n";
        boolean ok = combat.canWarp(id);
        if (ok) return "[Combat] canWarp=true\n";
        int rem = combat.getRemainingSeconds(id);
        int pen = combat.getPenaltyRemainingSeconds(id);
        return "[Combat] canWarp=false (combatRemaining=" + rem + "s, penaltyRemaining=" + pen + "s)\n";
    }
}
