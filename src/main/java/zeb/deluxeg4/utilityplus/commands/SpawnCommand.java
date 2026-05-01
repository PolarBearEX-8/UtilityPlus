package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.SpawnManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class SpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;

    public SpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("setspawn")) return handleSetSpawn(player);
        if (label.equalsIgnoreCase("spawn"))    return handleSpawn(player);
        return false;
    }

    private boolean handleSetSpawn(Player player) {
        if (!player.hasPermission("utilityplus.setspawn")) {
            player.sendMessage("§cYou don't have permission!"); return true;
        }
        Location previous = spawnManager.getSpawn();
        spawnManager.setSpawn(player.getLocation());
        if (previous == null) {
            player.sendMessage("§aSpawn point set!");
        } else {
            player.sendMessage("§aSpawn updated! §7Previous: §e" + formatLocation(previous));
        }
        player.sendMessage("§7New: §e" + formatLocation(player.getLocation()));
        return true;
    }

    private boolean handleSpawn(Player player) {
        if (!player.hasPermission("utilityplus.spawn")) {
            player.sendMessage("§cYou don't have permission!"); return true;
        }
        if (!spawnManager.hasSpawn()) {
            player.sendMessage("§cNo spawn set! Ask an admin to use /setspawn."); return true;
        }
        if (spawnManager.hasWarmup(player.getUniqueId())) {
            player.sendMessage("§eAlready teleporting to spawn!"); return true;
        }
        if (!player.isOp() && spawnManager.isOnCooldown(player.getUniqueId())) {
            long left = spawnManager.getCooldownSecondsLeft(player.getUniqueId());
            player.sendMessage("§cWait §e" + left + "§cs before using /spawn again."); return true;
        }

        int warmup = spawnManager.getWarmupSeconds();
        if (warmup <= 0) {
            spawnManager.applyCooldown(player.getUniqueId());
            PaperFoliaTasks.teleport(player, spawnManager.getSpawn(), spawnManager.getPlugin(), success -> {
                if (!success) {
                    player.sendMessage("§cTeleport failed.");
                    return;
                }
                player.sendMessage("§aTeleported to spawn!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            });
            return true;
        }

        Location startLoc = player.getLocation().clone();
        player.sendMessage("§eTeleporting to spawn in §a" + warmup + "§e sec... §7Don't move!");

        AtomicInteger remaining = new AtomicInteger(warmup);

        // Use Folia EntityScheduler for player-specific warmup
        ScheduledTask task = PaperFoliaTasks.runForPlayerTimer(spawnManager.getPlugin(), player, (t) -> {
            if (!player.isOnline()) {
                spawnManager.cancelWarmup(player.getUniqueId());
                t.cancel();
                return;
            }
            if (hasMoved(startLoc, player.getLocation())) {
                spawnManager.cancelWarmup(player.getUniqueId());
                player.sendMessage("§cTeleport cancelled — you moved!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                t.cancel();
                return;
            }

            int left = remaining.decrementAndGet();

            if (left > 0) {
                player.sendMessage("§eTeleporting in §a" + left + "§e...");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                return;
            }

            spawnManager.cancelWarmup(player.getUniqueId());
            spawnManager.applyCooldown(player.getUniqueId());
            t.cancel();
            PaperFoliaTasks.teleport(player, spawnManager.getSpawn(), spawnManager.getPlugin(), success -> {
                if (!success) {
                    player.sendMessage("§cTeleport failed.");
                    return;
                }
                player.sendMessage("§aTeleported to spawn!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            });
        }, 1L, 20L);

        spawnManager.startWarmup(player.getUniqueId(), task);
        return true;
    }

    private boolean hasMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    private String formatLocation(Location loc) {
        return String.format("%s [%.1f, %.1f, %.1f]",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }
}
