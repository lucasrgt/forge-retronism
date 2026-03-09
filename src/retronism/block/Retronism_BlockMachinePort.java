package retronism.block;

import net.minecraft.src.*;
import java.util.Random;
import retronism.mod_Retronism;

/**
 * Machine Port block — generic port, metadata set by controller on formation.
 * Metadata: 0 = energy (yellow), 1 = fluid (blue), 2 = gas (green)
 */
public class Retronism_BlockMachinePort extends Block {

    public int texEnergy;
    public int texFluid;
    public int texGas;

    public Retronism_BlockMachinePort(int id, int defaultTex) {
        super(id, defaultTex, Material.iron);
        setHardness(3.5F);
        setResistance(5.0F);
        setStepSound(Block.soundMetalFootstep);
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int metadata) {
        switch (metadata) {
            case 0: return texEnergy;
            case 1: return texFluid;
            case 2: return texGas;
            default: return texEnergy;
        }
    }

    @Override
    public int idDropped(int metadata, Random random) {
        return blockID;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return mod_Retronism.machinePortRenderID;
    }

    @Override
    protected int damageDropped(int metadata) {
        return 0; // Always drops as base item (no variants)
    }
}
