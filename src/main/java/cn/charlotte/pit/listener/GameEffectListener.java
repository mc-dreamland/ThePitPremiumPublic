package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.KillRecap;
import cn.charlotte.pit.data.sub.QuestData;
import cn.charlotte.pit.enchantment.AbstractEnchantment;
import cn.charlotte.pit.enchantment.EnchantmentFactor;
import cn.charlotte.pit.enchantment.param.event.NotPlayerOnly;
import cn.charlotte.pit.enchantment.param.event.PlayerOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.event.PitDamageEvent;
import cn.charlotte.pit.event.PitOriginalTimeChangeEvent;
import cn.charlotte.pit.event.PitUltimatelyDamageEvent;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.parm.listener.*;
import cn.charlotte.pit.parm.type.BowOnly;
import cn.charlotte.pit.parm.type.ThrowOnly;
import cn.charlotte.pit.perk.AbstractHandle;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.quest.QuestFactory;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/2 12:40
 */
@AutoRegister
@Slf4j
public class GameEffectListener implements Listener {
    private static final Map<Class<?>, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private final DecimalFormat numFormatTwo = new DecimalFormat("0.00");

    @SneakyThrows
    public static void processKilled(IPlayerKilledEntity ins, int level, Player killer, Entity target, ResourceManager resourceManager) {
        if (level != -1) {
            final Method method = ins.getClass().getMethod("handlePlayerKilled", int.class, Player.class, Entity.class, ResourceManager.class);
            if (method.isAnnotationPresent(PlayerOnly.class)) {
                if (target instanceof Player player) {
                    ins.handlePlayerKilled(level, killer, player, resourceManager);
                }
            } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
                if (!(target instanceof Player)) {
                    ins.handlePlayerKilled(level, killer, target, resourceManager);
                }
            } else {
                ins.handlePlayerKilled(level, killer, target, resourceManager);
            }
        }
    }

//    @EventHandler(priority = EventPriority.LOW)
//    public void modifyLeatherArmor(EntityDamageByEntityEvent event) {
//        if (!(event.getEntity() instanceof Player player)) return;
//
//        double originalDamage = event.getDamage();
//        double finalDamage = originalDamage;
//
//        // 检查头盔
//        if ("kings_helmet".equals(ItemUtil.getInternalName(player.getInventory().getHelmet()))) {
//            finalDamage -= originalDamage * 0.06;
//        }
//
//        // 检查护腿
//        if ("mythic_leggings".equals(ItemUtil.getInternalName(player.getInventory().getLeggings()))) {
//            if (!"dark".equals(ItemUtil.getItemStringData(player.getInventory().getLeggings(), "mythic_color"))) {
//                finalDamage -= originalDamage * 0.12;
//            }
//        }
//
//
//        if (finalDamage < 0) {
//            finalDamage = 0;
//        }
//
//        event.setDamage(finalDamage);
//    }

    @SneakyThrows
    public static void processBeKilledByEntity(IPlayerBeKilledByEntity ins, int level, Player myself, Entity target, ResourceManager resourceManager) {
        if (level != -1) {
            final Method method = ins.getClass().getMethod("handlePlayerBeKilledByEntity", int.class, Player.class, Entity.class, ResourceManager.class);
            if (method.isAnnotationPresent(PlayerOnly.class)) {
                if (target instanceof Player player) {
                    ins.handlePlayerBeKilledByEntity(level, myself, player, resourceManager);
                }
            } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
                if (!(target instanceof Player)) {
                    ins.handlePlayerBeKilledByEntity(level, myself, target, resourceManager);
                }
            } else {
                ins.handlePlayerBeKilledByEntity(level, myself, target, resourceManager);
            }
        }
    }

    public static void processBeKilledByEntity(TypedPerk<?> typedPerk, int level, Player myself, Entity target, ResourceManager resourceManager) {
        if (level != -1) {
            try {
                final Method method = ((AbstractHandle) typedPerk).getClass().getMethod("handlePlayerBeKilledByEntity", int.class, Player.class, Entity.class, ResourceManager.class);
                if (method.isAnnotationPresent(PlayerOnly.class)) {
                    if (target instanceof Player player) {
                        typedPerk.handlePlayerBeKilledByEntity(level, myself, player, resourceManager);
                    }
                } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
                    if (!(target instanceof Player)) {
                        typedPerk.handlePlayerBeKilledByEntity(level, myself, target, resourceManager);
                    }
                } else {
                    typedPerk.handlePlayerBeKilledByEntity(level, myself, target, resourceManager);
                }
            } catch (NoSuchMethodException ignored) {
                ignored.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFired(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)) {
            if (PitBotManager.inTraining(player)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
            event.setCancelled(true);
            if (player.getHealth() <= event.getFinalDamage()) {
                player.damage(50000);
                event.setCancelled(true);
            } else {
                player.setHealth(player.getHealth() - event.getFinalDamage());
            }

            PlayerProfile.getPlayerProfileByUuid(player.getUniqueId())
                    .setNoDamageAnimations(true);
        }
    }

//    @EventHandler
//    public void onItemDamaged(PlayerItemDamageEvent event) {
//        ItemStack item = event.getItem();
//        if(item.getItemMeta().isUnbreakable()) return;
//
//        event.setCancelled(true);
//        AtomicBoolean atomicBoolean = new AtomicBoolean();
//        for (IItemDamage itemDamage : ThePit.getInstance()
//                .getEnchantmentFactor()
//                .getiItemDamages()) {
//            int level = ((AbstractEnchantment) itemDamage).getItemEnchantLevel(event.getPlayer().getItemInHand());
//
//            itemDamage.handleItemDamaged(level, event.getItem(), event.getPlayer(), atomicBoolean);
//        }
//        if (atomicBoolean.get()) {
//            event.setCancelled(true);
//        }
//    }

    @EventHandler
    public void addEnchantToArrow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            StringBuilder sb = new StringBuilder();
            for (IPlayerShootEntity instance : ThePit.getInstance()
                    .getEnchantmentFactor()
                    .getPlayerShootEntities()) {
                final AbstractEnchantment enchant = (AbstractEnchantment) instance;
                final int level = enchant.getItemEnchantLevel(event.getBow());
                if (level > 0) {
                    sb.append(enchant.getNbtName())
                            .append(":")
                            .append(level)
                            .append(";");
                }
            }

            if (sb.length() == 0) {
                return;
            }
            event.getProjectile().setMetadata("enchant", new FixedMetadataValue(ThePit.getInstance(), sb.substring(0, sb.length() - 1)));
        }
    }

    @EventHandler
    public void onTimeChange(PitOriginalTimeChangeEvent event) {
        CC.boardCast("Time change to: " + event.getTime());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (PitBotManager.skipCustomCombat(event)) return;
        if (event.getEntity().hasMetadata("critical_hit_processed")) {
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            if (PlayerUtil.isCritical(attacker)) {
                event.getEntity().setMetadata("critical_hit_processed", new FixedMetadataValue(ThePit.getInstance(), true));
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> event.getEntity().removeMetadata("critical_hit_processed", ThePit.getInstance()), 1L);
            }
        }
//        if (event.getDamager() instanceof Player attacker) {
//            if (attacker.getInventory().getItemInMainHand().getType() == Material.AIR || attacker.getInventory().getItemInMainHand() == null) {
//                event.setDamage(1);
//            } else {
//                float f = (float) attacker.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
//                if (f > 9) {
//                    event.setDamage(0);
//                }
//            }
//        }

        EnchantmentFactor enchantmentFactor = ThePit.getInstance().getEnchantmentFactor();
        QuestFactory questFactory = ThePit.getInstance().getQuestFactory();

        AtomicDouble finalDamage = new AtomicDouble(0);
        AtomicDouble boostDamage = new AtomicDouble(1);
        AtomicBoolean cancel = new AtomicBoolean(false);
        Player damager = null;
        boolean shoot = false;
        Map<String, Integer> playerItemEnchantmentMap;
        Map<String, Integer> playerLeggingEnchantmentMap;

        if (event.getDamager() instanceof Player attacker) {
            if (PlayerUtil.isCritical(attacker)) {
                event.getEntity().setMetadata("critical_hit_processed", new FixedMetadataValue(ThePit.getInstance(), true));
                Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                    event.getEntity().removeMetadata("critical_hit_processed", ThePit.getInstance());
                }, 1L);
            }
            damager = (Player) event.getDamager();
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());
            if (NewConfiguration.isNoobProtect() && profile.getPrestige() <= 0 && profile.getLevel() < NewConfiguration.getNoobProtectLevel()) {
                boostDamage.getAndAdd(NewConfiguration.getNoobDamageBoost() - 1);
            }
            if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
                for (TypedPerk<?> activePerk : profile.getActivePerks()) {
                    processAttackEntity(activePerk, activePerk.getLevel(profile), damager, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            }

            boolean shouldIgnoreEnchantsDueToPrestige = profile.getPrestige() == 0;
            Map<String, Integer> attackerSword = Util.getEnchantments(attacker.getInventory().getItemInMainHand());
            Map<String, Integer> attackerLeggins = Util.getEnchantments(attacker.getInventory().getLeggings());
            boolean isVenom = PlayerUtil.isVenom(attacker);

            if (!shouldIgnoreEnchantsDueToPrestige) {
                boolean damgerIsDark = PlayerUtil.isEquippingSomber(damager) || PlayerUtil.isVenom(damager);
                boolean venomActive = isVenom || PlayerUtil.isVenom(damager);
                boolean tarIsDark = false;
                boolean tarHasArmageddon = false;
                if (event.getEntity() instanceof Player targetPlayer) {
                    tarIsDark = PlayerUtil.isEquippingSomber(targetPlayer) || PlayerUtil.isVenom(targetPlayer);
                    tarHasArmageddon = PlayerUtil.isEquippingArmageddon(targetPlayer) || Util.getPitProfile(targetPlayer).getBuffData().getBuff("purification_buff").getTier() > 0;
                }

                Map<String, AbstractEnchantment> enchantmentMap = enchantmentFactor.getEnchantmentMap();
                for (String nbt : attackerSword.keySet()) {
                    AbstractEnchantment abs = enchantmentMap.get(nbt);
                    if (abs != null && IAttackEntity.class.isAssignableFrom(abs.getClass())) {
                        boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                        if (venomActive && !isDarkEnchant)
                            continue;
                        if (damgerIsDark && !tarIsDark && !tarHasArmageddon)
                            continue;
                        processAttackEntity((IAttackEntity) abs, attackerSword.get(nbt), damager, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }
                for (String nbt : attackerLeggins.keySet()) {
                    AbstractEnchantment abs = enchantmentMap.get(nbt);
                    if (abs != null && IAttackEntity.class.isAssignableFrom(abs.getClass())) {
                        boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                        if (venomActive && !isDarkEnchant)
                            continue;
                        if (damgerIsDark && !tarIsDark && !tarHasArmageddon)
                            continue;
                        processAttackEntity((IAttackEntity) abs, attackerLeggins.get(nbt), damager, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }
            }

            for (IAttackEntity ins : questFactory.getAttackEntities()) {
                AbstractQuest quest = (AbstractQuest) ins;
                QuestData currentQuest = profile.getCurrentQuest();
                if (currentQuest != null && currentQuest.getInternalName().equals(quest.getQuestInternalName())) {
                    processAttackEntity(ins, currentQuest.getLevel(), damager, event.getEntity(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            }
        } else if (event.getDamager() instanceof Projectile projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            damager = (Player) (((Projectile) event.getDamager()).getShooter());
            shoot = true;
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());

            if (NewConfiguration.isNoobProtect() && profile.getPrestige() <= 0 && profile.getLevel() < NewConfiguration.getNoobProtectLevel()) {
                boostDamage.getAndAdd(NewConfiguration.getNoobDamageBoost() - 1);
            }

            if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
                for (TypedPerk<?> activePerk : profile.getActivePerks()) {
                    int level = activePerk.getLevel(profile);
                    processShootEntity(activePerk, level, damager, event.getEntity(), event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            }

            boolean shouldIgnoreEnchantsDueToPrestige = profile.getPrestige() == 0;

            boolean somberFound = PlayerUtil.isEquippingSomber(damager);
            if (!somberFound && event.getEntity() instanceof Player) {
                somberFound = PlayerUtil.isEquippingSomber((Player) event.getEntity());
            }
            boolean comboVenomFound = false;
            if (damager.getMetadata("combo_venom") != null) {
                final long now = System.currentTimeMillis();
                final List<MetadataValue> metadata = damager.getMetadata("combo_venom");
                final Optional<MetadataValue> first = metadata.stream()
                        .filter(metadataValue -> metadataValue.getOwningPlugin() instanceof ThePit && now <= metadataValue.asLong())
                        .findFirst();
                if (first.isPresent()) {
                    comboVenomFound = true;
                }
            }

            if (!shouldIgnoreEnchantsDueToPrestige && projectile.hasMetadata("enchant")) {
                final MetadataValue value = projectile.getMetadata("enchant").getFirst();
                final String enchants = value.asString();
                final String[] split = enchants.split(";");
                for (String enchantStr : split) {
                    final String[] enchStr = enchantStr.split(":");
                    final String enchantName = enchStr[0];
                    final int enchantValue = Integer.parseInt(enchStr[1]);

                    final AbstractEnchantment enchantment = enchantmentFactor.getEnchantmentMap().get(enchantName);

                    if ((PlayerUtil.shouldIgnoreEnchant(damager) || (event.getEntity() instanceof Player && PlayerUtil.shouldIgnoreEnchant(damager, (Player) event.getEntity()))) && (enchantment.getRarity() != EnchantmentRarity.DARK_NORMAL && enchantment.getRarity() != EnchantmentRarity.DARK_RARE)) {
                        continue;
                    }

                    this.processShootEntity((IPlayerShootEntity) enchantment, enchantValue, damager, event.getEntity(), event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            }

            for (IPlayerShootEntity ins : questFactory.getPlayerShootEntities()) {
                AbstractQuest quest = (AbstractQuest) ins;
                QuestData currentQuest = profile.getCurrentQuest();
                if (currentQuest != null && currentQuest.getInternalName().equals(quest.getQuestInternalName())) {
                    processShootEntity(ins, currentQuest.getLevel(), damager, event.getEntity(), event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                }
            }
        }

        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(event.getEntity().getUniqueId());

            boolean shouldIgnoreEnchantsDueToPrestige = profile.getPrestige() == 0;

            if (NewConfiguration.isNoobProtect() && profile.getPrestige() <= 0 && profile.getLevel() < NewConfiguration.getNoobProtectLevel()) {
                boostDamage.getAndAdd(NewConfiguration.getNoobDamageReduce() - 1);
            }
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.getType().equals(Material.DIAMOND_CHESTPLATE) && PitItems.ANGEL_CHEST_PLATE.isInternalNameEquals(chestplate)) {
                boostDamage.getAndAdd(-0.1);
            }
//            if (PlayerUtil.isEquippingAngelChestplate(player)) {
//                boostDamage.getAndAdd(-0.1);
//            }

            if (event.getDamager() instanceof Player) {
                damager = (Player) event.getDamager();
                PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
                    for (TypedPerk<?> activePerk : playerProfile.getActivePerks()) {
                        int level = activePerk.getLevel(playerProfile);
                        processPlayerDamaged(activePerk, level, player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }


                if (!shouldIgnoreEnchantsDueToPrestige) {
                    boolean damgerIsDark = PlayerUtil.isEquippingSomber(damager) || PlayerUtil.isVenom(damager);
                    boolean tarIsDark = PlayerUtil.isEquippingSomber(player) || PlayerUtil.isVenom(player);
                    boolean venomActive = PlayerUtil.isVenom(damager) || PlayerUtil.isVenom(player);
                    boolean tarHasArmageddon = PlayerUtil.isEquippingArmageddon(player) || Util.getPitProfile(player).getBuffData().getBuff("purification_buff").getTier() > 0;

                    playerLeggingEnchantmentMap = Util.getEnchantments(player.getInventory().getLeggings());
                    playerItemEnchantmentMap = Util.getEnchantments(player.getInventory().getItemInMainHand());
                    Map<String, AbstractEnchantment> enchantmentMap = enchantmentFactor.getEnchantmentMap();
                    for (String nbt : playerLeggingEnchantmentMap.keySet()) {
                        AbstractEnchantment abs = enchantmentMap.get(nbt);
                        if (abs != null && IPlayerDamaged.class.isAssignableFrom(abs.getClass())) {
                            boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                            if(venomActive && !isDarkEnchant)
                                continue;
                            if(damgerIsDark && !tarIsDark && !tarHasArmageddon)
                                continue;
                            processPlayerDamaged((IPlayerDamaged) abs, playerLeggingEnchantmentMap.get(nbt), player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                        }
                    }

                    for (String nbt : playerItemEnchantmentMap.keySet()) {
                        AbstractEnchantment abs = enchantmentMap.get(nbt);
                        if (abs != null && IPlayerDamaged.class.isAssignableFrom(abs.getClass())) {
                            boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                            if(venomActive && !isDarkEnchant)
                                continue;
                            if(damgerIsDark && !tarIsDark && !tarHasArmageddon)
                                continue;
                            processPlayerDamaged((IPlayerDamaged) abs, playerItemEnchantmentMap.get(nbt), player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                        }
                    }


//                    for(IPlayerDamaged ins : enchantmentFactor.getPlayerDamageds()){
//                        AbstractEnchantment enchant = (AbstractEnchantment) ins;
//
//                        if((PlayerUtil.shouldIgnoreEnchant(damager) || (event.getEntity() instanceof Player && PlayerUtil.shouldIgnoreEnchant(damager,(Player) event.getEntity()))) && (enchant.getRarity() != EnchantmentRarity.DARK_NORMAL && enchant.getRarity() != EnchantmentRarity.DARK_RARE)){
//                            continue;
//                        }
//
//                        int level = enchant.getItemEnchantLevel(player.getItemInHand());
//                        if(player.getItemInHand() != null && player.getItemInHand().getType() != Material.AIR && player.getItemInHand().getType() != Material.LEATHER_LEGGINGS){
//                            processPlayerDamaged(ins,level,player,event.getDamager(),event.getFinalDamage(),finalDamage,boostDamage,cancel);
//                        }
//                        if(player.getInventory().getLeggings() != null && player.getInventory().getLeggings().getType() != Material.AIR){
//                            level = enchant.getItemEnchantLevel(player.getInventory().getLeggings());
//                            processPlayerDamaged(ins,level,player,event.getDamager(),event.getFinalDamage(),finalDamage,boostDamage,cancel);
//                        }
//                    }
                }

                for (IPlayerDamaged ins : questFactory.getPlayerDamageds()) {
                    AbstractQuest quest = (AbstractQuest) ins;
                    QuestData currentQuest = profile.getCurrentQuest();
                    if (currentQuest != null && currentQuest.getInternalName().equals(quest.getQuestInternalName())) {
                        processPlayerDamaged(ins, currentQuest.getLevel(), player, event.getDamager(), event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }
            } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
                damager = (Player) ((Projectile) event.getDamager()).getShooter();
                PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                if (!"spire".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
                    for (TypedPerk<?> activePerk : playerProfile.getActivePerks()) {
                        processPlayerDamaged(activePerk, activePerk.getLevel(playerProfile), player, damager, event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }

                if (!shouldIgnoreEnchantsDueToPrestige) {
                    boolean damgerIsDark = PlayerUtil.isEquippingSomber(damager) || PlayerUtil.isVenom(damager);
                    boolean tarIsDark = PlayerUtil.isEquippingSomber(player) || PlayerUtil.isVenom(player);
                    boolean venomActive = PlayerUtil.isVenom(damager) || PlayerUtil.isVenom(player);
                    boolean tarHasArmageddon = PlayerUtil.isEquippingArmageddon(player) || Util.getPitProfile(player).getBuffData().getBuff("purification_buff").getTier() > 0;

                    playerLeggingEnchantmentMap = Util.getEnchantments(player.getInventory().getLeggings());
                    playerItemEnchantmentMap = Util.getEnchantments(player.getInventory().getItemInMainHand());
                    Map<String, AbstractEnchantment> enchantmentMap = enchantmentFactor.getEnchantmentMap();
                    for(String nbt : playerLeggingEnchantmentMap.keySet()){
                        AbstractEnchantment abs = enchantmentMap.get(nbt);
                        if(abs != null && IPlayerDamaged.class.isAssignableFrom(abs.getClass())){
                            boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                            if(venomActive && !isDarkEnchant)
                                continue;
                            if(damgerIsDark && !tarIsDark && !tarHasArmageddon)
                                continue;
                            processPlayerDamaged((IPlayerDamaged) abs,playerLeggingEnchantmentMap.get(nbt),damager,event.getEntity(),event.getFinalDamage(),finalDamage,boostDamage,cancel);
                        }
                    }
                    for(String nbt : playerItemEnchantmentMap.keySet()){
                        AbstractEnchantment abs = enchantmentMap.get(nbt);
                        if(abs != null && IPlayerDamaged.class.isAssignableFrom(abs.getClass())){
                            boolean isDarkEnchant = abs.getRarity() == EnchantmentRarity.DARK_NORMAL || abs.getRarity() == EnchantmentRarity.DARK_RARE;
                            if(venomActive && !isDarkEnchant)
                                continue;
                            if(damgerIsDark && !tarIsDark && !tarHasArmageddon)
                                continue;
                            processPlayerDamaged((IPlayerDamaged) abs,playerItemEnchantmentMap.get(nbt),damager,event.getEntity(),event.getFinalDamage(),finalDamage,boostDamage,cancel);
                        }
                    }
//                    for(IPlayerDamaged ins : enchantmentFactor.getPlayerDamageds()){
//                        AbstractEnchantment enchant = (AbstractEnchantment) ins;
//                        if((PlayerUtil.shouldIgnoreEnchant(damager) || (event.getEntity() instanceof Player && PlayerUtil.shouldIgnoreEnchant(damager,(Player) event.getEntity()))) && (enchant.getRarity() != EnchantmentRarity.DARK_NORMAL && enchant.getRarity() != EnchantmentRarity.DARK_RARE)){
//                            continue;
//                        }
//                        int level = enchant.getItemEnchantLevel(player.getItemInHand());
//                        if(player.getItemInHand() != null && player.getItemInHand().getType() != Material.AIR && player.getItemInHand().getType() != Material.LEATHER_LEGGINGS){
//                            processPlayerDamaged(ins,level,player,event.getDamager(),event.getFinalDamage(),finalDamage,boostDamage,cancel);
//                        }
//                        if(player.getInventory().getLeggings() != null && player.getInventory().getLeggings().getType() != Material.AIR){
//                            level = enchant.getItemEnchantLevel(player.getInventory().getLeggings());
//                            processPlayerDamaged(ins,level,player,damager,event.getFinalDamage(),finalDamage,boostDamage,cancel);
//                        }
//                    }
                }

                for (IPlayerDamaged ins : questFactory.getPlayerDamageds()) {
                    AbstractQuest quest = (AbstractQuest) ins;
                    QuestData currentQuest = profile.getCurrentQuest();
                    if (currentQuest != null && currentQuest.getInternalName().equals(quest.getQuestInternalName())) {
                        processPlayerDamaged(ins, currentQuest.getLevel(), player, damager, event.getFinalDamage(), finalDamage, boostDamage, cancel);
                    }
                }
            }
        }

        if (cancel.get()) {
            event.setCancelled(true);
            assert damager != null;
        }

        // TODO damager存在为NULL的情况，后续需排查。
        PitDamageEvent pitDamageEvent = new PitDamageEvent(
                damager,
                event.getEntity(),
                finalDamage,
                boostDamage,
                event.getDamage(),
                shoot,
                cancel.get()
        );

        pitDamageEvent.callEvent();

        if (pitDamageEvent.isCancel()) {
            event.setCancelled(true);
            return;
        }

        finalDamage = pitDamageEvent.getFinalDamage();
        boostDamage = pitDamageEvent.getBoostDamage();
        // This component is applied by setHealth below, bypassing the Bukkit damage event.
        // Ordinary damage is reduced separately at HIGHEST by TrainingProtection.
        finalDamage.set(PitBotManager.scalePvpDamage(damager, event.getEntity(), finalDamage.get()));

        event.setDamage(Math.max(1, pitDamageEvent.getDamage()) * Math.max(0.2, boostDamage.get()));

        if (damager != null) {
            // new PitDamageEvent(damager, event.getFinalDamage(), event.getDamage()).callEvent();

            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());
            List<KillRecap.DamageData> damageLogs = profile.getKillRecap().getDamageLogs();
            if (!damageLogs.isEmpty()) {
                damageLogs.getLast().setBoostDamage(boostDamage.get());
            }
        }

        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());

//            if (damager != null) {
//                new PitDamagePlayerEvent(damager, event.getFinalDamage(), event.getDamage(), player).callEvent();
//            }

            if (profile.getPrestige() != 0) {
                if (player.getInventory().getLeggings() != null) {
                    int enchantLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "Mirror");
                    if (enchantLevel > 1 && finalDamage.get() > 0 && finalDamage.get() < 1000) {
                        if (event.getDamager() instanceof Player && (!player.hasMetadata("mirror_latest_active") || (player.getMetadata("mirror_latest_active").size() > 0 && System.currentTimeMillis() - player.getMetadata("mirror_latest_active").get(0).asLong() > 500L))) {
                            player.setMetadata("mirror_latest_active", new FixedMetadataValue(ThePit.getInstance(), System.currentTimeMillis()));
                            damager = (Player) event.getDamager();
                            if (!player.getUniqueId().equals(damager.getUniqueId())) {
                                damager.damage(0.01, player);
                            }
                            float mirrorDamage = (float) (((enchantLevel * 25 - 25) * 0.01) * finalDamage.get());
                            damager.setHealth(Math.max(0.1, damager.getHealth() - mirrorDamage));
                        }
                    }
                    if (enchantLevel > 0 && finalDamage.get() > 0 && finalDamage.get() < 1000) {
                        finalDamage.set(0);
                    }
                }
            }

            if (damager != null) {
                ((CraftPlayer) player).getHandle().lastHurtByPlayer = ((CraftPlayer) damager).getHandle();
            }

            if (player.getHealth() <= finalDamage.get()) {
                event.setCancelled(true);
                if (PitBotManager.isTrainingPvp(damager, player)) {
                    // Unattributed/environment damage remains blocked in training.
                    CombatListener.handlePlayerDeath(player, damager, true);
                    return;
                }
                player.damage(500000.0);
            } else {
                AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                player.setHealth(Math.min(attribute.getValue(), Math.max(player.getHealth() - finalDamage.get(), 0)));
            }
        }
        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getPlayerOption().isDebugDamageMessage()) {
                player.sendMessage(CC.translate("&7受到伤害(Dmg/FDmg/CHeal): &c" + numFormatTwo.format(event.getDamage())
                        + "&7/&c" + numFormatTwo.format(event.getFinalDamage())
                        + "&7/&c" + numFormatTwo.format(player.getHealth()))
                );
            }
        }
        if (event.getDamager() instanceof Player player) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
            if (profile.getPlayerOption().isDebugDamageMessage()) {
                player.sendMessage(CC.translate("&7造成伤害(Damage/Final Damage): &c" + numFormatTwo.format(event.getDamage()) + "&7/&c" + numFormatTwo.format(event.getFinalDamage())));
                final double value = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
                final boolean critical = PlayerUtil.isCritical(player);
                player.sendMessage(CC.translate("&7基础: &c" + value + "&7,伤害系数: &c" + boostDamage.get() + "&7,额外伤害: &c" + numFormatTwo.format(event.getDamage() - value * (critical ? 1.5 : 1) * 2) + "&7,暴击: &c" + critical));
            }
        }

        PitUltimatelyDamageEvent ultimatelyDamageEvent = new PitUltimatelyDamageEvent(
                damager,
                event.getEntity(),
                // The Bukkit hit is reduced later, but event damage statistics must agree.
                PitBotManager.scalePvpDamage(damager, event.getEntity(), event.getFinalDamage())
        );
        ultimatelyDamageEvent.callEvent();
    }

    @SneakyThrows
    private void processShootEntity(TypedPerk<?> typedPerk, int level, Player damager, Entity target, Entity
            damageSource, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;
        //            final Method handleShootEntity = ((AbstractHandle) typedPerk).getClass().getMethod("handleShootEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        Method handleShootEntity = METHOD_CACHE.computeIfAbsent(typedPerk.getClass(), clazz -> {
            try {
                return clazz.getMethod("handleShootEntity", int.class, Player.class, Entity.class,
                        double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        if (handleShootEntity == null) return;
        if (handleShootEntity.isAnnotationPresent(BowOnly.class) && !(damageSource instanceof Arrow)) {
            return;
        }
        if (handleShootEntity.isAnnotationPresent(ThrowOnly.class) && damager instanceof Arrow) {
            return;
        }
        if (typedPerk.getClass().isAnnotationPresent(PlayerOnly.class) || handleShootEntity.isAnnotationPresent(PlayerOnly.class)) {
            if (target instanceof Player player) {
                typedPerk.handleShootEntity(level, damager, player, damage, finalDamage, boostDamage, cancel);
            }
        } else if (typedPerk.getClass().isAnnotationPresent(NotPlayerOnly.class)) {
            if (!(target instanceof Player)) {
                typedPerk.handleShootEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
            }
        } else {
            typedPerk.handleShootEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
        }
    }

    @SneakyThrows
    private void processShootEntity(IPlayerShootEntity ins, int level, Player damager, Entity target, Entity
            damageSource, double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;

//            Method handleShootEntity = ins.getClass().getMethod("handleShootEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        Method handleShootEntity = METHOD_CACHE.computeIfAbsent(ins.getClass(), clazz -> {
            try {
                return clazz.getMethod("handleShootEntity", int.class, Player.class, Entity.class,
                        double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        if (handleShootEntity == null) return;
        if (handleShootEntity.isAnnotationPresent(BowOnly.class) && !(damageSource instanceof Arrow)) {
            return;
        }
        if (handleShootEntity.isAnnotationPresent(ThrowOnly.class) && damager instanceof Arrow) {
            return;
        }
        if (ins.getClass().isAnnotationPresent(PlayerOnly.class) || handleShootEntity.isAnnotationPresent(PlayerOnly.class)) {
            if (target instanceof Player player) {
                ins.handleShootEntity(level, damager, player, damage, finalDamage, boostDamage, cancel);
            }
        } else if (ins.getClass().isAnnotationPresent(NotPlayerOnly.class)) {
            if (!(target instanceof Player)) {
                ins.handleShootEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
            }
        } else {
            ins.handleShootEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
        }
    }

    @SneakyThrows
    private void processPlayerDamaged(TypedPerk<?> typedPerk, int level, Player player, Entity damager,
                                      double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;
        Method method = METHOD_CACHE.computeIfAbsent(typedPerk.getClass(), clazz -> {
            try {
                return clazz.getMethod("handlePlayerDamaged", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        //                final Method method = ((AbstractHandle) typedPerk).getClass().getMethod("handlePlayerDamaged", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        if (method == null) return;
        if (method.isAnnotationPresent(PlayerOnly.class)) {
            if (damager instanceof Player damagerPlayer) {
                typedPerk.handlePlayerDamaged(level, player, damagerPlayer, damage, finalDamage, boostDamage, cancel);
            }
        } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
            if (!(damager instanceof Player)) {
                typedPerk.handlePlayerDamaged(level, player, damager, damage, finalDamage, boostDamage, cancel);
            }
        } else {
            typedPerk.handlePlayerDamaged(level, player, damager, damage, finalDamage, boostDamage, cancel);
        }
    }

    @SneakyThrows
    private void processPlayerDamaged(IPlayerDamaged ins, int level, Player player, Entity damager,
                                      double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;
        Method method = METHOD_CACHE.computeIfAbsent(ins.getClass(), clazz -> {
            try {
                return clazz.getMethod("handlePlayerDamaged", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        if (method == null) return;
//            final Method method = ins.getClass().getMethod("handlePlayerDamaged", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        if (method.isAnnotationPresent(PlayerOnly.class)) {
            if (damager instanceof Player damagerPlayer) {
                ins.handlePlayerDamaged(level, player, damagerPlayer, damage, finalDamage, boostDamage, cancel);
            }
        } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
            if (!(damager instanceof Player)) {
                ins.handlePlayerDamaged(level, player, damager, damage, finalDamage, boostDamage, cancel);
            }
        } else {
            ins.handlePlayerDamaged(level, player, damager, damage, finalDamage, boostDamage, cancel);
        }
    }

    private void processAttackEntity(IAttackEntity ins, int level, Player damager, Entity target,
                                     double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;
        Method method = METHOD_CACHE.computeIfAbsent(ins.getClass(), clazz -> {
            try {
                return clazz.getMethod("handleAttackEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        //                method = ins.getClass().getMethod("handleAttackEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        if(method == null) return;
        if (method.isAnnotationPresent(PlayerOnly.class)) {
                if (target instanceof Player player) {
                    ins.handleAttackEntity(level, damager, player, damage, finalDamage, boostDamage, cancel);
                }
            } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
                if (!(target instanceof Player) && damager == null) {
                    ins.handleAttackEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
                }
            } else {
                ins.handleAttackEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
            }

    }

    private void processAttackEntity(TypedPerk<?> typedPerk, int level, Player damager, Entity target,
                                     double damage, AtomicDouble finalDamage, AtomicDouble boostDamage, AtomicBoolean cancel) {
        if (level < 0) return;
        Method method = METHOD_CACHE.computeIfAbsent(typedPerk.getClass(), clazz -> {
            try {
                return clazz.getMethod("handleAttackEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
            } catch (NoSuchMethodException ignored) {
            }
            return null;
        });
        //                final Method method = ((AbstractHandle) typedPerk).getClass().getMethod("handleAttackEntity", int.class, Player.class, Entity.class, double.class, AtomicDouble.class, AtomicDouble.class, AtomicBoolean.class);
        if (method == null) return;
        if (method.isAnnotationPresent(PlayerOnly.class)) {
            if (target instanceof Player player) {
                typedPerk.handleAttackEntity(level, damager, player, damage, finalDamage, boostDamage, cancel);
            }
        } else if (method.isAnnotationPresent(NotPlayerOnly.class)) {
            if (!(target instanceof Player) && damager == null) {
                typedPerk.handleAttackEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
            }
        } else {
            typedPerk.handleAttackEntity(level, damager, target, damage, finalDamage, boostDamage, cancel);
        }

    }

}
