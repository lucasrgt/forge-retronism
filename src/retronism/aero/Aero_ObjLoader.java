package retronism.aero;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AeroMesh OBJ Loader by AeroCoding.dev
 * Carrega modelos OBJ do classpath em runtime — sem pipeline de conversão.
 *
 * Uso:
 *   Aero_MeshModel model = Aero_ObjLoader.load("/models/my_machine.obj");
 *
 * Exporte pelo Blockbench: File > Export > Export OBJ Model
 * Coloque apenas o .obj em src/retronism/assets/models/ (o .mtl não é usado).
 *
 * Suporte:
 *   - v  (vértices), vt (UVs), vn (ignorado — normal calculada da geometria)
 *   - f  (faces: triângulos e quads, triangulação por fan)
 *   - Índices negativos OBJ (referência do final da lista)
 *   - g, o, usemtl, mtllib, s → ignorados
 *
 * UV: aplica flip V (1-V) — OBJ usa V=0 embaixo, Minecraft usa V=0 no topo.
 *
 * Triângulos são classificados em 4 grupos de brightness ao parsear
 * (veja Aero_MeshModel.GROUP_*), evitando cálculo de normal por frame.
 */
public class Aero_ObjLoader {

    private static final Map cache = new HashMap();

    /** Carrega e cacheia um modelo OBJ do classpath. */
    public static Aero_MeshModel load(String resourcePath) {
        return load(resourcePath, resourcePath);
    }

    /** Carrega e cacheia um modelo OBJ do classpath com nome explícito. */
    public static Aero_MeshModel load(String resourcePath, String name) {
        if (cache.containsKey(resourcePath)) {
            return (Aero_MeshModel) cache.get(resourcePath);
        }
        try {
            InputStream is = Aero_ObjLoader.class.getResourceAsStream(resourcePath);
            if (is == null) {
                throw new RuntimeException("AeroObjLoader: resource not found: " + resourcePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            Aero_MeshModel model = parseObj(reader, name);
            is.close();
            cache.put(resourcePath, model);
            return model;
        } catch (Exception e) {
            throw new RuntimeException("AeroObjLoader: failed to load " + resourcePath + ": " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Parser OBJ
    // -----------------------------------------------------------------------

    private static Aero_MeshModel parseObj(BufferedReader reader, String name) throws Exception {
        List verts = new ArrayList();  // float[3]: x, y, z
        List uvs   = new ArrayList();  // float[2]: u, v  (já com flip V)

        // 4 listas — uma por grupo de brightness (TOP, BOTTOM, NS, EW)
        List[] groups = new List[4];
        for (int g = 0; g < 4; g++) groups[g] = new ArrayList();

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;

            if (line.startsWith("v ")) {
                String[] p = split(line.substring(2));
                verts.add(new float[]{f(p[0]), f(p[1]), f(p[2])});

            } else if (line.startsWith("vt ")) {
                String[] p = split(line.substring(3));
                // Flip V: OBJ usa V=0 embaixo, Minecraft usa V=0 no topo
                uvs.add(new float[]{f(p[0]), 1.0f - f(p[1])});

            } else if (line.startsWith("f ")) {
                parseFace(line.substring(2).trim(), verts, uvs, groups);
            }
            // vn, g, o, s, usemtl, mtllib → ignorar
        }

        if (allEmpty(groups)) {
            throw new RuntimeException("AeroObjLoader: no faces found in " + name);
        }

        float[][][] groupArrays = new float[4][][];
        for (int g = 0; g < 4; g++) {
            groupArrays[g] = (float[][]) groups[g].toArray(new float[groups[g].size()][]);
        }
        return new Aero_MeshModel(name, groupArrays);
    }

    private static void parseFace(String faceStr, List verts, List uvs, List[] groups) {
        String[] tokens = split(faceStr);
        float[][] poly = new float[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            poly[i] = parseFaceVert(tokens[i], verts, uvs);
        }

        // Triangulação por fan (funciona para polígonos convexos)
        for (int i = 1; i < poly.length - 1; i++) {
            float[] v0 = poly[0], v1 = poly[i], v2 = poly[i + 1];

            // Normal da face via produto vetorial
            float ax = v1[0]-v0[0], ay = v1[1]-v0[1], az = v1[2]-v0[2];
            float bx = v2[0]-v0[0], by = v2[1]-v0[1], bz = v2[2]-v0[2];
            float nx = ay*bz - az*by;
            float ny = az*bx - ax*bz;
            float nz = ax*by - ay*bx;
            float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
            if (len > 1e-7f) { nx /= len; ny /= len; nz /= len; }

            int group = brightnessGroup(nx, ny, nz);
            groups[group].add(new float[]{
                v0[0], v0[1], v0[2], v0[3], v0[4],
                v1[0], v1[1], v1[2], v1[3], v1[4],
                v2[0], v2[1], v2[2], v2[3], v2[4]
            });
        }
    }

    /**
     * Classifica a normal em um dos 4 grupos de brightness.
     * Mirrors the logic in Aero_MeshRenderer.
     */
    static int brightnessGroup(float nx, float ny, float nz) {
        float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ay >= ax && ay >= az) return ny > 0 ? Aero_MeshModel.GROUP_TOP : Aero_MeshModel.GROUP_BOTTOM;
        if (az >= ax)             return Aero_MeshModel.GROUP_NS;
        return                           Aero_MeshModel.GROUP_EW;
    }

    /**
     * Parseia um token de vértice de face: "v", "v/vt", "v//vn", "v/vt/vn"
     * Retorna float[5]: x, y, z, u, v
     */
    private static float[] parseFaceVert(String token, List verts, List uvs) {
        String[] parts = token.split("/", -1);

        int vi = Integer.parseInt(parts[0].trim());
        int ti = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1].trim()) : 0;

        float[] v  = (float[]) verts.get(vi < 0 ? verts.size() + vi : vi - 1);
        float[] uv = ti != 0
            ? (float[]) uvs.get(ti < 0 ? uvs.size() + ti : ti - 1)
            : new float[]{0.0f, 0.0f};

        return new float[]{v[0], v[1], v[2], uv[0], uv[1]};
    }

    private static boolean allEmpty(List[] groups) {
        for (int g = 0; g < groups.length; g++) if (!groups[g].isEmpty()) return false;
        return true;
    }

    private static String[] split(String s) { return s.trim().split("\\s+"); }
    private static float f(String s)         { return Float.parseFloat(s.trim()); }
}
