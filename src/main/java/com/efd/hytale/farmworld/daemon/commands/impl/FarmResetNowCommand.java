package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.FarmWorldService;

import java.util.List;

public final class FarmResetNowCommand implements ICommand {
    private final FarmWorldService farm;
    public FarmResetNowCommand(FarmWorldService farm) { this.farm = farm; }

    @Override public String root() { return "farm"; }
    @Override public List<String> path() { return List.of("reset", "now"); }
    @Override public String help() { return "Triggers an immediate farm world reset."; }

    @Override public String execute(String[] args) {
        farm.resetNow("Manual Command");
        return "";
    }
}
