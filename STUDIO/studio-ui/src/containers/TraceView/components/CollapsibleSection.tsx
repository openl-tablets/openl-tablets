import React, { useState } from 'react'
import { Tooltip } from 'antd'
import { CaretRightOutlined } from '@ant-design/icons'
import { useStyles } from './CollapsibleSection.styles'

interface CollapsibleSectionProps {
    /** The section title, shown in the header and clicked to collapse. */
    title: React.ReactNode
    /** Optional tooltip on the title. */
    hint?: string
    /** Optional content pinned to the right of the title (e.g. an action button). */
    extra?: React.ReactNode
    /** Class for the section container. */
    className?: string
    /** Test id for the section container. */
    panelTestId?: string
    /** Test id for the collapse toggle. */
    toggleTestId?: string
    children: React.ReactNode
}

/**
 * A left-panel section that collapses from its title, revealing more of the tree when it is not in use.
 * The title is a real button (keyboard-accessible, {@code aria-expanded}); anything in {@code extra}
 * stays visible whether the body is open or not.
 */
const CollapsibleSection: React.FC<CollapsibleSectionProps> = (
    { title, hint, extra, className, panelTestId, toggleTestId, children }
) => {
    const { styles } = useStyles()
    const [collapsed, setCollapsed] = useState(false)

    const toggle = (
        <button
            aria-expanded={!collapsed}
            className={styles.toggle}
            data-testid={toggleTestId}
            onClick={() => setCollapsed(open => !open)}
            type="button"
        >
            <CaretRightOutlined className={styles.caret} rotate={collapsed ? 0 : 90} />
            {title}
        </button>
    )

    return (
        <div className={className} data-testid={panelTestId}>
            <div className={styles.header}>
                {hint ? <Tooltip title={hint}>{toggle}</Tooltip> : toggle}
                {extra}
            </div>
            {!collapsed && children}
        </div>
    )
}

export default CollapsibleSection
