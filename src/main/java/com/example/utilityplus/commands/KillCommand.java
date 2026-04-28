package com.example.utilityplus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KillCommand implements CommandExecutor {

    // Track players who need to confirm their suicide
    private final Set<UUID> pendingConfirmation = new HashSet<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("utilityplus.kill")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }

        // Case: /kill <player>
        if (args.length > 0) {
            if (!sender.hasPermission("utilityplus.kill.others")) {
                sender.sendMessage("§cYou don't have permission to kill others!");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            target.setHealth(0);
            target.sendMessage("§cYou were killed by " + sender.getName() + ".");
            sender.sendMessage("§aYou killed " + target.getName() + ".");
            return true;
        }

        // Case: /kill (Self-kill with confirmation)
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cConsole must specify a player: /kill <player>");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (pendingConfirmation.contains(uuid)) {
            // Second time: Kill them
            player.setHealth(0);
            //player.sendMessage("§eYou have killed yourself.");
            pendingConfirmation.remove(uuid);
        } else {
            // First time: Ask for confirmation
            pendingConfirmation.add(uuid);
            player.sendMessage("§cAre you sure you want to die? §eType /kill again to confirm.");
            
            // Optional: Remove from pending after 10 seconds
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UtilityPlus"), 
                () -> pendingConfirmation.remove(uuid), 200L);
        }

        return true;
    }
}
