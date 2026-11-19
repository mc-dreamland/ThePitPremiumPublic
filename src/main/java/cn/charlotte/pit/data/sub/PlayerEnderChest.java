package cn.charlotte.pit.data.sub;

import cn.charlotte.pit.data.deserializer.EnderChestDeserializer;
import cn.charlotte.pit.data.serializer.EnderChestSerializer;
import cn.charlotte.pit.util.inventory.InventoryUtil;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author EmptyIrony, Yurinan
 * @since 2020/12/29 23:05
 */

@Setter
@Getter
@JsonSerialize(using = EnderChestSerializer.class)
@JsonDeserialize(using = EnderChestDeserializer.class)
public class PlayerEnderChest {

    private Inventory inventory;
    private int rows;

    public PlayerEnderChest(int rows) {
        inventory = Bukkit.createInventory(null, rows * 9, "末影箱");
        this.rows = rows;
    }

    public static PlayerEnderChest deserialization(String string, int rows) {
        PlayerEnderChest enderChest = new PlayerEnderChest(rows);
        enderChest.getInventory().setContents(InventoryUtil.stringToItems(string));

        return enderChest;
    }

    public void openEnderChest(Player player) {
        player.closeInventory();
        player.openInventory(inventory);
    }

    public String serialize() {
        int limit = 6 * 9;

        ItemStack[] itemStacks = new ItemStack[limit];
        for (int i = 0; i < limit; i++) {
            itemStacks[i] = inventory.getItem(i);
        }

        return InventoryUtil.itemsToString(itemStacks);
    }

}
