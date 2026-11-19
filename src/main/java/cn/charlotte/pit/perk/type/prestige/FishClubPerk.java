package cn.charlotte.pit.perk.type.prestige;


import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.QuestData;
import cn.charlotte.pit.event.PitQuestCompleteEvent;
import cn.charlotte.pit.item.MythicColor;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.mode.Mode;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/2/8 23:38
 */

@AutoRegister
public class FishClubPerk extends TypedPerk<PerksPrestigeEnum> implements Listener {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "钓鱼俱乐部";
    }

    @Override
    public Material getIcon() {
        return Material.WATER_BUCKET;
    }

    @Override
    public double requireCoins() {
        return 0;
    }

    @Override
    public double requireRenown(int level) {
//        return 999999999;
        if (level < 4) {
            return 5;
        } else {
            return 10;
        }
    }

    @Override
    public int requirePrestige() {
        return 3;
    }

    @Override
    public int requireLevel() {
        return 0;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.PRESTIGE;
    }

    @Override
    public List<String> getDescription(Player player) {
        return Arrays.asList(
                "&7I -> 您将可以在水中找到鲑鱼和鲽鱼(12%概率).",
                "&7II -> 钓鱼时增加25经验",
                "&7III -> 钓鱼时有概率掉落钻石盔甲(12%概率)",
                "&7IV -> 钓鱼速度增加20%",
                "&7V -> 您可以在水中找到 &b天蓝色神话之甲(1%概率)"
        );
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public void onPerkActive(Player player) {

    }

    @Override
    public void onPerkInactive(Player player) {
        InventoryUtil.removeItemWithInternalName(player, "fishing_fish");
        InventoryUtil.removeItemWithInternalName(player, "fishing_fish_irony");
    }

    @Override
    public void handleRespawn(int enchantLevel, Player player) {
        InventoryUtil.removeItemWithInternalName(player, "fishing_fish");
        InventoryUtil.removeItemWithInternalName(player, "fishing_fish_irony");
    }

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            event.getCaught().remove();
            event.setExpToDrop(0);

            final boolean diamondSuccess = RandomUtil.hasSuccessfullyByChance(0.008);
            if (CombatListener.isNight()) {
                event.getPlayer().sendMessage(CC.translate("夜深了，鱼儿都休息了!"));
                return;
            }
            if (diamondSuccess) {
                final ItemStack itemStack = new ItemBuilder(Material.DIAMOND_SWORD)
                        .canSaveToEnderChest(false)
                        .canTrade(false)
                        .canDrop(false)
                        .deathDrop(true)
                        .removeOnJoin(false)
                        .name("&6钻石剑")
                        .internalName("fishing_dia_armor")
                        .buildWithUnbreakable();

                Item item = event.getPlayer()
                        .getWorld()
                        .dropItemNaturally(
                                event.getCaught().getLocation(),
                                itemStack
                        );
                dropFishing(event.getPlayer(), event.getHook(), item);
                return;
            } else {
                Item item = event.getPlayer().getWorld().dropItemNaturally(event.getCaught().getLocation(), new ItemStack(Material.GOLD_INGOT, 1));
                item.setMetadata("gold", new FixedMetadataValue(ThePit.getInstance(), RandomUtil.random.nextInt(3) + 3));
                PlayerProfile profile = Util.getPitProfile(event.getPlayer());
                dropFishing(event.getPlayer(), event.getHook(), item);
                QuestData currentQuest = profile.getCurrentQuest();
                if (currentQuest != null &&
                        currentQuest.getEndTime() > System.currentTimeMillis() &&
                        currentQuest.getQuestType() == AbstractQuest.QuestType.FISHING) {

                    if (currentQuest.getCurrent() < currentQuest.getTotal()) {
                        int newProgress = currentQuest.getCurrent() + 1;
                        currentQuest.setCurrent(newProgress);

                        if (newProgress >= currentQuest.getTotal()) {
                            new PitQuestCompleteEvent(event.getPlayer(), currentQuest);
                        }

                    } else {
                        currentQuest.setCurrent(currentQuest.getTotal());
                    }
                }
            }

            final Player player = event.getPlayer();
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            int level = profile.getLevel(getType());
            if (level == 0) {
                return;
            }
            if (level >= 1) {
                boolean success = RandomUtil.hasSuccessfullyByChance(0.12);
                if (profile.isDebug()) {
                    success = true;
                }
                if (success) {
                    final Location location = player.getLocation();
                    final Entity caught = event.getCaught();
                    final Location caughtLocation = caught.getLocation();
                    final Item item = location.getWorld().dropItem(
                            caughtLocation,
                            RandomUtil.helpMeToChooseOne(
                                    PitItems.FISH_6.toItemStack(),
                                    PitItems.FISH_5.toItemStack(),
                                    PitItems.FISH_4.toItemStack(),
                                    PitItems.FISH_3.toItemStack(),
                                    PitItems.FISH_2.toItemStack(),
                                    PitItems.FISH_1.toItemStack()
                            )
                    );

                    dropFishing(player, caught, item);

                    player.sendMessage(CC.translate("&a钓鱼! 尊敬的钓鱼俱乐部会员, 您找到了一条 " + item.getItemStack().getItemMeta().getDisplayName() + "&a !"));
                }
            }

            if (level >= 2) {
                profile.setExperience(profile.getExperience() + 25);
                profile.applyExperienceToPlayer(player);
            }
            if (level >= 3) {
                final boolean success = RandomUtil.hasSuccessfullyByChance(0.12);
                if (success) {
                    final Location location = player.getLocation();
                    final Entity caught = event.getCaught();
                    final Location caughtLocation = caught.getLocation();

                    final Item item = location.getWorld().dropItem(
                            caughtLocation,
                            RandomUtil.helpMeToChooseOne(
                                    PitItems.FISHING_DIA_CHESTPLATE.toItemStack(),
                                    PitItems.FISHING_DIA_BOOTS.toItemStack(),
                                    PitItems.FISHING_DIA_LEGGINGS.toItemStack()
                            )
                    );

                    dropFishing(player, caught, item);

                    player.sendMessage(CC.translate("&a钓鱼! 尊敬的钓鱼俱乐部会员, 您找到了钻石护甲!"));
                }
            }

            if (level >= 5) {
                if (RandomUtil.hasSuccessfullyByChance(0.01)) {
                    final MythicLeggingsItem item = new MythicLeggingsItem();
                    item.setColor(MythicColor.AQUA);

                    player.getInventory().addItem(item.toItemStack());
                    player.sendMessage(CC.translate("&a钓鱼! 尊敬的钓鱼俱乐部会员, 您找到了天蓝色神话之甲!"));
                }
            }

        } else if (event.getState() == PlayerFishEvent.State.FISHING) {
            final Player player = event.getPlayer();
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

            int perkData = profile.getLevel(getType());

            if (perkData == 0) {
                return;
            }

            if (perkData >= 3) {
                final FishHook hook = event.getHook();
                hook.setBiteChance(Math.min(hook.getBiteChance() * 1.2, 1.0));
            }
        }
    }

    @EventHandler
    public void onInteractFish(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT")) {
            final Player player = event.getPlayer();
            if (player.getFoodLevel() >= 20) {
                final ItemStack itemInHand = player.getInventory().getItemInMainHand();
                final String internalName = ItemUtil.getInternalName(itemInHand);
                if ("fishing_fish".equals(internalName) || "fishing_fish_irony".equals(internalName)) {
                    player.setFoodLevel(19);
                }
            }
        }
    }

    @EventHandler
    public void onEatFish(PlayerItemConsumeEvent event) {
        if (ThePit.mode == Mode.Normal) {
            return;
        }
        final ItemStack item = event.getItem();
        final String internalName = ItemUtil.getInternalName(item);
        if ("fishing_fish".equals(internalName)) {
            final Player player = event.getPlayer();
            player.setFoodLevel(20);

            player.removePotionEffect(PotionEffectType.REGENERATION);
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 20 * 3, 2));

        } else if ("fishing_fish_irony".equals(internalName)) {
            final Player player = event.getPlayer();
            player.setFoodLevel(20);

            PlayerUtil.addAbsorption(player, 2);
        }
    }

    @EventHandler
    public void onItemInHandChange(PlayerItemHeldEvent event) {
        if (ThePit.mode == Mode.Normal) {
            return;
        }
        final Player player = event.getPlayer();
        if (player.getFoodLevel() < 20) {
            final ItemStack itemInHand = player.getItemInHand();
            final String internalName = ItemUtil.getInternalName(itemInHand);
            if (!"fishing_fish".equals(internalName) && !"fishing_fish_irony".equals(internalName)) {
                player.setFoodLevel(20);
            }
        }
    }

    public void dropFishing(Player player, Entity caught, Item item) {
        final Location location = player.getLocation();
        final Location caughtLocation = caught.getLocation();

        double d5 = location.getX() - caughtLocation.getX();
        double d6 = location.getY() - caughtLocation.getY();
        double d7 = location.getZ() - caughtLocation.getZ();
        double d8 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
        double d9 = 0.1D;

        item.setVelocity(new Vector(
                d5 * d9,
                d6 * d9 + Math.sqrt(d8) * 0.08D,
                d7 * d9));
    }
}
