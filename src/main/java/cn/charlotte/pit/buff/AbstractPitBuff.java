package cn.charlotte.pit.buff;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitStackBuffEvent;
import cn.charlotte.pit.util.chat.CC;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @Creator Misoryan
 * @Date 2021/5/9 17:00
 */
public abstract class AbstractPitBuff {
    
    public static List<String> getBuffList(Player player) {
        List<String> buffList = new ArrayList<>();
        PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBuffData().getBuffs().forEach((internalName, buff) -> {
            buff.refreshBuff();
            int tier = buff.getTier();
            if (tier <= 0) return;
            AbstractPitBuff abstractPitBuff = ThePit.getInstance().getBuffFactory().getBuffByInternalName(internalName);
            if (abstractPitBuff == null) return;
            long nearestExpireTime = buff.getNearestExpireTime();
            if (nearestExpireTime == 0L) return;
            long remainingTime = nearestExpireTime - System.currentTimeMillis();
            if (remainingTime > 0)
                buffList.add(String.format("%s&f:%s%s",
                        abstractPitBuff.getDisplayName(),
                        Util.tmin_s.format(remainingTime),
                        tier == 1 ? "" : tier == abstractPitBuff.getMaxTier() ? "(已达到最大阶)" : "(" + tier + "阶)"));
        });
        return buffList;
    }
    
    public abstract String getInternalBuffName();
    
    public abstract String getDisplayName();
    
    public abstract int getMaxTier();
    
    public abstract List<String> getDescription();
    
    public BuffData.Buff getPlayerBuffData(Player player) {
        return PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getBuffData().getBuff(this.getInternalBuffName());
    }
    
    public void stackBuff(Player player, long duration) {
        stackBuff(player, duration, 1, false);
    }
    
    public void stackBuff(Player player, long duration, int times, boolean cancel) {
        if (player == null || duration <= 0 || times <= 0) return;
        PitStackBuffEvent event = new PitStackBuffEvent(player, this, System.currentTimeMillis() + duration);
        event.callEvent();
        if (event.isCancel()) return;
        BuffData.Buff buffData = getPlayerBuffData(player);
        if (buffData == null) return;
        int actualTimes = Math.min(times, Math.max(0, getMaxTier() - buffData.getTier()));
        if (actualTimes == 0) return;
        for (int i = 0; i < actualTimes; i++) buffData.stackBuffData(duration);
        if (!cancel) player.sendMessage(CC.translate(String.format(
                "&9%s! &7你得到了 &c%d&7 层%s效果 &c%d&7 秒",
                getDisplayName(), actualTimes, getDisplayName(), duration / 1000
        )));
    }
    
    public void setBuff(Player player, long duration, int tier) {
        setBuff(player, duration, tier, false);
    }
    
    public void setBuff(Player player, long duration, int tier, boolean cancel) {
        if (player == null || duration <= 0) return;
        tier = Math.max(0, Math.min(tier, getMaxTier()));
        BuffData.Buff buffData = getPlayerBuffData(player);
        if (buffData == null) return;
        
        List<Long> buffList = buffData.getBuff();
        buffList.clear();
        for (int i = 0; i < tier; i++) buffList.add(System.currentTimeMillis() + duration);
        
        if (!cancel) player.sendMessage(CC.translate(String.format(
                "&9%s! &7你的%s效果已被设置为 &c%d&7 层 &c%d&7 秒",
                getDisplayName(), getDisplayName(), tier, duration / 1000
        )));
    }
}