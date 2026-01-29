package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.config.ConfigValidator;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
  private static final String DEFAULT_CONFIG_PATH = "defaults/farmworld.json";
  private static final String OVERRIDE_CONFIG_PATH = "config/farmworld.json";

  private final Logger logger;
  private final Gson gson;

  public ConfigManager(Logger logger) {
    this.logger = logger;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public FarmWorldConfig load() {
    JsonObject defaults = loadDefaults();
    JsonObject overrides = loadOverrides();
    JsonObject merged = merge(defaults, overrides);
    FarmWorldConfig config = gson.fromJson(merged, FarmWorldConfig.class);
    List<String> warnings = ConfigValidator.validate(config);
    for (String warning : warnings) {
      logger.warning(warning);
    }
    return config;
  }

  public void save(FarmWorldConfig config) {
    JsonObject json = gson.toJsonTree(config).getAsJsonObject();
    Path overridePath = Path.of(OVERRIDE_CONFIG_PATH);
    try {
      Files.createDirectories(overridePath.getParent());
      Files.writeString(overridePath, gson.toJson(json), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Konnte Konfiguration nicht schreiben: " + overridePath.toAbsolutePath(), ex);
    }
  }

  private JsonObject loadDefaults() {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
      if (inputStream == null) {
        logger.warning("[FarmWorld] Standardkonfiguration nicht gefunden: " + DEFAULT_CONFIG_PATH + ". Verwende leere Defaults.");
        return new JsonObject();
      }
      return JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "[FarmWorld] Standardkonfiguration konnte nicht gelesen werden; verwende leere Defaults.", ex);
      return new JsonObject();
    }
  }

  private JsonObject loadOverrides() {
    Path overridePath = Path.of(OVERRIDE_CONFIG_PATH);
    if (!Files.exists(overridePath)) {
      logger.info("[FarmWorld] Keine Override-Konfiguration gefunden: " + overridePath.toAbsolutePath());
      return new JsonObject();
    }
    try (InputStream inputStream = Files.newInputStream(overridePath)) {
      return JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "[FarmWorld] Override-Konfiguration konnte nicht gelesen werden; ignoriere Overrides.", ex);
      return new JsonObject();
    }
  }

  private JsonObject merge(JsonObject base, JsonObject override) {
    JsonObject result = base.deepCopy();
    for (String key : override.keySet()) {
      JsonElement overrideValue = override.get(key);
      if (overrideValue.isJsonObject() && result.has(key) && result.get(key).isJsonObject()) {
        JsonObject mergedChild = merge(result.getAsJsonObject(key), overrideValue.getAsJsonObject());
        result.add(key, mergedChild);
      } else {
        result.add(key, overrideValue);
      }
    }
    return result;
  }
}
