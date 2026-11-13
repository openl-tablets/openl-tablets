import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ProjectsViewSwitch } from './ProjectsViewSwitch'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const item = (title: string) => screen.getByTitle(title)

describe('ProjectsViewSwitch', () => {
    it('shows the other view on a click on either side, like an on/off switch', async () => {
        const onView = vi.fn()
        render(<ProjectsViewSwitch onView={onView} view="list" />)

        await userEvent.click(item('home.view_list'))
        await userEvent.click(item('home.view_grid'))

        expect(onView.mock.calls).toEqual([['grid'], ['grid']])
    })

    it('switches back from the grid', async () => {
        const onView = vi.fn()
        render(<ProjectsViewSwitch onView={onView} view="grid" />)

        await userEvent.click(item('home.view_grid'))

        expect(onView.mock.calls).toEqual([['list']])
    })

    it('flips on a click on the frame around the parts, and on one a screen reader sends to a radio button', () => {
        const onView = vi.fn()
        render(<ProjectsViewSwitch onView={onView} view="list" />)
        const control = screen.getByTestId('projects-view')

        fireEvent.click(control)
        fireEvent.click(control.querySelector('input:not(:checked)')!)

        expect(onView.mock.calls).toEqual([['grid'], ['grid']])
    })

    it('flips on Space, Enter and the arrow keys, and ignores other keys', () => {
        const onView = vi.fn()
        render(<ProjectsViewSwitch onView={onView} view="list" />)
        const control = screen.getByTestId('projects-view')

        for (const key of [' ', 'Enter', 'ArrowRight', 'ArrowLeft', 'Tab']) {
            fireEvent.keyDown(control, { key })
        }

        expect(onView).toHaveBeenCalledTimes(4)
        expect(onView).toHaveBeenLastCalledWith('grid')
    })

    it('leaves a key held down or pressed with a modifier alone', () => {
        const onView = vi.fn()
        render(<ProjectsViewSwitch onView={onView} view="list" />)
        const control = screen.getByTestId('projects-view')

        fireEvent.keyDown(control, { key: 'ArrowRight', repeat: true })
        fireEvent.keyDown(control, { key: 'ArrowLeft', altKey: true })
        fireEvent.keyDown(control, { key: 'ArrowRight', ctrlKey: true })
        fireEvent.keyDown(control, { key: 'ArrowRight', metaKey: true })

        expect(onView).not.toHaveBeenCalled()
    })
})
