package cn.charlotte.pit.activity.newyear.event;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitUltimatelyDamageEvent;
import cn.charlotte.pit.events.IEpicEvent;
import cn.charlotte.pit.events.MajorEventTasks;
import cn.charlotte.pit.events.IScoreBoardInsert;
import cn.charlotte.pit.item.type.mythic.MythicLeggingsItem;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.npc.NpcFactory;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.random.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Slf4j
public class BossEvent implements IEpicEvent, Listener, IScoreBoardInsert {

    private static final Random random = new Random();
    private static final HashMap<UUID, Integer> rankMap = new HashMap<>(16);
    private static final ItemStack SWORD = new ItemBuilder(Material.NETHERITE_SWORD).buildWithUnbreakable();
    private static final ItemStack CHESTPLATE = new ItemBuilder(Material.NETHERITE_CHESTPLATE).buildWithUnbreakable();
    private static final ItemStack HELMET = new ItemBuilder(Material.NETHERITE_HELMET).buildWithUnbreakable();
    private static final ItemStack BOOTS = new ItemBuilder(Material.NETHERITE_BOOTS).buildWithUnbreakable();
    private static final ItemStack LEGGINS = new ItemBuilder(Material.NETHERITE_LEGGINGS).buildWithUnbreakable();
    private static BukkitRunnable runnable;
    private static NPC npc;
    private static int overallDamage = 0;


    @Override
    public String getEventInternalName() {
        return "boss";
    }

    @Override
    public String getEventName() {
        return "&5世界BOSS";
    }

    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&5&l>> &d世界BOSS事件 &5&l<<");
        lore.add("");
        lore.add("&5&l事件机制");
        lore.add("&7- BOSS会自动追击附近玩家");
        lore.add("&7- 对BOSS造成伤害进入排名");
        lore.add("&7- 计分板显示个人伤害和排名");
        lore.add("&7- Boss免疫 灵魂吞噬者 附魔");
        lore.add("");
        lore.add("&5&l事件奖励机制");
        lore.add("&7> 个人排名：按造成的伤害量排名");
        lore.add("&7> 全服目标：&e10000伤害 &a全员奖励翻倍 &b声望 &f+ 1");
        lore.add("");
        lore.add("&5&l排名奖励");
        lore.add("&f第1名: &65000金币 &f+ &b4000经验 &f+ &b2声望");
        lore.add("&f第2-3名: &63000金币 &f+ &b2000经验 &f+ &b1声望");
        lore.add("&f第4-5名: &62500金币 &f+ &b1500经验");
        lore.add("&f其他玩家: &61000金币 &f+ &b500经验");
        return lore;
    }

    @Override
    public int requireOnline() {
        return CombatListener.isBedrockRoom() ? 30 : 80;
    }

    @Override
    public void onActive() {
        overallDamage = 0;
        rankMap.clear();
        Util.addEventData(getEventName());
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        spawnBoss();

        npc.setAlwaysUseNameHologram(true);

        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                if (npc == null || !npc.isSpawned()) {
                    spawnBoss();
                } else if (npc.isSpawned() && npc.getEntity() instanceof LivingEntity bossEntity) {

                    Player nearestPlayer = null;
                    double nearestDistance = Double.MAX_VALUE;

                    for (Entity entity : npc.getEntity().getNearbyEntities(40, 5, 40)) {
                        if (entity instanceof Player player && player.isOnline() && !player.isDead() && !NpcFactory.getRegistry().isNPC(player)) {
                            double distance = player.getLocation().distance(bossEntity.getLocation());
                            if (distance <= 40.0 && distance < nearestDistance && Util.getPitProfile(player).isInArena()) {
                                nearestPlayer = player;
                                nearestDistance = distance;
                            }
                        }
                    }

                    if (nearestPlayer != null) {
                        if (npc.getEntity().isInWater() || npc.getEntity().isInLava() || random.nextInt(100) <= 5)
                            npc.teleport(nearestPlayer.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                        npc.getNavigator().getLocalParameters().speedModifier(2.0f);
                        npc.getNavigator().setTarget(nearestPlayer, true);
                    }
                }
                PlayerProfile.getPlayerProfileByUuid(npc.getUniqueId()).setInArena(true);
            }
        };
        MajorEventTasks.track(this, runnable.runTaskTimer(ThePit.getInstance(), 0L, 20L));
        MajorEventTasks.later(this, () -> ThePit.getInstance().getEventFactory().inactiveEvent(this), 5 * 20L * 60L);
    }

    @Override
    public void onInactive() {
        stopRuntime();

        List<Map.Entry<UUID, Integer>> sortedEntries = new ArrayList<>(rankMap.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int rankCounter = 1;
        int previousDamage = -1;
        Map<UUID, Integer> rankMapWithSameRank = new HashMap<>();

        for (Map.Entry<UUID, Integer> entry : sortedEntries) {
            int currentDamage = entry.getValue();
            if (currentDamage != previousDamage) {
                previousDamage = currentDamage;
            }
            rankMapWithSameRank.put(entry.getKey(), rankCounter);
            rankCounter++;
        }

        List<UUID> topThreePlayers = sortedEntries.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        sortedEntries.forEach(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) return;

            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(entry.getKey());
            int rank = rankMapWithSameRank.get(entry.getKey());

            int coin = 0;
            int exp = 0;
            int renown = 0;

            if (rank == 1) {
                coin += 5000;
                exp += 4000;
                renown += 2;
            } else if (rank <= 3) {
                coin += 3000;
                exp += 2000;
                renown += 1;
            } else if (rank <= 5) {
                coin += 2500;
                exp += 1500;
            } else {
                coin += 1000;
                exp += 500;
            }

            if (overallDamage >= 10000) {
                coin *= 2;
                exp *= 2;
                renown += 1;
            }

            if (entry.getValue() >= 1000) {
                coin += 2500;
                exp += 1500;
                renown += 1;
            }
            int enchantBoostLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "paparazzi_enchant");
            if (PlayerUtil.isVenom(player) || PlayerUtil.isEquippingSomber(player)) {
                enchantBoostLevel = 0;
            }
            if (enchantBoostLevel > 0) {
                coin += (int) (0.5 * enchantBoostLevel * coin);
                renown += (int) Math.floor(0.5 * enchantBoostLevel * renown);
                MythicLeggingsItem mythicLeggings = new MythicLeggingsItem();
                mythicLeggings.loadFromItemStack(player.getInventory().getLeggings());
                if (mythicLeggings.isEnchanted()) {
                    if (mythicLeggings.getMaxLive() > 0 && mythicLeggings.getLive() <= 2) {
                        player.getInventory().setLeggings(new ItemStack(Material.AIR));
                    } else {
                        mythicLeggings.setLive(mythicLeggings.getLive() - 2);
                        player.getInventory().setLeggings(mythicLeggings.toItemStack());
                    }
                }
            }

            profile.setCoins(profile.getCoins() + coin);
            profile.setExperience(profile.getExperience() + exp);
            profile.setRenown(profile.getRenown() + renown);
            profile.getEventData().addEventrenown(renown);

            StringBuilder message = new StringBuilder();
            message.append(CC.GOLD).append(CC.CHAT_BAR).append("\n");
            message.append(CC.translate("&5&lBOSS事件结束: &d世界BOSS")).append("\n");
            message.append(CC.translate(String.format("&5你的奖励: &6+%s金币 &b+%s经验 &b+%s声望", coin, exp, renown))).append("\n");

            if (overallDamage >= 10000) {
                message.append(CC.translate("&5&l全局奖励: &a&l成功! &7所有人获得的奖励翻倍!")).append("\n");
            } else {
                message.append(CC.translate(String.format("&5&l全局奖励: &c&l失败! &7总共造成了%d伤害", overallDamage * 2))).append("\n");
            }

            message.append(CC.translate(String.format("&5&l你: &c造成%d❤ &7(排名#%s)", entry.getValue() / 2, rank))).append("\n");

            if (!topThreePlayers.isEmpty()) {
                message.append(CC.translate("&5顶级玩家: ")).append("\n");
                for (int i = 0; i < Math.min(3, topThreePlayers.size()); i++) {
                    Player topPlayer = Bukkit.getPlayer(topThreePlayers.get(i));
                    if (topPlayer != null && topPlayer.isOnline()) {
                        PlayerProfile topProfile = PlayerProfile.getPlayerProfileByUuid(topPlayer.getUniqueId());
                        int topDamage = rankMap.get(topPlayer.getUniqueId()) / 2;
                        message.append(CC.translate(String.format(" &d&l#%s %s &d造成了 &c%d❤ 伤害", i + 1, topProfile.getFormattedName(), topDamage))).append("\n");
                    }
                }
            }

            message.append(CC.GOLD).append(CC.CHAT_BAR);
            player.sendMessage(message.toString());
        });
        rankMap.clear(); overallDamage = 0;
    }

    private void stopRuntime() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        if (runnable != null) { runnable.cancel(); runnable = null; }
        if (npc != null) { npc.destroy(); npc = null; }
    }

    @Override public void onAbort() { stopRuntime(); rankMap.clear(); overallDamage = 0; }

    private void spawnBoss() {
        if (npc == null) npc = NpcFactory.getRegistry().createNPC(EntityType.PLAYER, CC.translate("&5BOSS"));
        npc.setProtected(false);
        npc.data().set(NPC.Metadata.GLOWING, true);
        if (!npc.isSpawned() && !npc.spawn(ThePit.getInstance().getPitConfig().getBossLocation()))
            throw new IllegalStateException("无法生成世界BOSS");
        if (!(npc.getEntity() instanceof Player entity)) throw new IllegalStateException("世界BOSS不是玩家实体");
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(2048);
        entity.setHealth(2048);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 254));
        npc.getNavigator().getDefaultParameters().attackRange(3.0);
        npc.getNavigator().getDefaultParameters().attackDelayTicks(5);
        entity.getInventory().setBoots(BOOTS.clone()); entity.getInventory().setChestplate(CHESTPLATE.clone());
        entity.getInventory().setLeggings(LEGGINS.clone()); entity.getInventory().setHelmet(HELMET.clone());
        entity.getInventory().setItemInMainHand(SWORD.clone());
    }

    @Override
    public ActivityType getEventType() {
        return IEpicEvent.super.getEventType();
    }

    @EventHandler
    public void onDamge(PitUltimatelyDamageEvent event) {
        if (!this.isActive() || event.getAttacker() == null || event.getUltimatelyDamage() <= 0
                || npc == null || npc.getEntity() == null || !npc.getEntity().getUniqueId().equals(event.getTarget().getUniqueId())) return;
        if (NpcFactory.getRegistry().isNPC(event.getAttacker())) return;
        if (!NpcFactory.getRegistry().isNPC(event.getTarget())) return;
        Player entity = (Player) event.getTarget();
        entity.getInventory().setBoots(BOOTS);
        entity.getInventory().setChestplate(CHESTPLATE);
        entity.getInventory().setLeggings(LEGGINS);
        entity.getInventory().setHelmet(HELMET);
        entity.getInventory().setItemInMainHand(SWORD);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, -1, 254));

        Player player = event.getAttacker();

        rankMap.merge(player.getUniqueId(), (int) event.getUltimatelyDamage(), Integer::sum);
        overallDamage += (int) event.getUltimatelyDamage() / 2;

        if (RandomUtil.hasSuccessfullyByChance(0.2)) {
            PlayerProfile profile = Util.getPitProfile(player);
            profile.setCoins(profile.getCoins() + random.nextInt(50));
            profile.setExperience(profile.getExperience() + random.nextInt(50));
        }
    }

    @Override
    public List<String> insert(Player player) {
        if (!rankMap.containsKey(player.getUniqueId())) {
            return List.of("&f造成伤害: &c0❤ &7(未参与)");
        }

        List<Map.Entry<UUID, Integer>> sortedEntries = new ArrayList<>(rankMap.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int rank = 1;
        int previousDamage = -1;
        int currentRank = 0;

        for (Map.Entry<UUID, Integer> entry : sortedEntries) {
            int currentDamage = entry.getValue();
            if (currentDamage != previousDamage) {
                previousDamage = currentDamage;
                currentRank = rank;
            }

            if (entry.getKey().equals(player.getUniqueId())) {
                return List.of(String.format("&f造成伤害: &c%d❤ &7(#%s)",
                        entry.getValue() / 2,
                        currentRank
                ));
            }
            rank++;
        }

        return List.of("&f造成伤害: &c0❤ &7(未参与)");
    }
}
