package com.efd.hytale.farmworld.shared.commands;

@FunctionalInterface
public interface CommandHandler {
  CommandResult handle(CommandContext context);
}
