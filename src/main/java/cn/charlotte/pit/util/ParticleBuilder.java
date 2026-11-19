package cn.charlotte.pit.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public class ParticleBuilder {
    Location location;
    ParticleType particle;
    float offsetX;
    float offsetY;
    float offsetZ;
    float velocity;
    int count;

    public ParticleBuilder(Location location, ParticleType particle) {
        this.location = location;
        this.particle = particle;
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        this.offsetZ = 0.0f;
        this.velocity = 0.0f;
        this.count = 1;
    }

    public void play() {
        ClientboundLevelParticlesPacket particlesPacket = new ClientboundLevelParticlesPacket((ParticleOptions) this.particle, true, (float) this.location.getX(), (float) this.location.getY(), (float) this.location.getZ(), this.offsetX, this.offsetY, this.offsetZ, this.velocity, this.count);

        Bukkit.getOnlinePlayers().forEach(p -> ((CraftPlayer) p).getHandle().connection.sendPacket(particlesPacket));
    }

    public ParticleBuilder setOffsetX(float offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public ParticleBuilder setOffsetY(float offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public ParticleBuilder setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }

    public ParticleBuilder setVelocity(float velocity) {
        this.velocity = velocity;
        return this;
    }

    public ParticleBuilder setCount(int count) {
        this.count = count;
        return this;
    }
}

