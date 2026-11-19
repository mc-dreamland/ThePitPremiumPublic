/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2016 Jaxon A Brown
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 *  rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 *  persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 *  OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package cn.charlotte.pit.util.beam.protocol;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The PacketFactory generates and modifies the packets for the library to use.
 * If you are looking into making your own Guardian Beam type, this class is for you.
 *
 * @author Jaxon A Brown
 */
public class PacketFactory {
    /**
     * Creates a packet to spawn a squid at the location.
     *
     * @param location location to spawn the squid.
     * @return beam packet used to spawn for players.
     */
    public static WrappedBeamPacket createPacketSquidSpawn(Location location) {
        int eid = EIDGen.generateEID();

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 32));

        WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(
                eid,
                UUID.randomUUID(),
                EntityTypes.SQUID,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                0,
                Vector3d.zero(),
                metadata
        );
        return new WrappedBeamPacket(wrapper);
    }

    /**
     * Creates a packet to spawn a guardian at the location.
     *
     * @param location    location to spawn the guardian.
     * @param squidPacket squid the guardian will target.
     * @return beam packet used to spawn for players.
     */
    public static WrappedBeamPacket createPacketGuardianSpawn(Location location, WrappedBeamPacket squidPacket) {
        int eid = EIDGen.generateEID();

        WrapperPlayServerSpawnLivingEntity squidWrapper = (WrapperPlayServerSpawnLivingEntity) squidPacket.handle();
        int squidEntityId = squidWrapper.getEntityId();

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 32));
        metadata.add(new EntityData<>(16, EntityDataTypes.INT, 0));
        metadata.add(new EntityData<>(17, EntityDataTypes.INT, squidEntityId));

        WrapperPlayServerSpawnLivingEntity wrapper = new WrapperPlayServerSpawnLivingEntity(
                eid,
                UUID.randomUUID(),
                EntityTypes.GUARDIAN,
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                0,
                Vector3d.zero(),
                metadata
        );
        return new WrappedBeamPacket(wrapper);
    }

    /**
     * Modifies location information of the given Spawn Packet.
     *
     * @param entitySpawnPacket SquidSpawn or GuardianSpawn packet for the entity.
     * @param location          location the entity should be spawned at.
     * @return beam packet used to spawn for players.
     */
    public static WrappedBeamPacket modifyPacketEntitySpawn(WrappedBeamPacket entitySpawnPacket, Location location) {
        WrapperPlayServerSpawnLivingEntity original = (WrapperPlayServerSpawnLivingEntity) entitySpawnPacket.handle();

        WrapperPlayServerSpawnLivingEntity modified = new WrapperPlayServerSpawnLivingEntity(
                original.getEntityId(),
                original.getEntityUUID(),
                original.getEntityType(),
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                original.getHeadPitch(),
                original.getVelocity(),
                original.getEntityMetadata()
        );
        return new WrappedBeamPacket(modified);
    }

    /**
     * Creates a packet to move an entity. Doesn't include where to move it to.
     *
     * @param entityPacket SquidSpawn or GuardianSpawn packet for the entity.
     * @return Skeleton packet for the given entity.
     */
    public static WrappedBeamPacket createPacketEntityMove(WrappedBeamPacket entityPacket) {
        WrapperPlayServerSpawnLivingEntity spawnWrapper = (WrapperPlayServerSpawnLivingEntity) entityPacket.handle();

        WrapperPlayServerEntityTeleport wrapper = new WrapperPlayServerEntityTeleport(
                spawnWrapper.getEntityId(),
                Vector3d.zero(),
                0,
                0,
                false
        );
        return new WrappedBeamPacket(wrapper);
    }

    /**
     * Adds location information to a packet to move an entity.
     *
     * @param entityMovePacket EntityMove packet to add location information to.
     * @param location         location to move the entity to.
     * @return Finished packet to teleport the given entity.
     */
    public static WrappedBeamPacket modifyPacketEntityMove(WrappedBeamPacket entityMovePacket, Location location) {
        WrapperPlayServerEntityTeleport original = (WrapperPlayServerEntityTeleport) entityMovePacket.handle();

        WrapperPlayServerEntityTeleport modified = new WrapperPlayServerEntityTeleport(
                original.getEntityId(),
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                location.getYaw(),
                location.getPitch(),
                original.isOnGround()
        );
        return new WrappedBeamPacket(modified);
    }

    /**
     * Creates a packet to remove the guardian and squid entities.
     *
     * @param squidPacket    SquidSpawn of the entity to remove
     * @param guardianPacket GuardianSpawn of the entity to remove
     * @return Packet to remove the guardian and squid when sent to a player.
     */
    public static WrappedBeamPacket createPacketRemoveEntities(WrappedBeamPacket squidPacket, WrappedBeamPacket guardianPacket) {
        WrapperPlayServerSpawnLivingEntity squidWrapper = (WrapperPlayServerSpawnLivingEntity) squidPacket.handle();
        WrapperPlayServerSpawnLivingEntity guardianWrapper = (WrapperPlayServerSpawnLivingEntity) guardianPacket.handle();

        WrapperPlayServerDestroyEntities wrapper = new WrapperPlayServerDestroyEntities(
                squidWrapper.getEntityId(),
                guardianWrapper.getEntityId()
        );
        return new WrappedBeamPacket(wrapper);
    }
}
