package com.notnawfas.invitesrewards;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter {

    private final InvitesRewards plugin;
    private JDA jda;

    private final Map<String, Integer> inviteCache = new ConcurrentHashMap<>();

    public DiscordBot(InvitesRewards plugin) {
        this.plugin = plugin;
    }

    public void start(String token) {
        if (token == null || token.isEmpty()) {
            plugin.getLogger().warning("Discord Bot Token is empty! JDA will not start.");
            return;
        }

        if (token.length() < 10) {
            plugin.getLogger().warning("Discord Bot Token appears invalid (too short). JDA will not start.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_INVITES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            plugin.getLogger().info("Starting Discord Bot...");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Discord Bot: " + e.getMessage());
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(5, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                    jda.awaitShutdown(3, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        event.getGuild().retrieveInvites().queue(invites -> {
            for (Invite invite : invites) {
                inviteCache.put(invite.getCode(), invite.getUses());
            }
        });
    }

    @Override
    public void onGuildInviteCreate(@NotNull GuildInviteCreateEvent event) {
        inviteCache.put(event.getInvite().getCode(), event.getInvite().getUses());
    }

    @Override
    public void onGuildInviteDelete(@NotNull GuildInviteDeleteEvent event) {
        inviteCache.remove(event.getCode());
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        String joinedUserId = joinedUser.getId();

        if (plugin.getRewardCodeManager().hasUserBeenCounted(joinedUserId)) {
            plugin.getLogger().info("User " + joinedUser.getName() + " (" + joinedUserId + ") has already been counted as an invite. Skipping.");
            return;
        }

        int minAgeDays = plugin.getConfigManager().getConfig().getInt("min-account-age-days", 7);
        long accountAge = ChronoUnit.DAYS.between(joinedUser.getTimeCreated(), OffsetDateTime.now());

        guild.retrieveInvites().queue(invites -> {
            List<Invite> usedInvites = new ArrayList<>();

            for (Invite invite : invites) {
                String code = invite.getCode();
                int currentUses = invite.getUses();
                int cachedUses = inviteCache.getOrDefault(code, 0);

                if (currentUses > cachedUses) {
                    usedInvites.add(invite);
                }
                inviteCache.put(code, currentUses);
            }

            for (Invite usedInvite : usedInvites) {
                if (usedInvite.getInviter() == null) {
                    continue;
                }

                User inviter = usedInvite.getInviter();

                if (accountAge < minAgeDays) {
                    String message = plugin.getConfigManager().getMessages().getString("messages.discord.fake-invite", "The user you invited does not meet the minimum account age requirement. Reward cancelled.");
                    sendDM(inviter, message);
                } else {
                    plugin.getRewardCodeManager().markUserCounted(joinedUserId);

                    String rewardCode = plugin.getRewardCodeManager().generateCode();
                    String messageTemplate = plugin.getConfigManager().getMessages().getString("messages.discord.success-invite", "Thanks for inviting %user%! Your reward code: %code%. Use /invitesreward claim %code% in-game!");

                    String message = messageTemplate
                            .replace("%user%", joinedUser.getName())
                            .replace("%code%", rewardCode);

                    sendDM(inviter, message);
                }
            }
        });
    }

    private void sendDM(User user, String message) {
        user.openPrivateChannel().queue(
                channel -> channel.sendMessage(message).queue(
                        success -> {},
                        error -> plugin.getLogger().warning("Failed to send DM to " + user.getName() + ": " + error.getMessage())
                ),
                error -> plugin.getLogger().warning("Failed to open private channel with " + user.getName() + ": " + error.getMessage())
        );
    }
}
