/**
 * Client-side bbmodel parser — extracts animation data from .bbmodel files.
 * This is a standalone copy of the parser logic (no Node.js dependencies).
 */

export interface ParsedClip {
  name: string
  loop: boolean
  length: number
  bones: Record<string, {
    rotation?: Record<string, [number, number, number]>
    position?: Record<string, [number, number, number]>
  }>
}

export interface BbmodelParseResult {
  pivots: Record<string, [number, number, number]>
  childMap: Record<string, string>
  clips: ParsedClip[]
  boneNames: string[]
}

export interface AeroAnimJson {
  format_version: string
  pivots: Record<string, [number, number, number]>
  childMap: Record<string, string>
  animations: Record<string, {
    loop: boolean
    length: number
    bones: Record<string, {
      rotation?: Record<string, [number, number, number]>
      position?: Record<string, [number, number, number]>
    }>
  }>
}

export function parseBbmodel(bbmodel: any): BbmodelParseResult {
  const pivots: Record<string, [number, number, number]> = {}
  const childMap: Record<string, string> = {}
  const boneNames: string[] = []
  const uuidToName = new Map<string, string>()
  const uuidToElementName = new Map<string, string>()

  if (Array.isArray(bbmodel.elements)) {
    for (const el of bbmodel.elements) {
      if (el.uuid && el.name) uuidToElementName.set(el.uuid, el.name)
    }
  }

  if (Array.isArray(bbmodel.outliner)) {
    walkOutliner(bbmodel.outliner, null, uuidToName, uuidToElementName, pivots, childMap, boneNames)
  }

  const clips: ParsedClip[] = []
  if (Array.isArray(bbmodel.animations)) {
    for (const anim of bbmodel.animations) {
      clips.push(parseAnimation(anim, uuidToName))
    }
  }

  return { pivots, childMap, clips, boneNames }
}

export function toAeroAnimJson(result: BbmodelParseResult): AeroAnimJson {
  const animations: AeroAnimJson['animations'] = {}
  for (const clip of result.clips) {
    animations[clip.name] = { loop: clip.loop, length: clip.length, bones: clip.bones }
  }
  return { format_version: '1.0', pivots: result.pivots, childMap: result.childMap, animations }
}

function walkOutliner(
  entries: any[], parentBoneName: string | null,
  uuidToName: Map<string, string>, uuidToElementName: Map<string, string>,
  pivots: Record<string, [number, number, number]>,
  childMap: Record<string, string>, boneNames: string[],
): void {
  for (const entry of entries) {
    if (typeof entry === 'string') {
      if (parentBoneName) {
        const elementName = uuidToElementName.get(entry)
        if (elementName) childMap[elementName] = parentBoneName
      }
    } else if (typeof entry === 'object' && entry !== null) {
      const boneName = entry.name as string
      if (!boneName) continue
      boneNames.push(boneName)
      if (entry.uuid) uuidToName.set(entry.uuid, boneName)
      if (Array.isArray(entry.origin) && entry.origin.length >= 3) {
        pivots[boneName] = [entry.origin[0], entry.origin[1], entry.origin[2]]
      }
      if (Array.isArray(entry.children)) {
        walkOutliner(entry.children, boneName, uuidToName, uuidToElementName, pivots, childMap, boneNames)
      }
    }
  }
}

function parseAnimation(anim: any, uuidToName: Map<string, string>): ParsedClip {
  const name = anim.name || 'unnamed'
  const loop = anim.loop === 'loop' || anim.loop === true
  const length = typeof anim.length === 'number' ? anim.length : 1.0
  const bones: ParsedClip['bones'] = {}

  if (anim.animators && typeof anim.animators === 'object') {
    for (const [uuid, animator] of Object.entries(anim.animators as Record<string, any>)) {
      const boneName = animator.name || uuidToName.get(uuid) || uuid
      if (!bones[boneName]) bones[boneName] = {}

      if (Array.isArray(animator.keyframes)) {
        for (const kf of animator.keyframes) {
          const channel = kf.channel as string
          if (channel !== 'rotation' && channel !== 'position') continue
          const timeStr = String(kf.time)
          const dp = Array.isArray(kf.data_points) && kf.data_points.length > 0
            ? kf.data_points[0] : { x: 0, y: 0, z: 0 }
          const values: [number, number, number] = [
            parseFloat(dp.x) || 0, parseFloat(dp.y) || 0, parseFloat(dp.z) || 0,
          ]
          if (!bones[boneName][channel]) bones[boneName][channel] = {}
          bones[boneName][channel]![timeStr] = values
        }
      }
    }
  }

  return { name, loop, length, bones }
}
