package cn.charlotte.pit.runnable;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.enchantment.IActionDisplayEnchant;
import cn.charlotte.pit.item.IActionDisplayItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.ActionBarUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public final class ActionBarDisplayRunnable {
    private static final long REFRESH_INTERVAL_MILLIS = 1000L;
    private static Map<String, IActionDisplayEnchant> enchants;
    private static Map<String, IActionDisplayItem> items;
    private static final Map<UUID, SentActionBar> lastSentActionBars = new HashMap<>();

    private static Map<String, IActionDisplayEnchant> getEnchants() {
        if (enchants == null) enchants = ThePit.getInstance().getEnchantmentFactor().getActionDisplayEnchants();
        return enchants;
    }

    private static Map<String, IActionDisplayItem> getItems() {
        if (items == null) {
            items = new HashMap<>();
            for (PitItems pitItems : PitItems.values())
                if (pitItems.getDisplayItem() != null) items.put(pitItems.getInternalName(), pitItems.getDisplayItem());
        }
        return items;
    }

    public static void start() {
        Bukkit.getScheduler().runTaskTimer(ThePit.getInstance(), () -> {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                var damageMetadata = player.getMetadata("showing_damage_data");
                long lastShownTime = damageMetadata.isEmpty() ? 0L : damageMetadata.getFirst().asLong();
                if (currentTime - lastShownTime <= 1000L) {
                    // Combat messages own the action bar during this interval. Clear the
                    // cached static text so it is sent again immediately afterwards.
                    lastSentActionBars.remove(player.getUniqueId());
                    continue;
                }

                StringBuilder message = new StringBuilder();
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem != null && handItem.getType() != Material.AIR)
                    message.append(createActionDisplay(player, handItem)).append(createActionDisplayNoEnch(player, handItem));
                for (ItemStack armorItem : player.getInventory().getArmorContents())
                    message.append(createActionDisplay(player, armorItem)).append(createActionDisplayNoEnch(player, armorItem));

                if (message.isEmpty()) {
                    lastSentActionBars.remove(player.getUniqueId());
                    continue;
                }

                String rawMessage = message.toString();
                UUID playerUuid = player.getUniqueId();
                SentActionBar previous = lastSentActionBars.get(playerUuid);
                if (previous == null
                        || !previous.message().equals(rawMessage)
                        || currentTime - previous.sentAt() >= REFRESH_INTERVAL_MILLIS) {
                    ActionBarUtil.sendActionBar(player, CC.translate(rawMessage));
                    lastSentActionBars.put(playerUuid, new SentActionBar(rawMessage, currentTime));
                }
            }
            lastSentActionBars.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }, 100L, 5L);
    }

    private static String createActionDisplay(Player player, ItemStack itemStack) {
        if (itemStack == null) return "";
        var mythicItem = Utils.getMythicItem(itemStack);
        if (mythicItem == null) return "";
        StringBuilder message = new StringBuilder();
        for (var entry : mythicItem.getEnchantments().entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            var enchantmentDisplay = getEnchants().get(key.getNbtName());
            if (enchantmentDisplay == null) continue;
            if (!message.isEmpty()) message.append(' ');
            message.append("&b&l")
                    .append(key.getEnchantName())
                    .append(' ')
                    .append(enchantmentDisplay.getText(value, player));
        }
        return message.toString();
    }

    private static String createActionDisplayNoEnch(Player player, ItemStack itemStack) {
        IActionDisplayItem displayItem = getItems().get(ItemUtil.getInternalName(itemStack));
        if (displayItem == null) return "";
        String text = displayItem.getText(player);
        return text != null && !text.isEmpty() ? " " + text : "";
    }

    private record SentActionBar(String message, long sentAt) {
    }
}
