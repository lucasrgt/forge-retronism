package retronism.block;

import net.minecraft.src.*;

public class Retronism_BlockMegaElectrolysisCasing extends Block {
    public Retronism_BlockMegaElectrolysisCasing(int id, int tex) {
        super(id, tex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
        setBlockName("megaelectrolysisCasing");
    }
}
