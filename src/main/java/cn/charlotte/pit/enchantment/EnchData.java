package cn.charlotte.pit.enchantment;

import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.item.IMythicItem;
import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Data
public class EnchData {

    private ItemStack AttachedEnchant;
    private IMythicItem mythicItem;
    private PlayerProfile profile;
    private boolean cancel;
    private Map<String, Object> args;

    public EnchData() {
        this.AttachedEnchant = null;
        this.mythicItem = null;
        this.cancel = false;
        this.profile = null;
        this.args = new HashMap<>();
    }

    public void setIs(ItemStack AttachedEnchant, IMythicItem mythicItem, PlayerProfile profile, Map<String, Object> args, boolean cancel) {
        this.AttachedEnchant = AttachedEnchant;
        this.args = args;
        this.mythicItem = mythicItem;
        this.profile = profile;
        this.cancel = cancel;
    }

    public void setIs(ItemStack AttachedEnchant, IMythicItem mythicItem, PlayerProfile profile) {
        this.AttachedEnchant = AttachedEnchant;
        this.args = new HashMap<>(4);
        this.mythicItem = mythicItem;
        this.profile = profile;
        this.cancel = false;
    }
}
