package com.example.utilityplus.managers;

import com.example.utilityplus.UtilityPlus;
import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;

public class CpuMonitor {

    private final UtilityPlus plugin;
    private final OperatingSystemMXBean osBean;
    
    // 15 minutes = 900 seconds
    private final double[] systemHistory = new double[900];
    private final double[] processHistory = new double[900];
    private int index = 0;
    private int totalSamples = 0;

    public CpuMonitor(UtilityPlus plugin) {
        this.plugin = plugin;
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        startSampling();
    }

    private void startSampling() {
        // Sample every second (20 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            double systemLoad = osBean.getCpuLoad() * 100;
            double processLoad = osBean.getProcessCpuLoad() * 100;

            synchronized (this) {
                systemHistory[index] = systemLoad;
                processHistory[index] = processLoad;
                index = (index + 1) % systemHistory.length;
                if (totalSamples < systemHistory.length) {
                    totalSamples++;
                }
            }
        }, 20L, 20L);
    }

    public CpuStats getStats(int seconds) {
        double systemSum = 0;
        double processSum = 0;
        int count = 0;

        synchronized (this) {
            int samplesToTake = Math.min(seconds, totalSamples);
            if (samplesToTake <= 0) return new CpuStats(0, 0);

            for (int i = 0; i < samplesToTake; i++) {
                int pos = (index - 1 - i + systemHistory.length) % systemHistory.length;
                double s = systemHistory[pos];
                double p = processHistory[pos];
                
                // OS bean might return -1 if not available
                if (s >= 0 && p >= 0) {
                    systemSum += s;
                    processSum += p;
                    count++;
                }
            }
        }

        if (count == 0) return new CpuStats(0, 0);
        return new CpuStats(systemSum / count, processSum / count);
    }

    public record CpuStats(double system, double process) {}
}
