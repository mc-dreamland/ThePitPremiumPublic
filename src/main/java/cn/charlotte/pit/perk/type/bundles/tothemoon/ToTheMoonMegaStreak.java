package cn.charlotte.pit.perk.type.bundles.tothemoon;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitStreakKillChangeEvent;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoRegister
@BotKillEffect(BotKillEffect.Kind.REWARD)
public class ToTheMoonMegaStreak extends TypedPerk<PerksBundlesEnum> implements MegaStreak, Listener {

    public static final Map<UUID, Double> cache = new HashMap<>();
    private final DecimalFormat format = new DecimalFormat("###,###,###,###");
    private final DecimalFormat decimalFormat = new DecimalFormat("##.##");

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "&b月球之旅";
    }

    @Override
    public Material getIcon() {
        return Material.END_STONE;
    }

    @Override
    public double requireCoins() {
        return 50000.0;
    }

    @Override
    public double requireRenown(int level) {
        return 150.0;
    }

    @Override
    public int requirePrestige() {
        return 14;
    }

    @Override
    public int requireLevel() {
        return 80;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> desc = new ArrayList<>();
        desc.add("&7激活要求连杀数: &c100 连杀");
        desc.add("");
        desc.add("&7当激活时: ");
        desc.add("  &a◼ &7立刻获得2000的存储经验&7.");
        desc.add("  &a◼ &7击杀额外获得&b +20%经验&7.");
        desc.add("  &a◼ &7每10连杀额外获得&b +1 基础经验 &c(上限30点基础经验)&7.");
        desc.add("  &a◼ &7每次击杀增加最大经验上限&b 100经验&7.");
        desc.add("");
        desc.add("&7但是: ");
        desc.add("  &c◼ &7每20击杀将额外承受&c +10%&7 的伤害.");
        desc.add("&7       (从100连杀开始)");
        desc.add("  &c◼ &7每20击杀将额外承受&c +0.1❤&7 的&f真实&7伤害.");
        desc.add("&7       (从200连杀开始)");
        desc.add("");
        desc.add("&7连杀期间: ");
        desc.add("  &b◼ &7复制并存储你获得的&b经验&7.");
        desc.add("");
        desc.add("&7当死亡时: ");
        desc.add("  &b◼ &7获得你存储的&b经验&7, 并且");
        desc.add("&7  额外获得超过100击杀部分的击杀数的&b0.005&7倍 &b经验");
        desc.add("&7  , 上限为&b1倍");
        return desc;
    }

    @Override
    public List<String> getDescriptionInRenownShop(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7包括以下连杀天赋:");
        lines.add("&8- &9超级连杀者");
        lines.add("&8- &9黄金助推器");
        lines.add("&8- &9经验助推器");
        lines.add(" ");
        lines.add("&7超级连杀天赋: &b月球之旅");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.MEGA_STREAK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.STREAK;
    }

    @Override
    public void onPerkActive(Player player) {
    }

    @Override
    public void onPerkInactive(Player player) {
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Player killer = event.getKiller();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(killer.getUniqueId());
        if (profile == null || !PlayerUtil.hasEquipPerk(killer, PerksBundlesEnum.TO_THE_MOON.getPerk())) return;

        if (profile.getStreakKills() >= 100) {
            double current = cache.getOrDefault(killer.getUniqueId(), 0.0);
            cache.put(killer.getUniqueId(), current + event.getResourceManager().calculateEXP());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStreak(PitStreakKillChangeEvent event) {
        if (event.isBotKill()) return;
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (!PlayerUtil.hasEquipPerk(player, this)) {
            return;
        }

        if (event.getFrom() < 100 && event.getTo() >= 100) {
            cache.put(player.getUniqueId(), 2000.0);
            CC.boardCast(MessageType.COMBAT,
                    "&c&l超级连杀! " + event.getPlayerProfile().getFormattedNameWithRoman() + " &7激活了 &b&l月球之旅 &7!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
            }
        }
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        int streak = (int) profile.getStreakKills();
        if (streak >= 100) {
            resourceManager.addExpModifier(1.2, "奔月");
            resourceManager.setMaxExp(resourceManager.getMaxExp() + 100 * (streak - 100));
            resourceManager.addBaseExp(Math.min((streak - 100D) / 10, 30D), "奔月");
        }
    }

    @Override
    public void handlePlayerDamaged(int enchantLevel, Player myself, Entity attacker, double damage,
                                    AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile == null) return;

        double streakKills = profile.getStreakKills();

        if (streakKills >= 100) {
            double extraBoost = Math.floor((streakKills - 100) / 20.0);
            boostDamage.addAndGet(extraBoost * 0.2);
        }

        if (streakKills >= 200) {
            double extraTrue = Math.floor((streakKills - 200) / 20.0);
            finalDamage.addAndGet(extraTrue * 0.1);
        }
    }

    @Override
    public void handlePlayerBeKilledByEntity(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId());
        if (profile == null) return;

        double streakKills = profile.getStreakKills();
        if (streakKills >= 100 && ThePit.getInstance().getEventFactory().getActiveEpicEvent() == null) {
            Double stored = cache.remove(myself.getUniqueId());
            if (stored != null && stored > 0) {
                double give = Math.min(((streakKills - 100) * 0.005) * stored + stored, stored * 2);
                double multiple = (give - stored) / stored;
                profile.setExperience(profile.getExperience() + give);
                profile.applyExperienceToPlayer(myself);
                myself.sendMessage(CC.translate(
                        "&b&l月球之旅! &7你挣取了 &b+" + format.format(give) + "经验&7 在这次连杀中. (&b" +
                                decimalFormat.format(multiple) + "x &7倍率)"));
            }
        }
    }

    @Override
    public int getStreakNeed() {
        return 100;
    }

    @Override
    public void handleTick(int enchantLevel, Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isInArena() && !PitBotManager.keepTrainingBotStreak(player, profile)) {
            profile.setStreakKills(0);
        }
        PlayerUtil.addEffectStreak(player, new PotionEffect(PotionEffectType.GLOWING, 86400 * 20, 0), getStreakNeed());
    }

    @Override
    public int loopTick(int enchantLevel) {
        return 3 * 20;
    }
}
