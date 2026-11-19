package cn.charlotte.pit.menu.heresy.button;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RagePantsCraftButton extends Button {
    @Override
    public ItemStack getButtonItem(Player player) {
        List<String> lines = new ArrayList<>();

        lines.add("&7使用 &5暗聚块 &7去合成一个");
        lines.add("&4暴怒之甲, &7使其可以有力对抗特殊附魔.");
        lines.add("");

        lines.add("&7合成耗费: &517 暗聚块");
        lines.add("");
        lines.add("&7合成物品: &c新鲜的暴怒之甲");
        lines.add("");
        if (InventoryUtil.getAmountOfItem(player, PitItems.CHUNK_OF_VILE.getInternalName()) < 17) {
            lines.add("&5你没有足够的暗聚块!");
        } else if (InventoryUtil.isInvFull(player.getInventory())) {
            lines.add("&c你的背包已满!");
        } else {
            lines.add("&e点击合成!");
        }
        return new ItemBuilder(Material.LEATHER_LEGGINGS)
                .name("&4暴怒之甲")
                .setLetherColor(MythicColor.RAGE.getLeatherColor())
                .lore(lines)
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        boolean unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.PURE_RAGE);

        if (!unlocked) return;

        if (InventoryUtil.isInvFull(player.getInventory())) {
            return;
        }
        if (InventoryUtil.getAmountOfItem(player, PitItems.CHUNK_OF_VILE.getInternalName()) < 17) {
            return;
        }
        if (InventoryUtil.removeItem(player, PitItems.CHUNK_OF_VILE.getInternalName(), 17)) {
            final MythicLeggingsItem item = new MythicLeggingsItem();
            item.setColor(MythicColor.RAGE);
            player.getInventory().addItem(item.toItemStack());
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            CC.boardCast("&4&l愤怒!!! " + profile.getFormattedName() + " &7成功合成了 &4暴怒之甲.");
        }
    }
}
