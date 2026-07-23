import type { ReactNode } from 'react'
import { Button, Dropdown } from 'antd'
import type { ButtonProps, DropdownProps } from 'antd'
import { DownOutlined } from '@ant-design/icons'

type Menu = NonNullable<DropdownProps['menu']>

interface MenuButtonProps extends Omit<ButtonProps, 'onClick'> {
    /** Entries the button opens; the caller handles the choice through the menu's own onClick. */
    menu: Menu
    /** Where the menu opens relative to the button. */
    placement?: DropdownProps['placement']
    children: ReactNode
}

/**
 * A button whose only job is to open a menu, with the caret Ant Design draws on such buttons.
 *
 * Ant Design has no component for this — `Dropdown.Button` is its split-button variant and is deprecated —
 * so this is the composition it recommends, kept in one place instead of being re-assembled per screen.
 */
export const MenuButton = ({ menu, placement, children, ...button }: MenuButtonProps) => (
    <Dropdown menu={menu} {...(placement ? { placement } : {})}>
        <Button {...button}>
            {children}
            <DownOutlined />
        </Button>
    </Dropdown>
)
