package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.managers.ChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public ChatCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        Player player = (Player) sender;

        if (label.equalsIgnoreCase("chatsettings")) {
            return handleSettings(player);
        }

        // /chat <subcommand>
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":      return handleGlobalOn(player);
            case "off":     return handleGlobalOff(player);
            case "teamon":  return handleTeamOn(player);
            case "teamoff": return handleTeamOff(player);
            case "pmon":    return handlePmOn(player);
            case "pmoff":   return handlePmOff(player);
            case "team":    return handleTeamMode(player);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleGlobalOn(Player p) {
        if (!chatManager.isGlobalMuted(p.getUniqueId())) {
            p.sendMessage("§eGlobal chat is already enabled.");
            return true;
        }
        chatManager.unmuteGlobal(p.getUniqueId());
        p.sendMessage("§aGlobal chat §aenabled§a. You will now see and send global messages.");
        return true;
    }

    private boolean handleGlobalOff(Player p) {
        if (chatManager.isGlobalMuted(p.getUniqueId())) {
            p.sendMessage("§eGlobal chat is already disabled.");
            return true;
        }
        chatManager.muteGlobal(p.getUniqueId());
        p.sendMessage("§cGlobal chat §cdisabled§c. You will no longer see or send global messages.");
        return true;
    }

    private boolean handleTeamOn(Player p) {
        if (!chatManager.isTeamChatMuted(p.getUniqueId())) {
            p.sendMessage("§eTeam chat is already enabled.");
            return true;
        }
        chatManager.unmuteTeamChat(p.getUniqueId());
        p.sendMessage("§aTeam chat §aenabled§a.");
        return true;
    }

    private boolean handleTeamOff(Player p) {
        if (chatManager.isTeamChatMuted(p.getUniqueId())) {
            p.sendMessage("§eTeam chat is already disabled.");
            return true;
        }
        chatManager.muteTeamChat(p.getUniqueId());
        p.sendMessage("§cTeam chat §cdisabled§c. You will no longer receive team messages.");
        return true;
    }

    private boolean handlePmOn(Player p) {
        if (!chatManager.isPmMuted(p.getUniqueId())) {
            p.sendMessage("§ePrivate messages are already enabled.");
            return true;
        }
        chatManager.unmutePm(p.getUniqueId());
        p.sendMessage("§aPrivate messages §aenabled§a.");
        return true;
    }

    private boolean handlePmOff(Player p) {
        if (chatManager.isPmMuted(p.getUniqueId())) {
            p.sendMessage("§ePrivate messages are already disabled.");
            return true;
        }
        chatManager.mutePm(p.getUniqueId());
        p.sendMessage("§cPrivate messages §cdisabled§c. No one can PM you.");
        return true;
    }

    private boolean handleTeamMode(Player p) {
        boolean on = chatManager.toggleTeamMode(p.getUniqueId());
        if (on) {
            p.sendMessage("§aTeam chat mode §aON§a. Everything you type will be sent to your team.");
            p.sendMessage("§7Use §e/chat team §7again to return to global chat.");
        } else {
            p.sendMessage("§eTeam chat mode §cOFF§e. You are back to global chat.");
        }
        return true;
    }

    private boolean handleSettings(Player p) {
        p.sendMessage("§6§l--- Chat Settings ---");
        p.sendMessage("§7Global Chat: " + (chatManager.isGlobalMuted(p.getUniqueId())   ? "§cOFF" : "§aON"));
        p.sendMessage("§7Team Chat:   " + (chatManager.isTeamChatMuted(p.getUniqueId()) ? "§cOFF" : "§aON"));
        p.sendMessage("§7Private Msg: " + (chatManager.isPmMuted(p.getUniqueId())       ? "§cOFF" : "§aON"));
        p.sendMessage("§7Team Mode:   " + (chatManager.isTeamMode(p.getUniqueId())      ? "§aON §7(all messages → team)" : "§cOFF"));
        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage("§6§lChat Commands:");
        p.sendMessage("§e/chat on§7/§eoff §7— Toggle global chat");
        p.sendMessage("§e/chat teamon§7/§eteamoff §7— Toggle team chat");
        p.sendMessage("§e/chat pmon§7/§epmoff §7— Toggle private messages");
        p.sendMessage("§e/chat team §7— Toggle team chat mode (redirect all typing to team)");
        p.sendMessage("§e/chatsettings §7— View your current chat settings");
    }
}
