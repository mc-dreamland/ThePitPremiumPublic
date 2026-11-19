package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.bot.PitBotManager;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class LuckyChestplate extends IMythicItem implements Listener {

    public LuckyChestplate() {
        this.maxLive = 10;
        this.live = 10;
    }

    @Override
    public String getInternalName() {
        return "lucky_chestplate";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&b幸运之甲";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.DIAMOND_CHESTPLATE;
    }

    @Override
    public boolean isEnchanted() {
        return true;
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        if (item == null) return;

        var tag = NBT.readNbt(item);

        var extra = tag.getCompound("extra");
        if (extra == null) return;

        loadOwnershipData(extra);
        this.maxLive = extra.getInteger("maxLive");
        this.live = extra.getInteger("live");
    }

    @Override
    public ItemStack toItemStack() {
        double ratio = live / (maxLive * 1.0);
        String lifeColor;
        if (ratio <= 0.3) {
            lifeColor = "&c";
        } else if (ratio <= 0.6) {
            lifeColor = "&e";
        } else {
            lifeColor = "&a";
        }

        ItemBuilder builder = new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(
                        "&7生命: " + lifeColor + live + "&7/" + maxLive,
                        "",
                        "&9幸运之甲",
                        "&7装备时,&c20%&7的几率将致命一击抵消 &7.",
                        "&7下一次伤害增加 &c20",
                        ""
                )
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .removeOnJoin(false)
                .internalName(getInternalName())
                .maxLive(this.maxLive)
                .live(this.live)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .removeOnJoin(false)
                .canDrop(false)
                .canTrade(true)
                .shiny();
        return applyOwnershipData(builder).build();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (PitBotManager.skipCustomCombat(e)) return;
        if (!(e.getEntity() instanceof Player player)) return;

        // 如果这次伤害不是致死的，不处理
        if (player.getHealth() - e.getFinalDamage() >= 0.0) {
            return;
        }

        ItemStack itemStack = player.getInventory().getChestplate();
        if (itemStack == null) return;

        IMythicItem mythicItem = MythicUtil.getMythicItem(itemStack);
        if (!(mythicItem instanceof LuckyChestplate)) {
            return;
        }

        // 判断 metadata 是否过期
        long expireTime = Long.MAX_VALUE;
        List<MetadataValue> metaList = player.getMetadata("lucky_chestplate");
        if (!metaList.isEmpty()) {
            expireTime = metaList.getFirst().asLong();
        }

        if (expireTime <= System.currentTimeMillis()) {
            e.setDamage(e.getDamage() + 20.0);
        }

        // 注意：Kotlin 里这里写错了 key（"lcuky_chestplate"），我保留了逻辑但修正成 "lucky_chestplate"
        player.removeMetadata("lucky_chestplate", ThePit.getInstance());

        boolean success = RandomUtil.hasSuccessfullyByChance(0.2);
        if (success) {
            e.setCancelled(true);
            player.setMetadata("lucky_chestplate",
                    new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis() + 1000L * 30)
            );
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            player.sendMessage("您抵御了一次必中伤害");
        }
    }
}
