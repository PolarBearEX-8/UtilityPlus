package com.example.utilityplus.commands;

import com.example.utilityplus.managers.TPAManager;
import com.example.utilityplus.managers.TPAManager.RequestType;
import com.example.utilityplus.managers.TPAManager.TPARequest;
import com.example.utilityplus.util.PaperFoliaTasks;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TPACommand implements CommandExecutor {

    private final TPAManager tpaManager;
    private final JavaPlugin plugin;

    public TPACommand(TPAManager tpaManager, JavaPlugin plugin) {
        this.tpaManager = tpaManager;
        this.plugin     = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        switch (label.toLowerCase()) {
            case "tpa":      return handleTpa(player, args, RequestType.TO);
            case "tpahere":  return handleTpa(player, args, RequestType.HERE);
            case "tpaccept": return handleAccept(player);
            case "tpdeny":   return handleDeny(player);
            case "tpcancel": return handleCancel(player);
            case "tpaon":    return handleTpaOn(player);
            case "tpaoff":   return handleTpaOff(player);
        }
        return false;
    }

    // ── /tpa <player>  /tpahere <player> ────────────────────────────

    private boolean handleTpa(Player requester, String[] args, RequestType type) {
        if (!requester.hasPermission("utilityplus.tpa")) {
            requester.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            requester.sendMessage(type == RequestType.TO
                    ? "§cUsage: /tpa <player>"
                    : "§cUsage: /tpahere <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage("§cPlayer §e" + args[0] + " §cis not online!");
            return true;
        }

        if (target.equals(requester)) {
            requester.sendMessage("§cYou cannot send a TPA request to yourself!");
            return true;
        }

        if (tpaManager.isTpaDisabled(target.getUniqueId())) {
            requester.sendMessage("§e" + target.getName() + " §cis not accepting teleport requests.");
            return true;
        }

        // Send the request
        tpaManager.sendRequest(requester, target, type);

        // Notify requester
        if (type == RequestType.TO) {
            requester.sendMessage("§eTeleport request sent to §a" + target.getName() + "§e.");
            requester.sendMessage("§7The request expires in §e" + tpaManager.getTimeoutSeconds() + "s§7. Use §e/tpcancel §7to cancel.");
        } else {
            requester.sendMessage("§eTeleport-here request sent to §a" + target.getName() + "§e.");
            requester.sendMessage("§7The request expires in §e" + tpaManager.getTimeoutSeconds() + "s§7. Use §e/tpcancel §7to cancel.");
        }

        // Notify target with clickable accept/deny buttons
        notifyTarget(target, requester, type);
        return true;
    }

    // ── /tpaccept ────────────────────────────────────────────────────

    private boolean handleAccept(Player target) {
        if (!target.hasPermission("utilityplus.tpa")) {
            target.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        TPARequest req = tpaManager.getLatestRequest(target.getUniqueId());
        if (req == null) {
            target.sendMessage("§cYou have no pending teleport requests!");
            return true;
        }

        Player requester = plugin.getServer().getPlayer(req.requesterUUID);
        if (requester == null || !requester.isOnline()) {
            tpaManager.removeRequest(target.getUniqueId(), req.requesterUUID);
            target.sendMessage("§cThe player who sent the request is no longer online.");
            return true;
        }

        // Remove request before warmup
        tpaManager.removeRequest(target.getUniqueId(), req.requesterUUID);

        // Determine who moves where
        Player traveller = req.type == RequestType.TO ? requester : target;
        Player destination = req.type == RequestType.TO ? target : requester;

        target.sendMessage("§aTeleport request from §e" + requester.getName() + " §aaccepted!");
        requester.sendMessage("§a" + target.getName() + " §aaccepted your teleport request!");

        startWarmup(traveller, destination);
        return true;
    }

    // ── /tpdeny ──────────────────────────────────────────────────────

    private boolean handleDeny(Player target) {
        if (!target.hasPermission("utilityplus.tpa")) {
            target.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        TPARequest req = tpaManager.getLatestRequest(target.getUniqueId());
        if (req == null) {
            target.sendMessage("§cYou have no pending teleport requests!");
            return true;
        }

        Player requester = plugin.getServer().getPlayer(req.requesterUUID);
        tpaManager.removeRequest(target.getUniqueId(), req.requesterUUID);

        target.sendMessage("§cTeleport request from §e" + req.requesterName + " §cdenied.");
        if (requester != null && requester.isOnline()) {
            requester.sendMessage("§e" + target.getName() + " §cdenied your teleport request.");
        }
        return true;
    }

    // ── /tpcancel ────────────────────────────────────────────────────

    private boolean handleCancel(Player requester) {
        if (!requester.hasPermission("utilityplus.tpa")) {
            requester.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        // Cancel warmup if requester is currently in warmup
        if (tpaManager.hasWarmup(requester.getUniqueId())) {
            tpaManager.cancelWarmup(requester.getUniqueId());
            requester.sendMessage("§cTeleport cancelled.");
            return true;
        }

        // Cancel pending outgoing requests
        List<TPARequest> cancelled = tpaManager.cancelAllRequestsBy(requester.getUniqueId());
        if (cancelled.isEmpty()) {
            requester.sendMessage("§cYou have no pending outgoing teleport requests!");
            return true;
        }

        for (TPARequest req : cancelled) {
            Player target = plugin.getServer().getPlayer(req.targetUUID);
            if (target != null && target.isOnline()) {
                target.sendMessage("§e" + requester.getName() + " §7cancelled their teleport request.");
            }
        }
        requester.sendMessage("§aTeleport request(s) cancelled.");
        return true;
    }

    // ── /tpaon / /tpaoff ─────────────────────────────────────────────

    private boolean handleTpaOn(Player player) {
        if (tpaManager.isTpaDisabled(player.getUniqueId())) {
            tpaManager.enableTpa(player.getUniqueId());
            player.sendMessage("§aTeleport requests are now §aenabled§a. Players can send you TPA requests.");
        } else {
            player.sendMessage("§eTeleport requests are already enabled.");
        }
        return true;
    }

    private boolean handleTpaOff(Player player) {
        if (!tpaManager.isTpaDisabled(player.getUniqueId())) {
            tpaManager.disableTpa(player.getUniqueId());
            player.sendMessage("§cTeleport requests are now §cdisabled§c. No one can send you TPA requests.");
        } else {
            player.sendMessage("§eTeleport requests are already disabled.");
        }
        return true;
    }

    // ── Warmup ───────────────────────────────────────────────────────

    private void startWarmup(Player traveller, Player destination) {
        int warmup = tpaManager.getWarmupSeconds();
        Location startLoc = traveller.getLocation().clone();

        if (warmup <= 0) {
            teleportToPlayer(traveller, destination);
            return;
        }

        traveller.sendMessage("§eTeleporting in §a" + warmup + "§e second(s)... §7Don't move!");

        AtomicInteger countdown = new AtomicInteger(warmup);

        // Use Folia EntityScheduler for player-specific tasks
        ScheduledTask task = PaperFoliaTasks.runForPlayerTimer(plugin, traveller, (t) -> {
            if (hasMoved(startLoc, traveller.getLocation())) {
                traveller.sendMessage("§cTeleport cancelled — you moved!");
                traveller.playSound(traveller.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
                tpaManager.cancelWarmup(traveller.getUniqueId());
                t.cancel();
                return;
            }

            int remaining = countdown.decrementAndGet();

            if (remaining <= 0) {
                tpaManager.cancelWarmup(traveller.getUniqueId());
                t.cancel();

                teleportToPlayer(traveller, destination);
                return;
            }

            // ติ้ง ทุกวินาที
            traveller.sendMessage("§eTeleporting in §a" + remaining + "§e...");
            traveller.playSound(traveller.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }, 1L, 20L);

        tpaManager.startWarmup(traveller.getUniqueId(), task);
    }

    private void teleportToPlayer(Player traveller, Player destination) {
        boolean scheduled = PaperFoliaTasks.runForPlayer(plugin, destination, () -> {
            if (!destination.isOnline()) {
                PaperFoliaTasks.runForSender(plugin, traveller, () ->
                        traveller.sendMessage("§cTeleport failed — destination player is no longer online."));
                return;
            }

            Location destinationLocation = destination.getLocation().clone();
            String destinationName = destination.getName();
            String travellerName = traveller.getName();

            PaperFoliaTasks.teleport(traveller, destinationLocation, plugin, success -> {
                if (!success) {
                    traveller.sendMessage("§cTeleport failed.");
                    return;
                }
                traveller.sendMessage("§aTeleported to §e" + destinationName + "§a!");
                traveller.playSound(traveller.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                PaperFoliaTasks.runForSender(plugin, destination, () ->
                        destination.sendMessage("§e" + travellerName + " §aarrived at your location."));
            });
        });

        if (!scheduled) {
            traveller.sendMessage("§cTeleport failed — destination player is no longer online.");
        }
    }

    private boolean hasMoved(Location from, Location to) {
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    // ── Clickable notification ────────────────────────────────────────

    private void notifyTarget(Player target, Player requester, RequestType type) {
        String typeDesc = type == RequestType.TO
                ? "§e" + requester.getName() + " §fwants to teleport §ato you."
                : "§e" + requester.getName() + " §fwants you to teleport §ato them.";

        target.sendMessage("§6§l[TPA] §r" + typeDesc);

        // Accept button
        TextComponent accept = new TextComponent("§a§l[✔ Accept]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§aClick to accept the teleport request")));

        // Deny button
        TextComponent deny = new TextComponent("  §c§l[✘ Deny]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§cClick to deny the teleport request")));

        target.spigot().sendMessage(accept, deny);
        target.sendMessage("§7This request expires in §e" + tpaManager.getTimeoutSeconds() + "s§7.");
    }
}
