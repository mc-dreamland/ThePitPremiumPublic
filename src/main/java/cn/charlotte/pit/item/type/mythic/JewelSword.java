package cn.charlotte.pit.item.type.mythic;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.data.sub.EnchantmentRecord;
import cn.charlotte.pit.enchantment.param.item.WeaponOnly;
import cn.charlotte.pit.enchantment.rarity.EnchantmentRarity;
import cn.charlotte.pit.event.PitKillEvent;
import cn.charlotte.pit.event.PitPlayerSpawnEvent;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import de.tr7zw.nbtapi.NBT;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.min;

@AutoRegister
public class JewelSword extends IMythicItem implements Listener {

    private static final int KILL_TARGET = 117;

    private int killed = 0;

    @Override
    public String getInternalName() {
        return "jewel_sword";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&d宝石之剑";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.GOLDEN_SWORD;
    }

    @Override
    public ItemStack toItemStack() {
        ItemStack itemStack = super.toItemStack();

        if (killed >= KILL_TARGET) {
            net.minecraft.world.item.ItemStack nmsCopy = Util.reflectGetNmsItem(itemStack); // 需要对应的反射工具
            NBT.modify(itemStack, nbt -> {
                if (nbt.hasCustomNbtData()) {
                    if (nbt.getCompound("extra") != null) {
                        nbt.getCompound("extra").setString("internal", "mythic_sword");
                    }
                }
            });
            return CraftItemStack.asCraftMirror(nmsCopy);
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&9隐藏的宝石");
        lore.add("&7当您使用本武器连续击杀&a" + KILL_TARGET + "&7名玩家后,");
        lore.add("&7本武器将会自动附魔至 T1,");
        lore.add("&7它将会必定包含一个满级附魔");
        lore.add("&7当前已完成: &c" + killed + "&7/&a" + KILL_TARGET);

        return new ItemBuilder(itemStack).lore(lore).jewelSwordKills(killed).itemDamage(6.5).build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        var tag = NBT.readNbt(item);
        if (tag != null && tag.getCompound("extra") != null) {
            this.killed = tag.getCompound("extra").getInteger("killed");
        }
        super.loadFromItemStack(item);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PitProfileLoadedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (player == null) return;
        clearKill(player);
    }

    @EventHandler
    public void onSpawn(PitPlayerSpawnEvent event) {
        clearKill(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        clearKill(event.getEntity());
    }

    private void clearKill(Player player) {
        for (int index = 0; index < player.getInventory().getSize(); index++) {
            ItemStack item = player.getInventory().getItem(index);
            if (item == null) continue;
            String name = ItemUtil.getInternalName(item);
            if (name == null) continue;
            if ("jewel_sword".equals(name)) {
                JewelSword sword = new JewelSword();
                sword.loadFromItemStack(item);
                sword.killed = 0;
                player.getInventory().setItem(index, sword.toItemStack());
            }
        }

        if (player.getItemOnCursor() != null) {
            ItemStack itemOnCursor = player.getItemOnCursor();
            if ("jewel_sword".equals(ItemUtil.getInternalName(itemOnCursor))) {
                JewelSword sword = new JewelSword();
                sword.loadFromItemStack(itemOnCursor);
                sword.killed = 0;
                player.setItemOnCursor(sword.toItemStack());
            }
        }
    }

    @EventHandler
    public void onKill(PitKillEvent event) {
        Player killer = event.getKiller();

        if (event.getTarget().getLastDamageCause() instanceof EntityDamageByEntityEvent cause) {
            if (!(cause.getDamager() instanceof Player)) {
                return;
            }
        }

        var profile = Util.getPitProfile(killer);
        ItemStack item = killer.getInventory().getItemInMainHand();

        String name = ItemUtil.getInternalName(item);
        if (!"jewel_sword".equals(name)) return;

        JewelSword sword = new JewelSword();
        sword.loadFromItemStack(item);
        sword.killed++;

        if (sword.killed >= KILL_TARGET) {
            boolean rare = RandomUtil.hasSuccessfullyByChance(0.005);

            var list = ThePit.getInstance().getEnchantmentFactor().getEnchantments().stream().filter(e -> e.getClass().getAnnotation(WeaponOnly.class) != null).filter(e -> rare ? e.getRarity() == EnchantmentRarity.RARE : e.getRarity() == EnchantmentRarity.NORMAL).toList();

            var enchant = list.get(RandomUtil.random.nextInt(list.size()));

            try {
                var enchantment = enchant.getClass().newInstance();

                MythicSwordItem newSword = new MythicSwordItem();
                newSword.setTier(1);
                newSword.setMaxLive(new Random().nextInt(5) + 5);
                newSword.setLive(newSword.getMaxLive());
                newSword.setColor(color); // color 应该是 IMythicItem 的字段
                newSword.getEnchantments().put(enchantment, min(3, enchant.getMaxEnchantLevel()));
                newSword.getEnchantmentRecords().add(new EnchantmentRecord(killer.getName(), "Jewel Sword", System.currentTimeMillis()));

                ItemStack newItemStack = newSword.toItemStack();
                killer.getInventory().setItemInMainHand(newItemStack);

                if (enchantment.getRarity() == EnchantmentRarity.RARE) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(Component.text(CC.translate("&d&l稀有附魔! &7" + profile.getFormattedName() + " &7通过 &9隐藏的宝石 &7附魔晋升出物品: " + newItemStack.getItemMeta().getDisplayName() + " &e[查看]")).hoverEvent(item.asHoverEvent()));
                    }
                }

                ThePit.getInstance().getSoundFactory().playSound("successfully", killer);
                killer.sendMessage(CC.translate("&d晋升! &7经过坚持与努力, 您的 &d宝石之剑 &7获得了附魔!"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            killer.getInventory().setItemInMainHand(sword.toItemStack());
        }
    }
}
