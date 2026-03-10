package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class ItemPipeTest {
	private Retronism_TileItemPipe pipe;

	// Use raw IDs to avoid Block/Item static initialization
	private static final int DIRT_ID = 3;
	private static final int STONE_ID = 1;
	private static final int SAND_ID = 12;

	@Before
	public void setUp() {
		pipe = new Retronism_TileItemPipe();
	}

	private ItemStack makeStack(int id, int count) {
		return new ItemStack(id, count, 0);
	}

	// === Buffer / Inventory ===

	@Test
	public void testBufferStartsNull() {
		assertNull(pipe.getStackInSlot(0));
	}

	@Test
	public void testSingleSlotInventory() {
		assertEquals(1, pipe.getSizeInventory());
	}

	@Test
	public void testSetInventorySlotContents() {
		pipe.setInventorySlotContents(0, makeStack(DIRT_ID, 10));
		assertNotNull(pipe.getStackInSlot(0));
		assertEquals(DIRT_ID, pipe.getStackInSlot(0).itemID);
		assertEquals(10, pipe.getStackInSlot(0).stackSize);
	}

	@Test
	public void testSetInventorySlotInvalid() {
		pipe.setInventorySlotContents(1, makeStack(DIRT_ID, 10));
		assertNull(pipe.getStackInSlot(0));
	}

	@Test
	public void testDecrStackSizeAll() {
		pipe.setInventorySlotContents(0, makeStack(DIRT_ID, 5));
		ItemStack result = pipe.decrStackSize(0, 5);
		assertNotNull(result);
		assertEquals(5, result.stackSize);
		assertNull(pipe.getStackInSlot(0));
	}

	@Test
	public void testDecrStackSizePartial() {
		pipe.setInventorySlotContents(0, makeStack(DIRT_ID, 10));
		ItemStack result = pipe.decrStackSize(0, 3);
		assertNotNull(result);
		assertEquals(3, result.stackSize);
		assertEquals(7, pipe.getStackInSlot(0).stackSize);
	}

	@Test
	public void testDecrStackSizeEmpty() {
		assertNull(pipe.decrStackSize(0, 1));
	}

	@Test
	public void testDecrStackSizeWrongSlot() {
		pipe.setInventorySlotContents(0, makeStack(DIRT_ID, 10));
		assertNull(pipe.decrStackSize(1, 1));
	}

	// === Side Config ===

	@Test
	public void testSideConfigDefaultsToIO() {
		for (int side = 0; side < 6; side++) {
			assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT, pipe.getSideMode(side));
		}
	}

	@Test
	public void testSupportsOnlyItemType() {
		assertTrue(pipe.supportsType(Aero_SideConfig.TYPE_ITEM));
		assertFalse(pipe.supportsType(Aero_SideConfig.TYPE_ENERGY));
		assertFalse(pipe.supportsType(Aero_SideConfig.TYPE_FLUID));
		assertFalse(pipe.supportsType(Aero_SideConfig.TYPE_GAS));
	}

	@Test
	public void testSetSideMode() {
		pipe.setSideMode(0, Aero_SideConfig.TYPE_ITEM, Aero_SideConfig.MODE_INPUT);
		assertEquals(Aero_SideConfig.MODE_INPUT, pipe.getSideMode(0));
		assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT, pipe.getSideMode(1));
	}

	@Test
	public void testSetSideModeRejectsUnsupportedType() {
		pipe.setSideMode(0, Aero_SideConfig.TYPE_ENERGY, Aero_SideConfig.MODE_INPUT);
		assertEquals(Aero_SideConfig.MODE_INPUT_OUTPUT, pipe.getSideMode(0));
	}

	// === Receive Item ===

	@Test
	public void testReceiveItemSetsBuffer() {
		assertTrue(pipe.receiveItem(makeStack(DIRT_ID, 1), 2));
		assertNotNull(pipe.getStackInSlot(0));
		assertEquals(DIRT_ID, pipe.getStackInSlot(0).itemID);
	}

	@Test
	public void testReceiveItemRejectsWhenBufferFull() {
		pipe.receiveItem(makeStack(DIRT_ID, 1), 0);
		assertFalse(pipe.receiveItem(makeStack(STONE_ID, 1), 1));
	}

	@Test
	public void testReceiveItemRejectsFilteredItem() {
		pipe.setWhitelist(true);
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		assertFalse(pipe.receiveItem(makeStack(STONE_ID, 1), 0));
		assertNull(pipe.getStackInSlot(0));
	}

	@Test
	public void testReceiveItemAcceptsWhitelistedItem() {
		pipe.setWhitelist(true);
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		assertTrue(pipe.receiveItem(makeStack(DIRT_ID, 1), 0));
		assertNotNull(pipe.getStackInSlot(0));
	}

	// === Slot Access ===

	@Test
	public void testCrusherExtractSlots() {
		Retronism_TileCrusher crusher = new Retronism_TileCrusher();
		assertArrayEquals(new int[]{1}, crusher.getExtractSlots());
	}

	@Test
	public void testCrusherInsertSlots() {
		Retronism_TileCrusher crusher = new Retronism_TileCrusher();
		assertArrayEquals(new int[]{0}, crusher.getInsertSlots());
	}

	@Test
	public void testInvName() {
		assertEquals("Item Pipe", pipe.getInvName());
	}

	@Test
	public void testInventoryStackLimit() {
		assertEquals(64, pipe.getInventoryStackLimit());
	}

	@Test
	public void testGetStackInSlotOutOfBounds() {
		assertNull(pipe.getStackInSlot(1));
		assertNull(pipe.getStackInSlot(-1));
	}

	// === Filter ===

	@Test
	public void testFilterDefaultBlacklistNoFilter() {
		assertFalse(pipe.isWhitelist());
		assertTrue(pipe.passesFilter(makeStack(DIRT_ID, 1)));
		assertTrue(pipe.passesFilter(makeStack(STONE_ID, 1)));
	}

	@Test
	public void testFilterBlacklistBlocksItem() {
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		assertFalse(pipe.passesFilter(makeStack(DIRT_ID, 1)));
		assertTrue(pipe.passesFilter(makeStack(STONE_ID, 1)));
	}

	@Test
	public void testFilterWhitelistAllowsOnlyFiltered() {
		pipe.setWhitelist(true);
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		assertTrue(pipe.passesFilter(makeStack(DIRT_ID, 1)));
		assertFalse(pipe.passesFilter(makeStack(STONE_ID, 1)));
	}

	@Test
	public void testFilterWhitelistEmptyBlocksAll() {
		pipe.setWhitelist(true);
		assertFalse(pipe.passesFilter(makeStack(DIRT_ID, 1)));
	}

	@Test
	public void testFilterMultipleSlots() {
		pipe.setWhitelist(true);
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		pipe.setFilterSlot(3, makeStack(STONE_ID, 1));
		assertTrue(pipe.passesFilter(makeStack(DIRT_ID, 1)));
		assertTrue(pipe.passesFilter(makeStack(STONE_ID, 1)));
		assertFalse(pipe.passesFilter(makeStack(SAND_ID, 1)));
	}

	@Test
	public void testFilterSlotSetAndGet() {
		assertNull(pipe.getFilterSlot(0));
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		assertNotNull(pipe.getFilterSlot(0));
		assertEquals(DIRT_ID, pipe.getFilterSlot(0).itemID);
	}

	@Test
	public void testFilterSlotClear() {
		pipe.setFilterSlot(0, makeStack(DIRT_ID, 1));
		pipe.setFilterSlot(0, null);
		assertNull(pipe.getFilterSlot(0));
	}

	@Test
	public void testFilterSlotOutOfBounds() {
		assertNull(pipe.getFilterSlot(-1));
		assertNull(pipe.getFilterSlot(9));
	}

	@Test
	public void testFilterNullItemFails() {
		assertFalse(pipe.passesFilter(null));
	}

	@Test
	public void testFilterChecksDamage() {
		pipe.setWhitelist(true);
		pipe.setFilterSlot(0, new ItemStack(DIRT_ID, 1, 0));
		assertFalse(pipe.passesFilter(new ItemStack(DIRT_ID, 1, 1)));
		assertTrue(pipe.passesFilter(new ItemStack(DIRT_ID, 1, 0)));
	}

	// === Priority ===

	@Test
	public void testPriorityDefault() {
		assertEquals(5, pipe.getPriority());
	}

	@Test
	public void testPrioritySetGet() {
		pipe.setPriority(0);
		assertEquals(0, pipe.getPriority());
		pipe.setPriority(9);
		assertEquals(9, pipe.getPriority());
	}

	@Test
	public void testPriorityClamped() {
		pipe.setPriority(-1);
		assertEquals(0, pipe.getPriority());
		pipe.setPriority(10);
		assertEquals(9, pipe.getPriority());
	}

	@Test
	public void testSidePriorityDefault() {
		for (int side = 0; side < 6; side++) {
			assertEquals(5, pipe.getSidePriority(side));
		}
	}

	@Test
	public void testSidePrioritySetGet() {
		pipe.setSidePriority(2, 0);
		assertEquals(0, pipe.getSidePriority(2));
		assertEquals(5, pipe.getSidePriority(0));
	}

	@Test
	public void testSidePriorityClamped() {
		pipe.setSidePriority(0, -1);
		assertEquals(0, pipe.getSidePriority(0));
		pipe.setSidePriority(0, 10);
		assertEquals(9, pipe.getSidePriority(0));
	}

	@Test
	public void testWhitelistToggle() {
		assertFalse(pipe.isWhitelist());
		pipe.setWhitelist(true);
		assertTrue(pipe.isWhitelist());
		pipe.setWhitelist(false);
		assertFalse(pipe.isWhitelist());
	}
}
