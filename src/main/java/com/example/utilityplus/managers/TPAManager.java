package com.example.utilityplus.managers;

import com.example.utilityplus.UtilityPlus;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages TPA requests and warmup timers.
 *
 * Request model:
 *   TPARequest { requester, target, type (TO/HERE), createdAt }
 *
 * Storage:
 *   pendingRequests: target UUID → list of requests sent TO that target
 *                   (a player can receive multiple requests from different senders)
 *   tpaDisabled:    set of UUIDs that have run /tpaoff
 *   warmupTasks:    UUID of the player being teleported → cancellable task
 */
public class TPAManager {

    public enum RequestType { TO, HERE }  // TPA = requester goes TO target; TPAHERE = target comes HERE

    // ── Inner record ─────────────────────────────────────────────────
    public static class TPARequest {
        public final UUID   requesterUUID;
        public final String requesterName;
        public final UUID   targetUUID;
        public final String targetName;
        public final RequestType type;
        public final long   createdAt; // ms

        public TPARequest(Player requester, Player target, RequestType type) {
            this.requesterUUID = requester.getUniqueId();
            this.requesterName = requester.getName();
            this.targetUUID    = target.getUniqueId();
            this.targetName    = target.getName();
            this.type          = type;
            this.createdAt     = System.currentTimeMillis();
        }
    }

    // ── State ─────────────────────────────────────────────────────────
    // target UUID → most-recent request from each sender
    // Map<targetUUID, Map<requesterUUID, TPARequest>>
    private final Map<UUID, Map<UUID, TPARequest>> pendingRequests = new HashMap<>();

    // Players who have disabled TPA
    private final Set<UUID> tpaDisabled = new HashSet<>();

    // Warmup task per teleporting player UUID
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();

    // Config
    private final UtilityPlus plugin;
    private final int timeoutSeconds;
    private final int warmupSeconds;

    public TPAManager(UtilityPlus plugin) {
        this.plugin         = plugin;
        this.timeoutSeconds = plugin.getConfig().getInt("tpa.timeout", 60);
        this.warmupSeconds  = plugin.getConfig().getInt("tpa.warmup",  3);
    }

    // ── TPA toggle ───────────────────────────────────────────────────

    public boolean isTpaDisabled(UUID uuid) { return tpaDisabled.contains(uuid); }

    public void enableTpa(UUID uuid)  { tpaDisabled.remove(uuid); }
    public void disableTpa(UUID uuid) { tpaDisabled.add(uuid); }

    // ── Send request ─────────────────────────────────────────────────

    /**
     * Stores a new TPA request. Overwrites any existing request from the
     * same requester to the same target.
     */
    public void sendRequest(Player requester, Player target, RequestType type) {
        TPARequest req = new TPARequest(requester, target, type);
        pendingRequests
                .computeIfAbsent(target.getUniqueId(), k -> new HashMap<>())
                .put(requester.getUniqueId(), req);

        // Auto-expire after timeout using Folia GlobalRegionScheduler
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
            removeRequest(target.getUniqueId(), requester.getUniqueId());
        }, timeoutSeconds * 20L);
    }

    /** Returns the most recent request sent TO the target (any sender). */
    public TPARequest getLatestRequest(UUID targetUUID) {
        Map<UUID, TPARequest> map = pendingRequests.get(targetUUID);
        if (map == null || map.isEmpty()) return null;
        return map.values().stream()
                .max(Comparator.comparingLong(r -> r.createdAt))
                .orElse(null);
    }

    /** Returns the request sent by a specific requester to a specific target. */
    public TPARequest getRequest(UUID targetUUID, UUID requesterUUID) {
        Map<UUID, TPARequest> map = pendingRequests.get(targetUUID);
        if (map == null) return null;
        return map.get(requesterUUID);
    }

    public boolean hasPendingRequest(UUID targetUUID) {
        Map<UUID, TPARequest> map = pendingRequests.get(targetUUID);
        return map != null && !map.isEmpty();
    }

    /** Returns all requests sent BY a specific player (as requester). */
    public List<TPARequest> getRequestsSentBy(UUID requesterUUID) {
        List<TPARequest> result = new ArrayList<>();
        for (Map<UUID, TPARequest> map : pendingRequests.values()) {
            TPARequest r = map.get(requesterUUID);
            if (r != null) result.add(r);
        }
        return result;
    }

    public void removeRequest(UUID targetUUID, UUID requesterUUID) {
        Map<UUID, TPARequest> map = pendingRequests.get(targetUUID);
        if (map == null) return;
        map.remove(requesterUUID);
        if (map.isEmpty()) pendingRequests.remove(targetUUID);
    }

    /** Removes ALL requests sent BY a requester (used by /tpcancel). */
    public List<TPARequest> cancelAllRequestsBy(UUID requesterUUID) {
        List<TPARequest> cancelled = getRequestsSentBy(requesterUUID);
        for (TPARequest r : cancelled) {
            removeRequest(r.targetUUID, requesterUUID);
        }
        return cancelled;
    }

    // ── Warmup ───────────────────────────────────────────────────────

    public int getWarmupSeconds() { return warmupSeconds; }
    public int getTimeoutSeconds() { return timeoutSeconds; }

    public void startWarmup(UUID uuid, ScheduledTask task) {
        cancelWarmup(uuid); // cancel any existing
        warmupTasks.put(uuid, task);
    }

    public void cancelWarmup(UUID uuid) {
        ScheduledTask t = warmupTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean hasWarmup(UUID uuid) { return warmupTasks.containsKey(uuid); }
}
