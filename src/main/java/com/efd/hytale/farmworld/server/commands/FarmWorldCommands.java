package com.efd.hytale.farmworld.server.commands;

import com.efd.hytale.farmworld.server.CommandBridge;
import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandResult;
import com.efd.hytale.farmworld.shared.config.FarmWorldConfig;
import com.efd.hytale.farmworld.shared.services.CombatTagService;
import com.efd.hytale.farmworld.shared.services.FarmWorldService;
import com.efd.hytale.farmworld.shared.services.ProtectionService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class FarmWorldCommands {
  private FarmWorldCommands() {}

  public static AbstractCommand createFarmCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldService farmWorldService,
      FarmWorldConfig config) {
    return new FarmWorldRootCommand("farm", "Farmworld-Status und Reset-Befehle.", registry, adminPermission);
  }

  public static AbstractCommand createProtectCommand(
      CommandRegistry registry,
      String adminPermission,
      FarmWorldConfig config,
      FarmWorldService farmWorldService,
      ProtectionService protectionService) {
    return new FarmWorldRootCommand("protect", "Schutzzone-Status und Test.", registry, adminPermission);
  }

  public static AbstractCommand createCombatCommand(
      CommandRegistry registry,
      String adminPermission,
      CombatTagService combatService) {
    return new FarmWorldRootCommand("combat", "Kampfstatus und Admin-Werkzeuge.", registry, adminPermission);
  }

  private static final class FarmWorldRootCommand extends AbstractCommand {
    private final CommandBridge bridge;
    private final String commandName;

    private FarmWorldRootCommand(String name, String description, CommandRegistry registry, String adminPermission) {
      super(name, description);
      this.commandName = name.toLowerCase(Locale.ROOT);
      this.bridge = new CommandBridge(registry);
      if (adminPermission != null && !adminPermission.isBlank()) {
        requirePermission(adminPermission);
      }
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext context) {
      String input = context.getInputString();
      String raw = buildRawCommand(input);
      CommandSender sender = context.sender();
      String actorId = resolveActorId(sender);
      CommandResult result = bridge.handleCommand(actorId, raw);
      context.sendMessage(Message.raw(result.message));
      return CompletableFuture.completedFuture(null);
    }

    private String buildRawCommand(String input) {
      if (input == null || input.isBlank()) {
        return commandName;
      }
      String trimmed = input.trim();
      if (trimmed.toLowerCase(Locale.ROOT).startsWith(commandName + " ")) {
        return trimmed;
      }
      if (trimmed.equalsIgnoreCase(commandName)) {
        return commandName;
      }
      return commandName + " " + trimmed;
    }

    private String resolveActorId(CommandSender sender) {
      if (sender == null) {
        return "unknown";
      }
      if (sender.getUuid() != null) {
        return sender.getUuid().toString();
      }
      String displayName = sender.getDisplayName();
      return displayName == null || displayName.isBlank() ? "unknown" : displayName;
    }
  }
}
