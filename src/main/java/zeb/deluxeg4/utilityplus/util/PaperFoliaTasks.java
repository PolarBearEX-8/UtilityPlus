package zeb.deluxeg4.utilityplus.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PaperFoliaTasks {

    private static final boolean FOLIA = hasClass("io.papermc.paper.threadedregions.RegionizedServer");

    private PaperFoliaTasks() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static ScheduledTask runAsync(Plugin plugin, Consumer<ScheduledTask> task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, task);
    }

    public static ScheduledTask runAsyncTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin,
                task,
                ticksToMillis(initialDelayTicks),
                ticksToMillis(periodTicks),
                TimeUnit.MILLISECONDS
        );
    }

    public static ScheduledTask runGlobalTimer(Plugin plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, initialDelayTicks, periodTicks);
    }

    public static ScheduledTask runGlobalDelayed(Plugin plugin, Consumer<ScheduledTask> task, long delayTicks) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task, delayTicks);
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    public static void runForSender(Plugin plugin, CommandSender sender, Runnable task) {
        if (sender instanceof Player player) {
            runForPlayer(plugin, player, task);
            return;
        }
        runGlobal(plugin, task);
    }

    public static boolean runForPlayer(Plugin plugin, Player player, Runnable task) {
        return player.getScheduler().execute(plugin, task, null, 1L);
    }

    public static ScheduledTask runForPlayerDelayed(Plugin plugin, Player player, Consumer<ScheduledTask> task, long delayTicks) {
        return player.getScheduler().runDelayed(plugin, task, null, delayTicks);
    }

    public static ScheduledTask runForPlayerTimer(
            Plugin plugin,
            Player player,
            Consumer<ScheduledTask> task,
            long initialDelayTicks,
            long periodTicks
    ) {
        return player.getScheduler().runAtFixedRate(plugin, task, null, initialDelayTicks, periodTicks);
    }

    public static void send(Plugin plugin, Player player, String message) {
        runForPlayer(plugin, player, () -> player.sendMessage(message));
    }

    public static void broadcast(Plugin plugin, String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(plugin, player, message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public static void teleport(Player player, Location destination, Plugin plugin, Consumer<Boolean> after) {
        player.teleportAsync(destination).whenComplete((success, error) -> runForPlayer(plugin, player, () -> {
            if (after != null) {
                after.accept(error == null && Boolean.TRUE.equals(success));
            }
        }));
    }

    private static long ticksToMillis(long ticks) {
        return Math.max(1L, ticks) * 50L;
    }

    private static boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
