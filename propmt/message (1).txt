package my.yourname.overclock;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Overclock extends JavaPlugin implements TabExecutor {

    private NamespacedKey dataKey;
    private NamespacedKey orderKey;

    private static final Set<String> CURSE = Set.of("vanishing_curse", "binding_curse");
    private static final Set<String> BINARY_ENCHANTS = Set.of("mending", "silk_touch", "infinity", "channeling", "multishot", "aqua_affinity");

    private static final Map<String, String> VANILLA_NAME = Map.ofEntries(
            // Sword/Weapon
            Map.entry("sharpness", "Sharpness"),
            Map.entry("smite", "Smite"),
            Map.entry("bane_of_arthropods", "Bane of Arthropods"),
            Map.entry("knockback", "Knockback"),
            Map.entry("fire_aspect", "Fire Aspect"),
            Map.entry("looting", "Looting"),
            Map.entry("sweeping_edge", "Sweeping Edge"),
            // Bow
            Map.entry("power", "Power"),
            Map.entry("punch", "Punch"),
            Map.entry("flame", "Flame"),
            Map.entry("infinity", "Infinity"),
            // Crossbow
            Map.entry("multishot", "Multishot"),
            Map.entry("quick_charge", "Quick Charge"),
            Map.entry("piercing", "Piercing"),
            // Trident
            Map.entry("impaling", "Impaling"),
            Map.entry("riptide", "Riptide"),
            Map.entry("loyalty", "Loyalty"),
            Map.entry("channeling", "Channeling"),
            // Armor - Protection
            Map.entry("protection", "Protection"),
            Map.entry("fire_protection", "Fire Protection"),
            Map.entry("blast_protection", "Blast Protection"),
            Map.entry("projectile_protection", "Projectile Protection"),
            // Armor - Other
            Map.entry("thorns", "Thorns"),
            Map.entry("respiration", "Respiration"),
            Map.entry("depth_strider", "Depth Strider"),
            Map.entry("aqua_affinity", "Aqua Affinity"),
            Map.entry("feather_falling", "Feather Falling"),
            Map.entry("soul_speed", "Soul Speed"),
            Map.entry("swift_sneak", "Swift Sneak"),
            // Tools
            Map.entry("efficiency", "Efficiency"),
            Map.entry("silk_touch", "Silk Touch"),
            Map.entry("fortune", "Fortune"),
            // Fishing
            Map.entry("luck_of_the_sea", "Luck of the Sea"),
            Map.entry("lure", "Lure"),
            // Mace
            Map.entry("density", "Density"),
            Map.entry("breach", "Breach"),
            Map.entry("wind_burst", "Wind Burst"),
            // Universal
            Map.entry("unbreaking", "Unbreaking"),
            Map.entry("mending", "Mending"),
            Map.entry("vanishing_curse", "Curse of Vanishing"),
            Map.entry("binding_curse", "Curse of Binding")
    );

    @Override
    public void onEnable() {
        dataKey = new NamespacedKey(this, "real_enchants");
        orderKey = new NamespacedKey(this, "enchant_order");
        Objects.requireNonNull(getCommand("overclock")).setExecutor(this);
        Objects.requireNonNull(getCommand("overclock")).setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cกรุณาถือไอเทมในมือก่อนใช้คำสั่ง!");
            return true;
        }

        // --- /overclock 32k ---
        if (args.length == 1 && args[0].equalsIgnoreCase("32k")) {
            apply32kToItem(item);
            return true;
        }

        // --- /overclock name <name> ---
        if (args.length >= 2 && args[0].equalsIgnoreCase("name")) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            name = ChatColor.translateAlternateColorCodes('&', name);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                item.setItemMeta(meta);
                player.sendMessage("§aเปลี่ยนชื่อไอเทมเป็น: §r" + name);
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /overclock <enchant> <level> | /overclock 32k | /overclock name <ชื่อ>");
            return true;
        }

        String enchName = args[0].toLowerCase();
        int add;
        try {
            add = Integer.parseInt(args[1]);
        } catch (Exception e) {
            player.sendMessage("§cตัวเลขไม่ถูกต้อง");
            return true;
        }

        Enchantment enchant = getEnchant(enchName);
        if (enchant == null) {
            player.sendMessage("§cไม่พบ Enchant นี้");
            return true;
        }

        Map<String, Integer> real = getReal(item);
        List<String> order = getOrder(item);

        int current = real.getOrDefault(enchName, 0);
        int newLevel = current + add;

        if (newLevel <= 0) {
            real.remove(enchName);
            order.remove(enchName);
            item.removeEnchantment(enchant);
        } else {
            real.put(enchName, newLevel);
            if (!order.contains(enchName)) order.add(enchName);
            item.addUnsafeEnchantment(enchant, Math.min(newLevel, 255));
        }

        saveReal(item, real);
        saveOrder(item, order);
        updateLore(item, real, order);

        player.sendMessage("§a" + enchName + " เลเวลตอนนี้คือ: " + newLevel);
        return true;
    }

    // ========================================
    // ⚔️ ฟังก์ชันอัปเกรดไอเทมในมือเป็น 32k
    // ========================================
    private void apply32kToItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // ชื่อสีฟ้าและตัวเอียง
        meta.setDisplayName("§b§oAlpha's Stacked 32k's");

        // ลบ Attribute เก่าออกก่อน (ถ้ามี) เพื่อไม่ให้บัค
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);

        // คำนวณดาเมจพื้นฐานของไอเทม (เพื่อให้รวมแล้วได้ 16391)
        // ดาบเนเธอไรต์พื้นฐานคือ 8, ดาบเพชรคือ 7
        double baseDamage = 7.0;
        if (item.getType().name().contains("NETHERITE")) baseDamage = 8.0;
        double modifierValue = 16391.0 - baseDamage;

        AttributeModifier modifier = new AttributeModifier(
                new NamespacedKey(this, "overclock_32k_damage"),
                modifierValue,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);

        // ข้อมูล PDC สำหรับระบบ Overclock
        List<String> forcedOrder = Arrays.asList("sharpness", "knockback", "fire_aspect", "looting", "sweeping_edge", "unbreaking", "mending", "vanishing_curse");
        meta.getPersistentDataContainer().set(orderKey, PersistentDataType.STRING, String.join(";", forcedOrder));

        String realData = "sharpness:32767;knockback:10;fire_aspect:32767;looting:10;sweeping_edge:3;unbreaking:32767;mending:1;vanishing_curse:1;";
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, realData);

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // ใส่ Lore
        List<String> lore = new ArrayList<>();
        lore.add("§7Sharpness enchantment.level.32767");
        lore.add("§7Knockback X");
        lore.add("§7Fire Aspect enchantment.level.32767");
        lore.add("§7Looting X");
        lore.add("§7Sweeping Edge III");
        lore.add("§7Unbreaking enchantment.level.32767");
        lore.add("§7Mending");
        lore.add("§cCurse of Vanishing");
        meta.setLore(lore);

        item.setItemMeta(meta);

        // ใส่พลังจริง (จำกัด 255)
        item.addUnsafeEnchantment(Enchantment.SHARPNESS, 255);
        item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 10);
        item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 255);
        item.addUnsafeEnchantment(Enchantment.LOOTING, 10);
        item.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 255);
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        item.addUnsafeEnchantment(Enchantment.VANISHING_CURSE, 1);
    }

    // --- ส่วนที่เหลือ (updateLore, getReal, saveReal, etc.) เหมือนเดิมจากโค้ดล่าสุดของคุณ ---

    private void updateLore(ItemStack item, Map<String, Integer> real, List<String> order) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> newLore = new ArrayList<>();
        if (meta.hasLore()) {
            for (String line : Objects.requireNonNull(meta.getLore())) {
                String clean = ChatColor.stripColor(line);
                // ตรวจสอบว่าเป็น enchant lore หรือไม่
                boolean isEnchantLine = false;
                if (clean.contains("enchantment.level.")) {
                    isEnchantLine = true;
                } else if (clean.matches(".*\\s[IVXLCDM]+$")) {
                    // ตรวจสอบว่าเริ่มต้นด้วยชื่อ enchant หรือไม่
                    String base = clean.replaceAll("\\s[IVXLCDM]+$", "").trim();
                    if (VANILLA_NAME.containsValue(base)) isEnchantLine = true;
                } else if (VANILLA_NAME.containsValue(clean)) {
                    isEnchantLine = true;
                }
                if (!isEnchantLine) {
                    newLore.add(line);
                }
            }
        }
        for (String key : order) {
            if (!real.containsKey(key)) continue;
            int lvl = real.get(key);
            String name = VANILLA_NAME.getOrDefault(key, key);
            String color = CURSE.contains(key) ? "§c" : "§7";
            // Binary enchant (level 1 doesn't show I) หรือ curse (level 1 doesn't show I)
            boolean hideLevelOne = BINARY_ENCHANTS.contains(key) || CURSE.contains(key);
            if (lvl == 1 && hideLevelOne) {
                newLore.add(color + name);
            } else if (lvl <= 10) {
                newLore.add(color + name + " " + toRoman(lvl));
            } else {
                newLore.add(color + name + " enchantment.level." + lvl);
            }
        }
        meta.setLore(newLore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private Map<String, Integer> getReal(ItemStack item) {
        Map<String, Integer> map = new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return map;
        String raw = meta.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        if (raw != null) {
            for (String part : raw.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2) try { map.put(kv[0], Integer.parseInt(kv[1])); } catch (Exception ignored) {}
            }
        }
        return map;
    }

    private void saveReal(ItemStack item, Map<String, Integer> map) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        StringBuilder sb = new StringBuilder();
        for (var e : map.entrySet()) sb.append(e.getKey()).append(":").append(e.getValue()).append(";");
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
    }

    private List<String> getOrder(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return new ArrayList<>();
        String raw = meta.getPersistentDataContainer().get(orderKey, PersistentDataType.STRING);
        return raw == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(raw.split(";")));
    }

    private void saveOrder(ItemStack item, List<String> list) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(orderKey, PersistentDataType.STRING, String.join(";", new LinkedHashSet<>(list)));
        item.setItemMeta(meta);
    }

    private Enchantment getEnchant(String name) {
        for (Enchantment e : Enchantment.values()) {
            if (e.getKey().getKey().equalsIgnoreCase(name)) return e;
        }
        return null;
    }

    private String toRoman(int number) {
        if (number <= 0) return "";
        String[] romans = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        int[] values = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(romans[i]);
            }
        }
        return result.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            if ("32k".startsWith(args[0].toLowerCase())) list.add("32k");
            if ("name".startsWith(args[0].toLowerCase())) list.add("name");
            for (Enchantment e : Enchantment.values()) {
                String name = e.getKey().getKey();
                if (name.startsWith(args[0].toLowerCase())) list.add(name);
            }
        } else if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("name") && !args[0].equalsIgnoreCase("32k")) {
                list.addAll(Arrays.asList("1", "10", "100", "32767"));
            }
        }
        return list;
    }
}