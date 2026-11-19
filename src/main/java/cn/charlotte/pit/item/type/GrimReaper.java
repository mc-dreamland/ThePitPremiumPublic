package cn.charlotte.pit.item.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.exception.SignGUIVersionException;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

/**
 * @author Araykal
 * @since 2025/1/18
 */
public class GrimReaper extends AbstractPitItem implements Listener {


    @Override
    public String getInternalName() {
        return "grim_reaper";
    }

    @Override
    protected String getDefaultItemDisplayName() {
        return "&4死神祝福";
    }

    @Override
    public Material getItemDisplayMaterial() {
        return Material.PAPER;
    }

    public ItemStack toItemStack() {
        return new ItemBuilder(this.getItemDisplayMaterial())
                .name(this.getItemDisplayName())
                .lore(
                        "&5暗黑构造物",
                        "",
                        "&7在死神祝福上填写玩家",
                        "&7即可致该名玩家立即死亡",
                        "",
                        "&e右键使用"
                )
                .internalName(this.getInternalName())
                .canSaveToEnderChest(true)
                .deathDrop(true)
                .shiny()
                .build();
    }

    @Override
    public void loadFromItemStack(ItemStack item) {

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }
        if ("grim_reaper".equals(ItemUtil.getInternalName(event.getItem()))) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);

            try {
                SignGUI signGUI = SignGUI.builder().setType(Material.BAMBOO_SIGN).setColor(DyeColor.CYAN)
                        .setLines("", "~~~~~~~~~~~~~", "§c请输入", "§c祝福人名")
                        .setHandler((player, result) -> {
                                    String[] lines = result.getLines();
                                    if (lines[0] == null || lines[0].isEmpty()) {
                                        return Collections.emptyList();
                                    }
                                    Player target = Bukkit.getPlayer(lines[0]);
                                    if (target == null) {
                                        player.sendMessage("&c该玩家不在线！");
                                        return Collections.emptyList();
                                    }

                                    player.sendMessage("§c死神祝福已发给 " + lines[0]);
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.5f, 1.5f);
                                    PlayerUtil.takeOneItem(event.getPlayer(), hand);
                                    player.closeInventory();

                                    target.sendMessage("§c你被" + player.getName() + "祝福了");
                                    target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.5f, 1.5f);
                                    Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
                                        PlayerUtil.deadPlayer(target);
                                    }, 30L);
                                    return Collections.emptyList();
                                }
                        ).build();
                signGUI.open(event.getPlayer());
            } catch (SignGUIVersionException e) {
                e.printStackTrace();
            }
        }
    }
}
