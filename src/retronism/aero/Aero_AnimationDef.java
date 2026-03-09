package retronism.aero;

/**
 * Definição imutável de animação para um tipo de máquina.
 * Mapeia IDs de estado (int) para nomes de clips no Aero_AnimBundle.
 *
 * Instância única por tipo de máquina — armazenar como campo static.
 *
 * Uso:
 * <pre>
 *   public static final int STATE_OFF = 0;
 *   public static final int STATE_ON  = 1;
 *
 *   public static final Aero_AnimBundle BUNDLE =
 *       Aero_AnimationLoader.load("/models/MyMachine.anim.json");
 *
 *   public static final Aero_AnimationDef ANIM_DEF = new Aero_AnimationDef()
 *       .state(STATE_OFF, "idle")
 *       .state(STATE_ON,  "spin");
 *
 *   // Por tile entity:
 *   public final Aero_AnimationState animState = ANIM_DEF.createState(BUNDLE);
 * </pre>
 *
 * Convenção: STATE_OFF deve ser 0 (padrão quando NBT não tem chave "Anim_state").
 */
public class Aero_AnimationDef {

    // Array esparso: stateClips[stateId] = nome do clip (null = sem animação)
    private String[] stateClips;

    private static final int INITIAL_CAPACITY = 4;

    public Aero_AnimationDef() {
        stateClips = new String[INITIAL_CAPACITY];
    }

    /**
     * Associa um estado ao clip que deve ser tocado.
     *
     * @param stateId   ID do estado (inteiro >= 0; STATE_OFF deve ser 0)
     * @param clipName  nome do clip no .anim.json (e.g. "spin", "idle")
     */
    public Aero_AnimationDef state(int stateId, String clipName) {
        if (stateId < 0) throw new IllegalArgumentException("stateId must be >= 0");
        if (stateId >= stateClips.length) {
            int newLen = Math.max(stateId + 1, stateClips.length * 2);
            String[] newArr = new String[newLen];
            System.arraycopy(stateClips, 0, newArr, 0, stateClips.length);
            stateClips = newArr;
        }
        stateClips[stateId] = clipName;
        return this;
    }

    /**
     * Retorna o nome do clip associado ao estado, ou null se não definido.
     */
    public String getClipName(int stateId) {
        if (stateId < 0 || stateId >= stateClips.length) return null;
        return stateClips[stateId];
    }

    /**
     * Cria um novo Aero_AnimationState para esta definição, ligado ao bundle.
     * Chamar uma vez por tile entity, no campo de instância.
     */
    public Aero_AnimationState createState(Aero_AnimBundle bundle) {
        return new Aero_AnimationState(this, bundle);
    }
}
