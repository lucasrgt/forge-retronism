package retronism.block;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.tile.*;

public class RetroNism_BlockTest extends Block {

	public RetroNism_BlockTest(int id, int textureIndex) {
		super(id, textureIndex, Material.iron);
	}

	public String getModName() { return "RetroNism"; }

	public int quantityDropped(java.util.Random random) {
		return 1;
	}

	public int idDropped(int metadata, java.util.Random random) {
		return this.blockID;
	}
}
