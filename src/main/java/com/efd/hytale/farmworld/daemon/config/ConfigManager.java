package com.efd.hytale.farmworld.daemon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

public final class ConfigManager {

    private final Path configDir;
    private final Gson gson;

    public ConfigManager(Path configDir) {
        this.configDir = Objects.requireNonNull(configDir, "configDir");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public <T> T loadOrCreateDefault(String fileName, Class<T> type, Supplier<T> defaultSupplier) {
        try {
            Files.createDirectories(configDir);
            Path file = configDir.resolve(fileName);
            if (Files.notExists(file)) {
                T def = defaultSupplier.get();
                save(fileName, def);
                return def;
            }
            return loadTyped(fileName, type, defaultSupplier);
        } catch (Exception e) {
            try {
                Path file = configDir.resolve(fileName);
                if (Files.exists(file)) Files.move(file, configDir.resolve(fileName + ".broken"));
            } catch (IOException ignored) {}
            T def = defaultSupplier.get();
            save(fileName, def);
            return def;
        }
    }

    public <T> T loadTyped(String fileName, Class<T> type, Supplier<T> fallback) throws IOException {
        Path file = configDir.resolve(fileName);
        if (Files.notExists(file)) return fallback.get();
        try (Reader r = Files.newBufferedReader(file)) {
            T obj = gson.fromJson(r, type);
            return obj != null ? obj : fallback.get();
        }
    }

    public void save(String fileName, Object obj) {
        try {
            Files.createDirectories(configDir);
            Path file = configDir.resolve(fileName);
            try (Writer w = Files.newBufferedWriter(file)) {
                gson.toJson(obj, w);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + fileName, e);
        }
    }
}
