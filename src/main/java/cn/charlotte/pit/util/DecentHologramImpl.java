package cn.charlotte.pit.util;

import cn.charlotte.pit.util.hologram.Hologram;
import cn.charlotte.pit.util.hologram.HologramAPI;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.HologramPage;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DecentHologramImpl implements Hologram {

    private Location loc;
    private String inputText;
    private final UUID uuid = UUID.randomUUID();
    private final eu.decentsoftware.holograms.api.holograms.Hologram hologram;
    private Hologram above;
    private int attachingEntity = -1;
    private final List<Player> receivers = new ArrayList<>();

    public DecentHologramImpl(Location loc, String inputText) {
        this.loc = loc;
        this.inputText = inputText;
        this.hologram = DHAPI.createHologram(uuid.toString(), loc, false, new ArrayList<>(List.of(inputText)));
    }

    @Override
    public boolean isSpawned() {
        return true;
    }

    @Override
    public void spawn(long ticks) {
        spawn();
    }

    @Override
    public boolean spawn(Collection<? extends Player> receivers) {
        hologram.setDefaultVisibleState(false);
        hologram.hideAll();
        if (receivers != null) {
            for (Player receiver : receivers) {
                hologram.setShowPlayer(receiver);
            }
        }
        this.receivers.clear();
        if (receivers != null) {
            this.receivers.addAll(receivers);
        }
        return spawn();
    }

    @Override
    public boolean spawn() {
        return true;
    }

    @Override
    public boolean deSpawn() {
        DHAPI.removeHologram(uuid.toString());
        return true;
    }

    @Override
    public String getText() {
        return inputText;
    }

    @Override
    public void setText(String text) {
        String after = text == null ? "" : text;
        if (after.equals(inputText)) {
            return;
        }
        inputText = after;
        for (HologramPage page : hologram.getPages()) {
            page.setLine(0, inputText);
        }
        hologram.hideAll();
        for (Player receiver : receivers) {
            hologram.setShowPlayer(receiver);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void update(long interval) {
    }

    @Override
    public Location getLocation() {
        return loc;
    }

    @Override
    public void setLocation(Location loc) {
        move(loc);
    }

    @Override
    public void move(Location loc) {
        hologram.setLocation(loc);
        this.loc = loc;
    }

    @Override
    public boolean isTouchable() {
        return false;
    }

    @Override
    public void setTouchable(boolean flag) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Hologram addLineBelow(String text) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Hologram getLineBelow() {
        return null;
    }

    @Override
    public boolean removeLineBelow() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Collection<Hologram> getLinesBelow() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Hologram addLineAbove(String text) {
        Hologram hologramLine = HologramAPI.createHologram(getLocation().add(0.0, 0.25, 0.0), text);
        this.above = hologramLine;
        return hologramLine;
    }

    @Override
    public Hologram getLineAbove() {
        return above;
    }

    @Override
    public boolean removeLineAbove() {
        if (above != null) {
            above.deSpawn();
        }
        return true;
    }

    @Override
    public Collection<Hologram> getLinesAbove() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Collection<Hologram> getLines() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Entity getAttachedTo() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void setAttachedTo(Entity entity) {
        if (entity == null) {
            attachingEntity = -1;
            return;
        }
        attachingEntity = entity.getEntityId();
        hologram.showAll();
    }

    private void sendAttach(Player player) {
        hologram.setShowPlayer(player);
    }
}
