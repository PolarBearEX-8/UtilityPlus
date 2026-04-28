package com.example.utilityplus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinMessageListener implements Listener {

    private final JavaPlugin plugin;

    public JoinMessageListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("join-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("join-message.message", "&a&l+ &7{player} &ejoined the server!");
        message = formatMessage(message, player);

        boolean hideVanilla = plugin.getConfig().getBoolean("join-message.hide-vanilla", true);
        if (hideVanilla) {
            event.setJoinMessage(null);
        }

        boolean broadcast = plugin.getConfig().getBoolean("join-message.broadcast", true);
        if (broadcast) {
            plugin.getServer().broadcastMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfig().getBoolean("leave-message.enabled", true)) {
            return;
        }

        String message = plugin.getConfig().getString("leave-message.message", "&c&l- &7{player} &eleft the server!");
        message = formatMessage(message, player);

        boolean hideVanilla = plugin.getConfig().getBoolean("leave-message.hide-vanilla", true);
        if (hideVanilla) {
            event.setQuitMessage(null);
        }

        boolean broadcast = plugin.getConfig().getBoolean("leave-message.broadcast", true);
        if (broadcast) {
            plugin.getServer().broadcastMessage(message);
        }
    }

    private String formatMessage(String message, Player player) {
        return message
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{world}", player.getWorld().getName())
                .replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))
                .replace("{max}", String.valueOf(plugin.getServer().getMaxPlayers()))
                .replace("&", "§");
    }
}
