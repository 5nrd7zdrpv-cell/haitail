package com.efd.hytale.farmworld.shared.commands;

public class CommandArgument {
  public final String name;
  public final boolean required;
  public final String description;

  public CommandArgument(String name, boolean required, String description) {
    this.name = name;
    this.required = required;
    this.description = description;
  }
}
