package cn.charlotte.pit.nametag.hiding;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Main-thread allocation. Seven legacy colour codes plus reset: unique, zero glyphs, <=16 UTF-16 chars. */
final class NameHidingAliases {
    private final Map<UUID, String> names = new HashMap<>();
    private int sequence;

    String name(UUID player) { return names.computeIfAbsent(player, ignored -> encode(++sequence)); }
    void remove(UUID player) { names.remove(player); }

    static String encode(int value) {
        if (value <= 0 || value > 0xfffffff) throw new IllegalStateException("客户端匿名标识已耗尽，请重启插件");
        StringBuilder result = new StringBuilder(16);
        for (int shift = 24; shift >= 0; shift -= 4) result.append('\u00a7').append(Character.forDigit((value >>> shift) & 15, 16));
        return result.append("\u00a7r").toString();
    }

    static String teamId(String sourceTeam, String alias) {
        // 1.20.6 accepts long team IDs. Keeping the original first preserves the existing priority sorting.
        StringBuilder token = new StringBuilder(7);
        for (int i = 1; i < 14; i += 2) token.append(alias.charAt(i));
        return (sourceTeam == null ? "~" : sourceTeam) + "_nh_" + token;
    }
}
