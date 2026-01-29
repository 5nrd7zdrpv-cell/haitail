package com.efd.hytale.farmworld.shared.commands;

import java.util.List;

public class CommandDefinition {
  public final String name;
  public final String description;
  public final List<CommandArgument> arguments;
  public final CommandHandler handler;

  public CommandDefinition(
      String name,
      String description,
      List<CommandArgument> arguments,
      CommandHandler handler) {
    this.name = name;
    this.description = description;
    this.arguments = List.copyOf(arguments);
    this.handler = handler;
  }
}
