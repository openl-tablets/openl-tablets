import type { ReactNode } from 'react'
import { Button, Dropdown, Space } from 'antd'
import type { ButtonProps, DropdownProps } from 'antd'
import { DownOutlined } from '@ant-design/icons'

interface SplitButtonProps extends ButtonProps {
    /** Entries the arrow opens; the caller handles the choice through the menu's own onClick. */
    menu: NonNullable<DropdownProps['menu']>
    /** Test id of the arrow button; the main button takes `data-testid` like any other button. */
    arrowTestId?: string
    /** What the arrow announces to assistive technology, since it carries no text. */
    arrowLabel?: string
    children: ReactNode
}

/**
 * A main action with a menu of related ones hanging off an arrow beside it.
 *
 * Replaces Ant Design's deprecated `Dropdown.Button`: that one also renders a block-level `Space.Compact`,
 * which pushes neighbouring buttons onto their own row. The menu opens aligned to the right edge of the
 * pair, exactly as Ant Design aligns its own split button.
 */
export const SplitButton = ({ menu, arrowTestId, arrowLabel, children, ...button }: SplitButtonProps) => (
    <Space.Compact>
        <Button {...button}>{children}</Button>
        <Dropdown menu={menu} placement="bottomRight">
            <Button
                aria-label={arrowLabel}
                data-testid={arrowTestId}
                icon={<DownOutlined />}
                {...(button.disabled === undefined ? {} : { disabled: button.disabled })}
                {...(button.type === undefined ? {} : { type: button.type })}
            />
        </Dropdown>
    </Space.Compact>
)
