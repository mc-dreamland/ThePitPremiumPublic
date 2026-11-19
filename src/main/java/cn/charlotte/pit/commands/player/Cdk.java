package cn.charlotte.pit.commands.player;


import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.CDKData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.util.chat.CC;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("cdk")
public class Cdk extends BaseCommand {
    private final PitCommands pitCommands;

    public Cdk(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void cdk(Player player, String cdk) {
        final CDKData data = CDKData.getCachedCDK().get(cdk);
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (data == null) {
            player.sendMessage(CC.translate("&c错误的CDK,请检查大小写是否一致!"));
            return;
        }

        if (data.getLimitPermission() != null) {
            if (!player.hasPermission(data.getLimitPermission())) {
                player.sendMessage(CC.translate("&c你没有领取这个CDK的权限!"));
                return;
            }
        }

        if (data.getLimitPrestige() <= 0 && profile.getPrestige() <= 0 && profile.getLevel() < data.getLimitLevel()) {
            player.sendMessage(CC.translate("&c你不满足领取条件!"));
            return;
        }
        if (data.getLimitPrestige() > 0 && profile.getPrestige() < data.getLimitPrestige()) {
            player.sendMessage(CC.translate("&c精通等级不满足要求哦,快去升级吧!"));
            return;
        }
        if (data.getLimitPrestige() > 0 && data.getLimitLevel() > 0 && profile.getPrestige() == data.getLimitPrestige() && profile.getLevel() < data.getLimitLevel()) {
            player.sendMessage("§c似乎还差一点等级就能领取到了...加油!");
            return;
        }

        final long now = System.currentTimeMillis();
        if (now > data.getExpireTime()) {
            player.sendMessage(CC.translate("&c该CDK已过期!"));
            return;
        }

        if (CDKData.isLoading()) {
            player.sendMessage(CC.translate("&c系统繁忙，请稍后再试!"));
            return;
        }

        if (data.getLimitClaimed() != -1 && data.getClaimedPlayers().size() >= data.getLimitClaimed()) {
            player.sendMessage(CC.translate("&c领取已达上限,下次快一点哦!"));
            return;
        }

        final boolean added = profile.getUsedCdk().add(cdk);
        if (added) {
            final Mail mail = new Mail();
            mail.setExpireTime(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
            mail.setCoins(data.getCoins());
            mail.setExp(data.getExp());
            mail.setRenown(data.getRenown());
            mail.setItem(data.getItem());
            mail.setSendTime(System.currentTimeMillis());
            mail.setTitle("&e【奖励】兑换码兑换奖励");
            mail.setContent("&f亲爱的玩家: 请查收通过兑换码获得的奖励");

            data.getClaimedPlayers().add(player.getName());
            data.active();

            profile.getMailData().sendMail(mail);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0.8F);
            player.sendMessage(CC.translate("&a领取成功! 请在邮件NPC处领取奖励!"));
        } else {
            player.sendMessage(CC.translate("&c你已经领取过这个CDK了!"));
        }
    }


}
