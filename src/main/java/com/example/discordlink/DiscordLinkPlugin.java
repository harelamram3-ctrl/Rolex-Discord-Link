package com.example.discordlink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DiscordLinkPlugin extends JavaPlugin implements CommandExecutor {

    private JDA jda;
    private final Map<String, UUID> pendingCodes = new HashMap<>();
    private final Map<UUID, String> linkedAccounts = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String token = getConfig().getString("bot-token");
        if (token != null && !token.equals("PUT_YOUR_TOKEN_HERE")) {
            try {
                jda = JDABuilder.createDefault(token)
                        .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                        .addEventListeners(new DiscordListener(this))
                        .build();
            } catch (Exception e) {
                getLogger().severe("שגיאה בהתחברות לבוט: " + e.getMessage());
            }
        }

        if (getCommand("link") != null) getCommand("link").setExecutor(this);
        if (getCommand("profile") != null) getCommand("profile").setExecutor(this);
    }

    @Override
    public void onDisable() {
        if (jda != null) jda.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (label.equalsIgnoreCase("link")) {
            String code = generateCode();
            pendingCodes.put(code, player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "קוד הקישור שלך: " + ChatColor.YELLOW + ChatColor.BOLD + code);
            player.sendMessage(ChatColor.GRAY + "רשום בערוץ בדיסקורד: !link " + code);
            return true;
        }

        if (label.equalsIgnoreCase("profile")) {
            openProfileGUI(player);
            return true;
        }

        return true;
    }

    private void openProfileGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "פרופיל שחקן");

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "סטטוס אימות: " + (linkedAccounts.containsKey(player.getUniqueId()) ? ChatColor.GREEN + "מאומת ✔" : ChatColor.RED + "לא מאומת ✖"));
            
            if (linkedAccounts.containsKey(player.getUniqueId())) {
                lore.add(ChatColor.GRAY + "חשבון דיסקורד: " + ChatColor.AQUA + linkedAccounts.get(player.getUniqueId()));
            } else {
                lore.add(ChatColor.YELLOW + "הקלד /link כדי לקשר את החשבון!");
            }

            meta.setLore(lore);
            skull.setItemMeta(meta);
        }

        gui.setItem(13, skull);
        player.openInventory(gui);
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

    public Map<String, UUID> getPendingCodes() { return pendingCodes; }
    public Map<UUID, String> getLinkedAccounts() { return linkedAccounts; }
}
