package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ScanMaterialUuidHistoryIndexTest {

    @Test
    void indexMustRetainOneBecomingTwoIdenticalCopiesButIgnoreMovingSlots() {
        var first = new ScanMaterialUuidHistoryIndex.IndexedItem();
        first.material = "golden_sword";
        first.internalName = "mythic_sword";
        first.displayName = "神话之剑";
        first.amount = 1;
        first.lore = List.of("生命: 19/19");
        first.location = "inventory[0]";
        var moved = new ScanMaterialUuidHistoryIndex.IndexedItem();
        moved.material = first.material;
        moved.internalName = first.internalName;
        moved.displayName = first.displayName;
        moved.amount = first.amount;
        moved.lore = first.lore;
        moved.location = "warehouse[1][0]";
        assertEquals(ScanMaterialUuidHistoryIndex.backupStateHash(List.of(first)),
                ScanMaterialUuidHistoryIndex.backupStateHash(List.of(moved)));
        assertNotEquals(ScanMaterialUuidHistoryIndex.backupStateHash(List.of(first)),
                ScanMaterialUuidHistoryIndex.backupStateHash(List.of(first, moved)));
        assertEquals(ScanMaterialUuidHistoryIndex.backupStateHash(List.of(first, moved)),
                ScanMaterialUuidHistoryIndex.backupStateHash(List.of(moved, first)));
    }

    @Test
    void currentLifeAndLocationIndependentStateMustMatch() {
        String first = ScanMaterialUuidHistoryIndex.comparisonStateHash(
                "golden_sword",
                "mythic_sword",
                "III 阶神话之剑",
                1,
                List.of("生命: 19/19 ", "鲸鲨"),
                "{extra:{boostedByGem:1b,ench:[\"shark:1\",\"vampire:3\"],live:19,maxLive:19,tier:3,uuid:\"same\"}}"
        );
        String second = ScanMaterialUuidHistoryIndex.comparisonStateHash(
                "golden_sword",
                "mythic_sword",
                "III 阶神话之剑",
                1,
                List.of("生命: 18/19 ", "鲸鲨"),
                "{extra:{boostedByEternal:0b,boostedByGem:1b,boostedByPhaseBook:0b,ench:[\"shark:1\",\"vampire:3\"],live:18,maxLive:19,tier:3,traded:0b,uuid:\"same\"}}"
        );

        assertEquals(first, second);
    }
}
