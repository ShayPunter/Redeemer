package co.uk.shaypunter.minecraft.redeemer.commands;

import co.uk.shaypunter.minecraft.redeemer.Redeemer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RedeemCommand implements CommandExecutor {

    private Redeemer redeemer = Redeemer.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1) {
            if (redeemer.getRedeemables().contains(args[0])) {
                if (!redeemer.hasRedeemablePermission((Player)sender, args[0])) {
                    sendPlayerMessage(sender, "noPermissionForRedeemable");
                    return true;
                }

                int redeemableTimes = redeemer.getConfig().getInt("redeemables." + args[0] + ".timesCanRedeem");
                String[] deserialisedRedeemedData = redeemer.getRedeemedUser(args[0], ((Player) sender).getUniqueId()).split(":");

                if (Integer.parseInt(deserialisedRedeemedData[1]) >= redeemableTimes) {
                    sendPlayerMessage(sender, "cannotRedeem");
                    return true;
                }

                for (String commandToRun : Redeemer.getInstance().getRedeemableCommands(args[0])) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun.replaceAll("%player%", sender.getName()));
                }

                redeemer.saveRedeemedUserToConfig(((Player) sender).getUniqueId(), args[0], Integer.parseInt(deserialisedRedeemedData[1]) + 1);
                sendPlayerMessage(sender, redeemer.getConfig("messages").getString("redeemed").replaceAll("%redeemable%", args[0]).replaceAll("%redeemableTimes%", String.valueOf((redeemableTimes - Integer.valueOf(deserialisedRedeemedData[1]) - 1))));
                return true;
            }

            if (!sender.hasPermission("redeemer.reload")) {
                sendPlayerMessage(sender, "noPermission");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                redeemer.reloadConfig();
                redeemer.getConfig("messages");
                redeemer.getConfig("data");
                sendPlayerMessage(sender, "&8[&aRedeemer&8] &aSuccessfully reloaded configs!");
                return true;
            }
        }

        List<String> redeemables = redeemer.getRedeemables();
        List<String> remove = new ArrayList<>();
        redeemables.forEach((redeemable) -> {
            if (Integer.parseInt(redeemer.getRedeemedUser(redeemable, ((Player)sender).getUniqueId()).split(":")[1]) >= (Redeemer.getInstance().getRedeemableTimes(redeemable) - 1))
                remove.add(redeemable);

            if (!redeemer.hasRedeemablePermission((Player) sender, redeemable))
                remove.add(redeemable);
        });
        redeemables.removeAll(remove);
        remove.clear();

        StringBuilder stringBuilder = new StringBuilder();
        redeemables.forEach((redeemable) -> {
            stringBuilder.append(redeemable).append(",");
        });

        if (stringBuilder.length() == 0) {
            sendPlayerMessage(sender, "noRedeemables");
            return true;
        }

        stringBuilder.deleteCharAt((stringBuilder.length() - 1));
        sendPlayerMessage(sender, redeemer.getConfig("messages").getString("redeemables").replaceAll("%redeemables%", stringBuilder.toString()));
        return true;
    }

    private void sendPlayerMessage(CommandSender sender, String message) {
        String messageToSend = (redeemer.getConfig("messages").getString(message) == null) ? message : redeemer.getConfig("messages").getString(message);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messageToSend));
    }
}
