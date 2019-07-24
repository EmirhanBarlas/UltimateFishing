package com.songoda.ultimatefishing.command.commands;

import com.songoda.ultimatefishing.UltimateFishing;
import com.songoda.ultimatefishing.command.AbstractCommand;
import com.songoda.ultimatefishing.gui.GUISell;
import com.songoda.ultimatefishing.rarity.Rarity;
import com.songoda.ultimatefishing.utils.Methods;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandSellAll extends AbstractCommand {

    public CommandSellAll(AbstractCommand parent) {
        super("sellall", parent, true);
    }

    @Override
    protected ReturnType runCommand(UltimateFishing instance, CommandSender sender, String... args) {

        Player player = (Player)sender;

        double totalNew = Methods.calculateTotal(player.getInventory());
        if (totalNew == 0) {
            instance.getLocale().getMessage("event.sell.fail").sendPrefixedMessage(player);
            return ReturnType.SUCCESS;
        }


        for (ItemStack itemStack : player.getInventory()) {
            if (itemStack == null) continue;

            Rarity rarity = instance.getRarityManager().getRarity(itemStack);

            if (rarity == null) continue;
            player.getInventory().remove(itemStack);
        }
        instance.getEconomy().deposit(player, totalNew);

        instance.getLocale().getMessage("event.sell.success")
                .processPlaceholder("total", Methods.formatEconomy(totalNew))
                .sendPrefixedMessage(player);

        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return "ultimatefishing.sellall";
    }

    @Override
    public String getSyntax() {
        return "/uf sellall";
    }

    @Override
    public String getDescription() {
        return "Sells all fish in your inventory.";
    }
}