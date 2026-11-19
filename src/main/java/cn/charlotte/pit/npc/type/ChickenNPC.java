package cn.charlotte.pit.npc.type;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.item.PitItems;
import cn.charlotte.pit.npc.Skin;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import cn.charlotte.pit.util.item.ItemUtil;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;


public class ChickenNPC extends SkinNPC {
    @Override
    public String getNpcInternalName() {
        return "ChickenNPC";
    }


    @Override
    public List<String> getHoloInfo() {
        return List.of(
                "&e&l小鸡",
                "&7兑换高级鸡蛋"
        );
    }

    @Override
    public List<String> getNpcDisplayName(Player player) {
        return List.of(
                "&e&l小鸡",
                "&7兑换高级鸡蛋"
        );
    }

    @Override
    public Location getNpcSpawnLocation() {
        return ThePit.getInstance().getPitConfig().getChickenLocation();
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
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (Objects.equals(ItemUtil.getInternalName(itemStack), "yummy_bread")) {
            if (InventoryUtil.isInvFull(player.getInventory())) {
                player.sendMessage(CC.translate("&e小鸡 &c抱歉!你的背包已满"));
                return;
            }
            PlayerUtil.takeOneItem(player, EquipmentSlot.HAND);
            player.getInventory().addItem(PitItems.HIGH_GRADE_EGGS.toItemStack());
            player.sendMessage(CC.translate("&e小鸡 &a感谢您的&e美味的面包,这些送给你!"));
            player.sendMessage(CC.translate("&a&l+ &e高级鸡蛋"));
        } else {
            player.sendMessage(CC.translate("&e小鸡 &c抱歉请你手持 &e美味的面包 &c再进行尝试"));
        }
    }

    @Override
    public ItemStack getNpcHeldItem() {
        return null;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public void initSkin(NPC npc) {
        if (getEntityType() == EntityType.PLAYER) {
            Skin skin = getNpcSkin();
            if (skin != null && skin.value() != null) {
                if (!npc.hasTrait(SkinTrait.class)) {
                    npc.addTrait(SkinTrait.class);
                }
                SkinTrait skinTrait = npc.getTrait(SkinTrait.class);
                skinTrait.setSkinPersistent(
                        "custom_skin_name",
                        skin.signature(),
                        skin.value()
                );
            }
        }
    }
}
