package cn.charlotte.pit.event;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.menu.mystery.MysteryBuildData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@EqualsAndHashCode(callSuper = true)
@Data
public class PitToMysteryBuildEvent extends PitEvent {

    private Player player;
    private PlayerProfile profile;
    private ItemStack mainItem;
    private ItemStack offItem;
    private MysteryBuildData data;


    public PitToMysteryBuildEvent(Player player, PlayerProfile profile, ItemStack mainItem, ItemStack offItem, MysteryBuildData data) {
        this.player = player;
        this.profile = profile;
        this.mainItem = mainItem;
        this.offItem = offItem;
        this.data = data;
    }
}
