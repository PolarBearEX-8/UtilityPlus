package com.example.utilityplus.listeners;

import com.example.utilityplus.managers.SpawnManager;
import com.example.utilityplus.util.PaperFoliaTasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class SpawnListener implements Listener {

    private final SpawnManager spawnManager;

    public SpawnListener(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    // ---------------------------------------------------------------
    // Resolve spawn world if it wasn't loaded during onEnable
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (spawnManager.hasPendingWorld()) {
            spawnManager.tryResolvePendingWorld(event.getWorld().getName());
        }
    }

    // ---------------------------------------------------------------
    // First-join teleport
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!spawnManager.isTpOnFirstJoin()) return;

        Player player = event.getPlayer();

        if (spawnManager.isFirstJoin(player.getUniqueId())) {
            spawnManager.markKnown(player.getUniqueId());
            
            // Only teleport to spawn on first join, NO RTP here.
            if (spawnManager.hasSpawn()) {
                PaperFoliaTasks.teleport(player, spawnManager.getSpawn(), spawnManager.getPlugin(), success -> {
                    if (success) {
                        player.sendMessage("§aWelcome to the server! You have been teleported to spawn.");
                    }
                });
            }
        }
    }

    // ---------------------------------------------------------------
    // On-death / no-respawn-point teleport
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        boolean hasBedSpawn = event.isBedSpawn() || event.isAnchorSpawn();

        // Priority: Bed/Anchor spawn first
        if (hasBedSpawn) {
            return; // Keep bed/anchor spawn, no random respawn
        }

        // No bed/anchor — use random respawn if enabled
        if (spawnManager.isRtpEnabled()) {
            event.setRespawnLocation(spawnManager.getRandomLocation());
            return;
        }

        // Fallback to normal spawn if configured
        if (spawnManager.isTpNoRespawnPoint() || spawnManager.isTpOnDeath()) {
            if (spawnManager.hasSpawn()) {
                event.setRespawnLocation(spawnManager.getSpawn());
            }
        }
    }
}
