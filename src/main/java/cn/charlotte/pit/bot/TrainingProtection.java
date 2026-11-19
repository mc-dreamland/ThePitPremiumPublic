package cn.charlotte.pit.bot;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.event.PitStackBuffEvent;
import cn.charlotte.pit.listener.CombatListener;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

/** Protects players across the boundary; player melee/arrows may hit training bots from outside. */
final class TrainingProtection implements Listener {
    private final PitBotManager manager;
    private final NamespacedKey launchInside;
    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.MINING_FATIGUE,
            PotionEffectType.HUNGER, PotionEffectType.NAUSEA, PotionEffectType.DARKNESS, PotionEffectType.LEVITATION,
            PotionEffectType.INSTANT_DAMAGE);
    private static final Set<String> NEGATIVE_BUFFS = Set.of("beeVenom", "venom_debuff", "rotten_de_buff",
            "pin_down_de_buff", "hemorrhage_de_buff", "heal_poison_de_buff");

    TrainingProtection(PitBotManager manager, ThePit plugin) {
        this.manager = manager;
        launchInside = new NamespacedKey(plugin, "training-projectile-origin");
    }

    boolean forbidden(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity source = PitBotManager.source(event.getDamager());
        boolean toInside = PitBotManager.inTraining(target);
        boolean fromInside = PitBotManager.inTraining(source);
        // Preserve the old protection against pets/TNT; only player melee/projectiles gain PvP.
        if (toInside && target instanceof Player && source instanceof Player
                && !(event.getDamager() instanceof Player) && !(event.getDamager() instanceof Projectile)) return true;
        Boolean launchedInside = null;
        if (event.getDamager() instanceof Projectile projectile) {
            Byte origin = projectile.getPersistentDataContainer().get(launchInside, PersistentDataType.BYTE);
            if (origin != null) launchedInside = origin == 1;
        }
        return TrainingCombatRules.blocked(fromInside, toInside, source instanceof Player, target instanceof Player,
                PitBotManager.isBot(source), PitBotManager.isBot(target), launchedInside);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageFirst(EntityDamageByEntityEvent event) {
        if (forbidden(event)) event.setCancelled(true);
        else if (!event.isCancelled()) manager.scaleDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageLast(EntityDamageByEntityEvent event) {
        if (forbidden(event)) event.setCancelled(true);
        else if (!event.isCancelled() && PitBotManager.isTrainingPvp(PitBotManager.source(event.getDamager()), event.getEntity())) {
            // Run after normal perk/enchant calculations, not at LOWEST where minimum-damage
            // clamps and flat bonuses would overwrite or bypass the reduction.
            CombatListener.applyDiamondSwordBonus(event);
            TrainingDamageScaling.scale(event, TrainingCombatRules.playerDamageMultiplier(true, true));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnvironment(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (PitBotManager.isBot(event.getEntity()) || PitBotManager.inTraining(event.getEntity())) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        Entity shooter = PitBotManager.source(event.getEntity());
        event.getEntity().getPersistentDataContainer().set(launchInside, PersistentDataType.BYTE,
                (byte) (PitBotManager.inTraining(shooter) ? 1 : 0));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (PitBotManager.inTraining(event.getEntity()) || PitBotManager.isBot(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotion(PotionSplashEvent event) {
        boolean fromInside = PitBotManager.inTraining(PitBotManager.source(event.getPotion()));
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (PitBotManager.isBot(entity) || (entity instanceof Player && (fromInside || PitBotManager.inTraining(entity)))) event.setIntensity(entity, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent event) {
        boolean fromInside = PitBotManager.inTraining(PitBotManager.source(event.getEntity()));
        event.getAffectedEntities().removeIf(entity -> PitBotManager.isBot(entity)
                || (entity instanceof Player && (fromInside || PitBotManager.inTraining(entity))));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEffect(EntityPotionEffectEvent event) {
        // Also blocks vanilla husk hunger and hostile player auras which bypass splash events.
        if (PitBotManager.isBot(event.getEntity())) event.setCancelled(true);
        else if (PitBotManager.inTraining(event.getEntity()) && event.getNewEffect() != null
                && NEGATIVE_EFFECTS.contains(event.getNewEffect().getType())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBuff(PitStackBuffEvent event) {
        if (PitBotManager.inTraining(event.getPlayer()) && NEGATIVE_BUFFS.contains(event.getBuff().getInternalBuffName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getCaught() != null && (PitBotManager.inTraining(event.getCaught()) || PitBotManager.inTraining(event.getPlayer()))) {
            event.setCancelled(true);
            event.getHook().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (PitBotManager.inTraining(event.getBlockPlaced().getLocation())
                && !PitBotManager.profile(event.getPlayer()).isEditingMode()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (PitBotManager.inTraining(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation())
                && !PitBotManager.profile(event.getPlayer()).isEditingMode()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (PitBotManager.inTraining(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (PitBotManager.inTraining(event.getToBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> PitBotManager.inTraining(block.getLocation()));
    }
}
