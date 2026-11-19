package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.data.sub.PlayerEnderChest;
import cn.charlotte.pit.data.sub.PlayerInv;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.parm.AutoRegister;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicBundleUuidUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AutoRegister
public class DuplicateItemCleanupListener implements Listener {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Type RULES_TYPE = new TypeToken<Map<String, CleanupRule>>() {
    }.getType();
    private static final int MAIN_INVENTORY_SIZE = 36;

    private volatile Map<String, CleanupRule> rules = Collections.emptyMap();

    public DuplicateItemCleanupListener() {
        reloadRules();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProfileLoaded(PitProfileLoadedEvent event) {
        if (rules.isEmpty()) {
            return;
        }

        PlayerProfile profile = event.getPlayerProfile();
        Player player = Bukkit.getPlayer(profile.getPlayerUUID());
        if (player == null || !player.isOnline()) {
            return;
        }

        RemovalSummary summary = new RemovalSummary();
        cleanupPlayerInventory(profile.getInventory(), summary);
        cleanupEnderChests(profile.getEnderChests(), summary);

        if (summary.removedCount <= 0) {
            return;
        }

        ThePit.getInstance().getSLF4JLogger().info(
                "Removed {} configured duplicate items from {} ({}) on login "
                        + "(inventory/armor: {}, ender chests: {}). Matched UUIDs: {}",
                summary.removedCount,
                player.getName(),
                player.getUniqueId(),
                summary.inventoryRemovedCount,
                summary.enderChestRemovedCount,
                summary.matchedRuleIds
        );

        Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage(CC.translate("&c系统检测到异常重复神话物品，已从你的背包和仓库中清理 &f"
                    + summary.removedCount + " &c件。"));
        });
    }

    private void cleanupPlayerInventory(PlayerInv inventory, RemovalSummary summary) {
        if (inventory == null) {
            return;
        }

        int removedBefore = summary.removedCount;
        ItemStack[] contents = inventory.getContents();
        boolean contentsChanged = cleanupItems(contents, Math.min(contents == null ? 0 : contents.length, MAIN_INVENTORY_SIZE), summary);
        if (contentsChanged) {
            inventory.setContents(contents);
        }

        ItemStack[] armorContents = inventory.getArmorContents();
        boolean armorChanged = cleanupItems(armorContents, armorContents == null ? 0 : armorContents.length, summary);
        if (armorChanged) {
            inventory.setArmorContents(armorContents);
        }
        summary.inventoryRemovedCount += summary.removedCount - removedBefore;
    }

    private void cleanupEnderChests(PlayerEnderChest[] enderChests, RemovalSummary summary) {
        if (enderChests == null) {
            return;
        }

        int removedBefore = summary.removedCount;
        for (PlayerEnderChest enderChest : enderChests) {
            if (enderChest == null || enderChest.getInventory() == null) {
                continue;
            }

            ItemStack[] contents = enderChest.getInventory().getContents();
            if (cleanupItems(contents, contents.length, summary)) {
                enderChest.getInventory().setContents(contents);
            }
        }
        summary.enderChestRemovedCount += summary.removedCount - removedBefore;
    }

    private boolean cleanupItems(ItemStack[] items, int scanLength, RemovalSummary summary) {
        if (items == null || scanLength <= 0) {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < scanLength; slot++) {
            ItemStack item = items[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }

            CleanupMatch match = findMatch(item);
            if (match == null) {
                continue;
            }

            items[slot] = null;
            changed = true;
            summary.removedCount++;
            summary.matchedRuleIds.add(match.ruleId);
        }
        return changed;
    }

    private CleanupMatch findMatch(ItemStack item) {
        CleanupMatch directMatch = findUuidMatch(ItemUtil.getUUID(item));
        if (directMatch != null) {
            return directMatch;
        }

        if (MythicOwnershipUtil.isFullEquipmentBundle(item)) {
            for (int index = 0; index < MythicBundleUuidUtil.BUNDLE_SIZE; index++) {
                CleanupMatch packedItemMatch = findUuidMatch(MythicBundleUuidUtil.getStoredUuid(item, index));
                if (packedItemMatch != null) {
                    return packedItemMatch;
                }
            }
        }
        return null;
    }

    private CleanupMatch findUuidMatch(String itemUuid) {
        if (itemUuid == null || itemUuid.isBlank()) {
            return null;
        }

        CleanupRule rule = rules.get(itemUuid);
        if (rule != null) {
            return new CleanupMatch(itemUuid);
        }

        for (Map.Entry<String, CleanupRule> entry : rules.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(itemUuid)) {
                continue;
            }
            return new CleanupMatch(entry.getKey());
        }
        return null;
    }

    private void reloadRules() {
        Path configPath = ThePit.getInstance().getDataFolder().toPath().resolve("duplicate-item-cleanup.json");
        try {
            ensureConfigFile(configPath);
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                Map<String, CleanupRule> loaded = GSON.fromJson(reader, RULES_TYPE);
                if (loaded == null) {
                    rules = Collections.emptyMap();
                    return;
                }
                Map<String, CleanupRule> normalized = new LinkedHashMap<>();
                for (Map.Entry<String, CleanupRule> entry : loaded.entrySet()) {
                    if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                        continue;
                    }
                    normalized.put(entry.getKey().trim(), entry.getValue());
                }
                rules = normalized;
            }
        } catch (Exception exception) {
            rules = Collections.emptyMap();
            ThePit.getInstance().getSLF4JLogger().error("Failed to load duplicate-item-cleanup.json", exception);
        }
    }

    private void ensureConfigFile(Path configPath) throws IOException {
        Files.createDirectories(configPath.getParent());
        if (Files.exists(configPath)) {
            return;
        }

        Map<String, CleanupRule> sample = new LinkedHashMap<>();

        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            GSON.toJson(sample, RULES_TYPE, writer);
        }
    }

    private static final class CleanupRule {
        private CleanupRule() {
        }
    }

    private static final class CleanupMatch {
        private final String ruleId;

        private CleanupMatch(String ruleId) {
            this.ruleId = ruleId;
        }
    }

    private static final class RemovalSummary {
        private int removedCount;
        private int inventoryRemovedCount;
        private int enderChestRemovedCount;
        private final List<String> matchedRuleIds = new ArrayList<>();
    }
}
