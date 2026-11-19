package cn.charlotte.pit.activity.newyear.item;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.lambda.playerReturnStringLambda;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@AutoRegister
@Slf4j
public class NewYearLuckyBag extends AbstractPitItem implements Listener {

    private static final RandomList<playerReturnStringLambda> randomList = new RandomList<>(
            Map.entry((p) -> {
                p.getInventory().addItem(PitItems.MILK_BUCKET.toItemStack());
                return "获得了一桶&f牛奶";
            }, 1),
            Map.entry((p) -> {
                p.getInventory().addItem(PitItems.CHUNK_OF_VILE.toManyItem(1));
                return "获得了&5暗聚块";
            }, 40),
            Map.entry((p) -> {
                int coins = ThreadLocalRandom.current().nextInt(300, 1001);
                Util.getPitProfile(p).addCoins(coins);
                return String.format("获得了&6%d金币", coins);
            }, 30),

            Map.entry((p) -> {
                int exp = ThreadLocalRandom.current().nextInt(300, 1001);
                Util.getPitProfile(p).addExp(exp);
                return String.format("获得了&b%d经验", exp);
            }, 30),
            Map.entry((p) -> {
                p.getInventory().addItem(PitItems.ACTION_GIFT_BOX.toItemStack());
                return "获得了&e行动礼盒";
            }, 20),
            Map.entry((p) -> {
                p.getInventory().addItem(PitItems.FUNKY_FEATHER.toItemStack());
                return "获得了时髦的羽毛";
            }, 9),
            Map.entry((p) -> {
                int bounty = ThreadLocalRandom.current().nextInt(5, 21);
                Util.getPitProfile(p).addActionBounty(bounty);
                return String.format("获得了&e%d行动赏金", bounty);
            }, 30)
    );

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.RED_WOOL)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .removeOnJoin(false)
                .canDrop(false)
                .canTrade(true)
                .canSaveToEnderChest(true)
                .deathDrop(false)
                .lore(
                        "",
                        "&e手持并右键打开!",
                        ""
                )
                .build()
                ;
    }

    @Override
    public String getInternalName() {
        return "new_year_lucky_bag";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&c新春福袋";
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(BlockPlaceEvent event) {
        if (!PitItems.NEW_YEAR_LUCKY_BAG.isInternalNameEquals(event.getItemInHand())) return;
        PlayerUtil.takeOneItem(event.getPlayer(), event.getHand());
        String reward = randomList.random().execute(event.getPlayer());
        CC.send(event.getPlayer(), reward);

        JsonObject operationLog = ItemOperationJson.operation("OPEN_GIFT", event.getPlayer());
        operationLog.addProperty("gift", getInternalName());
        operationLog.addProperty("reward", CC.stripColor(CC.translate(reward)));
        log.info("{}", ItemOperationJson.toJson(operationLog));

    }
}
