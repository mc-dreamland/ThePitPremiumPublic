package cn.charlotte.pit.util.rank;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LuckPermsUtil {

    private static final Cache<UUID, String> allPrefixMap = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();
    private static final Cache<UUID, String> vipPrefixMap = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();
    static LuckPerms luckPerms = LuckPermsProvider.get();
    static UserManager userManager = luckPerms.getUserManager();
    static GroupManager groupManager = luckPerms.getGroupManager();

    public LuckPermsUtil() {
    }

    public static void invalidatePrefixCache(UUID uuid) {
        allPrefixMap.invalidate(uuid);
        vipPrefixMap.invalidate(uuid);
    }

    // 不需要依赖LP的prefix，后续接入VIP
    public static String getAllPrefix(UUID uuid) {
        String allPrefix = allPrefixMap.getIfPresent(uuid);
        if (allPrefix != null) {
            return allPrefix;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return "&7";
        }
        allPrefix = PlaceholderAPI.setPlaceholders(player, "§r%avip_chat%§r%ctitle_prefix2%§7");
        allPrefixMap.put(uuid, allPrefix);
        return allPrefix;
    }

    // 不需要依赖LP的prefix，后续接入VIP
    public static String getVipPrefix(UUID uuid) {
        String vipPrefix = vipPrefixMap.getIfPresent(uuid);
        if (vipPrefix != null) {
            return vipPrefix;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return "&7";
        }
        vipPrefix = PlaceholderAPI.setPlaceholders(player, "§r%avip_chat%§7");
        vipPrefixMap.put(uuid, vipPrefix);
        return vipPrefix;
    }

    public static String getSuffix(UUID uuid) {
        return "§7";
    }

    public static User getUser(UUID uuid) {
        if (userManager == null) {
            return null;
        }

        if (userManager.isLoaded(uuid)) {
            return userManager.getUser(uuid);
        } else {
            CompletableFuture<User> userCompletableFuture = userManager.loadUser(uuid);
            return userCompletableFuture.join();
        }
    }

}
