package cn.charlotte.pit.events.impl;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.INormalEvent;
import cn.charlotte.pit.util.chat.CC;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


public class KingOfTheLadderEvents implements INormalEvent , Listener{

    @Getter
    private boolean isActive = false;
    private BukkitRunnable eventTimer;
    private KotlTower activeTower;
    private final List<Location> locations = ThePit.getInstance().getPitConfig().getLeaderLocations();
    @Override
    public List<String> getLore() {
        List<String> lore = new ArrayList<>();
        lore.add("&6&l>> " + getEventName() + " &6&l<<");
        lore.add("");
        lore.add("&6&l事件机制");
        lore.add("&7- 地图出现绿色高塔");
        lore.add("&7- 站在塔上每秒获50金币以及25经验");
        lore.add("&7- 但是会与同样在平台上的玩家平分金币与经验");
        lore.add("");
        return lore;
    }
    @Override
    public String getEventInternalName() {
        return "leader";
    }

    @Override
    public String getEventName() {
        return "&a&l高梯之王";
    }

    @Override
    public int requireOnline() {
        return NewConfiguration.getEventOnlineRequired().getOrDefault(getEventInternalName(), 5);
    }

    @Override
    public void onActive() {
        if (isActive) return;
        isActive = true;


        if (locations == null || locations.isEmpty()) {
            CC.boardCast(CC.translate("&c&l高梯之王！ &7事件位置未配置，请联系管理员设置！"));
            ThePit.getInstance().getEventFactory().inactiveEvent(this);
            return;
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(locations.size());
        Location randomLocation = locations.get(randomIndex);

        activeTower = new KotlTower(randomLocation);
        Bukkit.getScheduler().runTask(ThePit.getInstance(), activeTower::buildStructure);

        CC.boardCast(CC.translate("&a&l高梯之王事件开始！ &7站在绿色高塔上每秒获得金币！"));

        eventTimer = new BukkitRunnable() {
            @Override
            public void run() {
                ThePit.getInstance().getEventFactory().inactiveEvent(KingOfTheLadderEvents.this);
            }
        };
        eventTimer.runTaskLater(ThePit.getInstance(), 20 * 60 * 5);
        Bukkit.getPluginManager().registerEvents(this, ThePit.getInstance());
    }

    @Override
    public void onInactive() {
        if (!isActive) return;
        isActive = false;

        if (eventTimer != null && !eventTimer.isCancelled()) {
            eventTimer.cancel();
        }

        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> activeTower.deactivate());

        CC.boardCast("&6&l天坑事件结束: " + this.getEventName() + "&6&l!");
    }

    private static class KotlTower {
        private static final int STRUCTURE_HEIGHT = 16;
        private static final int PLATFORM_SIZE = 5;
        private static final double BASE_REWARD = 50.0;
        private static final double BASE_REWARD_XP = 25.0;
        private final Location location;
        private final Map<Block, Material> blocks = new HashMap<>();
        private final Map<Block, Object> blockStates = new HashMap<>(); // 存储方块状态
        private BukkitRunnable rewardTimer;
        private final Set<UUID> playersOnPlatform = ConcurrentHashMap.newKeySet();

        public KotlTower(Location location) {
            this.location = location;
        }

        public void buildStructure() {
            World world = location.getWorld();
            int startX = location.getBlockX();
            int startY = location.getBlockY();
            int startZ = location.getBlockZ();

            for (int y = 0; y < STRUCTURE_HEIGHT; y++) {
                for (int x = 0; x < PLATFORM_SIZE; x++) {
                    for (int z = 0; z < PLATFORM_SIZE; z++) {
                        Block block = world.getBlockAt(startX + x, startY + y, startZ + z);
                        saveOriginalBlock(block);
                        block.setType(Material.GREEN_TERRACOTTA);
                    }
                }
            }

            addLadders(world, startX, startY, startZ, BlockFace.EAST);
            addLadders(world, startX, startY, startZ, BlockFace.WEST);
            addLadders(world, startX, startY, startZ, BlockFace.NORTH);
            addLadders(world, startX, startY, startZ, BlockFace.SOUTH);

            rewardTimer = new BukkitRunnable() {
                @Override
                public void run() {
                    rewardPlayersOnPlatform();
                }
            };
            rewardTimer.runTaskTimer(ThePit.getInstance(), 20, 20);
        }

        public void deactivate() {
            if (rewardTimer != null) {
                rewardTimer.cancel();
            }
            restoreStructure();
            playersOnPlatform.clear();
        }

        private void addLadders(World world, int startX, int startY, int startZ, BlockFace face) {
            for (int y = 0; y < STRUCTURE_HEIGHT; y++) {
                for (int i = 0; i < PLATFORM_SIZE; i++) {
                    int xPos, zPos;
                    if (face == BlockFace.EAST || face == BlockFace.WEST) {
                        xPos = startX + (face == BlockFace.EAST ? PLATFORM_SIZE : -1);
                        zPos = startZ + i;
                    } else {
                        xPos = startX + i;
                        zPos = startZ + (face == BlockFace.SOUTH ? PLATFORM_SIZE : -1);
                    }

                    Block block = world.getBlockAt(xPos, startY + y, zPos);
                    saveOriginalBlock(block);
                    block.setType(Material.LADDER);

                    if (block.getBlockData() instanceof Directional) {
                        Directional directional = (Directional) block.getBlockData();
                        directional.setFacing(face);
                        block.setBlockData(directional);
                    }
                }
            }
        }

        private void saveOriginalBlock(Block block) {
            blocks.put(block, block.getType());
            blockStates.put(block, block.getBlockData().clone());
        }

        private void restoreStructure() {
            for (Map.Entry<Block, Material> entry : blocks.entrySet()) {
                Block block = entry.getKey();
                block.setType(entry.getValue());
                if (blockStates.containsKey(block)) {
                    block.setBlockData((org.bukkit.block.data.BlockData) blockStates.get(block));
                }
            }
            blocks.clear();
            blockStates.clear();
        }

        private void rewardPlayersOnPlatform() {
            World world = location.getWorld();
            if (world == null) return;

            int baseY = location.getBlockY() + STRUCTURE_HEIGHT - 1;
            int minY = baseY;
            int maxY = baseY + 5;

            int minX = location.getBlockX();
            int minZ = location.getBlockZ();
            int maxX = minX + PLATFORM_SIZE;
            int maxZ = minZ + PLATFORM_SIZE;

            playersOnPlatform.clear();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(world)) continue;

                Location loc = player.getLocation();
                double playerX = loc.getX();
                double playerY = loc.getY();
                double playerZ = loc.getZ();

                boolean inArea = (playerX >= minX && playerX < maxX) &&
                        (playerZ >= minZ && playerZ < maxZ) &&
                        (playerY >= minY && playerY <= maxY);

                if (inArea) {
                    playersOnPlatform.add(player.getUniqueId());
                }
            }

            int playerCount = playersOnPlatform.size();
            if (playerCount == 0) return;

            double rewardPerPlayer = BASE_REWARD / playerCount;
            double rewardPerPlayer_xp = BASE_REWARD_XP / playerCount;

            for (UUID playerId : playersOnPlatform) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(playerId);
                    if (profile != null) {
                        Location playerLoc = player.getLocation();
                        if ((playerLoc.getX() >= minX && playerLoc.getX() < maxX) &&
                                (playerLoc.getZ() >= minZ && playerLoc.getZ() < maxZ) &&
                                (playerLoc.getY() >= minY && playerLoc.getY() <= maxY)) {

                            profile.setCoins(profile.getCoins() + rewardPerPlayer);
                            profile.setExperience(profile.getExperience() + rewardPerPlayer_xp);
                            profile.grindCoins(rewardPerPlayer);
                            player.sendMessage(CC.translate("&a&l高梯之王! &7在塔上获得 &6" +
                                    String.format("%.1f", rewardPerPlayer) + "金币" + " &b" + String.format("%.1f", rewardPerPlayer_xp) + "经验"));
                        }
                    }
                }
            }
        }
    }
    /**
     * 监听方块放置事件，禁止在梯子附近4格内放置方块
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!getEventInternalName().equals(ThePit.getInstance().getEventFactory().getActiveNormalEventName())) {
            return;
        }

        Location placedLocation = event.getBlockPlaced().getLocation();

        if (hasLadderNearby(placedLocation)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.translate("&c你不能在梯子附近4格内放置方块！"));
        }
    }

    /**
     * 检查指定位置周围是否有梯子
     *
     * @param location 中心位置
     * @return 如果找到梯子返回true
     */
    private boolean hasLadderNearby(Location location) {
        World world = location.getWorld();
        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        for (int x = centerX - 4; x <= centerX + 4; x++) {
            for (int y = centerY - 4; y <= centerY + 4; y++) {
                for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.LADDER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}