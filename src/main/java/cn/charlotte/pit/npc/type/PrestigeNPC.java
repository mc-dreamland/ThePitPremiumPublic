package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.prestige.PrestigeMainMenu;
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
 * @Created_In: 2021/1/1 13:44
 */
public class PrestigeNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "prestige";
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
        lines.add("&6&l精通");
        if (profile.getLevel() >= 120 || profile.getPrestige() > 0) {
            lines.add("&e&l右键查看");
        } else {
            lines.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 120) + " &c时解锁");
        }
        return lines;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getPrestigeNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTczMzAyNDA0Mzc3MCwKICAicHJvZmlsZUlkIiA6ICI3NTA1YzE1MzY5OGM0OTlmYjZhMjk0MDZkMzMyOGI5NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWRkbzE2OCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zZjZlZWRhYmFiMDA5YzY1MWE1Nzg2NTNkNjFjMDFjMDM5Zjc3MGRmN2UwMTgxMjQ3MzgxNDNlY2UwYjdkNGU5IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                "iE/hCxMt4RKWN8vRsLKuGONdUeOtaWDISy7nhbWhzGBSp8laV3kotdqPMUorD/12AkZKmAe3nmsWd13fmRjaNTitvrKq5mhSC82IT7RUyfXrXj8afJM6Pm83u0t1Qpf/CQtnlTl3Ajpdv/n5VFPVKzb0cesxZQ64IWsI1AsBzofJYNdAcaXXMKZNTDJg/V9L8c64T56Fas8RnO4Ta2onCqUBSJ9o0JlSavrk3opwm1BsHE6KU3T+Nuq7yr7e3FK7Q/Jf1COehd3q8ViwiclfMJcNGC6Q7TbcfMFN6vmVEa0Inia6BPrOlGiAwpJMpq/l6UO6yn3f9cA/w2XvphyjNccKQ5rySzh3xMv/vb30gfFBO0vaoI9jqqw3lPUT12qvTIVzmGmi5wkezBxZFo1WfcN3tZrhFiLoGNmF6gh2QgYaS04zYvOSTMyEkDc8b7UNaBz3GMQt8cvOZGQ1cLvAQx4db1aWXsZ+m3/7R6GoqhC7u66av6bWenHBtNQqCLPrxYhTKgI/45FeCAdYI8rO/YPbnYHJv6B5JNnV6bNdtihHSi2XnWdynGBUyXOalXVREGehhiIXJ15J0NamoPdWxkz1IYWb9zQw2grZMQolH1fpcrX2DEl7F9pNDf/v45nvspoVPc49JlInH6ewHKzOqtf20wjvb1ytSAdsYaYKQkM="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel() < 120 && profile.getPrestige() == 0) {
            player.sendMessage(CC.translate("&c&l等级不足! &7精通在 " + LevelUtil.getLevelTag(profile.getPrestige(), 120) + " &7时解锁."));
        } else {
            new PrestigeMainMenu().openMenu(player);
        }
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
