package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.item.type.mythic.MythicBowItem;
import cn.charlotte.pit.item.type.mythic.MythicSwordItem;
import cn.charlotte.pit.util.FuncsKt;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemBuilder;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.item.MythicOwnershipUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

import static org.bukkit.inventory.EquipmentSlot.HAND;

@CommandAlias("pititems|pi")
@CommandPermission("pit.admin")
public class PitItemsCommand extends BaseCommand {
    private final PitCommands pitCommands;

    public PitItemsCommand(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }


    @Subcommand("itemlist")
    public void pitItem(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
        ThePit.api.openMenu(player, "admin_item");
    }

    @Subcommand("runelist")
    public void pitRuneItem(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
        ThePit.api.openMenu(player, "rune_item");
    }

    @Subcommand("enchants")
    public void openEnchant(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
        ThePit.api.openMenu(player, "admin_enchant");
    }


//    @Subcommand("give")
//    @CommandCompletion("@players")
//    public void giveItemInHand(Player player,OnlinePlayer onlinePlayer) {
//        Player target = onlinePlayer.getPlayer();
//        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
//            player.sendMessage(CC.translate("&c请手持要给予的物品!"));
//            return;
//        }
//        target.getInventory().addItem(player.getItemInHand());
//        target.sendMessage(CC.translate("&a一位管理员给予了你一些物品..."));
//        player.sendMessage(CC.translate("&a成功给予物品至 " + RankUtil.getPlayerColoredName(target.getUniqueId())));
//    }


    @Subcommand("give")
    @CommandCompletion("@players CUSTOM_NAME_PAPER|KILL_COUNTER_GEM 1")
    public void giveItem(CommandSender commandSender, OnlinePlayer onlinePlayer, String itemType, int amount) {
        Player target = onlinePlayer.getPlayer();
        PitItems pitItems = PitItems.valueOf(itemType);

        for (int i = 0; i < amount; i++) {
            ItemStack itemStack = pitItems.toItemStack();
            MythicOwnershipUtil.bindInitial(itemStack, target);
            target.getInventory().addItem(itemStack);
            target.sendMessage(CC.translate("&a你获得了: " + LegacyComponentSerializer.legacySection().serialize(itemStack.displayName())));
        }
        HashMap<String, String> log = new HashMap<>();
        log.put("log_type", "getItem");
        log.put("player", onlinePlayer.getPlayer().getName());
        log.put("uuid", onlinePlayer.getPlayer().getUniqueId().toString());
        log.put("item", itemType);
        log.put("amount", String.valueOf(amount));
        ThePit.getInstance().getLogger().info(new Gson().toJson(log));


    }


    @Subcommand("givebook")
    @CommandCompletion("@players")
    public void giveBook(Player player, OnlinePlayer onlinePlayer) {
        Player target = onlinePlayer.getPlayer();
        if (target == null || !target.isOnline()) {
            return;
        }
        ItemStack mythicReel = new ItemBuilder(Material.PAPER).name("&d附魔卷轴").deathDrop(false).canDrop(true).canSaveToEnderChest(true).internalName("mythic_reel").uuid(UUID.randomUUID())
                .lore(
                        "",
                        "&7将&6神话物品&7和&d附魔卷轴&7放入神话之井",
                        "&7将会为该&6神话物品&7带来一个随机的三级 &d&l稀有! &7附魔",
                        "",
                        "&7在神话之井使用"
                ).shiny().dontStack().build();
        target.getInventory().addItem(mythicReel);
    }


    @Subcommand("changeLives")
    @CommandCompletion("死亡不掉落次数|1|2|3")
    public void changeLives(Player player, String amount) {
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
            player.sendMessage(CC.translate("&c请先手持要修改的物品!"));
        }
        try {
            ItemStack stack = new ItemBuilder(player.getItemInHand()).live(Integer.parseInt(amount)).build();
            player.setItemInHand(MythicUtil.getMythicItem(stack).toItemStack());
            player.sendMessage(CC.translate("设置成功"));
        } catch (Exception ignored) {
            player.sendMessage("Error");
        }
    }

    @Subcommand("changeMaxLives")
    @CommandCompletion("最大死亡不掉落次数|1|2|3")
    public void changeMaxLives(Player player, String amount) {
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
            player.sendMessage(CC.translate("&c请先手持要修改的物品!"));
        }
        try {
            ItemStack stack = new ItemBuilder(player.getItemInHand()).maxLive(Integer.parseInt(amount)).build();
            player.setItemInHand(MythicUtil.getMythicItem(stack).toItemStack());
            player.sendMessage(CC.translate("设置成功"));
        } catch (Exception ignored) {
            player.sendMessage("Error");
        }
    }

    @Subcommand("changeTier")
    @CommandCompletion("稀有度|1|2|3")
    public void changeTier(Player player, String amount) {
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) {
            player.sendMessage(CC.translate("&c请先手持要修改的物品!"));
        }
        try {
            ItemStack stack = new ItemBuilder(player.getItemInHand()).tier(Integer.parseInt(amount)).build();
            player.setItemInHand(MythicUtil.getMythicItem(stack).toItemStack());
            player.sendMessage(CC.translate("设置成功"));
        } catch (Exception ignored) {
            player.sendMessage("Error");
        }
    }

    @Subcommand("addCounter")
    @CommandCompletion("1|2|3|数量")
    public void addCounter(Player player, String amount) {
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        Material type = itemInMainHand.getType();
        if (type.isAir() || (!type.equals(Material.GOLDEN_SWORD) && !type.equals(Material.BOW))) {
            player.sendMessage(Component.text("§7物品类型不符"));
            return;
        }
        String internalName = ItemUtil.getInternalName(itemInMainHand);
        if (internalName == null || (!internalName.equals(MythicSwordItem.INTERNAL_NAME) && !internalName.equals(MythicBowItem.INTERNAL_NAME))) {
            player.sendMessage(Component.text("§7物品类型不符"));
            return;
        }

        IMythicItem mythicItem = FuncsKt.toMythicItem(itemInMainHand);
        if (mythicItem == null) {
            return;
        }
        mythicItem.setKillCounterGem(mythicItem.getKillCounterGem() + Integer.parseInt(amount));
        player.getInventory().setItem(HAND, mythicItem.toItemStack());
        player.sendMessage(Component.text("§7以为你手上装备添加了 §e" + amount + " §7条击杀记录"));

    }
}
