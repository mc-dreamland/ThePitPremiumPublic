package cn.charlotte.pit.item.type.occultDoctrine;

import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitToMysteryBuildEvent;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.JewelSword;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.lambda.playerReturnStringListLambda;
import cn.charlotte.pit.menu.mystery.MysteryBuildData;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.util.RandomList;
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
public class MysteryBoxThree extends AbstractPitItem implements Listener {

    private static final Random random = new Random();

    private static final RandomList<lambda> randomList = new RandomList<>(
            Map.entry((player) -> {
                int count = random.nextInt(2000) + 1000;
                boolean isCoin = random.nextBoolean();
                return new data(new ItemStack[0], isCoin ? "&e金币" : "&b经验", target -> {
                    PlayerProfile profile = Util.getPitProfile(target);
                    if (isCoin) profile.addCoins(count);
                    else profile.addExp(count);
                });
            }, 300),
            Map.entry((player) -> {
                boolean b = RandomUtil.hasSuccessfullyByChance(0.1);
                ItemStack itemStack;
                if (b) itemStack = new JewelSword().toItemStack();
                else
                    itemStack = RandomUtil.helpMeToChooseOne(
                            new MythicLeggingsItem().toItemStack(),
                            new MythicBowItem().toItemStack(),
                            new MythicSwordItem().toItemStack()
                    );
                return new data(itemStack, "&d随机神话装备");
            }, 200),
            Map.entry((player) -> new data(PitItems.FUNKY_FEATHER.toItemStacks(random.nextInt(2) + 1), "&3时髦的羽毛"), 100),
            Map.entry((player) -> new data(PitItems.CHUNK_OF_VILE.toItemStacks(random.nextInt(1) + 2), PitItems.CHUNK_OF_VILE.getSingletonItem().getItemDisplayName()), 200),
            Map.entry((player) -> {
                AbstractPitItem item;
                if (RandomUtil.hasSuccessfullyByChance(0.05)) {
                    item = RandomUtil.helpMeToChooseOne(
                            PitItems.FLY_BOOTS.newMythicItem(),
                            PitItems.ANGEL_CHEST_PLATE.newMythicItem(),
                            PitItems.ARMAGEDDON_BOOTS.newMythicItem(),
                            PitItems.EXORCISM_ARMOR.newMythicItem(),
                            PitItems.PHASE_BOOK.getSingletonItem(),
                            PitItems.PHASE_BOOK.getSingletonItem(),
                            PitItems.PHASE_BOOK.getSingletonItem(),
                            PitItems.PHASE_BOOK.getSingletonItem(),
                            PitItems.PHASE_BOOK.getSingletonItem()
                    );
                } else {
                    item = RandomUtil.helpMeToChooseOne(
                            PitItems.BLACK_STONE.newMythicItem(),
                            PitItems.GLEAM_STONE.newMythicItem(),
                            PitItems.GREED_GOLD.newMythicItem(),
                            PitItems.BOUNTY_QUEST.newMythicItem(),
                            PitItems.BLACK_STONE.newMythicItem(),
                            PitItems.GLEAM_STONE.newMythicItem(),
                            PitItems.GREED_GOLD.newMythicItem(),
                            PitItems.BOUNTY_QUEST.newMythicItem(),
                            PitItems.LUCKY_STONE.getSingletonItem()
                    );
                }

                return new data(item.toItemStack(), "&d随机神秘物品");
            }, 200)

    );

    @Override
    public String getInternalName() {
        return "mystery_box_three";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&5神秘之盒 &bIII";
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
                        "&7奇怪的盒子,不安的声音消失了,它变得安静了.",
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

        boolean isMysteriousFragments = PitItems.MYSTERIOUS_FRAGMENTS.isInternalNameEquals(offItem);
        if (!isMysteriousFragments) {
            data.setCanceled(true);
            data.setCancelMessage("&c所选的材料不对");
            return;
        }

        if (offItem.getAmount() < 24) {
            data.setCanceled(true);
            data.setCancelMessage("&c材料不够");
            return;
        }
        data.setDelCount(offItem.getAmount() - 24);
        List<String> list = new ArrayList<>();
        list.add("&f你获得了如下的物品:");
        List<ItemStack> itemStacks = new ArrayList<>();
        List<data> rewards = new ArrayList<>();
        IntStream.range(0, 4).forEach(i -> {
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
