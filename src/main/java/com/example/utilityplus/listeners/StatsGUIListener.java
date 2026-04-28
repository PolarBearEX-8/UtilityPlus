package com.example.utilityplus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class StatsGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        if ("§0§lTop Stats".equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
