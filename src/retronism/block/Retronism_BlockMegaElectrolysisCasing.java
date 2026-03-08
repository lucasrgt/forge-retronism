package retronism.block;

import net.minecraft.src.*;
import retronism.*;

public class Retronism_BlockMegaElectrolysisCasing extends Block {
	public Retronism_BlockMegaElectrolysisCasing(int id, int tex) {
		super(id, tex, Material.iron);
		setHardness(3.5F);
		setResistance(5.0F);
		setStepSound(Block.soundMetalFootstep);
		setBlockName("retroNismMegaElectrolysisCasing");
	}

	public int getRenderType() {
		return mod_Retronism.megaElectrolysisCasingRenderID;
	}

	public boolean renderAsNormalBlock() { return false; }
	public boolean isOpaqueCube() { return false; }
}
