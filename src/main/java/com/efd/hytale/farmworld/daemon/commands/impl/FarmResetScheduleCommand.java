package com.efd.hytale.farmworld.daemon.commands.impl;

import com.efd.hytale.farmworld.daemon.commands.ICommand;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;
import com.efd.hytale.farmworld.daemon.services.FarmWorldService;
import com.efd.hytale.farmworld.daemon.services.ResetSchedule;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

public final class FarmResetScheduleCommand implements ICommand {
    private final Supplier<FarmWorldConfig> cfg;
    private final FarmWorldService farm;
    private final ZoneId zone;

    public FarmResetScheduleCommand(Supplier<FarmWorldConfig> cfg, FarmWorldService farm, ZoneId zone) {
        this.cfg = cfg; this.farm = farm; this.zone = zone;
    }

    @Override public String root() { return "farm"; }
    @Override public List<String> path() { return List.of("reset", "schedule"); }
    @Override public String help() { return "Shows reset policy, last reset, and next reset time."; }

    @Override public String execute(String[] args) {
        FarmWorldConfig c = cfg.get();
        Instant last = Instant.ofEpochSecond(c.lastResetEpochSeconds);
        Instant next = farm.nextResetInstant();

        StringBuilder sb = new StringBuilder();
        sb.append("[FarmWorld] World: ").append(c.farmWorld.name).append("\n");
        sb.append("[FarmWorld] IntervalDays: ").append(c.farmWorld.resetIntervalDays).append("\n");
        sb.append("[FarmWorld] ResetAt policy: ").append(c.farmWorld.resetAt == null ? "" : ResetSchedule.describePolicy(c.farmWorld.resetAt)).append("\n");
        sb.append("[FarmWorld] LastReset: ").append(last.atZone(zone)).append("\n");
        sb.append("[FarmWorld] NextReset: ").append(next.atZone(zone)).append("\n");
        return sb.toString();
    }
}
