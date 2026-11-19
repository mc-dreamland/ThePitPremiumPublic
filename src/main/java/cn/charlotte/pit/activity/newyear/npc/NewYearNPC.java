package cn.charlotte.pit.activity.newyear.npc;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.newyear.menu.main.NewYearMenu;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.npc.type.SkinNPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class NewYearNPC extends SkinNPC {

    @Override
    public String getNpcInternalName() {
        return "new_year_npc";
    }

    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e新年",
                "&b右键查看"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        return List.of(
                "&e新年",
                "&b右键查看"
        );
    }

    @Override
    public Location getNpcSpawnLocation() {
        return null;
    }

    //todo 给这个NPC加个皮肤

    @Override
    public Skin getNpcSkin() {
        return null;
    }

    @Override
    public void handlePlayerInteract(Player player) {
        new NewYearMenu().openMenu(player);
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
