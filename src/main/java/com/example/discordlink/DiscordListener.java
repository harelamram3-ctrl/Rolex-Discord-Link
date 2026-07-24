package com.example.discordlink;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordLinkPlugin plugin;
    private final String ROLE_ID = "1530336293072932934"; // ה-ID של הרול שלך

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

                String mcName = (player != null) ? player.getName() : "שחקן";
                String discordTag = event.getAuthor().getAsTag();

                // שמירת המידע לפרופיל בשרת
                plugin.getLinkedAccounts().put(playerUUID, discordTag);

                // שינוי הניקניים בדיסקורד
                event.getGuild().modifyNickname(event.getMember(), mcName).queue();

                // מתן הרול בדיסקורד
                Role role = event.getGuild().getRoleById(ROLE_ID);
                if (role != null) {
                    event.getGuild().addRoleToMember(event.getMember(), role).queue();
                }

                event.getChannel().sendMessage("✅ החשבון קושר בהצלחה! קיבלת את הרול בדיסקורד והשם שלך שונה ל-**" + mcName + "**.").queue();

                if (player != null && player.isOnline()) {
                    player.sendMessage("§a[Discord] החשבון שלך קושר בהצלחה ל-§e" + discordTag + "§a! רשום §f/profile §aכדי לראות את הפרופיל.");
                }

            } else {
                event.getChannel().sendMessage("❌ קוד אימות לא תקין או שפג תוקפו.").queue();
            }
        }
    }
}
