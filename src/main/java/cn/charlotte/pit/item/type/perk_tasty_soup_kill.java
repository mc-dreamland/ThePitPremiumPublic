package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.listener.PlayerListener;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class perk_tasty_soup_kill extends AbstractPitItem implements IPlayerInteract {

    @Override
    protected String getDefaultItemDisplayName() {
        return "&a可口浓汤";
    }

    @Override
    public void handlePlayerInteract(Player player, ItemStack itemStack, EquipmentSlot hand, PlayerInteractEvent event) {
        PlayerListener listener = PlayerListener.INSTANCE;
        event.setCancelled(true);
        if (System.currentTimeMillis() - listener.getGoldenAppleCooldown().getOrDefault(player.getUniqueId(), 0L) <= 1000L) {
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1F, 1F);
        listener.getGoldenAppleCooldown().put(player.getUniqueId(), System.currentTimeMillis());
        PlayerUtil.takeOneItem(player, hand);
        PlayerUtil.heal(player, 2);
        PlayerUtil.addEffect(player, new PotionEffect(PotionEffectType.SPEED, 20 * 7, 0));
//                player.setAbsorptionAmount(Math.min(8F, player.getAbsorptionAmount() + 2F));
        PlayerUtil.setAbsorption(player, Math.min(8F, player.getAbsorptionAmount() + 2F));
    }

    @Override
    public ItemStack toItemStack() {
        return new ItemBuilder(Material.BEETROOT_SOUP)
                .name(getItemDisplayName())
                .deathDrop(true)
                .removeOnJoin(true)
                .canSaveToEnderChest(false)
                .canDrop(false)
                .canTrade(false)
                .internalName("perk_tasty_soup_kill")
                .isHealingItem(true)
                .build();
    }

    @Override
    public String getInternalName() {
        return "perk_tasty_soup_kill";
    }
}
