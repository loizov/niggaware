package ru.niggaware.event;

import net.minecraft.network.Packet;

/**
 * Событие получения пакета с сервера
 */
public class EventReceivePacket {
    private Packet<?> packet;
    private boolean cancelled;
    
    public EventReceivePacket(Packet<?> packet) {
        this.packet = packet;
        this.cancelled = false;
    }
    
    public Packet<?> getPacket() {
        return packet;
    }
    
    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}