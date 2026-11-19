package cn.charlotte.pit.commands.admin;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.Util;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.config.NewConfiguration;
import cn.charlotte.pit.config.PitConfig;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.npc.AbstractPitNPC;
import cn.charlotte.pit.npc.NpcFactory;
import cn.charlotte.pit.setup.LocationsSetup;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.chat.ChatComponentBuilder;
import cn.charlotte.pit.util.chat.MessageType;
import cn.charlotte.pit.util.configuration.annotations.ConfigData;
import cn.charlotte.pit.util.configuration.annotations.ConfigSerializer;
import cn.charlotte.pit.util.configuration.serializer.LocationSerializer;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemBuilder;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

@CommandAlias("thepitsetup|setup|pitsetup")
@CommandPermission("pit.admin")
public class Setup extends BaseCommand {
    private final PitCommands pitCommands;

    public Setup(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    public static Location getSingleLocation(String field) {
        Object configValue = getConfigByPath(field);
        if (configValue instanceof Location) {
            return (Location) configValue;
        }
        return null;
    }

    public static Location getFromLocationList(String field, int index) {
        Object configValue = getConfigByPath(field);
        if (configValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Location> list = (List<Location>) configValue;
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        }
        return null;
    }

    public static void setConfigByPath(String path, Location value) {
        setConfigByPath(path, value, -1);
    }

    public static void setConfigByPath(String path, Location value, int index) {
        PitConfig config = ThePit.getInstance().getPitConfig();
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                ConfigData configData = field.getAnnotation(ConfigData.class);
                if (configData != null && configData.path().equals(path)) {
                    field.setAccessible(true);

                    Class<?> fieldType = field.getType();

                    if (fieldType == Location.class) {
                        field.set(config, value);
                    } else if (fieldType == List.class) {
                        ParameterizedType listType = (ParameterizedType) field.getGenericType();
                        Class<?> genericType = (Class<?>) listType.getActualTypeArguments()[0];

                        if (genericType == Location.class) {
                            @SuppressWarnings("unchecked")
                            List<Location> locationList = (List<Location>) field.get(config);
                            if (locationList != null) {
                                if (index >= 0 && index < locationList.size()) {
                                    locationList.set(index, value);
                                } else {
                                    locationList.add(value);
                                }
                            }
                        }
                    }

                    config.save();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getConfigByPath(String path) {
        PitConfig config = ThePit.getInstance().getPitConfig();
        try {
            for (Field field : config.getClass().getDeclaredFields()) {
                ConfigData configData = field.getAnnotation(ConfigData.class);
                if (configData != null && configData.path().equals(path)) {
                    field.setAccessible(true);
                    return field.get(config);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void addToListLocation(String path, Player player) {
        Object configValue = getConfigByPath(path);
        if (configValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Location> list = (List<Location>) configValue;
            list.add(player.getLocation());
            ThePit.getInstance().getPitConfig().save();
            player.sendMessage(CC.translate("&a添加成功！当前数量: " + list.size()));
        }
    }

    @Subcommand("edit")
    public void onChangeEdit(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        profile.setEditingMode(!profile.isEditingMode());
        if (profile.isEditingMode()) {
            player.sendMessage(CC.translate("&a你现在可以自由破坏方块"));
        } else {
            player.sendMessage(CC.translate("&c你关闭了自由破坏方块"));
        }
    }

    @Subcommand("punchY")
    @CommandCompletion("击飞高度|支持小数|默认4.0|4.0")
    public void punchY(Player player, String punchY) {
        double parsed = Double.parseDouble(punchY);

        YamlConfiguration config = NewConfiguration.getConfig();
        config.set("punch_y", parsed);
        NewConfiguration.save();

        player.sendMessage("ok");
    }

    @Subcommand("locs")
    @CommandCompletion("@locationFields")
    public void setLocCurrency(Player player, String field) {
        try {
            setConfigByPath(field, player.getLocation());
            player.sendMessage(CC.translate("&a位置设置成功: " + field + "|" + player.getLocation()));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(CC.translate("&c设置位置时发生错误"));
        }
    }

    @Subcommand("npc keeper")
    public void setKeeperLocation(Player player) {
        setConfigByPath("loc.npc.keeper", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc kinger")
    public void setKingerLocation(Player player) {
        setConfigByPath("loc.npc.kinger", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc cakebaker")
    public void setCakeBakerLocation(Player player) {
        setConfigByPath("loc.npc.cake_bake", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc chicken")
    public void setChickenLocation(Player player) {
        setConfigByPath("loc.npc.chicken", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc silverfish")
    public void setSilverfishLocation(Player player) {
        setConfigByPath("loc.npc.silver_fish", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc mail")
    public void setMailLocation(Player player) {
        setConfigByPath("loc.npc.mail", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc genesisdemon")
    public void setGenesisDemonLocation(Player player) {
        setConfigByPath("loc.npc.genesis_demon", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc genesisangel")
    public void setGenesisAngelLocation(Player player) {
        setConfigByPath("loc.npc.genesis_angel", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc shop")
    public void setShopNpc(Player player) {
        setConfigByPath("loc.npc.shop", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc quest")
    public void setQuestNpc(Player player) {
        setConfigByPath("loc.npc.quest", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc perk")
    public void setPerkNpc(Player player) {
        setConfigByPath("loc.npc.perk", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc leader")
    public void setLeaderNpc(Player player) {
        setConfigByPath("loc.npc.leaderboard", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc prestige")
    public void setPrestigeNpc(Player player) {
        setConfigByPath("loc.npc.prestige", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc status")
    public void setStatusNpc(Player player) {
        setConfigByPath("loc.npc.status", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc contract")
    public void setContractNpc(Player player) {
        setConfigByPath("loc.npc.contract", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc help")
    public void setHelpNpc(Player player) {
        setConfigByPath("loc.npc.help", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("npc trade")
    public void setTradeNpc(Player player) {
        setConfigByPath("loc.npc.tradeNPC", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("loc spireSpawn")
    public void spireSpawn(Player player) {
        setConfigByPath("loc.spire", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("loc egg")
    public void setEggLoc(Player player) {
        setConfigByPath("loc.egg", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo workTable")
    public void setWorkTable(Player player) {
        setConfigByPath("loc.holo.worktable", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo fireTable")
    public void setFireTable(Player player) {
        setConfigByPath("loc.holo.firetable", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo enchantForgeTable")
    public void setEnchantForgeTableLocation(Player player) {
        setConfigByPath("loc.enchantforge", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("loc pit")
    @CommandCompletion("loc1|loc2")
    public void setPitLoc(Player player, String type) {
        if (type.equalsIgnoreCase("loc1")) {
            setConfigByPath("loc.pita", player.getLocation());
        } else if (type.equalsIgnoreCase("loc2")) {
            setConfigByPath("loc.pitb", player.getLocation());
        }
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("loc enchantTable")
    public void onSetEnchantTable(Player player) {
        setConfigByPath("loc.enchant", player.getTargetBlock(Collections.singleton(Material.ENCHANTING_TABLE), 100).getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("listloc add spawn")
    public void addSpawn(Player player) {
        addToListLocation("list.spawn", player);
    }

    @Subcommand("listloc add hamburger")
    public void setHamburgerNpc(Player player, String value) {
        if (value.equalsIgnoreCase("a")) {
            addToListLocation("list.hamburger", player);
        }
    }

    @Subcommand("listloc add koth")
    public void setKothLoc(Player player) {
        addToListLocation("list.koth", player);
    }

    @Subcommand("listloc add spireFloor")
    public void spireFloorLocations(Player player) {
        addToListLocation("list.spirefloor", player);
    }

    @Subcommand("listloc add angelSpawns")
    public void addAngelSpawns(Player player) {
        addToListLocation("list.angel", player);
    }

    @Subcommand("listloc add demonSpawns")
    public void addDemonSpawns(Player player) {
        addToListLocation("list.demon", player);
    }

    @Subcommand("listloc add squads")
    public void squadsAddLocation(Player player) {
        addToListLocation("list.squads", player);
    }

    @Subcommand("listloc add blockHead")
    public void bhAddLocation(Player player) {
        addToListLocation("list.blockhead", player);
    }

    @Subcommand("listloc add leader")
    public void setLeaderLoc(Player player) {
        addToListLocation("list.leader", player);
    }

    @Subcommand("listloc add luckyWheel")
    public void setLuckyWheelLocation(Player player) {
        addToListLocation("list.luckywheel", player);
    }

    @Subcommand("listloc reset packages")
    public void resetPackagesLocations(Player player) {
        @SuppressWarnings("unchecked")
        List<Location> packages = (List<Location>) getConfigByPath("list.packages");
        if (packages != null) {
            packages.clear();
            ThePit.getInstance().getPitConfig().save();
        }
        player.sendMessage(CC.translate("&a重置成功！"));
    }

    @Subcommand("listloc reset koth")
    public void resetKothLocations(Player player) {
        @SuppressWarnings("unchecked")
        List<Location> kothLocs = (List<Location>) getConfigByPath("list.koth");
        if (kothLocs != null) {
            kothLocs.clear();
            ThePit.getInstance().getPitConfig().save();
        }
        player.sendMessage(CC.translate("&a重置成功！"));
    }

    @Subcommand("listloc reset hamburger")
    public void resetHamburgerVillagersLocations(Player player) {
        @SuppressWarnings("unchecked")
        List<Location> hamburgerLocs = (List<Location>) getConfigByPath("list.hamburger");
        if (hamburgerLocs != null) {
            hamburgerLocs.clear();
            ThePit.getInstance().getPitConfig().save();
        }
        player.sendMessage(CC.translate("&a重置成功！"));
    }

    @Subcommand("listloc clear hamburger")
    public void hamClearNpcLocations(Player player, String value) {
        if (value.equalsIgnoreCase("a")) {
            @SuppressWarnings("unchecked")
            List<Location> hamburgerLocs = (List<Location>) getConfigByPath("list.hamburger");
            if (hamburgerLocs != null) {
                hamburgerLocs.clear();
                ThePit.getInstance().getPitConfig().save();
            }
            player.sendMessage(CC.translate("&a清除成功！"));
        }
    }

    @Subcommand("listloc setup hamburger")
    public void addHamburgerVillagers(Player player) {
        addToListLocation("list.hamburger", player);
    }

    @Subcommand("listloc setup koth")
    public void addKothLocations(Player player) {
        addToListLocation("list.koth", player);
    }

    @Subcommand("listloc setup sewer")
    public void addSewerLocations(Player player) {
        addToListLocation("list.sewers", player);
    }

    @Subcommand("listloc setup packages")
    public void addPackagesLocations(Player player) {
        addToListLocation("list.packages", player);
    }

    @Subcommand("holo spawn")
    public void setSpawnHologramLocation(Player player) {
        setConfigByPath("loc.holo.spawn", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo enchant")
    public void setMythicHologram(Player player) {
        setConfigByPath("loc.holo.mythic", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo trashbin")
    public void setTrashBin(Player player) {
        setConfigByPath("loc.holo.trashbin", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo enderchest")
    public void setChestHologram(Player player) {
        setConfigByPath("loc.holo.chest", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo leader")
    public void setLeaderBoardHologram(Player player) {
        setConfigByPath("loc.holo.leaderboard", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo helper")
    public void setHelperHologram(Player player) {
        setConfigByPath("loc.holo.helper", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("holo enchantTip")
    public void setEnchantTipHologram(Player player) {
        setConfigByPath("loc.holo.enchanttip", player.getLocation());
        player.sendMessage(CC.translate("&a设置成功！"));
    }

    @Subcommand("getLocation")
    public void getLocation(Player player) {
        Location location = player.getLocation();
        player.sendMessage(new ChatComponentBuilder(location.toString())
                .setCurrentClickEvent(ClickEvent.suggestCommand(location.toString()))
                .build());
        player.sendMessage("§a位置已复制到剪贴板！");
    }

    @Subcommand("reloadnpc")
    public void reloadNpc(CommandSender sender) {
        for (AbstractPitNPC npc : NpcFactory.getPitNpc()) {
            npc.getNpc().spawn(npc.getNpcSpawnLocation());
        }
        sender.sendMessage("§aNPC 重载成功！");
    }

    @Subcommand("locs list")
    @CommandCompletion("all|single|list")
    public void listLocations(Player player, @Optional String type) {
        if (type == null || type.equalsIgnoreCase("all")) {
            player.sendMessage("§6§l=== 所有位置配置列表 ===");
            listSingleLocations(player);
            listLocationLists(player);
        } else if (type.equalsIgnoreCase("single")) {
            player.sendMessage("§6§l=== 单个位置列表 ===");
            listSingleLocations(player);
        } else if (type.equalsIgnoreCase("list")) {
            player.sendMessage("§6§l=== 列表位置列表 ===");
            listLocationLists(player);
        }
    }

    private void listSingleLocations(Player player) {
        player.sendMessage("§e§l单个位置:");
        for (Field field : PitConfig.class.getDeclaredFields()) {
            ConfigSerializer annotation = field.getAnnotation(ConfigSerializer.class);
            if (annotation == null || annotation.serializer() != LocationSerializer.class) {
                continue;
            }

            ConfigData fieldAnnotation = field.getAnnotation(ConfigData.class);
            if (fieldAnnotation == null) continue;

            String path = fieldAnnotation.path();
            if (!path.startsWith("loc.")) continue;

            Object configValue = getConfigByPath(path);
            if (configValue instanceof Location location) {
                sendLocationInfo(player, path, location);
            }
        }
    }

    private void listLocationLists(Player player) {
        player.sendMessage("§e§l列表位置:");
        for (Field field : PitConfig.class.getDeclaredFields()) {
            ConfigData fieldAnnotation = field.getAnnotation(ConfigData.class);
            if (fieldAnnotation == null) continue;

            String path = fieldAnnotation.path();
            if (!path.startsWith("list.")) continue;

            Object configValue = getConfigByPath(path);
            if (configValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<Location> locations = (List<Location>) configValue;
                sendLocationListInfo(player, path, locations);
            }
        }
    }

    private void sendLocationInfo(Player player, String field, Location location) {
        if (location != null) {
            String coords = String.format("(%.1f, %.1f, %.1f)", location.getX(), location.getY(), location.getZ());

            Component message = Component.text("§7- §f" + field + " §8» §e" + coords)
                    .hoverEvent(HoverEvent.showText(Component.text("位置: " + location)))
                    .clickEvent(ClickEvent.suggestCommand(location.toString()));

            Component actionButtons = Component.text()
                    .append(Component.text(" §8[§c删除§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击删除此位置")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup locs remove " + field)))
                    .append(Component.text(" §8[§a传送§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击传送至位置")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup locs tp " + field)))
                    .append(Component.text(" §8[§6快速设置§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击使用当前位置覆盖此位置")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup quickreplace " + field)))
                    .build();

            Component finalMessage = Component.text()
                    .append(message)
                    .append(actionButtons)
                    .build();

            player.sendMessage(finalMessage);
        } else {
            Component setButton = Component.text(" §8[§a快速设置§8]")
                    .hoverEvent(HoverEvent.showText(Component.text("点击使用当前位置设置此位置")))
                    .clickEvent(ClickEvent.runCommand("/pitsetup quickadd " + field));

            Component finalMessage = Component.text()
                    .append(Component.text("§7- §f" + field + " §8» §c未设置"))
                    .append(setButton)
                    .build();

            player.sendMessage(finalMessage);
        }
    }

    private void sendLocationListInfo(Player player, String field, List<Location> locations) {
        if (locations != null && !locations.isEmpty()) {
            Component header = Component.text("§7- §f" + field + " §8» §e" + locations.size() + " 个位置");

            Component actionButtons = Component.text()
                    .append(Component.text(" §8[§c清空§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击清空整个列表")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup locs clear " + field)))
                    .append(Component.text(" §8[§a添加§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击添加当前位置到列表")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup quickadd " + field)))
                    .build();

            Component headerMessage = Component.text()
                    .append(header)
                    .append(actionButtons)
                    .build();

            player.sendMessage(headerMessage);

            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                String coords = String.format("(%.1f, %.1f, %.1f)", loc.getX(), loc.getY(), loc.getZ());

                Component item = Component.text("  §8└ §7#" + (i + 1) + " §8» §e" + coords)
                        .hoverEvent(HoverEvent.showText(Component.text("位置: " + loc)))
                        .clickEvent(ClickEvent.suggestCommand(loc.toString()));

                Component itemActions = Component.text()
                        .append(Component.text(" §8[§c删除§8]")
                                .hoverEvent(HoverEvent.showText(Component.text("点击删除此位置")))
                                .clickEvent(ClickEvent.runCommand("/pitsetup locs remove " + field + " " + i)))
                        .append(Component.text(" §8[§a传送§8]")
                                .hoverEvent(HoverEvent.showText(Component.text("点击传送至位置")))
                                .clickEvent(ClickEvent.runCommand("/pitsetup locs tp " + field + " " + i)))
                        .append(Component.text(" §8[§6覆盖§8]")
                                .hoverEvent(HoverEvent.showText(Component.text("点击使用当前位置覆盖此位置")))
                                .clickEvent(ClickEvent.runCommand("/pitsetup quickreplace " + field + " " + i)))
                        .build();

                Component itemMessage = Component.text()
                        .append(item)
                        .append(itemActions)
                        .build();

                player.sendMessage(itemMessage);
            }
        } else {
            Component setButton = Component.text(" §8[§a快速添加§8]")
                    .hoverEvent(HoverEvent.showText(Component.text("点击添加当前位置到列表")))
                    .clickEvent(ClickEvent.runCommand("/pitsetup quickadd " + field));

            Component finalMessage = Component.text()
                    .append(Component.text("§7- §f" + field + " §8» §c空列表"))
                    .append(setButton)
                    .build();

            player.sendMessage(finalMessage);
        }
    }

    @Subcommand("locs remove")
    @CommandCompletion("@locationFields")
    public void removeLocation(Player player, String field, @Optional String indexStr) {
        try {
            boolean success;

            if (indexStr != null) {
                int index = Integer.parseInt(indexStr);
                success = removeFromLocationList(field, index);
            } else {
                success = removeSingleLocation(field);
            }

            if (success) {
                ThePit.getInstance().getPitConfig().save();
                player.sendMessage(CC.translate("&a删除成功！"));
            } else {
                player.sendMessage(CC.translate("&c删除失败，位置不存在或字段无效"));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c索引必须是数字"));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(CC.translate("&c删除失败，请查看控制台错误信息"));
        }
    }

    @Subcommand("locs tp")
    @CommandCompletion("@locationFields")
    public void teleportToLocation(Player player, String field, @Optional String indexStr) {
        try {
            Location targetLocation;

            if (indexStr != null) {
                int index = Integer.parseInt(indexStr);
                targetLocation = getFromLocationList(field, index);
            } else {
                targetLocation = getSingleLocation(field);
            }

            if (targetLocation != null) {
                player.teleport(targetLocation);
                player.sendMessage(CC.translate("&a传送成功！"));
            } else {
                player.sendMessage(CC.translate("&c传送失败，位置不存在或字段无效"));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c索引必须是数字"));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(CC.translate("&c传送失败，请查看控制台错误信息"));
        }
    }

    private boolean removeSingleLocation(String field) {
        Object current = getConfigByPath(field);
        if (current instanceof Location) {
            setConfigByPath(field, null);
            return true;
        }
        return false;
    }

    private boolean removeFromLocationList(String field, int index) {
        Object configValue = getConfigByPath(field);
        if (configValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<Location> list = (List<Location>) configValue;
            if (index >= 0 && index < list.size()) {
                list.remove(index);
                return true;
            }
        }
        return false;
    }

    @Subcommand("LocationsSetup")
    @CommandCompletion("@locationFields")
    public void LocationsSetup(Player player, @Optional String value) {
        PlayerProfile profile = Util.getPitProfile(player);
        if (profile.getLocationsSetup() == null || profile.getLocationsSetup().equals("no")) {
            if (value != null) {
                profile.setLocationsSetup(value);
                CC.send(MessageType.DEBUG, player, "&c已进入快捷坐标模式 - " + value);
                player.getInventory().addItem(new ItemBuilder(Material.ARROW).internalName("LocationsSetup").build());
            } else {
                profile.setLocationsSetup("All");
                CC.send(MessageType.DEBUG, player, "&c已进入快捷坐标模式");
                player.getInventory().addItem(new ItemBuilder(Material.ARROW).internalName("LocationsSetup").build());
            }
        } else {
            profile.setLocationsSetup("no");
            InventoryUtil.removeItemWithInternalName(player, "LocationsSetup");
            CC.send(MessageType.DEBUG, player, "&c已退出快捷坐标模式");
            LocationsSetup.removeAllHolograms();
        }
    }

    @Subcommand("LocationsSetup hologram show")
    @CommandCompletion("all|single|list|@locationFields")
    public void showHologram(Player player, @Optional String type) {
        if (type == null || type.equalsIgnoreCase("all")) {
            LocationsSetup.showAllHolograms();
            player.sendMessage(CC.translate("&a已显示所有位置的全息图"));
        } else if (type.equalsIgnoreCase("single")) {
            LocationsSetup.showSingleLocationsHolograms();
            player.sendMessage(CC.translate("&a已显示所有单个位置的全息图"));
        } else if (type.equalsIgnoreCase("list")) {
            LocationsSetup.showListLocationsHolograms();
            player.sendMessage(CC.translate("&a已显示所有列表位置的全息图"));
        } else {
            LocationsSetup.showHologramForField(type);
            player.sendMessage(CC.translate("&a已显示字段 '" + type + "' 的全息图"));
        }
    }

    @Subcommand("quickadd")
    @CommandCompletion("@locationFields")
    public void quickAddLocation(Player player, String field) {
        try {
            Location playerLoc = player.getLocation();

            Location centerLocation = new Location(
                    playerLoc.getWorld(),
                    Math.floor(playerLoc.getX()) + 0.5,
                    Math.floor(playerLoc.getY()),
                    Math.floor(playerLoc.getZ()) + 0.5,
                    playerLoc.getYaw(),
                    playerLoc.getPitch()
            );

            setConfigByPath(field, centerLocation);

            Component message = Component.text()
                    .append(Component.text("§a位置设置成功: " + field + " | "))
                    .append(Component.text("§e坐标: §7(" +
                            String.format("%.1f", centerLocation.getX()) + ", " +
                            String.format("%.1f", centerLocation.getY()) + ", " +
                            String.format("%.1f", centerLocation.getZ()) + ") | "))
                    .append(Component.text("§e仰角: §7" + String.format("%.1f", centerLocation.getPitch())))
                    .build();

            Component buttonRow = Component.text()
                    .append(Component.text(" §8[§b复制坐标§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标到剪贴板")))
                            .clickEvent(ClickEvent.suggestCommand(
                                    centerLocation.getX() + " " + centerLocation.getY() + " " + centerLocation.getZ() +
                                            " " + centerLocation.getYaw() + " " + centerLocation.getPitch()
                            )))
                    .append(Component.text(" §8[§a传送§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击传送至该位置")))
                            .clickEvent(ClickEvent.runCommand("/tp " +
                                    player.getName() + " " +
                                    centerLocation.getX() + " " +
                                    centerLocation.getY() + " " +
                                    centerLocation.getZ() + " " +
                                    centerLocation.getYaw() + " " +
                                    centerLocation.getPitch()
                            )))
                    .append(Component.text(" §8[§6查看详情§8]")
                            .hoverEvent(HoverEvent.showText(Component.text(
                                    "世界: " + centerLocation.getWorld().getName() + "\n" +
                                            "坐标: " + centerLocation + "\n" +
                                            "仰角: " + String.format("%.1f", centerLocation.getPitch()) + "\n" +
                                            "水平角: " + String.format("%.1f", centerLocation.getYaw())
                            )))
                            .clickEvent(ClickEvent.suggestCommand(centerLocation.toString())))
                    .build();

            player.sendMessage(message);
            player.sendMessage(buttonRow);

        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(CC.translate("&c设置位置时发生错误，请查看控制台"));
        }
    }

    @Subcommand("quickreplace")
    @CommandCompletion("@locationFields")
    public void quickReplaceLocation(Player player, String field, @Optional String indexStr) {
        try {
            Location playerLoc = player.getLocation();

            Location centerLocation = new Location(
                    playerLoc.getWorld(),
                    Math.floor(playerLoc.getX()) + 0.5,
                    Math.floor(playerLoc.getY()),
                    Math.floor(playerLoc.getZ()) + 0.5,
                    playerLoc.getYaw(),
                    playerLoc.getPitch()
            );

            setConfigByPath(field, centerLocation, indexStr != null ? Integer.parseInt(indexStr) : -1);

            Component message = Component.text()
                    .append(Component.text("§a位置替换成功: " + field))
                    .append(indexStr != null ? Component.text(" §7[索引: " + indexStr + "]") : Component.text(""))
                    .append(Component.text(" | 新坐标: §7(" +
                            String.format("%.1f", centerLocation.getX()) + ", " +
                            String.format("%.1f", centerLocation.getY()) + ", " +
                            String.format("%.1f", centerLocation.getZ()) + ")"))
                    .build();

            Component buttons = Component.text()
                    .append(Component.text(" §8[§b复制§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标")))
                            .clickEvent(ClickEvent.suggestCommand(
                                    centerLocation.getX() + " " + centerLocation.getY() + " " + centerLocation.getZ()
                            )))
                    .append(Component.text(" §8[§d查看列表§8]")
                            .hoverEvent(HoverEvent.showText(Component.text("点击查看所有位置")))
                            .clickEvent(ClickEvent.runCommand("/pitsetup locs list " +
                                    (indexStr != null ? "list" : "single"))))
                    .build();

            player.sendMessage(message);
            player.sendMessage(buttons);

        } catch (NumberFormatException e) {
            player.sendMessage(CC.translate("&c索引必须是数字"));
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(CC.translate("&c替换位置时发生错误"));
        }
    }
}