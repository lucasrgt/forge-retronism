package net.minecraft.src;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PortRegistryTest {

	// Test coordinates
	private static final int PX = 100, PY = 64, PZ = 200;
	private static final int CX = 50, CY = 60, CZ = 70;

	@Before
	public void setUp() {
		// Clean up static state between tests
		Aero_PortRegistry.unregisterPort(PX, PY, PZ);
		Aero_PortRegistry.unregisterPort(1, 2, 3);
		Aero_PortRegistry.unregisterPort(4, 5, 6);
		Aero_PortRegistry.unregisterPort(7, 8, 9);
		Aero_PortRegistry.unregisterPort(10, 11, 12);
		Aero_PortRegistry.unregisterAllForController(CX, CY, CZ);
		Aero_PortRegistry.unregisterAllForController(99, 99, 99);
	}

	// ==================== Register and Retrieve ====================

	@Test
	public void testRegisterAndGetPort() {
		// Arrange
		int type = Aero_PortRegistry.PORT_TYPE_ENERGY;
		int mode = Aero_PortRegistry.PORT_MODE_INPUT;

		// Act
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ, type, mode, 45);
		int[] info = Aero_PortRegistry.getPort(PX, PY, PZ);

		// Assert
		assertNotNull(info);
		assertEquals(6, info.length);
		assertEquals(CX, info[0]);
		assertEquals(CY, info[1]);
		assertEquals(CZ, info[2]);
		assertEquals(type, info[3]);
		assertEquals(mode, info[4]);
		assertEquals(45, info[5]);
	}

	@Test
	public void testRegisterShortOverloadDefaultBlockId() {
		// Arrange & Act - use short overload (no originalBlockId)
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.PORT_MODE_OUTPUT);

		// Assert - default originalBlockId should be 42
		assertEquals(42, Aero_PortRegistry.getOriginalBlockId(PX, PY, PZ));
	}

	// ==================== isPort ====================

	@Test
	public void testIsPortRegistered() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT);

		// Act & Assert
		assertTrue(Aero_PortRegistry.isPort(PX, PY, PZ));
	}

	@Test
	public void testIsPortUnregistered() {
		// Arrange - nothing registered

		// Act & Assert
		assertFalse(Aero_PortRegistry.isPort(999, 999, 999));
	}

	// ==================== isPortOfType ====================

	@Test
	public void testIsPortOfTypeCorrect() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_GAS, Aero_PortRegistry.PORT_MODE_INPUT);

		// Act & Assert
		assertTrue(Aero_PortRegistry.isPortOfType(PX, PY, PZ, Aero_PortRegistry.PORT_TYPE_GAS));
	}

	@Test
	public void testIsPortOfTypeWrong() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_GAS, Aero_PortRegistry.PORT_MODE_INPUT);

		// Act & Assert
		assertFalse(Aero_PortRegistry.isPortOfType(PX, PY, PZ, Aero_PortRegistry.PORT_TYPE_ENERGY));
	}

	@Test
	public void testIsPortOfTypeNonExistent() {
		// Arrange - nothing registered

		// Act & Assert
		assertFalse(Aero_PortRegistry.isPortOfType(999, 999, 999, Aero_PortRegistry.PORT_TYPE_FLUID));
	}

	// ==================== getPortType / getPortMode ====================

	@Test
	public void testGetPortType() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.PORT_MODE_OUTPUT);

		// Act & Assert
		assertEquals(Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.getPortType(PX, PY, PZ));
	}

	@Test
	public void testGetPortMode() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_OUTPUT);

		// Act & Assert
		assertEquals(Aero_PortRegistry.PORT_MODE_OUTPUT, Aero_PortRegistry.getPortMode(PX, PY, PZ));
	}

	// ==================== getOriginalBlockId ====================

	@Test
	public void testGetOriginalBlockIdExplicit() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT, 98);

		// Act & Assert
		assertEquals(98, Aero_PortRegistry.getOriginalBlockId(PX, PY, PZ));
	}

	@Test
	public void testGetOriginalBlockIdNonExistent() {
		// Arrange - nothing registered

		// Act & Assert - returns default 42 for non-existent
		assertEquals(42, Aero_PortRegistry.getOriginalBlockId(999, 999, 999));
	}

	// ==================== getControllerPos ====================

	@Test
	public void testGetControllerPos() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT);

		// Act
		int[] ctrl = Aero_PortRegistry.getControllerPos(PX, PY, PZ);

		// Assert
		assertNotNull(ctrl);
		assertEquals(3, ctrl.length);
		assertEquals(CX, ctrl[0]);
		assertEquals(CY, ctrl[1]);
		assertEquals(CZ, ctrl[2]);
	}

	@Test
	public void testGetControllerPosNonExistent() {
		// Arrange - nothing registered

		// Act & Assert
		assertNull(Aero_PortRegistry.getControllerPos(999, 999, 999));
	}

	// ==================== unregisterPort ====================

	@Test
	public void testUnregisterPort() {
		// Arrange
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT);
		assertTrue(Aero_PortRegistry.isPort(PX, PY, PZ));

		// Act
		Aero_PortRegistry.unregisterPort(PX, PY, PZ);

		// Assert
		assertFalse(Aero_PortRegistry.isPort(PX, PY, PZ));
		assertNull(Aero_PortRegistry.getPort(PX, PY, PZ));
	}

	// ==================== unregisterAllForController ====================

	@Test
	public void testUnregisterAllForController() {
		// Arrange - 3 ports for same controller
		Aero_PortRegistry.registerPort(1, 2, 3, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT);
		Aero_PortRegistry.registerPort(4, 5, 6, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.PORT_MODE_OUTPUT);
		Aero_PortRegistry.registerPort(7, 8, 9, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_GAS, Aero_PortRegistry.PORT_MODE_INPUT);

		// 1 port for a DIFFERENT controller
		Aero_PortRegistry.registerPort(10, 11, 12, 99, 99, 99,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_OUTPUT);

		// Act
		Aero_PortRegistry.unregisterAllForController(CX, CY, CZ);

		// Assert - all ports of the target controller are gone
		assertFalse(Aero_PortRegistry.isPort(1, 2, 3));
		assertFalse(Aero_PortRegistry.isPort(4, 5, 6));
		assertFalse(Aero_PortRegistry.isPort(7, 8, 9));

		// Assert - other controller's port is untouched
		assertTrue(Aero_PortRegistry.isPort(10, 11, 12));
	}

	// ==================== Multiple ports for same controller ====================

	@Test
	public void testMultiplePortsSameController() {
		// Arrange - register 3 ports with different types to same controller
		Aero_PortRegistry.registerPort(1, 2, 3, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT);
		Aero_PortRegistry.registerPort(4, 5, 6, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.PORT_MODE_OUTPUT);

		// Act & Assert - each port has its own type/mode
		assertEquals(Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.getPortType(1, 2, 3));
		assertEquals(Aero_PortRegistry.PORT_MODE_INPUT, Aero_PortRegistry.getPortMode(1, 2, 3));

		assertEquals(Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.getPortType(4, 5, 6));
		assertEquals(Aero_PortRegistry.PORT_MODE_OUTPUT, Aero_PortRegistry.getPortMode(4, 5, 6));

		// Both point to same controller
		int[] ctrl1 = Aero_PortRegistry.getControllerPos(1, 2, 3);
		int[] ctrl2 = Aero_PortRegistry.getControllerPos(4, 5, 6);
		assertArrayEquals(ctrl1, ctrl2);
	}

	// ==================== Overwrite port at same position ====================

	@Test
	public void testOverwritePortAtSamePosition() {
		// Arrange - register energy input port
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_ENERGY, Aero_PortRegistry.PORT_MODE_INPUT, 10);

		// Act - overwrite with fluid output port and different block id
		Aero_PortRegistry.registerPort(PX, PY, PZ, CX, CY, CZ,
			Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.PORT_MODE_OUTPUT, 20);

		// Assert - new values replace old
		assertEquals(Aero_PortRegistry.PORT_TYPE_FLUID, Aero_PortRegistry.getPortType(PX, PY, PZ));
		assertEquals(Aero_PortRegistry.PORT_MODE_OUTPUT, Aero_PortRegistry.getPortMode(PX, PY, PZ));
		assertEquals(20, Aero_PortRegistry.getOriginalBlockId(PX, PY, PZ));
	}

	// ==================== Querying non-existent port ====================

	@Test
	public void testGetPortNonExistent() {
		// Arrange - nothing registered

		// Act & Assert
		assertNull(Aero_PortRegistry.getPort(999, 999, 999));
		assertEquals(0, Aero_PortRegistry.getPortType(999, 999, 999));
		assertEquals(0, Aero_PortRegistry.getPortMode(999, 999, 999));
		assertFalse(Aero_PortRegistry.isPort(999, 999, 999));
	}

	// ==================== Constants ====================

	@Test
	public void testConstants() {
		// Assert - verify constant values match documented API
		assertEquals(1, Aero_PortRegistry.PORT_TYPE_ENERGY);
		assertEquals(2, Aero_PortRegistry.PORT_TYPE_FLUID);
		assertEquals(3, Aero_PortRegistry.PORT_TYPE_GAS);
		assertEquals(1, Aero_PortRegistry.PORT_MODE_INPUT);
		assertEquals(2, Aero_PortRegistry.PORT_MODE_OUTPUT);
	}
}
