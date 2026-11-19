package cn.charlotte.pit.setup;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.commands.admin.Setup;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.configuration.annotations.ConfigData;
import cn.charlotte.pit.util.configuration.annotations.ConfigSerializer;
import cn.charlotte.pit.util.configuration.serializer.LocationSerializer;
import cn.charlotte.pit.util.item.ItemUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocationsSetup implements Listener {

    private static final Map<String, String> activeHolograms = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public static void removeHologram(String hologramName) {
        String hologramId = activeHolograms.get(hologramName.toLowerCase());
        if (hologramId != null) {
            Hologram hologram = DHAPI.getHologram(hologramId);
            if (hologram != null) {
                hologram.delete();
            }
            activeHolograms.remove(hologramName.toLowerCase());
        }
    }

    public static void removeAllHolograms() {
        for (String hologramId : activeHolograms.values()) {
            Hologram hologram = DHAPI.getHologram(hologramId);
            if (hologram != null) {
                hologram.delete();
            }
        }
        activeHolograms.clear();
    }

    public static void showAllHolograms() {
        removeAllHolograms();
        showSingleLocationsHolograms();
        showListLocationsHolograms();
    }

    public static void showSingleLocationsHolograms() {
        for (Field field : PitConfig.class.getDeclaredFields()) {
            ConfigSerializer annotation = field.getAnnotation(ConfigSerializer.class);
            if (annotation == null || annotation.serializer() != LocationSerializer.class) {
                continue;
            }

            ConfigData fieldAnnotation = field.getAnnotation(ConfigData.class);
            if (fieldAnnotation == null) continue;

            String path = fieldAnnotation.path();
            if (!path.startsWith("loc.")) continue;

            Location location = Setup.getSingleLocation(path);
            if (location != null) {
                createLocationHologram(path, location);
            }
        }
    }

    public static void showListLocationsHolograms() {
        for (Field field : PitConfig.class.getDeclaredFields()) {
            ConfigData fieldAnnotation = field.getAnnotation(ConfigData.class);
            if (fieldAnnotation == null) continue;

            String path = fieldAnnotation.path();
            if (!path.startsWith("list.")) continue;
            @SuppressWarnings("unchecked")
            List<Location> locations = (List<Location>) Setup.getConfigByPath(path);
            if (locations != null && !locations.isEmpty()) {
                for (int i = 0; i < locations.size(); i++) {
                    Location location = locations.get(i);
                    if (location == null) continue;

                    try {
                        Location holoLocation = location.clone().add(0.5, 2, 0.5);
                        String safeField = path.toLowerCase().replace('.', '_');
                        String hologramId = "setup-" + safeField + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);

                        List<String> lines = new ArrayList<>();
                        lines.add("§6§l" + path + " #" + (i + 1));
                        lines.add("§7字段: §f" + path);
                        lines.add("§7索引: §f" + (i + 1) + " / " + locations.size());
                        lines.add(String.format("§7坐标: §f%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));

                        Hologram hologram = DHAPI.createHologram(hologramId, holoLocation, false, lines);
                        if (hologram != null) {
                            activeHolograms.put(path.toLowerCase() + "-" + i, hologramId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void showHologramForField(String field) {
        removeHologram(field);

        if (field.startsWith("list.")) {
            @SuppressWarnings("unchecked")
            List<Location> locations = (List<Location>) Setup.getConfigByPath(field);
            if (locations != null && !locations.isEmpty()) {
                for (int i = 0; i < locations.size(); i++) {
                    Location location = locations.get(i);
                    if (location == null) continue;

                    try {
                        Location holoLocation = location.clone().add(0.5, 2, 0.5);
                        String safeField = field.toLowerCase().replace('.', '_');
                        String hologramId = "setup-" + safeField + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);

                        List<String> lines = new ArrayList<>();
                        lines.add("§6§l" + field + " #" + (i + 1));
                        lines.add("§7字段: §f" + field);
                        lines.add("§7索引: §f" + (i + 1) + " / " + locations.size());
                        lines.add(String.format("§7坐标: §f%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));

                        Hologram hologram = DHAPI.createHologram(hologramId, holoLocation, false, lines);
                        if (hologram != null) {
                            activeHolograms.put(field.toLowerCase() + "-" + i, hologramId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Location location = Setup.getSingleLocation(field);
            if (location != null) {
                createLocationHologram(field, location);
            }
        }
    }

    private static void createLocationHologram(String field, Location location) {
        if (location == null) return;

        try {
            Location holoLocation = location.clone().add(0.5, 2, 0.5);
            String safeField = field.toLowerCase().replace('.', '_');
            String hologramId = "setup-" + safeField + "-" + UUID.randomUUID().toString().substring(0, 8);

            List<String> lines = new ArrayList<>();
            lines.add("§6§l" + field);
            lines.add("§7字段: §f" + field);
            lines.add(String.format("§7坐标: §f%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ()));

            Hologram hologram = DHAPI.createHologram(hologramId, holoLocation, false, lines);
            if (hologram != null) {
                activeHolograms.put(field.toLowerCase(), hologramId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!PlayerUtil.isStaff(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (ItemUtil.getInternalName(item) == null) {
            return;
        }
        if (!ItemUtil.getInternalName(item).equals("LocationsSetup")) {
            return;
        }

        PlayerProfile profile = Util.getPitProfile(player);
        String setupMode = profile.getLocationsSetup();

        if (setupMode == null || setupMode.equals("no") || setupMode.equals("All")) {
            CC.send(MessageType.DEBUG, player, "&c请先使用 /pitsetup LocationsSetup 进入设置模式");
            return;
        }

        if (cooldowns.containsKey(player.getUniqueId()) && System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < 1000L) {
            long remaining = 1000L - (System.currentTimeMillis() - cooldowns.get(player.getUniqueId()));
            double seconds = remaining / 1000.0;
            CC.send(MessageType.DEBUG, player, String.format("&c请等待 &e%.1f&c 秒后再操作", seconds));
            return;
        }
        event.setCancelled(true);

        Location location = new Location(
                player.getLocation().getWorld(),
                Math.floor(player.getLocation().getX()) + 0.5,
                Math.floor(player.getLocation().getY()),
                Math.floor(player.getLocation().getZ()) + 0.5,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        if (player.isSneaking()) {
            try {
                boolean isListField = setupMode.startsWith("list.");

                if (isListField) {
                    @SuppressWarnings("unchecked")
                    List<Location> locationList = (List<Location>) Setup.getConfigByPath(setupMode);
                    if (locationList != null && !locationList.isEmpty()) {
                        locationList.removeLast();
                        ThePit.getInstance().getPitConfig().save();
                        CC.send(MessageType.DEBUG, player, "&a已删除 &e" + setupMode + " &a的最后一个位置");
                        showHologramForField(setupMode);
                    } else {
                        CC.send(MessageType.DEBUG, player, "&c列表为空，无法删除");
                    }
                } else {
                    Setup.setConfigByPath(setupMode, null);
                    CC.send(MessageType.DEBUG, player, "&a已删除位置 &e" + setupMode);
                    showHologramForField(setupMode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                CC.send(MessageType.DEBUG, player, "&c删除位置时发生错误");
            }
        } else {
            try {
                boolean isListField = setupMode.startsWith("list.");

                if (isListField) {
                    Setup.setConfigByPath(setupMode, location);
                    CC.send(MessageType.DEBUG, player, "&a已添加位置到 &e" + setupMode);
                    showHologramForField(setupMode);
                } else {
                    Setup.setConfigByPath(setupMode, location);
                    CC.send(MessageType.DEBUG, player, "&a已设置位置 &e" + setupMode);
                    showHologramForField(setupMode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                CC.send(MessageType.DEBUG, player, "&c设置位置时发生错误");
            }
        }

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                cooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > 1000L + 5000L);
            }
        }.runTaskLater(ThePit.getInstance(), 100L);
    }
}