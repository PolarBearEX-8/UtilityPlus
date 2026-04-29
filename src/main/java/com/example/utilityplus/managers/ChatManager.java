package com.example.utilityplus.managers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player chat preferences (all in-memory, resets on restart).
 *
 * Flags:
 *  - globalMuted   : player has used /chat off  → won't see or send global chat
 *  - teamChatMuted : player has used /chat teamoff → won't receive team messages
 *  - pmMuted       : player has used /chat pmoff   → won't receive PMs
 *  - teamMode      : player has used /chat team    → every normal message goes to team chat
 *  - lastPmSender  : UUID of the last player who PM'd them (for /reply)
 */
public class ChatManager {

    private final Set<UUID> globalMuted   = ConcurrentHashMap.newKeySet();
    private final Set<UUID> teamChatMuted = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pmMuted       = ConcurrentHashMap.newKeySet();
    private final Set<UUID> teamMode      = ConcurrentHashMap.newKeySet();

    // Last PM sender per player (for /r /reply)
    private final java.util.Map<UUID, UUID> lastPmSender = new ConcurrentHashMap<>();

    // ── Global chat ──────────────────────────────────────────────────
    public boolean isGlobalMuted(UUID uuid)   { return globalMuted.contains(uuid); }
    public void muteGlobal(UUID uuid)         { globalMuted.add(uuid); }
    public void unmuteGlobal(UUID uuid)       { globalMuted.remove(uuid); }

    // ── Team chat ────────────────────────────────────────────────────
    public boolean isTeamChatMuted(UUID uuid) { return teamChatMuted.contains(uuid); }
    public void muteTeamChat(UUID uuid)       { teamChatMuted.add(uuid); }
    public void unmuteTeamChat(UUID uuid)     { teamChatMuted.remove(uuid); }

    // ── PM ───────────────────────────────────────────────────────────
    public boolean isPmMuted(UUID uuid)       { return pmMuted.contains(uuid); }
    public void mutePm(UUID uuid)             { pmMuted.add(uuid); }
    public void unmutePm(UUID uuid)           { pmMuted.remove(uuid); }

    // ── Team mode (redirect normal chat to team) ─────────────────────
    public boolean isTeamMode(UUID uuid)      { return teamMode.contains(uuid); }
    public void enableTeamMode(UUID uuid)     { teamMode.add(uuid); }
    public void disableTeamMode(UUID uuid)    { teamMode.remove(uuid); }
    public boolean toggleTeamMode(UUID uuid) {
        if (teamMode.contains(uuid)) { teamMode.remove(uuid); return false; }
        else                         { teamMode.add(uuid);    return true;  }
    }

    // ── Reply tracking ───────────────────────────────────────────────
    public UUID getLastPmSender(UUID uuid)          { return lastPmSender.get(uuid); }
    public void setLastPmSender(UUID target, UUID sender) { lastPmSender.put(target, sender); }
}
