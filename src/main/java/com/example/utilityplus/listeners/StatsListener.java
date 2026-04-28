package com.example.utilityplus.listeners;

import com.example.utilityplus.managers.StatsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class StatsListener implements Listener {

    private final StatsManager statsManager;

    public StatsListener(StatsManager statsManager) {
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Count death for victim
        statsManager.addDeath(victim.getUniqueId(), victim.getName());

        // Count kill for killer
        if (killer != null) {
            statsManager.addKill(killer.getUniqueId(), killer.getName());
        }
        
        // Auto-save every few deaths or use periodic save
        // For simplicity, we save on every death/kill here, but in a busy server 
        // you might want to save periodically.
        statsManager.saveData();
    }
}
