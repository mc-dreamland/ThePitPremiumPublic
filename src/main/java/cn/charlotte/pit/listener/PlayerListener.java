package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityPlayerData;
import cn.charlotte.pit.data.CustomPaperData;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerBanData;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.event.PitConsumeItemEvent;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitPlayerEnchantEvent;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.item.type.CustomNamePaper;
import cn.charlotte.pit.item.type.mythic.LuckyChestplate;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.medal.impl.challenge.LuckyDiamondMedal;
import cn.charlotte.pit.medal.impl.challenge.TrickleDownMedal;
import cn.charlotte.pit.menu.Help.button.UpdateMenu;
import cn.charlotte.pit.menu.cactus.CactusMenu;
import cn.charlotte.pit.menu.cactus.DimensionalCactusMenu;
import cn.charlotte.pit.menu.cactus.SacrosanctCactusMenu;
import cn.charlotte.pit.menu.customname.CustomNamePaperMenu;
import cn.charlotte.pit.menu.mystery.MysteryMenu;
import cn.charlotte.pit.mode.Mode;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.runnable.ProfileLoadRunnable;
import cn.charlotte.pit.runnable.SewersRunnable;
import cn.charlotte.pit.util.DirectionUtil;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.VectorUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import cn.charlotte.pit.util.log.ItemOperationJson;
import cn.charlotte.pit.util.random.RandomUtil;
import cn.charlotte.pit.util.time.TimeUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import net.wesjd.anvilgui.AnvilGUI;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:00
 */
@Slf4j
@AutoRegister
public class PlayerListener implements Listener {
    public final static Map<Integer, UUID> playerIdCache = new HashMap<>();
    public static PlayerListener INSTANCE;
    private static Cooldown musicCooldown = null;
    @Getter
    private final Map<UUID, Long> goldenAppleCooldown = new HashMap<>();
    private final Map<UUID, Cooldown> firstAidEggCooldown = new HashMap<>();
    private final Random random = new Random();
    private final DecimalFormat numFormat = new DecimalFormat("0.00");
    private final Map<UUID, Cooldown> tacticalInsertionCooldown = new HashMap<>();

    public PlayerListener() {
        INSTANCE = this;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ThePit pit = ThePit.getInstance();
        Player player = event.getPlayer();
        PlayerUtil.clearPlayer(player, true);
        if (!pit.getPitConfig().getSpawnLocations().isEmpty()) {
            Location location = pit.getPitConfig()
                    .getSpawnLocations()
                    .get(random.nextInt(pit.getPitConfig().getSpawnLocations().size()));

            player.teleport(location);
        } else {
            player.sendMessage(CC.translate("&cNo spawn found "));
        }

        if (ProfileLoadRunnable.getInstance() == null) {
            event.getPlayer().kickPlayer(" ");
            return;
        }

        playerIdCache.put(player.getEntityId(), player.getUniqueId());

        ProfileLoadRunnable.getInstance().handleJoin(player);

        //临时措施
        if (((player.hasPermission("meta.queuepool.CROSS") || player.hasPermission("meta.queuepool.cross")) && !player.getUniqueId().toString().startsWith("000000") && !player.hasPermission("heypixel.admin") && !player.isOp())) {
            player.kick(Component.text("§c[§4§l!§c] §c请关闭跨平台模式再尝试进入神话天坑玩法！"));
        }
    }

    @EventHandler
    public void onProfileLoadComplete(PitProfileLoadedEvent event) {
        PlayerProfile profile = event.getPlayerProfile();
        Player player = Bukkit.getPlayer(profile.getPlayerUUID());

        if (player == null || !player.isOnline()) {
            return;
        }

        if (ThePit.getBungeeServerName().equals("NULL")) {
            Util.backToLobby(player);
        }
        if (profile.isBanned()) {
            PlayerBanData banData = profile.getPlayerBanData();
            player.sendMessage(CC.translate("&4⚠ &c你当前已被禁止游玩天坑乱斗!"));
            player.sendMessage(CC.translate("&4⚠ &c此限制将在 &f" + TimeUtil.millisToRoundedTime(banData.getEnd() - System.currentTimeMillis()) + " &c后自动解除."));
            player.sendMessage(CC.translate("&4⚠ &c原因: &f" + profile.getPlayerBanData().getReason()));
            if (player.hasPermission("thepit.admin")) {
                player.sendMessage(CC.translate("&2⚠ &a但你是管理员,因此没有被移出房间."));
            } else {
                player.kickPlayer("You are currently suspended on this Pit server.");
            }
        } else {
            this.welcomePlayer(player);
        }

        PlayerInv playerInv = profile.getInventory();
        MythicOwnershipUtil.bindProfile(profile, player);
        playerInv.applyItemToPlayer(player);
        InventoryUtil.supplyItems(player);
        profile.setLastLogoutTime(System.currentTimeMillis());
        profile.applyExperienceToPlayer(player);
        ActivityPlayerData acData = profile.getActivityPlayerData();
        if (acData.getVer() != UpdateMenu.getVer()) {
            acData.clear();
        }

        PlayerUtil.clearPlayer(player, false, false);

        //todo 修复最大生命值
        int perkLevel = profile.getLevel(PerksPrestigeEnum.EXTRA_HEARTS);
        if (!"rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            profile.getExtraMaxHealth().remove("rage_pit");
            profile.getExtraMaxHealth().remove("疯狂天坑");
        } else {
            profile.getExtraMaxHealth().put("rage_pit", 20.0);
        }
        if (perkLevel > 0) {
            profile.getExtraMaxHealth().put(PerksPrestigeEnum.EXTRA_HEARTS.name(), 2.0 * perkLevel);
            player.setMaxHealth(profile.getMaxHealth());
        }
    }

    @EventHandler
    public void onPlayerInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
//            if (PlayerUtil.isStaffSpectating((Player) event.getPlayer())) {
//                return;
//            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
            if (!profile.isTempInvUsing()) {
                MythicOwnershipUtil.bindInventory((Player) event.getPlayer());
                profile.setInventory(PlayerInv.fromPlayerInventory(event.getPlayer().getInventory()));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MythicOwnershipUtil.bindInventory(event.getPlayer());
        firstAidEggCooldown.remove(event.getPlayer().getUniqueId());
        goldenAppleCooldown.remove(event.getPlayer().getUniqueId());
        tacticalInsertionCooldown.remove(event.getPlayer().getUniqueId());

        playerIdCache.remove(event.getPlayer().getEntityId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        Item pickedItem = event.getItem();
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        ItemStack stack = pickedItem.getItemStack();
        final String internalName = ItemUtil.getInternalName(stack);
        // 让原版拾取已有编号的材料，避免把同一件物品按新奖励重新编号。
        if ((PitItems.WHEAT.isInternalNameEquals(internalName)
                || PitItems.SUGAR.isInternalNameEquals(internalName)
                || PitItems.RUBBIN.isInternalNameEquals(internalName)
                || PitItems.PACKAGED_BALE.isInternalNameEquals(internalName))
                && AdvancedItemIdentity.readUuid(stack) != null) {
            return;
        }
        MythicOwnershipUtil.bindInitial(stack, player);
        if (internalName != null && (internalName.contains("fish") || internalName.equals("fishing_dia_armor") || internalName.contains("mythic"))) {
            return;
        }

        event.setCancelled(true);
//        if (PlayerUtil.isStaffSpectating(player)) {
//            return;
//        }
        pickedItem.remove();

        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(stack);

        if (nmsItem.getItem() instanceof ArmorItem) {
            if (ItemUtil.getInternalName(stack) == null) {
                return;
            }
            ItemStack itemStack;
            if ("lucky_diamond".equals(internalName)) {
                new LuckyDiamondMedal().addProgress(PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()), 1);
                itemStack = new ItemBuilder(stack.getType()).canDrop(false).canSaveToEnderChest(false).deathDrop(true).canTrade(false).removeOnJoin(true).internalName(internalName).lore("&b幸运钻石天赋物品").buildWithUnbreakable();
            } else {
                itemStack = new ItemBuilder(stack.getType()).canDrop(false).canSaveToEnderChest(true).deathDrop(true).internalName(internalName).buildWithUnbreakable();
            }

            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getType() != Material.AIR && itemStack.getType().name().contains("IRON") && item.getType() == itemStack.getType()) {
                    return;
                }
            }

            EquipmentSlot slot = InventoryUtil.getArmorSlot(itemStack.getType());
            if (slot == null) {
                return;
            }
            if (itemStack.getType().name().contains("BOOTS")) {
                if (PlayerUtil.hasEquipPerk(player, PerksBoostsEnum.MARATHON.getPerk())) {
                    return;
                }
            }
            ItemStack equipItem = player.getInventory().getItem(slot);
            if (itemStack.getType().name().contains("IRON")) {
                if (equipItem.getType().name().contains("DIAMOND")) {
                    return;
                }
                if (!equipItem.getType().name().contains("CHAINMAIL")) {
                    return;
                }
                //check if player is equipping a mythic pants
                final String internalName1 = ItemUtil.getInternalName(equipItem);
                if (internalName1 != null && (internalName1.equalsIgnoreCase(PitItems.EXORCISM_ARMOR.getInternalName()) || internalName1.equalsIgnoreCase("mythic_leggings") || internalName1.equals("armageddon_boots") || internalName1.equals("angel_chestplate") || internalName1.equals("kings_helmet"))) {
                    return;
                }

                player.getInventory().setItem(slot, itemStack);

                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 1F, 1F);
            } else if (itemStack.getType().name().contains("DIAMOND")) {
//                final String internalName1 = ItemUtil.getInternalName(equipItem);
                if (Utils.getMythicItem(equipItem) != null) {
                    //check if player is equipping a mythic pants
                    // 如果玩家已有高级装备，则不拾取
//                    InventoryUtil.addInvReverse(player.getInventory(), itemStack);
                    return;
//                } else if (equipItem.getType().name().contains("DIAMOND") || equipItem.getType().name().contains("IRON")) {
                } else if (equipItem.getType().name().contains("DIAMOND")) {
                    // 如果玩家已有高级装备，则不拾取
                    return;
                }

                player.getInventory().setItem(slot, itemStack);
                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 1F, 1F);
            }
        } else if (stack.getType() == Material.ARROW) {
            ItemBuilder arrowBuilder = new ItemBuilder(Material.ARROW).internalName("default_arrow").defaultItem().canDrop(false).canSaveToEnderChest(false);
            player.getInventory().addItem(arrowBuilder.build());
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1F, 1F);
        } else if ((stack.getType() == Material.BOW || CraftItemStack.asNMSCopy(stack).getItem() instanceof SwordItem) && !event.isCancelled()) {
            InventoryUtil.addInvReverse(player.getInventory(), pickedItem.getItemStack());
        } else if (stack.getType() == Material.GOLD_INGOT) {
            if (pickedItem.hasMetadata("gold")) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                int gold = pickedItem.getMetadata("gold").getFirst().asInt();
                if (PlayerUtil.hasEquipPerk(player, PerksBoostsEnum.TRICKLE_DOWN.getPerk())) {
                    gold += 10;
                    PlayerUtil.heal(player, 4);
                    new TrickleDownMedal().addProgress(profile, 10);
                }
                int level = Utils.getEnchantLevel(player.getInventory().getLeggings(), "pebble_enchant");
                if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                    level = 0;
                }
                if (level > 0) {
                    gold += level * 10;
                    if (level >= 3) {
                        PlayerUtil.heal(player, 2);
                    }
                }
                profile.setCoins(profile.getCoins() + gold);

                profile.setGoldPicked(profile.getGoldPicked() + 1);
                player.sendMessage(CC.translate("&6&l捡起硬币! &7从地上找到了&6 " + gold + " &7硬币!"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1.8F);
            }
        } else if (stack.getType() == Material.WHEAT) {
            Util.giveItems(player, PitItems.WHEAT.toItemStacks(stack.getAmount()));
            player.sendMessage(CC.translate("&a&l捡起小麦! " + "&f* " + stack.getAmount()));
        } else if (stack.getType() == Material.SUGAR) {
            Util.giveItems(player, PitItems.SUGAR.toItemStacks(stack.getAmount()));
            player.sendMessage(CC.translate("&a&l捡起糖! " + "&f* " + stack.getAmount()));
        } else if (stack.getType() == Material.SLIME_BALL) {
            Util.giveItems(player, PitItems.RUBBIN.toItemStacks(stack.getAmount()));
            player.sendMessage(CC.translate("&2&l捡起下水道废弃之物! " + "&f* " + stack.getAmount()));
        } else if (ThePit.mode == Mode.Normal) {
            player.getInventory().addItem(stack);
            player.sendMessage(CC.translate("§c你似乎得到了一些物品?"));
        } else if (stack.getType() == Material.HAY_BLOCK) {
            Util.giveItems(player, PitItems.PACKAGED_BALE.toItemStacks(stack.getAmount()));
            player.sendMessage(CC.translate("&e&l捡起干草块! " + "&f* " + stack.getAmount()));
        }
    }

    @EventHandler
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = Util.getPitProfile(player);

        if (event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
                event.setCancelled(true);
                int perkLevel = profile.getLevel(PerksPrestigeEnum.MYSTICISM);
                if (profile.isInArena()) {
                    return;
                }
                if (perkLevel > 0) {
                    ThePit.getApi().openMythicWellMenu(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 0.9F);
                    return;
                }
                player.sendMessage(CC.translate("&c你需要达到 " + LevelUtil.getLevelTag(0, 120) + " &c解锁精通玩法并解锁精通天赋 &6神话附魔师 &c以使用神话之井!"));
            } else if (event.getClickedBlock().getType() == Material.JUKEBOX && !profile.isInArena()) {
                if (!profile.isSupporter()) {
                    CC.send(MessageType.MISC, player, "&c你没有 &e天坑乱斗增值包 &c无法使用.");
                }
                if (!musicCooldown.hasExpired() || musicCooldown == null) {
                    CC.send(MessageType.MISC, player, "&c音符盒冷却ing...");
                }

            } else if (event.getClickedBlock().getType().equals(Material.SCULK_SHRIEKER) && (event.getAction().isLeftClick() || event.getAction().isRightClick()))
                new MysteryMenu().openMenu(player);

        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        Player player = e.getPlayer();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (e.getClickedBlock() != null && (e.getClickedBlock().getType() == Material.CRAFTING_TABLE
                || e.getClickedBlock().getType() == Material.ANVIL || e.getClickedBlock().getType() == Material.TRAPPED_CHEST)) {
            e.setCancelled(true);
            return;
        }
        ItemStack item = e.getItem();
        EquipmentSlot hand = e.getHand();
        if (hand == null) {
            return;
        }
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        String internalName = ItemUtil.getInternalName(item);
        if (internalName != null) {
            for (PitItems pitItem : PitItems.values()) {
                if (!pitItem.isPlayerInteracts()) {
                    continue;
                }
                String pitInternal = pitItem.getInternalName();
                if (pitInternal == null) {
                    continue;
                }
                if (internalName.equals(pitInternal)) {
                    if (pitItem.getScope() == PitItems.Scope.SINGLETON) {
                        AbstractPitItem singleton = pitItem.getSingletonItem();
                        if (singleton instanceof IPlayerInteract) {
                            ((IPlayerInteract) singleton).handlePlayerInteract(e.getPlayer(), item, hand, e);
                        }
                    }
                    break;
                }
            }
        }

        if (player.getGameMode() != GameMode.CREATIVE && item.getType() == Material.PLAYER_HEAD && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && "golden_head".equals(internalName)) {
            e.setCancelled(true);
            if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                e.setUseItemInHand(Event.Result.DENY);
                return;
            }
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
            goldenAppleCooldown.put(player.getUniqueId(), System.currentTimeMillis());
            PlayerUtil.takeOneItem(player, hand);
            new PitConsumeItemEvent(player, "golden_head").callEvent();
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 140, 0, false, false));
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.VERY_YUMMY)) {
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 140, 1, false, false));
                PlayerUtil.setAbsorption(player, 7);
            } else {
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 80, 1, false, false));
                PlayerUtil.setAbsorption(player, 6);
            }
            player.setSaturation(9.6F);
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.YUMMY)) {
                profile.setCoins(profile.getCoins() + 3);
            }
        } else if (item.getType() == Material.BAKED_POTATO) {
            if ("angry_potato".equals(internalName)) {
                e.setCancelled(true);
                if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                    e.setUseItemInHand(Event.Result.DENY);
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
                goldenAppleCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "angry_potato").callEvent();
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 80, 1));
//                player.setAbsorptionAmount(4.0F);
                PlayerUtil.setAbsorption(player, 4);
            }
        } else if (item.getType() == Material.MAGMA_CREAM) {
            if ("broken_soul".equals(internalName)) {
                e.setCancelled(true);
                if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                    e.setUseItemInHand(Event.Result.DENY);
                    return;
                }
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
                goldenAppleCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "broken_soul").callEvent();
                PlayerUtil.heal(player, 8);
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 60, 1));
//                player.setAbsorptionAmount(6.0F);
                PlayerUtil.setAbsorption(player, 6);
            }
        } else if (item.getType() == Material.AXOLOTL_SPAWN_EGG) {
            if ("first_aid_egg".equals(internalName)) {
                e.setCancelled(true);
                final Cooldown cooldown = firstAidEggCooldown.get(player.getUniqueId());
                if (cooldown != null && !cooldown.hasExpired()) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.getPlayer().sendMessage(CC.translate("&c在再次使用此物品前,请等待" + TimeUtil.millisToRoundedTime(cooldown.getRemaining()).replace(" ", "") + "!"));
                    return;
                }
                player.getLocation();
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
                firstAidEggCooldown.put(player.getUniqueId(), new Cooldown(30, TimeUnit.SECONDS));
                player.removePotionEffect(PotionEffectType.SPEED);
                PlayerUtil.heal(player, 5);
            }
//        }
//        else if (item.getType() == Material.BEETROOT_SOUP) {
//            if ("perk_tasty_soup_kill".equals(internalName)) {
//
//            }
//            if ("perk_tasty_soup_assist".equals(internalName)) {
//
//            }
        } else if (item.getType() == Material.CACTUS) {
            if ("cactus".equals(internalName)) {
                e.setCancelled(true);
                if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                    e.setUseItemInHand(Event.Result.DENY);
                    return;
                }
                new CactusMenu().openMenu(player);
            } else if ("sacrosanct_cactus".equals(internalName)) {
                e.setCancelled(true);
                if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                    e.setUseItemInHand(Event.Result.DENY);
                    return;
                }
                new SacrosanctCactusMenu().openMenu(player);
            } else if ("dimensional_cactus".equals(internalName)) {
                e.setCancelled(true);
                if (System.currentTimeMillis() - goldenAppleCooldown.getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
                    e.setUseItemInHand(Event.Result.DENY);
                    return;
                }
                new DimensionalCactusMenu().openMenu(player);
            }
        } else if (item.getType() == Material.MILK_BUCKET) {
            if ("milk_bucket".equals(internalName)) {
                e.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "milk_bucket").callEvent();
                PlayerUtil.addEffect(e.getPlayer(), new PotionEffect(PotionEffectType.REGENERATION, 20 * 20, 0));
                e.getPlayer().sendMessage(CC.translate("&f牛奶! &7你喝下了牛奶"));
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
            }
        } else if (item.getType() == Material.SWEET_BERRIES) {
            if ("sweet_berries".equals(ItemUtil.getInternalName(item))) {
                e.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "sweet_berries").callEvent();
                PlayerUtil.addAbsorption(player, 2);
                e.getPlayer().sendMessage(CC.translate("&f曲奇! &7你吃下了美味樱桃"));
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
            }
        } else if (item.getType() == Material.COOKIE) {
            if ("cookie".equals(ItemUtil.getInternalName(item))) {
                e.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "cookie").callEvent();
                PlayerUtil.addEffect(e.getPlayer(), new PotionEffect(PotionEffectType.SPEED, 20 * 20, 0));
                e.getPlayer().sendMessage(CC.translate("&f曲奇! &7你吃下了美味曲奇"));
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
            }
        } else if (item.getType() == Material.BLAZE_ROD) {
            if ("tactical_insertions".equals(internalName)) {
                Cooldown cooldown = tacticalInsertionCooldown.get(player.getUniqueId());
                if (cooldown != null && !cooldown.hasExpired()) {
                    player.sendMessage(CC.translate("&c冷却中! 请等待 " +
                            TimeUtil.millisToRoundedTime(cooldown.getRemaining()) + " 后使用"));
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "tactical_insertions").callEvent();
                tacticalInsertionCooldown.put(player.getUniqueId(), new Cooldown(60, TimeUnit.SECONDS));
                Location loc = player.getLocation();
                CombatListener.setloc(player, loc);
                String coord = String.format("X: %.1f, Y: %.1f, Z: %.1f",
                        loc.getX(), loc.getY(), loc.getZ());
                player.sendMessage(CC.translate("&6战术重部署装置 &7已成功设置了坐标(" + coord + ")为出生点"));
            }
        } else if (item.getType() == Material.PAPER && "CustomNamePaper".equals(internalName) && hand == EquipmentSlot.HAND) {
            CustomNamePaper customNamePaper = new CustomNamePaper();
            customNamePaper.loadFromItemStack(item);
            if (customNamePaper.isAllowed()) {
                e.setCancelled(true);
                new CustomNamePaperMenu(player.getUniqueId(), customNamePaper).openMenu(player);
//                if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
//
//                }
//                    String offhandInternalName = ItemUtil.getInternalName(player.getInventory().getItemInOffHand());
//                    if ("mythic_sword".equals(offhandInternalName) || "mythic_bow".equals(offhandInternalName)) {
//                        e.setCancelled(true);
//                        String name = customNamePaper.getCustomName();
//                        IMythicItem mythicItem = MythicUtil.getMythicItem(player.getInventory().getItemInOffHand());
//                        mythicItem.setCustomName(CC.translate(name));
//                        player.getInventory().setItemInOffHand(mythicItem.toItemStack());
//                        PlayerUtil.takeOneItem(player, hand);
//                        new PitConsumeItemEvent(player, "CustomNamePaper").callEvent();
//                        CC.boardCast("&e&l恭喜 " + profile.getFormattedName() + " &f将一件物品更名为了 " + name);
//                    } else {
//                        player.sendMessage(CC.translate("&c错误 &7仅支持修改 &d神话之剑 &7和 &b神话之弓"));
//                    }
//                } else {
//                    player.sendMessage(CC.translate("&c错误 &7副手请手持物品"));
//                }
            } else if (customNamePaper.isCustomNameApplied()) {
                e.setCancelled(true);
                long millis = customNamePaper.getMillis();
                if (millis > System.currentTimeMillis()) {
                    player.sendMessage(CC.translate("&c错误 &7此物品正在审核中，请等待"));
                } else {
                    PlayerUtil.takeOneItem(player, hand);
                    new PitConsumeItemEvent(player, "CustomNamePaper").callEvent();
                    CustomPaperData.load(customNamePaper.getUuid().toString())
                            .thenAcceptAsync(l -> {
                                player.closeInventory();
                                int allowed = l.getAllowed();
                                ItemStack to = l.toItem();
                                switch (allowed) {
                                    case CustomPaperData.ALLOWED_QUEUED ->
                                            player.sendMessage(CC.translate("&c错误 &7此物品正在审核中，请等待"));
                                    case CustomPaperData.ALLOWED_PROMISED ->
                                            player.sendMessage(CC.translate("&a审核成功 &7改名已可用"));
                                    default -> player.sendMessage(CC.translate("&c审核失败 &7请重新申请审核"));
                                }
                                player.getInventory().addItem(to);
                            }, ThePit.getInstance());
                }
            } else if (player.getInventory().firstEmpty() != -1) {
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "CustomNamePaper").callEvent();
                new AnvilGUI.Builder()
                        .title("§e§l物品更名")
                        .text("")
                        .onClick((slot, stateSnapshot) -> Collections.singletonList(AnvilGUI.ResponseAction.close()))
                        .onClose(stateSnapshot -> {
                            String text = stateSnapshot.getText();
                            if (StringUtils.isNotEmpty(text) && CustomNamePaper.valid(text)) {
                                customNamePaper.setCustomName(text);
                                customNamePaper.setMillis(System.currentTimeMillis() + 1000L * 60 * 10);// 10m
                                customNamePaper.setUuid(UUID.randomUUID());
                                CustomPaperData data = new CustomPaperData();
                                data.setPlayerName(player.getName());
                                data.setUuid(customNamePaper.getUuid().toString());
                                data.setCustomName(text);
                                data.setTime(new Date());
                                data.save();
                                ThePit.getInstance().getCustomNameValidator().notify(customNamePaper.getUuid(),
                                        player.getName(),
                                        customNamePaper.getCustomName());
                            } else {
                                player.sendMessage(CC.translate("&c错误 &7请输入符合长度要求（1-12字符）的物品名称 "));
                            }
                            ItemStack to = customNamePaper.toItemStack();
                            player.getInventory().addItem(to);
                        })
                        .plugin(ThePit.getInstance())
                        .open(player);
            }
        }
//        else if (item.getType() == Material.MUTTON) {
//            if ("AAABeef".equals(internalName) && e.getAction().toString().contains("RIGHT_CLICK")) {
//                PlayerUtil.takeOneItem(player, hand);
//                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 200, 0));
//                PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 200, 0));
//                AAABeefStreak.beefEffectMap.put(player.getUniqueId(), new Cooldown(10, TimeUnit.SECONDS));
//            }
//        }
        else if (item.getType() == Material.GREEN_BANNER && e.getAction().toString().contains("LEFT_CLICK") && profile.isInArena()) {
            if ("gain_banner".equals(internalName)) {
                PlayerUtil.takeOneItem(player, hand);
                new PitConsumeItemEvent(player, "gain_banner").callEvent();
                player.getWorld().getBlockAt(player.getLocation()).setType(Material.GREEN_BANNER);
                CC.boardCast("&a增益旗帜 &7玩家 " + profile.getFormattedName() + " &7使用了一个增益旗帜,在10分钟内且在旗帜12格内将获得15%的金币,经验加成");
                CombatListener.addBanner(player.getLocation(), player.getUniqueId());
            }
        } else if (item.getType() == Material.RECOVERY_COMPASS && e.getAction() == Action.RIGHT_CLICK_AIR) {
            if ("soul_magnet".equals(internalName)) {

                List<Location> validChests = new ArrayList<>();
                for (Location loc : SewersRunnable.getExistSewersChests()) {
                    if (!loc.getBlock().getType().isAir()) {
                        validChests.add(loc);
                    }
                }

                if (validChests.isEmpty()) {
                    player.sendMessage(CC.translate("&c当前没有可用的下水道箱子!"));
                    return;
                }

                Location playerLoc = player.getLocation();
                Location nearestChest = null;
                double minDistance = Double.MAX_VALUE;

                for (Location chest : validChests) {
                    if (!chest.getWorld().equals(playerLoc.getWorld())) continue;

                    double distance = playerLoc.distance(chest);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestChest = chest;
                    }
                }

                if (nearestChest != null) {
                    String targetDirection = DirectionUtil.getTargetDirection(player, nearestChest);
                    int x = nearestChest.getBlockX();
                    int y = nearestChest.getBlockY();
                    int z = nearestChest.getBlockZ();
                    player.sendMessage(CC.translate("&2下水道磁石 &7方向: &a" + targetDirection +
                            ", 距离约 &a" + (int) minDistance + " &7格, 坐标: &aX: " + x + ", Y: " + y + ", Z: " + z));
                }
            }
        } else if (item.getType() == Material.PAPER && "mythicRandomVoucher".equals(internalName)) {
            e.setCancelled(true);
            if (InventoryUtil.isInvFull(player.getInventory())) {
                player.sendMessage(CC.translate("&c背包已满"));
                return;
            }
            ItemStack mythic = RandomUtil.helpMeToChooseOne(
                    new MythicSwordItem().toItemStack(),
                    new MythicBowItem().toItemStack(),
                    new MythicLeggingsItem().toItemStack()
            );
            if (mythic == null) return;

            PlayerUtil.takeOneItem(player, hand);
            player.getInventory().addItem(mythic);
            log.info("{}", ItemOperationJson.toJson(
                    ItemOperationJson.mythicGranted(player, "MYTHIC_RANDOM_VOUCHER", mythic)
            ));
            player.sendMessage(CC.translate("&a你获得了随机的神话装备"));
            new PitConsumeItemEvent(player, "mythicRandomVoucher").callEvent();
        }
//        else if(item.getType() == Material.EXPERIENCE_BOTTLE && "mysteriousBottle".equals(internalName)){
//            e.setCancelled(true);
//            PlayerUtil.takeOneItem(player, hand);
//            CC.send(MessageType.MISC,player,"&a你感觉有一股神秘的力量传入到了你的身体里");
//            AbstractPitBuff buff = new MythicBuff();
//            buff.stackBuff(player,1000L * 60 * 5, 1 ,true);
//        }
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        ItemStack item = event.getItem();
        if (item.getType() == Material.GOLDEN_APPLE) {
//            player.setAbsorptionAmount(8.0F);
            PlayerUtil.setAbsorption(player, 8.0F);

            PlayerUtil.takeOneItem(player, event.getHand());
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            if (PlayerUtil.isPlayerUnlockedPerk(player, PerksPrestigeEnum.YUMMY)) {
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                profile.setCoins(profile.getCoins() + 3);
            }
            new PitConsumeItemEvent(player, "golden_apple").callEvent();
            event.setCancelled(true);
        }
        if ("perk_olympus".equals(ItemUtil.getInternalName(item))) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            PlayerUtil.takeOneItem(player, event.getHand());
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 2));
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 24 * 20, 0));
            PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.RESISTANCE, 4 * 20, 1));
            profile.setExperience(profile.getExperience() + 27);
            new PitConsumeItemEvent(player, "perk_olympus").callEvent();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        final Player killer = event.getKiller();
        final Cooldown cooldown = firstAidEggCooldown.get(killer.getUniqueId());
        if (cooldown != null && !cooldown.hasExpired()) {
            firstAidEggCooldown.put(killer.getUniqueId(), new Cooldown(cooldown.getRemaining() - 5 * 1000L));
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
//        if (PlayerUtil.isStaffSpectating(event.getPlayer())) {
//            event.setCancelled(true);
//            return;
//        }
        if (ItemUtil.isDefaultItem(event.getItemDrop().getItemStack())) {
            event.getItemDrop().remove();
        }

        if (!ItemUtil.canDrop(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler
    public void onClickInv(InventoryClickEvent event) {
        if (event.getClickedInventory() instanceof CraftingInventory craftingInventory) {
            event.setCancelled(true);
            craftingInventory.clear();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory() instanceof CraftingInventory craftingInventory) {
            craftingInventory.clear();
            event.setCancelled(true);
        }
    }

    private void welcomePlayer(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.isNicked()) {
            player.sendMessage(CC.translate("&2&l匿名模式! &7你现在对外显示的游戏名为: " + profile.getFormattedNameWithRoman()));
        }
    }

    @EventHandler
    public void onThrowTnt(PlayerInteractEvent event) {
        if ("tnt".equals(ItemUtil.getInternalName(event.getItem()))) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);

                if (event.getPlayer().getItemInHand().getAmount() > 1) {
                    event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
                } else {
                    event.getPlayer().setItemInHand(null);
                }

                BlockIterator blockIterator = new BlockIterator(event.getPlayer());
                TNTPrimed tntPrimed = (TNTPrimed) event.getPlayer().getWorld().spawnEntity(event.getPlayer().getLocation(), EntityType.TNT);
                tntPrimed.setFuseTicks(30);
                for (int i = 0; i < 20; i++) {
                    blockIterator.next();
                }

                tntPrimed.setMetadata("internal", new FixedMetadataValue(ThePit.getInstance(), "tnt"));
                tntPrimed.setMetadata("shooter", new FixedMetadataValue(ThePit.getInstance(), event.getPlayer().getUniqueId().toString()));

                VectorUtil.entityPush(tntPrimed, blockIterator.next().getLocation(), 25);
            }
        } else if ("red_packet".equals(ItemUtil.getInternalName(event.getItem()))) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                event.setCancelled(true);

                if (event.getPlayer().getItemInHand().getAmount() > 1) {
                    event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount() - 1);
                } else {
                    event.getPlayer().setItemInHand(null);
                }

                BlockIterator blockIterator = new BlockIterator(event.getPlayer());
                TNTPrimed tntPrimed = (TNTPrimed) event.getPlayer().getWorld().spawnEntity(event.getPlayer().getLocation(), EntityType.TNT);
                tntPrimed.setFuseTicks(30);
                for (int i = 0; i < 20; i++) {
                    blockIterator.next();
                }

                Integer money = ItemUtil.getItemIntData(event.getItem(), "money");
                String sender = ItemUtil.getItemStringData(event.getItem(), "sender");
                if (!event.getPlayer().getUniqueId().toString().equals(sender)) {
                    event.getPlayer().sendMessage(CC.translate("&C你并非红包的主人"));
                    return;
                }

                tntPrimed.setMetadata("money", new FixedMetadataValue(ThePit.getInstance(), money));
                tntPrimed.setMetadata("internal", new FixedMetadataValue(ThePit.getInstance(), "red_packet"));
                tntPrimed.setMetadata("shooter", new FixedMetadataValue(ThePit.getInstance(), event.getPlayer().getUniqueId().toString()));

                VectorUtil.entityPush(tntPrimed, blockIterator.next().getLocation(), 25);
            }
        }
    }

    @EventHandler
    public void onPreExplosion(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            if (event.getEntity().hasMetadata("internal") && !event.getEntity().getMetadata("internal").isEmpty() && "red_packet".equals(event.getEntity().getMetadata("internal").get(0).asString())) {
                Location location = event.getEntity().getLocation();
                float radius = event.getRadius();

                List<Player> players = PlayerUtil.getNearbyPlayers(location, radius);
                Map<Player, Double> distanceMap = new HashMap<>();
                double distanceSum = players.stream().mapToDouble(player -> {
                    double distance = player.getLocation().distance(location);
                    distanceMap.put(player, distance);
                    return distance;
                }).sum();

                int money = event.getEntity().getMetadata("money").getFirst().asInt();
                String shooter = event.getEntity().getMetadata("shooter").getFirst().asString();

                for (Map.Entry<Player, Double> entry : distanceMap.entrySet()) {
                    Double distance = entry.getValue();
                    Player player = entry.getKey();


                    double value = 1 - (distance / (distanceSum));
                    int given = (int) (money * value);
                    PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(UUID.fromString(shooter));
                    String name = profile.getPlayerName();

                    player.sendMessage(CC.translate("&c&l红包! &7你接受到了来自 &6" + name + " &7的红包 &6+" + given + "硬币&7(#" + numFormat.format(value * 100) + "%)"));
                    PlayerProfile profileByUuid = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                    profileByUuid.setCoins(profileByUuid.getCoins() + given);
                }

            }
        }
    }

    @EventHandler
    public void onTntExplode(EntityExplodeEvent event) {
        if (event.getEntity().hasMetadata("shooter") && !event.getEntity().getMetadata("shooter").isEmpty()) {
            event.setYield(0);
            event.blockList().clear();
            if (event.getEntity().hasMetadata("internal") && !event.getEntity().getMetadata("internal").isEmpty() && (event.getEntity().getMetadata("internal").get(0).asString().equalsIgnoreCase("tnt_enchant_item") || event.getEntity().getMetadata("internal").get(0).asString().equalsIgnoreCase("insta_boom_enchant_item"))) {
                return;
            }
            List<FallingBlockEntity> fallingBlocks = new ArrayList<>();
            List<Player> players = new ArrayList<>();

            for (Block b : event.blockList()) {

                if (RandomUtil.random.nextBoolean()) {
                    continue;
                }

                double x = -2.0d + (Math.random() * 4.0d);

                double y = -3.0d + (Math.random() * 6.0d);

                double z = -2.0d + (Math.random() * 4.0d);

                Location location = event.getLocation();
                ServerLevel world = ((CraftWorld) location.getWorld()).getHandle();
                int blockX = location.getBlockX();
                int blockY = location.getBlockY();
                int blockZ = location.getBlockZ();

                BlockState blockState = CraftMagicNumbers.getBlock(b.getType()).defaultBlockState();
                FallingBlockEntity fallingBlock = new FallingBlockEntity(world, blockX, blockY, blockZ, blockState);
                fallingBlock.setDeltaMovement(x, y, z);

                fallingBlocks.add(fallingBlock);

                ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(fallingBlock);
                ClientboundSetEntityMotionPacket vectorPacket = new ClientboundSetEntityMotionPacket(fallingBlock);
                ClientboundSetEntityDataPacket metaPacket =
                        new ClientboundSetEntityDataPacket(fallingBlock.getId(), fallingBlock.getEntityData().packDirty());


                for (Player player : PlayerUtil.getNearbyPlayers(event.getLocation(), 30)) {
                    players.add(player);
                    ServerPlayer entityPlayer = ((CraftPlayer) player).getHandle();
                    entityPlayer.connection.sendPacket(spawnPacket);
                    entityPlayer.connection.sendPacket(metaPacket);
                    entityPlayer.connection.sendPacket(vectorPacket);
                }
            }

            Bukkit.getScheduler().runTaskLaterAsynchronously(ThePit.getInstance(), () -> {
                List<Integer> list = fallingBlocks.stream()
                        .map(Entity::getId)
                        .collect(Collectors.toList());

                int[] a = new int[list.size()];

                for (int i = 0; i < list.size(); i++) {
                    a[i] = list.get(i);
                }

                ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(a);
                for (Player player : players) {
                    ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
                }

            }, 20 * 5);
        }
    }

    @EventHandler
    public void onEnchant(PitPlayerEnchantEvent event) {
        List<AbstractEnchantment> beforeEnchants = new ArrayList<>(event.getBeforeItem().getEnchantments().keySet());
        List<AbstractEnchantment> afterEnchants = new ArrayList<>(event.getAfterItem().getEnchantments().keySet());
        beforeEnchants.removeIf(enchant -> enchant.getRarity() != EnchantmentRarity.RARE);
        afterEnchants.removeIf(enchant -> enchant.getRarity() != EnchantmentRarity.RARE);
        if (beforeEnchants.isEmpty() && !afterEnchants.isEmpty()) {
            Stream<AbstractMedal> stream = ThePit.getInstance().getMedalFactory().getMedals().stream()
                    .filter(abstractMedal -> abstractMedal.getInternalName().equals("GET_RARE_ENCHANT"));
            Optional<AbstractMedal> first = stream.findFirst();
            AbstractMedal medal = first.get();

            medal.addProgress(PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId()), 1);
        }

        AbstractMedal medal = ThePit.getInstance().getMedalFactory().getMedals().stream()
                .filter(abstractMedal -> abstractMedal.getInternalName().equals("STATUS_ENCHANT_TIMES"))
                .findFirst()
                .get();

        medal.addProgress(PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId()), 1);
    }


}
