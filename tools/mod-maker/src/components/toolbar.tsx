import { Button } from '@/components/ui/button'
import { useStore } from '@/hooks/use-store'

export function Toolbar() {
  const name = useStore((s) => s.name)

  return (
    <div className="flex items-center justify-between px-4 py-2 bg-background border-b border-border">
      <div className="flex items-center gap-3">
        <span className="text-sm font-semibold">RetroNism Mod Maker</span>
        <span className="text-sm text-muted-foreground">{name}</span>
      </div>
      <div className="flex gap-1.5">
        <Button variant="outline" size="sm" onClick={() => useStore.getState().newProject()}>New</Button>
        <Button variant="outline" size="sm" onClick={() => useStore.getState().importJSON()}>Import JSON</Button>
        <Button variant="outline" size="sm" onClick={() => useStore.getState().exportJSON()}>Export JSON</Button>
        <Button variant="default" size="sm">Generate Code</Button>
        <Button variant="success" size="sm">Export to Mod</Button>
      </div>
    </div>
  )
}
