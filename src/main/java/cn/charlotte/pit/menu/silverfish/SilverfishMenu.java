package cn.charlotte.pit.menu.silverfish;

import cn.charlotte.pit.menu.silverfish.button.BuyBoneMealButton;
import cn.charlotte.pit.menu.silverfish.button.BuyMagnetButton;
import cn.charlotte.pit.menu.silverfish.button.BuyMythicLeggingsItemButton;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.Map;

public class SilverfishMenu extends Menu {

    private static final RandomList<String> randomList = new RandomList<>(
            Map.entry("今天有什么好货?", 1),
            Map.entry("有出牛奶吗?", 1),
            Map.entry("我怀念之前的建造者", 1),
            Map.entry("如果嫌弃下水道找东西太慢，可以换个下水道磁石", 1)
    );

    private static final Map<Integer, Button> buttons = Map.of(
            11, new BuyMythicLeggingsItemButton(),
            13, new BuyMagnetButton(),
                15,new BuyBoneMealButton()
    );
    @Override
    public String getTitle(Player player) {
        return String.format("下水道之鱼: %s", randomList.random());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        return buttons;
    }

    @Override
    public int getSize() {
        return 27;
    }
}
