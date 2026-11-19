package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.item.IMythicItem;
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
 * 悬赏令
 * 副手手持时对悬赏玩家伤害+
 * 生命 50
 */
@AutoRegister
public class BountyQuest extends IMythicItem implements Listener {

    public BountyQuest() {
        this.live = 50;
        this.maxLive = 50;
    }

    @Override
    public ItemStack toItemStack() {
        ItemBuilder builder = new ItemBuilder(Material.YELLOW_BANNER)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canTrade(true)
                .canDrop(false)
                .removeOnJoin(false)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .live(this.live)
                .maxLive(this.maxLive)
                .shiny()
                .lore(
                        "",
                        String.format("&7生命: %s&7/%s", Util.getLiveColor(this.live, this.maxLive) + live, maxLive),
                        "",
                        "&7一柄质朴的小旗子",
                        "&e通缉",
                        " &7攻击带赏金玩家时,对方每持有 &e200赏金 &7造成的伤害&c+1%",
                        " &7击杀获得的赏金数量 &e+50%",
                        ""
                );
        return applyOwnershipData(builder).build();
    }

    @Override
    public String getInternalName() {
        return "bounty_quest";
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e悬赏令";
    }

    @EventHandler
    public void onDamage(PitDamageEvent event) {
        Player attacker = event.getAttacker();
        if (attacker == null) {
            // WHY?
            return;
        }
        if (!(event.getTarget() instanceof Player targeter)) return;
        ItemStack itemStack = attacker.getInventory().getItemInOffHand();
        if (itemStack.getType() == Material.AIR || !getInternalName().equals(ItemUtil.getInternalName(itemStack)))
            return;
        PlayerProfile targeterProfile = Util.getPitProfile(targeter);

        event.getBoostDamage().addAndGet(0.01 * (0.5 / 200) * targeterProfile.getBounty());
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Double bounty = event.getResourceManager().getGoldAdditions().get("赏金");
        ItemStack itemStack = event.getKiller().getInventory().getItemInOffHand();
        if (bounty == null || itemStack.getType() == Material.AIR || !getInternalName().equals(ItemUtil.getInternalName(itemStack)))
            return;

        event.getResourceManager().addGoldModifier(1.5, "悬赏令");
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
