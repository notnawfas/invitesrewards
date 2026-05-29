package com.notnawfas.invitesrewards;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final InvitesRewards plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration rewards;
    private FileConfiguration data;

    private File configFile;
    private File messagesFile;
    private File rewardsFile;
    private File dataFile;

    public ConfigManager(InvitesRewards plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        dataFile = new File(plugin.getDataFolder(), "data.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
            }
        }

        reloadConfigs();
    }

    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        rewards = YamlConfiguration.loadConfiguration(rewardsFile);
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getRewards() {
        return rewards;
    }

    public FileConfiguration getData() {
        return data;
    }

    public void saveDataSync() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
        }
    }

    @Deprecated
    public void saveDataAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data.yml!");
            }
        });
    }
}
