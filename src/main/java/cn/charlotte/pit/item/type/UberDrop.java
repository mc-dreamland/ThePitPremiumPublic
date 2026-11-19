package cn.charlotte.pit.item.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.lambda.playerLambda;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Random;

@AutoRegister
@Slf4j
public class UberDrop extends AbstractPitItem implements Listener {
    private static final Random random = new Random();
    private static final RandomList<playerLambda> randomList = new RandomList<>(
            Map.entry((p) -> {
                int amount = random.nextInt(4) + 1;
                ItemStack[] reward = PitItems.RANDOMMYTHIC.toItemStacks(amount);
                Util.giveItems(p, reward);
                CC.send(MessageType.MISC, p, String.format("&d登峰造极掉落物! &7你获得了 %s %s", "&d随机神话兑换券",
                        amount > 1 ? String.format(" &f* %s", amount) : ""));
                logGift(p, "RANDOM_MYTHIC_VOUCHER", reward);
            }, 200),

            Map.entry((p) -> {
                int amount = random.nextInt(5) + 3;
                ItemStack[] reward = PitItems.FUNKY_FEATHER.toItemStacks(amount);
                Util.giveItems(p, reward);
                CC.send(MessageType.MISC, p, String.format("&d登峰造极掉落物! &7你获得了 %s %s", "&3时髦的羽毛",
                        String.format(" &f* %s", amount)));
                logGift(p, "FUNKY_FEATHER", reward);
            }, 200),

            Map.entry((p) -> {
                int amount = random.nextInt(3) + 2;

                ItemStack[] reward = PitItems.PIT_CACTUS.toItemStacks(amount);
                Util.giveItems(p, reward);
                CC.send(MessageType.MISC, p, String.format("&d登峰造极掉落物! &7你获得了 %s %s", "&a哲学仙人掌",
                        String.format(" &f* %s", amount)));
                logGift(p, "PIT_CACTUS", reward);
            }, 200),

            Map.entry((p) -> {
                ItemStack reward = PitItems.TOTALLY_LEGIT_GEM.toItemStack();
                p.getInventory().addItem(reward);
                CC.send(MessageType.MISC, p, "&d登峰造极掉落物! &7你获得了 &a遵纪守法的宝石");
                logGift(p, "TOTALLY_LEGIT_GEM", reward);
            }, 200),

            Map.entry((p) -> {
                ItemStack reward = PitItems.GLOBAL_ATTENTION_GEM.toItemStack();
                p.getInventory().addItem(reward);
                CC.boardCast("&d登峰造极掉落物! " + Util.getPitProfile(p).getFormattedName() + " &f获得了 &b&l举世瞩目的宝石");
                logGift(p, "GLOBAL_ATTENTION_GEM", reward);
            }, 1),

            Map.entry((p) -> {
                ItemStack reward = PitItems.JEWEL_SWORD.toItemStack();
                p.getInventory().addItem(reward);
                CC.send(MessageType.MISC, p, "&d登峰造极掉落物! &7你获得了 &d宝石之剑");
                logGift(p, "JEWEL_SWORD", reward);
            }, 199)
    );

    private static void logGift(Player player, String rewardName, ItemStack... rewards) {
        JsonObject operationLog = ItemOperationJson.operation("OPEN_GIFT", player);
        operationLog.addProperty("gift", "uber_drop");
        operationLog.addProperty("reward", rewardName);
        operationLog.add("items", ItemOperationJson.grantedItems(player, List.of(rewards)));
        log.info("{}", ItemOperationJson.toJson(operationLog));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(BlockPlaceEvent event) {
        EquipmentSlot hand = event.getHand();
        ItemStack itemInHand = event.getItemInHand();

        if (!"uber_drop".equals(ItemUtil.getInternalName(itemInHand))) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (InventoryUtil.isInvFull(player.getInventory())) {
            player.sendMessage(CC.translate("&c你的背包是满的, 无法使用物品"));
            return;
        }


        // 播放音效
        final float[] ticks = {0.3f};
        new BukkitRunnable() {
            @Override
            public void run() {
                player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, ticks[0]);
                ticks[0] += 0.1f;
                if (ticks[0] >= 2.0f) cancel();
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 2L);

        PlayerUtil.takeOneItem(player, hand);
        try {
            randomList.random().execute(player);
        } catch (Exception e) {
            log.error("UberDrop : ", e);
        }
    }

    @Override
    public String getInternalName() {
        return "uber_drop";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d登峰造极掉落物";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.ENDER_CHEST;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName("uber_drop")
                .name(getItemDisplayName())
                .lore(
                        "&7死亡时保留",
                        "",
                        "&e拿着并右键获得物品!"
                )
                .dontStack()
                .canTrade(false)
                .canSaveToEnderChest(true)
                .build();
    }
}
