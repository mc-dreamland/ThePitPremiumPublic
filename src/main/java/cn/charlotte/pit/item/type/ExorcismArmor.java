package cn.charlotte.pit.item.type;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.buff.impl.PurificationBuff;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.item.IActionDisplayItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@AutoRegister
public class ExorcismArmor extends IMythicItem implements Listener, IActionDisplayItem {

    private static final HashMap<UUID, Cooldown> cooldownMap = new HashMap<>(8);

    public ExorcismArmor() {
        this.maxLive = 50;
        this.live = 50;
    }

    @Override
    public String getInternalName() {
        return "exorcism_armor";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&e驱魔甲";
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    public ItemStack toItemStack() {


        ItemBuilder builder = new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .lore(
                        "",
                        String.format("&7生命: %s&7/%s", Util.getLiveColor(this.live, this.maxLive) + live, maxLive),
                        "",
                        "&e厚重装甲",
                        " &7比一般的钻石甲多 1.0 的护甲值.",
                        "&b洁净",
                        " &7当受到来自 &5暗黑之甲 &7的伤害时，给予 10 秒,",
                        " &b净化 &7效果(冷却60秒).",
                        "&b净化",
                        " &7免疫 &9无尽黑暗 &7附魔效果.",
                        ""
                )
                .live(this.live)
                .maxLive(this.maxLive)
                .shiny()
                .canTrade(true)
                .canDrop(false)
                .removeOnJoin(false)
                .deathDrop(false)
                .itemArmor(10.0)
                .canSaveToEnderChest(true);
        return applyOwnershipData(builder).buildWithUnbreakable();
    }

    @EventHandler
    public void onDamage(PitDamageEvent event) {
        Player attacker = event.getAttacker();
        if (attacker == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player myself)) {
            return;
        }
        if (!getInternalName().equals(ItemUtil.getInternalName(myself.getInventory().getChestplate()))) {
            return;
        }
        Cooldown cooldown = cooldownMap.get(myself.getUniqueId());
        if (cooldown != null && !cooldown.hasExpired()) {
            return;
        }
        if (!PlayerUtil.isEquippingSomber(attacker)) {
            return;
        }
        if (cooldown == null) cooldownMap.put(myself.getUniqueId(), new Cooldown(60L, TimeUnit.SECONDS));
        else cooldown.reset();
        new PurificationBuff().stackBuff(myself, 1000L * 10, 1, false);
    }


    @Override
    public String getText(Player player) {
        Cooldown cooldown = cooldownMap.get(player.getUniqueId());
        return String.format("&b&l驱魔甲 : &e&l%s", cooldown == null || cooldown.hasExpired() ? "&a&l✔" : Util.getCooldownActionText(cooldown));
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
