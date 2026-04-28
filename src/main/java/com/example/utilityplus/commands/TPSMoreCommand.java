package com.example.utilityplus.commands;

import com.example.utilityplus.UtilityPlus;
import com.example.utilityplus.managers.CpuMonitor;
import com.example.utilityplus.managers.TickMonitor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TPSMoreCommand implements CommandExecutor {

    private final boolean isFolia;
    private final UtilityPlus plugin;
    private final TickMonitor tickMonitor;
    private final CpuMonitor cpuMonitor;

    public TPSMoreCommand(TickMonitor tickMonitor, CpuMonitor cpuMonitor) {
        this.plugin = JavaPlugin.getPlugin(UtilityPlus.class);
        this.tickMonitor = tickMonitor;
        this.cpuMonitor = cpuMonitor;
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionScheduler");
            folia = true;
        } catch (ClassNotFoundException ignored) {}
        this.isFolia = folia;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.tpsmore")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            sender.sendMessage("§8§l--- §b§lTPS More Info §8§l---");

            if (!isFolia) {
                double[] tps = Bukkit.getTPS();
                sender.sendMessage("§7Server Implementation: §eLeaf Async");
                sender.sendMessage("§7Global TPS (1m, 5m, 15m): " + formatTPS(tps[0]) + "§7, " + formatTPS(tps[1]) + "§7, " + formatTPS(tps[2]));
            } else {
                sender.sendMessage("§7Server Implementation: §eFolia (Regionalized)");
            }

            // Per World Breakdown
            sender.sendMessage("§6Worlds Info:");
            for (World world : Bukkit.getWorlds()) {
                sender.sendMessage(" §b§n" + world.getName());
                
                if (isFolia) {
                    List<Double> worldTPS = getRegionTPSForWorld(world);
                    if (!worldTPS.isEmpty()) {
                        Collections.sort(worldTPS);
                        double lowest = worldTPS.get(0);
                        double median = worldTPS.get(worldTPS.size() / 2);
                        double highest = worldTPS.get(worldTPS.size() - 1);
                        sender.sendMessage("  §7Regional TPS: " + formatTPS(lowest) + " §7/ " + formatTPS(median) + " §7/ " + formatTPS(highest));
                        sender.sendMessage("  §7Active Regions: §e" + worldTPS.size());
                    } else {
                        sender.sendMessage("  §7Regional TPS: §cNo active regions");
                    }
                }

                int worldEntities = world.getEntityCount();
                int worldChunks = world.getLoadedChunks().length;
                int worldPlayers = world.getPlayers().size();
                
                sender.sendMessage("  §7Entities: §e" + worldEntities + " §7| Chunks: §e" + worldChunks + " §7| Players: §e" + worldPlayers);
            }

            // Tick Durations (Spark Style)
            TickMonitor.Stats stats10s = tickMonitor.getStats(200);
            TickMonitor.Stats stats1m = tickMonitor.getStats(1200);

            sender.sendMessage("§6Tick Durations (min/med/95%ile/max ms)");
            sender.sendMessage(" §7last 10s: " + formatStatsSpark(stats10s));
            sender.sendMessage(" §7last 1m:  " + formatStatsSpark(stats1m));

            // System Metrics
            sender.sendMessage("§6System Info:");
            
            CpuMonitor.CpuStats cpu10s = cpuMonitor.getStats(10);
            CpuMonitor.CpuStats cpu1m = cpuMonitor.getStats(60);
            CpuMonitor.CpuStats cpu15m = cpuMonitor.getStats(900);

            sender.sendMessage(" §7CPU Usage (10s, 1m, 15m):");
            sender.sendMessage("  §7System:  §e" + formatCpu(cpu10s.system()) + "%§7, §e" + formatCpu(cpu1m.system()) + "%§7, §e" + formatCpu(cpu15m.system()) + "%");
            sender.sendMessage("  §7Process: §e" + formatCpu(cpu10s.process()) + "%§7, §e" + formatCpu(cpu1m.process()) + "%§7, §e" + formatCpu(cpu15m.process()) + "%");

            sender.sendMessage(" §7Disk Usage: " + getDiskUsage());

            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            sender.sendMessage(" §7Memory: §e" + (heapUsage.getUsed() / 1024 / 1024) + " MB / " + (heapUsage.getMax() / 1024 / 1024) + " MB");

            // Summary
            int totalEntities = 0;
            int totalChunks = 0;
            for (World world : Bukkit.getWorlds()) {
                totalEntities += world.getEntityCount();
                totalChunks += world.getLoadedChunks().length;
            }
            sender.sendMessage("§6Summary:");
            sender.sendMessage(" §7Total Players: §e" + Bukkit.getOnlinePlayers().size() + "§7 / §e" + Bukkit.getMaxPlayers());
            sender.sendMessage(" §7Total Resources: §e" + totalEntities + " §7Entities | §e" + totalChunks + " §7Chunks");

            sender.sendMessage("§8§l-------------------------");
        });

        return true;
    }

    private String formatStatsSpark(TickMonitor.Stats s) {
        return String.format("%s §7/ %s §7/ %s §7/ %s",
                formatMSPT(s.min()), formatMSPT(s.median()), formatMSPT(s.p95()), formatMSPT(s.max()));
    }

    private String formatCpu(double cpu) {
        if (cpu < 0) return "N/A";
        return String.format("%.1f", cpu);
    }

    private String getDiskUsage() {
        File root = new File(".");
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;
        if (total == 0) return "§cN/A";
        double percent = (used * 100.0) / total;
        return String.format("§e%d GB / %d GB (%.1f%%)", used / 1024 / 1024 / 1024, total / 1024 / 1024 / 1024, percent);
    }

    private List<Double> getRegionTPSForWorld(World world) {
        List<Double> tpsList = new ArrayList<>();
        try {
            Method getHandle = world.getClass().getMethod("getHandle");
            Object worldServer = getHandle.invoke(world);
            Field regioniserField = worldServer.getClass().getField("regioniser");
            Object regioniser = regioniserField.get(worldServer);
            Method computeMethod = regioniser.getClass().getDeclaredMethod("computeForAllRegionsUnsynchronised", Consumer.class);
            computeMethod.invoke(regioniser, (Consumer<Object>) region -> {
                try {
                    Method getTickData = region.getClass().getMethod("getTickData");
                    Object tickData = getTickData.invoke(region);
                    Method getTPS = tickData.getClass().getMethod("getTPS");
                    double[] tpsArr = (double[]) getTPS.invoke(tickData);
                    if (tpsArr != null && tpsArr.length > 0) tpsList.add(tpsArr[0]);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        return tpsList;
    }

    private String formatTPS(double tps) {
        String color = (tps >= 18.0) ? "§a" : (tps >= 15.0) ? "§e" : "§c";
        return color + String.format("%.2f", Math.min(20.0, tps));
    }

    private String formatMSPT(double mspt) {
        String color = (mspt <= 40.0) ? "§a" : (mspt <= 50.0) ? "§e" : "§c";
        return color + String.format("%.1f", mspt);
    }
}
