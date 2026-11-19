package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@AutoRegister
public class gleamStone extends IMythicItem implements Listener {

    public gleamStone() {
        this.maxLive = Util.random.nextInt(15) + 5;
        this.live = this.maxLive;
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&3辉光石";
    }

    @Override
    public String getInternalName() {
        return "gleam_stone";
    }

    @Override
    public ItemStack toItemStack() {

        int gleam = Util.random.nextInt(29) + 1;

        ItemBuilder builder = new ItemBuilder(Material.LAPIS_LAZULI)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .lore(
                        "",
                        String.format("&7生命: %s&7/%s", Util.getLiveColor(this.live, this.maxLive) + live, maxLive),
                        "",
                        "&3辉光",
                        String.format(" &7副手手持时,击杀玩家 &b+%s经验.",gleam),
                        ""
                )
                .live(this.live)
                .maxLive(this.maxLive)
                .shiny()
                .changeNbt("gleam", gleam)
                .canTrade(true)
                .canDrop(false)
                .removeOnJoin(false)
                .deathDrop(false)
                .canSaveToEnderChest(true);
        return applyOwnershipData(builder).buildWithUnbreakable();
    }


    @EventHandler
    public void onKill(PitKillEvent event) {
        Player player = event.getKiller();
        ItemStack item = player.getInventory().getItemInOffHand();
        if (!PitItems.GLEAM_STONE.isInternalNameEquals(item)) return;
        int add;
        ReadableNBT extra = NBT.readNbt(item).getCompound("extra");
        if (extra == null) add = 1;
        else add = extra.getInteger("gleam");
        event.getResourceManager().addExp(add, getItemDisplayName());
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null) return;

        var tag = NBT.readNbt(item);
        if (tag == null) return;

        var extra = tag.getCompound("extra");
        if (extra == null) return;

        loadOwnershipData(extra);
        this.maxLive = extra.getInteger("maxLive");
        this.live = extra.getInteger("live");
    }
}
