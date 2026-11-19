package cn.charlotte.pit.menu.gem.button;

import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.menu.Button;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class KillCounterGemButton extends Button {

    private final ItemStack gem;
    private final int slt;
    private final ItemStack item;

    @Override
    public ItemStack getButtonItem(Player player) {
        return item;
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
        IMythicItem mythicItem = FuncsKt.toMythicItem(item);
        mythicItem.setKillCounterGem(0);
        mythicItem.getEnchantmentRecords().add(new EnchantmentRecord(
                player.getName(),
                "KillCounterGem",
                System.currentTimeMillis()));
        ItemStack ofNew = mythicItem.toItemStack();
        player.getInventory().setItem(slt, ofNew);
        // It's unsafe to close Inventory in click events
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.5f, 1.5f);
        player.sendMessage(CC.translate("&a使用成功!"));
        gem.setAmount(gem.getAmount() - 1);
    }
}
