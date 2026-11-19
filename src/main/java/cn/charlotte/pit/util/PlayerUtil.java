package cn.charlotte.pit.util;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitRegainHealthEvent;
import cn.charlotte.pit.mode.Mode;
import cn.charlotte.pit.perk.*;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerUtil {
    private static final PotionEffect nightVisionEffect = new PotionEffect(
            PotionEffectType.NIGHT_VISION,
            Integer.MAX_VALUE,
            0,
            false,
            false,
            false
    );
    /* ─────────────────────────  静态初始化  ───────────────────────── */

    /* ─────────────────────────  MegaStreak & Perk  ───────────────────────── */

    public static String getActiveMegaStreak(Player player) {
        final PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (!profile.isLoaded()) return null;

        if (profile.getChosePerk()[4] == null) return null;

        Optional<TypedPerk<?>> first = ThePit.getInstance()
                .getPerkFactory()
                .getPerks()
                .stream()
                .filter(abstractPerk -> abstractPerk.getPerkType() == PerkType.MEGA_STREAK &&
                        profile.perkEqualsChosePerk(abstractPerk.getInternalPerkName(), 4)
                )
                .findFirst();

        if (first.isPresent()) {
            AbstractPerk perk = first.get();
            if (perk instanceof MegaStreak && profile.getStreakKills() >= ((MegaStreak) perk).getStreakNeed()) {
                return CC.translate(perk.getDisplayName());
            }
        }
        return null;
    }

    /* ─────────────────────────  视觉效果 / 音效  ───────────────────────── */

    /**
     * 仅客户端闪电效果（无伤害）。
     */
    public static void playThunderEffect(Location thunderLocation) {
        World world = thunderLocation.getWorld();
        if (world == null) return;
        world.strikeLightningEffect(thunderLocation);          // 纯视觉
        world.playSound(thunderLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
    }

    public static void lightningEffect(Player receiver, Player target) {
        Location loc = target.getLocation();
        receiver.spawnParticle(Particle.FLASH, loc, 1);
        receiver.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
    }

    public static void lightningEffect(Player player) {
        Location loc = player.getLocation();
        player.spawnParticle(Particle.FLASH, loc, 1);
        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
    }

    public static void sendFirework(FireworkEffect effect, Location location) {
        Firework fw = location.getWorld().spawn(location, Firework.class, f -> {
            FireworkMeta meta = f.getFireworkMeta();
            meta.clearEffects();
            meta.addEffect(effect);
            meta.setPower(1);
            f.setFireworkMeta(meta);
        });
        // 立即爆炸
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), fw::detonate, 1L);
    }

    /* ─────────────────────────  元数据 & 标记  ───────────────────────── */

    public static boolean isVenom(Player player) {
        return player.hasMetadata("combo_venom") && player.getMetadata("combo_venom").get(0).asLong() > System.currentTimeMillis();
    }

    public static boolean isSinkingMoonlight(Player player) {
        return player.hasMetadata("sinking_moonlight") && player.getMetadata("sinking_moonlight").get(0).asLong() > System.currentTimeMillis();
    }

    public static boolean isEquippingSomber(Player player) {
        return player.getInventory().getLeggings() != null
                && player.getInventory().getLeggings().getType().equals(Material.LEATHER_LEGGINGS)
                && ThePit.getApi().getItemEnchantLevel(player.getInventory().getLeggings(), "somber_enchant") > 0
                && Util.getPitProfile(player).getPrestige() > 0;
    }

    public static boolean isEquippingArmageddon(Player player) {
        if (player.getInventory().getBoots() == null || player.getInventory().getBoots().getType().equals(Material.AIR) || player.getInventory().getBoots().getType().equals(Material.CHAINMAIL_BOOTS)) {
            return false;
        }
        return "armageddon_boots".equals(ItemUtil.getInternalName(player.getInventory().getBoots()));
    }

    public static boolean isEquippingAngelChestplate(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !chestplate.getType().equals(Material.DIAMOND_HELMET)) {
            return false;
        }

        return "angel_chestplate".equals(ItemUtil.getInternalName(player.getInventory().getChestplate()));
    }

    /* ─────────────────────────  战斗相关  ───────────────────────── */

    public static boolean isCritical(Player player) {
        // Paper/Spigot 在 1.20 提供了 API 方法 isOnGround()
        return player.getFallDistance() > 0.0F &&
                !player.isOnGround() &&
                !player.isInsideVehicle() &&
                !player.hasPotionEffect(PotionEffectType.BLINDNESS) &&
                !player.isGliding() &&
                !player.isSwimming();
    }

    public static boolean shouldIgnoreEnchant(Player self) {
        if (isSinkingMoonlight(self)) return true;
        return isEquippingSomber(self) || isVenom(self);
    }

    public static boolean shouldIgnoreEnchant(Player self, Player target) {
        if (isSinkingMoonlight(self)) return true;
        return isEquippingSomber(self) || isVenom(self) || isSinkingMoonlight(self) ||
                (isEquippingSomber(target) && !isEquippingArmageddon(self)) || isVenom(target);
    }

    /**
     * 对玩家造成伤害（可选择 true damage）。
     * 如果 canImmune = true 且玩家穿戴 Mirror 裤子，可免疫或反弹 true 伤害。
     *
     * @return true 表示伤害被免疫
     */
    public static boolean damage(Player victim, DamageType type, double damage, boolean canImmune) {
        // Custom true damage/DOT has no Bukkit damage event to enforce the training boundary.
        if (PitBotManager.inTraining(victim)) return true;
        return applyDamage(null, victim, type, damage, canImmune);
    }

    private static boolean applyDamage(Player attacker, Player victim, DamageType type, double damage, boolean canImmune) {
        boolean immune = false;
        if (victim.getInventory().getLeggings() != null && canImmune) {
            immune = ThePit.getApi().getItemEnchantLevel(victim.getInventory().getLeggings(), "Mirror") >= 1;
        }
        if (immune && type == DamageType.TRUE) {
            return true;
        }

        switch (type) {
            case NORMAL -> {
                if (attacker == null) victim.damage(damage);
                else victim.damage(damage, attacker);
            }
            case TRUE -> {
                double newHealth = Math.max(victim.getHealth() - damage, 0.0);
                victim.setHealth(newHealth);
            }
        }
        return false;
    }

    public static void damage(Player attacker, Player victim, DamageType type, double damage, boolean canImmune) {
        if (PitBotManager.blockedPvp(attacker, victim)) return;
        // NORMAL produces a Bukkit event and is scaled there; TRUE must be scaled here only.
        double appliedDamage = type == DamageType.TRUE ? PitBotManager.scalePvpDamage(attacker, victim, damage) : damage;
        boolean immune = applyDamage(attacker, victim, type, appliedDamage, canImmune);
        if (immune && type == DamageType.TRUE) {
            int mirrorLvl = ThePit.getApi().getItemEnchantLevel(victim.getInventory().getLeggings(), "Mirror");
            if (mirrorLvl >= 2) {
                // Use the unscaled source value so reflection is not reduced a second time.
                double reflected = PitBotManager.scalePvpDamage(victim, attacker, damage * (0.25 * mirrorLvl - 0.25));
                applyDamage(victim, attacker, type, reflected, false);
            }
        }
    }

    public static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    /* ─────────────────────────  Staff / Spectator  ───────────────────────── */

    public static boolean isStaff(Player player) {
        return player.hasPermission(getStaffPermission()) || player.hasPermission("pit.admin");
    }

    public static String getStaffPermission() {
        return "pit.staff.default";
    }

    public static int getPing(Player player) {
        int ping = player.getPing();
        if (ping >= 100) return ping - 30;
        if (ping >= 50) return ping - 20;
        if (ping >= 20) return ping - 10;
        return ping;
    }

//    public static boolean isStaffSpectating(Player player) {
//        return player.hasMetadata("STAFF_SPECTATOR");
//    }

//    public static Player getStaffSpectating(Player player) {
//        return isStaffSpectating(player) ? (Player) player.getMetadata("STAFF_SPECTATOR").get(0).value() : null;
//    }

//    public static void setStaffSpectateTarget(Player player, Player target) {
//        player.setMetadata("STAFF_SPECTATOR", new FixedMetadataValue(ThePit.getInstance(), target));
//    }

//    public static void clearStaffSpectateTarget(Player player) {
//        player.removeMetadata("STAFF_SPECTATOR", ThePit.getInstance());
//    }

    /* ─────────────────────────  Ping  ───────────────────────── */

    public static void clearPlayer(Player player) {
        clearPlayer(player, true, true);
    }

    /* ─────────────────────────  清理 / 重置玩家  ───────────────────────── */

    public static void clearPlayer(Player player, boolean closeInventory) {
        clearPlayer(player, closeInventory, true);
    }

    public static void clearPlayer(Player player, boolean closeInventory, boolean clearInventory) {
        if (player.isDead()) {
            player.spigot().respawn();
        }
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setLevel(0);
        player.setExp(0f);
        player.setSaturation(12.8f);
        player.setMaximumNoDamageTicks(20);
        player.setAllowFlight(false);
        player.setCanPickupItems(true);
        player.setWalkSpeed(0.2f);
        player.setFoodLevel(20);
        player.setSprinting(false);
        player.setArrowsInBody(0);
        PlayerUtil.resetAbsorption(player);

        if (clearInventory) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setItemOnCursor(null);
            player.getEnderChest().clear();
        }
        if (closeInventory) {
            player.closeInventory();
        }

        player.clearActivePotionEffects();
        player.setGameMode(GameMode.SURVIVAL);

        // 应用玩家额外生命值
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (ThePit.mode == Mode.Myhic) {
            player.setFoodLevel(profile.getFoodLevel());
        }
        double maxHealth = profile.getMaxHealth();

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);

        player.updateInventory();

        if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(nightVisionEffect);
        }
    }

    public static void resetAbsorption(Player player) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_ABSORPTION);
        if (healthAttribute == null) {
            player.registerAttribute(Attribute.GENERIC_MAX_ABSORPTION);
            healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_ABSORPTION);
        }
        healthAttribute.setBaseValue(0);
    }

    /**
     * 购买物品时，自动替换玩家背包物品
     * 仅可替换铁类物品，其余的则添加到玩家背包
     *
     * @param player
     * @param itemStacks
     */
    public static void addOrReplaceShopItem(Player player, ItemStack[] itemStacks) {
        for (ItemStack itemStack : itemStacks) {
            EquipmentSlot slot = InventoryUtil.getArmorSlot(itemStack.getType());
            if (slot == null) {
                if (itemStack.getType().toString().contains("DIAMOND")) {
                    //REPLACE IRON_SWORD
                    if (itemStack.getType().toString().contains("SWORD")) {
                        player.getInventory().remove(Material.IRON_SWORD);
                        player.getInventory().remove(Material.IRON_AXE);
                    }
                    if (itemStack.getType().toString().contains("SHOVEL")) {
                        player.getInventory().remove(Material.IRON_SWORD);
                        player.getInventory().remove(Material.IRON_AXE);
                    }
                    if (itemStack.getType().toString().contains("AXE")) {
                        player.getInventory().remove(Material.IRON_SWORD);
                        player.getInventory().remove(Material.IRON_AXE);
                    }
                }
                player.getInventory().addItem(itemStack);
            } else {
                ItemStack item = player.getInventory().getItem(slot);
                if ((item.getType().toString().contains("IRON") || item.getType().toString().contains("CHAINMAIL")) && Utils.getMythicItem(item) == null) {
                    player.getInventory().setItem(slot, null);
                    player.getInventory().setItem(slot, itemStack);
                } else {
                    player.getInventory().addItem(itemStack);
                }
            }
        }
    }

    public static void setAbsorption(Player player, double amount) {
        AttributeInstance absorption = player.getAttribute(Attribute.GENERIC_MAX_ABSORPTION);
        if (absorption == null) {
            player.registerAttribute(Attribute.GENERIC_MAX_ABSORPTION);
            absorption = player.getAttribute(Attribute.GENERIC_MAX_ABSORPTION);
        }
        if (absorption == null) {
            return;
        }
        if (player.getAbsorptionAmount() >= amount) {
            return;
        }
        absorption.setBaseValue(amount);
        double currentAbsorptionAmount = absorption.getValue();
        player.setAbsorptionAmount(Math.min(amount, Math.max(50, currentAbsorptionAmount * 3)));
    }

    public static void addAbsorption(Player player, double amount) {
        double heart = player.getAbsorptionAmount();
        if (heart >= 50 || heart + amount >= 50) {
            return;
        }
        setAbsorption(player, heart + amount);
    }

    public static void heal(Player player, double heal) {
        PitRegainHealthEvent event = new PitRegainHealthEvent(player, heal);
        event.callEvent();
        if (event.isCancelled()) return;
        double amount = event.getAmount();
        player.setHealth(Math.min(player.getHealth() + amount, player.getMaxHealth()));
    }


    /* ─────────────────────────  治疗 & 食物  ───────────────────────── */

    public static void food(Player player, int level) {
        player.setFoodLevel(Math.min(player.getFoodLevel() + level, 20));
    }

    public static void takeOneItem(Player player, @NotNull EquipmentSlot hand) {
        ItemStack item = player.getInventory().getItem(hand);
        if (item.getType() == Material.AIR) return;
        if (item.getAmount() <= 1) {
            player.getInventory().setItem(hand, null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(hand, item);
        }
    }

    /* ─────────────────────────  其他工具方法  ───────────────────────── */

    public static void denyMovement(Player player) {
        player.setFlying(false);
        player.setWalkSpeed(0f);
        player.setFoodLevel(0);
        player.setSprinting(false);
        player.setGravity(false);
    }

    public static void allowMovement(Player player) {
        player.setFlying(false);
        player.setWalkSpeed(0.2f);
        player.setFoodLevel(20);
        player.setSprinting(true);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
    }

    /**
     * 纯视觉死亡效果（替代旧 NMS 假人，使用临时盔甲架）。
     */
    public static void deathEffect(Player player, Player attacker) {
        Location loc = player.getLocation();
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            as.getEquipment().setHelmet(head);
        });
        attacker.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), stand::remove, 20L);
    }

    public static void sendParticle(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location, count);
    }

    /* ─────────────────────────  粒子  ───────────────────────── */

    public static float getDistance(Player p1, Player p2) {
        return getDistance(p1.getLocation(), p2.getLocation());
    }

    /* ─────────────────────────  距离  ───────────────────────── */

    public static float getDistance(Location l1, Location l2) {
        if (!Objects.equals(l1.getWorld(), l2.getWorld())) return Float.MAX_VALUE;
        return (float) l1.distance(l2);
    }

    public static boolean hasEquipPerk(Player player, TypedPerk<?> perksEnum) {
        if (player == null || Util.hasRealMan(player)) return false;
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        return perksEnum.hasEquip(profile);
    }

    /* ─────────────────────────  其它保持不变的方法（与数据库 / API 交互）  ───────────────────────── */

    public static int getPlayerHealItemLimit(Player player) {
        int limit = 2;
        boolean vampire = hasEquipPerk(player, PerksBoostsEnum.VAMPIRE.getPerk());
        boolean rambo = hasEquipPerk(player, PerksBoostsEnum.RAMBO.getPerk());
        boolean overHeal = hasEquipPerk(player, PerksBoostsEnum.OVER_HEAL.getPerk());
        boolean olympus = hasEquipPerk(player, PerksBoostsEnum.OLYMPUS.getPerk());
        int overHealEnchant = ThePit.getApi().getItemEnchantLevel(player.getInventory().getLeggings(), "over_heal_enchant");

        if (olympus) limit = 1;
        if (overHeal) limit *= 2;
        if (overHealEnchant > 0 && !isEquippingSomber(player) && !isVenom(player)) limit += overHealEnchant;
        if (rambo || vampire) limit = -999;     // 无限
        return limit;
    }

    public static int getPlayerHealItemAmount(Player player) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (ItemUtil.isHealingItem(stack)) amount += stack.getAmount();
        }
        return amount;
    }

    public static int getAmountOfActiveHealingPerk(Player player) {
        if (hasEquipPerk(player, PerksBoostsEnum.VAMPIRE.getPerk()) || hasEquipPerk(player, PerksBoostsEnum.RAMBO.getPerk()))
            return Integer.MAX_VALUE;
        int amount = 0;
        if (hasEquipPerk(player, PerksBoostsEnum.GOLDEN_HEADS.getPerk())) amount++;
        if (hasEquipPerk(player, PerksBoostsEnum.OLYMPUS.getPerk())) amount++;
        if (hasEquipPerk(player, PerksBoostsEnum.SOUP.getPerk())) amount++;
        return amount;
    }

    public static <E extends Enum<E> & PitPerkType> boolean isPlayerUnlockedPerk(Player p, E perk) {
        return getPlayerUnlockedPerkLevel(p, perk) > 0;
    }

    /**
     * 取得解锁等级（未解锁返回 0）
     */
    public static <E extends Enum<E> & PitPerkType> int getPlayerUnlockedPerkLevel(Player p, E perk) {
        Integer lvl = PlayerProfile.getPlayerProfileByUuid(p.getUniqueId())
                .getUnlockedPerkMap()
                .get(perk.name());
        return lvl == null ? 0 : lvl;
    }

    public static boolean isPlayerBoughtPerk(Player player, String internal) {
        return PlayerProfile.getPlayerProfileByUuid(player.getUniqueId()).getUnlockedPerkMap().containsKey(internal);
    }

    public static void deadPlayer(Player player) {
        damage(player, DamageType.TRUE, player.getMaxHealth() * 100, false);
    }

    public static int getPlayerArmorDefense(Player player) {
        int defense = 0;
        for (ItemStack i : player.getInventory().getArmorContents()) {
            if (i == null) continue;
            switch (i.getType()) {
                case LEATHER_HELMET, LEATHER_BOOTS, GOLDEN_BOOTS, CHAINMAIL_BOOTS -> defense += 1;
                case LEATHER_LEGGINGS, GOLDEN_HELMET, CHAINMAIL_HELMET, IRON_HELMET -> defense += 2;
                case LEATHER_CHESTPLATE, GOLDEN_LEGGINGS, DIAMOND_HELMET, DIAMOND_BOOTS -> defense += 3;
                case CHAINMAIL_LEGGINGS, NETHERITE_HELMET, NETHERITE_BOOTS, TURTLE_HELMET -> defense += 4;
                case GOLDEN_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_LEGGINGS -> defense += 5;
                case IRON_CHESTPLATE, DIAMOND_LEGGINGS -> defense += 6;
                case DIAMOND_CHESTPLATE -> defense += 8;

                case NETHERITE_LEGGINGS -> defense += 7;
                case NETHERITE_CHESTPLATE -> defense += 9;

                default -> {
                }
            }
        }
        return Math.min(20, defense);
    }

    public static void sendMessage(String message, Player... players) {
        for (Player p : players) p.sendMessage(message);
    }

    public static void sendMessage(String message, Set<Player> players) {
        players.forEach(p -> p.sendMessage(message));
    }

    public static List<Player> getNearbyPlayers(Location location, double radius) {
        List<Player> players = new ArrayList<>(location.getWorld().getNearbyPlayers(location, radius));
        // Do not let nearby PvP auras/DOT cross into or out of the training area.
        players.removeIf(player -> PitBotManager.inTraining(location) != PitBotManager.inTraining(player));
        return players;
    }

    public static List<Player> getNearbyPlayers(Location location, double radiusX, double radiusY, double radiusZ) {
        List<Player> players = new ArrayList<>(location.getWorld().getNearbyPlayers(location, radiusX, radiusY, radiusZ));
        players.removeIf(player -> PitBotManager.inTraining(location) != PitBotManager.inTraining(player));
        return players;
    }

    //用于增加新的效果,以后请用该方法添加药水效果
    public static void addEffect(Player player, PotionEffect newEffect) {
        for (PotionEffect existingEffect : player.getActivePotionEffects()) {
            if (existingEffect.getType().equals(newEffect.getType())) {
                if (newEffect.getAmplifier() > existingEffect.getAmplifier() || (existingEffect.getDuration() < newEffect.getDuration() && newEffect.getAmplifier() == existingEffect.getAmplifier())) {
                    player.removePotionEffect(existingEffect.getType());
                    player.addPotionEffect(newEffect);
                }
                return;
            }
        }
        player.addPotionEffect(newEffect);
    }

    public static void addEffectStreak(Player player, PotionEffect newEffect, double Streak) {
        if (!(Util.getPitProfile(player).getStreakKills() >= Streak)) {
            return;
        }
        for (PotionEffect existingEffect : player.getActivePotionEffects()) {
            if (existingEffect.getType().equals(newEffect.getType())) {
                if (newEffect.getAmplifier() == existingEffect.getAmplifier() && existingEffect.getDuration() < 600) {
                    player.removePotionEffect(existingEffect.getType());
                    player.addPotionEffect(newEffect);
                    return;
                } else {
                    player.addPotionEffect(newEffect);
                }
            }
        }
        player.addPotionEffect(newEffect);
    }

    public enum DamageType {NORMAL, TRUE}

    public static boolean isSupperStreak(PlayerProfile profile,Player player) {
        if (profile.getStreakKills() > 50) {
            if (PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.OVERDRIVE.getPerk()) || PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.HIGH_LANDER.getPerk()) || PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.BEAST_MODE.getPerk()) || PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.HERMIT.getPerk())) {
                return true;
            }
        }
        if (profile.getStreakKills() > 100) {
            return (PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.UBER.getPerk()) && profile.getStreakKills() >= 400) || PlayerUtil.hasEquipPerk(player, PerksBundlesEnum.TO_THE_MOON.getPerk());
        }
        return false;
    }

}
