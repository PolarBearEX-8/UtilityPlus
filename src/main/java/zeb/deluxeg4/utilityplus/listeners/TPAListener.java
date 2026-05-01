package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.managers.TPAManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class TPAListener implements Listener {

    private final TPAManager tpaManager;

    public TPAListener(TPAManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    /**
     * When a player quits:
     * - Cancel their warmup if they're in one
     * - Cancel all outgoing requests they sent
     * - Remove all incoming requests targeted at them
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Cancel warmup
        tpaManager.cancelWarmup(player.getUniqueId());

        // Cancel outgoing requests
        tpaManager.cancelAllRequestsBy(player.getUniqueId());

        // Note: incoming requests targeting this player will expire naturally via the scheduler.
        // No need to explicitly remove them — getLatestRequest() will ignore offline senders.
    }
}
