package com.example.utilityplus.tabcomplete;

import com.example.utilityplus.managers.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /home [1|2|3|4]
 * /sethome <1|2|3|4>
 * /delhome <1|2|3|4>
 */
public class HomeTabComplete implements TabCompleter {

    private static final List<String> SLOTS = Arrays.asList("1", "2", "3", "4");
    private final HomeManager homeManager;

    public HomeTabComplete(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || args.length != 1) return Collections.emptyList();

        String input = args[0].toLowerCase();
        List<String> result = new ArrayList<>();

        // For /delhome only suggest slots that actually have a home set
        if (alias.equalsIgnoreCase("delhome") && sender instanceof Player) {
            Player p = (Player) sender;
            for (String slot : SLOTS) {
                if (homeManager.getHome(p.getUniqueId(), slot) != null && slot.startsWith(input)) {
                    result.add(slot);
                }
            }
            return result;
        }

        for (String slot : SLOTS) {
            if (slot.startsWith(input)) result.add(slot);
        }
        return result;
    }
}
