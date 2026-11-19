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

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

/**
 * Wraps a packet wrapper for convenience.
 *
 * @author Jaxon A Brown
 */
public record WrappedBeamPacket(PacketWrapper<?> handle) {
    /**
     * Wraps the packet.
     *
     * @param handle packet to wrap.
     */
    public WrappedBeamPacket {
    }

    /**
     * Sends the packet to a lucky receiver!
     *
     * @param receiver player to send the packet to.
     */
    public void send(Player receiver) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(receiver, this.handle);
    }

    /**
     * Get the packet wrapper.
     *
     * @return packetevents packet wrapper.
     */
    @Override
    public PacketWrapper<?> handle() {
        return this.handle;
    }
}
