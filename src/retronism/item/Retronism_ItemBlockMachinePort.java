package retronism.item;

import net.minecraft.src.*;

public class Retronism_ItemBlockMachinePort extends ItemBlock {

    public Retronism_ItemBlockMachinePort(int blockId) {
        super(blockId);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public int getPlacedBlockMetadata(int damage) {
        return damage;
    }
}
