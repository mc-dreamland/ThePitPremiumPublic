package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.enchantment.DoEnch;
import cn.charlotte.pit.enchantment.EnchData;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class eternalHeart extends AbstractPitItem implements DoEnch {

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.NETHER_STAR)
                .name("&d永恒之星")
                .internalName(getInternalName())
                .lore(
                        "",
                        "&7一件十分独特的物品且强大的物品.",
                        "使用后使物品获得以下增益: ",
                        " &7死亡时有 &450% &7概率使神话物品不损失生命值.",
                        "",
                        "&7在神话之井中使用,&c(每一件物品只能使用一次)"
                )
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .removeOnJoin(false)
                .build();
    }

    @Override
    public String getInternalName() {
        return "eternal_heart";
    }

    @Override
    public void ench(EnchData enchData) {
        Player player = Bukkit.getPlayer(enchData.getProfile().getPlayerUUID());
        if (enchData.getMythicItem().isEternal()) {
            CC.send(MessageType.MISC, player, "&e神话之井 &7抱歉,该物品上已有相同嵌入的东西了");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            enchData.setCancel(true);
            return;
        }
        enchData.getMythicItem().setEternal(true);
        enchData.getProfile().setAttachedEnchant(null);
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "永恒之心";
    }
}
