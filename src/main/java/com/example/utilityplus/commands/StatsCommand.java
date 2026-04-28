package com.example.utilityplus.commands;

import com.example.utilityplus.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements CommandExecutor {

    private final StatsManager statsManager;

    public StatsCommand(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("stats")) {
            return handleStats(sender, args);
        } else if (label.equalsIgnoreCase("topstats")) {
            return handleTopStats(sender);
        }
        return false;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("utilityplus.stats")) {
            sender.sendMessage("§cYou don't have permission.");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                // Try to find in offline stats
                // We'll just show what we have in StatsManager if player is offline
                sender.sendMessage("§7Looking up offline stats for §e" + args[0] + "§7...");
                // Note: Finding by name in StatsManager requires iterating or a secondary map.
                // For now, let's keep it simple and only online or tell them if not found.
                sender.sendMessage("§cPlayer not online. (Offline lookup not implemented for name)");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player.");
                return true;
            }
            target = (Player) sender;
        }

        StatsManager.PlayerStats ps = statsManager.getStats(target.getUniqueId());
        sender.sendMessage("§8§l--- §b§lStats: §e" + target.getName() + " §8§l---");
        sender.sendMessage("§aKills: §e" + ps.getKills());
        sender.sendMessage("§cDeaths: §e" + ps.getDeaths());
        double kdr = ps.getDeaths() == 0 ? ps.getKills() : (double) ps.getKills() / ps.getDeaths();
        sender.sendMessage("§bKDR: §e" + String.format("%.2f", kdr));
        sender.sendMessage("§8§l----------------------");

        return true;
    }

    private boolean handleTopStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can open the Top Stats GUI.");
            return true;
        }
        if (!player.hasPermission("utilityplus.topstats")) {
            player.sendMessage("§cYou don't have permission.");
            return true;
        }

        openTopStatsGUI(player);
        return true;
    }

    private void openTopStatsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0§lTop Stats");

        // Top 5 Killers
        List<StatsManager.PlayerStats> topKills = statsManager.getTopKills(5);
        for (int i = 0; i < topKills.size(); i++) {
            inv.setItem(2 + i, createStatsItem(topKills.get(i), i + 1, "§aKills"));
        }

        // Top 5 Deaths
        List<StatsManager.PlayerStats> topDeaths = statsManager.getTopDeaths(5);
        for (int i = 0; i < topDeaths.size(); i++) {
            inv.setItem(20 + i, createStatsItem(topDeaths.get(i), i + 1, "§cDeaths"));
        }

        // Decoration
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        inv.setItem(0, createInfoItem("§a§lTop 5 Killers"));
        inv.setItem(18, createInfoItem("§c§lTop 5 Deaths"));

        player.openInventory(inv);
    }

    private ItemStack createStatsItem(StatsManager.PlayerStats ps, int rank, String typeLabel) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(ps.getUuid()));
        meta.setDisplayName("§eRank #" + rank + ": §b" + ps.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§7" + typeLabel + ": §e" + (typeLabel.contains("Kills") ? ps.getKills() : ps.getDeaths()));
        lore.add("§7Total Kills: §f" + ps.getKills());
        lore.add("§7Total Deaths: §f" + ps.getDeaths());
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(String name) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
