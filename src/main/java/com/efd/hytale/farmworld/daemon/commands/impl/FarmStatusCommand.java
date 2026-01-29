package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.FarmWorldService;

import java.util.List;

public final class FarmStatusCommand implements ICommand {
    private final FarmWorldService farm;
    public FarmStatusCommand(FarmWorldService farm) { this.farm = farm; }

    @Override public String root() { return "farm"; }
    @Override public List<String> path() { return List.of("status"); }
    @Override public String help() { return "Shows reset lock state."; }

    @Override public String execute(String[] args) {
        return "[FarmWorld] resetInProgress=" + farm.isResetInProgress() + "\n";
    }
}
