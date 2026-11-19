package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * 贪婪之金
 * 副手手持
 * 击杀玩家概率增加基础金币
 * 百分百获得额外金币
 */
@AutoRegister
public class greedGold extends IMythicItem implements Listener {

    public greedGold() {
        this.maxLive = Util.random.nextInt(5) + 1;
        this.live = this.maxLive;
    }

    @Override
    public ItemStack toItemStack() {

        double probability = (double) (Util.random.nextInt(49) + 1) / 100D;
        int add = Util.random.nextInt(19) + 1;

        ItemBuilder builder = new ItemBuilder(Material.GOLD_INGOT)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canTrade(true)
                .canDrop(false)
                .removeOnJoin(false)
                .live(this.live)
                .maxLive(this.maxLive)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .shiny()
                .changeNbt("gold_probability", probability)
                .changeNbt("gold_add", add)
                .lore(
                        "",
                        String.format("&7生命: %s&7/%s", Util.getLiveColor(this.live, this.maxLive) + live, maxLive),
                        "",
                        "&6贪婪",
                        String.format(" &7副手手持时,击杀玩家时有 %s%%概率&6+5基础金币&7.", probability),
                        String.format(" &7副手手持时,击杀玩家 &6+%s金币&7.", add),
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
        return "greed_gold";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "贪婪金块";
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Player player = event.getKiller();
        ItemStack item = player.getInventory().getItemInOffHand();
        if (!PitItems.GREED_GOLD.isInternalNameEquals(item)) return;
        ReadableNBT extra = NBT.readNbt(item).getCompound("extra");
        if (extra == null) return;
        if (RandomUtil.hasSuccessfullyByChance(extra.getDouble("gold_probability"))) {
            event.getResourceManager().addBaseGold(5.0, "贪婪金块");
        }
        event.getResourceManager().addGold(extra.getInteger("gold_add"), "贪婪金块");
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
