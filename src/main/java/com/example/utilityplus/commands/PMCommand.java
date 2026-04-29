package com.example.utilityplus.commands;

import com.example.utilityplus.UtilityPlus;
import com.example.utilityplus.managers.ChatManager;
import com.example.utilityplus.util.PaperFoliaTasks;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Handles: /msg /tell /w /whisper /dm /pm  — send a private message
 *          /r /reply                        — reply to last PM sender
 *
 * Format matches vanilla Minecraft style:
 *   Sender sees:  [You -> PlayerName] message
 *   Target sees:  [PlayerName -> You] message
 */
public class PMCommand implements CommandExecutor {

    private final ChatManager chatManager;

    public PMCommand(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        Player from = (Player) sender;

        // /r /reply — reply to last PM sender
        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
            return handleReply(from, args);
        }

        // /msg /tell /w /whisper /dm /pm <player> <message>
        if (args.length < 2) {
            from.sendMessage("§cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player to = from.getServer().getPlayer(args[0]);
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cPlayer §e" + args[0] + " §cis not online!");
            return true;
        }
        if (to.equals(from)) {
            from.sendMessage("§cYou cannot message yourself!");
            return true;
        }

        String message = buildMessage(args, 1);
        sendPM(from, to, message);
        return true;
    }

    private boolean handleReply(Player from, String[] args) {
        if (args.length < 1) {
            from.sendMessage("§cUsage: /r <message>");
            return true;
        }

        UUID lastSenderUUID = chatManager.getLastPmSender(from.getUniqueId());
        if (lastSenderUUID == null) {
            from.sendMessage("§cYou have no one to reply to!");
            return true;
        }

        Player to = from.getServer().getPlayer(lastSenderUUID);
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cThat player is no longer online.");
            return true;
        }

        String message = buildMessage(args, 0);
        sendPM(from, to, message);
        return true;
    }

    private void sendPM(Player from, Player to, String message) {
        // Check if target has PM muted
        if (chatManager.isPmMuted(to.getUniqueId())) {
            from.sendMessage("§e" + to.getName() + " §7is not accepting private messages.");
            return;
        }

        // Vanilla-style format
        String toSender  = "§7[§fYou §8-> §f" + to.getName()   + "§7] §f" + message;
        String toTarget  = "§7[§f" + from.getName() + " §8-> §fYou§7] §f" + message;
        UtilityPlus plugin = JavaPlugin.getPlugin(UtilityPlus.class);

        from.sendMessage(toSender);
        PaperFoliaTasks.send(plugin, to, toTarget);

        // Track last sender for /reply
        chatManager.setLastPmSender(to.getUniqueId(),   from.getUniqueId());
        chatManager.setLastPmSender(from.getUniqueId(), to.getUniqueId());
    }

    private String buildMessage(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
