package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.perk.normal.choose.PerkChooseMenu;
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
 * @Created_In: 2021/1/1 13:11
 */
public class PerkNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "perk";
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
        lines.add("&c&l天赋");
        if (profile.getLevel() >= 10) {
            lines.add("&e&l右键查看");
        } else {
            lines.add("&c在 " + LevelUtil.getLevelTag(profile.getPrestige(), 10) + " &c时解锁");
        }
        return lines;
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getPerkNpcLocation();
    }

    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTczOTU4ODE4NDMxMiwKICAicHJvZmlsZUlkIiA6ICIwNDQ4NGZjNzljZDI0YjQyODBlYmMxZDcyZDEyMzhhOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsb2ZpODBzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2VlOWExMjY4ZDYzOTQyYjhlZTUwOGViMWIwZGVmOGYxMDhlM2Y4ZmM3YmUwMjA4ZDRjYTBlY2RiNzVlZDJmYjAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                "h/eOx7Yh9JoaNmo/B5ZJE0F0z9a6kS4/66knpgf318UKpMBoPF6ZiO2+hPR9Uv7+l8HQdtChX+uGY0jC3WGjm7I07suHRp3BiEq2Ggx5FpQC+mi9paf38w3wR1P8bWeTtIA5Ac7RC9rcjzA6x7sDnGVjIMhlsdq+/QlCgpiZV+Py+d2fvqQEbPcS041wTop3VgPwBdVTFkriorMajm5E2Y+fX8+m9wy+o1qlHoWhy+oXAvCcQz38aq6P3o01MHvC9dB39g+jFg+IF2eKcLR3Fcy1cBI6pHTDyTcW/Jx8mvpm6/CQIIYzLxHWwOS18fnPhsqNhuVCQgxnTQOw30CKbgP1N7rj+twjUNytRdzMunf3ervCnBjioUyF2JC1ez5c8V0/5k1krIeIJXRyPsrTlCWoGFf2OpOXkSY6snWjThAr+TB6YFhFzo4Hmyr1pPzewKTp7Cxb3lLeQPveuB5As28rdeu5BNoHef6ik1DpY3wW8gsR/oN1UG96lvRjGakXYhsflqw/ctlAS+kBr+XcUaMHYxgdWo0tnhKahID0ikZXjZjFKgMFEQ0SOfnL6ArD2ORvNqoXm8JffyAhQ04tOzGQBouTo8Ob4mZ17QHbOwT415wwJXZObJ1VNZQr6xAa9kovJbF422Nl5mcMZ10jzCUu2Ews4zBuObrVFiLoG6Y="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(player.getUniqueId());
        if (profile.getLevel() < 10) {
            player.sendMessage(CC.translate("&c&l等级不足! &7天赋在 " + LevelUtil.getLevelTag(profile.getPrestige(), 10) + " &7时解锁."));
        } else {
            new PerkChooseMenu().openMenu(player);
        }
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
