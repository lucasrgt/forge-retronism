package retronism.block;

import net.minecraft.src.*;
import retronism.*;
import aero.machineapi.*;
import retronism.tile.*;
import retronism.gui.*;

public class Retronism_BlockTest extends Block {

	public Retronism_BlockTest(int id, int textureIndex) {
		super(id, textureIndex, Material.iron);
	}

	public boolean isOpaqueCube() { return false; }
	public boolean renderAsNormalBlock() { return false; }
	public int getRenderType() { return mod_Retronism.megaCrusherRenderID; }

	public String getModName() { return "Retronism"; }

	public int quantityDropped(java.util.Random random) {
		return 1;
	}

	public int idDropped(int metadata, java.util.Random random) {
		return this.blockID;
	}

	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer player) {
		if (player.isSneaking()) return false;
		if (world.multiplayerWorld) return true;

		// Check if already part of a formed multiblock
		Retronism_TileMegaCrusher core = findNearbyCore(world, x, y, z);
		if (core != null) {
			ModLoader.OpenGUI(player, new Retronism_GuiMegaCrusher(player.inventory, core));
			return true;
		}

		// Try to form a new multiblock
		if (tryFormMultiblock(world, x, y, z)) {
			Retronism_TileMegaCrusher newCore = (Retronism_TileMegaCrusher) world.getBlockTileEntity(x, y, z);
			if (newCore != null) {
				ModLoader.OpenGUI(player, new Retronism_GuiMegaCrusher(player.inventory, newCore));
			}
			return true;
		}

		return false;
	}

	private Retronism_TileMegaCrusher findNearbyCore(World world, int x, int y, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					int bx = x + dx, by = y + dy, bz = z + dz;
					if (world.getBlockId(bx, by, bz) == Retronism_Registry.megaCrusherCoreBlock.blockID) {
						TileEntity te = world.getBlockTileEntity(bx, by, bz);
						if (te instanceof Retronism_TileMegaCrusher) {
							Retronism_TileMegaCrusher core = (Retronism_TileMegaCrusher) te;
							if (x >= core.originX && x <= core.originX + 2
								&& y >= core.originY && y <= core.originY + 2
								&& z >= core.originZ && z <= core.originZ + 2) {
								return core;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private boolean tryFormMultiblock(World world, int x, int y, int z) {
		for (int ox = x - 2; ox <= x; ox++) {
			for (int oy = y - 2; oy <= y; oy++) {
				for (int oz = z - 2; oz <= z; oz++) {
					if (isValidStructure(world, ox, oy, oz)) {
						world.setBlockWithNotify(x, y, z, Retronism_Registry.megaCrusherCoreBlock.blockID);
						TileEntity te = world.getBlockTileEntity(x, y, z);
						if (te instanceof Retronism_TileMegaCrusher) {
							Retronism_TileMegaCrusher core = (Retronism_TileMegaCrusher) te;
							core.originX = ox;
							core.originY = oy;
							core.originZ = oz;
						}
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isValidStructure(World world, int ox, int oy, int oz) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 3; k++) {
					int bx = ox + i, by = oy + j, bz = oz + k;
					int bid = world.getBlockId(bx, by, bz);
					if (i == 1 && j == 1 && k == 1) {
						if (bid != 0) return false;
					} else {
						// Aceita tanto o bloco base quanto portos durante a montagem!
						if (bid == Retronism_Registry.testBlock.blockID || bid == Retronism_Registry.megaCrusherPortBlock.blockID) {
							continue;
						}
						return false;
					}
				}
			}
		}
		return true;
	}
}
