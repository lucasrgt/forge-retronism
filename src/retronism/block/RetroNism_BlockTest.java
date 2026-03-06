package retronism.block;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.tile.*;

public class Retronism_BlockTest extends Block {

	public Retronism_BlockTest(int id, int textureIndex) {
		super(id, textureIndex, Material.iron);
	}

	public String getModName() { return "Retronism"; }

	public int quantityDropped(java.util.Random random) {
		return 1;
	}

	public int idDropped(int metadata, java.util.Random random) {
		return this.blockID;
	}
}
