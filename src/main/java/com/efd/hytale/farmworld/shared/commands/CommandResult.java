package com.efd.hytale.farmworld.shared.commands;

public class CommandResult {
  public final boolean success;
  public final String message;

  private CommandResult(boolean success, String message) {
    this.success = success;
    this.message = message;
  }

  public static CommandResult ok(String message) {
    return new CommandResult(true, message);
  }

  public static CommandResult error(String message) {
    return new CommandResult(false, message);
  }
}
