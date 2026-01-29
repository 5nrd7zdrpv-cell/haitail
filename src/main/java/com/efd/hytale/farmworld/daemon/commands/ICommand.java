package com.efd.hytale.farmworld.daemon.commands;

import java.util.List;

public interface ICommand {
    String root();
    List<String> path();
    String help();
    String execute(String[] args);
}
