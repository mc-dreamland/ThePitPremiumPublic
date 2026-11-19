package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.bot.BotPerkEffects;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.activity.newyear.NewYearData;
import cn.charlotte.pit.commands.admin.Debug;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.goldHistory;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.sub.*;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.EnchantmentFactor;
import cn.charlotte.pit.enchantment.type.op.RealManEnchant;
import cn.charlotte.pit.event.*;
import cn.charlotte.pit.events.EventFactory;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.events.impl.KingOfTheHillEvents;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.LuckyChestplate;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.manager.KillStreakRewards;
import cn.charlotte.pit.movement.PlayerMoveHandler;
import cn.charlotte.pit.npc.NpcFactory;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.parm.listener.IPlayerBeKilledByEntity;
import cn.charlotte.pit.parm.listener.IPlayerKilledEntity;
import cn.charlotte.pit.parm.listener.IPlayerRespawn;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.runnable.ProfileLoadRunnable;
import cn.charlotte.pit.util.*;
import cn.charlotte.pit.util.chat.*;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.rank.RankUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import com.andan.avipsystem.api.VipSystemAPI;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.world.item.ArmorItem;
import org.bukkit.*;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.bukkit.inventory.EquipmentSlot.*;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 11:16
 */
@Slf4j
@AutoRegister
public class CombatListener implements Listener {

    public static final DecimalFormat numFormat = new DecimalFormat("0.000");
    private static final Random random = new Random();
    private static final DecimalFormat intFormat = new DecimalFormat("0");
    private static final Map<UUID, Location> loc = new ConcurrentHashMap<>();
    @Getter
    private static final HashMap<UUID, Pair<UUID, Integer>> aim = new HashMap<>();
    private static final Map<Location, BukkitTask> xpBanners = new HashMap<>();
    private static final Map<Location, UUID> bannerOwners = new HashMap<>();
    private static final Map<UUID, Boolean> godMap = new HashMap<>();
    private static final HashSet<UUID> bedrockPlayers = new HashSet<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    public static CombatListener INSTANCE;
    public static double eventBoost = ThePit.getInstance().getPitConfig().getBooster(); //1.0 to close
    @Getter
    @Setter
    static boolean onNight = false;
    static String boostString = " &6(限时加成x" + eventBoost + "倍奖励)";
    @Getter
    @Setter
    private static boolean isBedrockRoom = false;

    public CombatListener() {
        INSTANCE = this;
    }

    public static void setloc(Player player, Location Location) {
        loc.remove(player.getUniqueId());
        loc.put(player.getUniqueId(), Location);
    }


    public static void hidegod(Player player) {
        UUID uuid = player.getUniqueId();
        if (godMap.containsKey(uuid)) {
            godMap.remove(uuid);
        } else {
            godMap.put(uuid, true);
        }
    }

    public static void handleDamage(Entity damagerEntity, double finalDamage, Player player, Player damager, PlayerProfile playerProfile, PlayerProfile damagerProfile, double damage, boolean isShoot) {
        if (PitBotManager.blockedPvp(damager, player) || PitBotManager.isBot(damagerEntity)) return;
        playerProfile.setCombatTimer(new Cooldown((playerProfile.getBounty() == 0 ? 24 : 48), TimeUnit.SECONDS));
        damagerProfile.setCombatTimer(new Cooldown((damagerProfile.getBounty() == 0 ? 24 : 48), TimeUnit.SECONDS));
//        playerProfile.pushdamage(player.getUniqueId());
//        damagerProfile.pushdamage(damager.getUniqueId());
        playerProfile.setHit(playerProfile.getHit() + 1);
        Map<UUID, DamageData> map = playerProfile.getDamageMap();
        map.putIfAbsent(damager.getUniqueId(), new DamageData(damager.getUniqueId()));

        DamageData damageData = map.get(damager.getUniqueId());

        if (damageData.getTimer().hasExpired()) {
            damageData.setDamage(damage);
        } else {
            damageData.setDamage(damageData.getDamage() + damage);
        }
        damageData.setTimer(new Cooldown(30, TimeUnit.SECONDS));

        playerProfile.getDamageMap().put(damager.getUniqueId(), damageData);

        playerProfile.setHurtDamage((long) (playerProfile.getHurtDamage() + damage));
        if (isShoot) {
            if (damagerEntity instanceof Arrow) {
                playerProfile.setBowHurtDamage((long) (playerProfile.getBowHurtDamage() + damage));
                damagerProfile.setBowHit(damagerProfile.getBowHit() + 1);
                damagerProfile.setArrowTotalDamage((long) (damagerProfile.getArrowTotalDamage() + damage));
            } else if (damagerEntity instanceof FishHook) {
                damagerProfile.setRodHit(damagerProfile.getRodHit() + 1);
            }
        } else {
            playerProfile.setMeleeHurtDamage((long) (playerProfile.getMeleeHurtDamage() + damage));
            damagerProfile.setMeleeHit(damagerProfile.getMeleeHit() + 1);
            damagerProfile.setMeleeTotalDamage((long) (damagerProfile.getMeleeTotalDamage() + damage));
        }
        damagerProfile.setTotalDamage((long) (damagerProfile.getTotalDamage() + damage));


        int absorptionHearts = (int) (player.getAbsorptionAmount() / 2);
        int totalHearts = (int) player.getMaxHealth() / 2;
        int nowHearts = (int) player.getHealth() / 2;
        int damageHearts = (int) damage / 2;

        if (damagerProfile.getPlayerOption().getBarPriority() != PlayerOption.BarPriority.ENCHANT_ONLY && !NpcFactory.getRegistry().isNPC((Entity) player)) {
            int i;
            StringBuilder builder = new StringBuilder();
            builder.append(RankUtil.getPlayerColoredName(player.getUniqueId()));
            if (PlayerUtil.isVenom(player)) {
                builder.append(" &2");
            } else {
                builder.append(" &4");
            }
            for (i = 0; i < nowHearts; ++i) {
                builder.append("❤");
            }
            if (absorptionHearts > 0) {
                if (absorptionHearts < 25) {
                    builder.append("&e");
                    for (i = 0; i < absorptionHearts; ++i) {
                        builder.append("❤");
                    }
                } else {
                    builder.append("&e❤");
                    builder.append("...").append(absorptionHearts - 2).append("...❤");
                }
            }
            if (PlayerUtil.isVenom(player)) {
                builder.append("&a");
            } else {
                builder.append("&c");
            }
            for (i = 0; i < damageHearts; ++i) {
                builder.append("❤");
            }
            builder.append("&7");
            int heats = totalHearts - nowHearts - damageHearts;
            builder.append("❤".repeat(Math.max(0, heats)));
            if (PlayerUtil.isVenom(damager)) {
                long duration_damager = damager.getMetadata("combo_venom").getFirst().asLong() - System.currentTimeMillis();
                builder.append(" &7| &c&l沉默&b&l(我) &d&l")
                        .append(TimeUtil.millisToSeconds(duration_damager))
                        .append("秒&r");
            }
            if (PlayerUtil.isVenom(player)) {
                long duration_player = player.getMetadata("combo_venom").getFirst().asLong() - System.currentTimeMillis();
                builder.append(" &7| &c&l沉默&b&l(敌) &d&l")
                        .append(TimeUtil.millisToSeconds(duration_player))
                        .append("秒&r");
            }
            if (PerksPrestigeEnum.RAW_NUMBERS.getPerk().hasUnlock(damager)) {
                builder.append(" &c").append(numFormat.format(finalDamage)).append("HP");
            }
            ActionBarUtil.sendActionBar(damager, String.valueOf(builder));
//            int bartime = playerProfile.getPlayerOption().getBartime();
//            if(bartime > 1){
//                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> ActionBarUtil.sendActionBar(damager, String.valueOf(builder)), 10L * bartime);
//            }
            player.setMetadata("showing_damage_data", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis()));
        }
//        if (player.hasMetadata("backing")) {
//            player.sendMessage(CC.translate("&c回城被取消."));
//            player.removeMetadata("backing", ThePit.getInstance());
//        }


        //handle kill recap - start
        String damagerName = damagerProfile.getFormattedName();
        String playerName = playerProfile.getFormattedName();

        KillRecap.DamageData damagerData = new KillRecap.DamageData();
        damagerData.setDisplayName(playerName);
        damagerData.setAttack(true);
        damagerData.setMelee(!isShoot);
        damagerData.setAfterHealth(Math.max(player.getHealth() - finalDamage, 0));
        damagerData.setUsedItem(damager.getInventory().getItemInMainHand());
        damagerData.setTimer(new Cooldown(10, TimeUnit.SECONDS));


        List<KillRecap.DamageData> remove = damagerProfile.getKillRecap()
                .getDamageLogs()
                .stream()
                .filter(data -> data.getTimer().hasExpired())
                .toList();
        damagerProfile.getKillRecap()
                .getDamageLogs()
                .removeAll(remove);
        damagerProfile.getKillRecap()
                .getDamageLogs()
                .add(damagerData);

        KillRecap.DamageData playerData = new KillRecap.DamageData();
        playerData.setDisplayName(damagerName);
        playerData.setAttack(false);
        playerData.setMelee(!isShoot);
        playerData.setAfterHealth(Math.max(player.getHealth() - finalDamage, 0));
        playerData.setUsedItem(damager.getInventory().getItemInMainHand());
        playerData.setTimer(new Cooldown(10, TimeUnit.SECONDS));
        playerData.setDamage(finalDamage);

        List<KillRecap.DamageData> shouldRemove = playerProfile.getKillRecap()
                .getDamageLogs()
                .stream()
                .filter(data -> data.getTimer().hasExpired())
                .toList();

        List<KillRecap.DamageData> damageLogs = playerProfile.getKillRecap()
                .getDamageLogs();
        damageLogs.removeAll(shouldRemove);
        damageLogs.add(playerData);
        //handle kill recap - end
    }

    public static void handleKill(Player killer, PlayerProfile killerProfile, LivingEntity beKilled, PlayerProfile beKilledProfile) {
        if (PitBotManager.isBot(beKilled)) return;
        try {
            boolean isNight = isNight();


            final String coloredName = RankUtil.getPlayerColoredName(beKilled.getUniqueId());

            if (killerProfile.getPlayerOption().getBarPriority() != PlayerOption.BarPriority.ENCHANT_ONLY) {
                ActionBarUtil.sendActionBar(killer, " &a&l击杀! " + coloredName);
            }

            //process drop armor - start
            //fixme: here is an error about async
            if (beKilled instanceof Player) {
                //只有玩家需要掉落装备
                handleItemDrop(killerProfile, killer, (Player) beKilled);

                // 处理刷头，记录信息
                killerProfile.pushVictim(beKilled.getUniqueId());
                killerProfile.pushSlowVictim(beKilled.getUniqueId(), killerProfile, beKilledProfile);
                beKilledProfile.pushKiller(killer.getUniqueId());
            }
            //process drop armor - end
            int slowCount = killerProfile.countSlowVictim(beKilled.getUniqueId());
            int count = killerProfile.countVictim(beKilled.getUniqueId(), 5 * 60 * 1000L);
            //在内部方法中判断是否宵禁
            if (slowCount < 3 && count < 6 && !isNight && !killerProfile.isMaybeBot()) {
                killerProfile.setStreakKills(killerProfile.getStreakKills() + 1);
            }
            killerProfile.setKills(killerProfile.getKills() + 1);

            killerProfile.kingsQuestsData.checkUpdate();

            ResourceManager resourceManager = new ResourceManager();

            resourceManager.addBaseExp(5, "基础 §b经验");
            resourceManager.addBaseGold(10, "基础 §6金币");
            resourceManager.addBaseExp(killerProfile.getPrestige() * 0.5, "转生加成");
            resourceManager.addBaseGold(killerProfile.getPrestige() * 0.5, "转生加成");
            Location deathLoc = beKilled.getLocation();

            applyAreaRewardModifiers(killer, killerProfile, deathLoc, resourceManager);

            // 计算是否“被欺凌/刷人头”
            final long WINDOW_MS = 5 * 60 * 1000L; //2分钟限制
            int N;
            Location ragePitMiddle = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
            // 如果在坑内，适当放开阈值
            // 被击杀玩家如果超过60秒没造成任何伤害，则降低阈值使其更容易被处罚
            if (killer.getLocation().distance(ragePitMiddle) < 20) {
                N = System.currentTimeMillis() - beKilledProfile.getLastDamageAt() >= 60 ? 2 : 5;
            } else {
                N = System.currentTimeMillis() - beKilledProfile.getLastDamageAt() >= 60 ? 2 : 3;
            }
            boolean isBullying = false, isBoosting = false;
            int bullyCount = 0, boostCount = 0;

            if (beKilled instanceof Player) {
                bullyCount = beKilledProfile.countKiller(killer.getUniqueId(), WINDOW_MS);
                boostCount = killerProfile.countVictim(beKilled.getUniqueId(), WINDOW_MS);
                isBullying = bullyCount > N || beKilledProfile.isMaybeBot();
                isBoosting = boostCount > N || beKilledProfile.isMaybeBot();
            }

            if (killerProfile.kingsQuestsData.getAccepted() && !beKilledProfile.isMaybeBot() && !isBullying && !isBoosting) {
                if (!killerProfile.kingsQuestsData.getCompleted()) {
                    killerProfile.kingsQuestsData.setKilledPlayer(killerProfile.kingsQuestsData.getKilledPlayer() + 1);
                }
            }

            // 计算奖励前先应用惩罚（可按超出次数动态）
            if (isBullying) {
                double factor = beKilledProfile.isMaybeBot() ? 0.01 : penaltyFactor(bullyCount, N); // 如下函数
                if (factor < 1) {
                    resourceManager.addGoldModifier(factor, (beKilledProfile.isMaybeBot() ? "§4天外来客" : "&c以强凌弱"));
                    resourceManager.addExpModifier(factor, (beKilledProfile.isMaybeBot() ? "§4天外来客" : "&c以强凌弱"));
                }
            }
            if (isBoosting) {
                double factor = beKilledProfile.isMaybeBot() ? 0.01 : penaltyFactor(boostCount, N); // 如下函数
                if (factor < 1) {
                    resourceManager.addGoldModifier(factor, (beKilledProfile.isMaybeBot() ? "§4天外来客" : "&c以强凌弱"));
                    resourceManager.addExpModifier(factor, (beKilledProfile.isMaybeBot() ? "§4天外来客" : "&c以强凌弱"));
                }
            }


            applyPersonalRewardModifiers(killer, killerProfile, resourceManager);

            if (!beKilledProfile.isMaybeBot()) {
                handleGameEffect(killerProfile, killer, beKilled, resourceManager);
            }

            // 刷头或欺凌则不计入赏金
            if (!isNight && !isBullying && !isBoosting && !beKilledProfile.isMaybeBot()) {
                handleAddBounty(killerProfile, killer);
            }

            final KillRecap killRecap = initializationKillRecap(beKilledProfile, killerProfile, killer, resourceManager);

            if (!beKilledProfile.isMaybeBot()) {
                calculationKillReward(killerProfile, beKilledProfile, killRecap, killer, resourceManager);
            }

            //calculation kill reward - end

            handleKillBounty(killerProfile, beKilledProfile, resourceManager);
            applyCurfewRewardModifiers(resourceManager, isNight);

            PitKillEvent event = new PitKillEvent(killer, beKilled, resourceManager, new LinkedList<>());
            event.callEvent();

            resourceManager = event.getResourceManager();

            double totalCoins = resourceManager.calculateGold();
            double totalXp = resourceManager.calculateEXP();


            if (killerProfile.getLevel() < 120) {
                killerProfile.setExperience(killerProfile.getExperience() + totalXp);
            } else {
                totalXp = 0;
            }

            killerProfile.setCoins(killerProfile.getCoins() + totalCoins);
            killerProfile.grindCoins(totalCoins);
            killerProfile.applyExperienceToPlayer(killer);

            //process quest - start
            if (beKilled instanceof Player) {
                handleQuest(killerProfile, (Player) beKilled);
            }
            //process quest - end

            handleGivePlayerKillReward(killer);
            //golden head and vampire - end

            if (!isNight && !isBullying && !isBoosting) {
                //enchant - start
                handleMythicItemDrop(killerProfile, killer, beKilled);
                //enchant - end
                handleMysteryitemDrop(killerProfile, killer);

                handleCherryDrop(killer, killerProfile);
            }

            //refresh killer highest streak kills
            if (killerProfile.getHighestStreaks() < killerProfile.getStreakKills()) {
                killerProfile.setHighestStreaks((int) killerProfile.getStreakKills());
            }

            //BoardCast msg - start
            handleBoardCastMessage(killerProfile, beKilledProfile, killer, beKilled, totalCoins, totalXp, resourceManager, event.getMsg());
            //BoardCast msg - end

            if (beKilled instanceof Player) {
                killRecap.completeLog((Player) beKilled);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /** Currency/effects only: never call handleKill, PitKillEvent, progression, bounty or item-drop routines. */
    public static ResourceManager createBotKillReward(Player killer, PlayerProfile profile, LivingEntity bot,
                                                      double baseExperience, double baseCoins, boolean killingBlow, boolean streakAdvanced) {
        ResourceManager resources = new ResourceManager();
        // Keep the configured base pools instead of PvP base/prestige; bounded ordinary streak rewards are added below.
        resources.addBaseExp(baseExperience, "陪练基础经验");
        resources.addBaseGold(baseCoins, "陪练基础金币");
        applyAreaRewardModifiers(killer, profile, bot.getLocation(), resources);
        applyPersonalRewardModifiers(killer, profile, resources);
        // Never repeat a 10/20/30 milestone just because the Bot counter has reached its cap.
        if (streakAdvanced) KillStreakRewards.apply(resources, profile.getStreakKills(),
                PlayerUtil.hasEquipPerk(killer, PerksBoostsEnum.STREAKER.getPerk()), false);
        if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            Set<String> appliedPerks = new HashSet<>();
            for (TypedPerk<?> perk : profile.getActivePerks()) {
                if (!appliedPerks.add(perk.getInternalPerkName())) continue;
                try {
                    BotPerkEffects.apply(perk, perk.getLevel(profile), killer, bot, resources, killingBlow);
                } catch (Exception ex) {
                    log.warn("Bot 击杀天赋效果执行失败: " + perk.getInternalPerkName(), ex);
                }
            }
        }
        applyEquipmentRewardModifiers(killer, profile, resources);
        applyCurfewRewardModifiers(resources, isNight());
        if (killingBlow) handleGivePlayerKillReward(killer);
        return resources;
    }

    private static void applyAreaRewardModifiers(Player killer, PlayerProfile killerProfile, Location deathLoc,
                                                  ResourceManager resourceManager) {
        Location loc = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
        INormalEvent activeEpicEvent = ThePit.getInstance().getEventFactory().getActiveNormalEvent();
        if (activeEpicEvent != null) {
            if ("experience_blessed_land".equals(activeEpicEvent.getEventInternalName())) {
                Location location = killer.getLocation();
                if (location.distance(loc) < 10) {
                    resourceManager.addExpModifier(killerProfile.prestige >= 10 ? 1.5 : 2.0, "经验福地事件");
                }
            }
            if ("koth".equals(activeEpicEvent.getEventInternalName())) {
                if (KingOfTheHillEvents.isRegion(killer)) {
                    resourceManager.addGoldModifier(4.0, "&b占山&e为王");
                    resourceManager.addExpModifier(4.0, "&b占山&e为王");
                }
            }
        }
        for (Map.Entry<Location, UUID> entry : bannerOwners.entrySet()) {
            Location bannerLoc = entry.getKey();
            if (bannerLoc.getWorld().equals(deathLoc.getWorld()) &&
                    bannerLoc.distance(deathLoc) <= 12) {
                if (killerProfile.prestige <= 5) {
                    resourceManager.addExpModifier(1.5, "&a增益旗帜加成");
                    resourceManager.addGoldModifier(1.5, "&a增益旗帜加成");
                } else {
                    resourceManager.addExpModifier(1.25, "&a增益旗帜加成");
                    resourceManager.addGoldModifier(1.25, "&a增益旗帜加成");
                }
                break;
            }
        }
    }

    private static void applyPersonalRewardModifiers(Player killer, PlayerProfile killerProfile,
                                                      ResourceManager resourceManager) {
        int vipLevel = VipSystemAPI.getVipLevel(killer);
        if (Debug.getEnableVipBuff() > -1) {
            vipLevel = Debug.getEnableVipBuff();
        }
        resourceManager.addExpModifier(1 + vipLevel * 0.05d, "VIP加成");
        resourceManager.addGoldModifier(1 + vipLevel * 0.05d, "VIP加成");
        if (killerProfile.hasExtraExpBonus()) {
            resourceManager.addExpModifier(1 + 0.35, "额外经验加成");
        }
        if (killerProfile.hasExtraGoldBonus()) {
            resourceManager.addGoldModifier(1 + 0.35, "额外金币加成");
        }
        if (PerksBoostsEnum.CONGLOMERATE.getPerk().hasEquip(killerProfile)) {
            resourceManager.addGoldModifier(1 + 0.2, "财团分红");
            resourceManager.addExpModifier(0, "财团分红");
            if (killerProfile.isDebug()) {
                killer.sendMessage("已装备财团分红天赋");
            }
        }
    }

    private static void applyEquipmentRewardModifiers(Player killer, PlayerProfile killerProfile,
                                                       ResourceManager resourceManager) {
        //Genesis Boost Start
        if (killerProfile.getGenesisData().getTeam() == GenesisTeam.ANGEL && killerProfile.getGenesisData().getBoostTier() > 0) {
            resourceManager.addExpModifier(1 + 0.05 * killerProfile.getGenesisData().getBoostTier(), "Genesis Boost");
        }
        if (killerProfile.getGenesisData().getTeam() == GenesisTeam.DEMON && killerProfile.getGenesisData().getBoostTier() > 0) {
            resourceManager.addGoldModifier(1 + 0.05 * killerProfile.getGenesisData().getBoostTier(), "Genesis Boost");
        }
        //我把国王头盔写到这里了,写在国王头盔本身的加成里比较烦
        ItemStack helmet = killer.getInventory().getHelmet();
        if (helmet != null && "kings_helmet".equals(ItemUtil.getInternalName(helmet))) {
            resourceManager.addExpModifier(1.1, "国王的王冠");
            resourceManager.addGoldModifier(1.1, "国王的王冠");
        }
    }

    private static void applyCurfewRewardModifiers(ResourceManager resources, boolean night) {
        if (night) {
            resources.addGoldModifier(0.01, "宵禁");
            resources.addExpModifier(0.01, "宵禁");
        }
    }

    // 惩罚函数（平滑递增并夹到 1%）
    private static double penaltyFactor(int count, int N) {
        if (count <= N) return 1.0; // 前 N 次无惩罚

        int over = count - N; // 超出的次数
        return switch (over) {
            case 1 -> 0.8;
            case 2 -> 0.6;
            case 3 -> 0.4;
            case 4 -> 0.2;
            default -> 0.1;
        };
    }

    private static boolean hasPremiumItem(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            if (itemStack == null) {
                continue;
            }
            if (itemStack.getType().name().contains("LEATHER") || itemStack.getType().name().contains("DIAMOND")) {
                return true;
            }
        }

        return false;
    }

    public static void handlePlayerDeath(Player player, Player killer, boolean shouldRespawn) {
        PitDeathEvent pitDeathEvent = new PitDeathEvent(player, killer == null);
        pitDeathEvent.callEvent();

        PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (killer != null) {
            Player reallyKiller = Bukkit.getPlayer(killer.getUniqueId());
            if (reallyKiller != null) {
                PlayerProfile killerProfile = PlayerProfile.getPlayerProfileByUuid(killer.getUniqueId());
                handleKill(killer, killerProfile, player, playerProfile);
            }
        } else {
            EntityDamageEvent damageEvent = player.getLastDamageCause();
            if (damageEvent instanceof EntityDamageByEntityEvent entityEvent) {
                if (entityEvent.getDamager() instanceof Player) {
                    killer = (Player) entityEvent.getDamager();
                    PlayerProfile killerProfile = PlayerProfile.getPlayerProfileByUuid(killer.getUniqueId());
                    handleKill(killer, killerProfile, player, playerProfile);
                } else if (entityEvent.getDamager() instanceof TNTPrimed tntPrimed) {
                    if (!tntPrimed.getMetadata("shooter").isEmpty()) {
                        UUID uuid = UUID.fromString(tntPrimed.getMetadata("shooter").getFirst().asString());
                        if (uuid.equals(player.getUniqueId())) {
                            return;
                        }
                        killer = Bukkit.getPlayer(uuid);
                        if (killer != null && killer.isOnline()) {
                            PlayerProfile killerProfile = PlayerProfile.getPlayerProfileByUuid(uuid);
                            handleKill(killer, killerProfile, player, playerProfile);
                        }
                    }
                }
            }
        }

        final Player finalKiller = killer;

        double respawnTime = playerProfile.getRespawnTime();

        if (!shouldRespawn) {// player quit
            PlayerUtil.clearPlayer(player, true, false);
        }

        double mythicProtectChance = 0;

        int divineMiracleEnchantLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "divine_miracle_enchant");
        if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
            divineMiracleEnchantLevel = 0;
        }
        if (divineMiracleEnchantLevel > 0) {
            mythicProtectChance += 0.15 * divineMiracleEnchantLevel;
        }
        if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.DIVINE_INTERVENTION)) {
            mythicProtectChance += 0.05 * PlayerUtil.getPlayerUnlockedPerkLevel(player, PerksPrestigeEnum.DIVINE_INTERVENTION);
        }

        //Promotion effect
        if (PlayerUtil.hasEquipPerk(player, PerksBoostsEnum.ASSISTANT_STREAKER.getPerk()) && PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.PROMOTION)) {
            if (PlayerUtil.isSupperStreak(playerProfile, player)) {
                mythicProtectChance = 1;
            }
        }
        //Funky Feather Item
        PitItemLiveDropEvent pitItemLiveDropEvent = new PitItemLiveDropEvent(mythicProtectChance);
        pitItemLiveDropEvent.callEvent();
        mythicProtectChance = pitItemLiveDropEvent.getChance();
        if (mythicProtectChance < 1 && !pitItemLiveDropEvent.isCancelled() && Util.hasIMythicItem(player)) {
            for (int i = 0; i < 9; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || !item.getType().equals(Material.FEATHER)) {
                    continue;
                }
                if ("funky_feather".equals(ItemUtil.getInternalName(item))) {
                    if (item.getAmount() <= 1) {
                        player.getInventory().setItem(i, PitItems.AIR.toItemStack());
                    } else {
                        item.setAmount(item.getAmount() - 1);
                        player.getInventory().setItem(i, item);
                    }
                    mythicProtectChance = 1;
                    break;
                }
            }
            ItemStack item = player.getInventory().getItemInOffHand();
            if (mythicProtectChance < 1 && PitItems.FUNKY_FEATHER.isInternalNameEquals(item)) {
                if (item.getAmount() <= 1) {
                    player.getInventory().setItemInOffHand(PitItems.AIR.toItemStack());
                } else {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItemInOffHand(item);
                }
                mythicProtectChance = 1;

            }
        }

        boolean noProtect = !pitItemLiveDropEvent.isCancelled() && RandomUtil.hasSuccessfullyByChance(1 - mythicProtectChance);

        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem.getType() != Material.AIR) {
            final IMythicItem mythicCursorItem = Utils.getMythicItem(cursorItem);
            if (mythicCursorItem != null) {
                if (!noProtect) {
                    if ((mythicCursorItem instanceof LuckyChestplate) ||
                            mythicCursorItem.getEnchantments().containsKey(new RealManEnchant())) {
                        noProtect = true;
                    }
                }
                if (noProtect) {
                    player.setItemOnCursor(Utils.subtractLive(cursorItem));
                }
            }
        }

        if (!pitItemLiveDropEvent.isCancelled()) {
            for (int i = 0; i < 36; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;

                final IMythicItem mythicSwordItem = Utils.getMythicItem(item);
                if (mythicSwordItem == null) {
                    continue;
                }

                if (!noProtect) {
                    if ((mythicSwordItem instanceof LuckyChestplate) || mythicSwordItem.getEnchantments().containsKey(new RealManEnchant())) {
                        noProtect = true;
                    }
                }

                if (noProtect) {
                    player.getInventory().setItem(i, Utils.subtractLive(item));
                }
            }

            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (offHand.getType() != Material.AIR) {
                final IMythicItem mythicSwordItem = Utils.getMythicItem(offHand);
                if (mythicSwordItem != null) {
                    if (!noProtect) {
                        if ((mythicSwordItem instanceof LuckyChestplate) || mythicSwordItem.getEnchantments().containsKey(new RealManEnchant())) {
                            noProtect = true;
                        }
                    }
                    if (noProtect) {
                        player.getInventory().setItemInOffHand(Utils.subtractLive(offHand));
                    }
                }
            }
        }

        if (noProtect) {
            ItemStack helmet = player.getInventory().getHelmet();
            final IMythicItem mythicHelmet = Utils.getMythicItem(helmet);
            if (mythicHelmet != null) {
                player.getInventory().setHelmet(Utils.subtractLive(helmet));
            }
            ItemStack chestplate = player.getInventory().getChestplate();
            final IMythicItem mythicChest = Utils.getMythicItem(chestplate);
            if (mythicChest != null) {
                player.getInventory().setChestplate(Utils.subtractLive(chestplate));
            }
        }
        IMythicItem leggings = MythicUtil.getMythicItem(player.getInventory().getLeggings());
        if (leggings != null && (noProtect || leggings instanceof LuckyChestplate || leggings.getEnchantments().containsKey(new RealManEnchant()))) {
            player.getInventory().setLeggings(Utils.subtractLive(player.getInventory().getLeggings()));
        }
        if (noProtect) {
            ItemStack boots = player.getInventory().getBoots();
            IMythicItem mythicBoost = Utils.getMythicItem(boots);
            if (mythicBoost != null) {
                player.getInventory().setBoots(Utils.subtractLive(boots));
            }
        }

        if (!noProtect) {
            player.sendMessage(CC.translate("&d&l物品保护! &7由于一个天赋/附魔/物品提供的概率保护,本次死亡没有损失背包内神话物品生命."));
        }
        for (ItemStack itemStack : player.getInventory()) {
            if (ItemStack.empty().getType() == Material.HAY_BLOCK) {
                player.getInventory().remove(itemStack);
                continue;
            }
            if (ItemUtil.isDeathDrop(itemStack) || "dice_award".equals(ItemUtil.getInternalName(itemStack))) {
                player.getInventory().remove(itemStack);
            }
        }

        if (ItemUtil.isDeathDrop(player.getInventory().getHelmet()) || "dice_award".equals(ItemUtil.getInternalName(player.getInventory().getHelmet()))) {
            player.getInventory().setHelmet(PitItems.AIR.toItemStack());
        }
        if (ItemUtil.isDeathDrop(player.getInventory().getChestplate())) {
            player.getInventory().setChestplate(PitItems.AIR.toItemStack());
        }
        if (ItemUtil.isDeathDrop(player.getInventory().getLeggings())) {
            player.getInventory().setLeggings(PitItems.AIR.toItemStack());
        }
        if (ItemUtil.isDeathDrop(player.getInventory().getBoots())) {
            player.getInventory().setBoots(PitItems.AIR.toItemStack());
        }
        if (ItemUtil.isDeathDrop(player.getInventory().getItemInOffHand())) {
            player.getInventory().setItemInOffHand(PitItems.AIR.toItemStack());
        }

        //process assist - start
        try {
            Map<UUID, DamageData> damageMap = new HashMap<>(playerProfile.getDamageMap());
            List<DamageData> activeDamage = new ArrayList<>();
            for (Map.Entry<UUID, DamageData> entry : damageMap.entrySet()) {
                if (!entry.getValue().getTimer().hasExpired()) {
                    activeDamage.add(entry.getValue());
                }
            }

            double totalDamage = activeDamage
                    .stream()
                    .mapToDouble(DamageData::getDamage)
                    .sum();

            if (totalDamage > 0) {
                handleAssist(player, finalKiller, activeDamage, (long) totalDamage);
            }
        } catch (Exception e) {
            log.error("", e);
        }

        //process assist - end

        //save status - start
        playerProfile.getDamageMap().clear();
        playerProfile.setStreakKills(0);
        playerProfile.setInArena(false);
        playerProfile.setDeaths(playerProfile.getDeaths() + 1);
        playerProfile.setCombatTimer(new Cooldown(0));
        playerProfile.setBountyStreak(0);
        playerProfile.setStrengthNum(0);
        playerProfile.setStrengthTimer(new Cooldown(0));
        //save status - end

        InventoryUtil.supplyItems(player);

        if (!playerProfile.isTempInvUsing()) {
            playerProfile.setInventory(PlayerInv.fromPlayerInventory(player.getInventory()));
        }

        if (shouldRespawn) {
            // 设置玩家的初始状态
            player.setGameMode(GameMode.SPECTATOR);
            if (playerProfile.getRespawnTime() <= 1.5) {
                playerProfile.setRespawnTime(0.1d);
            }
            if (respawnTime > 0.1) {
                for (int i = 0; i < respawnTime; i++) {
                    final int remainingTime = (int) (respawnTime - i);

                    int delay = i * 20;
                    Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> TitleUtil.sendTitle(player, "&c你死了！", "&7将在 &6" + remainingTime + "秒 &7后复活", 5, 5, 20), delay);
                }

                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> doRespawn(player), (long) (respawnTime * 20L));
            } else {
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> doRespawn(player), 1L);
            }
        }
    }

    private static void doRespawn(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Location location;
        PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (loc.containsKey(player.getUniqueId())) {
            location = loc.get(player.getUniqueId());
            loc.remove(player.getUniqueId());
        } else {
            List<Location> spawnLocations = ThePit.getInstance().getPitConfig().getSpawnLocations();
            if (spawnLocations.isEmpty()) {
                location = Bukkit.getWorlds().getFirst().getSpawnLocation();
            } else {
                location = spawnLocations.get(random.nextInt(spawnLocations.size()));
            }
        }
        player.teleport(location);

        playerProfile.setInArena(false);

        player.setGameMode(GameMode.SURVIVAL);
        PlayerUtil.clearPlayer(player, false, false);

        playerProfile.applyExperienceToPlayer(player);
        TitleUtil.sendTitle(player, " ", " ", 5, 5, 20);

        for (TypedPerk<?> perk : playerProfile.getActivePerks()) {
            perk.handleRespawn(perk.getLevel(playerProfile), player);
        }

        EnchantmentFactor enchantmentFactor = ThePit.getInstance().getEnchantmentFactor();
        for (IPlayerRespawn ins : enchantmentFactor.getPlayerRespawns()) {
            AbstractEnchantment ench = (AbstractEnchantment) ins;

            int level = ench.getItemEnchantLevel(player.getInventory().getLeggings());
            if (level > 0) {
                ins.handleRespawn(level, player);
            }
        }
    }

    private static void handleAssist(Player player, Player killer, List<DamageData> damageData, long totalDamage) {
        PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        boolean night = isNight();

        for (DamageData data : damageData) {
            if (killer != null && killer.getUniqueId().equals(data.getUuid())) {
                continue;
            }
            if (totalDamage <= 1) {
                return;
            }
            KillRecap killRecap = playerProfile.getKillRecap();
            KillRecap.AssistData assistData = new KillRecap.AssistData();
            Player assistPlayer = Bukkit.getPlayer(data.getUuid());
            if (assistPlayer != null && assistPlayer.isOnline()) {

                PlayerProfile assistProfile = PlayerProfile.getPlayerProfileByUuid(data.getUuid());

                assistData.setDisplayName(assistProfile.getFormattedName());

                ResourceManager resourceManager = new ResourceManager();

                resourceManager.addBaseExp(5, "基础 §b经验 (助攻)");
                resourceManager.addBaseGold(10, "基础 §6金币 (助攻)");
                int vipLevel = VipSystemAPI.getVipLevel(assistPlayer);
                if (Debug.getEnableVipBuff() > -1) {
                    vipLevel = Debug.getEnableVipBuff();
                }
                if (vipLevel >= 3) {
                    resourceManager.addExpModifier(1 + vipLevel * 0.05d, "VIP加成");
                    resourceManager.addGoldModifier(1 + vipLevel * 0.05d, "VIP加成");
                }
                if (assistProfile.hasExtraExpBonus()) {
                    resourceManager.addExpModifier(1 + 0.35, "额外经验加成");
                }
                if (assistProfile.hasExtraGoldBonus()) {
                    resourceManager.addGoldModifier(1 + 0.35, "额外金币加成");
                }


                Location loc = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
                INormalEvent activeEpicEvent = ThePit.getInstance().getEventFactory().getActiveNormalEvent();
                if (activeEpicEvent != null) {
                    if ("experience_blessed_land".equals(activeEpicEvent.getEventInternalName())) {
                        Location location = assistPlayer.getLocation();
                        loc.setY(location.getY());
                        if (location.distance(loc) < 10) {
                            resourceManager.addExpModifier(assistProfile.prestige >= 10 ? 1.5 : 2.0, "经验福地事件");
                        }
                    }
                }
                final PitAssistEvent event = new PitAssistEvent(assistPlayer, player, resourceManager);
                event.callEvent();

                assistData.setBaseExp(10.0d + assistProfile.getPrestige() * 0.5);
                assistData.setBaseCoin(10.0d + assistProfile.getPrestige() * 0.5);

                AtomicDouble atomicDouble = new AtomicDouble();

                if (assistProfile.getStreakKills() != 0 && assistProfile.getStreakKills() % 10 == 0) {
//                    streakXp += Math.min(20, (assistProfile.getStreakKills() / 10d) * 5);
//                    totalCoins += Math.min(20, (assistProfile.getStreakKills() / 10d) * 5);
                    resourceManager.addExp(Math.min(20, (assistProfile.getStreakKills() / 10d) * 5), "连续助攻");
                    resourceManager.addGold(Math.min(20, (assistProfile.getStreakKills() / 10d) * 5), "连续助攻");

                    atomicDouble.getAndAdd((Math.min(20, (assistProfile.getStreakKills() / 10d) * 5)));
                }

                assistData.setStreakCoin(atomicDouble.get());
                assistData.setStreakExp(atomicDouble.get());
                atomicDouble.set(0);

                if (playerProfile.getLevel() < 100 && playerProfile.getLevel() - assistProfile.getLevel() >= 40) {
                    double levelDiff = playerProfile.getLevel() - assistProfile.getLevel();
                    double levelBonus = Math.min(levelDiff * 0.01, 0.6);
                    resourceManager.addExpModifier(1 + levelBonus, "等级压制");

                    atomicDouble.getAndAdd(levelDiff / 20d);
                }
                if (playerProfile.getPrestige() - assistProfile.getPrestige() >= 4) {
                    double prestigeDiff = playerProfile.getPrestige() - assistProfile.getPrestige();
                    double prestigeBonus = Math.min(prestigeDiff * 0.25, 2.0);
                    resourceManager.addExpModifier(1 + prestigeBonus, "精通等级压制");

                    atomicDouble.getAndAdd(prestigeDiff * 2);
                }

                assistData.setLevelDisparityExp(atomicDouble.get());
                assistData.setLevelDisparityCoin(atomicDouble.get());


                //process perk - start
                for (TypedPerk<?> perk : assistProfile.getActivePerks()) {
                    if (perk == null) {
                        continue;
                    }
                    perk.handlePlayerAssist(perk.getLevel(assistProfile), assistPlayer, player, player.getLastDamage(), player.getLastDamageCause().getFinalDamage(), resourceManager);
                }
                double percentage = data.getDamage() / totalDamage; //(0.0 ~ 1.0)
                if (percentage > 1) {
                    percentage = 1;
                }
                if (percentage < 0) {
                    percentage = 0;
                }

                assistData.setPercentage(percentage);
                assistData.setTotalCoin(resourceManager.calculateGold());
                assistData.setTotalExp(resourceManager.calculateEXP());
                killRecap.getAssistData().add(assistData);
                //与Hypixel一致
                int prestige = assistProfile.getPrestige();
                if (prestige > 3) {
                    if (prestige > 5) {
                        double pct = Math.max(0.25d, Math.min(percentage * 1.5, 0.8d));
                        resourceManager.addExpModifier(pct, "助攻伤害比例");
                        resourceManager.addGoldModifier(pct, "助攻伤害比例");
                    } else {
                        double pct = Math.max(0.3d, Math.min(percentage * 2, 0.8d));
                        resourceManager.addExpModifier(pct, "助攻伤害比例");
                        resourceManager.addGoldModifier(pct, "助攻伤害比例");
                    }
                } else {
                    double pct = Math.max(0.4d, Math.min(percentage * 3, 0.9d));
                    resourceManager.addExpModifier(pct, "助攻伤害比例");
                    resourceManager.addGoldModifier(pct, "助攻伤害比例");
                }
                //process perk - end
                final EventFactory factory = ThePit.getInstance().getEventFactory();
                if ("enchanting_bang_event".equals(factory.getActiveEpicEventName())) {
                    if (percentage > 0.2 && RandomUtil.hasSuccessfullyByChance(percentage)) {
                        ItemStack mythic = RandomUtil.helpMeToChooseOne(
                                new MythicSwordItem().toItemStack(),
                                new MythicBowItem().toItemStack(),
                                new MythicLeggingsItem().toItemStack()
                        );
                        if (mythic != null) {
                            assistPlayer.getInventory().addItem(mythic);
                            log.info("{}", ItemOperationJson.toJson(
                                    ItemOperationJson.mythicGranted(assistPlayer, "ENCHANTING_BANG_ASSIST", mythic)
                            ));
                        }
                    }
                }
                if (PlayerUtil.hasEquipPerk(assistPlayer, PerksBoostsEnum.CONGLOMERATE.getPerk())) {
                    resourceManager.addGold(resourceManager.calculateEXP() * 0.2, "财团分红");
                    resourceManager.addExpModifier(0, "财团分红");
                }

                if (night) {
                    resourceManager.addExpModifier(0.01, "宵禁");
                    resourceManager.addGoldModifier(0.01, "宵禁");
                }

                if (playerProfile.isMaybeBot()) {
                    resourceManager.addGoldModifier(0.01, "§4天外来客");
                    resourceManager.addExpModifier(0.01, "§4天外来客");
                }

                double calculateEXP = resourceManager.calculateEXP();
                double calculateGold = resourceManager.calculateGold();
                //Assistant to the streaker effect
                if (PlayerUtil.hasEquipPerk(assistPlayer, PerksBoostsEnum.ASSISTANT_STREAKER.getPerk()) && !isNight() && !playerProfile.isMaybeBot()) {
                    assistProfile.setStreakKills(assistProfile.getStreakKills() + Math.floor(100 * percentage) * 0.01);
                }

                assistProfile.setAssists(assistProfile.getAssists() + 1);
                assistProfile.setExperience(assistProfile.getExperience() + calculateEXP);
                assistProfile.setCoins(assistProfile.getCoins() + calculateGold);
                assistProfile.grindCoins(calculateGold);
                assistProfile.applyExperienceToPlayer(assistPlayer);


//                calculateGold = eventBoost * calculateGold;
//                calculateEXP = eventBoost * calculateEXP;
                assistPlayer.playSound(assistPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.7F);
                String expBreakdown = cn.charlotte.pit.commands.player.KillRecap.getExpBreakdown(resourceManager);
                String goldBreakdown = cn.charlotte.pit.commands.player.KillRecap.getGoldBreakdown(resourceManager);
                String t1 = "";
                if (!NpcFactory.getRegistry().isNPC(player))
                    t1 = CC.translate("&a&l助攻! &7" + numFormat.format(percentage * 100) + "% 的伤害在 " + playerProfile.getFormattedName());
                String gold = CC.translate(" &6+" + numFormat.format(calculateGold) + "硬币 ");
                String exp = CC.translate((assistProfile.getLevel() < 120 ? "&b+" + numFormat.format(calculateEXP) + "经验值" : ""));
                String end = CC.translate((eventBoost > 1 ? boostString : ""));
                TextComponent hoverText = Component.text(t1)
                        .append(Component.text(gold).hoverEvent(HoverEvent.showText(Component.text(goldBreakdown))))
                        .append(Component.text(exp).hoverEvent(HoverEvent.showText(Component.text(expBreakdown))))
                        .append(Component.text(end));

                CC.send(MessageType.COMBAT, assistPlayer, hoverText);
            }
        }
    }

    private static void handleKillBounty(PlayerProfile killerProfile, PlayerProfile playerProfile, ResourceManager resourceManager) {
        //handle bounty - start
        if (playerProfile.getBounty() != 0 && ThePit.getInstance().getEventFactory().getActiveEpicEvent() == null) {
            String bountyColor = "&6";
            if (ActivityType.GENES.isActive()) {
                if (playerProfile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                    bountyColor = "&b";
                }
                if (playerProfile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.DEMON) {
                    bountyColor = "&c";
                }
            }
            QuestData currentQuest = killerProfile.getCurrentQuest();
            if (currentQuest != null && currentQuest.getEndTime() > System.currentTimeMillis() && currentQuest.getQuestType() == AbstractQuest.QuestType.BOUNTY) {
                if (currentQuest.getCurrent() < currentQuest.getTotal()) {
                    currentQuest.setCurrent(currentQuest.getCurrent() + playerProfile.getBounty());
                    if (currentQuest.getCurrent() >= currentQuest.getTotal()) {
                        final Player player = Bukkit.getPlayer(killerProfile.getPlayerUUID());
                        if (player != null) {
                            new PitQuestCompleteEvent(player, currentQuest);
                        }
                    }
                } else {
                    currentQuest.setCurrent(currentQuest.getTotal());
                }
            }
            CC.boardCast(MessageType.BOUNTY, CC.translate("&6&l赏金! " + playerProfile.getFormattedName() + " &7被 " + killerProfile.getFormattedName() + " &7击杀. " + bountyColor + "&l(" + playerProfile.getBounty() + "g)"));
            if (
                    /*
                    ThePit.getInstance().getPitConfig().isGenesisEnable() && killerProfile.getGenesisData().getTier() >= 5) {
                     */
                    ActivityType.GENES.isActive() && killerProfile.getActivityPlayerData().getGenesisData().getTier() >= 6
            ) {
                resourceManager.addGoldModifier(1.5, "阵营赏金加成");
//                killerProfile.grindCoins(1.5 * playerProfile.getBounty());
//                killerProfile.setCoins(killerProfile.getCoins() + 1.5 * playerProfile.getBounty());
            }//去掉了else
            resourceManager.addBaseGold(playerProfile.getBounty(), "赏金");
//                killerProfile.grindCoins(playerProfile.getBounty());
//                killerProfile.setCoins(killerProfile.getCoins() + playerProfile.getBounty());

            if (killerProfile.getBuffData().getBuff("bounty_solvent").getTier() > 0) {
                Player killer = Bukkit.getPlayer(killerProfile.getPlayerUUID());
                if (killerProfile.isDebug()) {
                    killer.sendMessage("handleKillBounty|True");
                }
                resourceManager.addGoldModifier(1.5, "赏金溶剂");
            }
            playerProfile.setBounty(0);
        }
    }

    private static void handleQuest(PlayerProfile killerProfile, Player beKilledPlayer) {
        //process quest - start
        QuestData currentQuest = killerProfile.getCurrentQuest();
        if (currentQuest != null && currentQuest.getEndTime() > System.currentTimeMillis() && currentQuest.getQuestType() == AbstractQuest.QuestType.KILL) {
            if (currentQuest.getCurrent() < currentQuest.getTotal()) {
                if (currentQuest.getKilled().add(beKilledPlayer.getUniqueId().toString())) {
                    currentQuest.setCurrent(currentQuest.getCurrent() + 1);
                    if (currentQuest.getCurrent() >= currentQuest.getTotal()) {
                        final Player player = Bukkit.getPlayer(killerProfile.getPlayerUUID());
                        if (player != null) {
                            new PitQuestCompleteEvent(player, currentQuest);
                        }
                    }
                }
            } else {
                currentQuest.setCurrent(currentQuest.getTotal());
            }
        }
        //process quest - end
    }

    private static void handleCherryDrop(Player player, PlayerProfile killerProfile) {
        boolean isBetter = killerProfile.isBetterPlayer();
        if (RandomUtil.hasSuccessfullyByChance(killerProfile.isDebug() ? 0.5 : (isBetter ? 0.00001 : 0.000001))) {
            ItemStack itemStack = PitItems.CHERRY.toItemStack();
            Mail mail = new Mail();
            mail.setExpireTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
            mail.setSendTime(System.currentTimeMillis());
            PlayerInv contents = new PlayerInv();
            contents.setContents(new ItemStack[]{itemStack});
            mail.setItem(contents);
            if ("spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName()) ) {
                mail.setTitle("&e【尖塔夺魁】 樱桃");
                mail.setContent("&f你在 " + dateFormat.format(System.currentTimeMillis()) + " 参与的尖塔夺魁事件中获得了樱桃.\\n&f请注意及时收取樱桃.");
            } else if (InventoryUtil.isInvFull(player)) {
                mail.setTitle("&e【战斗】 樱桃");
                mail.setContent("&f你在 " + dateFormat.format(System.currentTimeMillis()) + " 战斗中获得了樱桃由于背包已满已发送邮件.\\n&f请注意及时收取樱桃.");
            }
            mail.build();
            Util.sendMail(player.getUniqueId(), mail);
            if (isBetter) {
                CC.boardCast(MessageType.MISC, String.format("&d樱桃 &7%s&7击杀玩家获得了一个樱桃!", killerProfile.getFormattedNameWithRoman()));
            } else {
                CC.send(MessageType.MISC, player, "&d樱桃 &7你击杀玩家获得了一个樱桃!");
            }
        }
    }

    private static void handleMythicItemDrop(PlayerProfile killerProfile, Player killer, LivingEntity beKilledPlayer) {
        int enchantPerkLevel = PerksPrestigeEnum.MYSTICISM.getPerk().getLevel(killerProfile);
        double modifiers = 1.0;
        if (enchantPerkLevel <= 0) {
            return;
        } else {
            modifiers += (enchantPerkLevel - 1) * 0.2d;
        }
        if (ThePit.getInstance().getActivityFactory().getFactoryType() == ActivityType.NEW_YEAR) {
            NewYearData data = killerProfile.getActivityPlayerData().getNewYearData();
            if (data.getPerkMap().containsKey("mysticism")) modifiers += data.getPerkMap().get("mysticism") * 0.1D;
        }

        int pantsLevel = Utils.getEnchantLevel(killer.getInventory().getLeggings(), "pants_radar");
        int mainHandLevel = Utils.getEnchantLevel(killer.getInventory().getItemInMainHand(), "pants_radar");

        if (pantsLevel > 0) {
            modifiers += pantsLevel * 0.3;
        }
        if (mainHandLevel > 0) {
            modifiers += mainHandLevel * 0.3;
        }
        if (killerProfile.hasEquipPerk("UBER") && killerProfile.getStreakKills() > 100.0) {
            modifiers += 0.5;
        }
        final EventFactory factory = ThePit.getInstance().getEventFactory();
        if ("enchanting_bang_event".equals(factory.getActiveEpicEventName()) && killer.hasPotionEffect(PotionEffectType.GLOWING)) {
            modifiers += 0.5;
        }

        double chance = NewConfiguration.getMythicDropChance(killer) * (modifiers * killerProfile.getBuffData().getBuff("mythic_buff").getTier() > 0 ? 2 : 1);
        boolean b = RandomUtil.hasSuccessfullyByChance(chance);
        if (b) {
            AbstractPitItem item;
            if (enchantPerkLevel >= 4) {
                item = RandomUtil.helpMeToChooseOne(
                        new MythicBowItem(),
                        new MythicSwordItem(),
                        new MythicLeggingsItem());
            } else {
                item = RandomUtil.helpMeToChooseOne(
                        new MythicBowItem(),
                        new MythicSwordItem());
            }

            ItemStack itemStack = item.toItemStack();
            if ("spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
                Mail mail = new Mail();
                mail.setExpireTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
                mail.setSendTime(System.currentTimeMillis());
                PlayerInv contents = new PlayerInv();
                contents.setContents(new ItemStack[]{itemStack});
                mail.setTitle("&e【尖塔夺魁】 神话物品发放");
                mail.setContent("&f你在 " + dateFormat.format(System.currentTimeMillis()) + " 参与的尖塔夺魁事件中获得了神话物品.\\n&f请注意及时收取神话物品.");
                mail.setItem(contents);
                Util.sendMail(killer.getUniqueId(), mail);
            } else {
                if (InventoryUtil.isInvFull(killer.getInventory())) {
                    beKilledPlayer.getWorld().dropItemNaturally(beKilledPlayer.getLocation(), itemStack);
                } else {
                    InventoryUtil.addInvReverse(killer.getInventory(), itemStack);
                }
            }
            log.info("{}", ItemOperationJson.toJson(
                    ItemOperationJson.mythicGranted(killer, "KILL_DROP", itemStack)
            ));

            CC.send(MessageType.MISC, killer, "&d&l神话装备! " + ("spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName()) ? "&7你在尖塔夺魁中拾取了掉落的神话物品! 已发放至邮件!" : "&7你在战斗中拾取了掉落的神话物品!"));
            new BukkitRunnable() {
                int task = 0;

                @Override
                public void run() {
                    killer.playSound(beKilledPlayer.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING,
                            1,
                            0.1F + (0.5F * task));
                    task++;

                    if (task >= 6) {
                        cancel();
                    }
                }
            }.runTaskTimer(ThePit.getInstance(), 10, 5);
        }
    }

    private static void handleItemDrop(PlayerProfile killerProfile, Player killer, Player beKilledPlayer) {
        try {
            //lucky diamond
            boolean enabledLuckyDiamond = PlayerUtil.hasEquipPerk(killer, PerksBoostsEnum.LUCKY_DIAMOND.getPerk());

            // 一个目标仅触发一次
            boolean hasSet = false;
            for (EquipmentSlot slot : new EquipmentSlot[]{CHEST, LEGS, FEET}) {
                ItemStack itemStack = beKilledPlayer.getInventory().getItem(slot);

                if (itemStack.getType() != Material.AIR && CraftItemStack.asNMSCopy(itemStack).getItem() instanceof ArmorItem && Utils.getMythicItem(itemStack) == null) {
                    //iron armor
                    if (itemStack.getType().name().contains("IRON")) {
                        if (!hasSet && enabledLuckyDiamond && RandomUtil.hasSuccessfullyByChance(0.3)) {
                            //只会替换玩家的铁质或锁链装备，避免替换掉神话装备...
                            Material type = killer.getInventory().getItem(slot).getType();
                            if (Utils.getMythicItem(killer.getInventory().getItem(slot)) != null) {
                                continue;
                            }
                            if (!type.toString().contains("IRON") && !type.toString().contains("CHAINMAIL")) {
                                continue;
                            }
                            String name = itemStack.getType().name();
                            if (name.equals("IRON_CHESTPLATE")) {
                                name = "铁胸甲";
                            } else if (name.equals("IRON_LEGGINGS")) {
                                name = "铁护腿";
                            } else {
                                name = "铁靴子";
                            }
                            itemStack = new ItemBuilder(Material.valueOf(itemStack.getType().name().replace("IRON", "DIAMOND")))
                                    .deathDrop(true)
                                    .canDrop(false)
                                    .canSaveToEnderChest(false)
                                    .internalName("lucky_diamond")
                                    .buildWithUnbreakable();
                            //player.getWorld().dropItemNaturally(killer.getLocation(), itemStack);
//                            ItemStack finalItemStack = itemStack;
//                            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> Bukkit.getPluginManager().callEvent(new PlayerPickupItemEvent(killer, beKilledPlayer.getWorld().dropItemNaturally(killerLoc, finalItemStack), 1)));
                            killer.getInventory().setItem(slot, itemStack);
                            killer.sendMessage("§b§l幸运钻石! §7您的 " + name + " 装备已升级为钻石类型!");
                            hasSet = true;
                            continue;
                        }
                        ItemStack finalItemStack1 = itemStack;
                        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> beKilledPlayer.getWorld().dropItemNaturally(beKilledPlayer.getLocation(), finalItemStack1));
                        continue;
                    }
                    //diamond armor
                    if (itemStack.getType().name().contains("DIAMOND")) {
                        //drop shop diamond armor only
                        if (itemStack.getType().name().contains("HELMET") || Utils.getMythicItem(itemStack) != null) {
                            continue;
                        }
                        if ("shopItem".equals(ItemUtil.getInternalName(itemStack)) && RandomUtil.hasSuccessfullyByChance(0.05)) {
                            ItemStack finalItemStack2 = itemStack;
                            Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> beKilledPlayer.getWorld().dropItemNaturally(beKilledPlayer.getLocation(), finalItemStack2));
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            for (ItemStack item : beKilledPlayer.getInventory().getContents()) {
                if (item == null) {
                    continue;
                }
                PitItems resource = switch (item.getType()) {
                    case WHEAT -> PitItems.WHEAT;
                    case SUGAR -> PitItems.SUGAR;
                    case SLIME_BALL -> PitItems.RUBBIN;
                    case HAY_BLOCK -> PitItems.PACKAGED_BALE;
                    default -> null;
                };
                if (resource == null || (resource == PitItems.PACKAGED_BALE && resource.isInternalNameEquals(item))) {
                    continue;
                }
                ItemStack[] drops = resource.isInternalNameEquals(item) && AdvancedItemIdentity.readUuid(item) != null
                        ? new ItemStack[]{item}
                        : resource.toItemStacks(item.getAmount());
                for (ItemStack drop : drops) {
                    killer.getWorld().dropItemNaturally(beKilledPlayer.getLocation(), drop);
                }
            }
        } catch (Exception e) {
            CC.printError(killer, e);
            CC.printError(beKilledPlayer, e);
        }
    }

    private static void handleAddBounty(PlayerProfile killerProfile, Player killer) {
        if (ThePit.getInstance().getEventFactory().getActiveEpicEvent() != null) {
            return;
        }
        boolean hasEquipHighLander = PlayerUtil.hasEquipPerk(killer, PerksBundlesEnum.HIGH_LANDER.getPerk());
        int maxBounty = (hasEquipHighLander && killerProfile.getStreakKills() >= 50) ? 10000 : 5000;
        boolean hasMaxBounty = killerProfile.getBounty() >= maxBounty;
        if (hasMaxBounty) {
            return;
        }

        killerProfile.setBountyStreak(killerProfile.getBountyStreak() + 1);
        int bountyStreak = killerProfile.getBountyStreak();
        if (bountyStreak < 6) {
            return;
        }
        int i = bountyStreak - 6;

        int bounty = choseBounty(killerProfile);
        if (bounty <= 0) {
            return;
        }
        killerProfile.setBounty(Math.min(maxBounty, killerProfile.getBounty() + bounty));
        killerProfile.setBountyStreak(0);

        String bountyColor = "&6";
        if (ThePit.getInstance().getPitConfig().isGenesisEnable()) {
            if (killerProfile.getGenesisData().getTeam() == GenesisTeam.ANGEL) {
                bountyColor = "&b";
            }
            if (killerProfile.getGenesisData().getTeam() == GenesisTeam.DEMON) {
                bountyColor = "&c";
            }
        }
        CC.boardCast(MessageType.BOUNTY, "&6&l赏金! " + killerProfile.getFormattedName() + " &7当前已经被以 " + bountyColor + killerProfile.getBounty() + bountyColor + "g &7的金额悬赏了!");
    }

    private static int choseBounty(PlayerProfile playerProfile) {
        int streak = playerProfile.getStreakCount();
        int Bounty = 0;
        if (streak <= 14) {
            Bounty += 50;
        } else if (streak <= 28) {
            Bounty += 100;
        } else if (streak <= 42) {
            Bounty += 150;
        } else if (streak <= 100) {
            Bounty += 200;
        } else if (streak <= 150) {
            Bounty += 250;
        } else if (streak > 250) {
            Bounty += 300;
        }
        if (streak >= 28 && RandomUtil.hasSuccessfullyByChance((double) streak / 1000)) {
            Bounty += RandomUtil.helpMeToChooseOne(150, 200, 250, 300, 350);
        }
        return Bounty;
    }

    private static void handleMysteryitemDrop(PlayerProfile profile, Player killer) {
        int level = PerksPrestigeEnum.OCCULTDOCTRINE.getPerk().getLevel(profile);
        if (level == 0) return;
        if (RandomUtil.hasSuccessfullyByChance(level * 0.002)) {
            Util.addItemOfMail(PitItems.MYSTERIOUS_FRAGMENTS.toItemStack(), killer, null);
            CC.send(killer, "&5神秘学说 &7你获得了一个 &5神秘碎片");
            return;
        }

        if (RandomUtil.hasSuccessfullyByChance(level * 0.0005)) {
            Util.addItemOfMail(PitItems.MYSTERY_BOX_FRAGMENT.toItemStack(), killer, null);
            CC.send(killer, "&5神秘学说 &7你获得了一个 &3神秘盒子残片");
        }
//        if (level > 3 && RandomUtil.hasSuccessfullyByChance((level - 2) * 0.0001)) {
//            Util.addItemOfMail(PitItems.MYSTERY_BOX_TWO.toItemStack(), killer, null);
//            CC.send(killer, "&5神秘学说 &7你获得了一个 &9II阶神秘之盒");
//            return;
//        }
//        if (RandomUtil.hasSuccessfullyByChance(level * 0.0002)) {
//            Util.addItemOfMail(PitItems.MYSTERY_BOX_ONE.toItemStack(), killer, null);
//            CC.send(killer, "&5神秘学说 &7你获得了一个 &9I阶神秘之盒");
//        }
    }

    private static void handleBoardCastMessage(PlayerProfile killerProfile, PlayerProfile playerProfile, Player killer, LivingEntity beKilledPlayer, double totalCoins, double totalXp, ResourceManager resourceManager, LinkedList<String> msg) {

        if (beKilledPlayer instanceof Player)
            (((Player) beKilledPlayer)).playSound(beKilledPlayer.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1, 1.5F);

        String genesisStatus = "";
        if (ThePit.getInstance().getPitConfig().isGenesisEnable() && killerProfile.getGenesisData().getTeam() != GenesisTeam.NONE && !isNight()) {
            if (killerProfile.getGenesisData().getTeam() == playerProfile.getGenesisData().getTeam()) {
                killerProfile.getGenesisData().setPoints(killerProfile.getGenesisData().getPoints() + 1);
                if (killerProfile.getGenesisData().getTeam() == GenesisTeam.ANGEL)
                    genesisStatus = " &b+1活动点数";
                if (killerProfile.getGenesisData().getTeam() == GenesisTeam.DEMON)
                    genesisStatus = " &c+1活动点数";
            } else {
                killerProfile.getGenesisData().setPoints(killerProfile.getGenesisData().getPoints() + 2);
                if (killerProfile.getGenesisData().getTeam() == GenesisTeam.ANGEL)
                    genesisStatus = " &b+2活动点数";
                if (killerProfile.getGenesisData().getTeam() == GenesisTeam.DEMON)
                    genesisStatus = " &c+2活动点数";
            }
        }

        final int streakNumberShort = killerProfile.addAndGetStreakNumberShort();
        String prefix;
        switch (streakNumberShort) {
            case 2: {
                ThePit.getInstance().getSoundFactory().playSound("double_streak", killer);
                prefix = "双杀";
                break;
            }
            case 3: {
                ThePit.getInstance().getSoundFactory().playSound("triple_streak", killer);
                prefix = "三杀";
                break;
            }
            case 4: {
                ThePit.getInstance().getSoundFactory().playSound("quadra_streak", killer);
                prefix = "四杀";
                break;
            }
            case 5: {
                ThePit.getInstance().getSoundFactory().playSound("streak", killer);
                prefix = "五杀";
                break;
            }
            default: {
                if (streakNumberShort > 5) {
                    prefix = "多杀";
                    ThePit.getInstance().getSoundFactory().playSound("streak", killer);
                } else {
                    killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.9F);
                    prefix = "击杀";
                }
            }
        }

        String expBreakdown = cn.charlotte.pit.commands.player.KillRecap.getExpBreakdown(resourceManager);
        String goldBreakdown = cn.charlotte.pit.commands.player.KillRecap.getGoldBreakdown(resourceManager);

        StringBuilder msgBuilder = new StringBuilder();
        if (!msg.isEmpty()) {
            for (String s : msg)
                msgBuilder.append(s);
        }

        if (totalXp > 0) {
            TextComponent info = Component.text(CC.translate("&a&l" + prefix + "! " + RankUtil.getPlayerColoredName(beKilledPlayer.getUniqueId())))
                    .append(Component.text(CC.translate(" &6+" + numFormat.format(totalCoins) + "硬币 ")).hoverEvent(HoverEvent.showText(Component.text(goldBreakdown))))
                    .append(Component.text(CC.translate(" &b+" + numFormat.format(totalXp) + "经验值")).hoverEvent(HoverEvent.showText(Component.text(expBreakdown))))
                    .append(Component.text(CC.translate(genesisStatus + msgBuilder + (eventBoost > 1 ? boostString : ""))));
            CC.send(MessageType.COMBAT, killer, info);
        } else {
            TextComponent info = Component.text(CC.translate("&a&l" + prefix + "! " + RankUtil.getPlayerColoredName(beKilledPlayer.getUniqueId())))
                    .append(Component.text(CC.translate(" &6+" + numFormat.format(totalCoins) + "硬币 ")).hoverEvent(HoverEvent.showText(Component.text(goldBreakdown))))
                    .append(Component.text(CC.translate(genesisStatus + msgBuilder.toString() + (eventBoost > 1 ? boostString : ""))));
            CC.send(MessageType.COMBAT, killer, info);
        }

        String deathString = CC.translate("&c&l死亡! &7被 " + killerProfile.getFormattedName() + " &7击杀.");

        TextComponent deathMsg = LegacyComponentSerializer.legacyAmpersand().deserialize(" &e&l死亡回放");
        net.kyori.adventure.text.event.HoverEvent<Component> hoverEvent = Component.text(CC.translate("&7点击查看你的死亡回放")).asHoverEvent();
        deathMsg = deathMsg.hoverEvent(hoverEvent).clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND, "/killrecap"));

        if (beKilledPlayer instanceof Player) {
            CC.send(MessageType.COMBAT, (Player) beKilledPlayer, LegacyComponentSerializer.legacyAmpersand().deserialize(deathString));
            CC.send(MessageType.COMBAT, (Player) beKilledPlayer, deathMsg);
        }

    }

    private static void handleGivePlayerKillReward(Player killer) {
        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            int limit = PlayerUtil.getPlayerHealItemLimit(killer);
            if (limit < 0) {
                limit = 2;
            }
            if (PlayerUtil.getPlayerHealItemAmount(killer) < limit) {
                killer.getInventory().addItem(PitItems.BAKED_POTATO.toItemStack());
            }
        } else if (PlayerUtil.getAmountOfActiveHealingPerk(killer) == 0) {
            if (PlayerUtil.getPlayerHealItemAmount(killer) < PlayerUtil.getPlayerHealItemLimit(killer)) {
                killer.getInventory().addItem(PitItems.GOLDEN_APPLE.toItemStack());
            }
        }
    }

    private static void handleGameEffect(PlayerProfile killerProfile, Player killer, LivingEntity player, ResourceManager resourceManager) {
        if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            for (TypedPerk<?> perk : killerProfile.getActivePerks()) {
                perk.handlePlayerKilled(perk.getLevel(killerProfile), killer, player, resourceManager);
            }
        }
        EnchantmentFactor enchantmentFactor = ThePit.getInstance().getEnchantmentFactor();
        Map<String, AbstractEnchantment> enchantmentMap = enchantmentFactor.getEnchantmentMap();
        Map<String, Integer> playerItemEnchantmentMap;
        Map<String, Integer> playerLeggingEnchantmentMap;
        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        playerItemEnchantmentMap = Util.getEnchantments(mainHand);
        playerLeggingEnchantmentMap = Util.getEnchantments(killer.getInventory().getLeggings());
        PlayerProfile profile = Util.getPitProfile(killer);
        boolean shouldIgnoreEnchantsDueToPrestige = profile.getPrestige() == 0;
        if (!shouldIgnoreEnchantsDueToPrestige) {
            for (String nbt : playerItemEnchantmentMap.keySet()) {
                AbstractEnchantment abs = enchantmentMap.get(nbt);
                if (abs != null && IPlayerKilledEntity.class.isAssignableFrom(abs.getClass()))
                    GameEffectListener.processKilled((IPlayerKilledEntity) abs, playerItemEnchantmentMap.get(nbt), killer, player, resourceManager);
            }
            for (String nbt : playerLeggingEnchantmentMap.keySet()) {
                AbstractEnchantment abs = enchantmentMap.get(nbt);
                if (abs != null && IPlayerKilledEntity.class.isAssignableFrom(abs.getClass()))
                    GameEffectListener.processKilled((IPlayerKilledEntity) abs, playerLeggingEnchantmentMap.get(nbt), killer, player, resourceManager);
            }
        }
//        for(IPlayerKilledEntity ins : ThePit.getInstance().getEnchantmentFactor().getPlayerKilledEntities()) {
//            AbstractEnchantment enchant = (AbstractEnchantment) ins;
//            int level = enchant.getItemEnchantLevel(killer.getItemInHand());
//            GameEffectListener.processKilled(ins,level,killer,player,resourceManager);
//            if(killer.getInventory().getLeggings() != null && killer.getInventory().getLeggings().getType() != Material.AIR) {
//                level = enchant.getItemEnchantLevel(killer.getInventory().getLeggings());
//                GameEffectListener.processKilled(ins,level,killer,player,resourceManager);
//            }
//        }

        if (player instanceof Player beKilledPlayer) {

            PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(beKilledPlayer.getUniqueId());
            for (TypedPerk<?> perk : playerProfile.getActivePerks()) {
                GameEffectListener.processBeKilledByEntity(perk, perk.getLevel(playerProfile), beKilledPlayer, killer, resourceManager);
            }

            mainHand = beKilledPlayer.getInventory().getItemInMainHand();
            playerItemEnchantmentMap = Util.getEnchantments(mainHand);
            playerLeggingEnchantmentMap = Util.getEnchantments(beKilledPlayer.getInventory().getLeggings());
            PlayerProfile beprofile = Util.getPitProfile(beKilledPlayer);
            shouldIgnoreEnchantsDueToPrestige = beprofile.getPrestige() == 0;
            if (!shouldIgnoreEnchantsDueToPrestige) {
                for (String nbt : playerItemEnchantmentMap.keySet()) {
                    AbstractEnchantment abs = enchantmentMap.get(nbt);
                    if (abs != null && IPlayerBeKilledByEntity.class.isAssignableFrom(abs.getClass()))
                        GameEffectListener.processBeKilledByEntity((IPlayerBeKilledByEntity) abs, playerLeggingEnchantmentMap.get(nbt), beKilledPlayer, killer, resourceManager);
                }
                for (String nbt : playerLeggingEnchantmentMap.keySet()) {
                    AbstractEnchantment abs = enchantmentMap.get(nbt);
                    if (abs != null && IPlayerBeKilledByEntity.class.isAssignableFrom(abs.getClass()))
                        GameEffectListener.processBeKilledByEntity((IPlayerBeKilledByEntity) abs, playerLeggingEnchantmentMap.get(nbt), beKilledPlayer, killer, resourceManager);
                }
            }
//            for(IPlayerBeKilledByEntity ins : ThePit.getInstance().getEnchantmentFactor().getPlayerBeKilledByEntities()) {
//                AbstractEnchantment enchant = (AbstractEnchantment) ins;
//
//                ItemStack itemInMainHand = beKilledPlayer.getInventory().getItemInMainHand();
//                int level = enchant.getItemEnchantLevel(itemInMainHand);
//                if(itemInMainHand.getType() != Material.AIR && itemInMainHand.getType() != Material.LEATHER_LEGGINGS) {
//                    GameEffectListener.processBeKilledByEntity(ins,level,beKilledPlayer,killer,resourceManager);
//                }
//                if(beKilledPlayer.getInventory().getLeggings() != null && beKilledPlayer.getInventory().getLeggings().getType() != Material.AIR) {
//                    level = enchant.getItemEnchantLevel(beKilledPlayer.getInventory().getLeggings());
//                    GameEffectListener.processBeKilledByEntity(ins,level,beKilledPlayer,killer,resourceManager);
//                }
//            }
        }

        applyEquipmentRewardModifiers(killer, killerProfile, resourceManager);
        // Handle KillCounterGem
        ItemStack item = killer.getInventory().getItem(HAND);
        Optional.ofNullable(ItemUtil.getInternalName(item))
                .filter(l -> l.equals(MythicSwordItem.INTERNAL_NAME) ||
                        l.equals(MythicBowItem.INTERNAL_NAME))
                .map(l -> FuncsKt.toMythicItem(item))
                .filter(l -> l.getKillCounterGem() != -1)
                .ifPresent(l -> {
                    int kill = l.getKillCounterGem() + 1;
                    l.setKillCounterGem(kill);
                    if (kill == 1 || kill % 100 == 0) {
                        Component component = new ChatComponentBuilder(CC.translate("&c&l击杀记录! &f玩家" + killerProfile.getFormattedName() + " &f手持&6[" +
                                item.getItemMeta().getDisplayName() +
                                "&6]&f已经造成了 &c" +
                                kill +
                                "&f 次击杀!"))
                                .setCurrentHoverEvent(item.asHoverEvent()).build();
                        CC.boardCast(MessageType.STREAK, component);
                    }
                    killer.getInventory().setItem(HAND, l.toItemStack());
                });
    }

    private static void calculationKillReward(PlayerProfile killerProfile, PlayerProfile beKilledProfile, KillRecap killRecap, Player killer, ResourceManager resourceManager) {
        KillStreakRewards.apply(resourceManager, killerProfile.getStreakKills(),
                PlayerUtil.hasEquipPerk(killer, PerksBoostsEnum.STREAKER.getPerk()),
                PerksBundlesEnum.HIGH_LANDER.getPerk().hasEquip(killerProfile));
        if (killerProfile.getStreakKills() <= 3) {
            killRecap.setNotStreakExp(4);
            killRecap.setNotStreakCoin(4);
        }
        double streakAddon = 0;

        if (beKilledProfile.getStreakKills() != 0 && beKilledProfile.getStreakKills() > 30) {
            resourceManager.addExp(Math.min(50, (killerProfile.getStreakKills() / 10d) * 5), "连杀终结");
            resourceManager.addGold(Math.min(50, (killerProfile.getStreakKills() / 10d) * 5), "连杀终结");

            streakAddon += Math.min(50, (killerProfile.getStreakKills() / 10d) * 5);
        }

        killRecap.setStreakCoin(streakAddon);
        killRecap.setStreakExp(streakAddon);

        double levelAddon = 0;
        if (beKilledProfile.getPrestige() - killerProfile.getPrestige() >= 4) {
            double prestigeDiff = beKilledProfile.getPrestige() - killerProfile.getPrestige();
            double prestigeExpBonus = Math.min(prestigeDiff * 0.25, 2.0); // 每级25%，上限200%
            double prestigeGoldBonus = Math.min(prestigeDiff * 0.25, 2.0); // 每级25%，上限200%

            resourceManager.addExpModifier(1 + prestigeExpBonus, "精通等级压制击杀");
            resourceManager.addGoldModifier(1 + prestigeGoldBonus, "精通等级压制击杀");

            levelAddon += prestigeDiff * 3;
        }
        if (killerProfile.getLevel() < 100 && beKilledProfile.getLevel() - killerProfile.getLevel() >= 40) {
            double levelDiff = beKilledProfile.getLevel() - killerProfile.getLevel();
            double levelExpBonus = Math.min(levelDiff * 0.01, 0.6); // 每级1%，上限80%
            double levelGoldBonus = Math.min(levelDiff * 0.01, 0.6); // 每级1%，上限80%

            resourceManager.addExpModifier(1 + levelExpBonus, "等级压制击杀");
            resourceManager.addGoldModifier(1 + levelGoldBonus, "等级压制击杀");

            levelAddon += levelDiff / 40d;
        }

        if (hasPremiumItem(killer)) {
            if (!hasPremiumItem(killer)) {
                resourceManager.addExp(5, "以弱胜强");
                resourceManager.addGold(5, "以弱胜强");

                levelAddon += 5;
            }
        }
        if (!isBedrockRoom) {
            if (beKilledProfile.getLevel() < 20) {
                resourceManager.addGoldModifier(0.8, "等级欺压");
                resourceManager.addExpModifier(0.8, "等级欺压");
            }
            if ((killerProfile.getPrestige() >= 1 && killerProfile.getLevel() - beKilledProfile.getLevel() > 30) && beKilledProfile.getPrestige() == 0) {
                if (beKilledProfile.getLevel() <= 10) {
                    resourceManager.addGoldModifier(0.6, "欺负菜鸡");
                    resourceManager.addExpModifier(0.6, "欺负菜鸡");
                    levelAddon = 0;
                } else if (beKilledProfile.getLevel() <= 30) {
                    resourceManager.addGoldModifier(0.85, "欺负菜鸟");
                    resourceManager.addExpModifier(0.85, "欺负菜鸟");
                    levelAddon = 0.25 * levelAddon;
                }
            }
        }

        killRecap.setLevelDisparityExp(levelAddon);
        killRecap.setLevelDisparityCoin(levelAddon);

//        totalCoins = eventBoost * totalCoins;
//        totalXp = eventBoost * totalXp;

//        resourceManager.addExpModifier(eventBoost, "活动增益");
//        resourceManager.addGold(eventBoost, "活动增益");

        //calculation kill reward - end
    }

    private static KillRecap initializationKillRecap(PlayerProfile beKilledProfile, PlayerProfile killerProfile, Player killer, ResourceManager resourceManager) {
        KillRecap beKilledKillRecap = beKilledProfile.getKillRecap();

        beKilledKillRecap.setKiller(killer.getUniqueId());

        beKilledKillRecap.getAssistData().clear();
        beKilledKillRecap.getPerk().clear();

        for (int i = 1; i < 5; i++) {
            String perkName = killerProfile.getChosePerk()[i - 1];
            if (perkName != null) {
                beKilledKillRecap.getPerk().add(perkName);
            }
        }

        beKilledKillRecap.setResourceManager(resourceManager);

        return beKilledKillRecap;
    }

    public static boolean isNight() {
        if (onNight) {
            return true;
        }
        if (!ThePit.getInstance().getPitConfig().isCurfewEnable()) {
            return false;
        }
        final Calendar instance = Calendar.getInstance();
        instance.setTimeInMillis(System.currentTimeMillis());
        final int hour = instance.get(Calendar.HOUR_OF_DAY);
        final int curfewStart = ThePit.getInstance().getPitConfig().getCurfewStart();
        final int curfewEnd = ThePit.getInstance().getPitConfig().getCurfewEnd();

        return hour >= curfewStart && hour <= curfewEnd;
    }

    public static void addBanner(Location location, UUID owner) {
        removeBanner(location);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                removeBanner(location);
                Player ownerPlayer = Bukkit.getPlayer(owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(CC.translate("&c你的增益旗帜已消失"));
                }
            }
        }.runTaskLater(ThePit.getInstance(), 20 * 60 * 10);

        xpBanners.put(location, task);
        bannerOwners.put(location, owner);
    }

    public static void removeBanner(Location location) {
        if (location.getBlock().getType() == Material.GREEN_BANNER) {
            location.getBlock().setType(Material.AIR);
        }
        BukkitTask task = xpBanners.remove(location);
        if (task != null) {
            task.cancel();
        }
        bannerOwners.remove(location);
    }

    public static void removeAllBanner() {
        List<Location> allBanners = new ArrayList<>(xpBanners.keySet());
        for (Location loc : allBanners) {
            removeBanner(loc);
        }
        xpBanners.clear();
        bannerOwners.clear();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onStrike(PitStreakKillChangeEvent event) {
        final PlayerProfile profile = event.getPlayerProfile();

        if (profile.getChosePerk()[4] == null) {
            return;
        }

        //debug code

        if (Math.floor(event.getFrom()) % 5 != 0 && Math.floor(event.getTo()) % 5 == 0 && event.getTo() > 0 && Math.floor(event.getFrom()) != Math.floor(event.getTo())) {
            CC.boardCast(MessageType.STREAK, "&c&l连杀! " + profile.getFormattedName() + " &7已经 &c" + intFormat.format(Math.floor(event.getTo())) + "&7 连杀了!");
        }
    }

    @EventHandler
    public void onTp(PlayerTeleportEvent event) {
        if (PlayerMoveHandler.getCantMoveList().contains(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        PlayerMoveHandler.checkMove(event.getTo(), event.getFrom(), event.getPlayer());
    }

    /** Training invokes this before its final 10% reduction; ordinary combat keeps its old timing. */
    public static void applyDiamondSwordBonus(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player player
                && damager.getInventory().getItemInMainHand().getType() == Material.DIAMOND_SWORD
                && "shopItem".equals(ItemUtil.getInternalName(damager.getInventory().getItemInMainHand()))
                && PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBounty() != 0) {
            event.setDamage(event.getDamage() * 1.2);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCombat(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getDamager() instanceof Player damager) {

            PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            PlayerProfile damagerProfile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());

            //DiamondSword Boost
            if (!PitBotManager.isTrainingPvp(damager, player)) applyDiamondSwordBonus(event);

            //CombatSpade Boost

            handleDamage(event.getDamager(), event.getFinalDamage(), player, damager, playerProfile, damagerProfile, event.getFinalDamage(), false);
            playerProfile.setLastDamageAt(System.currentTimeMillis());
            if (event.getFinalDamage() > (player.getHealth() + player.getAbsorptionAmount())) {
                event.setCancelled(true);
                handlePlayerDeath(player, damager, true);
            }
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player damager) {

            PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            PlayerProfile damagerProfile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());

            handleDamage(event.getDamager(), event.getFinalDamage(), player, damager, playerProfile, damagerProfile, event.getFinalDamage(), true);

            playerProfile.setLastDamageAt(System.currentTimeMillis());
            if (event.getFinalDamage() > (player.getHealth() + player.getAbsorptionAmount())) {
                event.setCancelled(true);
                handlePlayerDeath(player, damager, true);
            }
        } else if (event.getDamager() instanceof TNTPrimed && event.getEntity() instanceof Player deathPlayer) {
            List<MetadataValue> metadata = event.getDamager().getMetadata("internal");
            if (!metadata.isEmpty()) {
                if ("tnt".equals(metadata.getFirst().asString())) {
                    event.setDamage(event.getDamage() * (1 / 5F));
                } else if ("red_packet".equals(metadata.getFirst().asString())) {
                    event.setDamage(0);
                } else if ("tnt_enchant_item".equals(metadata.getFirst().asString())) {
                    if (PlayerUtil.isEquippingSomber(deathPlayer)) {
                        event.setCancelled(true);
                        return;
                    }
                    List<MetadataValue> tntDamage = event.getDamager().getMetadata("damage");
                    if (!tntDamage.isEmpty()) {
                        event.setDamage(1 + tntDamage.getFirst().asInt());
                        if (PlayerUtil.getDistance(event.getDamager().getLocation(), deathPlayer.getLocation()) > 3) {
                            event.setCancelled(true);
                        }
                    }
                } else if ("insta_boom_enchant_item".equals(metadata.getFirst().asString())) {
                    if (PlayerUtil.isEquippingSomber(deathPlayer)) {
                        event.setCancelled(true);
                        return;
                    }
                    List<MetadataValue> tntDamage = event.getDamager().getMetadata("damage");
                    if (!tntDamage.isEmpty()) {
                        event.setDamage(0.5 * tntDamage.getFirst().asInt());
                        if (PlayerUtil.getDistance(event.getDamager().getLocation(), deathPlayer.getLocation()) > 4) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

    }


//    @EventHandler
//    public void onJoinForVer(PlayerJoinEvent e) {
//        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(e.getPlayer().getUniqueId());
//        if (profile.getUpdate() != UpdateMenu.getVer()) {
//            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
//                if (e.getPlayer().isOnline()) {
//                    for (String line : UpdateMenu.versionToLog(UpdateMenu.getVer())) {
//                        e.getPlayer().sendMessage(CC.translate(line));
//                    }
//                    if (!profile.isAutoUpData()) {
//                        e.getPlayer().sendMessage(CC.translate("&d天坑神秘人提醒你记得前往图鉴NPC确认哦!&o(只需要随便点击一本书就可以确认了)"));
//                    }
//                    profile.setUpdate(UpdateMenu.getVer());
//                    if (profile.isdebug()) {
//                        e.getPlayer().sendMessage("目前Ver:" + UpdateMenu.getVer());
//                        e.getPlayer().sendMessage("Profile的Ver:" + profile.getUpdate());
//                    }
//                }
//            }, 60);
//        }
//        if (profile.getJoinMessage() != null && !profile.getJoinMessage().isEmpty()) {
//            String message = profile.getJoinMessage().replace("&player%", profile.getFormattedName());
//            for (Player online : ThePit.getInstance().getServer().getOnlinePlayers()) {
//                CC.send(MessageType.World, online, message);
//            }
//        }
//    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ProfileLoadRunnable.getInstance().handleQuit(player);

        if (PlayerProfile.getCacheProfile().containsKey(player.getUniqueId())) {
            final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (!PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile, player)) {
                handlePlayerDeath(player, null, false);
                if (!profile.isTempInvUsing()) {
                    profile.setInventory(PlayerInv.fromPlayerInventory(event.getPlayer().getInventory()));
                }
                profile.setBounty(0);
            }

        }
        for (TypedPerk<?> perk : ThePit.getInstance().getPerkFactory().getPerks()) {
            perk.handleQuit(0, player);
        }
        loc.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKilled(PlayerDeathEvent event) {
        event.deathMessage(null);
        handlePlayerDeath(event.getEntity(), event.getEntity().getKiller(), true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onStreakChangeEvent(PitStreakKillChangeEvent event) {
        EventFactory factory = ThePit.getInstance().getEventFactory();
        if (factory.getActiveEpicEvent() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBowDraw(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            float force = event.getForce();
            Util.getPitProfile(player).setForce(force);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        for (TypedPerk<?> perk : ThePit.getInstance().getPerkFactory().getPerks()) {
            perk.handleJoin(0, player);
        }


        bedrockRoom(player.getUniqueId());
        godjoin(player);
//        queryUUID(player);
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            PlayerProfile profile = Util.getPitProfile(player);
            String currentDate = LocalDate.now().toString();

            if (!currentDate.equals(profile.getLastboxinTime())) {
                profile.setLastboxinTime(currentDate);

                if (profile.getPrestige() >= 1 && profile.getLevel() >= 70 && profile.isSupporter()) {
                    player.getInventory().addItem(PitItems.ACTION_GIFT_BOX.toItemStack());
                    player.sendMessage(CC.translate("&f美好的一天"));
                    player.sendMessage(CC.translate("&e行动礼盒 + 1"));
                    if (profile.getPrestige() >= 15) {
                        player.getInventory().addItem(PitItems.FUNKY_FEATHER.toItemStack());
                        player.sendMessage(CC.translate("&e时髦的羽毛 + 1"));
                    }
                } else {
                    player.sendMessage(CC.translate("&e美好的一天"));
                }
                if (profile.getTodayGoldRecord() == null) {
                    return;
                }
                if (!profile.getTodayGoldRecord().isEmpty()) {
                    goldHistory goldHistory = new goldHistory();
                    profile.getTodayGoldRecord().forEach(map -> {
                        map.getGoldSource().forEach((key, value) ->
                                goldHistory.getGoldSource().merge(key, value, Long::sum));
                        goldHistory.setDailyGoldTotal(goldHistory.getDailyGoldTotal() + map.getDailyGoldTotal());
                    });
                    profile.getHistoricalGoldRecord().put(System.currentTimeMillis(), goldHistory);
                }
            }


        }, 60L);
    }

    private void bedrockRoom(UUID uniqueId) {
        if (uniqueId.toString().startsWith("00000")) {
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                Player target = Bukkit.getPlayer(uniqueId);
                if (target == null || !target.isOnline()) {
                    return;
                }
                if (bedrockPlayers.size() >= 10) {
                    isBedrockRoom = true;
                    return;
                }
                bedrockPlayers.add(uniqueId);
            }, 20 * 10);
        }
    }

    private void godjoin(Player player) {
        if (godMap.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Boolean> entry : godMap.entrySet()) {
            if (Bukkit.getPlayer(entry.getKey()) == null || PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).isGod()) {
                continue;
            }
            Player player1 = Bukkit.getPlayer(entry.getKey());
            if (player1 == null) {
                continue;
            }
            player.hidePlayer(ThePit.getInstance(), player1);
        }
    }

    private void queryUUID(Player player) {
        if (PlayerUtil.isStaff(player)) {
            return;
        }
        if (Util.getMythicItemUUIDs(player).isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            Set<UUID> uuidSet = new HashSet<>();
            List<UUID> uuids = Util.getMythicItemUUIDs(player);
            for (UUID UUID : uuids) {
                if (!uuidSet.add(UUID)) {
                    ThePit.getInstance().info("玩家|" + player.getName() + "|查询到相同UUID物品:" + UUID.toString());
                }
            }
            if (uuidSet.size() < uuids.size()) {
                player.sendMessage(CC.translate("&c你貌似拥有BUG级物品,请截图 + 游戏名称,进天坑交流群找管理员解决"));
                PlayerUtil.denyMovement(player);
            }
        }, 20 * 5);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                    doRespawn(event.getPlayer());
                }
                , 1L);
    }

    @EventHandler
    public void onDamage(PitDamageEvent e) {
        if (e.isShoot()) return;
        if (!(e.getTarget() instanceof Player target)) return;
        Player attacker = e.getAttacker();
        if (attacker == null) {
            return;
        }
        Pair<UUID, Integer> pair = aim.get(attacker.getUniqueId());
        if (pair != null) {
            if (pair.key().equals(target.getUniqueId())) {
                e.getBoostDamage().getAndAdd(0.1 * pair.value());
                aim.remove(attacker.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onSculkShriekerTrigger(BlockReceiveGameEvent event) {
        if (event.getBlock().getType().equals(Material.SCULK_SHRIEKER)) event.setCancelled(true);
    }
}
