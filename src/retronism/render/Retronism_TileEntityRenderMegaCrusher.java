package retronism.render;

import net.minecraft.src.*;
import net.minecraft.client.Minecraft;
import retronism.tile.Retronism_TileMegaCrusher;
import aero.modellib.Aero_MeshModel;
import aero.modellib.Aero_MeshRenderer;
import aero.modellib.Aero_ObjLoader;

public class Retronism_TileEntityRenderMegaCrusher extends TileEntitySpecialRenderer {

    public static final Aero_MeshModel MODEL = Aero_ObjLoader.load("/models/MegaCrusher.obj");

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
        bindTextureByName("/models/retronism_megacrusher.png");

        // Reset GL color to avoid tinting from other renders
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        World w = tile.worldObj;
        int ox = tile.originX, oy = tile.originY, oz = tile.originZ;

        float brightness;
        if (Minecraft.isAmbientOcclusionEnabled()) {
            // Smooth: média de 9 pontos em grid 3x3 acima da estrutura
            // → respeita sombras parciais (árvore, parede ao lado)
            float sum = 0;
            for (int dx = 0; dx <= 2; dx++)
                for (int dz = 0; dz <= 2; dz++)
                    sum += w.getLightBrightness(ox + dx, oy + 3, oz + dz);
            brightness = sum / 9f;
        } else {
            // Flat: máximo de 4 cantos — comportamento clássico do Minecraft
            brightness = Math.max(
                Math.max(w.getLightBrightness(ox + 1, oy + 3, oz - 1),
                         w.getLightBrightness(ox + 1, oy + 3, oz + 3)),
                Math.max(w.getLightBrightness(ox - 1, oy + 3, oz + 1),
                         w.getLightBrightness(ox + 3, oy + 3, oz + 1))
            );
        }
        Aero_MeshRenderer.renderAnimated(MODEL,
            Retronism_TileMegaCrusher.BUNDLE,
            Retronism_TileMegaCrusher.ANIM_DEF,
            tile.animState,
            d + offsetX, d1 + offsetY, d2 + offsetZ,
            brightness, f);
    }
}
