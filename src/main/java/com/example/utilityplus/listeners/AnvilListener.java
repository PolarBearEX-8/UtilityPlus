package com.example.utilityplus.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Allows players to use & color codes and special symbols when renaming
 * items on an Anvil.
 *
 * How it works:
 *   1. PrepareAnvilEvent  — translate & codes in the result item's display name
 *                           so the player sees the colored preview live.
 *   2. InventoryClickEvent — when the player takes the result (slot 2),
 *                            apply the translated name to the final item.
 *
 * Special character shortcuts (type the alias, get the symbol):
 *   {heart}  → ❤   {star}   → ★   {arrow}  → ➤
 *   {skull}  → ☠   {music}  → ♪   {check}  → ✔
 *   {cross}  → ✘   {dot}    → •   {diamond} → ◆
 *   {sword}  → ⚔   {shield} → 🛡  {fire}   → 🔥
 *
 * Permission: utilityplus.anvil.color (default: true)
 */
public class AnvilListener implements Listener {

    // Result slot index in an anvil inventory
    private static final int RESULT_SLOT = 2;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player player = (Player) event.getView().getPlayer();

        if (!player.hasPermission("utilityplus.anvil.color")) return;

        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName()) return;

        // Translate & codes + special symbols
        String raw         = meta.getDisplayName();
        String translated  = translate(raw);

        if (translated.equals(raw)) return; // nothing changed, skip

        meta.setDisplayName(translated);
        result.setItemMeta(meta);
        event.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory() instanceof AnvilInventory)) return;

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("utilityplus.anvil.color")) return;
        if (event.getRawSlot() != RESULT_SLOT) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        if (!meta.hasDisplayName()) return;

        String translated = translate(meta.getDisplayName());
        meta.setDisplayName(translated);
        result.setItemMeta(meta);
    }

    // ── Translation ──────────────────────────────────────────────────

    /**
     * Translates & color codes (e.g. &c → red) and {symbol} shortcuts.
     */
    private String translate(String input) {
        String s = replaceSymbols(input);
        s = ChatColor.translateAlternateColorCodes('&', s);
        return s;
    }

    /**
     * Replaces {keyword} aliases with Unicode symbols.
     */
    private String replaceSymbols(String input) {
        return input
                .replace("{heart}",   "❤")
                .replace("{star}",    "★")
                .replace("{arrow}",   "➤")
                .replace("{skull}",   "☠")
                .replace("{music}",   "♪")
                .replace("{check}",   "✔")
                .replace("{cross}",   "✘")
                .replace("{dot}",     "•")
                .replace("{diamond}", "◆")
                .replace("{sword}",   "⚔")
                .replace("{shield}",  "🛡")
                .replace("{fire}",    "🔥")
                .replace("{crown}",   "♛")
                .replace("{lightning}","⚡")
                .replace("{infinity}", "∞")
                .replace("{flower}",  "✿")
                .replace("{moon}",    "☽")
                .replace("{sun}",     "☀");
    }
}
