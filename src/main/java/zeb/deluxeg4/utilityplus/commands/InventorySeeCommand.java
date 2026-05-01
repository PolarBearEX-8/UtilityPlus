package zeb.deluxeg4.utilityplus.commands;

import zeb.deluxeg4.utilityplus.UtilityPlus;
import zeb.deluxeg4.utilityplus.util.PaperFoliaTasks;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InventorySeeCommand implements CommandExecutor {

    private final UtilityPlus plugin;

    public InventorySeeCommand(UtilityPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        String permission = cmd.equals("enderchestsee") ? "utilityplus.enderchestsee" : "utilityplus.invsee";

        if (!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("§cOnly players can open this GUI.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }

        PaperFoliaTasks.runForPlayer(plugin, viewer, () -> {
            if (cmd.equals("enderchestsee")) {
                viewer.openInventory(target.getEnderChest());
                viewer.sendMessage("§aOpened §e" + target.getName() + "§a's ender chest.");
                return;
            }

            viewer.openInventory(target.getInventory());
            viewer.sendMessage("§aOpened §e" + target.getName() + "§a's inventory.");
        });
        return true;
    }
}
