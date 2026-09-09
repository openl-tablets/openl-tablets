import React from 'react'
import { Flex, Popover, Space } from 'antd'

/** Where the popover hangs. The viewport rectangle of the toolbar button that opened it. */
export interface PopoverAnchor {
    left: number
    top: number
    width: number
    height: number
}

export interface TableInputPopoverProps {
    open: boolean
    anchor: PopoverAnchor
    /** Width of the popover content in pixels. */
    width: number
    /** Keeps the popover open on an outside click while an action it started is still running. */
    busy?: boolean
    onClose: () => void
    /** The action buttons, right-aligned under the content. */
    footer: React.ReactNode
    children: React.ReactNode
}

/**
 * A drop-down under a button of the legacy table page.
 *
 * The page and the React app share one document, so the popover hangs under the button by its viewport
 * rectangle. An outside click closes it, as the legacy drop-downs did.
 */
export const TableInputPopover: React.FC<TableInputPopoverProps> = ({ open, anchor, width, busy, onClose, footer, children }) => (
    <Popover
        destroyOnHidden
        arrow={false}
        open={open}
        placement="bottomLeft"
        styles={{ content: { maxHeight: '75vh', overflow: 'auto' } }}
        trigger="click"
        content={(
            <Space orientation="vertical" size="middle" style={{ width }}>
                {children}
                <Flex gap="small" justify="flex-end">{footer}</Flex>
            </Space>
        )}
        onOpenChange={next => {
            if (!next && !busy) {
                onClose()
            }
        }}
    >
        <span
            data-testid="table-input-anchor"
            style={{
                position: 'fixed',
                left: anchor.left,
                top: anchor.top,
                width: anchor.width,
                height: anchor.height,
                pointerEvents: 'none',
            }}
        />
    </Popover>
)

export default TableInputPopover
