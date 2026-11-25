import { Fragment, useRef, useState, type DependencyList, type ReactNode } from 'react'
import { Button, Dropdown } from 'antd'
import { MoreOutlined } from '@ant-design/icons'
import { useActionBarStyles, useActionFit } from './actionFit'

/** What a button drawn in the menu of the bar can do: close the menu, and stand what it opens at its trigger. */
export interface ActionMenu {
    close: () => void
    /** The trigger of the menu, which stays on screen once the menu closes. */
    anchor: () => HTMLElement | null
}

interface CollapsingActionBarProps<T> {
    actions: T[]
    keyOf: (action: T) => string
    /** Draws an action: as a button of the row, or, given the menu, as one of the buttons of the menu. */
    render: (action: T, menu?: ActionMenu) => ReactNode
    /** Draws the off-screen copy of an action the bar measures: as wide as its button, and with no test id. */
    renderMeasure: (action: T) => ReactNode
    /** What the fit depends on besides the number of actions: another set of actions of the same number. */
    deps: DependencyList
    /** The accessible name of the trigger of the menu. */
    menuLabel: string
    /** Whether the trigger of the menu stands disabled. */
    menuDisabled?: boolean
    /** The test id of the bar: its menu is `-overflow` after it, the trigger of the menu `-more`. */
    testId: string
    /** Drawn after the bar's own content, such as the dialogs its actions open. */
    children?: ReactNode
}

/**
 * A row of action buttons that never wraps: the actions it has no room for go, as the same buttons, to the menu of
 * the three-dots trigger at its end, which shows only while something is in it.
 *
 * <p>The bar measures an off-screen copy of every action and works out again what fits as the row resizes.
 */
export const CollapsingActionBar = <T,>({
    actions,
    keyOf,
    render,
    renderMeasure,
    deps,
    menuLabel,
    menuDisabled = false,
    testId,
    children,
}: CollapsingActionBarProps<T>) => {
    const { styles: fit } = useActionBarStyles()
    const { barRef, measureRef, visible } = useActionFit(actions.length, deps)
    const [open, setOpen] = useState(false)
    const triggerRef = useRef<HTMLButtonElement>(null)
    const menu: ActionMenu = { close: () => setOpen(false), anchor: () => triggerRef.current }
    const collapsed = actions.slice(visible)
    // The menu leaves the bar once everything fits, and is closed with it: it would come back open the next time an
    // action collapses.
    if (open && collapsed.length === 0) {
        setOpen(false)
    }
    // Built once per render, not inside popupRender: a function creating elements would read as a new component every
    // time the menu opens.
    const overflowPanel = (
        <div className={fit.overflow} data-testid={`${testId}-overflow`}>
            {collapsed.map(action => <Fragment key={keyOf(action)}>{render(action, menu)}</Fragment>)}
        </div>
    )
    const renderOverflow = () => overflowPanel

    return (
        <div ref={barRef} className={fit.bar} data-testid={testId}>
            {actions.slice(0, visible).map(action => <Fragment key={keyOf(action)}>{render(action)}</Fragment>)}
            {collapsed.length > 0 && (
                <Dropdown
                    onOpenChange={setOpen}
                    open={open}
                    placement="bottomRight"
                    popupRender={renderOverflow}
                    trigger={['click']}
                >
                    <Button
                        ref={triggerRef}
                        aria-label={menuLabel}
                        data-testid={`${testId}-more`}
                        disabled={menuDisabled}
                        icon={<MoreOutlined />}
                    />
                </Dropdown>
            )}
            <div ref={measureRef} aria-hidden className={fit.measure}>
                {actions.map(action => <Fragment key={keyOf(action)}>{renderMeasure(action)}</Fragment>)}
                <Button icon={<MoreOutlined />} />
            </div>
            {children}
        </div>
    )
}
