package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.status.StatusMenu;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Misoryan
 * @Created_In: 2021/1/2 21:31
 */
public class StatusNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "status";
    }

    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e%pitnpcholo_" + getNpcInternalName() + "_0%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_1%",
                "&e%pitnpcholo_" + getNpcInternalName() + "_2%"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        List<String> lines = new ArrayList<>();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        lines.add("&9&l统计信息");
        if (profile.getLevel() >= 50 || profile.getPrestige() > 0) {
            lines.add("&e&l右键查看");
        } else {
            lines.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 50) + " &c时解锁");
        }
        return lines;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getStatusNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTczMjk2Mjc3NjU5NiwKICAicHJvZmlsZUlkIiA6ICI0Yjg1NjFlNjRmOGE0NTY0YjA2MzY5YTVmMWYzZWExMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaWt1X1Nub3ciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTFmOTU0OGRhMjVlZmYwMDk4ZWJhZmYyYWVhZjk0MTEwMTc1NTdlOWRmYWMzZTY4YzRkNGNjOWUyYTZkMzk3NiIKICAgIH0sCiAgICAiQ0FQRSIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjlhNzY1Mzc2NDc5ODlmOWEwYjZkMDAxZTMyMGRhYzU5MWMzNTllOWU2MWEzMWY0Y2UxMWM4OGYyMDdmMGFkNCIKICAgIH0KICB9Cn0=",
                "NHQ1hTpiIm3MtyGTvQAf0fcBUH/Su1n9+ltcFXTvbDmBfTgdGxIQt6dbxkyml15QER3NM0ATXaSkB2MEOnGsNElUwTu5Pli2OCV0prwsccFO3f+S7YvwkykuDaKu/+Xbu0wFETrs+PYlp7kfcsITJb3u1NMlB1kkTx1zoedyyjC1E1raIVu2MWd9Sppr35wagZH6DQ+C7mSAC0qqpLbRU7YJ5bIdkl3Dzcv00rCDxi5J79w/FH11eWQtok0PQfIg8PF/o8LPKd62OC++06+4nhZrpjyiX4nD+OycMOOOSuTYgVXPUWqzolw7Z1VXhbL7LfoYilyDUckBtwjBrttXrzJMdFmKQGJ+ef2WOVifr4ecvBm2syqVGnFYjXzUg3knheGKrM8GYxnS+a+R9Lda0AzwniOUKvYrgoD0tkBp1q7OiQ6mnpsoIR2Dl4HNAmkVxFLl9vGLeUjG8IwiqE/0K69DwDXK6c1r1StXw69X+Olm2r0tjJkR411+JtKU1R/xJzrZlG2yfaLq3GA7ySF6cVsOpD8cNgky2TbNEwrnK49Kt93BEHm466z54/Mn1HEftTCAfWHuVBTbcurm2zR1JlX5z56n+NpM/PJvC/RPDxsJX16WYbHYNsaTa7yMnCSDIxTTTBySvxCPXBuJZfOBLFlqJUI7VB37tqGi1GxnAaA="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel() < 50 && profile.getPrestige() == 0) {
            player.sendMessage(CC.translate("&c&l等级不足! &7统计在 " + LevelUtil.getLevelTag(profile.getPrestige(), 50) + " &7时解锁."));
        } else {
            new StatusMenu().openMenu(player);
        }
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
