package retronism.block;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.tile.*;

import java.util.Random;

public class RetroNism_BlockPump extends BlockContainer {

	public RetroNism_BlockPump(int id, int textureIndex) {
		super(id, Material.iron);
		this.blockIndexInTexture = textureIndex;
	}

	protected TileEntity getBlockEntity() {
		return new RetroNism_TilePump();
	}

	public int idDropped(int metadata, Random random) {
		return this.blockID;
	}

	public int getBlockTextureFromSide(int side) {
		if (side == 0) return this.blockIndexInTexture + 17;
		if (side == 1) return this.blockIndexInTexture + 17;
		return this.blockIndexInTexture;
	}
}
