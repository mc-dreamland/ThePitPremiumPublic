package cn.charlotte.pit.map.kingsquests.ui.button;

import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public final class KingsQuestsPaper extends Button {

    public static final KingsQuestsPaper INSTANCE = new KingsQuestsPaper();

    private KingsQuestsPaper() {
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return new ItemBuilder(Material.PAPER)
                .name("&e国王任务流程")
                .lore(
                        "&f1.找 &b烘焙大师&f 制作 &d迷你蛋糕",
                        "&f2. 击杀 &c10000 &f名玩家",
                        "&f3. 从事件里获得 &b100点声望",
                        "&e国王任务奖励:",
                        "&f每多 &b1 精通等级 &f奖励&a×2000"
                )
                .build();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
    }
}
