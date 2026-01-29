package com.efd.hytale.farmworld.daemon.services;

import com.efd.hytale.farmworld.daemon.config.ConfigManager;
import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FarmWorldService {

    private final ConfigManager configManager;
    private final String configFile;
    private final Path worldsRoot;
    private final ZoneId zone;

    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> scheduledTask;

    private volatile FarmWorldConfig config;
    private final AtomicBoolean resetInProgress = new AtomicBoolean(false);

    public FarmWorldService(ConfigManager configManager, String configFile, Path worldsRoot, ZoneId zone) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.configFile = Objects.requireNonNull(configFile, "configFile");
        this.worldsRoot = Objects.requireNonNull(worldsRoot, "worldsRoot");
        this.zone = Objects.requireNonNull(zone, "zone");

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "farmworld-daemon-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public void start(FarmWorldConfig cfg) {
        this.config = cfg;
        scheduleNext();
    }

    public void stop() {
        if (scheduledTask != null) scheduledTask.cancel(false);
        scheduler.shutdownNow();
    }

    public boolean isResetInProgress() { return resetInProgress.get(); }

    public Instant nextResetInstant() {
        Instant last = Instant.ofEpochSecond(config.lastResetEpochSeconds);
        return ResetSchedule.computeNextReset(last, config.farmWorld.resetIntervalDays, config.farmWorld.resetAt, zone);
    }

    public synchronized void scheduleNext() {
        Instant next = nextResetInstant();
        long delayMs = Math.max(0, next.toEpochMilli() - System.currentTimeMillis());

        if (scheduledTask != null) scheduledTask.cancel(false);

        scheduledTask = scheduler.schedule(() -> {
            try { resetNow("Scheduled Reset"); }
            finally { scheduleNext(); }
        }, delayMs, TimeUnit.MILLISECONDS);

        System.out.println("[FarmWorld] Next reset: " + next.atZone(zone));
    }

    public void resetNow(String reason) {
        if (!resetInProgress.compareAndSet(false, true)) {
            System.out.println("[FarmWorld] Reset already in progress.");
            return;
        }

        String worldName = config.farmWorld.name;
        System.out.println("[FarmWorld] Reset start (" + worldName + ") (" + reason + ")");

        try {
            Path worldDir = worldsRoot.resolve(worldName);

            deleteDirectoryRecursive(worldDir);

            Files.createDirectories(worldDir);
            Files.createDirectories(worldDir.resolve("prefabs"));

            int sx = config.farmWorld.spawn.x;
            int sy = config.farmWorld.spawn.y;
            int sz = config.farmWorld.spawn.z;

            String spawnJson = "{\n  \"x\": " + sx + ",\n  \"y\": " + sy + ",\n  \"z\": " + sz + "\n}\n";
            Files.writeString(worldDir.resolve("spawn.json"), spawnJson, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String prefabPath = config.farmWorld.spawn.prefabPath;
            int px = sx + config.farmWorld.spawn.prefabOffsetX;
            int py = sy + config.farmWorld.spawn.prefabOffsetY;
            int pz = sz + config.farmWorld.spawn.prefabOffsetZ;

            if (prefabPath != null && !prefabPath.isBlank()) {
                Path prefabSrc = Path.of(prefabPath);
                Path prefabDst = worldDir.resolve("prefabs").resolve(prefabSrc.getFileName().toString());

                if (Files.exists(prefabSrc)) {
                    Files.copy(prefabSrc, prefabDst, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[FarmWorld] Prefab deployed: " + prefabDst);
                } else {
                    System.out.println("[FarmWorld] Prefab not found on disk (ok for now): " + prefabSrc.toAbsolutePath());
                }

                String placement = "world=" + worldName + " prefab=" + prefabPath + " at (" + px + "," + py + "," + pz + ")\n";
                Files.writeString(worldDir.resolve("prefab-placement.log"), placement, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.println("[FarmWorld] Prefab placement recorded: " + placement.trim());
            }

            config.lastResetEpochSeconds = Instant.now().getEpochSecond();
            configManager.save(configFile, config);

            System.out.println("[FarmWorld] Reset fertig (" + worldName + ")");
        } catch (Exception e) {
            System.out.println("[FarmWorld] Reset FAIL: " + e.getMessage());
            e.printStackTrace(System.err);
        } finally {
            resetInProgress.set(false);
        }
    }

    private static void deleteDirectoryRecursive(Path dir) throws IOException {
        if (dir == null) return;
        if (Files.notExists(dir)) return;

        Path normalized = dir.toAbsolutePath().normalize();
        if (normalized.getNameCount() < 2) {
            throw new IOException("Refusing to delete suspicious path: " + normalized);
        }

        try (var stream = Files.walk(normalized)) {
            stream.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ex) { throw new RuntimeException(ex); }
                    });
        }
    }
}
