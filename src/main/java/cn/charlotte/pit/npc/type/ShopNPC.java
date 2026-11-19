package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.shop.ShopMenu;
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
 * @Created_In: 2021/1/1 11:16
 */
public class ShopNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "shop";
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
        lines.add("&b&l商店");
        if (profile.getLevel() >= 10) {
            lines.add("&e&l右键查看");
        } else {
            lines.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 10) + " &c时解锁");
        }
        return lines;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getShopNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTc0NzQ3MTAyNzQ1NiwKICAicHJvZmlsZUlkIiA6ICJiMTM1MDRmMjMxOGI0OWNjYWFkZDcyYWVhYmMyNTQ1MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUeXBrZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UxOTRkNDA1YmYyZDcxMmY4MDJiYmE5ZDJjMjA3NzJhZGRlNmVlMWYyNjQzMzFhNjliYzk5ZDNiZTVhMjMwYyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                "uicAH/ep3W/OwQbDtHuXGPLrunvVd1tQq99xknX7EKAZxL5h59iXcnYUM8LsGvtLgqemnr9jVaoP30NPspVP0jQWeMlonQo2kRjtQ05QG4m/aAK7/6bb8VP58HdM/NC68xk7s4ngdNzucGu9lhW5vD9rX+3lpdsF30zuzTJ/A9DsBZJxpiCrKLjGfiwf6d5i3eC8SfTWpMCKe1bekramgCEDvpSz0qTHy9pvkIIXY/2SuIvToeQZ8PsITaqrKcjcBD1o93x/dNymEdaZiImf2xMujBTYh596eFDViGaYBSNHAGeoRpvkN2FkIWyd9OaDbRwVMcRmkuNV0tlsbL76/8F+INRC6hnB+T3tMVC+gLqveymMWBDCZtbO9dGtkgIvSdTXX+ZUU4FB9b9nXWW/UU7ulGW+sPzclOiwoMQJ+EQ7/N/ZLUC72ujJrQvl+WZK//D19DfGxj6SONJsT0EzS3CKkBt5u8Pli5DeZ+5PfH4BZ0ZquoHCuzL5bioPm3iuG6JJ6KfO3WIbSfi+8QQLMXxxJFkbdrNp7WsuQKmbCsJGt8VjtnOozFP29GDeE7pWAaWRTeU4lcYOW/ohjIO1yJyQFjIDo4Yu6WGogBJGBeCxOhz3udj6QiRiiAHNOrkjeAusjx0zZx1XopWjJ1PavtFXB7ahboJKWrYb6EZ+6lY=");
    }

    @Override
    public void handlePlayerInteract(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel() < 10) {
            player.sendMessage(CC.translate("&c&l等级不足! &7商店在 " + LevelUtil.getLevelTag(profile.getPrestige(), 10) + " &7时解锁."));
        } else {
            new ShopMenu().openMenu(player);
        }
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
