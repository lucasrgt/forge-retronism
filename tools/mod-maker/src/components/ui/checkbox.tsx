import * as React from 'react'
import { cn } from '@/lib/utils'

interface CheckboxProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string
}

const Checkbox = React.forwardRef<HTMLInputElement, CheckboxProps>(
  ({ className, label, id, ...props }, ref) => (
    <label
      htmlFor={id}
      className={cn('flex items-center gap-2 text-xs text-muted-foreground cursor-pointer select-none', className)}
    >
      <input type="checkbox" id={id} ref={ref} className="accent-primary" {...props} />
      {label}
    </label>
  ),
)
Checkbox.displayName = 'Checkbox'

export { Checkbox }
