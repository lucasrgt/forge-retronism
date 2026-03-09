package retronism.aero;

import net.minecraft.src.TileEntity;

/**
 * Interface para blocos que agem como Portos de uma Multiblock.
 */
public interface Aero_IPort {
    /**
     * Retorna o TileEntity do Core desta multiblock.
     */
    TileEntity getCore();

    /**
     * Define o Core desta multiblock.
     */
    void setCore(TileEntity core);

    /**
     * Tipo do porto (ex: "energy", "item", "fluid").
     */
    String getPortType();
}
