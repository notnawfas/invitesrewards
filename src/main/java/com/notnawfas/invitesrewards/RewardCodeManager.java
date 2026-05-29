package com.notnawfas.invitesrewards;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class RewardCodeManager {

    private final InvitesRewards plugin;
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private final Object dataLock = new Object();

    public RewardCodeManager(InvitesRewards plugin) {
        this.plugin = plugin;
    }

    public String generateCode() {
        synchronized (dataLock) {
            String code;
            List<String> codes;
            do {
                StringBuilder sb = new StringBuilder(CODE_LENGTH);
                for (int i = 0; i < CODE_LENGTH; i++) {
                    sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
                }
                code = sb.toString();
                codes = plugin.getConfigManager().getData().getStringList("active-codes");
            } while (codes.contains(code));

            codes.add(code);
            plugin.getConfigManager().getData().set("active-codes", codes);
            plugin.getConfigManager().saveDataSync();

            return code;
        }
    }

    public boolean claimCode(String code) {
        synchronized (dataLock) {
            List<String> codes = plugin.getConfigManager().getData().getStringList("active-codes");
            if (codes.contains(code)) {
                codes.remove(code);
                plugin.getConfigManager().getData().set("active-codes", codes);
                plugin.getConfigManager().saveDataSync();
                return true;
            }
            return false;
        }
    }

    public boolean hasUserBeenCounted(String discordUserId) {
        synchronized (dataLock) {
            List<String> countedUsers = plugin.getConfigManager().getData().getStringList("counted-users");
            return countedUsers.contains(discordUserId);
        }
    }

    public void markUserCounted(String discordUserId) {
        synchronized (dataLock) {
            List<String> countedUsers = plugin.getConfigManager().getData().getStringList("counted-users");
            if (!countedUsers.contains(discordUserId)) {
                countedUsers.add(discordUserId);
                plugin.getConfigManager().getData().set("counted-users", countedUsers);
                plugin.getConfigManager().saveDataSync();
            }
        }
    }
}
