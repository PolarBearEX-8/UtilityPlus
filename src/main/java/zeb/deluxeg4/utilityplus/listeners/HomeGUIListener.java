package zeb.deluxeg4.utilityplus.listeners;

import zeb.deluxeg4.utilityplus.commands.HomeCommand;
import zeb.deluxeg4.utilityplus.gui.HomeGUI;
import zeb.deluxeg4.utilityplus.managers.HomeManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HomeGUIListener implements Listener {

    private final HomeManager homeManager;

    public HomeGUIListener(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        if (!HomeGUI.getTitle().equals(event.getView().getTitle())) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();

        // ── Bed slot — teleport with warmup ─────────────────────────
        String bedSlot = HomeGUI.getBedSlotName(slot);
        if (bedSlot != null) {
            player.closeInventory();
            if (homeManager.hasHome(player.getUniqueId(), bedSlot)) {
                if (homeManager.hasWarmup(player.getUniqueId())) {
                    player.sendMessage("§eAlready teleporting!"); return;
                }
                Location dest = homeManager.getHome(player.getUniqueId(), bedSlot);
                HomeCommand.startWarmup(player, bedSlot, dest, homeManager);
            } else {
                suggestCommand(player, "/sethome " + bedSlot);
            }
            return;
        }

        // ── Delete slot ──────────────────────────────────────────────
        String delSlot = HomeGUI.getDeleteSlotName(slot);
        if (delSlot != null) {
            if (homeManager.hasHome(player.getUniqueId(), delSlot)) {
                homeManager.deleteHome(player.getUniqueId(), delSlot);
                player.sendMessage("§aHome §e" + delSlot + " §adeleted!");
                HomeGUI.open(player, homeManager);
            } else {
                player.sendMessage("§cHome §e" + delSlot + " §cis not set!");
            }
        }
    }

    private void suggestCommand(Player player, String command) {
        player.sendMessage("§eHome not set — click to fill the command:");
        TextComponent link = new TextComponent("§6§l» " + command + " §7(click to fill)");
        link.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to pre-fill: §f" + command)));
        player.spigot().sendMessage(link);
    }
}
