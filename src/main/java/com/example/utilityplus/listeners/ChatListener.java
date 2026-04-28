package com.example.utilityplus.listeners;

import com.example.utilityplus.commands.TeamCommand;
import com.example.utilityplus.managers.ChatManager;
import com.example.utilityplus.managers.TeamManager;
import com.example.utilityplus.managers.TeamManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Iterator;

public class ChatListener implements Listener {

    private final ChatManager chatManager;
    private final TeamManager teamManager;

    public ChatListener(ChatManager chatManager, TeamManager teamManager) {
        this.chatManager = chatManager;
        this.teamManager = teamManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();

        // ── Team mode: redirect all messages to team chat ────────────
        if (chatManager.isTeamMode(sender.getUniqueId())) {
            event.setCancelled(true);
            Team team = teamManager.getPlayerTeam(sender.getUniqueId());
            if (team == null) {
                sender.sendMessage("§cYou are not in a team! Team mode disabled.");
                chatManager.disableTeamMode(sender.getUniqueId());
                return;
            }
            TeamCommand.sendTeamMessage(sender, team, message);
            return;
        }

        // ── Global chat muted: block sending ─────────────────────────
        if (chatManager.isGlobalMuted(sender.getUniqueId())) {
            event.setCancelled(true);
            sender.sendMessage("§cGlobal chat is disabled. Use §e/chat on §cto re-enable.");
            return;
        }

        // ── Filter recipients who have global chat muted ──────────────
        Iterator<Player> recipients = event.getRecipients().iterator();
        while (recipients.hasNext()) {
            Player recipient = recipients.next();
            if (chatManager.isGlobalMuted(recipient.getUniqueId())) {
                recipients.remove();
            }
        }
        // Keep sender in recipients only if they can see their own message (not global muted)
        if (!chatManager.isGlobalMuted(sender.getUniqueId())) {
            event.getRecipients().add(sender);
        }
    }
}
