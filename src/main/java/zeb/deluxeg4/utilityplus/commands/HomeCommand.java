package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.gui.HomeGUI;
import zeb.deluxeg4.utilityplus.managers.HomeManager;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.atomic.AtomicInteger;

public class HomeCommand implements CommandExecutor {

    private final HomeManager homeManager;

    public HomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!"); return true;
        }
        Player player = (Player) sender;
        switch (label.toLowerCase()) {
            case "sethome": return handleSetHome(player, args);
            case "home":    return handleHome(player, args);
            case "delhome": return handleDelHome(player, args);
        }
        return false;
    }

    // ── /sethome <1-N> ───────────────────────────────────────────────
    private boolean handleSetHome(Player player, String[] args) {
        if (!player.hasPermission("utilityplus.sethome")) {
            player.sendMessage("§cYou don't have permission!"); return true;
        }
        if (args.length == 0) {
            player.sendMessage("§cUsage: /sethome <" + slotsHint(homeManager) + ">"); return true;
        }
        String slot = args[0];
        if (!homeManager.isValidSlot(slot)) {
            player.sendMessage("§cInvalid slot §e" + slot + "§c! Use: " + slotsHint(homeManager)); return true;
        }
        boolean overwrite = homeManager.hasHome(player.getUniqueId(), slot);
        homeManager.setHome(player.getUniqueId(), slot, player.getLocation());
        player.sendMessage(overwrite ? "§aHome §e" + slot + " §aupdated!" : "§aHome §e" + slot + " §aset!");
        return true;
    }

    // ── /home [1-N] ──────────────────────────────────────────────────
    private boolean handleHome(Player player, String[] args) {
        if (!player.hasPermission("utilityplus.home")) {
            player.sendMessage("§cYou don't have permission!"); return true;
        }
        if (args.length == 0) {
            HomeGUI.open(player, homeManager); return true;
        }
        String slot = args[0];
        if (!homeManager.isValidSlot(slot)) {
            player.sendMessage("§cInvalid slot §e" + slot + "§c! Use: " + slotsHint(homeManager)); return true;
        }
        Location loc = homeManager.getHome(player.getUniqueId(), slot);
        if (loc == null) {
            player.sendMessage("§cHome §e" + slot + " §cis not set! Use §e/sethome " + slot); return true;
        }
        if (homeManager.hasWarmup(player.getUniqueId())) {
            player.sendMessage("§eAlready teleporting!"); return true;
        }
        startWarmup(player, slot, loc, homeManager);
        return true;
    }

    // ── /delhome <1-N> ───────────────────────────────────────────────
    private boolean handleDelHome(Player player, String[] args) {
        if (!player.hasPermission("utilityplus.delhome")) {
            player.sendMessage("§cYou don't have permission!"); return true;
        }
        if (args.length == 0) {
            player.sendMessage("§cUsage: /delhome <" + slotsHint(homeManager) + ">"); return true;
        }
        String slot = args[0];
        if (!homeManager.isValidSlot(slot)) {
            player.sendMessage("§cInvalid slot §e" + slot + "§c! Use: " + slotsHint(homeManager)); return true;
        }
        if (homeManager.deleteHome(player.getUniqueId(), slot)) {
            player.sendMessage("§aHome §e" + slot + " §adeleted!");
        } else {
            player.sendMessage("§cHome §e" + slot + " §cis not set!");
        }
        return true;
    }

    // ── Static warmup — shared with HomeGUIListener ───────────────────
    /**
     * Starts the warmup countdown for teleporting to a home.
     * Static so HomeGUIListener can call it without coupling to HomeCommand.
     */
    public static void startWarmup(Player player, String slot, Location dest, HomeManager homeManager) {
        int warmup = homeManager.getWarmupSeconds();

        if (warmup <= 0) {
            PaperFoliaTasks.teleport(player, dest, homeManager.getPlugin(), success -> {
                if (!success) {
                    player.sendMessage("§cTeleport failed.");
                    return;
                }
                player.sendMessage("§aTeleported to home §e" + slot + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            });
            return;
        }

        Location startLoc = player.getLocation().clone();
        player.sendMessage("§eTeleporting to home §e" + slot + "§e in §a" + warmup + "§e sec... §7Don't move!");

        AtomicInteger remaining = new AtomicInteger(warmup);

        // Use Folia EntityScheduler for player-specific warmup
        ScheduledTask task = PaperFoliaTasks.runForPlayerTimer(homeManager.getPlugin(), player, (t) -> {
            if (!player.isOnline()) {
                homeManager.cancelWarmup(player.getUniqueId());
                t.cancel();
                return;
            }
            if (hasMoved(startLoc, player.getLocation())) {
                homeManager.cancelWarmup(player.getUniqueId());
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

            // TP!
            homeManager.cancelWarmup(player.getUniqueId());
            t.cancel();
            PaperFoliaTasks.teleport(player, dest, homeManager.getPlugin(), success -> {
                if (!success) {
                    player.sendMessage("§cTeleport failed.");
                    return;
                }
                player.sendMessage("§aTeleported to home §e" + slot + "§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            });
        }, 1L, 20L);

        homeManager.startWarmup(player.getUniqueId(), task);
    }

    private static boolean hasMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();
    }

    private static String slotsHint(HomeManager hm) {
        return String.join("|", hm.getActiveSlots());
    }
}
