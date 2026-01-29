package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.services.ProtectionService;

import java.util.List;

public final class ProtectStatusCommand implements ICommand {

    private final ProtectionService protection;

    public ProtectStatusCommand(ProtectionService protection) {
        this.protection = protection;
    }

    @Override public String root() { return "protect"; }
    @Override public List<String> path() { return List.of("status"); }
    @Override public String help() { return "Shows protection engine info."; }

    @Override public String execute(String[] args) {
        return "[Protect] Engine ready. Use: protect test <ACTION> <x> <y> <z> [perm=true|false]\n";
    }
}
