import type { ReactNode } from 'react'
import { Button, Tooltip } from 'antd'

interface IconActionProps {
    icon: ReactNode
    onClick: () => void
    size?: 'small'
    title: string
}

/**
 * A borderless icon button that edits the row or column it sits on.
 *
 * <p>The tooltip text is also the accessible name, so what a pointer user reads and what a screen reader announces
 * cannot drift apart.
 */
export const IconAction = ({ icon, onClick, size, title }: IconActionProps) => (
    <Tooltip title={title}>
        <Button aria-label={title} icon={icon} onClick={onClick} size={size} type="text" />
    </Tooltip>
)
