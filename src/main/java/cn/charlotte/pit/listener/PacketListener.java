package cn.charlotte.pit.listener;

import cn.charlotte.pit.ThePit;
import cn.charlotte.pit.data.PlayerProfile;
import cn.charlotte.pit.event.PitPotionAddEvent;
import cn.charlotte.pit.events.impl.major.RedVSBlueEvents;
import cn.charlotte.pit.util.MaterialConvert;
import cn.charlotte.pit.util.item.ItemBuilder;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/2/4 14:56
 */
public class PacketListener extends PacketListenerAbstract {

    public PacketListener() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        final Player viewer = event.getPlayer();

        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            handleEquipmentPacket(event);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
            handleEffectPacket(event, viewer);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            handleMetadataPacket(event, viewer);
        }
    }

    private void handleEquipmentPacket(PacketSendEvent event) {
        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        int entityId = wrapper.getEntityId();

        if (!"red_vs_blue".equals(ThePit.getInstance().getEventFactory().getActiveEpicEventName())) {
            return;
        }

        RedVSBlueEvents activeEpicEvent = (RedVSBlueEvents) ThePit.getInstance().getEventFactory().getActiveEpicEvent();
        List<Equipment> equipment = wrapper.getEquipment();

        for (Equipment slot : equipment) {
            if (slot.getSlot() != EquipmentSlot.HELMET) {
                continue;
            }

            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.getEntityId() != entityId) {
                    continue;
                }

                ItemStack helmetMaterial = null;
                if (activeEpicEvent.getRedTeam().contains(target.getUniqueId())) {
                    helmetMaterial = SpigotConversionUtil.fromBukkitItemStack(
                            new ItemBuilder(MaterialConvert.WOOL[14]).build());
                } else if (activeEpicEvent.getBlueTeam().contains(target.getUniqueId())) {
                    helmetMaterial = SpigotConversionUtil.fromBukkitItemStack(
                            new ItemBuilder(MaterialConvert.WOOL[11]).build());
                }

                if (helmetMaterial != null) {
                    slot.setItem(helmetMaterial);
                    event.markForReEncode(true);
                }
                break;
            }
            break;
        }
    }

    private void handleEffectPacket(PacketSendEvent event, Player viewer) {
        WrapperPlayServerEntityEffect wrapper = new WrapperPlayServerEntityEffect(event);
        int entityId = wrapper.getEntityId();
        if (viewer.getEntityId() != entityId) return;

        final PotionEffectType potion = SpigotConversionUtil.toBukkitPotionEffectType(wrapper.getPotionType());
        final int level = wrapper.getEffectAmplifier();
        final int duration = wrapper.getEffectDurationTicks();
        final boolean hide = !wrapper.isVisible();

        Bukkit.getScheduler().runTaskLater(ThePit.getInstance(), () -> {
            PitPotionAddEvent potionEvent = new PitPotionAddEvent(viewer, new PotionEffect(potion, duration, level, hide));
            potionEvent.callEvent();

            if (potionEvent.isCancelled()) {
                viewer.removePotionEffect(potion);
            }
        }, 1L);
    }

    private void handleMetadataPacket(PacketSendEvent event, Player viewer) {
        WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);

        PlayerProfile viewerProfile = PlayerProfile.getPlayerProfileByUuid(viewer.getUniqueId());
        if (viewerProfile.getPlayerOption().isShowGlowing()) {
            return;
        }

        int entityId = wrapper.getEntityId();
        UUID targetUUID = PlayerListener.playerIdCache.get(entityId);
        if (targetUUID == null) {
            return;
        }

        if (targetUUID.equals(viewer.getUniqueId())) {
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            return;
        }

        List<EntityData<?>> metadata = wrapper.getEntityMetadata();
        for (int i = 0; i < metadata.size(); i++) {
            EntityData<?> data = metadata.get(i);
            if (data.getIndex() == 0 && data.getValue() instanceof Byte flags) {
                byte newFlags = (byte) (flags & ~0x40);
                metadata.set(i, new EntityData<>(0, EntityDataTypes.BYTE, newFlags));
                event.markForReEncode(true);
                break;
            }
        }
    }
}
