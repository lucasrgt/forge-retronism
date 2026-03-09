package retronism.render;

import net.minecraft.src.*;
import retronism.tile.Retronism_TileMegaCrusher;
import retronism.aero.Aero_Model;
import retronism.aero.Aero_ModelRenderer;

public class Retronism_TileEntityRenderMegaCrusher extends TileEntitySpecialRenderer {

    // Now using AeroModel API!
    public static final Aero_Model MODEL = Retronism_MegaCrusherModel.MODEL;

    public void renderTileEntityAt(TileEntity tileEntity, double d, double d1, double d2, float f) {
        Retronism_TileMegaCrusher tile = (Retronism_TileMegaCrusher) tileEntity;
        
        if (!tile.validateStructure()) {
            return;
        }
        
        // Calculate origin
        double offsetX = tile.originX - tile.xCoord;
        double offsetY = tile.originY - tile.yCoord;
        double offsetZ = tile.originZ - tile.zCoord;
        
        // Bind high-res texture
        bindTextureByName("/block/retronism_megacrusher_hq.png");

        // Single call to AeroModel API! (rotation = 0 for now)
        float brightness = tile.getBlockType().getBlockBrightness(tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord);
        Aero_ModelRenderer.renderModel(MODEL, d + offsetX, d1 + offsetY, d2 + offsetZ, 0, brightness);
    }
}
