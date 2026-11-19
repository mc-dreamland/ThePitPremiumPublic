package cn.charlotte.pit.nametag.hiding;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

public record NameHidingConfig(NameHidingRules.Settings rules, int refreshTicks, int changeDelayTicks,
                               int packetUpdatesPerTick, int profileRefreshesPerTick,
                               boolean revealItemEnabled, int price, long durationSeconds) {
    public static NameHidingConfig load(JavaPlugin plugin) throws Exception {
        File file = new File(plugin.getDataFolder(), "name-hiding.yml");
        if (!file.exists()) plugin.saveResource("name-hiding.yml", false);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        try (var source = plugin.getResource("name-hiding.yml")) {
            if (source == null) throw new IllegalStateException("缺少 name-hiding.yml 默认配置");
            yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(source, StandardCharsets.UTF_8)));
        }
        var areas = EnumSet.noneOf(NameHidingRules.Area.class);
        if (yaml.getBoolean("areas.spawn")) areas.add(NameHidingRules.Area.SPAWN);
        if (yaml.getBoolean("areas.middle")) areas.add(NameHidingRules.Area.MIDDLE);
        if (yaml.getBoolean("areas.wild")) areas.add(NameHidingRules.Area.WILD);
        var rules = new NameHidingRules.Settings(yaml.getBoolean("enabled"),
                yaml.getInt("prestige.min"), yaml.getInt("prestige.max"),
                yaml.getDouble("show-above-streak"), yaml.getDouble("show-above-bounty"), areas);
        int ticks = yaml.getInt("refresh-ticks"), price = yaml.getInt("reveal-item.price");
        int delay = yaml.getInt("change-delay-ticks"), budget = yaml.getInt("packet-updates-per-tick");
        int profiles = yaml.getInt("profile-refreshes-per-tick");
        long seconds = yaml.getLong("reveal-item.duration-seconds");
        if (ticks < 1 || delay < 0 || budget < 1 || profiles < 1 || price < 0 || seconds < 1)
            throw new IllegalArgumentException("检查间隔/更新预算/持续时间必须为正数，合并延迟/价格必须非负");
        return new NameHidingConfig(rules, ticks, delay, budget, profiles, yaml.getBoolean("reveal-item.enabled"), price, seconds);
    }
}
