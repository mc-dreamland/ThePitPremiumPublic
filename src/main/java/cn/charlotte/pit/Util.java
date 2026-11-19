package cn.charlotte.pit;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.mail.Mail;
import cn.charlotte.pit.data.mail.MailBuilder;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.medal.AbstractMedal;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.time.TimeUtil;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class Util {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyy/MM/dd HH:mm");
    public static final DecimalFormat df = new DecimalFormat(",###,###,###,###");
    public static final Random random = new Random();
    private static final Field nmsItemField;
    public static DecimalFormat s0_000 = new DecimalFormat("0.000");
    public static DecimalFormat s0 = new DecimalFormat("0");
    public static DecimalFormat s0_0 = new DecimalFormat("0.0");
    public static DateFormat tmin_s = new SimpleDateFormat("mm:ss");
    public static DateFormat tyyyy_MM_dd_h_min_s = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
    public static DateFormat tyyyy_MM_dd = new SimpleDateFormat("yyyy年MM月dd日");

    static {
        try {
            nmsItemField = CraftItemStack.class.getDeclaredField("handle");
            nmsItemField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access CraftItemStack.handle field", e);
        }
    }

    public static PlayerProfile getPitProfile(Player player) {
        return PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
    }

    public static void releaseItem(Player player) {
        if (player instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().releaseUsingItem();
        }
    }

    public static List<UUID> getMythicItemUUIDs(Player player) {
        List<UUID> uuids = new ArrayList<>();
        PlayerProfile profile = Util.getPitProfile(player);

        PlayerInv inventory = profile.getInventory();
        if (inventory != null && inventory.getContents() != null) {
            for (org.bukkit.inventory.ItemStack item : inventory.getContents()) {
                if (item != null && MythicUtil.getMythicItem(item) != null) {
                    UUID uuid = MythicUtil.getIMythicItemUUID(MythicUtil.getMythicItem(item));
                    if (uuid != null) {
                        uuids.add(uuid);
                    }
                }
            }
        }

        PlayerEnderChest[] enderChests = profile.getEnderChests();
        if (enderChests != null) {
            for (PlayerEnderChest chest : enderChests) {
                if (chest != null && chest.getInventory() != null) {
                    chest.getInventory().getContents();
                    for (org.bukkit.inventory.ItemStack item : chest.getInventory().getContents()) {
                        if (item != null && MythicUtil.getMythicItem(item) != null) {
                            UUID uuid = MythicUtil.getIMythicItemUUID(MythicUtil.getMythicItem(item));
                            if (uuid != null) {
                                uuids.add(uuid);
                            }
                        }
                    }
                }
            }
        }
        return uuids;
    }

    public static void dead(Player player) {
        if (player != null) {
            PlayerUtil.damage(player, PlayerUtil.DamageType.TRUE, player.getMaxHealth() * 100, false);
        }
    }

    public static boolean hasRealMan(Player player) {
        if (player == null) return false;
        List<org.bukkit.metadata.MetadataValue> values = player.getMetadata("real_man");
        if (values == null || values.isEmpty()) {
            return false;
        }
        long time = values.get(0).asLong();
        return time >= System.currentTimeMillis();
    }

    public static net.minecraft.world.item.ItemStack reflectGetNmsItem(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null) return null;

        if (bukkitItem instanceof CraftItemStack craftItem) {
            try {
                return (net.minecraft.world.item.ItemStack) nmsItemField.get(craftItem);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to get NMS ItemStack", e);
            }
        } else {
            return createNmsItemInstance(bukkitItem);
        }
    }

    private static net.minecraft.world.item.ItemStack createNmsItemInstance(org.bukkit.inventory.ItemStack bukkitItem) {
        Item item = CraftMagicNumbers.getItem(bukkitItem.getType());
        if (item == null) return null;

        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, bukkitItem.getAmount());
        if (bukkitItem.hasItemMeta()) {
            ItemMeta meta = bukkitItem.getItemMeta();
            CraftItemStack.setItemMeta(stack, meta);
        }
        return stack;
    }

    /**
     * 播放稀有附魔粒子效果
     *
     * @param player 触发附魔的玩家
     */
    public static void playRareEnchantmentEffect(Player player) {
        Location enchantLocation = ThePit.getInstance().getPitConfig().getEnchantLocation();
        if (enchantLocation == null) {
            return;
        }

        Location startLocation = enchantLocation.clone().add(0, 4, 0); // 从上方4格开始

        playFallingParticles(startLocation, enchantLocation.clone().add(0, 1, 0), player); // 落至上方1格
    }

    /**
     * 播放下落粒子效果 - 3秒完成
     */
    private static void playFallingParticles(Location startLoc, Location endLoc, Player player) {
        new BukkitRunnable() {
            final int totalTicks = 60;
            final double totalHeight = startLoc.getY() - endLoc.getY();
            final double stepHeight = totalHeight / totalTicks;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel();
                    playBurstEffect(endLoc, player);
                    return;
                }

                double currentY = startLoc.getY() - (stepHeight * ticks);
                Location particleLoc = new Location(startLoc.getWorld(), startLoc.getX(), currentY, startLoc.getZ());

                player.spawnParticle(Particle.PORTAL, particleLoc, 8, 0.2, 0, 0.2, 0.05);

                if (ticks % 3 == 0) {
                    player.spawnParticle(Particle.WITCH, particleLoc, 2, 0.1, 0, 0.1, 0.01);
                }

                if (ticks % 10 == 0) {
                    player.playSound(particleLoc, Sound.BLOCK_PORTAL_AMBIENT, 0.2f, 1.0f + (ticks * 0.01f));
                }

                ticks++;
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 1L);
    }

    /**
     * 播放立体圆形爆发粒子效果 - 5秒扩展到半径5格，停留2秒
     */
    private static void playBurstEffect(Location center, Player player) {
        new BukkitRunnable() {
            final int expandDuration = 100;
            final int holdDuration = 40;
            final int totalDuration = expandDuration + holdDuration;
            final double maxRadius = 5.0;
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= totalDuration) {
                    this.cancel();
                    return;
                }

                double currentRadius;
                if (ticks < expandDuration) {
                    double progress = (double) ticks / expandDuration;
                    currentRadius = maxRadius * progress;
                } else {
                    currentRadius = maxRadius;
                }

                generateSphericalParticles(center, currentRadius, player, ticks);

                if (ticks % 2 == 0) {
                    player.spawnParticle(Particle.ENCHANT, center, 15, 0.5, 0.5, 0.5, 0.1);
                }

                if (ticks % 15 == 0) {
                    float pitch = 0.8f + (ticks * 0.003f);
                    player.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, Math.min(pitch, 1.5f));
                }

                ticks++;
            }
        }.runTaskTimer(ThePit.getInstance(), 0L, 1L);
    }

    /**
     * 生成球形粒子
     */
    private static void generateSphericalParticles(Location center, double radius, Player player, int tick) {
        int particlesPerLayer = 20;
        int layers = 8;

        for (int layer = 0; layer < layers; layer++) {
            double phi = Math.PI * layer / (layers - 1);

            for (int i = 0; i < particlesPerLayer; i++) {
                double theta = 2 * Math.PI * i / particlesPerLayer;

                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);

                Location particleLoc = center.clone().add(x, y, z);

                if (tick % 2 == 0) {
                    player.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                } else {
                    player.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }

        if (tick % 3 == 0) {
            for (int i = 0; i < 15; i++) {
                double randomX = (Math.random() - 0.5) * radius * 2;
                double randomY = (Math.random() - 0.5) * radius * 2;
                double randomZ = (Math.random() - 0.5) * radius * 2;

                Location randomLoc = center.clone().add(randomX, randomY, randomZ);
                if (randomLoc.distance(center) <= radius) {
                    player.spawnParticle(Particle.FIREWORK, randomLoc, 1, 0, 0, 0, 0.02);
                }
            }
        }
    }

    public static void backToLobby(Player player) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(stream);
            out.writeUTF("lobby-" + (530001 + new Random().nextInt()));

            player.sendPluginMessage(
                    ThePit.getInstance(),
                    "velocity:connect",
                    stream.toByteArray()
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMail(UUID uuid, Mail mail) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(uuid);
            profile.getMailData().sendMail(mail);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByUuid(uuid);
                    profile.getMailData().sendMail(mail);
                }
            }.runTaskAsynchronously(ThePit.getInstance());
        }
    }

    public static void sendMail(UUID uuid, Mail mail,String... msg) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(uuid);
            profile.getMailData().sendMail(mail);
            CC.send(player,msg);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByUuid(uuid);
                    profile.getMailData().sendMail(mail);
                }
            }.runTaskAsynchronously(ThePit.getInstance());
        }
    }

    public static List<String> templateMedalLore(PlayerProfile profile, int progress, boolean locked, AbstractMedal medal) {
        List<String> lines = new ArrayList<>();
        for (int i = 1; i <= medal.getMaxLevel(); i++) {
            var status = profile.getMedalData().view(medal.getInternalName(), i);

            for (String s : medal.getRequirementDescription(i).split("/s")) {
                lines.add("&f" + s);
            }
            lines.add("");
            if (!locked) {

                if (medal.getProgressRequirement(i) > 1) {
                    lines.add("&7进度: &a完成! &7(&a" + Util.df.format(progress) + "&7)");
                }
                lines.add("&7完成于 " + Util.dateFormat.format(status.getFinishedTime()) + " 。");
                lines.add("");
                lines.add("&a成就已解锁!");
            } else if (!medal.isHidden()) {
                if (medal.getProgressRequirement(i) > 1) {
                    lines.add("&7进度: &a" + Util.df.format(progress) + "&7/&a" + Util.df.format(medal.getProgressRequirement(i)));
                    lines.add("");
                }
                lines.add("&c成就尚未解锁!");
            }
        }
        return lines;
    }

    public static <E> E ifNull(E value, E defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static <E> List<E> ifNullOrEmpty(List<E> value, List<E> defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    /**
     * Grants newly generated rewards and mails any items that did not fit.
     * @return whether any of the items were mailed
     */
    public static boolean giveItems(Player player, ItemStack... items) {
        List<ItemStack> rewards = new ArrayList<>();
        for (ItemStack item : items) {
            for (ItemStack stack : AdvancedItemIdentity.splitForGrant(item)) {
                rewards.add(stack.clone());
            }
        }
        if (rewards.isEmpty()) return false;

        var remaining = player.getInventory().addItem(rewards.toArray(ItemStack[]::new));
        if (remaining.isEmpty()) return false;
        sendItemMail(player, new ArrayList<>(remaining.values()), "&e【物品发放】",
                "&f背包空间不足，剩余物品已寄至邮箱。请及时收取。");
        return true;
    }

    public static void sendItemMail(Player player, List<ItemStack> items, String title, String content) {
        int maxAttachments = player.getInventory().getStorageContents().length;
        long expiry = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        for (int start = 0; start < items.size(); start += maxAttachments) {
            List<ItemStack> attachments = items.subList(start, Math.min(items.size(), start + maxAttachments));
            sendMail(player.getUniqueId(), new MailBuilder().title(title).content(content)
                    .expireTime(expiry).item(attachments).build());
        }
    }

    public static PlayerInv toPlayerInv(ItemStack itemStack) {
        PlayerInv inv = new PlayerInv();
        inv.setContents(new org.bukkit.inventory.ItemStack[]{itemStack});
        return inv;
    }

    public static void addItemOfMail(org.bukkit.inventory.ItemStack itemStack, Player player, Mail mail) {
        if (!InventoryUtil.isInvFull(player)) {
            player.getInventory().addItem(itemStack);
            return;
        }
        if (mail == null) {
            mail = new Mail();
            mail.setExpireTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
            mail.setSendTime(System.currentTimeMillis());
            PlayerInv contents = new PlayerInv();
            contents.setContents(new org.bukkit.inventory.ItemStack[]{itemStack});
            mail.setTitle("&e【物品发放】");
            mail.setContent("&f你在 " + dateFormat.format(System.currentTimeMillis()) + " 中获得了物品.\\n&f请注意及时收取物品.");
            mail.setItem(contents);
        }
        if (mail.getItem() == null) {
            PlayerInv contents = new PlayerInv();
            contents.setContents(new org.bukkit.inventory.ItemStack[]{itemStack});
            mail.setItem(contents);
        }
        Util.sendMail(player.getUniqueId(), mail);
    }

//    public static boolean IMythicItemBoostMapHas(IMythicItem item,String name){
//        return item.getBoostmap().getOrDefault(name, false);
//    }

    public static Map<String, Integer> getEnchantments(org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return new HashMap<>();
        }

        return NBT.get(itemStack, tag -> {
            ReadableNBT extra = tag.getCompound("extra");
            if (extra == null || !extra.hasTag("ench")) {
                return new HashMap<>();
            }
            ReadableNBTList<String> list = extra.getStringList("ench");
            if (list == null || list.isEmpty()) {
                return new HashMap<>();
            }

            Map<String, Integer> result = new HashMap<>();
            for (String enchantString : list) {
                if (enchantString == null) continue;

                String[] parts = enchantString.split(":");
                if (parts.length != 2) continue;

                try {
                    result.merge(parts[0], Integer.parseInt(parts[1]), Integer::sum);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse enchant level: {}", parts[1], e);
                }
            }
            return result;
        });
    }

    public static boolean logicInstanceOne(boolean A, boolean B, boolean C, boolean D) {
        return A || ((!B || D) && !C);
    }

    public static boolean logicInstanceTwo(boolean A, boolean B, boolean C, boolean D) {
        return !A && !C && (!B || D);
    }

    public static boolean isSameBlockLocation(Location loc1, Location loc2) {
        World w1 = loc1.getWorld();
        World w2 = loc2.getWorld();
        return w1 != null && w1.equals(w2)
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    public static void addItem(Player player, org.bukkit.inventory.ItemStack itemStack) {
        if (!InventoryUtil.isInvFull(player)) player.getInventory().addItem(itemStack);
    }

    public static boolean hasIMythicItem(Player player) {
        for (ItemStack itemStack : player.getInventory()) if (Utils.getMythicItem(itemStack) != null) return true;
        return false;
    }

    public static String getCooldownActionText(Cooldown cooldown) {
        return (cooldown.hasExpired() ? "&a&l✔" : "&c&l" + TimeUtil.millisToRoundedTime(cooldown.getRemaining()).replace(" ", ""));
    }

    public static String getLiveColor(int live, int maxLive) {
        if (maxLive <= 0) return "&c";
        String lifeColor;
        double ratio = live / (maxLive * 1.0d);

        if (ratio <= 0.3) {
            lifeColor = "&c";
        } else if (ratio <= 0.6) {
            lifeColor = "&e";
        } else {
            lifeColor = "&a";
        }
        return lifeColor;
    }

    public static ItemStack delItemStack(ItemStack itemStack, int amount) {
        ItemStack item = itemStack.clone();
        if (itemStack.getAmount() - amount <= 0) return PitItems.AIR.toItemStack();
        else {
            item.setAmount(itemStack.getAmount() - amount);
            return item;
        }
    }

    public static boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    // Ps: 我有点傻非要在这里写个方法
    public static void addEventData(String name) {
        Bukkit.getOnlinePlayers().forEach((player) -> {
            PlayerProfile profile = getPitProfile(player);
            profile.getEventData().callHistoricalEvents(name, 1);
        });
    }

    @SafeVarargs
    public static <E> boolean matchesAll(E e1, E... e2) {
        if (e2 == null || e2.length == 0) return false;
        return Arrays.stream(e2).allMatch(e -> Objects.equals(e, e1));
    }

    @SafeVarargs
    public static <E> boolean matchesAny(E e1, E... e2) {
        if(e2.length == 1) return e1 == e2;
        return Arrays.asList(e2).contains(e1);
    }
}
