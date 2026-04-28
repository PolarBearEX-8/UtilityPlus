package com.example.utilityplus.listeners;

import com.example.utilityplus.managers.SpawnManager;
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
                player.teleport(spawnManager.getSpawn());
                player.sendMessage("§aWelcome to the server! You have been teleported to spawn.");
            }
        }
    }

    // ---------------------------------------------------------------
    // On-death / no-respawn-point teleport
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        boolean hasBedSpawn = event.isBedSpawn() || event.isAnchorSpawn();

        if (spawnManager.isTpOnDeath() || (spawnManager.isTpNoRespawnPoint() && !hasBedSpawn)) {
            if (spawnManager.isRtpEnabled()) {
                event.setRespawnLocation(spawnManager.getRandomLocation());
            } else if (spawnManager.hasSpawn()) {
                event.setRespawnLocation(spawnManager.getSpawn());
            }
        }
    }
}
