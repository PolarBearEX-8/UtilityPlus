package zeb.deluxeg4.utilityplus.tabcomplete;

import zeb.deluxeg4.utilityplus.managers.TeamManager;
import zeb.deluxeg4.utilityplus.managers.TeamManager.Team;
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

/**
 * /team <create|leave|kick|promote|demote|list|chat|info>
 *
 * create  — arg2: (type a name)
 * leave   — no args
 * kick    — arg2: online team members (excluding self)
 * promote — arg2: online team members that can be promoted
 * demote  — arg2: online team members that can be demoted
 * list    — no args
 * chat    — arg2+: free text
 * info    — no args
 */
public class TeamTabComplete implements TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList("create", "leave", "kick", "promote", "demote", "list", "chat", "info");

    private static final List<String> NO_PLAYER_ARG =
            Arrays.asList("leave", "list", "info", "create", "chat");

    private final TeamManager teamManager;

    public TeamTabComplete(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;

        // arg1 = subcommand
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) result.add(sub);
            }
            return result;
        }

        // arg2 onwards
        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (NO_PLAYER_ARG.contains(sub)) return Collections.emptyList();

            // kick / promote / demote → suggest online team members
            Team team = teamManager.getPlayerTeam(player.getUniqueId());
            if (team == null) return Collections.emptyList();

            TeamManager.Role actorRole = team.getRole(player.getUniqueId());
            String input = args[1].toLowerCase();
            List<String> names = new ArrayList<>();

            for (Map.Entry<UUID, TeamManager.Role> entry : team.getMembers().entrySet()) {
                if (entry.getKey().equals(player.getUniqueId())) continue;

                // Only suggest members the actor outranks
                if (!actorRole.isHigherThan(entry.getValue())) continue;

                Player member = Bukkit.getPlayer(entry.getKey());
                if (member != null && member.getName().toLowerCase().startsWith(input)) {
                    names.add(member.getName());
                }
            }
            return names;
        }

        return Collections.emptyList();
    }
}
