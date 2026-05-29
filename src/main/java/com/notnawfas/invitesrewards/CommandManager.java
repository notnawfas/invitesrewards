package com.notnawfas.invitesrewards;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

public class CommandManager implements CommandExecutor {

    private final InvitesRewards plugin;
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    public CommandManager(InvitesRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "generate":
                handleGenerate(sender);
                break;
            case "claim":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /invitesreward claim <code>");
                } else {
                    handleClaim(sender, args[1]);
                }
                break;
            case "reload":
                handleReload(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleGenerate(CommandSender sender) {
        if (!sender.hasPermission("invitesreward.admin")) {
            sender.sendMessage(colorize(plugin.getConfigManager().getMessages().getString("messages.no-permission", "&cYou do not have permission to use this command.")));
            return;
        }

        String code = plugin.getRewardCodeManager().generateCode();
        String messageTemplate = plugin.getConfigManager().getMessages().getString("messages.code-generated", "&a[InvitesRewards] &fSuccessfully Generated A Reward Code!\n&7To claim, run: &e/invitesreward claim %code%");

        String[] lines = messageTemplate.replace("%code%", code).split("\\n");
        for (String line : lines) {
            sender.sendMessage(colorize(line));
        }
    }

    private void handleClaim(CommandSender sender, String code) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can claim rewards.");
            return;
        }

        Player player = (Player) sender;

        if (!VALID_PLAYER_NAME.matcher(player.getName()).matches()) {
            player.sendMessage(ChatColor.RED + "Your username contains invalid characters. Please contact an administrator.");
            return;
        }

        if (!plugin.getRewardCodeManager().claimCode(code)) {
            player.sendMessage(colorize(plugin.getConfigManager().getMessages().getString("messages.invalid-code", "&c[InvitesRewards] Invalid or already claimed code.")));
            return;
        }

        ConfigurationSection groupsSection = plugin.getConfigManager().getRewards().getConfigurationSection("groups");
        if (groupsSection != null) {
            boolean rewardGiven = false;
            for (String groupKey : groupsSection.getKeys(false)) {
                ConfigurationSection group = groupsSection.getConfigurationSection(groupKey);
                if (group == null) continue;

                String perm = group.getString("permission");
                if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {
                    List<String> commands = group.getStringList("commands");
                    for (String cmd : commands) {
                        String finalCmd = cmd.replace("%player_name%", player.getName());
                        try {
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCmd);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to execute reward command '" + finalCmd + "': " + e.getMessage());
                        }
                    }
                    rewardGiven = true;
                    break;
                }
            }

            if (rewardGiven) {
                player.sendMessage(colorize(plugin.getConfigManager().getMessages().getString("messages.code-claimed", "&a[InvitesRewards] &fYou have successfully claimed your reward!")));
            } else {
                player.sendMessage(ChatColor.RED + "You claimed the code, but no reward was configured for your rank.");
            }
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("invitesreward.admin")) {
            sender.sendMessage(colorize(plugin.getConfigManager().getMessages().getString("messages.no-permission", "&cYou do not have permission to use this command.")));
            return;
        }

        plugin.getConfigManager().reloadConfigs();

        String newToken = plugin.getConfigManager().getConfig().getString("bot-token");
        if (newToken == null || newToken.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Bot token is empty in config! Discord bot will not restart.");
            plugin.getDiscordBot().stop();
            return;
        }

        plugin.getDiscordBot().stop();
        plugin.getDiscordBot().start(newToken);

        sender.sendMessage(colorize(plugin.getConfigManager().getMessages().getString("messages.reloaded", "&a[InvitesRewards] Configuration files reloaded successfully.")));
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = plugin.getConfigManager().getMessages().getStringList("messages.help");
        if (helpMessages.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "--- InvitesRewards Help ---");
            sender.sendMessage(ChatColor.WHITE + "/invitesreward generate" + ChatColor.GRAY + " - Generates a new reward code.");
            sender.sendMessage(ChatColor.WHITE + "/invitesreward claim <code>" + ChatColor.GRAY + " - Claims a reward code.");
            sender.sendMessage(ChatColor.WHITE + "/invitesreward reload" + ChatColor.GRAY + " - Reloads the plugin configuration.");
            sender.sendMessage(ChatColor.WHITE + "/invitesreward help" + ChatColor.GRAY + " - Shows this help message.");
        } else {
            for (String msg : helpMessages) {
                sender.sendMessage(colorize(msg));
            }
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
