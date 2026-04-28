package com.example.utilityplus.managers;

import com.example.utilityplus.UtilityPlus;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;

public class TickMonitor {

    private final UtilityPlus plugin;
    private final long[] tickTimes = new long[1200];
    private int index = 0;
    private long lastTickStartTime = System.nanoTime();

    public TickMonitor(UtilityPlus plugin) {
        this.plugin = plugin;
        
        boolean paperEvents = false;
        try {
            Class.forName("com.destroystokyo.paper.event.server.ServerTickEndEvent");
            paperEvents = true;
        } catch (ClassNotFoundException ignored) {}

        if (paperEvents) {
            Bukkit.getPluginManager().registerEvents(new PaperTickListener(this), plugin);
            plugin.getLogger().info("Using Paper ServerTickEndEvent for accurate MSPT tracking.");
        } else {
            startFallbackTicking();
        }
    }

    private void startFallbackTicking() {
        Runnable task = () -> {
            long currentTime = System.nanoTime();
            recordTick(currentTime - lastTickStartTime);
            lastTickStartTime = currentTime;
        };

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionScheduler");
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), 1L, 1L);
        } catch (ClassNotFoundException e) {
            Bukkit.getScheduler().runTaskTimer(plugin, task, 1L, 1L);
        }
    }

    protected synchronized void recordTick(long durationNanos) {
        tickTimes[index] = durationNanos;
        index = (index + 1) % tickTimes.length;
    }

    public Stats getStats(int ticks) {
        long[] samples;
        synchronized (this) {
            int count = Math.min(ticks, tickTimes.length);
            samples = new long[count];
            for (int i = 0; i < count; i++) {
                int pos = (index - 1 - i + tickTimes.length) % tickTimes.length;
                samples[i] = tickTimes[pos];
            }
        }

        if (samples.length == 0) return new Stats(0, 0, 0, 0);

        Arrays.sort(samples);
        double min = samples[0] / 1_000_000.0;
        double max = samples[samples.length - 1] / 1_000_000.0;
        double median = samples[samples.length / 2] / 1_000_000.0;
        double p95 = samples[(int) (samples.length * 0.95)] / 1_000_000.0;

        return new Stats(min, median, p95, max);
    }

    public static record Stats(double min, double median, double p95, double max) {}

    // Inner class to handle Paper-specific events safely
    private static class PaperTickListener implements Listener {
        private final TickMonitor monitor;

        public PaperTickListener(TickMonitor monitor) {
            this.monitor = monitor;
        }

        @EventHandler
        public void onTickEnd(com.destroystokyo.paper.event.server.ServerTickEndEvent event) {
            monitor.recordTick((long) (event.getTickDuration() * 1_000_000.0));
        }
    }
}
