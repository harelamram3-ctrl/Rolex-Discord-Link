package com.example.discordlink;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class DiscordLinkPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private JDA jda;
    private final Map<String, UUID> pendingCodes = new HashMap<>();

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

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (jda != null) jda.shutdown();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (label.equalsIgnoreCase("link")) {
            if (isLinked(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "[Discord] החשבון שלך כבר מקושר לדיסקורד! רשום /profile לצפייה בפרטים.");
                return true;
            }

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
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "פרופיל שחקן");

        // 1. באנר עליון ותחתון (זכוכית סגולה ואפורה)
        ItemStack topGlass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = topGlass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            topGlass.setItemMeta(glassMeta);
        }

        ItemStack fillGlass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillMeta = fillGlass.getItemMeta();
        if (fillMeta != null) {
            fillMeta.setDisplayName(" ");
            fillGlass.setItemMeta(fillMeta);
        }

        // מילוי ה-GUI ברקע יפה
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18) {
                gui.setItem(i, topGlass);
            } else {
                gui.setItem(i, fillGlass);
            }
        }

        boolean linked = isLinked(player.getUniqueId());
        String discordId = getDiscordId(player.getUniqueId());

        Member discordMember = null;
        if (linked && jda != null && !jda.getGuilds().isEmpty() && discordId != null) {
            try {
                Guild guild = jda.getGuilds().get(0);
                discordMember = guild.retrieveMemberById(discordId).complete();
            } catch (Exception ignored) {}
        }

        // 2. ראש השחקן (מרכז)
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "👤 " + player.getName());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");
            lore.add(ChatColor.GRAY + "סטטוס קישור: " + (linked ? ChatColor.GREEN + "● מקושר ומאומת" : ChatColor.RED + "● לא מקושר"));
            if (!linked) {
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "💡 הקלד /link כדי לקשר את החשבון!");
            }
            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");
            skullMeta.setLore(lore);
            skull.setItemMeta(skullMeta);
        }
        gui.setItem(13, skull);

        // 3. ספר אודות (כולל פרטי דיסקורד)
        ItemStack aboutBook = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta aboutMeta = aboutBook.getItemMeta();
        if (aboutMeta != null) {
            aboutMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "📜 אודות השחקן");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");
            lore.add(ChatColor.GRAY + "שם במשחק: " + ChatColor.WHITE + player.getName());

            if (linked && discordMember != null) {
                lore.add(ChatColor.GRAY + "דיסקורד: " + ChatColor.AQUA + discordMember.getUser().getAsTag());
                lore.add(ChatColor.GRAY + "כינוי בשרת: " + ChatColor.YELLOW + discordMember.getEffectiveName());
            } else if (linked) {
                lore.add(ChatColor.GRAY + "דיסקורד: " + ChatColor.GREEN + "מקושר (מידע בטעינה)");
            } else {
                lore.add(ChatColor.GRAY + "דיסקורד: " + ChatColor.RED + "לא מקושר");
            }

            lore.add(ChatColor.GRAY + "אימות: " + (linked ? ChatColor.GREEN + "מאושר ✔" : ChatColor.RED + "חסר ✖"));
            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");
            aboutMeta.setLore(lore);
            aboutBook.setItemMeta(aboutMeta);
        }
        gui.setItem(11, aboutBook);

        // 4. תפקידים בדיסקורד
        ItemStack rolesItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta rolesMeta = rolesItem.getItemMeta();
        if (rolesMeta != null) {
            rolesMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "⭐ תפקידים בדיסקורד");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");

            if (linked && discordMember != null) {
                List<Role> roles = discordMember.getRoles();
                if (!roles.isEmpty()) {
                    for (Role role : roles) {
                        lore.add(ChatColor.LIGHT_PURPLE + " • " + ChatColor.WHITE + role.getName());
                    }
                } else {
                    lore.add(ChatColor.GRAY + "אין תפקידים מיוחדים בדיסקורד");
                }
            } else if (linked) {
                lore.add(ChatColor.YELLOW + "טוען תפקידים משרת הדיסקורד...");
            } else {
                lore.add(ChatColor.RED + "יש לקשר את החשבון קודם!");
            }

            lore.add(ChatColor.DARK_GRAY + "▪-----------------------▪");
            rolesMeta.setLore(lore);
            rolesItem.setItemMeta(rolesMeta);
        }
        gui.setItem(15, rolesItem);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("פרופיל שחקן")) {
            event.setCancelled(true);
        }
    }

    public void saveLinkedAccount(UUID playerUUID, String discordId) {
        getConfig().set("linked-users." + playerUUID.toString(), discordId);
        saveConfig();
    }

    public boolean isLinked(UUID playerUUID) {
        return getConfig().contains("linked-users." + playerUUID.toString());
    }

    public String getDiscordId(UUID playerUUID) {
        return getConfig().getString("linked-users." + playerUUID.toString());
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
}
