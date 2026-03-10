/**
 * Blockbench .bbmodel parser — extracts animation data and converts to Aero .anim.json format.
 *
 * The .bbmodel format stores:
 *   - outliner[]: bone hierarchy (groups with origin/rotation and children)
 *   - animations[]: clips with animators containing keyframes per bone
 *
 * This parser extracts:
 *   - Pivots: bone origin points (Blockbench pixels, NOT divided by 16)
 *   - ChildMap: maps child element UUIDs to parent bone names
 *   - Clips: animation clips with rotation/position keyframes per bone
 *
 * Output format matches Aero_AnimationLoader expectations:
 * {
 *   "format_version": "1.0",
 *   "pivots": { "boneName": [px, py, pz] },
 *   "childMap": { "childElement": "parentBone" },
 *   "animations": {
 *     "clipName": {
 *       "loop": true/false,
 *       "length": seconds,
 *       "bones": {
 *         "boneName": {
 *           "rotation": { "0.0": [rx, ry, rz], ... },
 *           "position": { "0.0": [px, py, pz], ... }
 *         }
 *       }
 *     }
 *   }
 * }
 */

// -------------------------------------------------------------------------
// Types
// -------------------------------------------------------------------------

/** Parsed animation clip info (before export to .anim.json) */
export interface ParsedClip {
  name: string;
  loop: boolean;
  length: number;
  bones: Record<string, {
    rotation?: Record<string, [number, number, number]>;
    position?: Record<string, [number, number, number]>;
  }>;
}

/** Full parsed result from a .bbmodel file */
export interface BbmodelParseResult {
  /** Bone pivots in Blockbench pixels (NOT divided by 16 — loader does that) */
  pivots: Record<string, [number, number, number]>;
  /** Child element name → parent bone name */
  childMap: Record<string, string>;
  /** Animation clips */
  clips: ParsedClip[];
  /** Bone names found in outliner */
  boneNames: string[];
}

/** Aero .anim.json format */
export interface AeroAnimJson {
  format_version: string;
  pivots: Record<string, [number, number, number]>;
  childMap: Record<string, string>;
  animations: Record<string, {
    loop: boolean;
    length: number;
    bones: Record<string, {
      rotation?: Record<string, [number, number, number]>;
      position?: Record<string, [number, number, number]>;
    }>;
  }>;
}

// -------------------------------------------------------------------------
// Parser
// -------------------------------------------------------------------------

/**
 * Parses a .bbmodel JSON object and extracts animation-related data.
 * Geometry is NOT extracted — use the OBJ export from Blockbench for that.
 */
export function parseBbmodel(bbmodel: any): BbmodelParseResult {
  const pivots: Record<string, [number, number, number]> = {};
  const childMap: Record<string, string> = {};
  const boneNames: string[] = [];

  // UUID → name lookup (for resolving animator references)
  const uuidToName = new Map<string, string>();

  // UUID → element name (for childMap — elements are cubes/meshes)
  const uuidToElementName = new Map<string, string>();

  // Build element UUID → name map from top-level elements array
  if (Array.isArray(bbmodel.elements)) {
    for (const el of bbmodel.elements) {
      if (el.uuid && el.name) {
        uuidToElementName.set(el.uuid, el.name);
      }
    }
  }

  // Walk the outliner to extract bones (groups) and their hierarchy
  if (Array.isArray(bbmodel.outliner)) {
    walkOutliner(bbmodel.outliner, null, uuidToName, uuidToElementName, pivots, childMap, boneNames);
  }

  // Parse animations
  const clips: ParsedClip[] = [];
  if (Array.isArray(bbmodel.animations)) {
    for (const anim of bbmodel.animations) {
      clips.push(parseAnimation(anim, uuidToName));
    }
  }

  return { pivots, childMap, clips, boneNames };
}

/**
 * Converts a BbmodelParseResult to the Aero .anim.json format.
 */
export function toAeroAnimJson(result: BbmodelParseResult): AeroAnimJson {
  const animations: AeroAnimJson['animations'] = {};

  for (const clip of result.clips) {
    animations[clip.name] = {
      loop: clip.loop,
      length: clip.length,
      bones: clip.bones,
    };
  }

  return {
    format_version: '1.0',
    pivots: result.pivots,
    childMap: result.childMap,
    animations,
  };
}

// -------------------------------------------------------------------------
// Internals
// -------------------------------------------------------------------------

/**
 * Recursively walks the Blockbench outliner tree.
 *
 * Outliner entries can be:
 *   - Object (group/bone): { name, origin, children, uuid, ... }
 *   - String (element UUID reference): points to an element in the elements array
 */
function walkOutliner(
  entries: any[],
  parentBoneName: string | null,
  uuidToName: Map<string, string>,
  uuidToElementName: Map<string, string>,
  pivots: Record<string, [number, number, number]>,
  childMap: Record<string, string>,
  boneNames: string[],
): void {
  for (const entry of entries) {
    if (typeof entry === 'string') {
      // Element UUID reference — map to parent bone
      if (parentBoneName) {
        const elementName = uuidToElementName.get(entry);
        if (elementName) {
          childMap[elementName] = parentBoneName;
        }
      }
    } else if (typeof entry === 'object' && entry !== null) {
      // Group (bone)
      const boneName = entry.name as string;
      if (!boneName) continue;

      boneNames.push(boneName);

      if (entry.uuid) {
        uuidToName.set(entry.uuid, boneName);
      }

      // Extract pivot (origin in Blockbench pixels)
      if (Array.isArray(entry.origin) && entry.origin.length >= 3) {
        pivots[boneName] = [
          entry.origin[0] as number,
          entry.origin[1] as number,
          entry.origin[2] as number,
        ];
      }

      // Recurse into children
      if (Array.isArray(entry.children)) {
        walkOutliner(entry.children, boneName, uuidToName, uuidToElementName, pivots, childMap, boneNames);
      }
    }
  }
}

/**
 * Parses a single Blockbench animation entry into a ParsedClip.
 *
 * Blockbench animation format:
 * {
 *   name: "spin",
 *   loop: "loop" | "once" | "hold",
 *   length: 1.0,
 *   animators: {
 *     "bone-uuid": {
 *       name: "fan",
 *       keyframes: [
 *         { channel: "rotation", time: 0.0, data_points: [{ x: 0, y: 0, z: 0 }] },
 *         { channel: "rotation", time: 1.0, data_points: [{ x: 0, y: 0, z: 360 }] },
 *       ]
 *     }
 *   }
 * }
 */
function parseAnimation(anim: any, uuidToName: Map<string, string>): ParsedClip {
  const name = anim.name || 'unnamed';
  const loop = anim.loop === 'loop' || anim.loop === true;
  const length = typeof anim.length === 'number' ? anim.length : 1.0;

  const bones: ParsedClip['bones'] = {};

  if (anim.animators && typeof anim.animators === 'object') {
    for (const [uuid, animator] of Object.entries(anim.animators as Record<string, any>)) {
      // Resolve bone name: animator.name or UUID lookup
      const boneName = animator.name || uuidToName.get(uuid) || uuid;

      if (!bones[boneName]) {
        bones[boneName] = {};
      }

      if (Array.isArray(animator.keyframes)) {
        for (const kf of animator.keyframes) {
          const channel = kf.channel as string; // "rotation" | "position" | "scale"
          if (channel !== 'rotation' && channel !== 'position') continue;

          const timeStr = String(kf.time);
          const dp = Array.isArray(kf.data_points) && kf.data_points.length > 0
            ? kf.data_points[0]
            : { x: 0, y: 0, z: 0 };

          const values: [number, number, number] = [
            parseFloat(dp.x) || 0,
            parseFloat(dp.y) || 0,
            parseFloat(dp.z) || 0,
          ];

          if (!bones[boneName][channel]) {
            bones[boneName][channel] = {};
          }
          bones[boneName][channel]![timeStr] = values;
        }
      }
    }
  }

  return { name, loop, length, bones };
}
