package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;
import com.efd.hytale.farmworld.daemon.services.CombatService;

import java.util.List;
import java.util.function.Supplier;

public final class CombatQuitCommand implements ICommand {

    private final Supplier<FarmWorldConfig> cfg;
    private final CombatService combat;

    public CombatQuitCommand(Supplier<FarmWorldConfig> cfg, CombatService combat) {
        this.cfg = cfg;
        this.combat = combat;
    }

    @Override public String root() { return "combat"; }
    @Override public List<String> path() { return List.of("quit"); }
    @Override public String help() { return "Simulates quit: combat quit <playerId>"; }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) return "Usage: combat quit <playerId>\n";
        FarmWorldConfig c = cfg.get();
        if (!c.combat.enabled) return "[Combat] disabled\n";
        String id = args[0];
        String res = combat.onPlayerQuit(id);
        return "[Combat] " + res + "\n";
    }
}
