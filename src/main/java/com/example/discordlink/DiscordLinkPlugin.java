package com.example.discordlink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DiscordLinkPlugin extends JavaPlugin implements CommandExecutor {

    private JDA jda;
    private final Map<String, UUID> pendingCodes = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        String token = getConfig().getString("bot-token");
        if (token == null || token.equals("PUT_YOUR_TOKEN_HERE")) {
            getLogger().severe("יש להגדיר Token תקין בקובץ config.yml!");
        } else {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                        .build();
                getLogger().info("הבוט של דיסקורד מתחבר...");
            } catch (Exception e) {
                getLogger().severe("שגיאה בהתחברות לבוט: " + e.getMessage());
            }
        }

        if (getCommand("link") != null) {
            getCommand("link").setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("פקודה זו מיועדת לשחקנים בלבד!");
            return true;
        }

        String code = generateCode();
        pendingCodes.put(code, player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "קוד הקישור שלך הוא: " + ChatColor.YELLOW + ChatColor.BOLD + code);
        player.sendMessage(ChatColor.GRAY + "שלח את הקוד לבוט בדיסקורד!");

        return true;
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
