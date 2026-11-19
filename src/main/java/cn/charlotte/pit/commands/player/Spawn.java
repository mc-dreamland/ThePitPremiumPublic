package cn.charlotte.pit.commands.player;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.commands.PitCommands;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitPlayerSpawnEvent;
import cn.charlotte.pit.util.PlayerUtil;
import cn.charlotte.pit.util.chat.CC;
import cn.charlotte.pit.util.item.ItemUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

@CommandAlias("spawn|respawn|home|back")
public class Spawn extends BaseCommand {
    private final PitCommands pitCommands;

    public Spawn(PitCommands pitCommands) {
        this.pitCommands = pitCommands;
    }

    @Default
    public void onCommand(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfile profile = PlayerProfile.getPlayerProfileByUuid(uuid);

        if (!profile.getCombatTimer().hasExpired() || PlayerUtil.isSupperStreak(profile,player)) {
            player.sendMessage(CC.translate("&c&l您无法在战斗中传送！"));
            return;
        }

        if (player.getGameMode() == GameMode.SPECTATOR || player.hasPotionEffect(PotionEffectType.GLOWING)) {
            player.sendMessage(CC.translate("&c&l您无法在当前状态下传送！"));
            return;
        }

        Location location = ThePit.getInstance().getPitConfig()
                .getSpawnLocations()
                .get(PitCommands.random.nextInt(ThePit.getInstance().getPitConfig().getSpawnLocations().size()));

        player.teleport(location);

        for (ItemStack item : player.getInventory()) {
            if (ItemUtil.isRemovedOnJoin(item)) {
                player.getInventory().remove(item);
            }
        }

        if (ItemUtil.isRemovedOnJoin(player.getInventory().getHelmet())) {
            player.getInventory().setHelmet(new ItemStack(Material.AIR));
        }

        if (ItemUtil.isRemovedOnJoin(player.getInventory().getChestplate())) {
            player.getInventory().setChestplate(new ItemStack(Material.AIR));
        }

        if (ItemUtil.isRemovedOnJoin(player.getInventory().getLeggings())) {
            player.getInventory().setLeggings(new ItemStack(Material.AIR));
        }

        if (ItemUtil.isRemovedOnJoin(player.getInventory().getBoots())) {
            player.getInventory().setBoots(new ItemStack(Material.AIR));
        }
        profile.setBounty(0);
        profile.setStreakKills(0);
        profile.setInArena(false);

        new PitPlayerSpawnEvent(player).callEvent();

        PlayerUtil.clearPlayer(player, true, false);
    }
}