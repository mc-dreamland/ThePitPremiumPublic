package cn.charlotte.pit.menu.Help.button.wiki;

import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WikiMenu extends Menu {
    private static final Map<Integer, Button> buttonList = new HashMap<>(
            Map.of(
                    0, getButton("&c如何回城?", "", "&f使用以下指令",
                            "&f/spawn，/respawn，/home，/back", "&f只要不处于战斗模式即可立马回城", ""),
                    1, getButton("&c如何获得金剑?", "", "&f精通等级一且在精通商店里解锁神话附魔师后击杀玩家概率获得"),
                    2, getButton("&c金剑为什么死亡不掉落?", "", "&7没有附魔的过的神话物品都是死亡不掉落的", "&7附魔过后的神话物品会有命数,死亡后会掉命,命数掉光即消失", ""),
                    26,getButton("&c哪里反馈或者疑问?","","&f如有补充或者遗漏,请前往交流群内寻找天坑神秘人进行反馈!","")
            )
    );

    private static Button getButton(String name, String... lore) {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PAPER).name(name).lore(lore).build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                CC.send(player, Arrays.stream(lore).filter(str -> !str.isBlank()).toList());
            }
        };
    }

    @Override
    public String getTitle(Player player) {
        return "天坑小百科";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return buttonList;
    }

    @Override
    public int getSize() {
        return 27;
    }
}
