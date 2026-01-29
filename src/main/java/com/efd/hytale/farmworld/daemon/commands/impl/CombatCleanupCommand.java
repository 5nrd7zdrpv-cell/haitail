package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.CombatService;

import java.util.List;

public final class CombatCleanupCommand implements ICommand {

    private final CombatService combat;

    public CombatCleanupCommand(CombatService combat) {
        this.combat = combat;
    }

    @Override public String root() { return "combat"; }
    @Override public List<String> path() { return List.of("cleanup"); }
    @Override public String help() { return "Purges expired combat entries."; }

    @Override
    public String execute(String[] args) {
        combat.cleanupExpired();
        return "[Combat] cleanup done\n";
    }
}
