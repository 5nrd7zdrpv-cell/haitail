package com.efd.hytale.farmworld.daemon.tcp;

import com.efd.hytale.farmworld.daemon.config.FarmWorldConfig;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public final class TcpCommandServer {

    private final FarmWorldConfig.TcpSection cfg;
    private final Function<String, String> commandHandler;
    private final Semaphore connectionSlots;
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
        int maxConnections = Math.max(1, cfg.maxConnections);
        this.connectionSlots = new Semaphore(maxConnections);
    }

    public void start() {
        running = true;
        pool.submit(() -> {
            try (ServerSocket ss = new ServerSocket(cfg.port)) {
                serverSocket = ss;
                while (running) {
                    Socket s = ss.accept();
                    if (!connectionSlots.tryAcquire()) {
                        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
                            out.write("ERR server busy\n");
                            out.flush();
                        } catch (IOException ignored) {}
                        try { s.close(); } catch (IOException ignored) {}
                        continue;
                    }
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
            if (cfg.readTimeoutMillis > 0) {
                socket.setSoTimeout(cfg.readTimeoutMillis);
            }

            out.write("FarmWorldDaemon TCP. Please AUTH <password>\n");
            out.flush();

            boolean authed = false;
            long windowStart = System.nanoTime();
            int commandsThisWindow = 0;
            int maxLineLength = cfg.maxLineLength > 0 ? cfg.maxLineLength : 2048;
            int rateLimit = Math.max(1, cfg.commandRateLimitPerMinute);
            String line;
            while ((line = readLineLimited(in, maxLineLength)) != null) {
                if (LINE_TOO_LONG.equals(line)) {
                    out.write("ERR line too long\n");
                    out.flush();
                    continue;
                }
                line = line.trim();
                if (line.isBlank()) continue;

                if (!authed) {
                    if (line.toUpperCase().startsWith("AUTH ")) {
                        String pass = line.substring(5).trim();
                        if (isPasswordMatch(pass, cfg.password)) {
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

                long now = System.nanoTime();
                long elapsed = now - windowStart;
                if (elapsed > 60_000_000_000L) {
                    windowStart = now;
                    commandsThisWindow = 0;
                }
                commandsThisWindow++;
                if (commandsThisWindow > rateLimit) {
                    out.write("ERR rate limit exceeded\n");
                    out.flush();
                    continue;
                }

                String resp = commandHandler.apply(line);
                if (resp == null || resp.isBlank()) resp = "OK\n";
                if (!resp.endsWith("\n")) resp += "\n";
                out.write(resp);
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            connectionSlots.release();
        }
    }

    private static final String LINE_TOO_LONG = "__LINE_TOO_LONG__";

    private static String readLineLimited(BufferedReader in, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = in.read()) != -1) {
            if (ch == '\n') break;
            if (ch == '\r') continue;
            if (sb.length() >= maxChars) {
                while ((ch = in.read()) != -1 && ch != '\n') {}
                return LINE_TOO_LONG;
            }
            sb.append((char) ch);
        }
        if (ch == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    private static boolean isPasswordMatch(String provided, String expected) {
        if (expected == null) expected = "";
        if (provided == null) provided = "";
        return MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
