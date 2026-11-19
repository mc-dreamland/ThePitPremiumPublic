package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.chat.CC;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class KingOfTheHillEvents implements INormalEvent, Listener {

    private static final int EVENT_DURATION_MINUTES = 5;
    private static final int RADIUS = 7;
    public static boolean isActive = false;
    @Getter
    private static HillData hillData;
    private final Map<Block, Material> blocks = new HashMap<>();
    private final Map<Block, BlockData> blockData = new HashMap<>();
    Location location = null;

    public static boolean isRegion(Player player) {
        Location loc = player.getLocation();
        return isGoldBlock(loc.add(0, -1, 0)) || isGoldBlock(loc.add(0, -2, 0));
    }

    private static boolean isGoldBlock(Location location) {
        return location.getBlock().getType() == Material.GOLD_BLOCK;
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> " + getEventName() + " &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 会生成一个金块平台");
        lore.add("&7- 平台上击杀玩家可以获得4倍&b经验&e金币");
        lore.add("");
        return lore;
    }

    @Override
    public String getEventInternalName() {
        return "koth";
    }

    @Override
    public String getEventName() {
        return "&b&l占山为王";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isActive) {
            return;
        }
        if (!"koth".equals(ThePit.getInstance().getEventFactory().getActiveNormalEventName())) {
            return;
        }

        hillData.spawnHologramsForPlayer(event.getPlayer());
    }

    @Override
    public void onActive() {
        List<Location> locations = ThePit.getInstance().getPitConfig().getKothLocs();
        isActive = true;


        if (locations == null || locations.isEmpty()) {
            Bukkit.broadcastMessage(CC.translate("&c&l占山为王！ &7活动区域未设置，请联系管理员设置！"));
            deactivateEvent();
            return;
        }
        int i = new Random().nextInt(locations.size());
        this.location = locations.get(i);
        hillData = new HillData(this.location);


        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        Bukkit.broadcastMessage(CC.translate("&b&l占山为王！ &7现在你可以在指定区域内获得四倍的&b经验&7与&6硬币&7加成！"));
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        spawnKothCircle(this.location, RADIUS, Material.GOLD_BLOCK);
        scheduleEventDeactivation();
    }

    private void scheduleEventDeactivation() {
        Bukkit.getScheduler().runTaskLater(
                ThePit.getInstance(),
                this::deactivateEvent,
                20L * 60 * KingOfTheHillEvents.EVENT_DURATION_MINUTES
        );
    }

    private void deactivateEvent() {
        ThePit.getInstance().getEventFactory().inactiveEvent(this);
        onInactive();
    }

    @Override
    public void onInactive() {
        despawnKothCircle();
        HandlerList.unregisterAll(this);
        isActive = false;
    }

    public void spawnKothCircle(Location center, int radius, Material material) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int extendedRadius = radius + 1;
                int squaredRadius = extendedRadius * extendedRadius;

                World world = center.getWorld();
                int centerX = center.getBlockX();
                int centerY = center.getBlockY();
                int centerZ = center.getBlockZ();
                for (int x = centerX - extendedRadius; x <= centerX + extendedRadius; x++) {
                    for (int z = centerZ - extendedRadius; z <= centerZ + extendedRadius; z++) {
                        if (Math.pow(centerX - x, 2) + Math.pow(centerZ - z, 2) <= squaredRadius) {
                            Block block = world.getBlockAt(x, centerY, z);
                            saveBlockData(block);
                            block.setType(material);
                        }
                    }
                }
                setupCentralBeacon(center);
                setupHolograms(center);
            }
        }.runTask(ThePit.getInstance());
    }

    private void saveBlockData(Block block) {
        blocks.put(block, block.getType());
        blockData.put(block, block.getBlockData());
    }

    private void setupCentralBeacon(Location center) {
        Block beaconBlock = center.clone().add(0, -1, 0).getBlock();
        saveBlockData(beaconBlock);
        beaconBlock.setType(Material.BEACON);
    }

    private void setupHolograms(Location center) {
        hillData.setHolograms();

    }

    public void despawnKothCircle() {
        blocks.forEach(Block::setType);
        blockData.forEach(Block::setBlockData);
        hillData.despawnAllHolograms();
    }

    @Data
    public static class HillData {
        private final Location center;
        private Hologram hologram;

        public HillData(Location location) {
            this.center = location;
            Hologram hd = DHAPI.getHologram("KOTH");
            if (hd != null) {
                this.hologram = hd;
                this.hologram.showAll();
            } else {
                this.hologram = DHAPI.createHologram("KOTH", center.clone().add(0.5, 5.0, 0.5), List.of("&b&l占山为王", "&e&l4x &b经验值&e/&6硬币"));
                this.hologram.showAll();
            }
        }

        public void setHolograms() {
            Hologram hd = DHAPI.getHologram("KOTH");
            if (hd != null) {
                this.hologram = hd;
                DHAPI.moveHologram(this.hologram, center.clone().add(0.5, 5.0, 0.5));
                this.hologram.showAll();
            } else {
                this.hologram = DHAPI.createHologram("KOTH", center.clone().add(0.5, 5.0, 0.5), List.of("&b&l占山为王", "&e&l4x &b经验值&e/&6硬币"));
                this.hologram.showAll();
            }
        }

        public void despawnAllHolograms() {
            if (hologram != null) {
                hologram.delete();
                hologram = null;
            }
        }

        public void spawnHologramsForPlayer(Player player) {
            if (hologram == null) {
                this.hologram = DHAPI.createHologram("KOTH", center.clone().add(0.5, 5.0, 0.5), List.of("&b&l占山为王", "&e&l4x &b经验值&e/&6硬币"));
                this.hologram.showAll();
            } else {
                hologram.show(player, 0);
            }
        }
    }
}
