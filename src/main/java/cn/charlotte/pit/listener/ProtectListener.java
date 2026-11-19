package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.DroppedEntityData;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.manager.BlockRestoreManager;
import cn.charlotte.pit.medal.impl.challenge.ObsidianBreakMedal;
import cn.charlotte.pit.mode.Mode;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerksBundlesEnum;
import cn.charlotte.pit.perk.PerksPassivesEnum;
import cn.charlotte.pit.runnable.ClearRunnable;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:48
 */
@AutoRegister
public class ProtectListener implements Listener {
    //很傻的将TNT移至白名单
    private final HashSet<Material> ALLOWED_PLACED_BLOCKS = new HashSet<>(List.of(
            Material.OBSIDIAN, Material.COBBLESTONE, Material.OAK_PLANKS, Material.BEDROCK, Material.TNT,
            Material.DARK_PRISMARINE, Material.SOUL_SAND, Material.DRIED_KELP_BLOCK
    ));


    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        event.getWorld().getEntities().clear();
    }

    @EventHandler
    public void onPhysic(BlockPhysicsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSandFall(EntityChangeBlockEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onMoistureChangeEvent(MoistureChangeEvent event) {
        event.setCancelled(true);
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Block hitBlock = event.getHitBlock();
        if (hitBlock != null) {
            Material type = hitBlock.getType();
            if (type == Material.DECORATED_POT) {
                event.setCancelled(true);
                if (event.getEntity() != null) {
                    event.getEntity().remove();
                }
            }
        }
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        Material type = clickedBlock.getType();
        if (type.equals(Material.DECORATED_POT) || type.equals(Material.GRINDSTONE) || type.equals(Material.HOPPER)
                || type.equals(Material.DISPENSER) || type.equals(Material.DROPPER)
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPortalEnter(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    /**
     * 在出生点禁止射箭
     */
    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).isInArena()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

        if (profile.isEditingMode()) {
            return;
        }

        EquipmentSlot hand = event.getHand();
        ItemStack itemInHand = event.getItemInHand();

        if (!profile.isInArena()) {
            event.setCancelled(true);
            return;
        }

        if ("rage_pit".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            event.setCancelled(true);
            return;
        }

        if ("pungent_kill_streak".equals(ItemUtil.getInternalName(itemInHand))) {
            event.setCancelled(true);
            return;
        }

        Location location = event.getBlock().getLocation();

        for (int i = -1; i < 1; i++) {
            for (int j = -1; j < 1; j++) {
                for (int k = -1; k < 1; k++) {
                    if (location.clone().add(i, j, k).getBlock().getType() == Material.ENDER_PEARL
                            || location.clone().add(i, j, k).getBlock().getType() == Material.WATER
                    ) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (!ALLOWED_PLACED_BLOCKS.contains(location.getBlock().getType())) {
            event.setCancelled(true);
            return;
        }

        if (location.getBlockY() >= ThePit.getInstance().getPitConfig().getArenaHighestY()) {
            event.setCancelled(true);
            return;
        }

        Location loc = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
        loc.setY(location.getY());
        if (location.distance(loc) < 7) {
            event.setCancelled(true);
        }

        //tnt block check
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.TNT) {
            if ("tnt_enchant_item".equals(ItemUtil.getInternalName(itemInHand))) {
                event.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                    return;
                }
                TNTPrimed tntPrimed = (TNTPrimed) player.getWorld().spawnEntity(block.getLocation(), EntityType.TNT);
                tntPrimed.setFuseTicks(30);

                tntPrimed.setMetadata("internal", new FixedMetadataValue(ThePit.getInstance(), "tnt_enchant_item"));
                tntPrimed.setMetadata("shooter", new FixedMetadataValue(ThePit.getInstance(), player.getUniqueId().toString()));
                int enchantLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "TNT");
                tntPrimed.setMetadata("damage", new FixedMetadataValue(ThePit.getInstance(), Math.max(0, enchantLevel)));
                return;
            }
            if ("insta_boom_enchant_item".equals(ItemUtil.getInternalName(itemInHand))) {
                event.setCancelled(true);
                PlayerUtil.takeOneItem(player, hand);
                if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                    return;
                }
                TNTPrimed tntPrimed = (TNTPrimed) player.getWorld().spawnEntity(block.getLocation(), EntityType.TNT);
                tntPrimed.setFuseTicks(0);

                tntPrimed.setMetadata("internal", new FixedMetadataValue(ThePit.getInstance(), "tnt_enchant_item"));
                tntPrimed.setMetadata("shooter", new FixedMetadataValue(ThePit.getInstance(), player.getUniqueId().toString()));
                int enchantLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "insta_boom_tnt_enchant");
                tntPrimed.setMetadata("damage", new FixedMetadataValue(ThePit.getInstance(), Math.max(0, enchantLevel)));

                return;
            }
        }

        long baseTime;
        if (block.getType() == Material.OBSIDIAN || block.getType() == Material.BEDROCK) {
            baseTime = 2400;
        } else {
            baseTime = 300;
        }

        boolean hasHermit;
        int passiveLevel = PerksPassivesEnum.BUILD_BATTLER.getPerk().getLevel(profile);
        if (passiveLevel > 0) {
            baseTime = (long) (baseTime * (1 + (0.6d * passiveLevel)));
        }
        long existTime = baseTime;

        hasHermit = PerksBundlesEnum.HERMIT.getPerk().hasEquip(profile);
        if (hasHermit) {
            existTime *= 2;
        }


        BlockState originalBlockState = event.getBlockReplacedState();
        long delayTicks = Math.round(existTime);

        BlockRestoreManager.INSTANCE.scheduleRestore(block.getLocation(), originalBlockState, delayTicks);
            /*
            if (ThePit.getInstance().getPitConfig().getArenaHighestY() - location.getY() <= 20) {
                existTime = 12L;
                ActionBarUtil.sendActionBar(event.getPlayer(), "&c你正在高处建筑,方块会更快消失!");
            }

             */
//        ClearRunnable.getClearRunnable().placeBlock(event.getBlock().getLocation(), new Cooldown(existTime, TimeUnit.SECONDS));
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent e) {
        if (ThePit.mode == Mode.Normal) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlace(PlayerBucketEmptyEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (!profile.isEditingMode()) {
            if (!profile.isInArena()) {
                event.setCancelled(true);
                return;
            }

            Location location = event.getBlockClicked().getLocation();

            for (int i = -3; i < 3; i++) {
                for (int j = -3; j < 3; j++) {
                    for (int k = -3; k < 3; k++) {
                        if (location.clone().add(i, j, k).getBlock().getType() == Material.LILY_PAD
                                || location.clone().add(i, j, k).getBlock().getType() == Material.WATER
                        ) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage(CC.translate("&c你无法修改此处方块!"));
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntity().getType() == EntityType.SNOW_GOLEM) {
            event.setCancelled(true);
        }
        if (event.getEntity() instanceof Item || event.getEntity() instanceof Arrow) {
            DroppedEntityData data = new DroppedEntityData();
            data.setTimer(new Cooldown(30, TimeUnit.SECONDS));
            data.setEntity(event.getEntity());

            ClearRunnable.getClearRunnable()
                    .getEntityData()
                    .add(data);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand stand) {
            if (stand.getHelmet() != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onWeatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
                return;
            }
//            if (PlayerUtil.isStaffSpectating((Player) event.getEntity())) {
//                event.setCancelled(true);
//                return;
//            }
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getEntity().getUniqueId());
            if (!profile.isInArena() && !PitBotManager.inTraining(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getEntity().getUniqueId());
            if (!profile.isInArena() && !PitBotManager.inTraining(event.getEntity())) {
                event.setCancelled(true);
            }
        }

        if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                Player shooter = (Player) (((Projectile) event.getDamager()).getShooter());
                if (event.getEntity().getUniqueId().equals(shooter.getUniqueId())) {
                    event.setCancelled(true);
                }
                PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(shooter.getUniqueId());
                if (!profile.isInArena() && !PitBotManager.inTraining(shooter)
                        && !PitBotManager.canAttackTrainingBot(shooter, event.getEntity())) {
                    event.setCancelled(true);
                }
            }
        }

        if (event.getDamager() instanceof Player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getDamager().getUniqueId());
            if (!profile.isInArena() && !PitBotManager.inTraining(event.getDamager())
                    && !PitBotManager.canAttackTrainingBot((Player) event.getDamager(), event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onBreak(BlockBreakEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (profile.isDebug()) {
            event.getPlayer().sendMessage(String.valueOf(event.getBlock().getType()));
        }
        if (profile.isEditingMode()) {
            return;
        }

        Block block = event.getBlock();

        event.setDropItems(false);
        if (event.getBlock().getType() == Material.OBSIDIAN) {
            if (profile.isDebug()) {
                event.getPlayer().sendMessage(CC.translate("&aYes |" + Utils.getEnchantLevel(event.getPlayer().getInventory().getLeggings(), "purple_gold")));
            }
            new ObsidianBreakMedal().addProgress(profile, 1);
            int level = Utils.getEnchantLevel(event.getPlayer().getInventory().getLeggings(), "purple_gold");
            if (PlayerUtil.isEquippingSomber(event.getPlayer()) || PlayerUtil.isVenom(event.getPlayer())) {
                level = 0;
            }
            if (level > 0) {
                profile.setCoins(profile.getCoins() + level * 3 + 3 - level);
                if (level >= 2) {
                    PotionEffect potionEffect = new PotionEffect(PotionEffectType.REGENERATION, level * 20, 2);
                    PlayerUtil.addEffect(event.getPlayer(), potionEffect);
                }
            }
        }
        if (BlockRestoreManager.INSTANCE.isScheduled(block.getLocation())) {
            BlockRestoreManager.INSTANCE.finishRestore(block.getLocation());
        } else {
            if (!profile.isEditingMode()) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    private void onBedEnter(PlayerBedEnterEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (!profile.isEditingMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDamage(PlayerItemDamageEvent event) {
        if (event.getItem().getItemMeta().isUnbreakable() || Util.matchesAny(Util.ifNull(ItemUtil.getInternalName(event.getItem()), ""),
                PitItems.ANGEL_CHEST_PLATE.getInternalName(), PitItems.ARMAGEDDON_BOOTS.getInternalName()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onShear(PlayerShearEntityEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (!profile.isEditingMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onEditBook(PlayerEditBookEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (!profile.isEditingMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onSignChange(SignChangeEvent event) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getPlayer().getUniqueId());
        if (!profile.isEditingMode()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onFoodLevel(FoodLevelChangeEvent event) {
        if (ThePit.mode == Mode.Normal) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent e) {
//        e.setCancelled(true);
    }

    @EventHandler
    public void onShoot(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile instanceof Arrow) {
            Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), projectile::remove, 20 * 5);
        }
        if (projectile instanceof Egg) {
            event.setCancelled(true);
        }
    }
}
