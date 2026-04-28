package com.example.utilityplus.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("utilityplus.gamemode")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        Player target;
        GameMode mode;

        // Determine gamemode from command label
        switch (label.toLowerCase()) {
            case "gmc":
                mode = GameMode.CREATIVE;
                break;
            case "gms":
                mode = GameMode.SURVIVAL;
                break;
            case "gmsp":
                mode = GameMode.SPECTATOR;
                break;
            case "gma":
                mode = GameMode.ADVENTURE;
                break;
            default:
                sender.sendMessage("§cUnknown gamemode command.");
                return true;
        }

        // Determine target player
        if (args.length >= 1) {
            // Target specified
            target = sender.getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
            if (!sender.hasPermission("utilityplus.gamemode.others")) {
                sender.sendMessage("§cYou don't have permission to change other players' gamemode.");
                return true;
            }
        } else {
            // Self target
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player. Usage: /" + label + " <player>");
                return true;
            }
            target = (Player) sender;
        }

        // Set gamemode
        target.setGameMode(mode);

        String modeName = mode.name().toLowerCase();
        String displayMode = modeName.substring(0, 1).toUpperCase() + modeName.substring(1);

        if (target == sender) {
            target.sendMessage("§aYour gamemode has been changed to §e" + displayMode + "§a!");
        } else {
            target.sendMessage("§aYour gamemode has been changed to §e" + displayMode + " §aby §e" + sender.getName() + "§a!");
            sender.sendMessage("§aChanged §e" + target.getName() + "§a's gamemode to §e" + displayMode + "§a!");
        }

        return true;
    }
}
