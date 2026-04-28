package com.example.utilityplus.listeners;

import com.example.utilityplus.UtilityPlus;
import com.example.utilityplus.commands.VanishCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class VanishListener implements Listener {

    private final UtilityPlus plugin;
    private final VanishCommand vanishCommand;

    public VanishListener(UtilityPlus plugin, VanishCommand vanishCommand) {
        this.plugin = plugin;
        this.vanishCommand = vanishCommand;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // 1. Hide already vanished players from the joining player
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (vanishCommand.isVanished(online.getUniqueId())) {
                if (!joiningPlayer.hasPermission("utilityplus.vanish.see")) {
                    joiningPlayer.hidePlayer(plugin, online);
                }
            }
        }
        
        // 2. If the joining player themselves should be vanished (if we add persistence later), 
        // handle it here. For now, vanish is not persistent.
    }
}
