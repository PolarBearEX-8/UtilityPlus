package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.managers.ChatManager;
import zeb.deluxeg4.utilityplus.managers.TeamManager;
import zeb.deluxeg4.utilityplus.managers.TeamManager.Invite;
import zeb.deluxeg4.utilityplus.managers.TeamManager.Role;
import zeb.deluxeg4.utilityplus.managers.TeamManager.Team;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    private final TeamManager teamManager;
    private final ChatManager chatManager;
    private final UtilityPlus plugin;

    public TeamCommand(TeamManager teamManager, ChatManager chatManager) {
        this.teamManager = teamManager;
        this.chatManager = chatManager;
        this.plugin = JavaPlugin.getPlugin(UtilityPlus.class);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!"); return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) { sendUsage(player); return true; }

        switch (args[0].toLowerCase()) {
            case "invite":  return handleInvite(player, args);
            case "accept":  return handleAccept(player);
            case "deny":    return handleDeny(player);
            case "leave":   return handleLeave(player);
            case "disband": return handleDisband(player);
            case "kick":    return handleKick(player, args);
            case "promote": return handlePromote(player, args);
            case "demote":  return handleDemote(player, args);
            case "list":    return handleList(player);
            case "chat":    return handleChat(player, args);
            case "info":    return handleInfo(player);
            default: sendUsage(player); return true;
        }
    }

    // ── /team invite <player> ────────────────────────────────────────
    // Auto-creates team on first invite if player has no team yet
    private boolean handleInvite(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cUsage: /team invite <player>"); return true; }

        // Auto-create team if not in one yet
        if (!teamManager.isInTeam(p.getUniqueId())) {
            String createErr = teamManager.createTeam(p.getUniqueId(), p.getName());
            if (createErr != null) { p.sendMessage("§c" + createErr); return true; }
            Team created = teamManager.getPlayerTeam(p.getUniqueId());
            p.sendMessage("§aCreated §6" + created.displayName + "§a! You are the §6Owner§a.");
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            p.sendMessage("§cPlayer §e" + args[1] + " §cis not online!"); return true;
        }
        if (target.equals(p)) { p.sendMessage("§cYou cannot invite yourself!"); return true; }

        String error = teamManager.sendInvite(p.getUniqueId(), p.getName(), target.getUniqueId());
        if (error != null) { p.sendMessage("§c" + error); return true; }

        p.sendMessage("§aInvite sent to §e" + target.getName()
                + "§a! Expires in §e" + teamManager.getInviteTimeoutSeconds() + "s§a.");

        // Clickable notification to target
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        PaperFoliaTasks.send(plugin, target, "§6§l[Team Invite] §r§e" + p.getName()
                + " §finvited you to join §6" + team.displayName + "§f!");

        TextComponent accept = new TextComponent("§a§l[✔ Accept]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aAccept the invite")));

        TextComponent space = new TextComponent("  ");

        TextComponent deny = new TextComponent("§c§l[✘ Deny]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team deny"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§cDeny the invite")));

        PaperFoliaTasks.runForPlayer(plugin, target, () -> {
            target.spigot().sendMessage(accept, space, deny);
            target.sendMessage("§7Expires in §e" + teamManager.getInviteTimeoutSeconds() + "s§7.");
        });
        return true;
    }

    // ── /team accept ─────────────────────────────────────────────────
    private boolean handleAccept(Player p) {
        Invite inv = teamManager.getInvite(p.getUniqueId());
        if (inv == null) { p.sendMessage("§cYou have no pending invite (or it expired)."); return true; }

        String error = teamManager.acceptInvite(p.getUniqueId());
        if (error != null) { p.sendMessage("§c" + error); return true; }

        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        p.sendMessage("§aYou joined §6" + team.displayName + "§a!");
        TeamManager.broadcastToTeam(team, "§e" + p.getName() + " §ajoined the team!", p.getUniqueId());
        return true;
    }

    // ── /team deny ───────────────────────────────────────────────────
    private boolean handleDeny(Player p) {
        Invite inv = teamManager.getInvite(p.getUniqueId());
        if (inv == null) { p.sendMessage("§cYou have no pending invite."); return true; }

        teamManager.removeInvite(p.getUniqueId());
        p.sendMessage("§eInvite denied.");
        Player inviter = Bukkit.getPlayer(inv.inviterUUID);
        if (inviter != null) PaperFoliaTasks.send(plugin, inviter, "§e" + p.getName() + " §7denied your invite.");
        return true;
    }

    // ── /team leave ──────────────────────────────────────────────────
    private boolean handleLeave(Player p) {
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }
        if (team.getRole(p.getUniqueId()) == Role.OWNER && team.size() > 1) {
            p.sendMessage("§cYou are the Owner! Promote someone first or use §e/team disband§c.");
            return true;
        }
        String name = team.displayName;
        boolean disbanded = teamManager.leaveTeam(p.getUniqueId());
        if (disbanded) {
            p.sendMessage("§eYou left §6" + name + "§e. Team disbanded.");
        } else {
            p.sendMessage("§eYou left §6" + name + "§e.");
            // Notify remaining members — re-fetch team since displayName may have updated
            Team remaining = teamManager.getAllTeams().stream()
                    .filter(t -> t.id.equals(team.id)).findFirst().orElse(null);
            if (remaining != null)
                TeamManager.broadcastToTeam(remaining, "§e" + p.getName() + " §7left the team.", null);
        }
        return true;
    }

    // ── /team disband ────────────────────────────────────────────────
    private boolean handleDisband(Player p) {
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }
        if (team.getRole(p.getUniqueId()) != Role.OWNER) {
            p.sendMessage("§cOnly the Owner can disband the team."); return true;
        }
        TeamManager.broadcastToTeam(team, "§c§lTeam has been disbanded by the Owner.", p.getUniqueId());
        teamManager.disbandTeam(team);
        p.sendMessage("§cTeam disbanded.");
        return true;
    }

    // ── /team kick <player> ──────────────────────────────────────────
    private boolean handleKick(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cUsage: /team kick <player>"); return true; }
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }

        Role actorRole = team.getRole(p.getUniqueId());
        if (actorRole == Role.MEMBER) { p.sendMessage("§cOnly Owner or Co-Leader can kick."); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage("§cPlayer §e" + args[1] + " §cis not online!"); return true; }
        if (!team.hasMember(target.getUniqueId())) {
            p.sendMessage("§e" + target.getName() + " §cis not in your team!"); return true; }
        if (target.equals(p)) { p.sendMessage("§cUse /team leave to leave."); return true; }
        if (!actorRole.isHigherThan(team.getRole(target.getUniqueId()))) {
            p.sendMessage("§cYou cannot kick someone of equal or higher rank."); return true; }

        teamManager.kickMember(team, target.getUniqueId());
        p.sendMessage("§aKicked §e" + target.getName() + "§a.");
        PaperFoliaTasks.send(plugin, target, "§cYou were kicked from §e" + team.displayName + "§c.");
        TeamManager.broadcastToTeam(team, "§e" + target.getName() + " §7was kicked.", target.getUniqueId());
        return true;
    }

    // ── /team promote <player> ───────────────────────────────────────
    private boolean handlePromote(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cUsage: /team promote <player>"); return true; }
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.hasMember(target.getUniqueId())) {
            p.sendMessage("§e" + args[1] + " §cis not in your team or is offline!"); return true; }
        String error = teamManager.promote(team, p.getUniqueId(), target.getUniqueId());
        if (error != null) { p.sendMessage("§c" + error); return true; }

        Role newRole = team.getRole(target.getUniqueId());
        TeamManager.broadcastToTeam(team, "§e" + target.getName() + " §7→ §6" + newRole.getDisplay(), null);
        PaperFoliaTasks.send(plugin, target, "§aPromoted to §6" + newRole.getDisplay() + "§a!");
        return true;
    }

    // ── /team demote <player> ────────────────────────────────────────
    private boolean handleDemote(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("§cUsage: /team demote <player>"); return true; }
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !team.hasMember(target.getUniqueId())) {
            p.sendMessage("§e" + args[1] + " §cis not in your team or is offline!"); return true; }
        String error = teamManager.demote(team, p.getUniqueId(), target.getUniqueId());
        if (error != null) { p.sendMessage("§c" + error); return true; }

        Role newRole = team.getRole(target.getUniqueId());
        TeamManager.broadcastToTeam(team, "§e" + target.getName() + " §7demoted to §6" + newRole.getDisplay(), null);
        PaperFoliaTasks.send(plugin, target, "§cDemoted to §6" + newRole.getDisplay() + "§c.");
        return true;
    }

    // ── /team list ───────────────────────────────────────────────────
    private boolean handleList(Player p) {
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }

        p.sendMessage("§6§l--- " + team.displayName + " (" + team.size() + "/" + teamManager.getMaxMembers() + ") ---");
        for (Map.Entry<UUID, Role> entry : team.getMembers().entrySet()) {
            Player member = Bukkit.getPlayer(entry.getKey());
            String name   = member != null ? member.getName()
                    : "§8" + (Bukkit.getOfflinePlayer(entry.getKey()).getName() != null
                              ? Bukkit.getOfflinePlayer(entry.getKey()).getName() : "Unknown");
            String dot     = member != null ? "§a● " : "§7○ ";
            String roleCol = entry.getValue() == Role.OWNER    ? "§6" :
                             entry.getValue() == Role.CO_LEADER ? "§b" : "§f";
            p.sendMessage(dot + roleCol + "[" + entry.getValue().getDisplay() + "] §r" + name);
        }
        return true;
    }

    // ── /team chat <message> ─────────────────────────────────────────
    private boolean handleChat(Player p, String[] args) {
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }
        if (args.length < 2) { p.sendMessage("§cUsage: /team chat <message>"); return true; }
        sendTeamMessage(p, team, buildMessage(args, 1));
        return true;
    }

    // ── /team info ───────────────────────────────────────────────────
    private boolean handleInfo(Player p) {
        Team team = teamManager.getPlayerTeam(p.getUniqueId());
        if (team == null) { p.sendMessage("§cYou are not in a team!"); return true; }

        p.sendMessage("§6§l--- Team Info ---");
        p.sendMessage("§7Team:      §e" + team.displayName);
        p.sendMessage("§7Your Role: §6" + team.getRole(p.getUniqueId()).getDisplay());
        p.sendMessage("§7Members:   §e" + team.size() + "§7/§e" + teamManager.getMaxMembers());
        p.sendMessage("§7Team Mode: " + (chatManager.isTeamMode(p.getUniqueId()) ? "§aON" : "§cOFF"));
        p.sendMessage("§7Auto-kick: §e" + teamManager.getOfflineKickMinutes() + " min offline");
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    public static void sendTeamMessage(Player sender, Team team, String message) {
        String fmt = "§7[§6" + team.displayName + "§7] §f" + sender.getName() + "§7: §f" + message;
        for (UUID uuid : team.getMembers().keySet()) {
            Player m = Bukkit.getPlayer(uuid);
            if (m != null) {
                UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);
                PaperFoliaTasks.send(plugin, m, fmt);
            }
        }
    }

    private String buildMessage(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) { if (i > from) sb.append(" "); sb.append(args[i]); }
        return sb.toString();
    }

    private void sendUsage(Player p) {
        p.sendMessage("§6§lTeam Commands:");
        p.sendMessage("§e/team invite <player> §7— Invite (auto-creates team)");
        p.sendMessage("§e/team accept §7— Accept an invite");
        p.sendMessage("§e/team deny §7— Deny an invite");
        p.sendMessage("§e/team leave §7— Leave your team");
        p.sendMessage("§e/team disband §7— Disband (Owner only)");
        p.sendMessage("§e/team kick <player> §7— Kick a member");
        p.sendMessage("§e/team promote <player> §7— Promote a member");
        p.sendMessage("§e/team demote <player> §7— Demote a member");
        p.sendMessage("§e/team list §7— List members");
        p.sendMessage("§e/team chat <msg> §7— Send team message");
        p.sendMessage("§e/team info §7— Team info");
    }
}
