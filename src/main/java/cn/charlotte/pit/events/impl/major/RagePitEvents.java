package cn.charlotte.pit.events.impl.major;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitUltimatelyDamageEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventMapEdits;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.BlockRestoreManager;
import cn.charlotte.pit.medal.impl.challenge.RagePitTopMedal;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.FaweUtils;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;
import lombok.AllArgsConstructor;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/30 0:33
 */

public class RagePitEvents implements IEpicEvent, Listener {
    @Getter
    private static final Map<UUID, DamageData> damageMap = new HashMap<>();
    private final MajorEventMapEdits mapEdits = new MajorEventMapEdits();
    @Getter
    private static int killed = 0;
    private static BukkitRunnable runnable;
    @Getter
    private static Cooldown timer;

    public static int getDamageRank(Player player) {
        DamageData damage = damageMap.get(player.getUniqueId());
        HashSet<Map.Entry<UUID, DamageData>> entry = new HashSet<>(damageMap.entrySet());

        Map<Double, Integer> rank = new HashMap<>();
        int i = 1;

        for (DamageData en : entry.stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingDouble(DamageData::getDamage).reversed())
                .toList()) {
            rank.put(en.getDamage(), i);
            i++;
        }

        return rank.get(damage.getDamage());
    }

    @Override
    public String getEventInternalName() {
        return "rage_pit";
    }

    @Override
    public String getEventName() {
        return "疯狂天坑";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e疯狂天坑事件 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 玻璃墙围绕中心坑阻挡粘液球平台");
        lore.add("&7- 玩家血量翻倍但无法自然恢复生命");
        lore.add("&7- 击杀玩家获得 &c愤怒的土豆");
        lore.add("&7- 食用土豆获得 &c生命恢复II &7持续4秒");
        lore.add("&7- 计分板记录伤害和击杀数");
        lore.add("");
        lore.add("&6&l事件奖励机制");
        lore.add("&7> 个人排名：按造成的伤害量排名");
        lore.add("&7> 全服目标：&e600击杀 &a全员金币翻倍");
        lore.add("");
        lore.add("&6&l排名奖励");
        lore.add("&f第1-3名: &62000金币 &f+ &b2声望");
        lore.add("&f第4-20名: &6500金币 &f+ &b1声望");
        lore.add("&f其他玩家: &6100金币");
        return lore;
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 20 : 50;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Void> prepare() {
        var config = ThePit.getInstance().getPitConfig();
        Location middle = java.util.Objects.requireNonNull(config.getRagePitMiddle(), "未配置疯狂天坑中心").clone();
        Location door = java.util.Objects.requireNonNull(config.getPitDoorCenter(), "未配置天坑门").clone();
        int radius = config.getRagePitRadius(), height = config.getRagePitHeight();
        if (radius <= 0 || height <= 0 || config.getSpawnLocations().isEmpty()) throw new IllegalStateException("疯狂天坑尺寸/出生点配置无效");
        var wallWorld = new BukkitWorld(java.util.Objects.requireNonNull(middle.getWorld()));
        var doorWorld = new BukkitWorld(java.util.Objects.requireNonNull(door.getWorld()));
        var center = BlockVector3.at(middle.getBlockX(), middle.getBlockY(), middle.getBlockZ());
        var doorAt = BlockVector3.at(door.getBlockX(), door.getBlockY(), door.getBlockZ());
        var schematic = FaweUtils.requireSchematic("pit_door.schem");
        BlockRestoreManager.INSTANCE.forceRestoreAll();
        return mapEdits.prepare(batch -> {
            batch.edit(wallWorld, edit -> edit.makeCylinder(center, new BlockPattern(BlockTypes.GLASS.getDefaultState().toBaseBlock()), radius, height, false));
            batch.edit(doorWorld, edit -> FaweUtils.pasteSchematicOrThrow(edit, doorAt, schematic, true));
        });
    }

    @Override public java.util.concurrent.CompletableFuture<Void> restore() { return mapEdits.restore(); }

    @Override
    public void onAbort() {
        HandlerList.unregisterAll(this);
        if (runnable != null) { runnable.cancel(); runnable = null; }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);
            if (profile.getExtraMaxHealth().remove("rage_pit") != null) player.setMaxHealth(profile.getMaxHealth());
            InventoryUtil.removeItemWithInternalName(player, "angry_potato");
        }
        damageMap.clear();
        killed = 0;
    }

    @Override
    public void onActive() {
        damageMap.clear();
        killed = 0;
        Bukkit.getPluginManager()
                .registerEvents(this, ThePit.getInstance());
        //set Player's max health and teleport to spawn
        for (Player player : Bukkit.getOnlinePlayers()) {

            PlayerProfile profile = Util.getPitProfile(player);
            PlayerProfile.callHistoricalEvents(player, profile, getEventName(), 1);
            profile.getExtraMaxHealth().put("rage_pit", 20.0);
            player.setMaxHealth(profile.getMaxHealth());
            player.setHealth(player.getMaxHealth());
            Location location = ThePit.getInstance().getPitConfig()
                    .getSpawnLocations()
                    .get(RandomUtil.random.nextInt(ThePit.getInstance().getPitConfig().getSpawnLocations().size()));
            player.teleport(location);
            profile.setBounty(0);
            profile.setStreakKills(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.5F);
            player.sendMessage(CC.translate("&5&l大型事件! &6&l疯狂天坑 &7事件开始!"));
        }

        timer = new Cooldown(5, TimeUnit.MINUTES);

        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (timer.hasExpired()) {
                    ThePit.getInstance()
                            .getEventFactory()
                            .inactiveEvent(RagePitEvents.this);
                    return;
                }

                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setTitle(CC.translate("&5&l大型事件! &6&l" + getEventName() + " &7将在 &a" + TimeUtil.millisToTimer(timer.getRemaining()) + "&7 后结束!"));
                ThePit.getInstance()
                        .getBossBar()
                        .getBossBar()
                        .setProgress(timer.getRemaining() / (1000 * 60 * 5D));
            }
        };

        MajorEventTasks.track(this, runnable.runTaskTimer(ThePit.getInstance(), 20, 20));
    }

    @EventHandler
    public void onPlayerKill(PitKillEvent event) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            killed++;
        }
    }

    @EventHandler
    public void onPlayerDamage(PitUltimatelyDamageEvent event) {
        if (event.getAttacker() != null && event.getUltimatelyDamage() > 0 && "rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            damageMap.putIfAbsent(event.getAttacker().getUniqueId(), new DamageData(event.getAttacker().getUniqueId(), 0));
            DamageData damageData = damageMap.get(event.getAttacker().getUniqueId());
            damageData.setDamage(damageData.getDamage() + event.getUltimatelyDamage());
        }
    }

    @Override
    public void onInactive() {
        HandlerList.unregisterAll(this);

        //还原玩家血量,移除活动道具
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getExtraMaxHealth().remove(getEventInternalName());

            profile.getExtraMaxHealth().remove("rage_pit");
            player.setMaxHealth(PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getExtraMaxHealthValue() + 20.0);
            InventoryUtil.removeItemWithInternalName(player, "angry_potato");
        }
        //取消Runnable
        if (runnable != null) { runnable.cancel(); runnable = null; }
        //还原BossBar
        ThePit.getInstance()
                .getBossBar()
                .getBossBar()
                .setTitle("");
        //获取伤害前3名玩家
        HashSet<Map.Entry<UUID, DamageData>> entry = new HashSet<>(damageMap.entrySet());
        List<DamageData> collect = entry
                .stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingDouble(DamageData::getDamage).reversed())
                .collect(Collectors.toList());
        //结算消息
        int rank = 1;
        for (DamageData data : collect) {
            Player player = Bukkit.getPlayer(data.getUuid());
            if (player == null || !player.isOnline()) {
                continue;
            }

            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            player.sendMessage(CC.GOLD + CC.CHAT_BAR);
            player.sendMessage(CC.translate("&6&l天坑事件结束: &c" + this.getEventName()));
            double rewardCoins = 0;
            int rewardRenown = 0;
            if (rank <= 3) {
                rewardCoins += 2000;
                rewardRenown += 2;
            } else if (rank <= 20) {
                rewardCoins += 500;
                rewardRenown += 1;
            } else {
                rewardCoins += 100;
            }
            if (killed >= 600) {
                rewardCoins = 2 * rewardCoins;
            }
            if (ThePit.getInstance().getPitConfig().isGenesisEnable() && profile.getGenesisData().getTier() >= 5 && rewardRenown > 0) {
                rewardRenown++;
            }
            int enchantBoostLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "paparazzi_enchant");
            if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                enchantBoostLevel = 0;
            }
            if (enchantBoostLevel > 0) {
                rewardCoins += 0.5 * enchantBoostLevel * rewardCoins;
                rewardRenown += Math.floor(0.5 * enchantBoostLevel * rewardRenown);
                MythicLeggingsItem mythicLeggings = new MythicLeggingsItem();
                mythicLeggings.loadFromItemStack(player.getInventory().getLeggings());
                if (mythicLeggings.isEnchanted()) {
                    if (mythicLeggings.getMaxLive() > 0 && mythicLeggings.getLive() <= 2) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    } else {
                        mythicLeggings.setLive(mythicLeggings.getLive() - 2);
                        player.getInventory().setLeggings(mythicLeggings.toItemStack());
                    }
                }
            }
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.SELF_CONFIDENCE)) {
                if (rank <= 5) {
                    rewardCoins += 5000;
                } else if (rank <= 10) {
                    rewardCoins += 2500;
                } else if (rank <= 15) {
                    rewardCoins += 1000;
                }
            }
            if (profile.isBetterPlayer()) {
                rewardCoins += rewardCoins / 2;
                rewardRenown += 1;
            }
            profile.grindCoins(rewardCoins);
            profile.setCoins(profile.getCoins() + rewardCoins);
            profile.changeRenown(profile.getRenown() + rewardRenown);
            profile.getEventData().addEventrenown(rewardRenown);
            profile.kingsQuestsData.checkUpdate();
            if (profile.kingsQuestsData.getAccepted()) {
                if (!profile.kingsQuestsData.getCompleted()) {
                    profile.kingsQuestsData.setCollectedRenown(profile.kingsQuestsData.getCollectedRenown() + rewardRenown);
                }
            }

            player.sendMessage(CC.translate("&6你的奖励: &6+" + rewardCoins + "硬币 &e+" + rewardRenown + "声望"));
            if (killed >= 600) {
                player.sendMessage(CC.translate("&6&l全局奖励: &a&l成功! &7所有人获得的金币翻倍!"));
            } else {
                player.sendMessage(CC.translate("&6&l全局奖励: &c&l失败! &7总共击败了" + killed + "名玩家"));
            }
            int damage = (int) (data.getDamage() / 2);
            player.sendMessage(CC.translate("&6&l你: &c造成" + damage + "❤ &7(排名#" + getDamageRank(player) + ")"));
            if (collect.size() >= 3) {
                player.sendMessage(CC.translate("&6顶级玩家: "));
                for (int i = 0; i < 3; i++) {
                    Player top = Bukkit.getPlayer(collect.get(i).getUuid());
                    if (top != null && top.isOnline()) {
                        PlayerProfile topProfile = PlayerProfile.getPlayerProfileByUuid(top.getUniqueId());

                        int d = (int) (collect.get(i).getDamage() / 2);
                        player.sendMessage(CC.translate(" &e&l#" + (i + 1) + " " + topProfile.getFormattedName() + " &e造成了 &c" + d + "❤ 伤害"));
                    }
                }
            }

            player.sendMessage(CC.GOLD + CC.CHAT_BAR);

            if (damageMap.size() >= 50 && getDamageRank(player) > 0 && getDamageRank(player) <= 3) {
                new RagePitTopMedal().addProgress(profile, 1);
            }


            rank++;
        }

        damageMap.clear();
        killed = 0;
    }

    @Data
    @AllArgsConstructor
    public static class DamageData {
        private UUID uuid;
        private double damage;
    }
}
