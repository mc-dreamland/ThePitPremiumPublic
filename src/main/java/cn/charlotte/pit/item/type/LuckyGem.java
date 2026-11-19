package cn.charlotte.pit.item.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.ParticleBuilder;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.particles.ParticleTypes;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Objects;

@AutoRegister
@Slf4j
public class LuckyGem extends AbstractPitItem implements Listener {

    @Override
    public String getInternalName() {
        return "lucky_gem";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "§e幸运宝石";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.NETHER_STAR;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .name(getItemDisplayName())
                .lore(Arrays.asList(
                        "&7死亡时保留",
                        "",
                        "&7为自身添加&e幸运,",
                        "&7持续60秒,",
                        "&e&l幸运！&7大幅度提升附魔稀有概率",
                        "",
                        "&e右键使用"
                ))
                .internalName(getInternalName())
                .canSaveToEnderChest(true)
                .canTrade(false)
                .deathDrop(false)
                .shiny()
                .build();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(hand);
        ItemStack itemInEvent = event.getItem();

        // 与 Kotlin 的 `item != itemInEvent` 等价（走 equals 语义）
        if (!Objects.equals(item, itemInEvent)) {
            return;
        }

        if ("lucky_gem".equals(ItemUtil.getInternalName(item))) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);

            player.sendMessage(CC.translate("&e&l幸运! &7自身运气已大幅度提升。"));

            ItemStack consumedItem = item.clone();
            consumedItem.setAmount(1);

            // 扣除手中 1 个道具
            PlayerUtil.takeOneItem(player, hand);

            // 标记 lucky
            player.setMetadata("lucky", new FixedMetadataValue(ThePit.getInstance(), true));

            JsonObject operationLog = ItemOperationJson.operation("USE_LUCKY_GEM", player);
            operationLog.add("item", ItemOperationJson.item(consumedItem));
            operationLog.addProperty("durationSeconds", 60);
            operationLog.addProperty("rareChance", NewConfiguration.getChance(player, MythicColor.NONE));
            log.info("{}", ItemOperationJson.toJson(operationLog));

            // 60 秒（1200 tick）定时器 + 粒子效果
            new BukkitRunnable() {
                private int tick = 0;

                @Override
                public void run() {
                    if (tick >= 1200) {
                        cancel();
                        player.sendMessage(CC.translate("&e&l幸运! &7自身运气已降低。"));
                        player.removeMetadata("lucky", ThePit.getInstance());
                    } else {
                        new ParticleBuilder(
                                player.getLocation().add(0.0, 2.0, 0.0),
                                ParticleTypes.HAPPY_VILLAGER
                        )
                                .setVelocity(0.5f)
                                .setCount(5)
                                .play();
                        tick++;
                    }
                }
            }.runTaskTimer(ThePit.getInstance(), 0L, 1L);
        }
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // no-op
    }
}
