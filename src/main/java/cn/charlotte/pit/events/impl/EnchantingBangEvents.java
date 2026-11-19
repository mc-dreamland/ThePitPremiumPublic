package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitEnchBangEvent;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class EnchantingBangEvents implements INormalEvent, Listener {

    private Map<UUID, Boolean> glowingPlayers = new HashMap<>();
    private BukkitTask inactiveTask;
    private Set<UUID> killerSet = new HashSet<>();

    @Override
    public String getEventInternalName() {
        return "enchantingBangEvent";
    }

    @Override
    public String getEventName() {
        return "附魔爆发";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> &d附魔爆发 &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 随机选择3名玩家进行发光");
        lore.add("&7-  &a发光玩家:神话装备掉落概率 + 150%");
        lore.add("&7-  &c击杀发光玩家获得随机神话装备,助攻玩家按助攻比例(只限高于20%)概率获得随机神话装备");
        lore.add("");
        return lore;
    }

    @Override
    public int requireOnline() {
        return (NewConfiguration.getEventOnlineRequired().get(getEventInternalName()) == null ?
                20 : NewConfiguration.getEventOnlineRequired().get(getEventInternalName()));
    }

    @Override
    public void onActive() {
        List<UUID> arenaPlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = Util.getPitProfile(player);
            if (profile.isInArena()) {
                arenaPlayers.add(player.getUniqueId());
            }
        }

        if (arenaPlayers.isEmpty()) {
            CC.boardCast("&d附魔爆发 &c没人被选上");
            ThePit.getInstance().getEventFactory().inactiveEvent(this);
            return;
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getEventData().callHistoricalEvents(getEventName(), 1);
        });

        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());

        Collections.shuffle(arenaPlayers);
        int playerCount = Math.min(3, arenaPlayers.size());
        arenaPlayers = arenaPlayers.subList(0, playerCount);
        glowingPlayers = new HashMap<>();
        killerSet = new HashSet<>();
        arenaPlayers.forEach(p -> glowingPlayers.put(p, false));


        for (UUID playerUuid : glowingPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.GLOWING, 20 * 5 * 60, 0));
            }
        }

        CC.boardCast("&d&l附魔爆发!\n&7随机挑选" + glowingPlayers.size() + "名玩家进行发光\n&7击杀发光玩家获得随机神话装备\n&7携带发光效果的玩家获得 +150%的神话掉落概率");
        CC.boardCast("&a幸运玩家:");

        for (UUID playerUuid : glowingPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                CC.boardCast(" &a-" + Util.getPitProfile(player).getFormattedName());
            }
        }

        inactiveTask = Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            ThePit.getInstance().getEventFactory().inactiveEvent(this);
        }, 5 * 20L * 60L);
    }

    @Override
    public void onInactive() {
        if (inactiveTask != null && !inactiveTask.isCancelled()) {
            inactiveTask.cancel();
            inactiveTask = null;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.GLOWING);
        }
        new PitEnchBangEvent(glowingPlayers.keySet().stream().toList(), killerSet);
        glowingPlayers.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }

        if (glowingPlayers.containsKey(target.getUniqueId()) &&
                isActive()
                && !glowingPlayers.get(target.getUniqueId())) {

            glowingPlayers.put(target.getUniqueId(), true);

            ItemStack mythicItem = RandomUtil.helpMeToChooseOne(
                    new MythicSwordItem().toItemStack(),
                    new MythicBowItem().toItemStack(),
                    new MythicLeggingsItem().toItemStack()
            );

            if (mythicItem != null) {
                event.getKiller().getInventory().addItem(mythicItem);
                log.info("{}", ItemOperationJson.toJson(
                        ItemOperationJson.mythicGranted(event.getKiller(), "ENCHANTING_BANG_KILL", mythicItem)
                ));
            }

            killerSet.add(event.getKiller().getUniqueId());

            if (glowingPlayers.values().stream().allMatch(killed -> killed)) {
                if (inactiveTask != null && !inactiveTask.isCancelled()) {
                    inactiveTask.cancel();
                    inactiveTask = null;
                }
                CC.boardCast("&d附魔爆发 &a所有发光玩家都被击杀了!事件提前结束!");
                ThePit.getInstance().getEventFactory().inactiveEvent(this);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(!isActive()) return;
        glowingPlayers.remove(event.getPlayer().getUniqueId());
        if (!glowingPlayers.values().stream().allMatch(killed -> killed)) return;
        if (inactiveTask != null && !inactiveTask.isCancelled()) {
            inactiveTask.cancel();
            inactiveTask = null;
        }
        CC.boardCast("&d附魔爆发 &a所有发光玩家都被击杀了!事件提前结束!");
        ThePit.getInstance().getEventFactory().inactiveEvent(this);
    }

}
