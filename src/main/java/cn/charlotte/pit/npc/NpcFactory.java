package cn.charlotte.pit.npc;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.event.PitProfileLoadedEvent;
import cn.charlotte.pit.npc.runnable.NpcRunnable;
import cn.charlotte.pit.npc.type.GenesisAngelNpc;
import cn.charlotte.pit.npc.type.GenesisDemonNpc;
import cn.charlotte.pit.parm.AutoRegister;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import lombok.SneakyThrows;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.npc.CitizensNPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2020/12/30 22:35
 */
@AutoRegister
public class NpcFactory implements Listener {
    private static final HashMap<String, AbstractPitNPC> pitNpc = new HashMap<>();
    private static final HashMap<UUID, AbstractPitNPC> pitNpcByUUID = new HashMap<>();
    @Getter
    private static NPCRegistry registry;

    static {
        registry = new CitizensNPCRegistry(new MemoryNPCDataStore());
    }

    public static Collection<AbstractPitNPC> getPitNpc() {
        return NpcFactory.pitNpc.values();
    }

    public static HashMap<String, AbstractPitNPC> getPitNpcMap() {
        return NpcFactory.pitNpc;
    }

    @SneakyThrows
    public void init(Collection<Class<? extends AbstractPitNPC>> classes) {

        for (Class<?> clazz : classes) {
            if (!ThePit.isDEBUG_SERVER()) {
                if (clazz.getName().contains("InfinityItemNPC")) {
                    continue;
                }

            }
            if (AbstractPitNPC.class.isAssignableFrom(clazz)) {
                AbstractPitNPC abstractPitNPC = (AbstractPitNPC) clazz.newInstance();
                if (abstractPitNPC instanceof GenesisAngelNpc || abstractPitNPC instanceof GenesisDemonNpc) {
                    if (ThePit.getInstance().getPitConfig().getGenesisStartDate() >= System.currentTimeMillis()) {
                        ThePit.getInstance().info(String.format("Npc %s 未在活动期间，未启用!", abstractPitNPC.getNpcInternalName()));
                        continue;
                    }
                }
                NPC npc = registry.createNPC(abstractPitNPC.getEntityType(), "");

                npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                npc.data().set(NPC.Metadata.NAMEPLATE_VISIBLE, false);
                npc.scheduleUpdate(NPC.NPCUpdate.PACKET);

                if (abstractPitNPC.getNpcHeldItem() != null) {
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.HAND, abstractPitNPC.getNpcHeldItem());
                }

                if (abstractPitNPC.getNpcHeldItem() != null) {
                    npc.getOrAddTrait(Equipment.class).set(Equipment.EquipmentSlot.OFF_HAND, abstractPitNPC.getNpcOffItem());
                }

                if (abstractPitNPC.getNpcSpawnLocation() == null) {
                    ThePit.getInstance().info(String.format("Npc %s,初始化异常，未配置坐标!", abstractPitNPC.getNpcInternalName()));
                    continue;
                }
                npc.spawn(abstractPitNPC.getNpcSpawnLocation());
                abstractPitNPC.setNpc(npc);
                if (npc.getEntity() == null) {
                    ThePit.getInstance().info(String.format("Npc %s,初始化异常，尝试强制加载区块！", abstractPitNPC.getNpcInternalName()));
                    abstractPitNPC.getNpcSpawnLocation().getWorld().loadChunk(abstractPitNPC.getNpcSpawnLocation().getChunk());
                    npc.spawn(abstractPitNPC.getNpcSpawnLocation());
                    if (npc.getEntity() == null) {
                        ThePit.getInstance().info(String.format("Npc %s,初始化异常", abstractPitNPC.getNpcInternalName()));
                        continue;
                    }
                }
                if (npc.getEntity().getType().equals(EntityType.PLAYER)) {
                    abstractPitNPC.initSkin(npc);
                }
                npc.teleport(abstractPitNPC.getNpcSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                pitNpc.put(abstractPitNPC.getNpcInternalName(), abstractPitNPC);
                pitNpcByUUID.put(abstractPitNPC.getNpc().getUniqueId(), abstractPitNPC);
            }
        }

        new NpcRunnable().runTaskTimerAsynchronously(ThePit.getInstance(), 20, 20);
    }

    @EventHandler
    @SneakyThrows
    public void onPlayerJoin(PitProfileLoadedEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerProfile().getPlayerUUID());
        if (player == null || !player.isOnline()) {
            return;
        }

        for (AbstractPitNPC abstractPitNPC : pitNpc.values()) {
            if (abstractPitNPC.getHologram() != null) {
                abstractPitNPC.getHologram().setShowPlayer(player);
                abstractPitNPC.updateHoloLines(player);
            } else {
                List<String> holoInfo = abstractPitNPC.getHoloInfo();
                Hologram hologram = DHAPI.getHologram(abstractPitNPC.getNpcInternalName());
                double y = 2.5;
                if (abstractPitNPC.getEntityType() != EntityType.PLAYER) {
                    y = 1.5;
                }
                if (hologram == null) {
                    hologram = DHAPI.createHologram(
                            abstractPitNPC.getNpcInternalName(),
                            abstractPitNPC.getNpcSpawnLocation().clone().add(0, y, 0),
                            false,
                            holoInfo
                    );
                }
                abstractPitNPC.setHologram(hologram);
                hologram.setUpdateInterval(5);
                hologram.show(player, 0);
            }

        }
    }

//    @EventHandler
//    public void onPlayerQuite(PlayerQuitEvent event) {
//        ThePit.getInstance().getServer().getScheduler().runTaskAsynchronously(ThePit.getInstance(), () -> {
//            for (AbstractPitNPC abstractPitNPC : pitNpc.values()) {
//                for (HologramPage page : abstractPitNPC.getHologram().getPages()) {
//                    for (HologramLine line : page.getLines()) {
//                        line.getPlayerTextMap().remove(event.getPlayer().getUniqueId());
//                        line.getLastTextMap().remove(event.getPlayer().getUniqueId());
//                    }
//                }
//            }
//        });
//
//    }

    @EventHandler
    public void onInteract(NPCLeftClickEvent event) {
        AbstractPitNPC abstractPitNPC = pitNpcByUUID.get(event.getNPC().getUniqueId());
        if (abstractPitNPC == null) {
            return;
        }
        abstractPitNPC.handlePlayerInteract(event.getClicker());
    }

    @EventHandler
    public void onInteract(NPCRightClickEvent event) {
        AbstractPitNPC abstractPitNPC = pitNpcByUUID.get(event.getNPC().getUniqueId());
        if (abstractPitNPC == null) {
            return;
        }
        abstractPitNPC.handlePlayerInteract(event.getClicker());
    }
}
