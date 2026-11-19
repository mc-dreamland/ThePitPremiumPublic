package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.quest.main.QuestMenu;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.level.LevelUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/19 18:38
 */
public class QuestNpc extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "quest";
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
        lines.add("&3&l任务");
        if (profile.getLevel() >= 30 || profile.getPrestige() > 0) {
            lines.add("&e&l右键查看");
        } else {
            lines.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 30) + " &c时解锁");
        }
        return lines;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getQuestNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin("ewogICJ0aW1lc3RhbXAiIDogMTczMzAyMzQ4NDIyMywKICAicHJvZmlsZUlkIiA6ICI4ZDEwODFlYjQxMzg0Yjg1YjNjODdjNjJhZGNhNTIxOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJjeXciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQ2YTU2ZDYzYTZjNjYwMjE2MDVkZDg5NjVmNmE5MzdlYmJkYzAyYmI4MTkyMDVlZGFhOGVjMmE5ZTBhMDViMCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                "UhmhXhYY68IvLjxNuDoOGFlyUdgCRCHGfFl3Gg5vPceLJ2WvNtpLuePvYsJ4P7JvRs3TjFsiM89k3aJIwqaN91ii9SPn8HyAVOsAzUAJ/fSUDEe8I0F5BEgQ3aytS8T17/AMgPRu1XN0WffJvHaTw9kvMdd2ciWUIa/tpy5uWgTHkEaqDWjJvvh2lcTUhEXRNddFee53myi7fHmkoPrRj7OcctuKoDseNuyK7BR7dKeKJfsnGF2yUyyHWPvQk3v9ehi7Ar9J7bfAQ8k/Yzo/R5OFa+7mqZST1AAw9bZGM3sE4nnS1YxsbTJHmxA3qxFqTQ89Exy7p77gVNx48VPMaJXYk9elIBnnbIQ0bOJjt5OYRoF1WorNtLPNdrfGJYRd4ZAYcC5iKckWaOdw/0zQ9SB8Qu561pVrT3BZ1xYiI3cfnm9uVLTs6pKZHUQ9Ri++Fn2LS3c6l5JB/Gi7m13d5EVVtBaLPX8viupf4KIKYfIbbgI0kkc2JXkKjwde5j+LmbvXnON2g1tQDNihu1DL+q7Gul3GY41h4hjK1FdGmeT0Im0mfz52JV/4lLRbNbNmwvIfA9kuMtyFN5Wu8el48GCzIilq7ets9RL0uheL5C/tr1HHzpRvEirRlAgErr3aWiFvUfwDt46HOSCj6FdVdgGcXxAqUr7T0JcKXa0WrVE="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel() < 30) {
            player.sendMessage(CC.translate("&c&l等级不足! &7任务在 " + LevelUtil.getLevelTag(profile.getPrestige(), 30) + " &7时解锁."));
            return;
        }
        new QuestMenu().openMenu(player);
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return new ItemStack(Material.BOOK);
    }
}
