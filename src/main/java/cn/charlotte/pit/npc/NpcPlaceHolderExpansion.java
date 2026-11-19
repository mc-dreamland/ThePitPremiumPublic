package cn.charlotte.pit.npc;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NpcPlaceHolderExpansion extends PlaceholderExpansion {
    @NotNull
    @Override
    public String getIdentifier() {
        return "pitnpcholo";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Miku_Snow";
    }

    @NotNull
    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }


    /**
     * %pitnpcholo_npcname_line%
     *
     * @param player
     * @param params
     * @return
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        if (!player.isOnline()) {
            return "";
        }
        String[] s = params.split("_");
        String npcName = s[0];
        int index = Integer.parseInt(s[1]);
        AbstractPitNPC abstractPitNPC = NpcFactory.getPitNpcMap().get(npcName);
        if (abstractPitNPC == null) {
            return "";
        }
        List<String> textLine = abstractPitNPC.getNpcTextLine(player.getPlayer());
        if (index > textLine.size() - 1) {
            return "";
        }
        return textLine.get(index);
    }
}
