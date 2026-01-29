package com.efd.hytale.farmworld.shared.commands;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandRegistry {
  private final Map<String, CommandDefinition> commands = new LinkedHashMap<>();

  public void register(CommandDefinition definition) {
    String key = definition.name.toLowerCase(Locale.ROOT);
    commands.put(key, definition);
  }

  public CommandResult execute(String actorId, String commandName, List<String> args) {
    CommandDefinition definition = commands.get(commandName.toLowerCase(Locale.ROOT));
    if (definition == null) {
      return CommandResult.error("[FarmWorld] Unbekannter Befehl: " + commandName);
    }
    int requiredCount = (int) definition.arguments.stream().filter(arg -> arg.required).count();
    if (args.size() < requiredCount) {
      return CommandResult.error("[FarmWorld] Fehlende Argumente für " + definition.name + ".");
    }
    return definition.handler.handle(new CommandContext(actorId, args));
  }

  public Collection<CommandDefinition> all() {
    return List.copyOf(commands.values());
  }
}
