package cn.charlotte.pit.perk.type.bundles.beastmode;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Araykal
 * @since 2025/1/16
 */
@AutoRegister
public class MonsterKillStreak extends TypedPerk<PerksBundlesEnum> implements Listener, Streakable {

    public static final HashMap<UUID, Integer> times = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "怪物";
    }

    @Override
    public Material getIcon() {
        return Material.LEGACY_LEASH;
    }

    @Override
    public double requireCoins() {
        return 10000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 40;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> list = new ArrayList<>();
        list.add("&7此天赋每 &c25 连杀 &7触发一次.");
        list.add(" ");
        list.add("&7触发时:");
        list.add("  &a▶ &7提升血量上限 &c1❤ &7(最高提升2次)");
        return list;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.KILL_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.KILL_STREAK;
    }


    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.getExtraMaxHealth().remove(getInternalPerkName());
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
        times.remove(player.getUniqueId());
    }

    @Override
    public void handleRespawn(int enchantLevel, Player myself) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        profile.getExtraMaxHealth().remove(getInternalPerkName());
        myself.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
        times.remove(myself.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onStreak(PitStreakKillChangeEvent event) {
        Player myself = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (myself == null || !myself.isOnline()) {
            return;
        }
        if (!PlayerUtil.hasEquipPerk(myself, this)) {
            return;
        }
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        //trigger check (every X streak)
        int streak = 25;
        if (Math.floor(event.getFrom()) % streak != 0 && Math.floor(event.getTo()) % streak == 0) {
            Integer heart = times.get(myself.getUniqueId());
            if (heart == null) {
                heart = 0;
            }
            if (heart < 2) {
                profile.getExtraMaxHealth().put(getInternalPerkName(), 2.0 * (heart + 1));
                myself.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
//                myself.setHealth(myself.getHealth() + 2);
                times.put(myself.getUniqueId(), heart + 1);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            Player player = event.getPlayer();
            times.put(player.getUniqueId(), 0);
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            profile.getExtraMaxHealth().remove(getInternalPerkName());
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
        }, 20);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player myself = event.getPlayer();
        times.remove(myself.getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player myself = event.getEntity();
        times.remove(myself.getUniqueId(), 0);

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        profile.getExtraMaxHealth().remove(getInternalPerkName());
        myself.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(profile.getMaxHealth());
    }

    @Override
    public int getActiveStreak() {
        return 25;
    }
}
