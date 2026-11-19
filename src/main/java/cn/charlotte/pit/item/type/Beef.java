package cn.charlotte.pit.item.type;

import cn.charlotte.pit.item.AbstractPitItem;
import cn.charlotte.pit.item.listenerType.IPlayerInteract;
import cn.charlotte.pit.listener.CombatListener;
import cn.charlotte.pit.perk.type.bundles.beastmode.AAABeefStreak;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.cooldown.Cooldown;
import cn.charlotte.pit.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.TimeUnit;

public class Beef extends AbstractPitItem implements IPlayerInteract{

    @Override
    protected String getDefaultItemDisplayName() {
        return "&eAAA级牛排";
    }

    
    @Override
    public ItemStack toItemStack(){
        return new ItemBuilder(Material.MUTTON)
                .name(getItemDisplayName())
                .lore(
                        "&7使用后给予",
                        "",
                        "&7增加 &c10% 伤害加成&f(0:10)",
                        "&b速度I&f(0:10)",
                        "&3抗性I&f(0:10)",
                        ""
                )
                .deathDrop(true)
                .removeOnJoin(true)
                .canSaveToEnderChest(false)
                .canDrop(false)
                .canTrade(false)
                .internalName(getInternalName())
                .build();
    }
    
    @Override
    public String getInternalName(){
        return "AAABeef";
    }
    
    @Override
    public void handlePlayerInteract(Player player,ItemStack itemStack,EquipmentSlot hand,PlayerInteractEvent event){
        Cooldown cooldown = AAABeefStreak.beefEffectMap.get(player.getUniqueId());
        if(cooldown != null && !cooldown.hasExpired()){
            CC.send(player,"&c你必须得等到上一个牛排效果结束才能使用");
            return;
        }
        AAABeefStreak.beefEffectMap.put(player.getUniqueId(),new Cooldown(10,TimeUnit.SECONDS));
        PlayerUtil.takeOneItem(player,hand);
        PlayerUtil.addEffect(player,new PotionEffect(PotionEffectType.SPEED,200,0));
        PlayerUtil.addEffect(player,new PotionEffect(PotionEffectType.RESISTANCE,200,0));
    }
}
