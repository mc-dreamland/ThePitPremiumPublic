package cn.charlotte.pit.perk.type.boosts;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.bot.PitBotManager;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.KillRecap;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.impl.major.BlockHeadEvents;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.manager.BlockRestoreManager;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.RunTracker;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import com.mojang.datafixers.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.bukkit.Material.*;

@AutoRegister
public class LavaBucketPerk extends TypedPerk<PerksBoostsEnum> implements Listener {
    public static final ItemStack LAVA_BUCKET =
            new ItemBuilder(Material.LAVA_BUCKET)
                    .canDrop(false)
                    .deathDrop(false)
                    .canSaveToEnderChest(false)
                    .internalName("perk_lava_bucket")
                    .lore("&7天赋物品")
                    .buildWithUnbreakable();
    public static final ItemStack EMPTY_BUCKET =
            new ItemBuilder(BUCKET)
                    .canDrop(false)
                    .deathDrop(false)
                    .canSaveToEnderChest(false)
                    .internalName("perk_lava_empty_bucket")
                    .lore("&7天赋物品")
                    .buildWithUnbreakable();
    public final RunTracker runTracker = new RunTracker();
    public final Map<UUID, Integer> lavaExistTimer = new HashMap<>();
    public final Map<UUID, Location> placedLava = new HashMap<>();
    public final Map<UUID, Pair<Material, BlockData>> previousLavaBlock = new HashMap<>();

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "岩浆桶";
    }

    @Override
    public Material getIcon() {
        return Material.LAVA_BUCKET;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public double requireCoins() {
        return 1000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 10;
    }

    @Override
    public List<String> getDescription(Player player) {
        return List.of("§7出生时获得岩浆桶.");
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public void onPerkActive(Player player) {
        player.getInventory().addItem(LAVA_BUCKET.clone());
    }

    @Override
    public void onPerkInactive(Player player) {
        InventoryUtil.removeItemWithInternalName(player, "perk_lava_bucket");
    }

    @Override
    public void handleRespawn(int enchantLevel, Player myself) {
        UUID uuid = myself.getUniqueId();

        BlockRestoreManager.INSTANCE.finishRestoreByPlayer(uuid);

        InventoryUtil.removeItemWithInternalName(myself, "perk_lava_bucket");
        InventoryUtil.removeItemWithInternalName(myself, "perk_lava_empty_bucket");
        myself.getInventory().addItem(LAVA_BUCKET.clone());
    }


    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        if (PitBotManager.inTraining(p) || PitBotManager.inTraining(block.getLocation())
                || PitBotManager.inTraining(e.getBlockClicked().getRelative(e.getBlockFace()).getLocation())) {
            e.setCancelled(true);
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(p.getUniqueId());
        e.setCancelled(true);

        if (!profile.isInArena()) {
            return;
        }


        Location ragePitMiddle = ThePit.getInstance().getPitConfig().getRagePitMiddle().clone();
        Location location = block.getLocation().clone();
        ragePitMiddle.setY(location.getY());
        if (location.distance(ragePitMiddle) < 10) {
            return;
        }
        if (block.isLiquid()) {
            p.sendMessage("§c你无法在流体中放置岩浆!");
            return;
        }

        if (e.getBucket() == Material.LAVA_BUCKET) {
            // 阻止 BlockHeadEvent 时候放置岩浆
            IEpicEvent activeEpicEvent = ThePit.getInstance().getEventFactory().getActiveEpicEvent();
            if (activeEpicEvent instanceof BlockHeadEvents) {
                e.setCancelled(true);
                return;
            }

            // 一人只允许一桶
            if (BlockRestoreManager.INSTANCE.hasPlacedLava(p.getUniqueId())) {
                p.sendMessage("§c你已经放置过一桶岩浆了！");
                e.setCancelled(true);
                return;
            }
            // 交给 BlockRestoreManager 统一处理
            BlockRestoreManager.INSTANCE.placeTemporaryLava(
                    p.getUniqueId(), block, 240L);  // 240 Tick ≈ 12 秒

            EquipmentSlot hand = e.getHand();
            p.getInventory().setItem(hand, LavaBucketPerk.EMPTY_BUCKET.clone());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.getBucket() != Material.BUCKET) return;
        if (e.getBlock().getType() != WATER) {
            e.setCancelled(true);
            return;
        }
        Block block = e.getBlock();
        if (block.getType() != Material.LAVA || !block.hasMetadata("placer")) return;

        UUID placer = UUID.fromString(block.getMetadata("placer").getFirst().asString());
        if (!placer.equals(e.getPlayer().getUniqueId())) return;   // 只能自己收

        BlockRestoreManager.INSTANCE.finishRestoreByPlayer(placer);

        // 空桶 -> 岩浆桶
        e.getPlayer().getInventory().setItem(e.getHand(), LAVA_BUCKET.clone());
        e.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (e.getBlock().getType() == LAVA || e.getBlock().getType() == WATER) e.setCancelled(true);
    }

    @EventHandler
    public void onLavaDamage(EntityDamageEvent e) {

        if (!(e.getEntity() instanceof Player damaged)) {
            return;
        }
        if (PitBotManager.inTraining(damaged) && (e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || e.getCause() == EntityDamageEvent.DamageCause.LAVA)) {
            e.setCancelled(true);
            damaged.setFireTicks(0);
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            e.setCancelled(true);
            e.getEntity().setFireTicks(0);
        }

        if (e.getCause() != EntityDamageEvent.DamageCause.LAVA) {
            return;
        }

        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(damaged.getUniqueId());
        if (!profile.isInArena()) {
            return;
        }

        Player damager;
        Block block = null;
        int x = 0;
        int z = 0;

        int i = 0;
        mainLoop:
        while (true) {
            i++;
            if (i > 10) {
                break;
            }
            for (int y = 0; y < 3; y++) {
                Block blocc = damaged.getLocation().add(x, y, z).getBlock();

                if (blocc.getType() == LAVA && blocc.hasMetadata("placer")) {
                    block = damaged.getLocation().add(x, y, z).getBlock();
                    break mainLoop;
                }
            }

            if (x == 0) {
                x--;
                continue;
            }

            if (x == -1) {
                x += 2;
                continue;
            }

            if (x == 1) {
                x = 0;
                if (z == 0) {
                    z--;
                    continue;
                }
                if (z == -1) {
                    z += 2;
                    continue;
                }

                if (z == 1) break;
            }
        }

        if (block == null) {
            return;
        }
        e.setDamage(0);
        damager = Bukkit.getPlayer(UUID.fromString(block.getMetadata("placer").getFirst().asString()));
        if (damager == null) {
            return;
        }

        PlayerProfile playerProfile = PlayerProfile.getPlayerProfileByUuid(damaged.getUniqueId());
        PlayerProfile damagerProfile = PlayerProfile.getPlayerProfileByUuid(damager.getUniqueId());


        //TODO 处理伤害、助攻、击杀回放等。
//        handleDamage(damager, 1, damaged, damager, playerProfile, damagerProfile, 1, false);

        KillRecap.DamageData damagerData = new KillRecap.DamageData();
        damagerData.setDisplayName(damager.getName());
        damagerData.setAttack(false);
        damagerData.setMelee(false);
        damagerData.setLava(true);
        damagerData.setDamage(1);
        damagerData.setAfterHealth(Math.max(damaged.getHealth() - 1, 0));
        damagerData.setUsedItem(null);
        damagerData.setTimer(new Cooldown(10, TimeUnit.SECONDS));

        playerProfile.getKillRecap()
                .getDamageLogs()
                .add(damagerData);

        boolean willDie = damaged.getHealth() - 1 <= 0;
        if (willDie) {
            // If the damaged player is the same as the lava placer, set killer to null to avoid self-kill benefits.
            CombatListener.handlePlayerDeath(damaged, damaged.equals(damager) ? null : damager, true);
        } else {
            damaged.setHealth(damaged.getHealth() - 1);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        BlockRestoreManager.INSTANCE.finishRestoreByPlayer(
                e.getPlayer().getUniqueId());
    }

}
