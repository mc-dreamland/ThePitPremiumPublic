package cn.charlotte.pit.perk.type.boosts.based;

import cn.charlotte.pit.perk.BotKillEffect;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.manager.ResourceManager;
import cn.charlotte.pit.medal.impl.challenge.MinerMedal;
import cn.charlotte.pit.perk.PerkGroup;
import cn.charlotte.pit.perk.PerkType;
import cn.charlotte.pit.perk.PerksBoostsEnum;
import cn.charlotte.pit.perk.TypedPerk;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.AdvancedItemIdentity;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/3 20:26
 */
@BotKillEffect(BotKillEffect.Kind.KILL_EFFECT)
public class MineManPerk extends TypedPerk<PerksBoostsEnum> {

    @Override
    public String getInternalPerkName() {
        return getType().name();
    }

    @Override
    public String getDisplayName() {
        return "矿工";
    }

    @Override
    public Material getIcon() {
        return Material.DIAMOND_PICKAXE;
    }

    @Override
    public double requireCoins() {
        return 3000;
    }

    @Override
    public double requireRenown(int level) {
        return 0;
    }

    @Override
    public int requirePrestige() {
        return 0;
    }

    @Override
    public int requireLevel() {
        return 30;
    }

    @Override
    public PerkType getPerkType() {
        return PerkType.PERK;
    }

    @Override
    public PerkGroup getPerkGroup() {
        return PerkGroup.BOOST;
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7天赋携带期间额外获得以下物品:");
        lines.add("  &f▶ &b钻石稿 &7(&9效率 IV&7)");
        lines.add("  &f▶ &f24 * 圆石");
        lines.add("&7击杀玩家额外获得以下物品:");
        lines.add("  &f▶ &f3 * 圆石");
        return lines;
    }

    @Override
    public int getMaxLevel() {
        return 0;
    }

    @Override
    public void onPerkActive(Player player) {
        player.getInventory().addItem(new ItemBuilder(Material.DIAMOND_PICKAXE).deathDrop(false).canDrop(false).canSaveToEnderChest(false).internalName("perk_miner").enchant(Enchantment.EFFICIENCY, 4).buildWithUnbreakable());
        giveBlocks(player, 24);
    }

    @Override
    public void onPerkInactive(Player player) {
        InventoryUtil.removeItemWithInternalName(player, "perk_miner");
    }

    @Override
    public void handlePlayerKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        giveBlocks(myself, 3);
        int heldBlocks = Arrays.stream(myself.getInventory().getStorageContents())
                .filter(item -> item != null && item.getType() == Material.COBBLESTONE
                        && "perk_miner".equals(ItemUtil.getInternalName(item)))
                .mapToInt(ItemStack::getAmount)
                .sum();
        if (heldBlocks >= 64) {
            new MinerMedal().setProgress(PlayerProfile.getPlayerProfileByUuid(myself.getUniqueId()), 1);
        }
    }

    @Override
    public void handleBotKilled(int enchantLevel, Player myself, Entity target, ResourceManager resourceManager) {
        giveBlocks(myself, 3); // Bot kills must not advance the miner medal.
    }

    private void giveBlocks(Player myself, int amount) {
        ItemStack cobblestone = new ItemBuilder(Material.COBBLESTONE).canDrop(false).deathDrop(true)
                .canSaveToEnderChest(false).internalName("perk_miner").amount(amount).build();
        myself.getInventory().addItem(AdvancedItemIdentity.splitForGrant(cobblestone));
    }
}
