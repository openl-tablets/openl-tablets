import type { ReactNode } from 'react'
import { Button, Space, Tooltip } from 'antd'
import { useSharedStyles } from './sharedStyles'

interface CompactFieldProps {
    /** The control the field is read and written through. */
    children: ReactNode
    /** What the button hanging off the end of the control does. */
    action: {
        icon: ReactNode
        /** Named for the pointer and for a screen reader alike. */
        title: string
        onClick: () => void
        'data-testid'?: string
    }
}

/**
 * A control with one button hanging off its end, sized so the control takes the room and the button does
 * not.
 *
 * <p>The sizing is the reason this is shared rather than written out each time: a Select or an
 * AutoComplete is preceded by a hidden element while it has the focus, which makes addressing the control
 * by its position wrong. {@link useSharedStyles}'s `compactField` says how, once.
 */
export const CompactField = ({ children, action }: CompactFieldProps) => {
    const { styles: shared } = useSharedStyles()
    return (
        <Space.Compact block className={shared.compactField}>
            {children}
            <Tooltip title={action.title}>
                <Button
                    aria-label={action.title}
                    data-testid={action['data-testid']}
                    icon={action.icon}
                    onClick={action.onClick}
                />
            </Tooltip>
        </Space.Compact>
    )
}
