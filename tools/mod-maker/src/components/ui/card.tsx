import * as React from 'react'
import { cn } from '@/lib/utils'

const Card = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cn('rounded-lg bg-secondary p-3 space-y-2', className)} {...props} />
  ),
)
Card.displayName = 'Card'

const CardTitle = React.forwardRef<HTMLHeadingElement, React.HTMLAttributes<HTMLHeadingElement>>(
  ({ className, ...props }, ref) => (
    <h3 ref={ref} className={cn('text-[11px] font-bold uppercase tracking-wider text-primary', className)} {...props} />
  ),
)
CardTitle.displayName = 'CardTitle'

export { Card, CardTitle }
