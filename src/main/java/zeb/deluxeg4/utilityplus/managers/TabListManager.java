package zeb.deluxeg4.utilityplus.managers;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TabListManager {

    private final UtilityPlus plugin;
    private ScheduledTask updateTask;
    private boolean enabled;
    private long updateIntervalTicks;
    private List<String> headerLines;
    private List<String> footerLines;

    public TabListManager(UtilityPlus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("tab-list.enabled", true);
        int updateIntervalSeconds = Math.max(1, plugin.getConfig().getInt("tab-list.update-interval", 5));
        this.updateIntervalTicks = updateIntervalSeconds * 20L;
        this.headerLines = plugin.getConfig().getStringList("tab-list.header");
        this.footerLines = plugin.getConfig().getStringList("tab-list.footer");

        stop();
        if (enabled) {
            start();
            updateAll();
        } else {
            clearAll();
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void update(Player player) {
        if (!enabled) {
            clear(player);
            return;
        }

        String header = formatLines(headerLines, player);
        String footer = formatLines(footerLines, player);
        player.setPlayerListHeaderFooter(header, footer);
    }

    private void start() {
        updateTask = PaperFoliaTasks.runGlobalTimer(plugin, task -> updateAll(), updateIntervalTicks, updateIntervalTicks);
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () -> update(player));
        }
    }

    private void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaperFoliaTasks.runForPlayer(plugin, player, () -> clear(player));
        }
    }

    private void clear(Player player) {
        player.setPlayerListHeaderFooter("", "");
    }

    private String formatLines(List<String> lines, Player player) {
        return color(String.join("\n", lines)
                .replace("%server_tps_1_colored%", formatTps(getAverageWorldTps()))
                .replace("%server_online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%player_ping%", String.valueOf(player.getPing()))
                .replace("%server_uptime%", formatUptime()));
    }

    private double getAverageWorldTps() {
        String[] worldNames = {"world", "world_nether", "world_the_end"};
        double total = 0.0D;
        int count = 0;

        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            Double tps = getWorldTps(world);
            if (tps != null) {
                total += tps;
                count++;
            }
        }

        if (count > 0) {
            return total / count;
        }

        return Bukkit.getTPS()[0];
    }

    private Double getWorldTps(World world) {
        try {
            Method getTps = Bukkit.getServer().getClass().getMethod("getTPS", Location.class);
            double[] tps = (double[]) getTps.invoke(Bukkit.getServer(), world.getSpawnLocation());
            if (tps != null && tps.length > 0) {
                return tps[0];
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            // Official Folia/Paper do not expose per-location TPS; some forks do.
        }

        return null;
    }

    private String formatTps(double tps) {
        String color = tps > 18.0D ? "&a" : tps > 16.0D ? "&e" : "&c";
        return color + String.format("%.2f", Math.min(tps, 20.0D));
    }

    private String formatUptime() {
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
