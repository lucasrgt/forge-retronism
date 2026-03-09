package retronism.aero;

import java.util.Map;

/**
 * Container com todos os dados de animação carregados de um arquivo .anim.json:
 *   - clips: Map<String, Aero_AnimClip>  — clips nomeados
 *   - pivots: Map<String, float[]>       — pivot de cada bone em block units (pixels / 16)
 *
 * Instância imutável, segura para armazenar como campo static.
 * Criada por Aero_AnimationLoader.load().
 */
public class Aero_AnimBundle {

    /** Map<String, Aero_AnimClip> — clips indexados por nome. */
    public final Map clips;

    /**
     * Map<String, float[]> — pivot de cada bone em block units (pixels / 16).
     * Ausente = pivot [0, 0, 0].
     */
    public final Map pivots;

    Aero_AnimBundle(Map clips, Map pivots) {
        this.clips  = clips;
        this.pivots = pivots;
    }

    /**
     * Retorna o clip pelo nome, ou null se não existir.
     * Exemplo: bundle.getClip("spin")
     */
    public Aero_AnimClip getClip(String name) {
        if (name == null) return null;
        return (Aero_AnimClip) clips.get(name);
    }

    /**
     * Retorna o pivot do bone em block units, ou float[]{0,0,0} se não definido.
     * Valores já divididos por 16 (converter pixels Blockbench → block units).
     */
    public float[] getPivot(String boneName) {
        float[] p = (float[]) pivots.get(boneName);
        return p != null ? p : new float[]{0f, 0f, 0f};
    }
}
