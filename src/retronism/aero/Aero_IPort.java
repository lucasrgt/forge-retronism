package retronism.aero;

import net.minecraft.src.TileEntity;

/**
 * Interface for blocks that act as Ports of a Multiblock.
 */
public interface Aero_IPort {
    /**
     * Returns the Core TileEntity of this multiblock.
     */
    TileEntity getCore();

    /**
     * Sets the Core of this multiblock.
     */
    void setCore(TileEntity core);

    /**
     * Port type (e.g. "energy", "item", "fluid").
     */
    String getPortType();
}
