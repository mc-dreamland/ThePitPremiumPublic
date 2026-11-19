package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class GoldenFish extends AbstractPitItem implements IPlayerInteract {

    private HashMap<UUID, Cooldown> map = new HashMap<>(8);

    @Override
    public String getInternalName() {
        return "golden_fish";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e黄金鱼";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.GLOW_SQUID_SPAWN_EGG;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .live(100)
                .maxLive(100)
                .deathDrop(false)
                .removeOnJoin(false)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore(
                        "&7死亡后掉落",
                        "&7使用 &a幸运转盘抽奖券 &7可以找唱片机获得随机物品"
                )
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 空实现
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        UUID uuid = player.getUniqueId();

    }
}
