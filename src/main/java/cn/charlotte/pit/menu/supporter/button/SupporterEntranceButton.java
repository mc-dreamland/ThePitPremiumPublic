package cn.charlotte.pit.menu.supporter.button;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.api.PointsAPI;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.impl.PlayerPointsAPIImpl;
import cn.charlotte.pit.menu.supporter.SupporterMenu;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import com.andan.avipsystem.api.VipSystemAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Creator Misoryan
 * @Date 2021/5/4 10:17
 */
public class SupporterEntranceButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        final int price = NewConfiguration.getVipPrice();

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("&7展示您对天坑乱斗游戏的支持,");
        lines.add("&7并解锁专属权益!");
        lines.add("");
        lines.add("&7特权包权益:");
        lines.add("&e◼ &7可开关的 &e✬ &7会员标志显示");
        lines.add("&e◼ &7/events 查看范围两天内的&d事件&7排布表");
        lines.add("&e◼ &7/show 展示手中物品给房间内全部玩家");
        lines.add("&e◼ &7将 &c神&6话&e之&a甲 &7以专属染料进行染色");
        lines.add("&e◼ &7可自选的隐藏自身档案 (无法被他人/view查看)");
        lines.add("&e◼ &7每日交易上限: §e25 -> §c50");
        lines.add("&e◼ &7每日交易金币: §e5万 -> §c25万");
        lines.add("&e◼ &7在线奖励: §e+400%");
        lines.add("&e◼ &7每天免费领取一个 &e行动礼盒&7(必须要到达精通等级1且天坑等级70级才行!)");
        lines.add("&e◼ &7每天免费领取一个 &3时髦的羽毛&7(必须要到达精通等级15且天坑等级70级才行!)");
        lines.add("&e◼ &7末影箱#0 §e+3行");
        lines.add("");
        lines.add("&7VIP权益:");
        lines.add("&e◼ &7VIP额外获得经验/硬币加成(每级§e+5%)");
        if (VipSystemAPI.getVipLevel(player) >= 1) {
            lines.add("&e    ◼ &7当前: &e+" + (5d * VipSystemAPI.getVipLevel(player)) + "%");
        }
        lines.add("");
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        try {

            PointsAPI pointsAPI = ThePit.getInstance().getPointsAPI();
            if (pointsAPI == null) {
                pointsAPI = PlayerPointsAPIImpl.INSTANCE;
            }

            if (profile.isSupporter()) {
                lines.add("&a&l感谢您的购买!");
            } else {
                lines.add("&e§lVIP5 §e及以上 §7自动获取天坑增值包权益");
                lines.add("&7或花费 §e§l6800点券 §c解锁天坑增值包权益");
                lines.add(" ");
                lines.add("&8当增值包权益增加新内容时,");
                lines.add("&8已激活的用户会自动获得新的增值包权益.");
            }
            lines.add(" ");
            if (profile.isSupporter()) {
                lines.add("&e点击调整相关设定!");
            } else {
                lines.add("§c您当前的会员等级: §r" + PlaceholderAPI.setPlaceholders(player, "%avip_chat%"));
                lines.add("&e您当前的点券余额: §a" + pointsAPI.getPoints(player) + (pointsAPI.hasPoints(player, price) ? " §7(§a点击购买§7)" : " §7(§c点券余额不足§7)"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ItemBuilder(Material.BEACON)
                .name("&e天坑乱斗增值包")
                .lore(lines)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        final int price = NewConfiguration.getVipPrice();

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isSupporter()) {
            new SupporterMenu().openMenu(player);
        } else {
            player.getOpenInventory().close();
            if (player.getUniqueId().toString().startsWith("00000")) {
                // PE由于开启动画效果导致弹出form界面时箱子商店关闭的动画还未结束，导致无法正确展示确认购买界面
                // 让我们延迟一会再打开
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), String.format("deliveryconfirm %s pit_rank_pack", player.getName()));
                }, 20L);
            } else {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), String.format("deliveryconfirm %s pit_rank_pack", player.getName()));
            }
//            try {
//
//                final PointsAPI implemenPointsAPI = ThePit.getInstance().getPointsAPI();
//                final PointsAPI pointsAPI;
//                pointsAPI = Objects.requireNonNullElse(implemenPointsAPI, PlayerPointsAPIImpl.INSTANCE);
//
//                boolean haveEnoughPoints = pointsAPI.hasPoints(player, price);
//
//                if (!haveEnoughPoints) { //do nothing if player have not enough points
//                    player.sendMessage("§c您未拥有足够的点券!");
//                    return;
//                }
//                new ConfirmMenu("确认购买: 天坑乱斗增值包 (" + price + " " + NewConfiguration.getPriceName() + ")", confirm -> {
//                    if (confirm) {
//                        //remove player's points
//                        pointsAPI.costPoints(player, price, "pit_rank_pack");
//
//                        profile.setBetterPlayer(true);
//                        CC.boardCast("&6&l惊了! " + profile.getFormattedNameWithRoman() + " &7购买了 &e天坑增值包 &7! &a十分感谢!");
//                        new SupporterMedal().setProgress(profile, 1);
//                    }
//                }, true, 5).openMenu(player);
//            } catch (Exception e) {
//                e.printStackTrace();
//                player.sendMessage(CC.translate("&c获取" + NewConfiguration.getPriceName() + "信息失败,请重试!"));
//            }
        }
    }
}
