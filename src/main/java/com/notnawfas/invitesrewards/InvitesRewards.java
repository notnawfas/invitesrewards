package com.notnawfas.invitesrewards;

import org.bukkit.plugin.java.JavaPlugin;

public class InvitesRewards extends JavaPlugin {

    private ConfigManager configManager;
    private RewardCodeManager rewardCodeManager;
    private DiscordBot discordBot;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        getLogger().info("InvitesRewards is starting...");

        // Initialize Managers
        configManager = new ConfigManager(this);
        configManager.setup();

        rewardCodeManager = new RewardCodeManager(this);
        discordBot = new DiscordBot(this);

        // Register Commands
        commandManager = new CommandManager(this);
        if (getCommand("invitesreward") != null) {
            getCommand("invitesreward").setExecutor(commandManager);
        } else {
            getLogger().severe("Command 'invitesreward' not found in plugin.yml!");
        }

        // Start Discord Bot Async
        String token = configManager.getConfig().getString("bot-token");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            discordBot.start(token);
        });

        getLogger().info("InvitesRewards has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("InvitesRewards is shutting down...");

        if (discordBot != null) {
            discordBot.stop();
        }

        getLogger().info("InvitesRewards has been successfully disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RewardCodeManager getRewardCodeManager() {
        return rewardCodeManager;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
