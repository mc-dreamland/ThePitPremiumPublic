package cn.charlotte.pit.npc;

import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.util.chat.CC;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:35
 */
public abstract class AbstractPitNPC {
    private NPC npc;
    @Getter
    @Setter
    private Hologram hologram;

    //NPC 内部名称
    public abstract String getNpcInternalName();

    public abstract List<String> getHoloInfo();

    //NPC 显示名称 可以自行加空格
    public abstract List<String> getNpcDisplayName(Player player);

    public List<String> getNpcTextLine(Player player) {
        List<String> displayName = this.getNpcDisplayName(player);
        List<String> text = new ArrayList<>();
        for (String s : displayName) {
            text.add(CC.translate(s));
        }

        return text;
    }

    //NPC 生成位置
    public abstract Location getNpcSpawnLocation();

    //Player name
    public abstract Skin getNpcSkin();

    //玩家交互处理
    public abstract void handlePlayerInteract(Player player);

    //npc 手持物品
    public abstract ItemStack getNpcHeldItem();

    public NPC getNpc() {
        return this.npc;
    }

    public void setNpc(NPC npc) {
        this.npc = npc;
    }

    public abstract void initSkin(NPC npc);

    public void updateHoloLines(Player player) {
        hologram.show(player, 0);
    }

    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }

    public ActivityType getActiviType() {
        return ActivityType.NULL;
    }

    public ItemStack getNpcOffItem() {
        return null;
    }

    ;
}
