package retronism.aero;

/**
 * AeroModel API by AeroCoding.dev
 * Ultra-lightweight 3D model container for Minecraft Beta 1.7.3.
 */
public class Aero_Model {

    public final String name;
    public final float[][] elements;
    public final float textureSize;
    public final float scale;

    /**
     * @param name - Identificador do modelo
     * @param elements - Array de peças [x1, y1, z1, x2, y2, z2, u1_down, v1_down, u2_down, v2_down, ...]
     * @param textureSize - Tamanho da textura (ex: 128.0f)
     * @param scale - Escala do modelo (ex: 16.0f para 1 bloco = 16 unidades)
     */
    public Aero_Model(String name, float[][] elements, float textureSize, float scale) {
        this.name = name;
        this.elements = elements;
        this.textureSize = textureSize;
        this.scale = scale;
    }

    public Aero_Model(String name, float[][] elements) {
        this(name, elements, 128.0f, 16.0f);
    }
}
