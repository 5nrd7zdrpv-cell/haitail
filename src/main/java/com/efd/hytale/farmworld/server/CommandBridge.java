package com.efd.hytale.farmworld.server;

import com.efd.hytale.farmworld.shared.commands.CommandRegistry;
import com.efd.hytale.farmworld.shared.commands.CommandResult;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandBridge {
  private final CommandRegistry registry;

  public CommandBridge(CommandRegistry registry) {
    this.registry = registry;
  }

  public CommandResult handleCommand(String actorId, String rawCommand) {
    if (rawCommand == null || rawCommand.isBlank()) {
      return CommandResult.error("[FarmWorld] Leerer Befehl.");
    }
    String trimmed = rawCommand.trim();
    String[] parts = trimmed.split("\\s+");
    String commandName = parts[0].toLowerCase(Locale.ROOT);
    List<String> args = parts.length > 1 ? Arrays.asList(parts).subList(1, parts.length) : List.of();
    return registry.execute(actorId, commandName, args);
  }

  // TODO: Integrate with the Hytale command API once the official server hook is available.
}
