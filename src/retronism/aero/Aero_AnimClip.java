package retronism.aero;

/**
 * Dados imutáveis de um clip de animação.
 *
 * Armazena keyframes de rotação e posição para cada bone (named group OBJ),
 * em arrays paralelos ordenados por tempo crescente.
 *
 * Unidades:
 *   - Tempo: segundos (float)
 *   - Rotation: graus Euler [X, Y, Z] — aplicados na ordem Z→Y→X (compatível Bedrock/GeckoLib)
 *   - Position: pixels Blockbench (divide /16 para block units no renderer)
 */
public class Aero_AnimClip {

    public final String  name;
    public final boolean loop;
    public final float   length;    // duração em segundos

    // Parallel arrays indexados por bone index (0-based, ordem de adição)
    final String[]    boneNames;
    final float[][]   rotTimes;     // rotTimes[bi]    = float[] de timestamps (segundos)
    final float[][][] rotValues;    // rotValues[bi][ki] = float[3] {rx, ry, rz}
    final float[][]   posTimes;
    final float[][][] posValues;    // posValues[bi][ki] = float[3] {px, py, pz}

    Aero_AnimClip(String name, boolean loop, float length,
                  String[] boneNames,
                  float[][] rotTimes, float[][][] rotValues,
                  float[][] posTimes, float[][][] posValues) {
        this.name      = name;
        this.loop      = loop;
        this.length    = length;
        this.boneNames = boneNames;
        this.rotTimes  = rotTimes;
        this.rotValues = rotValues;
        this.posTimes  = posTimes;
        this.posValues = posValues;
    }

    /** Retorna o índice do bone pelo nome, ou -1 se não encontrado. */
    public int indexOfBone(String name) {
        for (int i = 0; i < boneNames.length; i++) {
            if (boneNames[i].equals(name)) return i;
        }
        return -1;
    }

    /**
     * Amostra a rotação do bone em um dado tempo, com interpolação LINEAR.
     * Retorna float[3] {rx, ry, rz} em graus, ou null se o bone não tem keyframes de rotação.
     */
    public float[] sampleRot(int boneIdx, float time) {
        float[] times = rotTimes[boneIdx];
        float[][]  vals  = rotValues[boneIdx];
        if (times == null || times.length == 0) return null;
        return sample(times, vals, time);
    }

    /**
     * Amostra a posição do bone em um dado tempo, com interpolação LINEAR.
     * Retorna float[3] {px, py, pz} em pixels, ou null se o bone não tem keyframes de posição.
     */
    public float[] samplePos(int boneIdx, float time) {
        float[] times = posTimes[boneIdx];
        float[][] vals  = posValues[boneIdx];
        if (times == null || times.length == 0) return null;
        return sample(times, vals, time);
    }

    // -----------------------------------------------------------------------
    // Internos
    // -----------------------------------------------------------------------

    /** Interpolação LINEAR entre keyframes. Clamp fora dos limites. */
    private static float[] sample(float[] times, float[][] vals, float time) {
        int n = times.length;
        if (n == 1) {
            return copy3(vals[0]);
        }
        // Clamp antes do primeiro keyframe
        if (time <= times[0]) return copy3(vals[0]);
        // Clamp depois do último keyframe
        if (time >= times[n - 1]) return copy3(vals[n - 1]);

        // Busca binária do intervalo
        int lo = 0, hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (times[mid] <= time) lo = mid; else hi = mid;
        }

        float t0 = times[lo], t1 = times[hi];
        float alpha = (t1 > t0) ? (time - t0) / (t1 - t0) : 0f;

        float[] a = vals[lo];
        float[] b = vals[hi];
        return new float[]{
            a[0] + (b[0] - a[0]) * alpha,
            a[1] + (b[1] - a[1]) * alpha,
            a[2] + (b[2] - a[2]) * alpha
        };
    }

    private static float[] copy3(float[] src) {
        return new float[]{src[0], src[1], src[2]};
    }
}
