package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitToMysteryBuildEvent;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.GoldenBric;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.lambda.playerReturnStringListLambda;
import cn.charlotte.pit.menu.mystery.MysteryBuildData;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.RandomList;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@AutoRegister
@Slf4j
public class MysteryBoxOne extends AbstractPitItem implements Listener {
    private static final Random random = new Random();

    private static final RandomList<lambda> randomList = new RandomList<>(
            Map.entry((player) -> {
                int count = random.nextInt(1000) + 500;
                boolean isCoin = random.nextBoolean();
                return new data(new ItemStack[0], isCoin ? "&e金币" : "&b经验", target -> {
                    PlayerProfile profile = Util.getPitProfile(target);
                    if (isCoin) profile.addCoins(count);
                    else profile.addExp(count);
                });
            }, 400),
            Map.entry((player) -> {
                ItemStack itemStack = RandomUtil.helpMeToChooseOne(
                        new MythicLeggingsItem().toItemStack(),
                        new MythicBowItem().toItemStack(),
                        new MythicSwordItem().toItemStack()
                );
                return new data(itemStack, "&d随机神话装备");
            }, 200),
            Map.entry((player) -> {
                int count = random.nextInt(1) + 1;
                return new data(PitItems.CHUNK_OF_VILE.toItemStacks(count), PitItems.CHUNK_OF_VILE.getSingletonItem().getItemDisplayName());
            }, 50),
            Map.entry((player) -> {
                PitItems crop = PitItems.randomOf(PitItems.WHEAT, PitItems.PACKAGED_BALE, PitItems.SUGAR);
                int amount = crop == PitItems.PACKAGED_BALE ? 1 : random.nextInt(63) + 1;
                return new data(crop.toItemStacks(amount), "&a随机的农作物");
            }, 300),
            Map.entry((player)->{
                AbstractPitItem item = RandomUtil.helpMeToChooseOne(
                        PitItems.BLACK_STONE.newMythicItem(),
                        PitItems.GLEAM_STONE.newMythicItem(),
                        PitItems.GREED_GOLD.newMythicItem(),
                        PitItems.BOUNTY_QUEST.newMythicItem()
                );
                return new data(item.toItemStack(), item.getItemDisplayName());
            },50)
    );

    @Override
    public String getInternalName() {
        return "mystery_box_One";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&9神秘之盒 &cI";
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.SCULK_CATALYST)
                .name(getItemDisplayName())
                .internalName(getInternalName())
                .canDrop(false)
                .removeOnJoin(false)
                .canTrade(true)
                .deathDrop(false)
                .canSaveToEnderChest(true)
                .lore(
                        "",
                        "&7奇怪的盒子,里面说不定有啥好东西.",
                        "",
                        "&e在 &5神秘造物台 &e中打开"
                )
                .build();
    }

    @EventHandler
    public void onBuild(PitToMysteryBuildEvent event) {

        MysteryBuildData data = event.getData();
        ItemStack offItem = event.getOffItem();

        if (!getInternalName().equals(ItemUtil.getInternalName(event.getMainItem()))) return;
        data.setHandled(true);

        boolean isChunkOfVileItem = PitItems.CHUNK_OF_VILE.isInternalNameEquals(offItem);
        boolean isMysteriousFragments = PitItems.MYSTERIOUS_FRAGMENTS.isInternalNameEquals(offItem);
        if (!isMysteriousFragments && !isChunkOfVileItem) {
            data.setCanceled(true);
            data.setCancelMessage("&c所选的材料不对");
            return;
        }

        if (isMysteriousFragments) {
            if (offItem.getAmount() < 8) {
                data.setCanceled(true);
                data.setCancelMessage("&c材料不够");
                return;
            }
            data.setDelCount(offItem.getAmount() - 8);
            List<String> list = new ArrayList<>();
            list.add("&f你获得了如下的物品:");
            List<ItemStack> itemStacks = new ArrayList<>();
            List<data> rewards = new ArrayList<>();
            IntStream.range(0, 2).forEach(i -> {
                data d = randomList.random().execute(event.getPlayer());
                list.add(d.msg);
                itemStacks.addAll(List.of(d.items));
                rewards.add(d);
            });
            data.setPostCommitAction(() -> {
                rewards.forEach(reward -> reward.grant(event.getPlayer()));
                var loggedItems = ItemOperationJson.grantedItems(event.getPlayer(), itemStacks);
                boolean mailed = Util.giveItems(event.getPlayer(), itemStacks.toArray(ItemStack[]::new));
                data.setDelivery(mailed ? "MAIL" : "INVENTORY");
                JsonObject operationLog = ItemOperationJson.operation("OPEN_GIFT", event.getPlayer());
                operationLog.addProperty("operationId", data.getOperationId());
                operationLog.addProperty("gift", getInternalName());
                operationLog.addProperty("delivery", data.getDelivery());
                operationLog.add("rewards", ItemOperationJson.textValues(list.subList(1, list.size())));
                operationLog.add("items", loggedItems);
                log.info("{}", ItemOperationJson.toJson(operationLog));
                if (mailed) {
                    list.add("&7背包空间不足，放不下的物品已通过邮件发送。");
                }
                data.setReturnMessage(list);
            });
            data.setReturnItemStack(null);
        } else {
            if (offItem.getAmount() < 8) {
                data.setCanceled(true);
                data.setCancelMessage("&c材料不够");
                return;
            }
            ItemStack upgradedBox = PitItems.MYSTERY_BOX_TWO.toItemStack();
            AdvancedItemIdentity.inheritUuid(event.getMainItem(), upgradedBox);
            data.setReturnItemStack(upgradedBox);
            data.setDelCount(offItem.getAmount() - 8);
            data.setReturnMessage(List.of("&a升级成功！获得神秘之盒 II"));
            data.setDelivery("TABLE");
        }
    }

    private record data(ItemStack[] items, String msg, Consumer<Player> action) {
        private data(ItemStack itemStack, String msg) {
            this(new ItemStack[]{itemStack}, msg);
        }

        private data(ItemStack[] items, String msg) {
            this(items, msg, ignored -> {
            });
        }

        private void grant(Player player) {
            action.accept(player);
        }
    }

    private interface lambda {
        data execute(Player player);
    }
}
