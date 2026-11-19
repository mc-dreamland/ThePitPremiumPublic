package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.util.chat.CC;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ContractNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "ContractNPC";
    }

    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e&l合同大师",
                "&7接受合同以达到更高的目标"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        return List.of(
                "&e&l合同大师",
                "&7接受合同以达到更高的目标"
        );
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getContractnpcLocation();
    }


    @Override
    public Skin getNpcSkin() {
        return new Skin(
                "ewogICJ0aW1lc3RhbXAiIDogMTczNzU3NDI5OTE5MiwKICAicHJvZmlsZUlkIiA6ICJiZDNhNWRmY2ZkZjg0NDczOTViZDJiZmUwNGY0YzAzMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJwcmVja3Jhc25vIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzIwMGZkZmE4NTI0ZTVmMjU2YzUyMzAwOGRkMzBmNGQxNjRjZDZmZTE4N2E4ODZkYWQ0ZmIwMzEyMGYxNTdkMzUiCiAgICB9CiAgfQp9",
                "ZkCt0hyZ9r32sxk1tsMrwIJ51pmv0YA7CSl1fjGpqiDVJOoGTvJiEfiCUNAz5KED9IAmBB0N2GTJmxIKltj+D9V+QeEVn4Yws+Ld3BgwtffmertQw3YkqNixp+J0Ogx2Djl8XHdZv+zQp+12V2JR6D1QDjgL0MYGYU3vYQIMqScXvn7Mu/gEjEJkzQJFiZA8yV89mzyUD2Go0opJq0W/tmqhIO41FD3llkVOP1TnvEoX6N6pzlbzn+tcKJqIdrMf0FNv8+XNrOeK+oc5ybMzKDk8k8Q7mB3U59ipe9x1OptM7HSp5a0WB6DV+58l4SIqpM349oPNNIlSNGaS6Jxidfhz0JbK9n+TvIFOcQQrdEkO7uOjir70FTXAoLQK8Hl3ScBvKUd022Nqoyq6CwqwdnfHvN+unjGsqh2RyH0bkyDlQJNbXTsOIITNseAkSU4bC3FGCKen4Y2V9yxxK5D3ynbVeB6u4D4tjo17YiUJaCp8Jwr+kiLXi2FlV2wA2mn/60l4IOTFuVk6LtjDhc+db1q619ClC7C4PRg/GHwibb5a0b/EK8MpSmW8svdxCWCV9Y8iIEeyJgvpWxK99lTzuDcU8XsvDNIsKjObBLWTOajOZBQRTYtkQDYdGCPLbPyWhA6ULPLoTZtdsQfHnI3HZjFgqM1RiV278EW+M3gMsBk="
        );
    }

    @Override
    public void handlePlayerInteract(Player player) {
        player.sendMessage(CC.translate("&c暂未开放"));
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }
}
