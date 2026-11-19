package cn.charlotte.pit.listener;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.config.NewConfiguration.IntRange;
import cn.charlotte.pit.util.chat.CC;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class MythicMobListener implements Listener {

    public static final MythicMobListener INSTANCE = new MythicMobListener();

    private MythicMobListener() {
    }

    private static double randomFromRange(IntRange range) {
        int min = range.getFirst();  // 含
        int max = range.getLast();   // 含
        int value = ThreadLocalRandom.current().nextInt(min, max + 1);
        return value;
    }

    @EventHandler
    public void onMMDead(MythicMobDeathEvent event) {
        ActiveMob mob = event.getMob();
        Entity beKilled = mob.getEntity().getBukkitEntity();

        if (!(beKilled instanceof LivingEntity)) {
            return;
        }

        String internalName = mob.getType().getInternalName();
        if (PitBotManager.isBot(beKilled) || PitBotManager.isBotType(internalName)) return;

        // 假设该方法返回 Map<String, NewConfiguration.MythicMobsConf>
        Map<String, NewConfiguration.MythicMobsConf> mobConfigs = NewConfiguration.getMythicMobs();
        if (mobConfigs == null) {
            return;
        }

        NewConfiguration.MythicMobsConf cfg = mobConfigs.get(internalName);
        if (cfg == null) {
            return;
        }

        Object killerObj = event.getKiller();
        if (!(killerObj instanceof Player killer)) {
            return;
        }

        /*
        CombatListener.INSTANCE.handleKill(
            killer,
            killer.getPitProfile(),
            beKilled,
            PlayerProfile.NONE_PROFILE
        );
        */

        // coins
        IntRange coinsRange = (cfg.coinsRange != null) ? cfg.coinsRange : new IntRange(0, 0);
        double coinsToAdd = randomFromRange(coinsRange);

        // exp
        IntRange expRange = (cfg.expRange != null) ? cfg.expRange : new IntRange(0, 0);
        double expToAdd = randomFromRange(expRange);

        // 加钱加经验
        var profile = Util.getPitProfile(killer);
        profile.setCoins(profile.getCoins() + coinsToAdd);
        profile.setExperience(profile.getExperience() + expToAdd);

        // 提示消息
        String killMessage = cfg.killMessage;
        if (killMessage != null) {
            String msg = killMessage
                    .replace("{mobName}", mob.getType().getDisplayName().toString())
                    .replace("{exp}", Double.toString(expToAdd))
                    .replace("{coin}", Double.toString(coinsToAdd));
            killer.sendMessage(CC.translate(msg));
        }

        // 如需全服广播，取消注释并使用 cfg.killBroadcast / cfg.killBroadcastMessage
        // if (cfg.killBroadcast && cfg.killBroadcastMessage != null) {
        //     String bmsg = cfg.killBroadcastMessage
        //         .replace("{player}", killer.getName())
        //         .replace("{mobName}", mob.getType().getDisplayName().toString())
        //         .replace("{exp}", Double.toString(expToAdd))
        //         .replace("{coin}", Double.toString(coinsToAdd));
        //     Bukkit.broadcastMessage(CC.translate(bmsg));
        // }
    }
}
