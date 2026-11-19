package cn.charlotte.pit.menu.customname;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomNamePaperUseTest {
    private final UUID playerUuid = UUID.randomUUID();
    private final UUID paperUuid = UUID.randomUUID();
    private final String name = "&a测试改名";
    private final CustomNamePaperUse use = new CustomNamePaperUse(playerUuid, paperUuid, name);

    @Test
    void aVoucherAuthorizesExactlyOneRenameAcrossAllButtonsAndRepeatedClicks() {
        assertTrue(use.tryUse(playerUuid, paperUuid, name, true));
        assertFalse(use.isActive());
        for (int button = 0; button < 41; button++) {
            assertFalse(use.tryUse(playerUuid, paperUuid, name, true));
        }
    }

    @Test
    void anotherVoucherCannotPayForTheOldMenusName() {
        assertFalse(use.tryUse(playerUuid, UUID.randomUUID(), name, true));
        assertTrue(use.isActive());
        assertTrue(use.tryUse(playerUuid, paperUuid, name, true));
    }

    @Test
    void changedNamesAndUnapprovedVouchersCannotBeUsed() {
        assertFalse(use.tryUse(playerUuid, paperUuid, "另一个名字", true));
        assertFalse(use.tryUse(playerUuid, paperUuid, name, false));
        assertTrue(use.isActive());
        assertTrue(use.tryUse(playerUuid, paperUuid, name, true));
    }

    @Test
    void anotherPlayerCannotUseTheMenu() {
        assertFalse(use.tryUse(UUID.randomUUID(), paperUuid, name, true));
        assertTrue(use.isActive());
    }

    @Test
    void missingVoucherDataCannotMatch() {
        assertFalse(use.tryUse(playerUuid, null, name, true));
        assertFalse(use.tryUse(playerUuid, paperUuid, null, true));
        assertFalse(use.tryUse(null, paperUuid, name, true));
        assertTrue(use.isActive());
    }

    @Test
    void closingOrRejectingTheMenuPermanentlyInvalidatesItsCallbacks() {
        use.close();
        use.close();
        assertFalse(use.isActive());
        assertFalse(use.tryUse(playerUuid, paperUuid, name, true));
    }

    @Test
    void openingAgainWithAnUnspentVoucherCreatesAnIndependentSession() {
        use.close();
        CustomNamePaperUse reopened = new CustomNamePaperUse(playerUuid, paperUuid, name);
        assertTrue(reopened.tryUse(playerUuid, paperUuid, name, true));
        assertFalse(use.tryUse(playerUuid, paperUuid, name, true));
    }

    @Test
    void anEmptyNameCannotConsumeAVoucher() {
        CustomNamePaperUse emptyName = new CustomNamePaperUse(playerUuid, paperUuid, "");
        assertFalse(emptyName.tryUse(playerUuid, paperUuid, "", true));
    }
}
