package com.example.utilityplus.commands;

import com.example.utilityplus.util.PaperFoliaTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class BroadcastCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public BroadcastCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.broadcast")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /" + label + " <message>");
            return true;
        }

        String prefix = plugin.getConfig().getString("broadcast.prefix", "&6&l[BROADCAST]&r ");
        String message = String.join(" ", args);

        String broadcastMessage = (prefix + message).replace("&", "§");
        PaperFoliaTasks.broadcast(plugin, broadcastMessage);

        return true;
    }
}
