package cn.charlotte.pit.commands.admin;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static cn.charlotte.pit.commands.admin.CurrentHistoryItemActions.*;
import static org.junit.jupiter.api.Assertions.*;

class CurrentHistoryItemActionsTest {
    private static final String UUID = "bb7fbf1d-c915-493c-abc7-297e478b47ed";

    @Test void ordinaryRightClickOpensEnchantmentHistoryForCurrentEquipmentWithoutBlacklisting() {
        assertEquals(Action.ENCHANTMENTS, choose(true, false, true, "mythic_sword", UUID));
        assertNotEquals(Action.BLACKLIST, choose(true, false, true, "mythic_sword", UUID));
    }

    @Test void ordinaryLeftClickRetainsTheExistingOwnershipAndBackupHistoryNavigation() {
        assertEquals(Action.HISTORY, choose(true, false, false, "mythic_sword", UUID));
    }

    @Test void shiftActionsStillHavePriorityAndCannotAccidentallyOpenEnchantmentHistoryInstead() {
        assertEquals(Action.BLACKLIST, choose(true, true, true, "mythic_sword", UUID));
        assertEquals(Action.BLACKLIST, choose(true, true, false, "mythic_sword", UUID));
        assertEquals(Action.BLACKLIST, choose(true, true, true, "funky_feather", UUID));
        assertEquals(Action.BLACKLIST, choose(true, true, false, "mythic_sword", null));
    }

    @Test void otherUuidTraceScreensKeepTheirExistingClickBehavior() {
        for (boolean shift : List.of(false, true)) for (boolean right : List.of(false, true))
            assertEquals(Action.HISTORY, choose(false, shift, right, "mythic_sword", UUID));
    }

    @Test void missingIdentityCannotOfferOrOpenEquipmentEnchantmentHistory() {
        for (String uuid : Arrays.asList(null, "", "  ")) {
            assertFalse(canViewEnchantments("mythic_sword", uuid));
            assertEquals(Action.HISTORY, choose(true, false, true, "mythic_sword", uuid));
        }
    }

    @Test void supportedEquipmentFamiliesMatchTheExistingLocalEnchantmentBuilderIncludingSpecialEquipment() {
        for (String internal : List.of("mythic_sword", "mythic_bow", "mythic_leggings", "angel_chestplate",
                "exorcism_armor", "kings_helmet", "armageddon_boots", "jewel_sword", "magic_fishing_rod")) {
            assertTrue(MaterialEnchantmentHistory.eligible(internal));
            assertTrue(canViewEnchantments(internal, UUID), internal);
            assertEquals(Action.ENCHANTMENTS, choose(true, false, true, internal, UUID), internal);
        }
        assertTrue(canViewEnchantments("MYTHIC_SWORD", UUID));
    }

    @Test void nonEnchantableConsumablesDoNotAdvertiseAFalseEntryOrChangeTheirOriginalRightClickBehavior() {
        for (String internal : Arrays.asList(null, "", "funky_feather", "phase_book", "totally_legit_gem",
                "mystery_box_two", "new_year_lucky_bag", "shopitem")) {
            assertFalse(canViewEnchantments(internal, UUID));
            assertEquals(Action.HISTORY, choose(true, false, true, internal, UUID));
        }
    }
}
