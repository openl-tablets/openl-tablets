import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Button } from 'antd'
import { CollapsingActionBar, type ActionMenu } from './CollapsingActionBar'

const ACTIONS = ['save', 'copy', 'deploy', 'export']

const bar = (run: (action: string, menu?: ActionMenu) => void) => render(
    <CollapsingActionBar
        actions={ACTIONS}
        deps={[]}
        keyOf={action => action}
        menuLabel="Actions"
        renderMeasure={action => <Button>{action}</Button>}
        testId="bar"
        render={(action, menu) => (
            <Button data-testid={`action-${action}`} onClick={() => run(action, menu)}>{action}</Button>
        )}
    >
        <div data-testid="dialogs" />
    </CollapsingActionBar>
)

describe('CollapsingActionBar', () => {
    afterEach(() => vi.restoreAllMocks())

    it('shows every action as a button while the row has room, and no menu', () => {
        bar(vi.fn())

        expect(screen.getByTestId('bar')).toContainElement(screen.getByTestId('action-export'))
        expect(screen.queryByTestId('bar-more')).toBeNull()
        expect(screen.getByTestId('dialogs')).toBeInTheDocument()
    })

    it('moves the actions the row has no room for to its menu, which a button may close and anchor at', async () => {
        // A row of 300 px, every button 100 px wide: one fits beside the trigger of the menu.
        vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100)
        vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(300)
        const run = vi.fn((_action: string, menu?: ActionMenu) => {
            expect(menu?.anchor()).toBe(screen.getByTestId('bar-more'))
            menu?.close()
        })
        bar(run)

        expect(screen.queryByTestId('action-export')).toBeNull()
        await userEvent.click(screen.getByLabelText('Actions'))
        expect(await screen.findByTestId('bar-overflow')).toContainElement(screen.getByTestId('action-export'))

        await userEvent.click(screen.getByTestId('action-export'))

        expect(run).toHaveBeenCalledWith('export', expect.objectContaining({ close: expect.any(Function) }))
    })

    it('closes its menu once everything fits, so the menu does not come back open by itself', async () => {
        vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockReturnValue(100)
        const room = vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(300)
        // Another set of actions each time, so the bar works out what fits in the room of the moment.
        const view = (set: string) => (
            <CollapsingActionBar
                actions={ACTIONS}
                deps={[set]}
                keyOf={action => action}
                menuLabel="Actions"
                render={action => <Button data-testid={`action-${action}`}>{action}</Button>}
                renderMeasure={action => <Button>{action}</Button>}
                testId="bar"
            />
        )
        const { rerender } = render(view('narrow'))
        await userEvent.click(screen.getByLabelText('Actions'))
        expect(await screen.findByTestId('bar-overflow')).toBeInTheDocument()

        room.mockReturnValue(1000)
        rerender(view('wide'))
        expect(screen.queryByTestId('bar-more')).toBeNull()

        room.mockReturnValue(300)
        rerender(view('narrow again'))
        expect(screen.getByTestId('bar-more')).toBeInTheDocument()
        expect(screen.queryByTestId('bar-overflow')).toBeNull()
    })
})
