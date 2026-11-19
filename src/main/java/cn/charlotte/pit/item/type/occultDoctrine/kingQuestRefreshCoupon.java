package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class kingQuestRefreshCoupon extends AbstractPitItem implements IPlayerInteract {

    /**
     * AbstractMap.SimpleEntry<Cooldown,Cooldown>
     * 1. 指首先的冷却
     * 2. 指冷却过后的时间
     */
    private final HashMap<UUID, Map.Entry<Cooldown, Cooldown>> map = new HashMap<>(8);

    @Override
    public String getInternalName() {
        return "king_quest_refresh_coupon";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e国王任务刷新券";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.PAPER)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .lore(
                        "",
                        "&7右键使用,使用后重置你的国王任务进度.",
                        "&c(温馨提示! 无法反悔! 请想好再使用!!!)",
                        ""
                ).build();
    }


    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        Map.Entry<Cooldown, Cooldown> pair = map.get(player.getUniqueId());
        if (pair == null || pair.getValue().hasExpired()) {
            CC.send(MessageType.MISC, player, "&7你想好了吗? 已提供了3秒的冷静时间,请等待冷静时间过后才能继续选择", "&7冷静期结束后会给你30秒选择是否重置国王任务,30秒后则自动取消");
            map.put(player.getUniqueId(), Map.entry(new Cooldown(3L, TimeUnit.SECONDS), new Cooldown(33L, TimeUnit.SECONDS)));
        } else if (!pair.getKey().hasExpired()) {
            CC.send(MessageType.MISC, player, String.format("&c你还处于冷静期! %s", Util.getCooldownActionText(pair.getKey())));
        } else {
            map.remove(player.getUniqueId());
            PlayerProfile profile = Util.getPitProfile(player);
            profile.getKingsQuestsData().setAccepted(false);
            profile.getKingsQuestsData().setCompleted(false);
            profile.getKingsQuestsData().setKilledPlayer(0);
            profile.getKingsQuestsData().setCollectedRenown(0);
            CC.send(MessageType.MISC, player, "&7你的国王任务已经重置");
            PlayerUtil.takeOneItem(player,hand);
        }
        event.setCancelled(true);
    }
}
