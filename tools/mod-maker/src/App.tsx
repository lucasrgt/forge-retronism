import { useEffect } from 'react'
import { Toolbar } from '@/components/toolbar'
import { LeftPanel } from '@/components/left-panel'
import { RightPanel } from '@/components/right-panel'
import { StructureEditor } from '@/components/structure-editor'
import { GuiBuilder } from '@/components/gui-builder'
import { Tabs } from '@/components/ui/tabs'
import { useStore, type SerializedMultiblock } from '@/hooks/use-store'

const TAB_ITEMS = [
  { id: 'structure', label: '3D Structure' },
  { id: 'gui', label: 'GUI Builder' },
]

export default function App() {
  const activeTab = useStore((s) => s.activeTab)
  const blocks = useStore((s) => s.blocks)
  const mcpConnected = useStore((s) => s.mcpConnected)

  // Listen for MCP state updates from Electron main process
  useEffect(() => {
    const api = (window as any).api
    if (!api?.onMcpStateUpdate) return
    api.onMcpStateUpdate((content: string) => {
      try {
        const data = JSON.parse(content) as SerializedMultiblock
        useStore.getState().deserialize(data)
        useStore.getState().setMcpConnected(true)
      } catch { /* ignore parse errors */ }
    })
  }, [])

  return (
    <div className="flex flex-col h-screen">
      <Toolbar />
      <div className="flex flex-1 overflow-hidden">
        <LeftPanel />
        <div className="flex-1 flex flex-col overflow-hidden">
          <Tabs
            tabs={TAB_ITEMS}
            active={activeTab}
            onChange={(id) => useStore.getState().setActiveTab(id as any)}
          />
          <div className="flex-1 flex flex-col overflow-hidden">
            {activeTab === 'structure' && <StructureEditor />}
            {activeTab === 'gui' && <GuiBuilder />}
          </div>
          <div className="flex items-center justify-between px-3 py-1.5 border-t border-border text-xs text-muted-foreground">
            <span>Click: select | Shift+click / Right-click: place | Alt+drag: orbit | Space+drag: pan | Scroll: zoom</span>
            <div className="flex items-center gap-3">
              <span className={`flex items-center gap-1 ${mcpConnected ? 'text-green-400' : 'text-muted-foreground'}`}>
                <span className={`w-1.5 h-1.5 rounded-full ${mcpConnected ? 'bg-green-400' : 'bg-muted-foreground'}`} />
                MCP {mcpConnected ? 'Connected' : 'Idle'}
              </span>
              <span>Blocks: {blocks.size}</span>
            </div>
          </div>
        </div>
        <RightPanel />
      </div>
    </div>
  )
}
