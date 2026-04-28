package com.example.utilityplus.listeners;

import com.example.utilityplus.managers.TeamManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TeamListener implements Listener {

    private final TeamManager teamManager;

    public TeamListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        teamManager.recordOffline(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        teamManager.recordOnline(event.getPlayer().getUniqueId());
    }
}
