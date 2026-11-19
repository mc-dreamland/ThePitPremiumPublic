package cn.charlotte.pit.menu.perk.normal.buy;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.killstreak.StreakChooseMenu;
import cn.charlotte.pit.menu.perk.normal.buy.button.PerkBuyButton;
import cn.charlotte.pit.menu.perk.normal.buy.button.ResetButton;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.menu.Button;
import cn.charlotte.pit.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 22:43
 */
public class StreakPerkBuyMenu extends Menu {
    private final int page;
    private final PerkType perkType;

    public StreakPerkBuyMenu(int page, PerkType perkType) {
        this.page = page;
        this.perkType = perkType;
    }

    @Override
    public String getTitle(Player player) {
        return "天赋";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> button = new HashMap<>();

        TreeMap<Integer, ArrayList<? extends TypedPerk<PerksBundlesEnum>>> streaks = Arrays.stream(PerksBundlesEnum.values()).map(PerksBundlesEnum::getPerk)
                .filter(Streakable.class::isInstance)
                .collect(Collectors.groupingBy(
                        p -> ((Streakable) p).getActiveStreak(),
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toCollection(ArrayList::new),
                                list -> {
                                    list.sort(Comparator.comparingInt(AbstractPerk::requireLevel));
                                    return list;
                                })));

        ArrayList<Map.Entry<Integer, ArrayList<? extends TypedPerk<PerksBundlesEnum>>>> entries = new ArrayList<>(streaks.entrySet());
        // ③ 遍历并生成按钮：1, 10, 19, 28 …  形成一列竖排
        for (int i = 0; i < entries.size(); i++) {

            // GUI 槽位：胸甲 GUI 一行 9 个格子 ⇒ 1 + i*9 可得到第 2 列
            int slot = i * 9 + 1;

            // 当前条目
            int killReq = entries.get(i).getKey();
            ArrayList<? extends TypedPerk<PerksBundlesEnum>> perks = entries.get(i).getValue();

            button.put(slot, new Button() {

                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.GLOW_ITEM_FRAME).name(String.format("&c%d 次击杀", killReq)).build();
                }

                @Override
                public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                }

                @Override
                public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                    return true; // 返回 true ⇒ 点击后立即刷新按钮显示
                }
            });


            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            int extraSlot = 0;
            for (TypedPerk<PerksBundlesEnum> perk : perks) {
                boolean unlocked = true;
                //highlander

                PerksBundlesEnum bundlesEnum = perk.getType();
                unlocked = bundlesEnum.hasUnlockParent(player);

//                if (bundlesEnum.equals(PerksBundlesEnum.KHANATE) || bundlesEnum.equals(PerksBundlesEnum.GOLD_NANO_FACTORY) || bundlesEnum.equals(PerksBundlesEnum.RUSH)) {
//                    unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.HIGHLANDER.name());
//                } else if (bundlesEnum.equals(PerksBundlesEnum.LEECH) || bundlesEnum.equals(PerksBundlesEnum.ASSURED_STRIKE) || bundlesEnum.equals(PerksBundlesEnum.APOSTLE_TO_RNGESUS)) {
//                    unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.MAGNUM_OPUS.name());
//                    //grand finale
//                } else if (bundlesEnum.equals(PerksBundlesEnum.MONSTER) || bundlesEnum.equals(PerksBundlesEnum.R_AND_R) || bundlesEnum.equals(PerksBundlesEnum.TOUGH_SKIN) || bundlesEnum.equals(PerksBundlesEnum.TACTICAL_RETREAT)) {
//                    unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.BEAST_MODE.name());
//                } else if (bundlesEnum.equals(PerksBundlesEnum.PUNGENT) || bundlesEnum.equals(PerksBundlesEnum.AURA_OF_PROTECTION) || bundlesEnum.equals(PerksBundlesEnum.GLASS_PICKAXE) || bundlesEnum.equals(PerksBundlesEnum.ICE_CUBE)) {
//                    unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.HERMIT.name());
//                    //hermit
//                } else if (bundlesEnum.equals(PerksBundlesEnum.SUPER_STREAKER) || bundlesEnum.equals(PerksBundlesEnum.XP_STACK) || bundlesEnum.equals(PerksBundlesEnum.GOLD_STACK)) {
//                    unlocked = PlayerUtil.isPlayerUnlockedPerk(player, PerksBundlesEnum.TO_THE_MOON.name());
//                }
                if (unlocked) {
                    button.put(slot + extraSlot + 1, new PerkBuyButton(perk, page, perkType));
                    extraSlot++;
                } else {
                    if (perk.requirePrestige() <= profile.getPrestige()) {
                        button.put(slot + extraSlot + 1, new PerkBuyButton(perk, page, perkType));
                        extraSlot++;
                    }
                }
            }

        }
//
//        for (int i : perkSlot) {
//            if (!button.containsKey(i)) {
//                button.put(i, new UnKnowButton());
//            }
//        }

//        button.put(49, new BackToPerkChooseButton());
        button.put(49, new Button() {

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW).name("&a返回").lore("&7点击回到连杀天赋选择解密.").build();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton, ItemStack currentItem) {
                new StreakChooseMenu().openMenu(player);
            }

            @Override
            public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
                return true; // 返回 true ⇒ 点击后立即刷新按钮显示
            }
        });
        if (perkType == PerkType.PERK) {
            button.put(50, new ResetButton(page));
        }

        return button;
    }

    @Override
    public int getSize() {
        return 6 * 9;
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }
}
