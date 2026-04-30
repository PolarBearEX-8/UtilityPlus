package com.example.utilityplus.listeners;

import com.example.utilityplus.commands.KillCommand;
import com.example.utilityplus.managers.ChatManager;
import com.example.utilityplus.managers.StatsManager;
import com.example.utilityplus.util.PaperFoliaTasks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsListener implements Listener {

    private final StatsManager statsManager;
    private final ChatManager chatManager;
    private final JavaPlugin plugin;

    public StatsListener(StatsManager statsManager, ChatManager chatManager, JavaPlugin plugin) {
        this.statsManager = statsManager;
        this.chatManager = chatManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Count death for victim
        statsManager.addDeath(victim.getUniqueId(), victim.getName());

        // Count kill for killer
        if (killer != null) {
            statsManager.addKill(killer.getUniqueId(), killer.getName());
        }

        colorDeathMessage(event, victim, killer);
        
        // Auto-save every few deaths or use periodic save
        // For simplicity, we save on every death/kill here, but in a busy server 
        // you might want to save periodically.
        statsManager.saveData();
    }

    private void colorDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
        boolean selfKillCommand = victim.hasMetadata(KillCommand.SELF_KILL_METADATA);
        if (selfKillCommand) {
            victim.removeMetadata(KillCommand.SELF_KILL_METADATA, plugin);
        }

        if (!plugin.getConfig().getBoolean("death-message.enabled", true)) {
            if (selfKillCommand) {
                broadcastDeathMessage(event, victim, victim.getName() + " ended their life");
            }
            return;
        }

        String originalMessage = event.getDeathMessage();
        if (originalMessage == null || originalMessage.isEmpty()) {
            return;
        }
        if (selfKillCommand) {
            originalMessage = victim.getName() + " ended their life";
        }

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));
        String template = color(plugin.getConfig().getString("death-message.message", "{message}"));
        String killerName = killer != null ? killer.getName() : "";

        String message = messageColor + template
                .replace("{message}", originalMessage)
                .replace("{player}", victim.getName())
                .replace("{killer}", killerName);

        message = colorName(message, victim.getName());

        if (killer != null) {
            message = colorName(message, killer.getName());
        }

        broadcastDeathMessage(event, victim, message);
    }

    private void broadcastDeathMessage(PlayerDeathEvent event, Player victim, String message) {
        event.setDeathMessage(null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (chatManager.isDeathMessagesMuted(player.getUniqueId())
                    || chatManager.isIgnoringDeathMessage(player.getUniqueId(), victim.getName())) {
                continue;
            }
            PaperFoliaTasks.send(plugin, player, message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private String colorName(String message, String name) {
        if (name == null || name.isEmpty()) {
            return message;
        }

        String messageColor = color(plugin.getConfig().getString("death-message.message-color", "&c"));
        String nameColor = color(plugin.getConfig().getString("death-message.name-color", "&b"));

        return message.replace(name, nameColor + name + messageColor);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
