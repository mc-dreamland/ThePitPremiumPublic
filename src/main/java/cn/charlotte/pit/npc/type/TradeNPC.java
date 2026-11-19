package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.temp.TradeRequest;
import cn.charlotte.pit.game.runnable.TradeMonitorRunnable;
import cn.charlotte.pit.menu.trade.TradeManager;
import cn.charlotte.pit.menu.trade.npc.tradeMenuOfNpc;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TradeNPC extends SkinNPC {

    public static List<TradeRequest> getPendingRequests(Player player) {
        return TradeMonitorRunnable.getPendingRequests(player.getUniqueId());
    }

    @Override
    public String getNpcInternalName() {
        return "tradeNPC";
    }

    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e%pitnpcholo_" + getNpcInternalName() + "_0%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_1%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_2%"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        List<String> list = new ArrayList<>(8);
        list.add("&e&l交易员");
        if (!TradeManager.isTradingEnabled()) {
            list.add("&c" + TradeManager.TRADE_DISABLED_MESSAGE);
            return list;
        }
        PlayerProfile profile = Util.getPitProfile(player);
        if (profile.getLevel() < 60) {
            list.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &c时解锁");
            return list;
        }
        List<TradeRequest> pendingRequests = getPendingRequests(player);
        if (pendingRequests.isEmpty()) {
            list.add("&f&l右键查看");
            return list;
        }
        list.add(String.format("&a你有 %s 个请求需要处理", pendingRequests.size()));
        return list;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getTradeNPC();
    }

    @Override
    public Skin getNpcSkin() {
        return null;
    }

    @Override
    public void handlePlayerInteract(Player player) {
        if (!TradeManager.ensureTradingEnabled(player)) {
            return;
        }
        PlayerProfile profile = Util.getPitProfile(player);
        if (profile.getLevel() < 60) {
            player.sendMessage(CC.translate("&c&l等级不足! &7交易员在 " + LevelUtil.getLevelTag(profile.getPrestige(), 60) + " &7时解锁."));
            return;
        }
        new tradeMenuOfNpc(getPendingRequests(player)).openMenu(player);
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
