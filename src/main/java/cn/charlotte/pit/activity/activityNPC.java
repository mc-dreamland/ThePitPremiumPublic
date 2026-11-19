package cn.charlotte.pit.activity;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.genes.menu.GenesMenu;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.npc.type.SkinNPC;
import cn.charlotte.pit.util.chat.CC;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
@Slf4j
public class activityNPC extends SkinNPC {

    @Override
    public String getNpcInternalName() {
        return "activityNPC";
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
        ActivityType type = ThePit.getInstance().getActivityFactory().getFactoryType();
        return List.of(
                "&e活动大使",
                Util.matchesAny(type, null, ActivityType.NULL) ? "&c尚未安排任何活动" : String.format("&e当期活动:%s", type.getName())
        );
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getActivityNPCLocation();
    }

    // todo 补充NPC皮肤

    @Override
    public Skin getNpcSkin() {
        return null;
    }

    @Override
    public void handlePlayerInteract(Player player) {
        ActivityType type = ThePit.getInstance().getActivityFactory().getFactoryType();
        if (Util.matchesAny(type, null, ActivityType.NULL)) {
            CC.send(player, "&c目前没有任何活动!");
            return;
        }

        ActivityType type2 = Arrays.stream(ActivityType.values())
                .filter(type1 -> type1.equals(type))
                .findFirst().orElse(null);
        if (type2 == null) return;
        type2.getMenu().openMenu(player);
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
