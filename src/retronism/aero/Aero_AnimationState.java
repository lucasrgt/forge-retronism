package retronism.aero;

import net.minecraft.src.NBTTagCompound;

/**
 * Estado mutável de animação por tile entity.
 *
 * Rastreia qual clip está tocando, o tempo de playback atual e anterior
 * (para interpolação partial-tick), e persiste via NBT.
 *
 * Ciclo de vida:
 * <pre>
 *   // Campo da TileEntity:
 *   public final Aero_AnimationState animState = ANIM_DEF.createState(BUNDLE);
 *
 *   // Em updateEntity() — tick() ANTES de setState():
 *   animState.tick();
 *   animState.setState(isRunning ? STATE_ON : STATE_OFF);
 *
 *   // Em writeToNBT / readFromNBT:
 *   animState.writeToNBT(nbt);
 *   animState.readFromNBT(nbt);
 * </pre>
 */
public class Aero_AnimationState {

    /** Estado atual (público para o renderer e lógica da máquina). */
    public int currentState;

    private final Aero_AnimationDef def;
    private final Aero_AnimBundle   bundle;

    private float playbackTime;      // segundos, tempo atual no clip
    private float prevPlaybackTime;  // segundos, tempo no tick anterior (para interpolação)

    /** Construído por Aero_AnimationDef.createState(). */
    Aero_AnimationState(Aero_AnimationDef def, Aero_AnimBundle bundle) {
        this.def          = def;
        this.bundle       = bundle;
        this.currentState = 0;
        this.playbackTime = 0f;
        this.prevPlaybackTime = 0f;
    }

    // -----------------------------------------------------------------------
    // Tick — chamar no início de updateEntity()
    // -----------------------------------------------------------------------

    /**
     * Avança o playback em 1 tick (1/20 segundo).
     * Salva o tempo anterior para interpolação partial-tick.
     * Deve ser chamado ANTES de setState() a cada tick.
     */
    public void tick() {
        prevPlaybackTime = playbackTime;

        Aero_AnimClip clip = getCurrentClip();
        if (clip == null || clip.length <= 0f) {
            playbackTime = 0f;
            return;
        }

        playbackTime += 1f / 20f;

        if (clip.loop) {
            // Wrap no final do clip
            if (playbackTime >= clip.length) {
                playbackTime = playbackTime % clip.length;
                // Corrige prevPlaybackTime para a interpolação não saltar
                if (prevPlaybackTime >= clip.length) prevPlaybackTime = prevPlaybackTime % clip.length;
            }
        } else {
            // Clamp no final
            if (playbackTime >= clip.length) {
                playbackTime     = clip.length;
                prevPlaybackTime = clip.length;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Mudar estado
    // -----------------------------------------------------------------------

    /**
     * Muda o estado atual. Se o clip associado ao novo estado for diferente
     * do clip atual, o playback é resetado para o início.
     * Deve ser chamado APÓS tick().
     */
    public void setState(int stateId) {
        if (stateId == currentState) return;

        String oldClip = def.getClipName(currentState);
        String newClip = def.getClipName(stateId);

        currentState = stateId;

        // Reseta o tempo apenas se o clip mudar (ou se não havia clip antes)
        boolean clipChanged = (newClip == null) ? (oldClip != null)
                                                : !newClip.equals(oldClip);
        if (clipChanged) {
            playbackTime     = 0f;
            prevPlaybackTime = 0f;
        }
    }

    // -----------------------------------------------------------------------
    // Acesso pelo renderer
    // -----------------------------------------------------------------------

    /**
     * Retorna o tempo de playback interpolado para o frame atual.
     * Lida com wrap de loop: quando playbackTime < prevPlaybackTime (cruzou o final),
     * interpola corretamente sem saltar para trás.
     *
     * @param partialTick  fração do tick (0.0–1.0) fornecida pelo TileEntitySpecialRenderer
     */
    public float getInterpolatedTime(float partialTick) {
        Aero_AnimClip clip = getCurrentClip();
        if (clip == null || clip.length <= 0f) return 0f;

        float cur  = playbackTime;
        float prev = prevPlaybackTime;

        if (clip.loop && cur < prev) {
            // Cruzou o boundary de loop — interpola "por cima" do wrap
            cur += clip.length;
            float t = prev + (cur - prev) * partialTick;
            return t % clip.length;
        }

        return prev + (cur - prev) * partialTick;
    }

    /** Retorna o clip atualmente ativo, ou null se o estado não tiver clip definido. */
    public Aero_AnimClip getCurrentClip() {
        String clipName = def.getClipName(currentState);
        if (clipName == null) return null;
        return bundle.getClip(clipName);
    }

    /** Expõe o bundle para o renderer acessar pivots e clips. */
    public Aero_AnimBundle getBundle() { return bundle; }

    /** Expõe a def para o renderer acessar nomes de clips. */
    public Aero_AnimationDef getDef() { return def; }

    // -----------------------------------------------------------------------
    // NBT
    // -----------------------------------------------------------------------

    /**
     * Persiste estado e tempo de playback.
     * Keys: "Anim_state", "Anim_time"
     */
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Anim_state", currentState);
        nbt.setFloat("Anim_time", playbackTime);
    }

    /**
     * Restaura estado e tempo do NBT.
     * prevPlaybackTime = playbackTime para evitar artefato no primeiro frame após load.
     * Se as keys estiverem ausentes (save antigo), usa defaults (state=0, time=0).
     */
    public void readFromNBT(NBTTagCompound nbt) {
        currentState      = nbt.getInteger("Anim_state");   // 0 se ausente
        playbackTime      = nbt.hasKey("Anim_time") ? nbt.getFloat("Anim_time") : 0f;
        prevPlaybackTime  = playbackTime;
    }
}
