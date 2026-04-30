package com.example.utilityplus.commands;

import com.example.utilityplus.managers.HomeManager;
import com.example.utilityplus.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single TabCompleter that handles all UtilityPlus commands.
 * Register this as the tab completer for every command in plugin.yml.
 *
 * Covered commands:
 *   Spawn  : /setspawn, /spawn
 *   Home   : /sethome, /home, /delhome
 *   TPA    : /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tpaon, /tpaoff
 *   Chat   : /chat, /chatsettings
 *   PM     : /msg, /tell, /w, /whisper, /dm, /pm, /r, /reply
 *   Team   : /team
 */
public class TabCompleterManager implements TabCompleter {

    private final HomeManager homeManager;
    private final TeamManager teamManager;

    // Fixed home slot names
    private static final List<String> HOME_SLOTS = Arrays.asList("1", "2", "3", "4");

    // /chat subcommands
    private static final List<String> CHAT_SUBS = Arrays.asList(
            "on", "off", "teamon", "teamoff", "pmon", "pmoff", "team"
    );

    // /team subcommands
    private static final List<String> TEAM_SUBS = Arrays.asList(
            "invite", "accept", "deny",
            "leave", "disband",
            "kick", "promote", "demote",
            "list", "chat", "info"
    );

    // /team subcommands that require a player argument
    private static final List<String> TEAM_PLAYER_SUBS = Arrays.asList(
            "kick", "promote", "demote", "invite"
    );

    public TabCompleterManager(HomeManager homeManager, TeamManager teamManager) {
        this.homeManager = homeManager;
        this.teamManager = teamManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return empty();
        Player player = (Player) sender;
        String label = command.getName().toLowerCase();

        switch (label) {

            // ── Spawn ────────────────────────────────────────────────
            case "setspawn":
            case "spawn":
                return empty(); // no arguments

            // ── Home ─────────────────────────────────────────────────
            case "sethome":
                // /sethome <1|2|3|4>  — suggest empty slots first, then occupied
                if (args.length == 1) return filter(HOME_SLOTS, args[0]);
                return empty();

            case "home":
                // /home <1|2|3|4>  — only suggest slots the player actually has
                if (args.length == 1) {
                    List<String> set = new ArrayList<>(homeManager.getHomes(player.getUniqueId()));
                    return filter(set.isEmpty() ? HOME_SLOTS : set, args[0]);
                }
                return empty();

            case "delhome":
                // /delhome <1|2|3|4>  — only suggest slots the player has
                if (args.length == 1) {
                    List<String> existing = new ArrayList<>(homeManager.getHomes(player.getUniqueId()));
                    return filter(existing.isEmpty() ? HOME_SLOTS : existing, args[0]);
                }
                return empty();

            // ── TPA ──────────────────────────────────────────────────
            case "tpa":
            case "tpahere":
                // /tpa <player>  — online players except self
                if (args.length == 1) return filterOnlinePlayers(player, args[0]);
                return empty();

            case "tpaccept":
            case "tpdeny":
            case "tpcancel":
            case "tpaon":
            case "tpaoff":
                return empty(); // no arguments

            // ── Chat ─────────────────────────────────────────────────
            case "chat":
                if (args.length == 1) return filter(CHAT_SUBS, args[0]);
                return empty();

            case "chatsettings":
                return empty();

            // ── PM ───────────────────────────────────────────────────
            case "msg":
            case "tell":
            case "w":
            case "whisper":
            case "dm":
            case "pm":
                // /msg <player> <message...>
                if (args.length == 1) return filterOnlinePlayers(player, args[0]);
                return empty(); // free-text message — no suggestions

            case "r":
            case "reply":
            case "l":
            case "last":
                return empty(); // free-text

            case "ignore":
            case "ignorehard":
            case "ignoredeathmsgs":
                if (args.length == 1) return filterOnlinePlayers(player, args[0]);
                return empty();

            case "ignorelist":
            case "togglechat":
            case "toggleprivatemsgs":
            case "toggledeathmsgs":
            case "toggledeathmsgshard":
            case "queue":
                return empty();

            // ── Team ─────────────────────────────────────────────────
            case "team":
                if (args.length == 1) {
                    return filter(TEAM_SUBS, args[0]);
                }
                if (args.length == 2) {
                    String sub = args[0].toLowerCase();

                    if (TEAM_PLAYER_SUBS.contains(sub)) {
                        if (sub.equals("invite")) {
                            // Suggest online players NOT already in a team
                            return Bukkit.getOnlinePlayers().stream()
                                    .filter(p -> !p.equals(player))
                                    .filter(p -> !teamManager.isInTeam(p.getUniqueId()))
                                    .map(Player::getName)
                                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                                    .sorted()
                                    .collect(Collectors.toList());
                        }
                        // kick/promote/demote — suggest online teammates
                        TeamManager.Team team = teamManager.getPlayerTeam(player.getUniqueId());
                        if (team != null) {
                            List<String> teammates = new ArrayList<>();
                            for (java.util.UUID uuid : team.getMembers().keySet()) {
                                if (uuid.equals(player.getUniqueId())) continue;
                                Player member = Bukkit.getPlayer(uuid);
                                if (member != null) teammates.add(member.getName());
                            }
                            return filter(teammates, args[1]);
                        }
                        return filterOnlinePlayers(player, args[1]);
                    }

                    if (sub.equals("chat")) {
                        return empty(); // free-text message
                    }
                }
                return empty();

            default:
                return empty();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Filter a list by the current partial input (case-insensitive). */
    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }

    /** Online players except the sender, filtered by partial input. */
    private List<String> filterOnlinePlayers(Player sender, String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(sender))
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> empty() {
        return new ArrayList<>();
    }
}
