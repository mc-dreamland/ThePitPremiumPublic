package cn.charlotte.pit.activity.genes;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityData;
import cn.charlotte.pit.activity.IActivity;
import cn.charlotte.pit.activity.genes.data.GenesPlayerData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.mail.MailBuilder;
import cn.charlotte.pit.event.PitAssistEvent;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@Slf4j
public class GenesListener implements IActivity, Listener {

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    @Override
    public void end(ActivityData activityData) {
        HandlerList.unregisterAll(this);
        for (UUID uuid : activityData.getUuidList()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player player = Bukkit.getPlayer(uuid);
                    PlayerProfile profile;
                    if (player != null && player.isOnline())
                        profile = PlayerProfile.getPlayerProfileByUuid(uuid);
                    else
                        profile = PlayerProfile.getOrLoadPlayerProfileByUuid(uuid);

                    if (profile == null)
                        return;

                    try {
                        Mail mail = new MailBuilder()
                                .coins(50000.0 + profile.getActivityPlayerData().getGenesisData().getPoints() * 100)
                                .exp(50000.0 + profile.getActivityPlayerData().getGenesisData().getPoints() * 100)
                                .renown(5 + Math.max(0, (int) (profile.getActivityPlayerData().getGenesisData().getPoints() / 1000)))
                                .title("&e【活动结束发放礼品】")
                                .content("&f你参与了 天使恶魔 活动!", "&f这是活动结束奖励!")
                                .build();
                        Util.sendMail(uuid, mail, "&e邮箱 &f你有一封新的信封!");
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            }.runTaskAsynchronously(ThePit.getInstance());
        }
        CC.boardCast(MessageType.ACTIVITY, "&b天使&c恶魔 &7活动结束了");
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        PlayerProfile profile = Util.getPitProfile(event.getKiller());
        if (profile.getActivityPlayerData().getGenesisData().getTeam() == GenesisTeam.NONE) return;
        if (!(event.getTarget() instanceof Player targeter)) return;
        GenesPlayerData killData = profile.getActivityPlayerData().getGenesisData();
        GenesPlayerData targeterData = Util.getPitProfile(targeter).getActivityPlayerData().getGenesisData();
        int potions = killData.getTeam() != targeterData.getTeam() ? 2 : 1;
        if (killData.isBetter()) potions++;
        event.getMsg().add(String.format(" +%s%s活动点数", killData.getTeam() == GenesisTeam.ANGEL ? "&b" : "&c", potions));
        killData.addPoints(potions);
        if (killData.getTier() < 8) return;
        if (killData.getTeam() == GenesisTeam.ANGEL)
            event.getResourceManager().addExpModifier(1 + (double) ((killData.getTier() - 7) * 8) / 100, "天使阵营加成");
        else
            event.getResourceManager().addGoldModifier(1 + (double) ((killData.getTier() - 7) * 8) / 100, "恶魔阵营加成");
    }

    @EventHandler
    public void onDamage(PitDamageEvent event) {
        Player attacker = event.getAttacker();
        if (attacker == null) return;
        PlayerProfile profile = Util.getPitProfile(attacker);
        if (profile.getActivityPlayerData().getGenesisData().getTier() < 3) return;
        if (!(event.getTarget() instanceof Player targeter)) return;
        GenesPlayerData attackData = profile.getActivityPlayerData().getGenesisData();
        GenesPlayerData targeterData = Util.getPitProfile(targeter).getActivityPlayerData().getGenesisData();
        if (attackData.getTeam() != targeterData.getTeam() && attackData.getTier() >= 1)
            event.setDamage(event.getDamage() + 1.0D);
        if (attackData.getTeam() == GenesisTeam.ANGEL && attackData.getTier() >= 5) {
            for (ItemStack is : targeter.getInventory().getArmorContents()) {
                if (is != null && is.getType().name().contains("LEATHER")) {
                    event.setDamage(event.getDamage() + 0.5D);
                    break;
                }
            }
        }
        if (attackData.getTeam() == GenesisTeam.DEMON && attackData.getTier() >= 5) {
            for (ItemStack is : targeter.getInventory().getArmorContents()) {
                if (is != null && is.getType().name().contains("DIAMOND")) {
                    event.setDamage(event.getDamage() + 0.5D);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onAss(PitAssistEvent event) {
        PlayerProfile profile = Util.getPitProfile(event.getAssist());
        if (profile.getActivityPlayerData().getGenesisData().getTier() < 8) return;
        GenesPlayerData killData = profile.getActivityPlayerData().getGenesisData();
        if (killData.getTeam() == GenesisTeam.ANGEL)
            event.getResourceManager().addExpModifier(1 + (double) ((killData.getTier() - 7) * 8) / 100, "天使阵营加成");
        else
            event.getResourceManager().addGoldModifier(1 + (double) ((killData.getTier() - 7) * 8) / 100, "恶魔阵营加成");
    }
}