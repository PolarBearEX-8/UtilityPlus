package com.example.utilityplus.gui;

import com.example.utilityplus.managers.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class HomeGUI {

    public static final int[] BED_SLOTS = {10, 12, 14, 16};
    public static final int[] DEL_SLOTS = {19, 21, 23, 25};

    private static final String GUI_TITLE = "§6§lHOMES";

    private static final Material[] BED_COLORS_SET = {
            Material.LIGHT_BLUE_BED, Material.CYAN_BED,
            Material.BLUE_BED, Material.PURPLE_BED
    };
    private static final Material BED_EMPTY = Material.RED_BED;
    private static final Material DEL_SET   = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final Material DEL_EMPTY = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material FILL      = Material.BLACK_STAINED_GLASS_PANE;

    private HomeGUI() {}

    public static void open(Player player, HomeManager homeManager) {
        player.openInventory(build(player, homeManager));
    }

    public static Inventory build(Player player, HomeManager homeManager) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        ItemStack filler = makeItem(FILL, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

        List<String> activeSlots = homeManager.getActiveSlots();

        for (int i = 0; i < activeSlots.size(); i++) {
            String slot    = activeSlots.get(i);
            boolean hasHome = homeManager.hasHome(player.getUniqueId(), slot);
            Location loc    = hasHome ? homeManager.getHome(player.getUniqueId(), slot) : null;

            inv.setItem(BED_SLOTS[i], buildBedItem(slot, i, hasHome, loc,
                    homeManager.getWarmupSeconds()));
            inv.setItem(DEL_SLOTS[i], buildDeleteItem(slot, hasHome));
        }

        return inv;
    }

    private static ItemStack buildBedItem(String slot, int index, boolean hasHome,
                                          Location loc, int warmup) {
        Material mat  = hasHome ? BED_COLORS_SET[index] : BED_EMPTY;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();

        if (hasHome) {
            meta.setDisplayName("§a§lHome " + slot);
            String warmupLine = warmup > 0 ? "§7Warmup: §e" + warmup + "s" : "§7Warmup: §aInstant";
            meta.setLore(Arrays.asList(
                    "§7World: §f" + loc.getWorld().getName(),
                    "§7X: §f" + String.format("%.1f", loc.getX()),
                    "§7Y: §f" + String.format("%.1f", loc.getY()),
                    "§7Z: §f" + String.format("%.1f", loc.getZ()),
                    "",
                    warmupLine,
                    "§e▶ Click to teleport"
            ));
        } else {
            meta.setDisplayName("§c§lHome " + slot);
            meta.setLore(Arrays.asList(
                    "§7This home is not set.",
                    "",
                    "§e▶ Click to set this home"
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildDeleteItem(String slot, boolean hasHome) {
        ItemStack item = new ItemStack(hasHome ? DEL_SET : DEL_EMPTY);
        ItemMeta  meta = item.getItemMeta();
        if (hasHome) {
            meta.setDisplayName("§c§lDelete Home " + slot);
            meta.setLore(Arrays.asList("§7Click to delete.", "§c⚠ Cannot be undone!"));
        } else {
            meta.setDisplayName("§7Delete Home " + slot);
            meta.setLore(List.of("§8No home to delete."));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static String getBedSlotName(int rawSlot) {
        for (int i = 0; i < BED_SLOTS.length; i++)
            if (BED_SLOTS[i] == rawSlot) return HomeManager.ALL_SLOTS.get(i);
        return null;
    }

    public static String getDeleteSlotName(int rawSlot) {
        for (int i = 0; i < DEL_SLOTS.length; i++)
            if (DEL_SLOTS[i] == rawSlot) return HomeManager.ALL_SLOTS.get(i);
        return null;
    }

    public static String getTitle() { return GUI_TITLE; }
}
