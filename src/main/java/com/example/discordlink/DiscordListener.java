package com.example.discordlink;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final DiscordLinkPlugin plugin;

    public DiscordListener(DiscordLinkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // התעלמות מהודעות של בוטים
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();

        // בדיקה אם ההודעה מתחילה בפקודה !link
        if (message.startsWith("!link ")) {
            String code = message.substring(6).trim();

            // בדיקה אם הקוד קיים במערכת
            if (plugin.getPendingCodes().containsKey(code)) {
                UUID playerUUID = plugin.getPendingCodes().remove(code);
                String discordUser = event.getAuthor().getAsTag();

                event.getChannel().sendMessage("✅ החשבון קושר בהצלחה! משתמש הדיסקורד " + discordUser + " קושר למיינקראפט.").queue();
                plugin.getLogger().info("השחקן בעל ה-UUID: " + playerUUID + " קושר למשתמש: " + discordUser);
            } else {
                event.getChannel().sendMessage("❌ קוד אימות לא תקין או שפג תוקפו.").queue();
            }
        }
    }
}
