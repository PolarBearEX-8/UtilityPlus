package com.example.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("utilityplus.vanish")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            // Unvanish
            vanishedPlayers.remove(uuid);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
            player.sendMessage("§aYou are now §evisible§a!");
        } else {
            // Vanish
            vanishedPlayers.add(uuid);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.hasPermission("utilityplus.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
            player.sendMessage("§aYou are now §7vanished§a!");
        }

        return true;
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public void setVanished(Player player, boolean vanished) {
        UUID uuid = player.getUniqueId();
        if (vanished) {
            vanishedPlayers.add(uuid);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (!online.hasPermission("utilityplus.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            vanishedPlayers.remove(uuid);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }
    }
}
