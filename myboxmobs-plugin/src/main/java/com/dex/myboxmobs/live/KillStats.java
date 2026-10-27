package com.dex.myboxmobs.live;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kill counts per player and mob (spec 9), in SQLite {@code data.db}. Everything is cached in memory (read on the
 * main thread by PlaceholderAPI); writes go through one background thread.
 */
public final class KillStats {

    private final File file;
    private final Logger log;
    private final Map<UUID, Map<String, Long>> kills = new ConcurrentHashMap<>();
    private Connection conn;
    private ExecutorService writer;

    public KillStats(File dataFolder, Logger log) {
        this.file = new File(dataFolder, "data.db");
        this.log = log;
    }

    public boolean open() {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("CREATE TABLE IF NOT EXISTS kills (uuid TEXT NOT NULL, mob TEXT NOT NULL,"
                        + " count INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(uuid, mob))");
                try (ResultSet rs = st.executeQuery("SELECT uuid, mob, count FROM kills")) {
                    while (rs.next()) {
                        kills.computeIfAbsent(UUID.fromString(rs.getString(1)), k -> new ConcurrentHashMap<>())
                                .put(rs.getString(2), rs.getLong(3));
                    }
                }
            }
            writer = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MyBoxMobs-db");
                t.setDaemon(true);
                return t;
            });
            return true;
        } catch (ClassNotFoundException | SQLException | IllegalArgumentException ex) {
            log.log(Level.SEVERE, "Could not open data.db: kill statistics disabled", ex);
            return false;
        }
    }

    public void addKill(UUID player, String mob) {
        kills.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).merge(mob, 1L, Long::sum);
        if (writer == null) return;
        writer.execute(() -> {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO kills(uuid, mob, count) VALUES(?, ?, 1)"
                    + " ON CONFLICT(uuid, mob) DO UPDATE SET count = count + 1")) {
                ps.setString(1, player.toString());
                ps.setString(2, mob);
                ps.executeUpdate();
            } catch (SQLException ex) {
                log.log(Level.WARNING, "Could not save a kill", ex);
            }
        });
    }

    public long kills(UUID player, String mob) {
        Map<String, Long> m = kills.get(player);
        return m == null ? 0 : m.getOrDefault(mob, 0L);
    }

    public long total(UUID player) {
        Map<String, Long> m = kills.get(player);
        if (m == null) return 0;
        long sum = 0;
        for (long v : m.values()) sum += v;
        return sum;
    }

    public void close() {
        if (writer != null) {
            writer.shutdown();
            try {
                if (!writer.awaitTermination(5, TimeUnit.SECONDS)) log.warning("Kill writes still pending at shutdown");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException ignored) {
            // closing anyway
        }
    }
}
