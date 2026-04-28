package com.example.utilityplus.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /msg|tell|w|whisper|dm|pm <player> <message...>
 * /r|reply <message...>
 */
public class PMTabComplete implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // /r /reply — no player completion needed
        if (alias.equalsIgnoreCase("r") || alias.equalsIgnoreCase("reply")) {
            return Collections.emptyList();
        }

        // arg1 = player name
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sender instanceof Player && p.equals(sender)) continue;
                if (p.getName().toLowerCase().startsWith(input)) names.add(p.getName());
            }
            return names;
        }

        return Collections.emptyList();
    }
}
