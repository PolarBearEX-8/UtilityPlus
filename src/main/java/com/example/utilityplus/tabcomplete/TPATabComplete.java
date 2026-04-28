package com.example.utilityplus.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /tpa <player>
 * /tpahere <player>
 * /tpaccept — no args
 * /tpdeny   — no args
 * /tpcancel — no args
 * /tpaon    — no args
 * /tpaoff   — no args
 */
public class TPATabComplete implements TabCompleter {

    private static final List<String> NO_ARGS = Arrays.asList("tpaccept","tpdeny","tpcancel","tpaon","tpaoff");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (NO_ARGS.contains(alias.toLowerCase())) return Collections.emptyList();

        // /tpa and /tpahere — arg1 = online player name (excluding self)
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
