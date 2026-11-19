package cn.charlotte.pit.activity.newyear.event;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class RandomRedEnvelopesEvent implements INormalEvent, Listener {

    private static final Random random = new Random();
    private final List<Location> blockLocation = new ArrayList<>(3);
    private BukkitRunnable runnable;

    @Override
    public String getEventInternalName() {
        return "random_red_envelopes_event";
    }

    @Override
    public String getEventName() {
        return "红包大战";
    }

    @Override
    public List<String> getLore() {
        return List.of("");
    }

    @Override
    public int requireOnline() {
        return 5;
    }

    @Override
    public void onActive() {
        this.blockLocation.clear();
        if (this.runnable != null) this.runnable.cancel();
        List<Location> locations = ThePit.getInstance().getPitConfig().getRandomredBlockLocationList();
        Collections.shuffle(locations);
        this.blockLocation.addAll(locations.subList(0, Math.min(random.nextInt(7) + 3, locations.size())));
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
        this.blockLocation.forEach(loc -> {
            loc.getBlock().setType(Material.REDSTONE_BLOCK);
            CC.boardCast(MessageType.EVENT, String.format("&c红包在 &fX: %s ,Y: %s ,Z: %s 位置", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        });
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                ThePit.getInstance().getEventFactory().inactiveEvent(RandomRedEnvelopesEvent.this);
            }
        };
        this.runnable.runTaskLater(ThePit.getInstance(), 20 * 60 * 5);
    }

    @Override
    public void onInactive() {
        this.blockLocation.forEach(loc -> loc.getBlock().setType(Material.AIR));
        this.blockLocation.clear();
        CC.boardCast(MessageType.EVENT, "&c红包大战 &7已结束");
        HandlerList.unregisterAll(this);
    }

    @Override
    public int duration() {
        return INormalEvent.super.duration();
    }

    @Override
    public ActivityType getEventType() {
        return ActivityType.NEW_YEAR;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (!getEventInternalName().equals(ThePit.getInstance().getEventFactory().getActiveNormalEvent().getEventInternalName()) || block == null || block.getType() != Material.REDSTONE_BLOCK || !contains(block.getLocation())
            /*                !this.blockLocation.contains(block.getLocation())*/) return;
        Player myself = event.getPlayer();
        for (Entity e : myself.getNearbyEntities(10, 5, 10)) {
            if (!(e instanceof Player player)) continue;
            send(player, random.nextInt(400) + 100, random.nextInt(400) + 100);
        }

        if (RandomUtil.hasSuccessfullyByChance(Util.getPitProfile(myself).isDebug() ? 0.5 : 0.01)) {
            Util.addItemOfMail(PitItems.COMMEMORATIVE_COIN.toItemStack(), myself, null);
            CC.send(MessageType.EVENT, myself, "&a真棒! &7你获得了 &e&l纪念币");
        }
        send(myself, random.nextInt(700) + 300, random.nextInt(700) + 300);
        block.setType(Material.AIR);

        int i = 0;
        for (Location location : this.blockLocation) {
            if (Util.isSameBlockLocation(location, block.getLocation())) {
                this.blockLocation.remove(i);
                break;
            }
            i++;
        }

        if (this.blockLocation.isEmpty()) this.runnable.cancel();
    }

    private void send(Player player, int gold, int exp) {
        PlayerProfile profile = Util.getPitProfile(player);
        profile.setCoins(profile.getCoins() + gold);
        profile.setExperience(profile.getExperience() + exp);
        CC.send(MessageType.EVENT, player, "&a获得红包!", String.format("&f+ &e金币 &f* %s", gold), String.format("&f+ &b经验 &f* %s", exp));
    }

    private boolean contains(Location loc) {
        return this.blockLocation.stream().anyMatch(location -> Util.isSameBlockLocation(loc, location));
    }
}
