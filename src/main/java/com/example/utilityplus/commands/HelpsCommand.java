package com.example.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class HelpsCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public HelpsCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.helps")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        boolean isOp = sender.isOp();
        String type = isOp ? "op" : "normal";

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number.");
                return true;
            }
        }

        if (page < 1) {
            sender.sendMessage("§cPage must be 1 or higher.");
            return true;
        }

        // Find max pages
        int maxPages = 0;
        for (int i = 1; i <= 99; i++) {
            List<String> check = plugin.getConfig().getStringList("helps." + type + "." + i);
            if (!check.isEmpty()) {
                maxPages = i;
            }
        }

        if (maxPages == 0) {
            sender.sendMessage("§cNo help pages configured.");
            return true;
        }

        if (page > maxPages) {
            sender.sendMessage("§cPage " + page + " does not exist. Max pages: " + maxPages);
            return true;
        }

        List<String> pageLines = plugin.getConfig().getStringList("helps." + type + "." + page);

        sender.sendMessage("§b§lHelps §7(Page " + page + "/" + maxPages + ")");
        sender.sendMessage("§8--------------------------------");

        int count = 0;
        for (String line : pageLines) {
            if (line != null && !line.isEmpty()) {
                sender.sendMessage(line.replace("&", "§"));
                count++;
                if (count >= 5) break; // limit 5 lines per page
            }
        }

        if (page < maxPages) {
            sender.sendMessage("§7Use §e/helps " + (page + 1) + " §7to see more.");
        }
        sender.sendMessage("§8--------------------------------");

        return true;
    }
}
