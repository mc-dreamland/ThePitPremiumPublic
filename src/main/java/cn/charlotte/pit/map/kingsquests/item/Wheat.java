package cn.charlotte.pit.map.kingsquests.item;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.util.Utils;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.random.RandomUtil;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Objects;

public class Wheat extends AbstractPitItem implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getBlock();

        if (block.getType() != Material.WHEAT) {
            return;
        }
        event.setCancelled(true);

        if (!(block.getBlockData() instanceof Ageable cropData)) {
            return;
        }

        if (cropData.getAge() < cropData.getMaximumAge()) {
            return;
        }

        cropData.setAge(0);

        if (!CombatListener.isNight()) {
            player.getInventory().addItem(toItemStack());
        } else {
            player.sendMessage(CC.translate("&c夜已深 &7已禁用小麦获得"));
        }

        int enchantLevel = Utils.getEnchantLevel(player.getInventory().getLeggings(), "Farmer");
        int enchantLevel2 = Utils.getEnchantLevel(player.getInventory().getLeggings(), "laborer");

        if (Util.getPitProfile(player).isDebug()) {
            player.sendMessage(String.valueOf(enchantLevel));
        }

        if (enchantLevel > 0) {
            if (RandomUtil.hasSuccessfullyByChance(0.1 * enchantLevel)) {
                cropData.setAge(cropData.getMaximumAge());
                player.sendMessage(CC.translate("&a农民 &7由于你的辛勤劳作,小麦直接成熟了"));
            }
        }

        if (enchantLevel2 > 0) {
            if (RandomUtil.hasSuccessfullyByChance(0.2 * enchantLevel2)) {
                Item item = player.getWorld().dropItemNaturally(player.getLocation(),
                        new ItemStack(Material.GOLD_INGOT, 1));
                item.setMetadata("gold", new FixedMetadataValue(ThePit.getInstance(),
                        RandomUtil.random.nextInt(2) + 1));
            }
        }

        block.setBlockData(cropData);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        org.bukkit.block.Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block.getType() != Material.CRAFTING_TABLE) return;
        ItemStack Item = player.getInventory().getItemInMainHand();
        if (Item.getType() != Material.WHEAT && !Objects.equals(ItemUtil.getInternalName(Item), "Wheat")) {
            player.sendMessage(CC.translate("&c请你手持 &a小麦 &c再尝试兑换!"));
            return;
        }
        if (Item.getAmount() < 64) {
            player.sendMessage(CC.translate("&c你还缺少 (" + (64 - Item.getAmount()) + "/64)&a小麦"));
            return;
        }
        Item.setAmount(0);
        player.getInventory().addItem(PitItems.PACKAGED_BALE.toItemStack());
        player.sendMessage(CC.translate("&e成功兑换干草块"));
    }

    @Override
    public String getInternalName() {
        return "wheat";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a小麦";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.WHEAT;
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(getItemDisplayMaterial())
                .internalName(getInternalName())
                .canDrop(false)
                .canTrade(true)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .name(getItemDisplayName())
                .lore("&7死亡时掉落")
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {
        // 保持空实现
    }
}
