package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.event.PitQuestInactiveEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * 黑石 :
 */
@AutoRegister
public class BlackStone extends IMythicItem implements Listener {

    public BlackStone() {
        this.live = 10;
        this.maxLive = 10;
    }

    @Override
    public ItemStack toItemStack() {
        ItemBuilder builder = new ItemBuilder(Material.BLACKSTONE)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canTrade(true)
                .canDrop(false)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .removeOnJoin(false)
                .live(this.live)
                .maxLive(this.maxLive)
                .lore(
                        "",
                        String.format("&7生命: %s&7/%s", Util.getLiveColor(this.live, this.maxLive) + live, maxLive),
                        "",
                        "&f漆黑的石头",
                        " &7当完成II级夜幕任务,额外奖励一个 &5暗聚块",
                        " &7并扣除 &c一条生命&7.",
                        ""
                );
        return applyOwnershipData(builder).build();
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    public String getInternalName() {
        return "black_stone";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "黑石";
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

    @EventHandler
    public void onComplete(PitQuestInactiveEvent event) {
        Player player = event.getPlayer();
        if (event.getQuestData().getLevel() < 2 || event.getQuestData().getCurrent() < event.getQuestData().getTotal() || !event.getQuestData().isNight() || !Util.getPitProfile(player).isNightQuestEnable())
            return;
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (itemStack == null || itemStack.getType() == Material.AIR || !getInternalName().equals(ItemUtil.getInternalName(itemStack)))
                continue;
            IMythicItem mythicItem = new BlackStone();
            mythicItem.loadFromItemStack(itemStack);
            if (mythicItem.isEnchanted()) {
                if (mythicItem.getLive() <= 0 || mythicItem.getLive() - 1 == 0) {
                    player.getInventory().setItem(i, PitItems.AIR.toItemStack());
                } else {
                    mythicItem.setLive(mythicItem.getLive() - 1);
                    player.getInventory().setItem(i, mythicItem.toItemStack());
                }
            }
            player.getInventory().addItem(PitItems.CHUNK_OF_VILE.toItemStack());
            return;
        }
    }
}
