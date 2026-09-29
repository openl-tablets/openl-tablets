import type { ReactNode } from 'react'
import { Button, Tooltip } from 'antd'

interface IconActionProps {
    icon: ReactNode
    onClick: () => void
    size?: 'small'
    title: string
    'data-testid'?: string
}

/**
 * A borderless icon button that acts on the row, column or group it sits on.
 *
 * <p>The tooltip text is also the accessible name, so what a pointer user reads and what a screen reader announces
 * cannot drift apart.
 */
export const IconAction = ({ icon, onClick, size, title, 'data-testid': testId }: IconActionProps) => (
    <Tooltip title={title}>
        <Button aria-label={title} data-testid={testId} icon={icon} onClick={onClick} size={size} type="text" />
    </Tooltip>
)
