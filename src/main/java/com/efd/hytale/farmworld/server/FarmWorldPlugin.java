package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandRegistrySelfTest;
import com.efd.hytale.farmworld.shared.commands.DefaultCommands;
import com.efd.hytale.farmworld.shared.config.ConfigValidator;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.efd.hytale.farmworld.shared.services.FarmWorldWorldAdapter;
import com.efd.hytale.farmworld.server.commands.FarmWorldCommands;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FarmWorldPlugin extends JavaPlugin {
  private final Logger logger = Logger.getLogger(FarmWorldPlugin.class.getName());

  private ScheduledExecutorService executorService;
  private FarmWorldConfig config;
  private FarmWorldService farmWorldService;
  private CombatTagService combatService;
  private ProtectionService protectionService;
  private CommandBridge commandBridge;
  private CombatBridge combatBridge;
  private ProtectionBridge protectionBridge;

  public FarmWorldPlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  public void setup() {
    logger.info("[FarmWorld] Plugin wird eingerichtet...");
    ConfigManager configManager = new ConfigManager(logger);
    config = configManager.load();
    for (String issue : ConfigValidator.validateSevere(config)) {
      logger.severe(issue);
    }

    executorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "FarmWorldScheduler");
      thread.setDaemon(true);
      return thread;
    });

    ExecutorScheduler scheduler = new ExecutorScheduler(executorService);
    AsyncConfigStore configStore = new AsyncConfigStore(configManager, executorService, logger);
    FarmWorldWorldAdapter worldAdapter = new LoggingFarmWorldWorldAdapter(logger);
    farmWorldService = new FarmWorldService(config, scheduler, configStore, worldAdapter, logger);
    combatService = new CombatTagService(config.combat);
    protectionService = new ProtectionService(config.protection, logger);
    combatBridge = new CombatBridge(combatService, logger);
    protectionBridge = new ProtectionBridge(config, protectionService);

    logger.info("[FarmWorld] Plugin-Einrichtung abgeschlossen.");
  }

  @Override
  public void start() {
    logger.info("[FarmWorld] Plugin wird gestartet...");
    CommandRegistry registry = new CommandRegistry();
    new DefaultCommands().register(registry, farmWorldService, combatService, protectionService, config);
    commandBridge = new CommandBridge(registry);
    String adminPermission = getBasePermission() + ".admin";
    getCommandRegistry().registerCommand(
        FarmWorldCommands.createFarmCommand(registry, adminPermission, farmWorldService, config));
    logger.info("[FarmWorld] Root-Befehl registriert: /farm.");
    getCommandRegistry().registerCommand(
        FarmWorldCommands.createProtectCommand(registry, adminPermission, config));
    logger.info("[FarmWorld] Root-Befehl registriert: /protect.");
    getCommandRegistry().registerCommand(
        FarmWorldCommands.createCombatCommand(registry, adminPermission, combatService));
    logger.info("[FarmWorld] Root-Befehl registriert: /combat.");
    String commandList = registry.all().stream()
        .map(command -> command.name)
        .collect(Collectors.joining(", "));
    logger.info("[FarmWorld] Registrierte FarmWorld-Befehle: " + commandList + ".");
    if (farmWorldService != null) {
      farmWorldService.start();
    }

    registerEventHandlers();
    runSelfTest();
    logger.info("[FarmWorld] Plugin gestartet.");
  }

  @Override
  public void shutdown() {
    logger.info("[FarmWorld] Plugin wird beendet...");
    if (farmWorldService != null) {
      farmWorldService.stop();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
    if (combatService != null) {
      combatService.clearAll();
    }
    logger.info("[FarmWorld] Plugin wurde beendet.");
  }

  public CommandBridge getCommandBridge() {
    return commandBridge;
  }

  public CombatTagService getCombatService() {
    return combatService;
  }

  public ProtectionService getProtectionService() {
    return protectionService;
  }

  public CombatBridge getCombatBridge() {
    return combatBridge;
  }

  public ProtectionBridge getProtectionBridge() {
    return protectionBridge;
  }

  private void runSelfTest() {
    List<String> issues = CommandRegistrySelfTest.run();
    if (issues.isEmpty()) {
      logger.info("[FarmWorld] Command-Registry-Selbsttest bestanden.");
      return;
    }
    for (String issue : issues) {
      logger.warning("[FarmWorld] Selbsttest-Hinweis: " + issue);
    }
    logger.log(Level.INFO, "[FarmWorld] Selbsttest abgeschlossen mit {0} Hinweis(en).", issues.size());
  }

  private void registerEventHandlers() {
    getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
    getEventRegistry().register(PlayerReadyEvent.class, this::onPlayerReady);
    getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    getEventRegistry().register(PlayerInteractEvent.class, this::onPlayerInteract);
    getEventRegistry().register(DrainPlayerFromWorldEvent.class, this::onPlayerDrain);
    getEntityStoreRegistry().registerSystem(new CombatDamageEventSystem(combatService));
    getEntityStoreRegistry().registerSystem(new ProtectionBreakBlockEventSystem(protectionBridge, config));
    getEntityStoreRegistry().registerSystem(new ProtectionPlaceBlockEventSystem(protectionBridge, config));
  }

  private void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef != null) {
      combatService.recordPlayer(playerRef.getUuid(), playerRef.getUsername());
    }
  }

  private void onPlayerReady(PlayerReadyEvent event) {
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }
    PlayerRef playerRef = player.getPlayerRef();
    if (playerRef != null) {
      combatService.recordPlayer(playerRef.getUuid(), player.getDisplayName());
    }
    applySpawn(player, event.getPlayer().getWorld());
  }

  private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    if (playerRef == null) {
      return;
    }
    if (combatService.isInCombat(playerRef.getUuid())) {
      logger.warning("[FarmWorld] Combat-Logout: " + playerRef.getUsername());
    }
  }

  private void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (player == null || event.getTargetBlock() == null || player.getWorld() == null) {
      return;
    }
    boolean allowed = protectionBridge.onInteract(
        player.getDisplayName(),
        event.getTargetBlock().x,
        event.getTargetBlock().y,
        event.getTargetBlock().z,
        player.getWorld().getName(),
        config.farmWorld.instanceId,
        ProtectionPermissionHelper.collectBypassPermissions(player, config),
        List.of());
    if (!allowed) {
      event.setCancelled(true);
    }
  }

  private void onPlayerDrain(DrainPlayerFromWorldEvent event) {
    Player player = event.getHolder().getComponent(Player.getComponentType());
    if (player == null) {
      return;
    }
    World world = event.getWorld();
    if (world == null) {
      return;
    }
    com.efd.hytale.farmworld.shared.config.FarmWorldSpawn spawn = farmWorldService.resolveSpawn();
    if (!world.getName().equalsIgnoreCase(spawn.worldId)) {
      return;
    }
    Vector3f rotation = event.getTransform() != null ? event.getTransform().getRotation() : Vector3f.ZERO;
    event.setTransform(new Transform(new Vector3d(spawn.x, spawn.y, spawn.z), rotation));
  }

  private void applySpawn(Player player, World world) {
    if (player == null || world == null) {
      return;
    }
    com.efd.hytale.farmworld.shared.config.FarmWorldSpawn spawn = farmWorldService.resolveSpawn();
    if (!world.getName().equalsIgnoreCase(spawn.worldId)) {
      return;
    }
    if (player.getReference() == null) {
      return;
    }
    player.moveTo(player.getReference(), spawn.x, spawn.y, spawn.z, world.getEntityStore().getStore());
  }
}
