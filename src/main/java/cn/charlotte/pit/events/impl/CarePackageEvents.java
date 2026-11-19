package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.menu.pack.PackageMenu;
import cn.charlotte.pit.util.DirectionUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.hologram.Hologram;
import cn.charlotte.pit.util.hologram.HologramAPI;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.Data;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/30 16:41
 */

public class CarePackageEvents implements INormalEvent, Listener, IScoreBoardInsert {
    @Getter
    private static Location chest;
    @Getter
    private static ChestData chestData;

    private static BukkitRunnable runnable;

    private static Cooldown endTimer;

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e" + getEventName() + " &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 空投箱随机降落指定位置");
        lore.add("&7- 交替点击开启空投箱");
        lore.add("&7- 全服玩家共用同一计分器");
        lore.add("&7- 计分板显示距离方位");
        lore.add("&7- 空投箱内拥有 &e金币 &b经验 &b声望 &d神话装备");
        lore.add("");
        return lore;
    }

    @Override
    public String getEventInternalName() {
        return "care_package";
    }

    @Override
    public String getEventName() {
        return "空投";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    private Location generateLocation() {
        return RandomUtil.generateRandomLocation();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ("care_package".equals(ThePit.getInstance().getEventFactory().getActiveNormalEventName())) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST && event.getClickedBlock().getLocation().equals(chest)) {
                event.setCancelled(true);
                if (chestData == null) {
                    return;
                }
                if (chestData.getNum() <= 0) {
                    click(event.getPlayer(), chestData);
                    return;
                }
                if (chestData.isLeft()) {
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        click(event.getPlayer(), chestData);
                    }
                } else {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        click(event.getPlayer(), chestData);
                    }
                }
            }
        }
    }

    private void click(Player player, ChestData data) {
        if (data.getNum() <= 0) {
            new PackageMenu().openMenu(player);
            data.getFirstHologram().deSpawn();
            data.getSecondHologram().setText(CC.translate("&a&l" + (data.isLeft() ? "左键" : "右键") + "开启"));
            return;
        }
        data.setNum(data.getNum() - 1);

        chest.getWorld().playSound(chest, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5F, 1.5F);

        data.setLeft(!data.isLeft());
        Hologram hologram = data.getFirstHologram();
        if (data.getNum() > 150) {
            hologram.setText(CC.translate("&a&l" + data.getNum()));
            data.getSecondHologram().setText(CC.translate("&a&l" + (data.isLeft() ? "左键" : "右键") + "点击"));
        } else if (data.getNum() > 35) {
            hologram.setText(CC.translate("&e&l" + data.getNum()));
            data.getSecondHologram().setText(CC.translate("&e&l" + (data.isLeft() ? "左键" : "右键") + "点击"));
        } else {
            hologram.setText(CC.translate("&c&l" + data.getNum()));
            data.getSecondHologram().setText(CC.translate("&c&l" + (data.isLeft() ? "左键" : "右键") + "点击"));
        }
    }

    @Override
    public void onActive() {
        final List<Location> locations = ThePit.getInstance().getPitConfig().getPackageLocations();
        if (locations.isEmpty()) {
            CC.boardCast("&c警告! &6空投&7 坐标信息未配置, 请联系管理员");
            ThePit.getInstance().getEventFactory().inactiveEvent(this);
            return;
        }

        final Location location = locations.get(RandomUtil.random.nextInt(locations.size())).clone().getBlock().getLocation();
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        Bukkit.getPluginManager()
                .registerEvents(this, ThePit.getInstance());
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            location.getWorld().strikeLightningEffect(location);
            CC.boardCast("&6&l空投! &7一个新的空投已在地图降落!打开可以获得神话物品,声望等稀有物资!");
            location.getBlock().setType(Material.CHEST);
            chest = location;

            Map<Integer, ItemStack> items = PackageMenu.getItems();
            for (int i = 0; i < RandomUtil.random.nextInt(3) + 3; i++) {
                int nextInt = RandomUtil.random.nextInt(27);
                while (items.get(nextInt) != null && items.get(nextInt).getType() != Material.AIR) {
                    nextInt = RandomUtil.random.nextInt(27);
                }
                items.put(nextInt, RandomUtil.helpMeToChooseOne(
                        new MythicLeggingsItem().toItemStack(),
                        new MythicSwordItem().toItemStack(),
                        new MythicBowItem().toItemStack(),
                        new MythicSwordItem().toItemStack(),
                        new MythicBowItem().toItemStack()
                ));
            }

            for (int i = 0; i < RandomUtil.random.nextInt(3) + 3; i++) {
                int nextInt = RandomUtil.random.nextInt(27);
                while (items.get(nextInt) != null && items.get(nextInt).getType() != Material.AIR) {
                    nextInt = RandomUtil.random.nextInt(27);
                }
                items.put(nextInt, new ItemBuilder(Material.GOLD_BLOCK).name("&e+2声望").internalName("renown_reward").shiny().build());
            }

            for (int i = 0; i < RandomUtil.random.nextInt(3) + 3; i++) {
                if (items.get(i) == null) {
                    items.put(i, RandomUtil.helpMeToChooseOne(new ItemBuilder(Material.EXPERIENCE_BOTTLE).name("&b+1000经验值").internalName("xp_reward").shiny().build(), new ItemBuilder(Material.GOLD_INGOT).name("&6+1000硬币").internalName("coin_reward").shiny().build()));
                }
            }

            chestData = new ChestData();
            chestData.setFirstHologram(HologramAPI.createHologram(location.getBlock().getLocation().clone().add(0.5, 2.4, 0.5), CC.translate("&a&l200")));
            chestData.setSecondHologram(HologramAPI.createHologram(location.getBlock().getLocation().clone().add(0.5, 2.0, 0.5), CC.translate("&a&l左键点击")));

            chestData.getFirstHologram().spawn();
            chestData.getSecondHologram().spawn();

            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    ThePit.getInstance()
                            .getEventFactory()
                            .inactiveEvent(CarePackageEvents.this);
                }
            };
            runnable.runTaskLaterAsynchronously(ThePit.getInstance(), 20 * 60 * 5);
            endTimer = new Cooldown(5, TimeUnit.MINUTES);
        });
    }

    @Override
    public void onInactive() {
        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            HandlerList.unregisterAll(this);
            chestData.getSecondHologram().deSpawn();
            PackageMenu.getItems().clear();
            chest.getBlock().setType(Material.AIR);
            chestData = null;
            chest = null;
            runnable.cancel();
            runnable = null;
        });
    }

    /**
     * 监听方块放置事件，禁止在空投附近4格内放置方块
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if ("care_package".equals(ThePit.getInstance().getEventFactory().getActiveNormalEventName()) && chest != null) {
            Location placedLocation = event.getBlockPlaced().getLocation();

            if (isWithinRadius(placedLocation, chest, 4)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(CC.translate("&c你不能在空投箱附近4格内放置方块！"));
            }
        }
    }

    /**
     * 检查位置是否在目标位置的指定半径内
     *
     * @param location 要检查的位置
     * @param target   目标位置
     * @param radius   半径（格数）
     * @return 是否在半径内
     */
    private boolean isWithinRadius(Location location, Location target, int radius) {
        if (!location.getWorld().equals(target.getWorld())) {
            return false;
        }

        int dx = Math.abs(location.getBlockX() - target.getBlockX());
        int dy = Math.abs(location.getBlockY() - target.getBlockY());
        int dz = Math.abs(location.getBlockZ() - target.getBlockZ());

        return dx <= radius && dy <= radius && dz <= radius;
    }

    @Override
    public List<String> insert(Player player) {
        if (chest == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        String targetDirection = DirectionUtil.getTargetDirection(player, chest);
        int distance = (int) player.getLocation().distance(chest);

        if (endTimer.getRemaining() > 2 * 60 * 1000L) {
            lines.add("&f剩余: &a" + TimeUtil.millisToTimer(endTimer.getRemaining()));
        } else if (endTimer.getRemaining() >= 60 * 1000L) {
            lines.add("&f剩余: &e" + TimeUtil.millisToTimer(endTimer.getRemaining()));
        } else {
            lines.add("&f剩余: &c" + TimeUtil.millisToTimer(endTimer.getRemaining()));
        }

        if (!chestData.getRewarded().contains(player.getUniqueId())) {
            if (chestData.getNum() == 200) {
                lines.add("&f追踪: &c&l? &f" + distance + "m");

            } else if (chestData.getNum() > 0) {
                lines.add("&f追踪: &c&l" + targetDirection + " &f" + distance + "m");
            } else {
                lines.add("&f追踪: &a&l" + targetDirection + " &f" + distance + "m");
            }
        }

        return lines;
    }


    @Data
    public static class ChestData {
        private boolean left = true;
        private int num = 200;
        private Hologram firstHologram;
        private Hologram secondHologram;
        private List<UUID> rewarded = new ArrayList<>();
    }

}
