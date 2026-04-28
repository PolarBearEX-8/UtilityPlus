package com.example.utilityplus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HelpsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.helps")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        boolean isOp = sender.isOp();
        List<String> helpLines = new ArrayList<>();

        if (!isOp) {
            addLimitedHelp(helpLines);
        } else {
            addFullHelp(helpLines);
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid page number.");
                return true;
            }
        }

        int linesPerPage = 10;
        int maxPages = (int) Math.ceil((double) helpLines.size() / linesPerPage);
        if (maxPages == 0) maxPages = 1;

        if (page < 1 || page > maxPages) {
            sender.sendMessage("§cPage " + page + " does not exist. Max pages: " + maxPages);
            return true;
        }

        sender.sendMessage("§b§lHelps §7(Page " + page + "/" + maxPages + ")");
        sender.sendMessage("§8--------------------------------");

        int start = (page - 1) * linesPerPage;
        int end = Math.min(start + linesPerPage, helpLines.size());

        for (int i = start; i < end; i++) {
            sender.sendMessage(helpLines.get(i));
        }

        if (page < maxPages) {
            sender.sendMessage("§7Use §e/helps " + (page + 1) + " §7to see more.");
        }
        sender.sendMessage("§8--------------------------------");

        return true;
    }

    private void addLimitedHelp(List<String> lines) {
        lines.add("§e/chatsettings §7- Open chat settings GUI");
        lines.add("§e/msg <player> <msg> §7- Send a private message");
        lines.add("§e/tell <player> <msg> §7- Alias for /msg");
        lines.add("§e/w <player> <msg> §7- Alias for /msg");
        lines.add("§e/whisper <player> <msg> §7- Alias for /msg");
        lines.add("§e/dm <player> <msg> §7- Alias for /msg");
        lines.add("§e/pm <player> <msg> §7- Alias for /msg");
        lines.add("§e/r <msg> §7- Reply to the last private message");
        lines.add("§e/reply <msg> §7- Alias for /r");
        //lines.add("§e/move §7- (Coming soon)");
        //sflines.add("§e/skin §7- (Coming soon)");
        lines.add("§e/stats [player] §7- View player stats");
        lines.add("§e/topstats §7- View leaderboard (GUI)");
        lines.add("§e/kill §7- Kill yourself");
    }

    private void addFullHelp(List<String> lines) {
        // Category: Teleportation & Spawn
        lines.add("§6--- Teleportation & Spawn ---");
        lines.add("§e/spawn §7- Teleport to spawn");
        lines.add("§e/setspawn §7- Set the spawn point (OP)");
        lines.add("§e/tpa <player> §7- Request to teleport to a player");
        lines.add("§e/tpahere <player> §7- Request a player to teleport to you");
        lines.add("§e/tpaccept / /tpdeny §7- Accept or deny TPA requests");
        lines.add("§e/tpcancel §7- Cancel your outgoing TPA request");
        lines.add("§e/tpaon / /tpaoff §7- Toggle receiving TPA requests");
        lines.add("§e/s <player> §7- Summon a player to you (OP)");

        // Category: Homes
        lines.add("§6--- Homes ---");
        lines.add("§e/home [1-4] §7- Teleport to your home");
        lines.add("§e/sethome <1-4> §7- Set a home location");
        lines.add("§e/delhome <1-4> §7- Delete a home location");

        // Category: Messaging & Chat
        lines.add("§6--- Messaging & Chat ---");
        lines.add("§e/msg <player> <msg> §7- Send a private message");
        lines.add("§e/r <msg> §7- Reply to the last private message");
        lines.add("§e/chat <on|off|team|...> §7- Change chat settings");
        lines.add("§e/chatsettings §7- Open chat settings GUI");

        // Category: Team System
        lines.add("§6--- Team System ---");
        lines.add("§e/team info §7- Show team information");
        lines.add("§e/team create <name> §7- Create a new team");
        lines.add("§e/team invite <player> §7- Invite a player to your team");
        lines.add("§e/team list §7- List all teams");

        // Category: Utilities & Admin
        lines.add("§6--- Utilities & Admin ---");
        lines.add("§e/stats [player] §7- View player stats");
        lines.add("§e/topstats §7- View leaderboard (GUI)");
        lines.add("§e/tpsmore [show] §7- View detailed performance info (OP)");
        lines.add("§e/v §7- Toggle vanish (OP)");
        lines.add("§e/bc <msg> §7- Broadcast a message (OP)");
        lines.add("§e/gmc/gms/gmsp/gma §7- Change gamemode (OP)");
        lines.add("§e/kill [player] §7- Kill yourself or others (OP)");
        lines.add("§e/upreload §7- Reload plugin configuration (OP)");
    }
}
