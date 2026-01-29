package com.efd.hytale.farmworld.shared.commands;

import java.util.List;

public class CommandContext {
  public final String actorId;
  public final List<String> args;

  public CommandContext(String actorId, List<String> args) {
    this.actorId = actorId;
    this.args = List.copyOf(args);
  }
}
