package retronism.aero;

/**
 * AeroMesh Model — container para modelos OBJ triangulados.
 *
 * Triângulos pré-classificados em 4 grupos de brightness (mesmo sistema
 * direcional do Aero_ModelRenderer):
 *
 *   GROUP_TOP    (ny dominante, positivo) → fator 1.0
 *   GROUP_BOTTOM (ny dominante, negativo) → fator 0.5
 *   GROUP_NS     (nz dominante)           → fator 0.8
 *   GROUP_EW     (nx dominante)           → fator 0.6
 *
 * Classificação ocorre no parse (Aero_ObjLoader), não por frame.
 * Isso reduz as chamadas de setColorOpaque_F de O(N triângulos) para 4.
 *
 * Cada triângulo é float[15]:
 *   [0-4]   vértice 0: x, y, z, u, v
 *   [5-9]   vértice 1: x, y, z, u, v
 *   [10-14] vértice 2: x, y, z, u, v
 *
 * A normal não é guardada — foi usada apenas para classificar o grupo.
 */
public class Aero_MeshModel {

    public static final int GROUP_TOP    = 0;
    public static final int GROUP_BOTTOM = 1;
    public static final int GROUP_NS     = 2;
    public static final int GROUP_EW     = 3;

    static final float[] BRIGHTNESS_FACTORS = {1.0f, 0.5f, 0.8f, 0.6f};

    public final String name;
    public final float scale;

    /**
     * Triângulos por grupo de brightness.
     * groups[GROUP_TOP][i] = float[15] do i-ésimo triângulo top-facing.
     */
    public final float[][][] groups;

    public Aero_MeshModel(String name, float[][][] groups, float scale) {
        this.name = name;
        this.groups = groups;
        this.scale = scale;
    }

    /** Construtor padrão: scale=1 (OBJ do Blockbench já exporta em block-units). */
    public Aero_MeshModel(String name, float[][][] groups) {
        this(name, groups, 1.0f);
    }

    /** Total de triângulos somando todos os grupos. */
    public int triangleCount() {
        int n = 0;
        for (int g = 0; g < 4; g++) n += groups[g].length;
        return n;
    }
}
