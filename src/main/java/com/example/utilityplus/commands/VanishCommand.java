package com.example.utilityplus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("utilityplus.vanish")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (vanishedPlayers.contains(uuid)) {
            setVanished(player, false);
            player.sendMessage("§aYou are now §evisible§a!");
        } else {
            setVanished(player, true);
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
            
            // Following AchyMake/Minecraft-Essentials logic:
            player.setInvisible(true);           // หายตัว (ไม่มี Effect/Particle)
            player.setAllowFlight(true);         // บินได้
            player.setInvulnerable(true);        // อมตะ (เลือกได้ตามความเหมาะสม)
            player.setSleepingIgnored(true);     // ไม่ต้องนอน
            player.setCollidable(false);         // ไม่มีแรงกระแทกกับคนอื่น
            player.setSilent(true);              // ไม่มีเสียง (เดิน, กิน, ฯลฯ)
            player.setCanPickupItems(false);     // ไม่เก็บไอเทม
            
            // Hide player from others who don't have permission to see vanished players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("utilityplus.vanish.see") && !online.equals(player)) {
                    online.hidePlayer(plugin, player);
                }
            }
        } else {
            vanishedPlayers.remove(uuid);
            
            player.setInvisible(false);
            player.setAllowFlight(player.hasPermission("utilityplus.fly")); // บินต่อถ้ามี Permission fly
            player.setInvulnerable(false);
            player.setSleepingIgnored(false);
            player.setCollidable(true);
            player.setSilent(false);
            player.setCanPickupItems(true);
            
            // Show player to everyone
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }
    }
}
