import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { WorkspaceHeader } from './WorkspaceHeader'

describe('WorkspaceHeader', () => {
    it('follows every step of the trail with a slash, the last one too, so the title reads as its next step', () => {
        render(
            <WorkspaceHeader
                crumbs={[<a key="projects" href="/projects">Projects</a>, false, <span key="repository">Design</span>]}
                testId="header"
                title="Bank Rating"
            />
        )

        const steps = [screen.getByRole('link', { name: 'Projects' }), screen.getByText('Design')]
            .map(name => name.parentElement)
        // Each name stands with its slash in a step of its own, which the trail wraps between, never inside.
        expect(steps.map(step => step?.textContent)).toEqual(['Projects/', 'Design/'])
        expect(steps[0]?.lastElementChild).toHaveAttribute('aria-hidden')
        // A step left out leaves no slash behind.
        expect(steps[0]?.parentElement?.children).toHaveLength(2)
    })
})
