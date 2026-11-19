package cn.charlotte.pit.util.item;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemGrantTest {
    @Test
    void enabledBatchProducesOneFreshIdentityPerItemWithoutMutatingOriginal() {
        TestStack original = new TestStack(4);
        UUID originalId = original.id;

        ItemStack[] grant = AdvancedItemIdentity.splitForGrant(original, item -> true,
                (item, id) -> ((TestStack) item).id = id);

        assertEquals(4, grant.length);
        var ids = new HashSet<UUID>();
        for (ItemStack item : grant) {
            assertNotSame(original, item);
            assertEquals(1, item.getAmount());
            assertNotEquals(originalId, ((TestStack) item).id);
            assertTrue(ids.add(((TestStack) item).id), "Each reward must have a different UUID");
        }
        assertEquals(4, original.getAmount());
        assertEquals(originalId, original.id);
    }

    @Test
    void disabledBatchPreservesStackCountAndDoesNotGenerateUuids() {
        TestStack original = new TestStack(4);

        ItemStack[] grant = AdvancedItemIdentity.splitForGrant(original, item -> false,
                (item, id) -> fail("UUID generation is disabled"));

        assertArrayEquals(new ItemStack[]{original}, grant);
        assertEquals(4, original.getAmount());
    }

    @Test
    void existingSingleRewardKeepsItsUuidWhenPassedThroughDelivery() {
        TestStack original = new TestStack(1);
        UUID originalId = original.id;

        ItemStack[] grant = AdvancedItemIdentity.splitForGrant(original, item -> true,
                (item, id) -> fail("A single item already has its own UUID"));

        assertSame(original, grant[0]);
        assertEquals(originalId, original.id);
    }

    private static final class TestStack extends ItemStack {
        private int amount;
        private UUID id = UUID.randomUUID();

        TestStack(int amount) {
            this.amount = amount;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public TestStack clone() {
            TestStack copy = new TestStack(amount);
            copy.id = id;
            return copy;
        }
    }
}
