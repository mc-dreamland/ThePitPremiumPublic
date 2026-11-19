package cn.charlotte.pit.menu.genesis;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.menu.genesis.button.GenesisPerkButton;
import cn.charlotte.pit.activity.genes.menu.GenesisStatusButton;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/4 10:02
 */

public class GenesisMenu extends Menu {

    private final GenesisTeam genesisTeam;
    private final boolean bypassed = false;

    public GenesisMenu(GenesisTeam genesisTeam) {
        this.genesisTeam = genesisTeam;
    }

    @Override
    public String getTitle(Player player) {
        if (genesisTeam == GenesisTeam.ANGEL) {
            return "天使";
        }
        if (genesisTeam == GenesisTeam.DEMON) {
            return "恶魔";
        }
        return "未知NPC";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        Map<Integer, Button> button = new HashMap<>();
        button.put(29, new GenesisStatusButton(genesisTeam, profile));
        button.put(11, createButton());
        if (ThePit.getInstance().getPitConfig().isGenesisEnable() || bypassed) {
            button.put(15, new GenesisPerkButton(genesisTeam, profile, (profile.getGenesisData().getTier() >= 7 ? 7 : profile.getGenesisData().getTier() + 1)));
        }
        return button;
    }

    @Override
    public int getSize() {
        return 5 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public boolean isUpdateAfterClick() {
        return true;
    }

    private Button createButton() {
        return new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.CHEST).name("阵营商店").lore("", "&b将你的点数换成你所需要的吧", "").build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                if (Util.getPitProfile(player).getGenesisData().getTeam() == GenesisTeam.NONE) {
                    player.sendMessage(CC.translate("&c请先选择一个阵营在进行尝试"));
                    return;
                }
                new GenesisBuyMenu().openMenu(player);
            }
        };
    }
}
