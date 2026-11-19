package cn.charlotte.pit.data;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * @Author: EmptyIrony
 * @Date: 2021/1/3 12:57
 */
public record LeaderBoardEntry(String name, UUID uuid, int rank, double experience, int prestige) {
    private static SequencedMap<UUID, LeaderBoardEntry> leaderBoardEntries = Maps.newLinkedHashMap();

    public static Map<UUID, LeaderBoardEntry> getLeaderBoardEntries() {
        return LeaderBoardEntry.leaderBoardEntries;
    }

    public static void setLeaderBoardEntries(SequencedMap<UUID, LeaderBoardEntry> leaderBoardEntries) {
        LeaderBoardEntry.leaderBoardEntries = leaderBoardEntries;
    }

}
