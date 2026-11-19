package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.util.ParticleBuilder;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DamagePlusEvents implements INormalEvent, Listener, IScoreBoardInsert {

    private final Map<String, EventData> killMap = new HashMap<>();
    // 添加一个映射表来记录每个玩家击杀过的玩家，避免重复计数
    private final Map<UUID, Set<UUID>> playerKillRecords = new HashMap<>();
    private final int KILL_COUNT_THRESHOLD = 15;
    private boolean isActive = false;

    @Override
    public String getEventInternalName() {
        return "hunt";
    }

    @Override
    public String getEventName() {
        return "猎杀";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().get(getEventInternalName());
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &e猎杀 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 持续5分钟全体增益");
        lore.add("&7- 获得速度II+跳跃II");
        lore.add("&7- 每15击杀获1声望");
        lore.add("&7- 上限5声望(75击杀)");
        lore.add("");
        return lore;
    }

    @Override
    public void onActive() {
        isActive = true;
        CC.boardCast("&c&l猎杀!\n&7五分钟内获得：\n  &b速度 x2\n  &a跳跃提升 x2\n&7效果\n&7每击杀15名不同玩家将获得 &6声望 +1 &7最高可获得5声望."); // 修改为跳跃x2
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        runEffectTask();
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });
        // 五分钟后自动结束
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            ThePit.getInstance().getEventFactory().inactiveEvent(this);
            isActive = false;
        }, 5 * 20L * 60L);
    }

    private void runEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isActive) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerEffect(player);
                        new ParticleBuilder(player.getLocation().clone().add(0.0, 2.0, 0.0), ParticleTypes.FLAME)
                                .setCount(3)
                                .play();
                    }
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        removeEffect(player);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 20L);
    }

    @Override
    public void onInactive() {
        HandlerList.unregisterAll(this);
        isActive = false;
        killMap.clear();
        playerKillRecords.clear();
        CC.boardCast("&c&l猎杀! &7已结束");
    }

    private void playerEffect(Player player) {
        PlayerUtil.addEffect(player, PotionEffectType.SPEED.createEffect(114514, 1));
        PlayerUtil.addEffect(player, PotionEffectType.JUMP_BOOST.createEffect(114514, 1)); // 修改为跳跃II (等级1)
    }

    private void removeEffect(Player player) {
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (isActive) {
            event.getPlayer().sendMessage(CC.translate("&c&l猎杀!\n&7五分钟内获得：\n  &b速度 x2\n  &a跳跃提升 x2\n&7效果\n&7每击杀15名不同玩家将获得 &6声望 +1"));
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        if (isActive) {
            Player killer = event.getKiller();
            Player victim = (Player) event.getTarget();
            if (victim == null) {
                return;
            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(killer.getUniqueId());
            String playerName = killer.getName();

            Set<UUID> killerRecords = playerKillRecords.computeIfAbsent(killer.getUniqueId(), k -> new HashSet<>());
            if (killerRecords.contains(victim.getUniqueId())) {
                return;
            }
            killerRecords.add(victim.getUniqueId());

            EventData eventData = killMap.computeIfAbsent(playerName, k -> new EventData());

            if (eventData.prestige >= 5) {
                return;
            }

            eventData.kill++;

            if (eventData.kill % KILL_COUNT_THRESHOLD == 0) {
                eventData.prestige++;
                if (profile != null) {
                    profile.changeRenown(profile.getRenown() + 1);
                    profile.kingsQuestsData.checkUpdate();
                    if (profile.kingsQuestsData.getAccepted()) {
                        if (!profile.kingsQuestsData.getCompleted()) {
                            profile.kingsQuestsData.setCollectedRenown(profile.kingsQuestsData.getCollectedRenown() + 1);
                        }
                    }
                }
                killer.sendMessage(CC.translate("&c&l猎杀! &6声望 +1"));
                killer.playSound(killer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
            }
        }
    }

    public int getKillTo(Player player) {
        EventData eventData = killMap.getOrDefault(player.getName(), new EventData());
        return KILL_COUNT_THRESHOLD - (eventData.kill % KILL_COUNT_THRESHOLD);
    }

    @Override
    public List<String> insert(Player player) {
        List<String> list = new ArrayList<>();
        if (player != null) {
            EventData eventData = killMap.getOrDefault(player.getName(), new EventData());
            list.add("&f所需击杀: &b" + getKillTo(player));
            list.add("&f已击杀: &e" + eventData.kill + "&7/&e15");
            list.add("&f获得声望: &e" + eventData.prestige);
        }
        return list;
    }

    public static class EventData {
        public int kill = 0;
        public int prestige = 0;

        public EventData() {
        }
    }
}