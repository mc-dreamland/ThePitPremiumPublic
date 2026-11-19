package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.activity.ActivityData;
import cn.charlotte.pit.activity.ActivityType;
import cn.charlotte.pit.activity.IActivity;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.events.*;
import cn.charlotte.pit.game.runnable.TradeMonitorRunnable;
import cn.charlotte.pit.item.IMythicItem;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.menu.admin.CopyInv.CopyInvMenu;
import cn.charlotte.pit.menu.admin.backpack.BackPackViewMenu;
import cn.charlotte.pit.perk.PerksPrestigeEnum;
import cn.charlotte.pit.quest.AbstractQuest;
import cn.charlotte.pit.runnable.FreeCeRunnable;
import cn.charlotte.pit.runnable.RebootRunnable;
import cn.charlotte.pit.runnable.SewersRunnable;
import cn.charlotte.pit.util.MythicUtil;
import cn.charlotte.pit.util.Pair;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemUtil;
import cn.charlotte.pit.util.level.LevelUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.google.common.collect.Multimap;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.NBTType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.IntStream;
@Slf4j
@CommandAlias("pitdebug")
@CommandPermission("pit.admin")
public class Debug extends BaseCommand {
    @Getter
    public static int enableVipBuff = -1;
    private final PitCommands pitCommands;

    public Debug(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Subcommand("refreshEvent")
    public static void refreshEvent(Player player) {
        EventsHandler.refreshEvents();
        player.sendMessage("okay");
    }

    @Subcommand("getNbt")
    public static void getNbt(CommandSender sender) {
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR) {
            player.sendMessage(CC.translate("&c手中没有物品!"));
            return;
        }

        NBTItem nbtItem = new NBTItem(item);

        List<String> nbtLines = new ArrayList<>();
        nbtLines.add(CC.translate("&6=== 物品NBT数据 ==="));
        nbtLines.add(CC.translate("&7物品: &f" + item.getType()));

        if (nbtItem.hasNBTData()) {
            extractNBTData(nbtItem, "", nbtLines);
        } else {
            nbtLines.add(CC.translate("&e该物品没有NBT数据"));
        }

        nbtLines.add(CC.translate("&6=== 属性修饰符 ==="));
        extractAttributeModifiers(item, nbtLines);

        for (String line : nbtLines) {
            player.sendMessage(line);
        }
    }

    private static void extractAttributeModifiers(ItemStack item, List<String> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            lines.add(CC.translate("&7无属性修饰符"));
            return;
        }

        Multimap<Attribute, AttributeModifier> modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            lines.add(CC.translate("&7无属性修饰符"));
            return;
        }

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();

            String attributeName = getAttributeDisplayName(attribute);
            String slotName = getSlotDisplayName(modifier.getSlot());
            String operationName = getOperationDisplayName(modifier.getOperation());
            double amount = modifier.getAmount();
            String uuid = modifier.getUniqueId().toString().substring(0, 8) + "...";

            String line = String.format("&7%s &8| &e%.1f &8| &a%s &8| &b%s &8| &7%s",
                    attributeName, amount, operationName, slotName, uuid);
            lines.add(CC.translate(line));
        }
    }

    private static String getAttributeDisplayName(Attribute attribute) {
        return switch (attribute.getKey().getKey().toLowerCase()) {
            case "generic.armor" -> "护甲值";
            case "generic.armor_toughness" -> "盔甲韧性";
            case "generic.attack_damage" -> "攻击伤害";
            case "generic.attack_speed" -> "攻击速度";
            case "generic.knockback_resistance" -> "击退抗性";
            case "generic.max_health" -> "最大生命值";
            case "generic.movement_speed" -> "移动速度";
            default -> attribute.getKey().getKey();
        };
    }

    private static String getSlotDisplayName(EquipmentSlot slot) {
        if (slot == null) return "任意槽位";

        return switch (slot) {
            case HEAD -> "头部";
            case CHEST -> "胸部";
            case LEGS -> "腿部";
            case FEET -> "脚部";
            case HAND -> "主手";
            case OFF_HAND -> "副手";
            default -> slot.name();
        };
    }

    private static String getOperationDisplayName(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_NUMBER -> "加法";
            case ADD_SCALAR -> "倍乘";
            case MULTIPLY_SCALAR_1 -> "倍率";
            default -> operation.name();
        };
    }

    private static void extractNBTData(NBTCompound compound, String indent, List<String> lines) {
        for (String key : compound.getKeys()) {
            try {
                if (compound.hasTag(key)) {
                    NBTType type = compound.getType(key);
                    String valueStr = getNBTValueAsString(compound, key, type);
                    lines.add(indent + key + ": " + valueStr);

                    if (type == NBTType.NBTTagCompound) {
                        NBTCompound nested = compound.getCompound(key);
                        if (nested != null) {
                            extractNBTData(nested, indent + "  ", lines);
                        }
                    } else if (type == NBTType.NBTTagList) {
                        NBTList<?> list = compound.getCompoundList(key);
                        if (list != null) {
                            lines.add(indent + key + ": [列表, 大小: " + list.size() + "]");
                            for (Object element : list) {
                                if (element != null) {
                                    extractNBTData((NBTCompound) element, indent + "    ", lines);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                lines.add(indent + key + ": <读取错误>");
            }
        }
    }

    private static String getNBTValueAsString(NBTCompound compound, String key, NBTType type) {
        try {
            return switch (type) {
                case NBTTagString -> "\"" + compound.getString(key) + "\"";
                case NBTTagInt -> String.valueOf(compound.getInteger(key));
                case NBTTagDouble -> String.valueOf(compound.getDouble(key));
                case NBTTagFloat -> String.valueOf(compound.getFloat(key));
                case NBTTagLong -> String.valueOf(compound.getLong(key));
                case NBTTagShort -> String.valueOf(compound.getShort(key));
                case NBTTagByte -> String.valueOf(compound.getByte(key));
                case NBTTagByteArray -> {
                    byte[] byteArray = compound.getByteArray(key);
                    yield "[Byte数组, 长度: " + byteArray.length + "]";
                }
                case NBTTagIntArray -> {
                    int[] intArray = compound.getIntArray(key);
                    yield "[Int数组, 长度: " + intArray.length + "]";
                }
                case NBTTagCompound -> "{复合标签}";
                case NBTTagList -> "[列表]";
                default -> {
                    Object obj = compound.getObject(key, Object.class);
                    yield obj != null ? obj.toString() : "null";
                }
            };
        } catch (Exception e) {
            return "<类型: " + type + ">";
        }
    }

    @Subcommand("debug")
    @CommandCompletion("infNpc|toggle|enchNpc|toPublic|toPrivate|getInternalName|debug")
    public void setDebugNpcs(Player player, String value) {
        final PitConfig pitConfig = ThePit.getInstance().getPitConfig();
        if (value.equals("infNpc")) {
            pitConfig.setInfinityNpcLocation(player.getLocation());
            pitConfig.save();

            player.sendMessage(CC.translate("&aOK!"));
        }

        if (value.equalsIgnoreCase("toggle")) {
            ThePit.toggleDebugServer();
            player.sendMessage("Debug: " + ThePit.isDEBUG_SERVER());
        }

        if (value.equals("enchNpc")) {
            pitConfig.setEnchantNpcLocation(player.getLocation());
            pitConfig.save();

            player.sendMessage(CC.translate("&aOK!"));
        }

        if (value.equals("toPublic")) {
            pitConfig.setDebugServerPublic(true);
            pitConfig.save();

            player.sendMessage(CC.translate("&a现在开启了"));
        }

        if (value.equals("toPrivate")) {
            pitConfig.setDebugServerPublic(false);
            pitConfig.save();

            player.sendMessage(CC.translate("&c现在关闭了"));
        }
        if (value.equals("getInternalName")) {
            player.sendMessage(ItemUtil.getInternalName(player.getInventory().getItemInMainHand()));
        }
        if (value.equals("debug")) {
            PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId()).setDebug(!PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId()).isDebug());
            player.sendMessage((PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId()).isDebug()) ? "开启debug模式" : "关闭debug模式");
        }
    }

    @Subcommand("reboot")
    @CommandCompletion("服务器配置切换|自定义内容 10|<waitTime>|等待时间秒")
    public void reboot(CommandSender sender, String value, int waitTime) {
        final PitConfig pitConfig = ThePit.getInstance().getPitConfig();

        if (waitTime <= 30) {
            waitTime = 30;
        }
        sender.sendMessage(Component.text("重启任务: " + value + ", 将在 " + waitTime + " 秒后执行!"));
        pitConfig.setDebugServer(!pitConfig.isDebugServer());
        ThePit.getInstance().getRebootRunnable()
                .addRebootTask(new RebootRunnable.RebootTask(value, System.currentTimeMillis() + waitTime * 1000L));
    }

    @Subcommand("isOP")//一点点辅助功能罢了(^w^)
    @CommandCompletion("fly|God|Create")
    public void isOP(CommandSender sender, String type) {
        Player player = (Player) sender;
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        switch (type) {
            case "fly" -> {
                player.setAllowFlight(!player.getAllowFlight());
                player.setFlying(player.getAllowFlight());
                player.sendMessage((player.getAllowFlight()) ? "开启飞行模式" : "关闭飞行模式");
            }
            case "God" -> {
                CombatListener.hidegod(player);
                profile.setGod(!profile.isGod());
                if (profile.isGod()) {
                    for (Player online : ThePit.getInstance().getServer().getOnlinePlayers()) {
                        if (online != player && !PlayerProfile.getPlayerProfileByUuid(online.getUniqueId()).isGod()) {
                            online.hidePlayer(ThePit.getInstance(), player);
                        }
                    }
                } else {
                    for (Player online : ThePit.getInstance().getServer().getOnlinePlayers()) {
                        if (online != player) {
                            online.showPlayer(ThePit.getInstance(), player);
                        }
                    }
                }
                player.sendMessage((PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId()).isGod()) ? "开启god模式" : "关闭god模式");
            }
            case "Create" ->
                    player.setGameMode(player.getGameMode() != org.bukkit.GameMode.CREATIVE ? org.bukkit.GameMode.CREATIVE : GameMode.SURVIVAL);
        }
    }

    @Subcommand("tp")//一点点辅助功能罢了(^w^)
    public void ontp(CommandSender sender, OnlinePlayer onlinePlayer) {
        Player player = (Player) sender;
        player.teleport(onlinePlayer.player.getLocation());
        player.sendMessage("已进行TP! " + ((PlayerProfile.getOrLoadPlayerProfileByUuid(player.getUniqueId()).isGod()) ? "目前隐身状态" : "目前现身状态"));
    }

    @Subcommand("SetBetter")
    public void SetProfile(CommandSender sender, OnlinePlayer onlinePlayer) {
        if (!(onlinePlayer instanceof Player)) {
            PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
            profile.setBetterPlayer(!profile.isBetterPlayer());
            sender.sendMessage((profile.isBetterPlayer()) ? "&a现在为True" : "&c现在为False");
        }
    }

    @Subcommand("SetFreeCe")
    @CommandCompletion("Check|Enble|Quiet")
    public void SetFreeCe(CommandSender Sender, String type) {
        switch (type) {
            case "Check" -> Sender.sendMessage(CC.translate((FreeCeRunnable.isB() ? "&aYes" : "&cNo")));
            case "Enble" -> FreeCeRunnable.setB(!FreeCeRunnable.isB());
            case "Quiet" -> FreeCeRunnable.FreeCeQuiet();
        }
    }

    @Subcommand("Rename")
    public void Rename(CommandSender Sender, String name) {
        Player player = (Player) Sender;
//瞎写的,但好像没啥用?
        if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            player.getInventory().addItem(PitItems.CUSTOM_NAME_PAPER.toItemStack());
        }
        String InternalName = ItemUtil.getInternalName(player.getInventory().getItemInMainHand());
        if (InternalName.equals("CustomNamePaper")) {
            ItemStack itemStack = player.getInventory().getItemInMainHand().clone();
            itemStack.getItemMeta().setDisplayName(name);
            PlayerUtil.takeOneItem(player, player.getHandRaised());
            player.getInventory().addItem(itemStack);
        }
    }

    @Subcommand("CheckVer")
    public void CheckVer(CommandSender Sender, OnlinePlayer onlinePlayer) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
        Sender.sendMessage(String.valueOf(profile.getUpdate()));
    }

    @Subcommand("SetVer")
    public void SetVer(CommandSender Sender, OnlinePlayer onlinePlayer, double Ver) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
        profile.setUpdate(Ver);
        Sender.sendMessage(String.valueOf(profile.getUpdate()));
    }

    @Subcommand("debugPerk")
    public void debugPerk(CommandSender Sender, OnlinePlayer onlinePlayer) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
        profile.setLevel(PerksPrestigeEnum.CONTRACTOR.getPerk().getInternalPerkName(), 0);
        onlinePlayer.player.sendMessage("Yes");
    }

    @Subcommand("SetJoinMessage")//用于售卖加入文本
    @CommandCompletion("欢迎 %player% 加入了游戏")
    public void SetJoinMessage(CommandSender Sender, OnlinePlayer onlinePlayer, String Name) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
        profile.setJoinMessage(Name);
        Sender.sendMessage("Yes");
    }

    @Subcommand("SewerChestQuiet")
    public void SewerChestQuiet(CommandSender Sender) {
        SewersRunnable.checkAndRefreshChests(true);
        Sender.sendMessage("Yes");
    }

    @Subcommand("SetNight")
    public void SetNight(CommandSender Sender) {
        CombatListener.setOnNight(!CombatListener.isOnNight());
        Sender.sendMessage((CombatListener.isOnNight()) ? "Yes" : "No");
    }

    @Subcommand("FastQuest")
    public void FastQuest(CommandSender Sender, OnlinePlayer onlinePlayer) {
        AbstractQuest.resetQuest(onlinePlayer.getPlayer(), PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getPlayer().getUniqueId()).getCurrentQuest().getLevel());
        Sender.sendMessage("Yes");
    }

    @Subcommand("ThisActiveEpicEvent")
    public void ThisActiveEpicEvent(CommandSender Sender) {
        if (ThePit.getInstance().getEventFactory().getActiveEpicEvent() == null) {
            Sender.sendMessage("Null");
            return;
        }
        Sender.sendMessage("Yes");
    }

    @Subcommand("CopyInvPlayer")
    public void copyInvPlayer(CommandSender sender, OnlinePlayer onlinePlayer) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(onlinePlayer.getPlayer().getUniqueId());
        if (profile == null) {
            sender.sendMessage("Null");
        } else {
            new CopyInvMenu((Player) sender, profile);
        }
    }

    @Subcommand("ViewBackPack")
    public void viewBackPack(CommandSender sender, String playerName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行。");
            return;
        }
        player.sendMessage("§7正在读取玩家档案...");
        Bukkit.getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
            try {
                PlayerProfile open = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
                PlayerProfile target = PlayerProfile.getOrLoadPlayerProfileByName(playerName);
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        new BackPackViewMenu(player, open, target);
                    }
                });
            } catch (Exception exception) {
                ThePit.getInstance().getSLF4JLogger().warn("Failed to load backpack profile {}", playerName, exception);
                Bukkit.getScheduler().runTask(ThePit.getInstance(), () -> {
                    if (player.isOnline()) {
                        player.sendMessage("§c未找到玩家或读取档案失败，请查看服务器日志。");
                    }
                });
            }
        });

    }

    @Subcommand("getIMythicItemUUID")
    public void getIMythicItemUUID(CommandSender Sender) {
        Player player = (Player) Sender;
        IMythicItem mythicItem = MythicUtil.getMythicItem(player.getInventory().getItemInMainHand());
        if (mythicItem == null || mythicItem.getUuid() == null) {
            player.sendMessage("当前物品没有物品 UUID。");
        } else {
            player.sendMessage(MythicUtil.getIMythicItemUUID(mythicItem).toString());
        }
    }

    @Subcommand("CopyInvUUID")
    public void CopyInvUUID(CommandSender Sender, String UUID) {
        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByUuid(java.util.UUID.fromString(UUID));
        if (profile == null) {
            Sender.sendMessage("Null");
        } else {
            new CopyInvMenu((Player) Sender, profile);
        }
    }

    @Subcommand("containsMythicItemUUID")
    public void LookMythicItemUUID(CommandSender Sender, OnlinePlayer onlinePlayer) {
        PlayerProfile profile = PlayerProfile.getOrLoadPlayerProfileByUuid(onlinePlayer.player.getUniqueId());
        if (profile == null) {
            Sender.sendMessage("Null");
        } else {
            List<UUID> UUIDList = Util.getMythicItemUUIDs(onlinePlayer.player);
            Set<UUID> uuidSet = new HashSet<>();
            for (UUID UUID : UUIDList) {
                if (!uuidSet.add(UUID)) {
                    onlinePlayer.player.sendMessage(CC.translate("&c" + UUID.toString()));
                } else {
                    onlinePlayer.player.sendMessage(CC.translate("&a" + UUID.toString()));
                }
            }
        }
    }

    @Subcommand("refreshUUID")
    public void refreshUUID(CommandSender Sender) {
        Player player = Bukkit.getPlayer(Sender.getName());
        IMythicItem mythicItem = MythicUtil.getMythicItem(player.getInventory().getItemInMainHand());
        if (mythicItem != null) {
            if (!mythicItem.isUuidEnabled()) {
                player.sendMessage("此物品已关闭唯一 UUID。");
                return;
            }
            mythicItem.uuid = UUID.randomUUID();
            PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
            player.getInventory().addItem(mythicItem.toItemStack());
        }
    }

    @Subcommand("Debugmodify")
    public void Debugmodify(CommandSender sender) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOp()) return;
            PlayerProfile profile = Util.getPitProfile(player);
            profile.setRenown(500000);
            profile.setPrestige(20);
            profile.setCoins(20000000);
            profile.setExperience(LevelUtil.getLevelTotalExperience(profile.getPrestige(), 120));
            profile.applyExperienceToPlayer(player);
            profile.getGenesisData().setPoints(500000);
            sender.sendMessage("okay");
        }
    }

    @Subcommand("activateVipBonus")
    public void activateVipBonus(Player player, int vaule) {
        enableVipBuff = vaule;
        player.sendMessage(String.valueOf(enableVipBuff));
    }

    @Subcommand("DebugKill")
    @Syntax("<player> <amount>")
    @CommandCompletion("@players @range:1-5")
    public void DebugKill(CommandSender sender, OnlinePlayer player, int kills) {
        IntStream.range(0, kills).forEach(i ->
                CombatListener.handlePlayerDeath(player.getPlayer(), (Player) sender, false)
        );
    }

    @Subcommand("DebugBed")
    public void DebugBed(Player player) {
        CombatListener.setBedrockRoom(!CombatListener.isBedrockRoom());
        CC.send(MessageType.DEBUG, player, CombatListener.isBedrockRoom() ? "Yes" : "No");
    }

    @Subcommand("DebugTrade")
    public void DebugTrade(Player player) {
        CC.send(MessageType.DEBUG, player, TradeMonitorRunnable.getTradeRequests().toString());
    }

    @Subcommand("getEnchLevel")
    public void getEnchLevel(Player player, String name) {
        CC.send(MessageType.DEBUG, player, String.valueOf(Util.getEnchantments(player.getInventory().getItemInMainHand()).get(name)));
    }

    @Subcommand(
            "debugLogicInstanceOne"
    )
    public void debugLogicInstanceOne(Player player, int a, int b, int c, int d) {
        CC.send(MessageType.DEBUG, player, Util.logicInstanceOne(a == 1, b == 1, c == 1, d == 1) ? "yes" : "no");
    }

    @Subcommand(
            "debugLogicInstanceTwo"
    )
    public void debugLogicInstanceTwo(Player player, int a, int b, int c, int d) {
        CC.send(MessageType.DEBUG, player, Util.logicInstanceTwo(a == 1, b == 1, c == 1, d == 1) ? "yes" : "no");
    }

    @Subcommand(
            "debugDamage"
    )
    public void toDamage(Player player, OnlinePlayer onlinePlayer, double d) {
        onlinePlayer.getPlayer().damage(d, player);
    }

    @Subcommand(
            "getPlayerProfile"
    )
    public void get(Player player, OnlinePlayer onlinePlayer) {
        player.sendMessage(Util.ifNull(Util.getPitProfile(onlinePlayer.getPlayer()).toString(), "null"));
    }

    @Subcommand(
            "debugItem"
    )
    public void giveItem(Player player, String name, Integer i) {
        for (PitItems items : PitItems.values()) {
            if (!items.isInternalNameEquals(name)) continue;
            if (items.getSingletonItem() == null) {
                CC.send(player, "&cNull: " + name);
                return;
            }
            player.getInventory().addItem(i == 0 ? items.toSingletonItemStack() : items.toItemStack());
            return;
        }
        CC.send(player, "&cNull: " + name);
    }

    @Subcommand(
            "pushEvent"
    )
    public void pushEvent(String msg) {
        log.info("{}",EventsHandler.eventQueue.toString());
        if(EventsHandler.eventQueue.isEmpty()) return;
        EventsHandler.eventQueue.place(0,new Pair<>(new Pair<>(msg,false),new Cooldown(
                 5 * 60 * 1000L
        )));
    }

    @Subcommand(
            "activityend"
    )
    public void activityEnd(Player sender,double ver){
        for(ActivityType type : ActivityType.values()){
            if(ver != type.getDecimal()) return;
            try {
                ActivityData data = new ActivityData();
                data.getUuidList().add(sender.getUniqueId());
                ((IActivity) type.getClazz().getDeclaredConstructor().newInstance()).end(data);
            }catch (Exception e){
                log.warn("",e);
            }
        }

    }
}
