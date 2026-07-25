package com.example.discordlink;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordLinkPlugin plugin;
    private final String ROLE_ID = "1530336293072932934";

    public DiscordListener(DiscordLinkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!link ")) {
            String code = message.substring(6).trim();

            if (plugin.getPendingCodes().containsKey(code)) {
                UUID playerUUID = plugin.getPendingCodes().remove(code);
                Player player = Bukkit.getPlayer(playerUUID);

                String mcName = (player != null) ? player.getName() : "Player";
                
                // שמירת ה-ID האישי של משתמש הדיסקורד עבור ה-GUI
                plugin.getLinkedAccounts().put(playerUUID, event.getAuthor().getId());

                // שינוי ניקניים בדיסקורד לשם המשחק
                try {
                    event.getGuild().modifyNickname(event.getMember(), mcName).queue(
                        s -> {},
                        f -> plugin.getLogger().warning("Failed to change nickname: " + f.getMessage())
                    );
                } catch (Exception ignored) {}

                // מתן הרול בדיסקורד
                try {
                    Role role = event.getGuild().getRoleById(ROLE_ID);
                    if (role != null) {
                        event.getGuild().addRoleToMember(event.getMember(), role).queue(
                            s -> {},
                            f -> plugin.getLogger().warning("Failed to add role: " + f.getMessage())
                        );
                    }
                } catch (Exception ignored) {}

                event.getChannel().sendMessage("✅ החשבון קושר בהצלחה! המשתמש **" + event.getAuthor().getAsTag() + "** קושר למיינקראפט.").queue();

                if (player != null && player.isOnline()) {
                    player.sendMessage("§a[Discord] החשבון שלך קושר בהצלחה! הקלד §f/profile §aכדי לראות את הפרופיל האישי שלך.");
                }
            } else {
                event.getChannel().sendMessage("❌ קוד אימות לא תקין או שפג תוקפו.").queue();
            }
        }
    }
}
