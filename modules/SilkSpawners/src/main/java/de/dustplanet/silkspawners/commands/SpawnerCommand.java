package de.dustplanet.silkspawners.commands;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import de.dustplanet.silkspawners.SilkSpawners;
import de.dustplanet.silkspawners.events.SilkSpawnersSpawnerChangeEvent;
import de.dustplanet.util.SilkUtil;

/**
 * Handles the commands.
 *
 * @author xGhOsTkiLLeRx
 */

public class SpawnerCommand implements CommandExecutor {
    private SilkUtil su;
    private SilkSpawners plugin;

    public SpawnerCommand(SilkSpawners instance, SilkUtil util) {
        su = util;
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        switch (args.length) {
            case 1:
                switch (args[0].toLowerCase(Locale.ENGLISH)) {
                    case "help":
                        handleHelp(sender);
                        break;
                    case "all":
                    case "list":
                        handleList(sender);
                        break;
                    case "reload":
                    case "rl":
                        handleReload(sender);
                        break;
                    case "info":
                    case "view":
                        handleView(sender);
                        break;
                    default:
                        handleUnknownArgument(sender);
                        break;
                }
                break;
            case 2:
                switch (args[0].toLowerCase(Locale.ENGLISH)) {
                    case "change":
                    case "set":
                        handleChange(sender, args[1]);
                        break;
                    case "selfget":
                    case "i":
                        handleGive(sender, sender.getName(), args[1], null);
                        break;
                    default:
                        handleUnknownArgument(sender);
                        break;
                }
                break;
            case 3:
                switch (args[0].toLowerCase(Locale.ENGLISH)) {
                    case "give":
                    case "add":
                        handleGive(sender, args[1], args[2].toLowerCase(Locale.ENGLISH), null);
                        break;
                    case "selfget":
                    case "i":
                        handleGive(sender, sender.getName(), args[1], args[2]);
                        break;
                    default:
                        handleUnknownArgument(sender);
                        break;
                }
                break;
            case 4:
                switch (args[0].toLowerCase(Locale.ENGLISH)) {
                    case "give":
                    case "add":
                        handleGive(sender, args[1], args[2].toLowerCase(Locale.ENGLISH), args[3]);
                        break;
                    default:
                        handleUnknownArgument(sender);
                        break;
                }
                break;
            default:
                handleUnknownArgument(sender);
                break;
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void handleGive(CommandSender sender, String receiver, String mob, String amountString) {
        int amount = plugin.config.getInt("defaultAmountGive", 1);
        boolean saveData = false;

        // Check given amount
        if (amountString != null && !amountString.isEmpty()) {
            amount = su.getNumber(amountString);
            if (amount == -1) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("useNumbers")));
                return;
            }
        }

        // Check player
        Player player = su.nmsProvider.getPlayer(receiver);
        // Online check
        if (player == null) {
            player = this.su.nmsProvider.loadPlayer(Bukkit.getOfflinePlayer(receiver));
            if (player == null) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("playerOffline")));
                return;
            }
            saveData = true;
        }

        // Check if it's an egg or not
        boolean isEgg = su.isEgg(mob);
        String egg = mob;
        if (isEgg) {
            egg = egg.replaceFirst("egg$", "");
        }

        if (isEgg) {
            handleGiveEgg(sender, player, egg, amount, saveData);
        } else {
            handleGiveSpawner(sender, player, mob, amount, saveData);
        }
    }

    private void handleGiveEgg(CommandSender sender, Player receiver, String mob, int amount, boolean saveData) {
        if (su.isUnknown(mob)) {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("unknownCreature"))
                    .replace("%creature%", mob));
            return;
        }
        String entityID = su.getDisplayNameToMobID().get(mob);
        String creature = su.getCreatureName(entityID);

        String mobName = creature.toLowerCase(Locale.ENGLISH).replace(" ", "");

        // Add egg
        if (sender.hasPermission("silkspawners.freeitemegg." + mobName)) {
            // Have space in inventory
            if (receiver.getInventory().firstEmpty() == -1) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noFreeSlot")));
                return;
            }
            receiver.getInventory().addItem(su.newEggItem(entityID, amount, su.getCreatureEggName(entityID)));
            if (saveData) {
                receiver.saveData();
            }
            if (sender instanceof Player) {
                Player pSender = (Player) sender;
                if (pSender.getUniqueId() == receiver.getUniqueId()) {
                    su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("addedEgg"))
                            .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                } else {
                    su.sendMessage(sender,
                            ChatColor
                                    .translateAlternateColorCodes('\u0026',
                                            plugin.localization.getString("addedEggOtherPlayer").replace("%player%", receiver.getName()))
                                    .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                }
            } else {
                su.sendMessage(sender,
                        ChatColor
                                .translateAlternateColorCodes('\u0026',
                                        plugin.localization.getString("addedEggOtherPlayer").replace("%player%", receiver.getName()))
                                .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
            }
            return;
        }
        su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionFreeEgg")));

    }

    private void handleGiveSpawner(CommandSender sender, Player receiver, String mob, int amount, boolean saveData) {
        if (su.isUnknown(mob)) {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("unknownCreature"))
                    .replace("%creature%", mob));
            return;
        }

        String entityID = su.getDisplayNameToMobID().get(mob);
        String creature = su.getCreatureName(entityID);
        // Filter spaces (like Zombie Pigman)
        String mobName = creature.toLowerCase(Locale.ENGLISH).replace(" ", "");

        // Add spawner
        if (sender.hasPermission("silkspawners.freeitem." + mobName)) {
            // Have space in inventory
            if (receiver.getInventory().firstEmpty() == -1) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noFreeSlot")));
                return;
            }
            receiver.getInventory().addItem(su.newSpawnerItem(entityID, su.getCustomSpawnerName(entityID), amount, false));
            if (saveData) {
                receiver.saveData();
            }
            if (sender instanceof Player) {
                Player pSender = (Player) sender;
                if (pSender.getUniqueId() == receiver.getUniqueId()) {
                    su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("addedSpawner"))
                            .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                } else {
                    su.sendMessage(sender,
                            ChatColor
                                    .translateAlternateColorCodes('\u0026',
                                            plugin.localization.getString("addedSpawnerOtherPlayer").replace("%player%",
                                                    receiver.getName()))
                                    .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
                }
            } else {
                su.sendMessage(sender,
                        ChatColor
                                .translateAlternateColorCodes('\u0026',
                                        plugin.localization.getString("addedSpawnerOtherPlayer").replace("%player%", receiver.getName()))
                                .replace("%creature%", creature).replace("%amount%", Integer.toString(amount)));
            }
            return;
        }
        su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionFreeSpawner")));

    }

    private void handleChange(CommandSender sender, String newMob) {
        if (sender instanceof Player) {
            if (su.isUnknown(newMob)) {
                su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("unknownCreature"))
                        .replace("%creature%", newMob));
                return;
            }

            String entityID = su.getDisplayNameToMobID().get(newMob);
            String creature = su.getCreatureName(entityID);
            // Filter spaces (like Zombie Pigman)
            String mobName = creature.toLowerCase(Locale.ENGLISH).replace(" ", "");

            Player player = (Player) sender;

            int distance = plugin.config.getInt("spawnerCommandReachDistance", 6);
            // If the distance is -1, return
            if (distance != -1) {
                // Get the block
                Block block = su.nmsProvider.getSpawnerFacing(player, distance);
                if (block != null) {
                    handleBlockChange(player, block, entityID, mobName);
                    return;
                }
            }

            ItemStack itemInHand = su.nmsProvider.getSpawnerItemInHand(player);
            Material itemMaterial;
            try {
                itemMaterial = itemInHand.getType();
            } catch (@SuppressWarnings("unused") NullPointerException e) {
                itemMaterial = null;
            }

            if (itemMaterial != null && itemMaterial == su.nmsProvider.getSpawnerMaterial()) {
                handleChangeSpawner(player, entityID, mobName, itemInHand);
            } else if (itemMaterial != null && su.nmsProvider.getSpawnEggMaterials().contains(itemMaterial)) {
                handleChangeEgg(player, entityID, mobName, itemInHand);
            } else {
                su.sendMessage(player,
                        ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("spawnerNotDeterminable")));
            }
        } else {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noConsole")));
        }

    }

    private void handleBlockChange(Player player, Block block, String entityID, String mobName) {
        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            su.sendMessage(player,
                    ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionChangingSpawner")));
            return;
        }
        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, block, entityID,
                su.getSpawnerEntityID(block), 1);
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }
        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        if (su.setSpawnerType(block, newEntityID, player,
                ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("changingDeniedWorldGuard")))) {
            su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("changedSpawner"))
                    .replace("%creature%", newMob));
        }

    }

    private void handleChangeSpawner(Player player, String entityID, String mobName, ItemStack itemInHand) {
        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            su.sendMessage(player,
                    ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionChangingSpawner")));
            return;
        }

        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, null, entityID,
                su.getStoredSpawnerItemEntityID(itemInHand), itemInHand.getAmount());
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }

        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        ItemStack newItem = su.setSpawnerType(itemInHand, newEntityID, plugin.localization.getString("spawnerName"));
        su.nmsProvider.setSpawnerItemInHand(player, newItem);
        su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("changedSpawner"))
                .replace("%creature%", newMob));

    }

    private void handleChangeEgg(Player player, String entityID, String mobName, ItemStack itemInHand) {
        if (!player.hasPermission("silkspawners.changetype." + mobName)) {
            su.sendMessage(player,
                    ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionChangingEgg")));
            return;
        }

        // Call the event and maybe change things!
        SilkSpawnersSpawnerChangeEvent changeEvent = new SilkSpawnersSpawnerChangeEvent(player, null, entityID,
                su.getStoredSpawnerItemEntityID(itemInHand), itemInHand.getAmount());
        plugin.getServer().getPluginManager().callEvent(changeEvent);
        // See if we need to stop
        if (changeEvent.isCancelled()) {
            return;
        }

        // Get the new ID (might be changed)
        String newEntityID = changeEvent.getEntityID();
        String newMob = su.getCreatureName(entityID);
        ItemStack newItem = su.setSpawnerType(itemInHand, newEntityID, plugin.localization.getString("spawnerName"));
        su.nmsProvider.setSpawnerItemInHand(player, newItem);
        su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("changedEgg"))
                .replace("%creature%", newMob));

    }

    private void handleUnknownArgument(CommandSender sender) {
        su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("unknownArgument")));
    }

    private void handleHelp(CommandSender sender) {
        if (sender.hasPermission("silkspawners.help")) {
            String message = ChatColor.translateAlternateColorCodes('\u0026',
                    plugin.localization.getString("help").replace("%version%", plugin.getDescription().getVersion()));
            su.sendMessage(sender, message);
        } else {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermission")));
        }
    }

    private void handleReload(CommandSender sender) {
        if (sender.hasPermission("silkspawners.reload")) {
            plugin.reloadConfigs();
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("configsReloaded")));
        } else {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermission")));
        }
    }

    private void handleList(CommandSender sender) {
        su.showAllCreatures(sender);
    }

    private void handleView(CommandSender sender) {
        if (sender instanceof Player) {
            // If the distance is -1, return
            int distance = plugin.config.getInt("spawnerCommandReachDistance", 6);
            if (distance == -1) {
                return;
            }
            // Get the block, returns null for non spawner blocks
            Player player = (Player) sender;
            Block block = su.nmsProvider.getSpawnerFacing(player, distance);
            if (block == null) {
                su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("lookAtSpawner")));
                return;
            }
            String entityID = su.getSpawnerEntityID(block);
            if (player.hasPermission("silkspawners.viewtype")) {
                su.sendMessage(player, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("getSpawnerType"))
                        .replace("%creature%", su.getCreatureName(entityID)));
            } else {
                su.sendMessage(player,
                        ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noPermissionViewType")));
            }
        } else {
            su.sendMessage(sender, ChatColor.translateAlternateColorCodes('\u0026', plugin.localization.getString("noConsole")));
        }
    }
}
