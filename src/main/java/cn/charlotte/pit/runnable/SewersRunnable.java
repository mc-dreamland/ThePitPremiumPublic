package cn.charlotte.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SewersRunnable extends BukkitRunnable implements Listener {

    private static final int MAX_SEWERS_CHESTS = 15;
    private static final List<Location> existSewersChests = new ArrayList<>();
    //(type.equals(Material.DARK_PRISMARINE) || type.equals(Material.SOUL_SAND) || type.equals(Material.DRIED_KELP_BLOCK))
    private static final RandomList<String> randomList = new RandomList<>(
            Map.entry("xp", 150),
            Map.entry("gold", 150),
            Map.entry("diamond_chestplate", 150),
            Map.entry("diamond_leggings", 50),
            Map.entry("diamond_boots", 150),
            Map.entry("rubbish", 49),
            Map.entry("milk_buckets", 1),
            Map.entry("dark_prismarine", 100),
            Map.entry("soul_sand", 100),
            Map.entry("dried_kelp_block", 100)
    );

    public static List<Location> getExistSewersChests() {
        return new ArrayList<>(existSewersChests);
    }

    public static void checkAndRefreshChests(boolean force) {
        if (existSewersChests.size() >= MAX_SEWERS_CHESTS) {
            return;
        }
        int base = 4;
        int totalOnline = Bukkit.getOnlinePlayers().size();

        if (!force && CombatListener.isNight()) {
            CC.SuperboardCast(MessageType.SEWERS_CHEST, CC.translate("&2下水道过于黑暗，导致下水道未刷新新的物资！"));
            return;
        }

        List<Location> allLocations = ThePit.getInstance().getPitConfig().getSewersChestsLocations();
        if (allLocations == null || allLocations.isEmpty()) return;

        // 没必要情况，在开始限制了最大同时存在的数量
//        for (Location loc : allLocations) {
//            Block b = loc.getBlock();
//            if (b.getType() != Material.AIR) {
//                b.setType(Material.AIR);
//            }
//        }

        int extraByOnline = (totalOnline - 30) / 35;

        int playersInTargetRegions = 0;
        int vipPlayersInTargetRegions = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (inSewer(p)) {
                PlayerProfile pitProfile = Util.getPitProfile(p);
                if (pitProfile.prestige >= 3 && pitProfile.getLevel() >= 30) {
                    playersInTargetRegions++;
                }
                if (pitProfile.isSupporter()) {
                    vipPlayersInTargetRegions++;
                }
            }
        }
        int extraByRegion = playersInTargetRegions / 5;
        int extraVipByRegion = vipPlayersInTargetRegions / 3;

        int targetChestCount = base + extraByOnline + extraByRegion + extraVipByRegion;

        targetChestCount = Math.min(targetChestCount, allLocations.size());

        if (!force && targetChestCount <= 0) {
            CC.SuperboardCast(MessageType.SEWERS_CHEST, CC.translate("&2当前条件不满足，下水道未刷新新的物资！"));
            return;
        }

        List<Location> shuffled = new ArrayList<>(allLocations);
        Collections.shuffle(shuffled);
        List<Location> selectedLocs = shuffled.subList(0, targetChestCount);

        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            for (Location loc : selectedLocs) {
                loc.getBlock().setType(Material.END_PORTAL_FRAME);
                existSewersChests.add(loc);
            }
        });

        CC.SuperboardCast(
                MessageType.SEWERS_CHEST,
                CC.translate("&2下水道箱子已刷新 &7(基础: " + base
                        + " &7/ 在线加成: " + extraByOnline
                        + " &7/ 区域加成: " + extraByRegion
                        + " &7/ 会员加成: " + extraVipByRegion
                        + " &7/ 总计: " + targetChestCount + ")")
        );
    }

    public static boolean inSewer(Player player) {
        return PlaceholderAPI.setPlaceholders(player, "%worldguard_region_name%").toLowerCase(Locale.ROOT).startsWith("xxd");
    }

    @Override
    public void run() {
        checkAndRefreshChests(false);
    }

    private void schedulePossibleRespawn() {
        int sec = 60 + RandomUtil.random.nextInt(61); // [60, 120]
        long delay = sec * 20L;

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            if (existSewersChests.size() >= MAX_SEWERS_CHESTS) return;
            if (CombatListener.isNight()) return;
            // 33% 概率再生
            if (RandomUtil.random.nextInt(100) >= 33) return;

            List<Location> allLocations = ThePit.getInstance().getPitConfig().getSewersChestsLocations();
            if (allLocations == null || allLocations.isEmpty()) return;

            List<Location> shuffled = new ArrayList<>(allLocations);
            Collections.shuffle(shuffled);

            Location respawnLoc = null;
            for (Location loc : shuffled) {
                boolean exists = existSewersChests.stream()
                        .anyMatch(e -> Util.isSameBlockLocation(e, loc));
                if (!exists && loc.getBlock().getType() == Material.AIR) {
                    respawnLoc = loc;
                    break;
                }
            }
            if (respawnLoc == null) return;

            respawnLoc.getBlock().setType(Material.END_PORTAL_FRAME);
            existSewersChests.add(respawnLoc);

            CC.SuperboardCast(
                    MessageType.SEWERS_CHEST,
                    CC.translate("&2一个新的下水道箱子在黑暗中悄然出现了...")
            );
        }, delay);
    }

    @EventHandler
    public void onChestInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        if (!player.isOp() && CombatListener.isNight() && clickedBlock.getType() == Material.END_PORTAL_FRAME) {
            player.sendMessage(Component.text("§c夜深！下水道太暗了，你无法搜寻该箱子！"));
            return;
        }


        Location clickedLoc = clickedBlock.getLocation();

        existSewersChests.stream()
                .filter(loc -> Util.isSameBlockLocation(clickedLoc, loc))
                .findFirst().ifPresent(chestLoc -> claim(player, chestLoc));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material type = event.getBlock().getType();
        Player player = event.getPlayer();
        if (!inSewer(player)) {
            if (type.equals(Material.DARK_PRISMARINE) || type.equals(Material.SOUL_SAND) || type.equals(Material.DRIED_KELP_BLOCK)) {
                player.sendMessage(Component.text("§7该方块只能在下水道中放置!"));
                event.setCancelled(true);
            }
        } else {
            if (!type.equals(Material.DARK_PRISMARINE) && !type.equals(Material.SOUL_SAND) && !type.equals(Material.DRIED_KELP_BLOCK)) {
                player.sendMessage(Component.text("§7该方块无法在下水道中放置!"));
                event.setCancelled(true);
            }
        }
    }

    private void claim(Player player, Location chestLoc) {
        if (!player.isOp() && CombatListener.isNight()) {
            player.sendMessage(CC.translate("&c夜已深 &7已禁用下水道箱子的获得"));
            return;
        }
        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&c你的背包是满的, 无法领取下水道箱子"));
            return;
        }

        chestLoc.getBlock().setType(Material.AIR);
        existSewersChests.remove(chestLoc);

        // Kotlin 扩展：在 Java 调用顶层函数：<FileName>Kt.getPitProfile(player)
        var profile = Util.getPitProfile(player);

        String id = randomList.random();
        if (id == null) return;

        String rewardName = "";
        int amount = 0;

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        switch (id) {
            case "xp": {
                amount = RandomUtil.random.nextInt(50) + 100;
                profile.setExperience(profile.getExperience() + amount);
                profile.applyExperienceToPlayer(player);
                rewardName = "&b" + amount + " 经验";
                break;
            }
            case "gold": {
                amount = RandomUtil.random.nextInt(200) + 50;
                profile.setCoins(profile.getCoins() + amount);
                profile.grindCoins(amount);
                rewardName = "&e" + amount + " 金币";
                break;
            }
            case "diamond_chestplate": {
                player.getInventory().addItem(
                        new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                                .name("&2下水道之钻石胸甲")
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable()
                );
                rewardName = "&2下水道之钻石胸甲 " + rewardName;
                break;
            }
            case "diamond_leggings": {
                player.getInventory().addItem(
                        new ItemBuilder(Material.DIAMOND_LEGGINGS)
                                .name("&2下水道之钻石裤")
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable()
                );
                rewardName = "&2下水道之钻石裤 " + rewardName;
                break;
            }
            case "diamond_boots": {
                player.getInventory().addItem(
                        new ItemBuilder(Material.DIAMOND_BOOTS)
                                .name("&2下水道之钻石鞋子")
                                .deathDrop(true)
                                .canSaveToEnderChest(true)
                                .canTrade(true)
                                .internalName("shopItem")
                                .buildWithUnbreakable()
                );
                rewardName = "&2下水道之钻石鞋子 " + rewardName;
                break;
            }
            case "dark_prismarine": {
                amount = 32;
                Util.giveItems(player, new ItemBuilder(Material.DARK_PRISMARINE)
                        .canDrop(false)
                        .canSaveToEnderChest(false)
                        .deathDrop(true)
                        .internalName("sewer_block")
                        .amount(amount).build());
                rewardName = "&7" + amount + " &2暗海晶砖";
                break;
            }
            case "soul_sand": {
                amount = 32;
                Util.giveItems(player, new ItemBuilder(Material.SOUL_SAND)
                        .canDrop(false)
                        .canSaveToEnderChest(false)
                        .deathDrop(true)
                        .internalName("sewer_block")
                        .amount(amount).build());
                rewardName = "&7" + amount + " &2灵魂沙";
                break;
            }
            case "dried_kelp_block": {
                amount = 32;
                Util.giveItems(player, new ItemBuilder(Material.DRIED_KELP_BLOCK)
                        .canDrop(false)
                        .canSaveToEnderChest(false)
                        .deathDrop(true)
                        .internalName("sewer_block")
                        .amount(amount).build());
                rewardName = "&7" + amount + " &2干海带块";
                break;
            }
            case "rubbish": {
                player.getInventory().addItem(PitItems.RUBBIN.toItemStack());
                rewardName = rewardName + " &7+1 &2下水道废弃物";
                break;
            }
            case "milk_buckets": {
                player.getInventory().addItem(
                        PitItems.MILK_BUCKET.toItemStack()
                );
                rewardName = "&f牛奶 +1" + rewardName;
                break;
            }
            default:
                break;
        }

        player.sendMessage(CC.translate("&9下水道! &7你领取了下水道奖励. " + rewardName));
        schedulePossibleRespawn();
    }
}
