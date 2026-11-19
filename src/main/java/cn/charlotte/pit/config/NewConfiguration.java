package cn.charlotte.pit.config;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.util.time.TimeUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NewConfiguration {

    private static final Map<String, Integer> eventOnlineRequired = new HashMap<>();
    private static final Map<String, MythicMobsConf> mythicMobs = new HashMap<>();
    private static final Map<MythicColor, List<Rate>> rareRate = new HashMap<>();
    private static final List<Map.Entry<String, Double>> mythicChance = new ArrayList<>();
    private static final HashMap<String, Double> festivalBoosts = new HashMap<>();
    private static int vipPrice = 500;
    private static String priceName = "点券";
    private static String dateFormat = "MM/dd HH:mm";
    private static boolean noobProtect = true;
    private static int noobProtectLevel = 120;
    private static double noobDamageBoost = 1.1;
    private static double noobDamageReduce = 0.9;
    private static double luckGem = 0.30;
    private static boolean customChatFormatEnable = false;
    private static String customChatFormat = "%pit_level_tag_roman% %s&f: %s";
    private static boolean scoreboardShowtime = true;
    private static String pitSupportPermission = "pit.vip";
    private static final Map<String, Object> defaults = new HashMap<>() {{
        put("water-mark", "&k~~~~~");
        put("bot-name", "&cBot");
        put("vip-price", 500);
        put("price-name", "点券");
        put("lobby-command", "hub");

        put("luck-gem", 0.30);

        put("dateFormat", "MM/dd HH:mm");

        put("pitSupportPermission", pitSupportPermission);
        put("removeSupportWhenNoPermission", false);

        put("GenesisAngel-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTYxMTcxMzEzMTMyOCwKICAicHJvZmlsZUlkIiA6ICJiMGQ3MzJmZTAwZjc0MDdlOWU3Zjc0NjMwMWNkOThjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJPUHBscyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xOTA3MjdjNjNkMmQ3MjUwZTQ1NTA4NTBiMmQ0YTdlMTEwZDFkMzliNjhmYjcwMmRkYjkzYmIwYjJlZjg0ZCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
        put("GenesisDemon-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTY1NzU5OTQzODI2MCwKICAicHJvZmlsZUlkIiA6ICJmZTYxY2RiMjUyMTA0ODYzYTljY2E2ODAwZDRiMzgzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNeVNoYWRvd3MiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDAyMWRiYjc3MzdiZDM1MjM0NDRkNTc3NjBlMWY2MzkzOGVlMTI4NjA4MDM4OTU1M2IzYTY4M2VlOGEzYjkwYiIKICAgIH0KICB9Cn0=");
        put("keeper-npc-skin", "eyJ0aW1lc3RhbXAiOjE1ODE5MTIzMjQ4MzQsInByb2ZpbGVJZCI6IjgyYzYwNmM1YzY1MjRiNzk4YjkxYTEyZDNhNjE2OTc3IiwicHJvZmlsZU5hbWUiOiJOb3ROb3RvcmlvdXNOZW1vIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzI4MWEwZDVkOWY3OGU4Y2FlOTlmMGVhNDExNDhkYmQ2YjJkZTAyNmEzYzc5NTgyMzg4NjMyMGJhNWVkMDI0In19fQ==");
        put("mail-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTY0MjQ0OTExOTcxNCwKICAicHJvZmlsZUlkIiA6ICJkODAwZDI4MDlmNTE0ZjkxODk4YTU4MWYzODE0Yzc5OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVCTFJ4eCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kNDkwZjY2OGU4ZGY0YzliZDMyODVjMmJiNWU0NWU0YWZlYWZiYzhkZWQ0Y2VkZWQzMzU0MmNjZTgyODVmMzM1IgogICAgfQogIH0KfQ==");
        put("perk-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTY2OTQ2NDc3NDI4MCwKICAicHJvZmlsZUlkIiA6ICJmODFhNzJhZWZjMjY0MjU0YTQ5NzE0OWYzMjJiZjJlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEZXJsYW5fODgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGFmMmQ1ZjgzZjIyNGU0ODA0NjgwZTBjMzNlNGEyZWNjNTk2ZmYyYjBjNzFlMDY2ODgxNmJhNDI5MTJhYzQyZiIKICAgIH0KICB9Cn0=");
        put("prestige-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTYxNDE5OTYwMzQwOCwKICAicHJvZmlsZUlkIiA6ICJiNzQ3OWJhZTI5YzQ0YjIzYmE1NjI4MzM3OGYwZTNjNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTeWxlZXgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGRlOTgyMDAzNTBkMjM4ZjJjNjBhYWI5MmE0NmM2ZTY1ODc5ZWE1ZWE3OWExMGJiZmU1NjZhNTg5MWUwNDNiOSIKICAgIH0KICB9Cn0=");
        put("quest-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTU5OTIxNzI3NjA5NywKICAicHJvZmlsZUlkIiA6ICJkNjBmMzQ3MzZhMTI0N2EyOWI4MmNjNzE1YjAwNDhkYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCSl9EYW5pZWwiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmM4Yjc5N2M1NjQ4YzQwNDFkNWE0ZTYwYTY1OGMxMjAzMGJiZGQ3OTM4NWRjMzA4NGRlZmVkYzBjZmQ1MmZjNSIKICAgIH0KICB9Cn0=");
        put("shop-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTY0NTk4MDY0NjU4NywKICAicHJvZmlsZUlkIiA6ICI1MjhlYzVmMmEzZmM0MDA0YjYwY2IwOTA5Y2JiMjdjYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJQdWxpenppIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2NmNzhlNzVkZjlkMjg4YjM5ZDRlMDFjNzEwYjRlZjNhYmI2NDQwNGVmZDU2YWQ5OTE1ZTU5ZWVhMTI2ZWNlMGUiCiAgICB9CiAgfQp9");
        put("status-npc-skin", "ewogICJ0aW1lc3RhbXAiIDogMTY0ODY3MjI4MTczMiwKICAicHJvZmlsZUlkIiA6ICI1MjhlYzVmMmEzZmM0MDA0YjYwY2IwOTA5Y2JiMjdjYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJQdWxpenppIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2MxNTIxYTAyNTY2ODgxYjIwNTA4Mzg5YjM4MmEzMDIzMWE4NDViYTc0OWEyN2QxNDQ5NTUyNzQ3NDgxY2Y4YjkiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");

        put("noob-protect.enable", true);
        put("noob-protect.level", 120);
        put("noob-protect.damage_boost", 1.1);
        put("noob-protect.damage_reduce", 0.9);

        put("mm-listener.mm_mob_a.xp-provide", "10-100");
        put("mm-listener.mm_mob_a.coins-provide", "10-100");
        put("mm-listener.mm_mob_a.kill-message", "&c&l击杀! &a成功击杀{mobName} 获得 {coin} {exp}");
        put("mm-listener.mm_mob_a.kill-broadcast.enable", true);
        put("mm-listener.mm_mob_a.kill-broadcast.message", "&c&l击杀! &a成功击杀{mobName} 获得 {coin} {exp}");

        put("custom-chat-format.enable", false);
        put("custom-chat-format.format", "%pit_genesis_tag%%pit_level_tag_roman% %s&f: %s");

        put("event-online-required.hunt", 3);
        put("event-online-required.patronage", 5);
        put("event-online-required.dragon_egg", 10);
        put("event-online-required.koth", 10);
        put("event-online-required.auction", 10);
        put("event-online-required.cake", 10);
        put("event-online-required.care_package", 20);
        put("event-online-required.everyone_bounty_event", 10);
        put("event-online-required.quick_math_event", 5);
        put("event-online-required.ham", 20);
        put("event-online-required.rage_pit", 20);
        put("event-online-required.red_vs_blue", 20);
        put("event-online-required.spire", 20);
        put("event-online-required.block_head", 999);
        put("event-online-required.squads", 999);

        put("highest-prestige", 35);

        put("scoreboard-showtime", true);

        put("rate.dark.vip1.test", "pit.vip1");
        put("rate.dark.vip1.value", 0.08);
        put("rate.dark.vip2.test", "pit.vip2");
        put("rate.dark.vip2.value", 0.04);
        put("rate.dark.default.value", 0.02);

        put("rate.normal.vip1.test", "pit.vip1");
        put("rate.normal.vip1.value", 0.08);
        put("rate.normal.vip2.test", "pit.vip2");
        put("rate.normal.vip2.value", 0.04);
        put("rate.normal.default.value", 0.02);

        put("rate.rage.vip1.test", "pit.vip1");
        put("rate.rage.vip1.value", 0.08);
        put("rate.rage.vip2.test", "pit.vip2");
        put("rate.rage.vip2.value", 0.04);
        put("rate.rage.default.value", 0.02);

        put("rate.dark_green.vip1.test", "pit.vip1");
        put("rate.dark_green.vip1.value", 0.08);
        put("rate.dark_green.vip2.test", "pit.vip2");
        put("rate.dark_green.vip2.value", 0.04);
        put("rate.dark_green.default.value", 0.02);

        put("mythicDropChance.vip1.test", "permission.vip1");
        put("mythicDropChance.vip1.value", 0.01);
        put("mythicDropChance.vip2.test", "permission.vip2");
        put("mythicDropChance.vip2.value", 0.02);
        put("New.Worldenble", true);
        put("New.WorldTime", 20 * 60 * 5L);
    }};
    private static boolean removeSupportWhenNoPermission = false;
    private static UUID kingsQuestsMarker = UUID.randomUUID();
    private static String waterMark = "&k~~~~~";
    private static String botName = "&cBot";
    private static YamlConfiguration config;
    private static Double defaultMythicChance = 0.01d;

    private NewConfiguration() {
        // private constructor for singleton
    }

    public static int getVipPrice() {
        return vipPrice;
    }

    public static String getPriceName() {
        return priceName;
    }

    public static String getDateFormat() {
        return dateFormat;
    }

    public static boolean isNoobProtect() {
        return noobProtect;
    }

    public static int getNoobProtectLevel() {
        return noobProtectLevel;
    }

    public static double getNoobDamageBoost() {
        return noobDamageBoost;
    }

    public static double getNoobDamageReduce() {
        return noobDamageReduce;
    }

    public static double getLuckGem() {
        return luckGem;
    }

    public static boolean isCustomChatFormatEnable() {
        return customChatFormatEnable;
    }

    public static String getCustomChatFormat() {
        return customChatFormat;
    }

    public static boolean isScoreboardShowtime() {
        return scoreboardShowtime;
    }

    public static String getPitSupportPermission() {
        return pitSupportPermission;
    }

    public static boolean isRemoveSupportWhenNoPermission() {
        return removeSupportWhenNoPermission;
    }

    public static UUID getKingsQuestsMarker() {
        return kingsQuestsMarker;
    }

    public static void setKingsQuestsMarker(UUID kingsQuestsMarker) {
        NewConfiguration.kingsQuestsMarker = kingsQuestsMarker;
    }

    public static String getWaterMark() {
        return waterMark;
    }

    public static String getBotName() {
        return botName;
    }

    public static YamlConfiguration getConfig() {
        return config;
    }

    public static Map<String, Integer> getEventOnlineRequired() {
        return eventOnlineRequired;
    }

    public static Map<String, MythicMobsConf> getMythicMobs() {
        return mythicMobs;
    }

    public static void save() {
        try {
            config.save(new File(ThePit.getInstance().getDataFolder(), "custom.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadFile() {
        File file = new File(ThePit.getInstance().getDataFolder(), "custom.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static void load() {
        refreshAndSave();

        vipPrice = config.getInt("vip-price", 500);
        priceName = config.getString("price-name", "点券");

        dateFormat = config.getString("dateFormat", "MM/dd HH:mm");

        noobProtect = config.getBoolean("noob-protect.enable");
        noobProtectLevel = config.getInt("noob-protect.level");
        noobDamageBoost = config.getDouble("noob-protect.damage_boost");
        noobDamageReduce = config.getDouble("noob-protect.damage_reduce");

        luckGem = config.getDouble("luck-gem", 0.30);
        waterMark = config.getString("water-mark");
        botName = config.getString("bot-name");
        customChatFormatEnable = config.getBoolean("custom-chat-format.enable", false);
        customChatFormat = config.getString("custom-chat-format.format", "%pit_level_tag_roman% %s&f: %s");

//        PrestigeStatusButton.limit = config.getInt("highest-prestige");

        eventOnlineRequired.clear();
        ConfigurationSection eventSection = config.getConfigurationSection("event-online-required");
        if (eventSection != null) {
            for (String eventName : eventSection.getKeys(false)) {
                eventOnlineRequired.put(eventName, eventSection.getInt(eventName));
            }
        }

//        ThePunchEnchant.PUNCH_Y = config.getDouble("punch_y", 4.0);

        pitSupportPermission = config.getString("pitSupportPermission", pitSupportPermission);
        removeSupportWhenNoPermission = config.getBoolean("removeSupportWhenNoPermission", false);

        scoreboardShowtime = config.getBoolean("scoreboard-showtime");

        mythicChance.clear();
        ConfigurationSection mythicDropSection = config.getConfigurationSection("mythicDropChance");
        if (mythicDropSection != null) {
            for (String groupName : mythicDropSection.getKeys(false)) {
                if (groupName.equals("default")) {
                    defaultMythicChance = 0.005;
                    continue;
                }
                String permission = mythicDropSection.getString(groupName + ".test");
                double chance = 0.0005;
                if (permission != null) {
                    mythicChance.add(new AbstractMap.SimpleEntry<>(permission, chance));
                }
            }
        }
        mythicChance.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        String kingsMarkerStr = config.getString("kingsQuestsMarker");
        if (kingsMarkerStr != null) {
            kingsQuestsMarker = UUID.fromString(kingsMarkerStr);
        } else {
            config.set("kingsQuestsMarker", kingsQuestsMarker.toString());
            save();
        }

        mythicMobs.clear();
        ConfigurationSection mmListenerSection = config.getConfigurationSection("mm-listener");
        if (mmListenerSection != null) {
            for (String mmid : mmListenerSection.getKeys(false)) {
                String xpRangeStr = mmListenerSection.getString(mmid + ".xp-provide");
                String coinsRangeStr = mmListenerSection.getString(mmid + ".coins-provide");
                if (xpRangeStr == null || coinsRangeStr == null) continue;

                IntRange xpRange = parseRange(xpRangeStr);
                IntRange coinsRange = parseRange(coinsRangeStr);

                String killMessage = mmListenerSection.getString(mmid + ".kill-message");
                boolean killBroadcast = mmListenerSection.getBoolean(mmid + ".kill-broadcast.enable");
                String killBroadcastMessage = mmListenerSection.getString(mmid + ".kill-broadcast.message");

                mythicMobs.put(mmid, new MythicMobsConf(xpRange.getFirst(), xpRange.getLast(), coinsRange.getFirst(), coinsRange.getLast(), killMessage, killBroadcast, killBroadcastMessage));
            }
        }

        rareRate.clear();

        ConfigurationSection darkRateSection = config.getConfigurationSection("rate.dark");
        if (darkRateSection != null) {
            List<Rate> darkRates = new ArrayList<>();
            for (String key : darkRateSection.getKeys(false)) {
                String permission = darkRateSection.getString(key + ".test", "pit." + key);
                darkRates.add(new Rate(permission, 0.002));
            }
            rareRate.put(MythicColor.DARK, darkRates);
        }

        ConfigurationSection normalSection = config.getConfigurationSection("rate.normal");
        if (normalSection != null) {
            List<Rate> normalRates = new ArrayList<>();
            for (String key : normalSection.getKeys(false)) {
                String permission = normalSection.getString(key + ".test");
                if (permission == null) continue;
                double chance = 0.005;
                normalRates.add(new Rate(permission, chance));
            }
            rareRate.put(MythicColor.NONE, normalRates);
        }

        ConfigurationSection rageSection = config.getConfigurationSection("rate.rage");
        if (rageSection != null) {
            List<Rate> rageRates = new ArrayList<>();
            for (String key : rageSection.getKeys(false)) {
                String permission = rageSection.getString(key + ".test", "pit." + key);
                double chance = 0.005;
                rageRates.add(new Rate(permission, chance));
            }
            rareRate.put(MythicColor.RAGE, rageRates);
        }

        ConfigurationSection darkGreenSection = config.getConfigurationSection("rate.dark_green");
        if (darkGreenSection != null) {
            List<Rate> darkGreenRates = new ArrayList<>();
            for (String key : darkGreenSection.getKeys(false)) {
                String permission = darkGreenSection.getString(key + ".test", "pit." + key);
                double chance = 0.005;
                darkGreenRates.add(new Rate(permission, chance));
            }
            rareRate.put(MythicColor.DARK_GREEN, darkGreenRates);
        }

        ConfigurationSection festivals = config.getConfigurationSection("festival-boosts");
        if (festivals != null) {
            for (String key : festivals.getKeys(false)) {
                festivalBoosts.put(key, festivals.getDouble(key));
            }
        }
    }

    private static IntRange parseRange(String rangeStr) {
        String[] split = rangeStr.split("-");
        int start = Integer.parseInt(split[0]);
        int end = Integer.parseInt(split[1]);
        return new IntRange(start, end);
    }

    public static double getMythicDropChance(Player player) {
        if (mythicChance.isEmpty()) return defaultMythicChance;
        for (Map.Entry<String, Double> entry : mythicChance) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultMythicChance;
    }

    public static double getFestivalBoosts() {
        if (!TimeUtil.IS_HOLIDAY) {
            return 0.0d;
        }
        return festivalBoosts.getOrDefault(TimeUtil.HOLIDAY_NAME, festivalBoosts.getOrDefault("default", 0.35d));
    }

    public static double getChance(Player player, MythicColor color) {
        List<Rate> list;
        if (color == MythicColor.DARK || color == MythicColor.RAGE || color == MythicColor.DARK_GREEN) {
            list = rareRate.get(color);
        } else {
            list = rareRate.get(MythicColor.NONE);
        }

        if (player.hasMetadata("lucky")) {
            return luckGem;
        }

        if (list == null || list.isEmpty()) {
            return 0.001;
        }

        double maxValue = 0.001;
        for (Rate rate : list) {
            if (player.hasPermission(rate.permission) && rate.value > maxValue) {
                maxValue = rate.value;
            }
        }
        return maxValue;
    }

    private static void refreshAndSave() {
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (config.get(entry.getKey()) == null) {
                config.set(entry.getKey(), entry.getValue());
            }
        }
        save();
    }

    private record Rate(String permission, double value) {
    }

    public static final class IntRange {
        private final int first;
        private final int last;

        public IntRange(int first, int last) {
            this.first = first;
            this.last = last;
        }

        public int getFirst() {
            return first;
        }

        public int getLast() {
            return last;
        }
    }

    public static class MythicMobsConf {
        public final IntRange expRange;
        public final IntRange coinsRange;
        public final String killMessage;
        public final boolean killBroadcast;
        public final String killBroadcastMessage;

        public MythicMobsConf(int expStart, int expEnd, int coinsStart, int coinsEnd, String killMessage, boolean killBroadcast, String killBroadcastMessage) {
            this.expRange = new IntRange(expStart, expEnd);
            this.coinsRange = new IntRange(coinsStart, coinsEnd);
            this.killMessage = killMessage;
            this.killBroadcast = killBroadcast;
            this.killBroadcastMessage = killBroadcastMessage;
        }
    }

    // Getters and setters can be added here if needed

}
