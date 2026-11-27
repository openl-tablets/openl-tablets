import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { WorkspaceHeader } from './WorkspaceHeader'

describe('WorkspaceHeader', () => {
    it('follows every step of the trail with a slash, and ends it with the name of what is open', () => {
        render(
            <WorkspaceHeader
                crumbs={[<a key="projects" href="/projects">Projects</a>, false, <span key="folder">Design</span>]}
                testId="header"
                title="Bank Rating"
                titleAfter={<span>compiled</span>}
                titleBefore={<span>opened</span>}
            />
        )

        const steps = [screen.getByRole('link', { name: 'Projects' }), screen.getByText('Design')]
            .map(name => name.parentElement)
        // Each name stands with its slash in a step of its own, which the trail wraps between, never inside.
        expect(steps.map(step => step?.textContent)).toEqual(['Projects/', 'Design/'])
        expect(steps[0]?.lastElementChild).toHaveAttribute('aria-hidden')
        // The name is the last step, with no slash after it and the marks of its state on either side.
        const name = screen.getByRole('heading', { name: 'Bank Rating' })
        expect(name).toHaveAttribute('aria-current', 'page')
        expect(name.parentElement?.textContent).toBe('openedBank Ratingcompiled')
        // A step left out leaves no slash behind.
        expect(steps[0]?.parentElement?.children).toHaveLength(3)
    })

    it('parts the name from the step under what it names by a slash, ending the trail in that step', () => {
        render(
            <WorkspaceHeader
                crumbs={[<a key="projects" href="/projects">Projects</a>]}
                testId="header"
                title="Bank Rating"
                titleAfter={<span>compiled</span>}
                trailAfter={<span>main</span>}
            />
        )

        const name = screen.getByRole('heading', { name: 'Bank Rating' })
        // The marks of its state stay beside the name, the slash after them.
        expect(name.parentElement?.textContent).toBe('Bank Ratingcompiled/')
        expect(screen.getByText('main').parentElement?.textContent).toBe('main')
        expect(name.parentElement?.parentElement?.children).toHaveLength(3)
    })

    it('leaves no slash after the name when no step follows it', () => {
        render(<WorkspaceHeader crumbs={[]} testId="header" title="Bank Rating" trailAfter={false} />)

        expect(screen.getByRole('heading', { name: 'Bank Rating' }).parentElement?.textContent).toBe('Bank Rating')
    })

    it('ends the line of the trail with the actions, which take a line of their own only where it leaves them no room', () => {
        const { rerender } = render(
            <WorkspaceHeader actions={<button type="button">Save</button>} crumbs={[]} testId="header" title="Bank Rating" />
        )

        const header = screen.getByTestId('header')
        expect(header.children).toHaveLength(2)
        expect(getComputedStyle(header).flexWrap).toBe('wrap')
        const actions = header.lastElementChild as HTMLElement
        expect(getComputedStyle(actions).justifyContent).toBe('flex-end')
        expect(getComputedStyle(actions).flexBasis).toBe('240px')
        expect(header.lastElementChild).toContainElement(screen.getByRole('button', { name: 'Save' }))
        expect(header.firstElementChild).not.toContainElement(screen.getByRole('button', { name: 'Save' }))

        rerender(<WorkspaceHeader crumbs={[]} testId="header" title="Bank Rating" />)
        expect(header.children).toHaveLength(1)
    })
})
