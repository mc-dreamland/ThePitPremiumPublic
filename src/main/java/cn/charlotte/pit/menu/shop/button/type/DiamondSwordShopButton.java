package cn.charlotte.pit.menu.shop.button.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.genesis.team.GenesisTeam;
import cn.charlotte.pit.menu.shop.button.AbstractShopButton;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/1 17:12
 */
public class DiamondSwordShopButton extends AbstractShopButton {

    public DiamondSwordShopButton(boolean b) {
        super(b);
    }

    @Override
    public String getInternalName() {
        return "diamond_sword";
    }

    @Override
    public ItemStack getDisplayButtonItem(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&9攻击被悬赏的玩家造成的伤害 +20%");
        lines.add(" ");
        lines.add("&7&o死亡后消失");
        lines.add("&7价格: &6" + getDiscountPrice(player, getPrice(player)) + " 硬币");

        if (profile.getCoins() >= getDiscountPrice(player, getPrice(player))) {
            lines.add("&e点击购买!");
        } else {
            lines.add("&c硬币不足!");
        }

        if (PerksBoostsEnum.BARBARIAN.getPerk().hasEquip(profile)) {
            return new ItemBuilder(Material.DIAMOND_AXE)
                    .lore(lines)
                    .deathDrop(true)
                    .canSaveToEnderChest(false)
                    .canDrop(false)
                    .canTrade(false)
                    .internalName("perk_barbarian")
                    .itemDamage(8)
                    .buildWithUnbreakable();
        }
        return new ItemBuilder(Material.DIAMOND_SWORD)
                .lore(lines)
                .deathDrop(true)
                .canTrade(true)
                .internalName("shopItem")
                .itemDamage(7)
                .buildWithUnbreakable();
    }

    @Override
    public boolean shouldUpdate(Player player, int slot, ClickType clickType) {
        return true;
    }

    @Override
    public ItemStack[] getResultItem(Player player) {
        if (getButtonItem(player).getType().equals(Material.DIAMOND_AXE)) {
            return new ItemStack[]{new ItemBuilder(Material.DIAMOND_AXE)
                    .deathDrop(true)
                    .internalName("perk_barbarian")
                    .itemDamage(8)
                    .buildWithUnbreakable()};
        }
        return new ItemStack[]{new ItemBuilder(Material.DIAMOND_SWORD)
                .deathDrop(true)
                .canSaveToEnderChest(true)
                .canDrop(true)
                .canTrade(true)
                .internalName("shopItem")
                .buildWithUnbreakable()};
    }

    @Override
    public int getPrice(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (ActivityType.GENES.isActive() && profile.getActivityPlayerData().getGenesisData().getTier() >= 2) {
            return (int) (0.5D * 150);
        }
        return 150;
    }

    //@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onCombat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getDamager() instanceof Player damager) {
                if (damager.getItemInHand() != null && damager.getItemInHand().getType() == Material.DIAMOND_SWORD && ItemUtil.getInternalName(damager.getItemInHand()).equalsIgnoreCase("shopItem")) {
                    PlayerProfile targetProfile = PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId());
                    if (targetProfile.getBounty() != 0) {
                        event.setDamage(event.getDamage() * 1.2);
                    }
                }
            }
        }
    }
}
