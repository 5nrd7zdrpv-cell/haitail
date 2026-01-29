package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;
import com.efd.hytale.farmworld.daemon.services.CombatService;

import java.util.List;
import java.util.function.Supplier;

public final class CombatTagCommand implements ICommand {

    private final Supplier<FarmWorldConfig> cfg;
    private final CombatService combat;

    public CombatTagCommand(Supplier<FarmWorldConfig> cfg, CombatService combat) {
        this.cfg = cfg;
        this.combat = combat;
    }

    @Override public String root() { return "combat"; }
    @Override public List<String> path() { return List.of("tag"); }
    @Override public String help() { return "Tags combat: combat tag <playerId> [seconds] [reason...]"; }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) return "Usage: combat tag <playerId> [seconds] [reason...]\n";

        String playerId = args[0];
        if (!combat.isValidPlayerId(playerId)) return "Invalid playerId.\n";
        int seconds = 0;
        int idx = 1;

        if (args.length >= 2) {
            try {
                seconds = Integer.parseInt(args[1]);
                idx = 2;
            } catch (NumberFormatException ignored) {
                seconds = 0;
                idx = 1;
            }
        }

        String reason = "";
        if (args.length > idx) {
            StringBuilder sb = new StringBuilder();
            for (int i = idx; i < args.length; i++) {
                if (i > idx) sb.append(' ');
                sb.append(args[i]);
            }
            reason = sb.toString();
        } else {
            reason = "manual";
        }

        FarmWorldConfig c = cfg.get();
        if (!c.combat.enabled) return "[Combat] disabled\n";

        combat.tag(playerId, reason, seconds);
        return "[Combat] tagged " + playerId + " for " + (seconds > 0 ? seconds : c.combat.tagSeconds) + "s reason=" + reason + "\n";
    }
}
