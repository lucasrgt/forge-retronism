import { Card, CardTitle } from '@/components/ui/card'
import { Select } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { useStore } from '@/hooks/use-store'
import { blockRegistry } from '@/core/types'

export function RightPanel() {
  const blocks = useStore((s) => s.blocks)
  const selectedBlock = useStore((s) => s.selectedBlock)
  const removeBlock = useStore((s) => s.removeBlock)
  const placeBlock = useStore((s) => s.placeBlock)

  // Count blocks by type
  const counts: Record<string, number> = {}
  for (const [, block] of blocks) {
    counts[block.type] = (counts[block.type] || 0) + 1
  }

  const selEntry = selectedBlock ? blocks.get(selectedBlock) : null
  const selCoords = selectedBlock?.split(',').map(Number)

  return (
    <div className="w-56 bg-background border-l border-border overflow-y-auto p-3 space-y-3 shrink-0">
      {selEntry && selCoords && (
        <Card>
          <CardTitle>Selected Block</CardTitle>
          <p className="text-xs text-muted-foreground">
            Position: <span className="text-foreground font-bold">{selectedBlock}</span>
          </p>
          <Label>Type</Label>
          <Select
            value={selEntry.type}
            onChange={(e) => {
              placeBlock(selCoords[0], selCoords[1], selCoords[2], e.target.value as any, selEntry.mode)
            }}
          >
            {[...blockRegistry.values()].map((def) => (
              <option key={def.id} value={def.id}>{def.label}</option>
            ))}
          </Select>
          {(blockRegistry.get(selEntry.type)?.category === 'port') && (
            <>
              <Label>Default Mode</Label>
              <Select
                value={selEntry.mode}
                onChange={(e) => {
                  placeBlock(selCoords[0], selCoords[1], selCoords[2], selEntry.type, e.target.value as any)
                }}
              >
                <option value="input">Input</option>
                <option value="output">Output</option>
                <option value="input_output">Input/Output</option>
              </Select>
            </>
          )}
          <Button variant="destructive" size="sm" className="w-full" onClick={() => {
            removeBlock(selCoords[0], selCoords[1], selCoords[2])
            useStore.getState().setSelectedBlock(null)
          }}>
            Delete Block
          </Button>
        </Card>
      )}

      <Card>
        <CardTitle>Structure Summary</CardTitle>
        <div className="space-y-0.5 text-xs">
          <p>Total blocks: <span className="font-bold text-foreground">{blocks.size}</span></p>
          {[...blockRegistry.values()].map((def) => (
            counts[def.id] ? <p key={def.id}>{def.label}: <span className="font-bold text-foreground">{counts[def.id]}</span></p> : null
          ))}
        </div>
      </Card>
    </div>
  )
}
