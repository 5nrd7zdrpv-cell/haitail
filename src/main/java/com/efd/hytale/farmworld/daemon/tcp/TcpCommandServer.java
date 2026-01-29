package com.efd.hytale.farmworld.daemon.tcp;

import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public final class TcpCommandServer {

    private final FarmWorldConfig.TcpSection cfg;
    private final Function<String, String> commandHandler;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "farmworld-tcp-client");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public TcpCommandServer(FarmWorldConfig.TcpSection cfg, Function<String, String> commandHandler) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        this.commandHandler = Objects.requireNonNull(commandHandler, "commandHandler");
    }

    public void start() {
        running = true;
        pool.submit(() -> {
            try (ServerSocket ss = new ServerSocket(cfg.port)) {
                serverSocket = ss;
                while (running) {
                    Socket s = ss.accept();
                    pool.submit(() -> handleClient(s));
                }
            } catch (IOException e) {
                if (running) System.err.println("[daemon] TCP server error: " + e.getMessage());
            }
        });
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        pool.shutdownNow();
    }

    private void handleClient(Socket s) {
        try (Socket socket = s;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            out.write("FarmWorldDaemon TCP. Please AUTH <password>\n");
            out.flush();

            boolean authed = false;
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isBlank()) continue;

                if (!authed) {
                    if (line.toUpperCase().startsWith("AUTH ")) {
                        String pass = line.substring(5).trim();
                        if (pass.equals(cfg.password)) {
                            authed = true;
                            out.write("OK\n");
                        } else {
                            out.write("ERR invalid password\n");
                        }
                        out.flush();
                        continue;
                    } else {
                        out.write("ERR not authenticated\n");
                        out.flush();
                        continue;
                    }
                }

                if (line.equalsIgnoreCase("QUIT")) {
                    out.write("BYE\n");
                    out.flush();
                    break;
                }

                String resp = commandHandler.apply(line);
                if (resp == null || resp.isBlank()) resp = "OK\n";
                if (!resp.endsWith("\n")) resp += "\n";
                out.write(resp);
                out.flush();
            }
        } catch (IOException ignored) {}
    }
}
