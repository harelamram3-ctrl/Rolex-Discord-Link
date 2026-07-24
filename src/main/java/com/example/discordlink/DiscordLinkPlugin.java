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
import org.bukkit.inventory.meta.ItemMeta;
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

        // הגנה על תפריט ה-GUI שלא יוכלו לקחת פריטים
        Bukkit.getPluginManager().registerEvents(new GuiListener(), this);
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

        // 1. באנר עליון (זכוכית סגולה)
        ItemStack banner = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta bannerMeta = banner.getItemMeta();
        if (bannerMeta != null) {
            bannerMeta.setDisplayName(" ");
            banner.setItemMeta(bannerMeta);
        }
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, banner);
        }

        // 2. תמונת פרופיל (הראש של השחקן באמצע)
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + player.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Discord Tag: " + ChatColor.WHITE + (linkedAccounts.getOrDefault(player.getUniqueId(), "לא מקושר")));
            lore.add("");
            lore.add(ChatColor.GRAY + "סטטוס: " + (linkedAccounts.containsKey(player.getUniqueId()) ? ChatColor.GREEN + "● מאומת ומקושר" : ChatColor.RED + "● לא מאומת"));
            skullMeta.setLore(lore);
            skull.setItemMeta(skullMeta);
        }
        gui.setItem(13, skull);

        // 3. תיאור / אודות (About Me)
        ItemStack aboutBook = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta aboutMeta = aboutBook.getItemMeta();
        if (aboutMeta != null) {
            aboutMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "📝 אודות (About Me)");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "-------------------");
            lore.add(ChatColor.YELLOW + "Owner in: " + ChatColor.WHITE + "RolexNetWork ⚡ Offical");
            lore.add(ChatColor.AQUA + "Link: " + ChatColor.UNDERLINE + "discord.gg/BTfmJQhnrC");
            lore.add(ChatColor.GRAY + "RolexNetWork - ComeBack");
            lore.add(ChatColor.DARK_GRAY + "-------------------");
            aboutMeta.setLore(lore);
            aboutBook.setItemMeta(aboutMeta);
        }
        gui.setItem(11, aboutBook);

        // 4. תפקידים ותגים בדיסקורד (Roles)
        ItemStack rolesItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta rolesMeta = rolesItem.getItemMeta();
        if (rolesMeta != null) {
            rolesMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🏷️ תפקידים בדיסקורד");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "-------------------");
            lore.add(ChatColor.GOLD + "• Owner");
            lore.add(ChatColor.LIGHT_PURPLE + "• RolexNetWork Booster");
            lore.add(ChatColor.GREEN + "• MC Real player");
            lore.add(ChatColor.DARK_GRAY + "-------------------");
            rolesMeta.setLore(lore);
            rolesItem.setItemMeta(rolesMeta);
        }
        gui.setItem(15, rolesItem);

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
