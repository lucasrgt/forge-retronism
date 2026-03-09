package retronism.aero;

import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;

/**
 * AeroMesh Renderer by AeroCoding.dev
 * Renderiza modelos OBJ (Aero_MeshModel) usando GL_TRIANGLES.
 *
 * Performance:
 *   - Triângulos pré-classificados em 4 grupos de brightness no parse
 *   - setColorOpaque_F chamado 4× por frame (vs N× na versão ingênua)
 *   - Preparado para Display Lists: cada grupo pode virar um glCallList
 *
 * Uso em TileEntitySpecialRenderer:
 *   Aero_MeshRenderer.renderModel(MODEL, d + ox, d1 + oy, d2 + oz, rotation, brightness);
 *
 * Uso em renderInventory:
 *   Aero_MeshRenderer.renderInventory(rb, MODEL);
 *
 * NOTA: usa Tessellator com GL_TRIANGLES — só chamar fora de um bloco
 * startDrawingQuads() ativo. O contexto de TileEntitySpecialRenderer é seguro.
 */
public class Aero_MeshRenderer {

    /**
     * Renderiza com flat lighting (brilho uniforme por grupo de face).
     * @param brightness brilho base (0.0~1.0), de getLightBrightness()
     */
    public static void renderModel(Aero_MeshModel model, double x, double y, double z, float rotation, float brightness) {
        Tessellator tess = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        applyRotation(rotation);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        float sc = model.scale;
        tess.startDrawing(GL11.GL_TRIANGLES);
        for (int g = 0; g < 4; g++) {
            float[][] tris = model.groups[g];
            if (tris.length == 0) continue;
            float bright = brightness * Aero_MeshModel.BRIGHTNESS_FACTORS[g];
            tess.setColorOpaque_F(bright, bright, bright);
            for (int i = 0; i < tris.length; i++) {
                float[] t = tris[i];
                tess.addVertexWithUV(t[0]/sc, t[1]/sc, t[2]/sc, t[3], t[4]);
                tess.addVertexWithUV(t[5]/sc, t[6]/sc, t[7]/sc, t[8], t[9]);
                tess.addVertexWithUV(t[10]/sc, t[11]/sc, t[12]/sc, t[13], t[14]);
            }
        }
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    /**
     * Renderiza com smooth lighting (Ambient Occlusion).
     * Cada vértice recebe o brilho do bloco do mundo na sua posição,
     * e o OpenGL interpola entre vértices — igual ao AO nativo do Minecraft.
     *
     * @param world  mundo atual
     * @param ox,oy,oz  canto mínimo da estrutura no mundo (originX/Y/Z)
     */
    /**
     * @param topY  Y acima da estrutura para amostrar luz (evita blocos sólidos).
     *              Ex.: oy + 3 para estrutura de 3 blocos de altura.
     */
    public static void renderModel(Aero_MeshModel model, double x, double y, double z, float rotation,
                                    World world, int ox, int topY, int oz) {
        Tessellator tess = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        applyRotation(rotation);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        float sc = model.scale;
        tess.startDrawing(GL11.GL_TRIANGLES);
        for (int g = 0; g < 4; g++) {
            float[][] tris = model.groups[g];
            if (tris.length == 0) continue;
            float factor = Aero_MeshModel.BRIGHTNESS_FACTORS[g];
            for (int i = 0; i < tris.length; i++) {
                float[] t = tris[i];
                // Centroide do triângulo em XZ — amostra bilinear no plano horizontal
                // fixando Y acima da estrutura (evita blocos sólidos + sem gradiente vertical)
                float wx = ox + (t[0] + t[5] + t[10]) / (3.0f * sc);
                float wz = oz + (t[2] + t[7] + t[12]) / (3.0f * sc);
                int x0 = (int) Math.floor(wx); int x1 = x0 + 1;
                int z0 = (int) Math.floor(wz); int z1 = z0 + 1;
                float tx = wx - x0, tz = wz - z0;
                float b00 = world.getLightBrightness(x0, topY, z0);
                float b10 = world.getLightBrightness(x1, topY, z0);
                float b01 = world.getLightBrightness(x0, topY, z1);
                float b11 = world.getLightBrightness(x1, topY, z1);
                float bright = lerp(lerp(b00, b10, tx), lerp(b01, b11, tx), tz) * factor;
                tess.setColorOpaque_F(bright, bright, bright);
                tess.addVertexWithUV(t[0]/sc, t[1]/sc, t[2]/sc, t[3], t[4]);
                tess.addVertexWithUV(t[5]/sc, t[6]/sc, t[7]/sc, t[8], t[9]);
                tess.addVertexWithUV(t[10]/sc, t[11]/sc, t[12]/sc, t[13], t[14]);
            }
        }
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPopMatrix();
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static void applyRotation(float rotation) {
        if (rotation != 0) {
            GL11.glTranslatef(0.5f, 0.5f, 0.5f);
            GL11.glRotatef(rotation, 0.0f, 1.0f, 0.0f);
            GL11.glTranslatef(-0.5f, -0.5f, -0.5f);
        }
    }

    /**
     * Renderiza miniatura do modelo no inventário.
     * Auto-escala e centraliza com rotação isométrica clássica.
     */
    public static void renderInventory(RenderBlocks rb, Aero_MeshModel model) {
        GL11.glPushMatrix();

        float sc = model.scale;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int g = 0; g < 4; g++) {
            float[][] tris = model.groups[g];
            for (int i = 0; i < tris.length; i++) {
                float[] t = tris[i];
                for (int v = 0; v < 3; v++) {
                    int b = v * 5;
                    float px = t[b]/sc, py = t[b+1]/sc, pz = t[b+2]/sc;
                    if (px < minX) minX = px; if (px > maxX) maxX = px;
                    if (py < minY) minY = py; if (py > maxY) maxY = py;
                    if (pz < minZ) minZ = pz; if (pz > maxZ) maxZ = pz;
                }
            }
        }

        float maxDim = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        
        // 2. Escala e Rotação Isométrica
        float scale = 0.7f / maxDim;
        GL11.glScalef(scale, scale, scale);
        
        GL11.glRotatef(30.0f, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(45.0f, 0.0f, 1.0f, 0.0f);
        
        GL11.glEnable(32826); // GL_RESCALE_NORMAL_EXT

        float cx = (minX + maxX) / 2.0f;
        float cy = (minY + maxY) / 2.0f;
        float cz = (minZ + maxZ) / 2.0f;
        
        renderModel(model, -cx, -cy, -cz, 0, 1.0f);

        GL11.glPopMatrix();
    }
}
