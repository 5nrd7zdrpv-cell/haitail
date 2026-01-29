package com.efd.hytale.farmworld.daemon.commands;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandManager {
    private final Map<String, List<ICommand>> byRoot = new ConcurrentHashMap<>();

    public void register(ICommand cmd) {
        byRoot.computeIfAbsent(cmd.root().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(cmd);
    }

    public String dispatchWithOutput(String root, String remainderLine) {
        String r = root.toLowerCase(Locale.ROOT);
        List<ICommand> cmds = byRoot.get(r);
        if (cmds == null || cmds.isEmpty()) return "Unknown command root. Use: farm | protect | combat\n";

        String[] tokens = remainderLine == null || remainderLine.isBlank()
                ? new String[0]
                : remainderLine.trim().split("\s+");

        for (ICommand c : cmds) {
            if (matchesPath(tokens, c.path())) {
                String[] args = Arrays.copyOfRange(tokens, c.path().size(), tokens.length);
                String out = c.execute(args);
                return out == null ? "" : out;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available subcommands for ").append(r).append(":\n");
        cmds.stream().sorted(Comparator.comparing(a -> String.join(" ", a.path())))
                .forEach(c -> sb.append("  ").append(r).append(" ").append(String.join(" ", c.path()))
                        .append(" — ").append(c.help()).append("\n"));
        return sb.toString();
    }

    private boolean matchesPath(String[] tokens, List<String> path) {
        if (tokens.length < path.size()) return false;
        for (int i = 0; i < path.size(); i++) {
            if (!tokens[i].equalsIgnoreCase(path.get(i))) return false;
        }
        return true;
    }
}
