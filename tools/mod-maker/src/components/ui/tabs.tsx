import * as React from 'react'
import { cn } from '@/lib/utils'

interface TabsProps {
  tabs: { id: string; label: string }[]
  active: string
  onChange: (id: string) => void
  className?: string
}

export function Tabs({ tabs, active, onChange, className }: TabsProps) {
  return (
    <div className={cn('flex bg-card border-b-2 border-border', className)}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          onClick={() => onChange(tab.id)}
          className={cn(
            'px-6 py-2 text-xs font-bold transition-all border-b-2 -mb-[2px]',
            active === tab.id
              ? 'text-primary border-primary'
              : 'text-muted-foreground border-transparent hover:text-foreground hover:bg-white/[0.03]',
          )}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
