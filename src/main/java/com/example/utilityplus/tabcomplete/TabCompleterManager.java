package com.example.utilityplus.tabcomplete;

import com.example.utilityplus.managers.HomeManager;
import com.example.utilityplus.managers.TeamManager;
import com.example.utilityplus.managers.TeamManager.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TabCompleterManager implements TabCompleter {

    private static final List<String> HOME_SLOTS  = Arrays.asList("1", "2", "3", "4");
    private static final List<String> CHAT_SUBS   = Arrays.asList("on","off","teamon","teamoff","pmon","pmoff","team");
    private static final List<String> TEAM_SUBS   = Arrays.asList("invite","accept","deny","leave","disband","kick","promote","demote","list","chat","info");
    private static final List<String> TPA_NO_ARGS = Arrays.asList("tpaccept","tpdeny","tpcancel","tpaon","tpaoff");
    // subcommands ที่ไม่ต้องการ player argument
    private static final List<String> TEAM_NO_PLAYER = Arrays.asList("accept","deny","leave","disband","list","info","chat");

    private final HomeManager homeManager;
    private final TeamManager teamManager;

    public TabCompleterManager(HomeManager homeManager, TeamManager teamManager) {
        this.homeManager = homeManager;
        this.teamManager = teamManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = alias.toLowerCase();

        // ── Spawn ────────────────────────────────────────────────────
        if (cmd.equals("setspawn") || cmd.equals("spawn")) {
            return Collections.emptyList();
        }

        // ── Home ─────────────────────────────────────────────────────
        if (cmd.equals("home") || cmd.equals("sethome") || cmd.equals("delhome")) {
            if (args.length != 1 || !(sender instanceof Player)) return Collections.emptyList();
            Player p = (Player) sender;
            String input = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String slot : HOME_SLOTS) {
                if (!slot.startsWith(input)) continue;
                if (cmd.equals("delhome") && homeManager.getHome(p.getUniqueId(), slot) == null) continue;
                result.add(slot);
            }
            return result;
        }

        // ── TPA (no-arg commands) ────────────────────────────────────
        if (TPA_NO_ARGS.contains(cmd)) {
            return Collections.emptyList();
        }

        // ── TPA / tpahere ────────────────────────────────────────────
        if (cmd.equals("tpa") || cmd.equals("tpahere")) {
            if (args.length != 1) return Collections.emptyList();
            return onlinePlayers(sender, args[0]);
        }

        // ── Chat ─────────────────────────────────────────────────────
        if (cmd.equals("chat")) {
            if (args.length != 1) return Collections.emptyList();
            return filter(CHAT_SUBS, args[0]);
        }
        if (cmd.equals("chatsettings")) {
            return Collections.emptyList();
        }

        // ── PM / reply ───────────────────────────────────────────────
        if (cmd.equals("r") || cmd.equals("reply")) {
            return Collections.emptyList();
        }
        if (Arrays.asList("msg","tell","w","whisper","dm","pm").contains(cmd)) {
            if (args.length != 1) return Collections.emptyList();
            return onlinePlayers(sender, args[0]);
        }

        // ── Team ─────────────────────────────────────────────────────
        if (cmd.equals("team")) {
            if (args.length == 1) return filter(TEAM_SUBS, args[0]);

            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (TEAM_NO_PLAYER.contains(sub)) return Collections.emptyList();
                if (!(sender instanceof Player)) return Collections.emptyList();

                Player actor = (Player) sender;
                String input = args[1].toLowerCase();

                // /team invite — แนะนำ online players ที่ยังไม่มีทีม
                if (sub.equals("invite")) {
                    List<String> names = new ArrayList<>();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.equals(actor)) continue;
                        if (teamManager.isInTeam(p.getUniqueId())) continue;
                        if (p.getName().toLowerCase().startsWith(input)) names.add(p.getName());
                    }
                    return names;
                }

                // /team kick / promote / demote — แนะนำเฉพาะ teammate ที่ rank ต่ำกว่า
                Team team = teamManager.getPlayerTeam(actor.getUniqueId());
                if (team == null) return Collections.emptyList();

                TeamManager.Role actorRole = team.getRole(actor.getUniqueId());
                List<String> names = new ArrayList<>();
                for (Map.Entry<UUID, TeamManager.Role> entry : team.getMembers().entrySet()) {
                    if (entry.getKey().equals(actor.getUniqueId())) continue;
                    if (!actorRole.isHigherThan(entry.getValue())) continue;
                    Player member = Bukkit.getPlayer(entry.getKey());
                    if (member != null && member.getName().toLowerCase().startsWith(input)) {
                        names.add(member.getName());
                    }
                }
                return names;
            }
        }

        return Collections.emptyList();
    }

    private List<String> onlinePlayers(CommandSender sender, String input) {
        String low = input.toLowerCase();
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && p.equals(sender)) continue;
            if (p.getName().toLowerCase().startsWith(low)) names.add(p.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String input) {
        String low = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.startsWith(low)) result.add(opt);
        }
        return result;
    }
}