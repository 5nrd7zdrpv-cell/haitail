package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;
import com.efd.hytale.farmworld.daemon.services.ProtectionService;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class ProtectTestCommand implements ICommand {

    private final Supplier<FarmWorldConfig> cfg;
    private final ProtectionService protection;

    public ProtectTestCommand(Supplier<FarmWorldConfig> cfg, ProtectionService protection) {
        this.cfg = cfg;
        this.protection = protection;
    }

    @Override public String root() { return "protect"; }
    @Override public List<String> path() { return List.of("test"); }
    @Override public String help() { return "Tests: protect test <ACTION> <x> <y> <z> [perm=true|false]"; }

    @Override
    public String execute(String[] args) {
        if (args.length < 4) {
            return "Usage: protect test <ACTION> <x> <y> <z> [perm=true|false]\n" +
                   "Actions: PLACE, BREAK, INTERACT, DAMAGE, EXPLOSION, FIRE_SPREAD, LIQUID\n";
        }

        try {
            ProtectionService.Action action = ProtectionService.Action.valueOf(args[0].toUpperCase(Locale.ROOT));
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);

            boolean perm = false;
            if (args.length >= 5) {
                perm = Boolean.parseBoolean(args[4].replace("perm=", "").trim());
            }

            FarmWorldConfig c = cfg.get();
            String world = c.farmWorld.name;

            ProtectionService.Decision d = protection.isBlocked(action, world, x, y, z, perm);

            return "[Protect] action=" + action +
                    " pos=(" + x + "," + y + "," + z + ")" +
                    " perm=" + perm +
                    " -> blocked=" + d.blocked() +
                    (d.reason().isBlank() ? "" : " reason=" + d.reason()) +
                    "\n";
        } catch (NumberFormatException nfe) {
            return "Invalid coordinates. Expected integers.\n";
        } catch (IllegalArgumentException iae) {
            return "Invalid ACTION. Use: PLACE, BREAK, INTERACT, DAMAGE, EXPLOSION, FIRE_SPREAD, LIQUID\n";
        }
    }

}
